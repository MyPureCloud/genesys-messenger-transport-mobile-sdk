package com.genesys.cloud.messenger.transport.core.events

/**
 * Base class for Transport events.
 */
sealed class Event {
    /**
     *  This event indicates that the agent has begun typing a message.
     *
     *  @param durationInMilliseconds amount of time to display visual typing indicator in UI.
     */
    data class Typing(val durationInMilliseconds: Int) : Event()
}
