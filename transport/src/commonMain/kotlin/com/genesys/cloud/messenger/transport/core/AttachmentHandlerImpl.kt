package com.genesys.cloud.messenger.transport.core

import com.genesys.cloud.messenger.transport.core.Attachment.State.Detached
import com.genesys.cloud.messenger.transport.core.Attachment.State.Detaching
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
import com.genesys.cloud.messenger.transport.util.logs.LogMessages
import io.ktor.client.plugins.ResponseException
import io.ktor.http.ContentType
import io.ktor.http.defaultForFilePath
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
    private val processedAttachments: MutableMap<String, ProcessedAttachment> = mutableMapOf(),
) : AttachmentHandler {
    private val uploadDispatcher = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override var fileAttachmentProfile: FileAttachmentProfile? = null

    @Throws(IllegalArgumentException::class)
    override fun prepare(
        attachmentId: String,
        byteArray: ByteArray,
        fileName: String,
        uploadProgress: ((Float) -> Unit)?,
    ): OnAttachmentRequest {
        validate(byteArray, fileName)
        Attachment(id = attachmentId, fileName = fileName, state = Presigning).also {
            log.i { LogMessages.presigningAttachment(it) }
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
            log.i { LogMessages.uploadingAttachment(it.attachment) }
            it.attachment = it.attachment.copy(state = Uploading)
                .also(updateAttachmentStateWith)
            it.job = uploadDispatcher.launch {
                when (val result = api.uploadFile(presignedUrlResponse, it.byteArray, it.uploadProgress)) {
                    is Result.Success -> {} // Nothing to do here. We are waiting for UploadSuccess/Failure Event from Shyrka.
                    is Result.Failure -> handleUploadFailure(presignedUrlResponse.attachmentId, result)
                }
            }
        }
    }

    override fun onUploadSuccess(uploadSuccessEvent: UploadSuccessEvent) {
        processedAttachments[uploadSuccessEvent.attachmentId]?.let {
            log.i { LogMessages.attachmentUploaded(it.attachment) }
            it.attachment = it.attachment.copy(
                state = Uploaded(uploadSuccessEvent.downloadUrl)
            ).also(updateAttachmentStateWith)
            it.job = null
        }
    }

    override fun detach(attachmentId: String): DeleteAttachmentRequest? {
        processedAttachments[attachmentId]?.let {
            log.i { LogMessages.detachingAttachment(attachmentId) }
            it.job?.cancel()
            if (it.attachment.state is Uploaded) {
                it.attachment =
                    it.attachment.copy(state = Detaching).also(updateAttachmentStateWith)
                return DeleteAttachmentRequest(
                    token = token,
                    attachmentId = attachmentId
                )
            } else {
                onDetached(attachmentId)
            }
        }
        return null
    }

    override fun onDetached(attachmentId: String) {
        log.i { LogMessages.attachmentDetached(attachmentId) }
        processedAttachments.remove(attachmentId)?.let {
            updateAttachmentStateWith(it.attachment.copy(state = Detached))
        }
    }

    override fun onError(attachmentId: String, errorCode: ErrorCode, errorMessage: String) {
        log.e { LogMessages.attachmentError(attachmentId, errorCode, errorMessage) }
        processedAttachments.remove(attachmentId)
        updateAttachmentStateWith(Attachment(attachmentId, state = Error(errorCode, errorMessage)))
    }

    override fun onMessageError(code: ErrorCode, message: String?) {
        processedAttachments.mapNotNull { it.value.takeSendingId() }.forEach {
            onError(it, code, message ?: "")
        }
    }

    override fun onSending() {
        processedAttachments.forEach { entry ->
            entry.value.takeUploaded()?.let {
                log.i { LogMessages.sendingAttachment(it.attachment.id) }
                it.attachment = it.attachment.copy(state = Sending)
                    .also(updateAttachmentStateWith)
            }
        }
    }

    override fun onSent(attachments: Map<String, Attachment>) {
        log.i { LogMessages.attachmentSent(attachments) }
        attachments.forEach { entry ->
            processedAttachments.remove(entry.key)?.also {
                updateAttachmentStateWith(entry.value)
            }
        }
    }

    override fun clearAll() = processedAttachments.clear()

    override fun onAttachmentRefreshed(presignedUrlResponse: PresignedUrlResponse) {
        updateAttachmentStateWith(
            Attachment(
                id = presignedUrlResponse.attachmentId,
                fileName = presignedUrlResponse.fileName,
                state = Attachment.State.Refreshed(presignedUrlResponse.url)
            )
        )
    }

    /**
     * Validate if attachment match requirements for upload.
     * In case fileAttachmentProfile is not set, consider attachment eligible for upload.
     *
     * @throws IllegalArgumentException if attachment does not match [fileAttachmentProfile] requirements.
     */
    @Throws(IllegalArgumentException::class)
    private fun validate(byteArray: ByteArray, fileName: String) {
        fileAttachmentProfile?.let {
            if (!it.enabled) {
                throw IllegalArgumentException(ErrorMessage.FileAttachmentIsDisabled)
            }
            if (byteArray.isEmpty()) {
                throw IllegalArgumentException(ErrorMessage.FileSizeIsToSmall)
            }
            if (byteArray.isInvalid(it.maxFileSizeKB)) {
                throw IllegalArgumentException(ErrorMessage.fileSizeIsTooBig(it.maxFileSizeKB))
            }
            if (fileName.isProhibited(it.blockedFileTypes)) {
                throw IllegalArgumentException(ErrorMessage.fileTypeIsProhibited(fileName))
            }
        }
    }

    private fun handleUploadFailure(attachmentId: String, result: Result.Failure) {
        if (result.errorCode is ErrorCode.CancellationError) {
            log.w { "Cancellation exception was thrown, while uploading attachment." }
            return
        }

        log.e { "uploadFile($attachmentId) respond with error: ${result.errorCode}, and message: ${result.message}" }
        onError(
            attachmentId = attachmentId,
            errorCode = result.errorCode,
            errorMessage = result.message ?: "ResponseException during attachment upload",
        )
    }
}

private fun ByteArray.isInvalid(maxFileSizeKB: Long?): Boolean =
    maxFileSizeKB?.let { this.toKB() > maxFileSizeKB } ?: false

private fun String.isProhibited(blockedFileTypes: List<String>): Boolean =
    blockedFileTypes.contains(".${substringAfterLast('.')}")

internal class ProcessedAttachment(
    var attachment: Attachment,
    var byteArray: ByteArray,
    var job: Job? = null,
    val uploadProgress: ((Float) -> Unit)? = null,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as ProcessedAttachment

        if (attachment != other.attachment) return false
        if (!byteArray.contentEquals(other.byteArray)) return false
        if (job != other.job) return false
        if (uploadProgress != other.uploadProgress) return false

        return true
    }

    override fun hashCode(): Int {
        var result = attachment.hashCode()
        result = 31 * result + byteArray.contentHashCode()
        result = 31 * result + (job?.hashCode() ?: 0)
        result = 31 * result + (uploadProgress?.hashCode() ?: 0)
        return result
    }
}

private fun ProcessedAttachment.takeSendingId(): String? =
    this.takeIf { it.attachment.state is Sending }?.attachment?.id

private fun ProcessedAttachment.takeUploaded(): ProcessedAttachment? =
    this.takeIf { it.attachment.state is Uploaded }

private fun ByteArray.toKB(): Long = size / 1000L
