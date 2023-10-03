package com.genesys.cloud.messenger.transport.core

import com.genesys.cloud.messenger.transport.shyrka.receive.PresignedUrlResponse
import com.genesys.cloud.messenger.transport.shyrka.receive.UploadSuccessEvent
import com.genesys.cloud.messenger.transport.shyrka.send.DeleteAttachmentRequest
import com.genesys.cloud.messenger.transport.shyrka.send.OnAttachmentRequest

internal interface AttachmentHandler {
    var fileAttachmentProfile: FileAttachmentProfile?

    @Throws(IllegalArgumentException::class)
    fun prepare(
        attachmentId: String,
        byteArray: ByteArray,
        fileName: String,
        uploadProgress: ((Float) -> Unit)? = null
    ): OnAttachmentRequest

    fun upload(presignedUrlResponse: PresignedUrlResponse)

    fun detach(attachmentId: String): DeleteAttachmentRequest?

    fun onUploadSuccess(uploadSuccessEvent: UploadSuccessEvent)

    fun onDetached(attachmentId: String)

    fun onError(attachmentId: String, errorCode: ErrorCode, errorMessage: String)

    fun onSending()

    fun onSent(attachments: Map<String, Attachment>)

    fun onMessageError(code: ErrorCode, message: String?)

    fun onAttachmentRefreshed(presignedUrlResponse: PresignedUrlResponse)

    fun clearAll()
}
