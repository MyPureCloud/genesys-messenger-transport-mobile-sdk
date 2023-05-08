package com.genesys.cloud.messenger.transport.shyrka.receive

import com.genesys.cloud.messenger.transport.shyrka.receive.StructuredMessageEvent.Type
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
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
        Error,
        Presence,
    }
}

@Serializable
internal data class TypingEvent(
    val eventType: Type,
    val typing: Typing,
) : StructuredMessageEvent() {
    @Serializable
    internal data class Typing(
        val type: String,
        val duration: Long? = null,
    )
}

@Serializable
internal data class PresenceEvent(
    val eventType: Type,
    val presence: Presence,
) : StructuredMessageEvent() {
    @Serializable
    internal data class Presence(val type: Type) {
        @Serializable
        internal enum class Type {
            Disconnect,
            Join,
        }
    }
}

@Serializable
internal object Unknown : StructuredMessageEvent()

internal object StructuredMessageEventSerializer :
    JsonContentPolymorphicSerializer<StructuredMessageEvent>(StructuredMessageEvent::class) {
    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<out StructuredMessageEvent> {
        return when (element.jsonObject["eventType"]?.jsonPrimitive?.content) {
            Type.Typing.name -> TypingEvent.serializer()
            Type.Presence.name -> PresenceEvent.serializer()
            else -> Unknown.serializer()
        }
    }
}
