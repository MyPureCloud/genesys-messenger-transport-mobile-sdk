package com.genesys.cloud.messenger.transport.core.events

import com.genesys.cloud.messenger.transport.core.CorrectiveAction
import com.genesys.cloud.messenger.transport.core.ErrorCode

/**
 * Base class for Transport events.
 */
sealed class Event {
    /**
     *  This event indicates that the agent has begun typing a message.
     *
     *  @param durationInMilliseconds amount of time to display visual typing indicator in UI.
     */
    data class AgentTyping(val durationInMilliseconds: Long) : Event()

    /**
     * This event indicates error.
     *
     * @param errorCode indicating what went wrong.
     * @param message is an optional message.
     * @param correctiveAction is an action that can help resolve this error.
     */
    data class Error(val errorCode: ErrorCode, val message: String?, val correctiveAction: CorrectiveAction) : Event()
}
