package com.genesys.cloud.messenger.transport.shyrka.send

import com.genesys.cloud.messenger.transport.shyrka.receive.StructuredMessageEvent
import com.genesys.cloud.messenger.transport.shyrka.receive.TypingEvent
import com.genesys.cloud.messenger.transport.util.TracingIds
import kotlinx.serialization.Required
import kotlinx.serialization.Serializable

@Serializable
internal data class UserTypingRequest(
    override val token: String,
    override val tracingId: String,
) : BaseWebMessagingRequest() {
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
    constructor(token: String) : this(token, TracingIds.newId())
}
