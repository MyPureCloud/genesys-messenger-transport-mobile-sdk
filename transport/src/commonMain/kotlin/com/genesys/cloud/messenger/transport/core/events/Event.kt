package com.genesys.cloud.messenger.transport.core.events

import com.genesys.cloud.messenger.transport.core.CorrectiveAction
import com.genesys.cloud.messenger.transport.core.ErrorCode

/**
 * Base class for Transport events.
 */
@kotlinx.serialization.Serializable
sealed class Event {
    /**
     *  This event indicates that the agent has begun typing a message.
     *
     *  @param durationInMilliseconds amount of time to display visual typing indicator in UI.
     */
    data class AgentTyping(val durationInMilliseconds: Long) : Event()

    /**
     * This event indicates a successful health check response to the
     * [com.genesys.cloud.messenger.transport.core.MessagingClient.sendHealthCheck] call.
     */
    object HealthChecked : Event()

    /**
     * This event indicates error.
     *
     * @param errorCode indicating what went wrong.
     * @param message is an optional message.
     * @param correctiveAction is an action that can help resolve this error.
     */
    data class Error(
        val errorCode: ErrorCode,
        val message: String?,
        val correctiveAction: CorrectiveAction,
    ) : Event()

    /**
     * This event indicates that the conversation was successfully autostarted.
     */
    object ConversationAutostart : Event()

    /**
     * This event indicates that the conversation was ended by an agent.
     */
    object ConversationDisconnect : Event()

    /**
     * Sent when the connection is closed due to exceeding the maximum number of simultaneously open sessions.
     * Detailed information about Genesys Cloud Web Messaging capabilities is available in the [Developer Center](https://developer.genesys.cloud).
     */
    object ConnectionClosed : Event()

    /**
     * Sent when auth code was successfully exchanged for access token.
     */
    object Authenticated : Event()

    /**
     * Sent when user has been logged out from authenticated session.
     */
    object Logout : Event()
}
