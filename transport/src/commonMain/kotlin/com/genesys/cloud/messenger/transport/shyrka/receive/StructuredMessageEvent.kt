package com.genesys.cloud.messenger.transport.shyrka.receive

import com.genesys.cloud.messenger.transport.shyrka.receive.StructuredMessageEvent.Type
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

@Serializable(with = StructuredMessageEventSerializer::class)
internal sealed class StructuredMessageEvent {
    @Serializable
    enum class Type {
        @SerialName("Typing")
        Typing,
    }
}

@Serializable
internal data class TypingEvent(
    val typing: Typing,
) : StructuredMessageEvent() {
    @Serializable
    internal data class Typing(
        val type: String,
        val duration: Int,
    )
}

internal object StructuredMessageEventSerializer :
    JsonContentPolymorphicSerializer<StructuredMessageEvent>(StructuredMessageEvent::class) {
    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<out StructuredMessageEvent> {
        return when (element.jsonObject["eventType"]?.jsonPrimitive?.content) {
            Type.Typing.name -> TypingEvent.serializer()
            else -> throw SerializationException("Unknown EventType: key 'eventType' not found or does not matches any known event type.")
        }
    }
}
