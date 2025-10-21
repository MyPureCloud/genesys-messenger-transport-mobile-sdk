package com.genesys.cloud.messenger.transport.shyrka.send

import com.genesys.cloud.messenger.transport.util.TracingIds
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
    @Required
    override val tracingId: String = TracingIds.newId(),
) : BaseWebMessagingRequest() {
    @Required
    override val action: String = RequestAction.ON_ATTACHMENT.value
}
