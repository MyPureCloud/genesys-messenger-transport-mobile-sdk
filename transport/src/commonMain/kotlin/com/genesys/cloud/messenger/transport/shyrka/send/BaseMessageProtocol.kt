package com.genesys.cloud.messenger.transport.shyrka.send

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

@Serializable(with = BaseMessagingProtocolSerializer::class)
internal sealed class BaseMessageProtocol {
    abstract val type: Type

    @Serializable
    enum class Type {
        Text,
        Event,
        Structured
    }
}

internal object BaseMessagingProtocolSerializer :
    JsonContentPolymorphicSerializer<BaseMessageProtocol>(BaseMessageProtocol::class) {
    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<out BaseMessageProtocol> {
        return when (element.jsonObject["type"]?.jsonPrimitive?.contentOrNull) {
            BaseMessageProtocol.Type.Text.name -> TextMessage.serializer()
            BaseMessageProtocol.Type.Structured.name -> StructuredMessage.serializer()
            BaseMessageProtocol.Type.Event.name -> EventMessage.serializer()
            else -> error("Unknown type for BaseMessagingProtocol")
        }
    }
}
