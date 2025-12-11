package com.genesys.cloud.messenger.transport.shyrka.send

import com.genesys.cloud.messenger.transport.util.Platform
import kotlinx.serialization.Required
import kotlinx.serialization.Serializable

@Serializable
internal data class GetAttachmentRequest(
    override val token: String,
    val attachmentId: String,
    override val tracingId: String = Platform().randomUUID(),
) : WebMessagingRequest {
    @Required
    override val action: String = RequestAction.GET_ATTACHMENT.value
}
