package com.genesys.cloud.messenger.transport.core

import com.genesys.cloud.messenger.transport.core.Attachment.State.Presigning
import com.genesys.cloud.messenger.transport.core.Attachment.State.Uploaded
import com.genesys.cloud.messenger.transport.core.Attachment.State.Uploading
import com.genesys.cloud.messenger.transport.network.WebMessagingApi
import com.genesys.cloud.messenger.transport.shyrka.receive.PresignedUrlResponse
import com.genesys.cloud.messenger.transport.shyrka.receive.UploadSuccessEvent
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

internal class AttachmentHandler(
    private val api: WebMessagingApi,
    private val token: String,
    private val log: Log,
    private val updateAttachmentStateWith: (Attachment) -> Unit,
) {
    private val processedAttachments = mutableMapOf<String, ProcessedAttachment>()
    private val uploadDispatcher = CoroutineScope(Dispatchers.Main + SupervisorJob())

    fun prepareAttachment(
        attachmentId: String,
        byteArray: ByteArray,
        fileName: String,
        uploadProgress: ((Float) -> Unit)? = null,
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

    fun upload(presignedUrlResponse: PresignedUrlResponse) {
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

    fun uploadSuccess(uploadSuccessEvent: UploadSuccessEvent) {
        processedAttachments[uploadSuccessEvent.attachmentId]?.let {
            log.i { "Attachment uploaded: ${it.attachment}" }
            it.attachment = it.attachment.copy(
                state = Uploaded(uploadSuccessEvent.downloadUrl)
            ).also(updateAttachmentStateWith)
            it.job = null
        }
    }

    fun detach(attachmentId: String, delete: () -> Unit) {
        processedAttachments[attachmentId]?.let {
            log.i { "Detaching: ${it.attachment}" }
            when (it.attachment.state) {
                is Uploaded -> delete()
                else -> removeAttachment(it.attachment.id)
            }
            updateAttachmentStateWith(it.attachment.copy(state = Attachment.State.Detached))
        }
    }

    fun onDeleted(attachmentId: String) {
        processedAttachments.remove(attachmentId)?.let {
            log.i { "Attachment deleted: ${it.attachment}" }
            updateAttachmentStateWith(it.attachment.copy(state = Attachment.State.Deleted))
        }
    }

    fun onError(attachmentId: String, errorCode: ErrorCode, errorMessage: String) {
        processedAttachments[attachmentId]?.let {
            log.e { "Attachment error. ErrorCode: $errorCode, errorMessage: $errorMessage" }
            updateAttachmentStateWith(
                it.attachment.copy(
                    state = Attachment.State.Error(
                        errorCode,
                        errorMessage
                    )
                )
            )
        }
    }

    fun clear() = processedAttachments.forEach { removeAttachment(it.value.attachment.id) }

    private fun removeAttachment(id: String) =
        processedAttachments.remove(id)?.job?.cancel()
}

private class ProcessedAttachment(
    var attachment: Attachment,
    var byteArray: ByteArray,
    var job: Job? = null,
    val uploadProgress: ((Float) -> Unit)?,
)
