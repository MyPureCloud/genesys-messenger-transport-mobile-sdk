package com.genesys.cloud.messenger.transport.shyrka.send

import com.genesys.cloud.messenger.transport.core.Message
import kotlinx.serialization.Required

internal data class StructuredMessage (
    val text: String,
    val metadata: Map<String, String>? = null,
    val content: List<Message.Content> = emptyList(),
) : BaseMessageProtocol {
    @Required
    override val type = BaseMessageProtocol.Type.Structured
}
