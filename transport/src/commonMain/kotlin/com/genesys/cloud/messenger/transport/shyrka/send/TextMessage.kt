package com.genesys.cloud.messenger.transport.shyrka.send

import com.genesys.cloud.messenger.transport.core.Message
import kotlinx.serialization.Required
import kotlinx.serialization.Serializable

@Serializable
internal data class TextMessage(
    val text: String,
    val metadata: Map<String, String>? = null,
    val content: List<Message.Content> = emptyList(),
    val channel: Channel? = null,
    val _type: BaseMessageProtocol.Type = BaseMessageProtocol.Type.Text,
) : BaseMessageProtocol {
    @Required
    override val type = _type
}
