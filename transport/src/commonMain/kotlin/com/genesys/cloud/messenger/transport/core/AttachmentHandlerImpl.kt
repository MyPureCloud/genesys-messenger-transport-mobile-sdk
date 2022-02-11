package com.genesys.cloud.messenger.transport.core

import com.genesys.cloud.messenger.transport.core.Attachment.State.Deleted
import com.genesys.cloud.messenger.transport.core.Attachment.State.Deleting
import com.genesys.cloud.messenger.transport.core.Attachment.State.Detached
import com.genesys.cloud.messenger.transport.core.Attachment.State.Error
import com.genesys.cloud.messenger.transport.core.Attachment.State.Presigning
import com.genesys.cloud.messenger.transport.core.Attachment.State.Sending
import com.genesys.cloud.messenger.transport.core.Attachment.State.Uploaded
import com.genesys.cloud.messenger.transport.core.Attachment.State.Uploading
import com.genesys.cloud.messenger.transport.network.WebMessagingApi
import com.genesys.cloud.messenger.transport.shyrka.receive.PresignedUrlResponse
import com.genesys.cloud.messenger.transport.shyrka.receive.UploadSuccessEvent
import com.genesys.cloud.messenger.transport.shyrka.send.DeleteAttachmentRequest
import com.genesys.cloud.messenger.transport.shyrka.send.OnAttachmentRequest
import com.genesys.cloud.messenger.transport.util.logs.Log
import io.ktor.client.features.ResponseException
import io.ktor.http.ContentType
import io.ktor.http.defaultForFilePath
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

internal class AttachmentHandlerImpl(
    private val api: WebMessagingApi,
    private val token: String,
    private val log: Log,
    private val updateAttachmentStateWith: (Attachment) -> Unit,
    private val processedAttachments: MutableMap<String, ProcessedAttachment> = mutableMapOf()
) : IAttachmentHandler {
    private val uploadDispatcher = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun prepare(
        attachmentId: String,
        byteArray: ByteArray,
        fileName: String,
        uploadProgress: ((Float) -> Unit)?,
    ): OnAttachmentRequest {
        Attachment(id = attachmentId, fileName = fileName, state = Presigning).also {
            log.i { "Presigning attachment: $it" }
            updateAttachmentStateWith(it)
            processedAttachments[it.id] = ProcessedAttachment(
                attachment = it,
                byteArray = byteArray,
                uploadProgress = uploadProgress,
            )
        }
        return OnAttachmentRequest(
            token,
            attachmentId = attachmentId,
            fileName = fileName,
            fileType = ContentType.defaultForFilePath(fileName).toString(),
            fileSize = byteArray.size,
            errorsAsJson = true,
        )
    }

    override fun upload(presignedUrlResponse: PresignedUrlResponse) {
        processedAttachments[presignedUrlResponse.attachmentId]?.let {
            log.i { "Uploading attachment: ${it.attachment}" }
            it.attachment = it.attachment.copy(state = Uploading)
                .also(updateAttachmentStateWith)
            it.job = uploadDispatcher.launch {
                try {
                    api.uploadFile(presignedUrlResponse, it.byteArray, it.uploadProgress)
                } catch (responseException: ResponseException) {
                    onError(
                        it.attachment.id,
                        ErrorCode.mapFrom(responseException.response.status.value),
                        responseException.message ?: "ResponseException during attachment upload"
                    )
                } catch (cancellationException: CancellationException) {
                    log.w { "cancellationException during attachment upload: ${it.attachment}" }
                }
            }
        }
    }

    override fun onUploadSuccess(uploadSuccessEvent: UploadSuccessEvent) {
        processedAttachments[uploadSuccessEvent.attachmentId]?.let {
            log.i { "Attachment uploaded: ${it.attachment}" }
            it.attachment = it.attachment.copy(
                state = Uploaded(uploadSuccessEvent.downloadUrl)
            ).also(updateAttachmentStateWith)
            it.job = null
        }
    }

    override fun detach(attachmentId: String, delete: () -> Unit) {
        processedAttachments.remove(attachmentId)?.let {
            log.i { "Attachment detached: $attachmentId" }
            it.job?.cancel()
            if (it.attachment.state is Uploaded) {
                delete()
            }
            updateAttachmentStateWith(it.attachment.copy(state = Detached))
        }
    }

    override fun delete(attachmentId: String): DeleteAttachmentRequest {
        log.i { "Deleting attachment: $attachmentId" }
        updateAttachmentStateWith(Attachment(attachmentId, state = Deleting))
        return DeleteAttachmentRequest(
            token = token,
            attachmentId = attachmentId
        )
    }

    override fun onDeleted(attachmentId: String) {
        updateAttachmentStateWith(Attachment(attachmentId, state = Deleted))
    }

    override fun onError(attachmentId: String, errorCode: ErrorCode, errorMessage: String) {
        log.e { "Attachment error with id: $attachmentId. ErrorCode: $errorCode, errorMessage: $errorMessage" }
        processedAttachments.remove(attachmentId)
        updateAttachmentStateWith(Attachment(attachmentId, state = Error(errorCode, errorMessage)))
    }

    override fun onSending() {
        processedAttachments.forEach { entry ->
            entry.value.takeUploaded()?.let {
                log.i { "Sending attachment: ${it.attachment.id}" }
                it.attachment = it.attachment.copy(state = Sending)
                    .also(updateAttachmentStateWith)
            }
        }
    }

    override fun onSent(attachments: Map<String, Attachment>) {
        log.i { "Attachments sent: $attachments" }
        attachments.forEach { entry ->
            processedAttachments.remove(entry.key)?.also {
                updateAttachmentStateWith(entry.value)
            }
        }
    }
}

internal class ProcessedAttachment(
    var attachment: Attachment,
    var byteArray: ByteArray,
    var job: Job? = null,
    val uploadProgress: ((Float) -> Unit)?,
)

private fun ProcessedAttachment.takeUploaded(): ProcessedAttachment? =
    this.takeIf { it.attachment.state is Uploaded }
