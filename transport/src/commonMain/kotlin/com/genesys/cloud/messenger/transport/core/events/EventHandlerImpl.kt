package com.genesys.cloud.messenger.transport.core.events

import com.genesys.cloud.messenger.transport.shyrka.receive.StructuredMessageEvent
import com.genesys.cloud.messenger.transport.shyrka.receive.TypingEvent
import com.genesys.cloud.messenger.transport.util.logs.Log
import com.genesys.cloud.messenger.transport.util.logs.LogTag

internal class EventHandlerImpl(
    val log: Log = Log(enableLogs = false, LogTag.EVENT_HANDLER),
) : EventHandler {

    override fun onEvent(event: StructuredMessageEvent) {
        log.i { "on event: ${event.toTransportEvent()}" }
    }
}

private fun StructuredMessageEvent.toTransportEvent(): Event {
    return when (this) {
        is TypingEvent -> {
            Event.Typing(durationInMilliseconds = typing.duration)
        }
    }
}
