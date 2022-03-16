package com.genesys.cloud.messenger.transport.shyrka.send

import com.genesys.cloud.messenger.transport.core.Message
import kotlinx.serialization.Required
import kotlinx.serialization.Serializable

@Serializable
internal data class TextMessage(
    val text: String,
    val metadata: Map<String, String>? = null,
    val content: List<Message.Content> = emptyList()
) {
    @Required val type = "Text"
}
