package com.genesys.cloud.messenger.transport.shyrka.send

import kotlinx.serialization.Required
import kotlinx.serialization.Serializable

internal interface BaseMessageProtocol {
    @Required
    val type: Type

    @Serializable
    enum class Type {
        Text,
        Event,
    }
}
