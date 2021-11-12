package com.genesys.cloud.messenger.transport.shyrka.send

import kotlinx.serialization.Required
import kotlinx.serialization.Serializable

@Serializable
internal data class TextMessage(
    val text: String,
    val metadata: Map<String, String>? = null
) {
    @Required val type = "Text"
}
