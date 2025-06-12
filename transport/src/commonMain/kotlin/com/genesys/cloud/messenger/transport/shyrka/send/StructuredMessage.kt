package com.genesys.cloud.messenger.transport.shyrka.send

import com.genesys.cloud.messenger.transport.core.Message
import kotlinx.serialization.Required
import kotlinx.serialization.Serializable

@Serializable
internal data class StructuredMessage(
    val text: String,
    val metadata: Map<String, String>? = null,
    val content: List<Message.Content> = emptyList(),
    val channel: Channel? = null,
) : BaseMessageProtocol() {
    @Required
    override val type = BaseMessageProtocol.Type.Structured
}
