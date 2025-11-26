package com.genesys.cloud.messenger.transport.shyrka.send

import kotlinx.serialization.Required
import kotlinx.serialization.Serializable

@Serializable
internal data class DeleteAttachmentRequest(
    override val token: String,
    val attachmentId: String,
    @Required
    override val tracingId: String,
) : BaseWebMessagingRequest() {
    @Required
    override val action: String = RequestAction.DELETE_ATTACHMENT.value
}
