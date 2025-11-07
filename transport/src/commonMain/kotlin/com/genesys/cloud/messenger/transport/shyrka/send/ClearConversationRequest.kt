package com.genesys.cloud.messenger.transport.shyrka.send

import com.genesys.cloud.messenger.transport.shyrka.receive.PresenceEvent
import com.genesys.cloud.messenger.transport.shyrka.receive.StructuredMessageEvent
import com.genesys.cloud.messenger.transport.util.TracingIds
import kotlinx.serialization.Required
import kotlinx.serialization.Serializable

@Serializable
internal data class ClearConversationRequest(
    override val token: String,
    @Required
    override val tracingId: String = TracingIds.newId(),
) : BaseWebMessagingRequest() {
    @Required
    override val action: String = RequestAction.ON_MESSAGE.value
    @Required
    val message: EventMessage =
        EventMessage(
            listOf(
                PresenceEvent(
                    eventType = StructuredMessageEvent.Type.Presence,
                    presence = PresenceEvent.Presence(type = PresenceEvent.Presence.Type.Clear),
                )
            )
        )
}
