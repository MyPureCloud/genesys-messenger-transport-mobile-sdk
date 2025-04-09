package com.genesys.cloud.messenger.transport.shyrka.send

import kotlinx.serialization.Required
import kotlinx.serialization.Serializable

@Serializable
internal data class OnAttachmentRequest(
    override val token: String,
    val attachmentId: String,
    val fileName: String,
    val fileType: String,
    val fileSize: Int? = null,
    val fileMd5: String? = null,
    val errorsAsJson: Boolean,
) : WebMessagingRequest {
    @Required
    override val action: String = RequestAction.ON_ATTACHMENT.value

    override fun toString(): String {
        return "(action='$action', attachmentId='$attachmentId', fileName='$fileName', " +
                "fileType='$fileType', fileSize=$fileSize, fileMd5=$fileMd5)"
    }
}
