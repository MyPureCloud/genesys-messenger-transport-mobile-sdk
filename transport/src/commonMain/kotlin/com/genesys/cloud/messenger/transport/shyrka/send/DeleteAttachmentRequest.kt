package com.genesys.cloud.messenger.transport.shyrka.send

import com.genesys.cloud.messenger.transport.util.TracingIds
import kotlinx.serialization.Required
import kotlinx.serialization.Serializable

@Serializable
internal data class DeleteAttachmentRequest(
    override val token: String,
    val attachmentId: String,
    override val tracingId: String,
) : BaseWebMessagingRequest() {
    @Required
    override val action: String = RequestAction.DELETE_ATTACHMENT.value
    constructor(token: String, attachmentId: String) : this(token, attachmentId, TracingIds.newId())
}
