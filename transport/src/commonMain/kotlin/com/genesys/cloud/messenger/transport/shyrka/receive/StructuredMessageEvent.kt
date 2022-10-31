package com.genesys.cloud.messenger.transport.shyrka.receive

import com.genesys.cloud.messenger.transport.core.ErrorCode
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
    abstract val eventType: Type
    @Serializable
    enum class Type {
        @SerialName("Typing")
        Typing,
        Error,
        HealthChecked,
        Presence,
    }
}

@Serializable
internal data class TypingEvent(
    override val eventType: Type,
    val typing: Typing,
) : StructuredMessageEvent() {
    @Serializable
    internal data class Typing(
        val type: String,
        val duration: Long? = null,
    )
}

internal data class ErrorEvent(
    override val eventType: Type = Type.Error,
    val errorCode: ErrorCode,
    val message: String?,
) : StructuredMessageEvent()

internal data class HealthCheckEvent(
    override val eventType: Type = Type.HealthChecked,
) : StructuredMessageEvent()

@Serializable
internal data class PresenceEvent(
    override val eventType: Type,
    val presence: Presence,
) : StructuredMessageEvent() {
    @Serializable
    internal data class Presence(val type: String)
}

internal object StructuredMessageEventSerializer :
    JsonContentPolymorphicSerializer<StructuredMessageEvent>(StructuredMessageEvent::class) {
    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<out StructuredMessageEvent> {
        return when (element.jsonObject["eventType"]?.jsonPrimitive?.content) {
            Type.Typing.name -> TypingEvent.serializer()
            Type.Presence.name -> PresenceEvent.serializer()
            else -> throw SerializationException("Unknown EventType: key 'eventType' not found or does not matches any known event type.")
        }
    }
}
