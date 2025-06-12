package com.genesys.cloud.messenger.transport.shyrka.send

import com.genesys.cloud.messenger.transport.shyrka.receive.StructuredMessageEvent
import kotlinx.serialization.Required
import kotlinx.serialization.Serializable

@Serializable
internal data class EventMessage(
    val events: List<StructuredMessageEvent>,
    val channel: Channel? = null,
) : BaseMessageProtocol() {
    @Required override val type = BaseMessageProtocol.Type.Event
}
