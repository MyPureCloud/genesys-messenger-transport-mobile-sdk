package com.genesys.cloud.messenger.transport.core.events

import com.genesys.cloud.messenger.transport.shyrka.receive.StructuredMessageEvent
import com.genesys.cloud.messenger.transport.shyrka.receive.TypingEvent
import com.genesys.cloud.messenger.transport.util.logs.Log
import com.genesys.cloud.messenger.transport.util.logs.LogTag

internal class EventHandlerImpl(
    val log: Log = Log(enableLogs = false, LogTag.EVENT_HANDLER),
) : EventHandler {

    override var eventListener: ((Event) -> Unit)? = null

    override fun onEvent(event: StructuredMessageEvent) {
        val transportEvent = event.toTransportEvent()
        log.i { "on event: $transportEvent" }
        eventListener?.invoke(transportEvent)
    }
}

private fun StructuredMessageEvent.toTransportEvent(): Event {
    return when (this) {
        is TypingEvent -> {
            Event.Typing(durationInMilliseconds = typing.duration)
        }
    }
}
