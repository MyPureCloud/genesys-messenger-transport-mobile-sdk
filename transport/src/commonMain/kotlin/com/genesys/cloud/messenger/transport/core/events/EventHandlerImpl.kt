package com.genesys.cloud.messenger.transport.core.events

import com.genesys.cloud.messenger.transport.shyrka.receive.PresenceEvent
import com.genesys.cloud.messenger.transport.shyrka.receive.StructuredMessageEvent
import com.genesys.cloud.messenger.transport.shyrka.receive.TypingEvent
import com.genesys.cloud.messenger.transport.shyrka.receive.Unknown
import com.genesys.cloud.messenger.transport.util.logs.Log
import com.genesys.cloud.messenger.transport.util.logs.LogMessages
import com.genesys.cloud.messenger.transport.util.logs.LogTag

private const val FALLBACK_TYPING_INDICATOR_DURATION = 5000L

internal class EventHandlerImpl(
    internal val log: Log = Log(enableLogs = false, LogTag.EVENT_HANDLER),
) : EventHandler {

    override var eventListener: ((Event) -> Unit)? = null

    override fun onEvent(event: Event?) {
        if (event == null) {
            log.i { LogMessages.UNKNOWN_EVENT_RECEIVED }
            return
        }
        log.i { LogMessages.onEvent(event) }
        eventListener?.invoke(event)
    }
}

internal fun StructuredMessageEvent.toTransportEvent(): Event? {
    return when (this) {
        is TypingEvent -> {
            Event.AgentTyping(typing.duration ?: FALLBACK_TYPING_INDICATOR_DURATION)
        }
        is PresenceEvent -> {
            when (this.presence.type) {
                PresenceEvent.Presence.Type.Join -> Event.ConversationAutostart
                PresenceEvent.Presence.Type.Disconnect -> Event.ConversationDisconnect
                PresenceEvent.Presence.Type.Clear -> null // Ignore. Event.ConversationClear should be dispatched upon receiving SessionClearedEvent and not StructuredMessageEvent with Type.Clear
            }
        }
        is Unknown -> null
    }
}
