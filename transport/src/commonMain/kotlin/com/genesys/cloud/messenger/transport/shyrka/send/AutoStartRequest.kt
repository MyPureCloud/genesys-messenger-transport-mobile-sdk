package com.genesys.cloud.messenger.transport.shyrka.send

import com.genesys.cloud.messenger.transport.shyrka.receive.PresenceEvent
import com.genesys.cloud.messenger.transport.shyrka.receive.StructuredMessageEvent
import kotlinx.serialization.Required
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
internal data class AutoStartRequest(
    override val token: String,
    @Transient
    val customAttributes: Map<String, String> = emptyMap(),
) : WebMessagingRequest {
    @Required
    override val action: String = RequestAction.ON_MESSAGE.value
    @Required
    val message: EventMessage = EventMessage(
        listOf(
            PresenceEvent(
                eventType = StructuredMessageEvent.Type.Presence,
                presence = PresenceEvent.Presence(type = PresenceEvent.Presence.Type.Join),
            )
        ),
        channel = Channel(Channel.Metadata(customAttributes)),
    )
}
