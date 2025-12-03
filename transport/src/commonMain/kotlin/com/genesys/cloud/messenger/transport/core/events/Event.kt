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
    data object HealthChecked : Event()

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
    data object ConversationAutostart : Event()

    /**
     * This event indicates that the conversation was ended by an agent.
     */
    data object ConversationDisconnect : Event()

    /**
     * Sent when the connection is closed. A more detailed reason is provided in the [reason] parameter.
     * Detailed information about Genesys Cloud Web Messaging capabilities is available in the [Developer Center](https://developer.genesys.cloud).
     */
    data class ConnectionClosed(val reason: Reason) : Event() {
        /**
         * Reasons for connection closure.
         *
         * @property UserSignedIn Indicates the connection was closed because the user signed in to an authenticated session on another device.
         * @property ConversationCleared Indicates the connection was closed because the user cleared the conversation.
         * @property SessionLimitReached Indicates the connection was closed due to exceeding the maximum number of simultaneously open sessions.
         */
        enum class Reason {
            UserSignedIn,
            ConversationCleared,
            SessionLimitReached,
        }
    }

    /**
     * Sent when auth code was successfully exchanged for access token.
     */
    data object Authorized : Event()

    /**
     * Sent when user has been logged out from authenticated session.
     */
    data object Logout : Event()

    /**
     * Sent when conversation was successfully cleared.
     */
    data object ConversationCleared : Event()

    /**
     * Sent when user successfully stepped up from guest to authenticated session.
     *
     * @param firstName is an optional first name of the user.
     * @param lastName is an optional last name of the user.
     */
    data class SignedIn(
        val firstName: String? = null,
        val lastName: String? = null
    ) : Event()

    /**
     *  Sent as confirmation when existing authenticated session was cleared on other devices as a result of step-up.
     */
    data object ExistingAuthSessionCleared : Event()

    /**
     * Sent when the session duration has been updated.
     *
     * @param durationInSeconds The duration of the session in seconds.
     */
    data class SessionDuration(val durationInSeconds: Long) : Event()
}
