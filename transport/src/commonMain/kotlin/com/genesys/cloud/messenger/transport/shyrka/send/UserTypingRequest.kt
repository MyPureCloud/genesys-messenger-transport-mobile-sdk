package com.genesys.cloud.messenger.transport.shyrka.send

import com.genesys.cloud.messenger.transport.shyrka.receive.StructuredMessageEvent
import com.genesys.cloud.messenger.transport.shyrka.receive.TypingEvent
import kotlinx.serialization.Required
import kotlinx.serialization.Serializable

@Serializable
internal data class UserTypingRequest(
    override val token: String,
) : WebMessagingRequest {
    @Required
    override val action: String = RequestAction.ON_MESSAGE.value

    @Required
    val message = EventMessage(
        listOf(
            TypingEvent(
                eventType = StructuredMessageEvent.Type.Typing,
                typing = TypingEvent.Typing(type = "On"),
            )
        )
    )

    override fun toString(): String {
        return "(action='$action', message=$message)"
    }
}
