package com.genesys.cloud.messenger.transport.shyrka.send

import com.genesys.cloud.messenger.transport.util.Platform
import kotlinx.serialization.Required
import kotlinx.serialization.Serializable

@Serializable
internal data class DeleteAttachmentRequest(
    override val token: String,
    val attachmentId: String,
    override val tracingId: String = Platform().randomUUID()
) : BaseWebMessagingRequest() {
    @Required
    override val action: String = RequestAction.DELETE_ATTACHMENT.value
}
