package com.genesys.cloud.messenger.transport.shyrka.send

import kotlinx.serialization.Required
import kotlinx.serialization.Serializable

@Serializable
internal data class DeleteAttachmentRequest(
    override val token: String,
    val attachmentId: String
) : WebMessagingRequest {
    @Required
    override val action: String = RequestAction.DELETE_ATTACHMENT.value

    override fun toString(): String {
        return "(action='$action', attachmentId='$attachmentId')"
    }
}
