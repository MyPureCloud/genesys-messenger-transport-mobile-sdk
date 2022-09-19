package com.genesys.cloud.messenger.transport.core.events

import com.genesys.cloud.messenger.transport.core.toCorrectiveAction
import com.genesys.cloud.messenger.transport.shyrka.receive.ErrorEvent
import com.genesys.cloud.messenger.transport.shyrka.receive.HealthCheckEvent
import com.genesys.cloud.messenger.transport.shyrka.receive.StructuredMessageEvent
import com.genesys.cloud.messenger.transport.shyrka.receive.TypingEvent
import com.genesys.cloud.messenger.transport.util.logs.Log
import com.genesys.cloud.messenger.transport.util.logs.LogTag

private const val FALLBACK_TYPING_INDICATOR_DURATION = 5000L

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
            Event.AgentTyping(typing.duration ?: FALLBACK_TYPING_INDICATOR_DURATION)
        }
        is ErrorEvent -> {
            Event.Error(
                errorCode = errorCode,
                message = message,
                correctiveAction = errorCode.toCorrectiveAction()
            )
        }
        is HealthCheckEvent -> Event.HealthChecked
    }
}
