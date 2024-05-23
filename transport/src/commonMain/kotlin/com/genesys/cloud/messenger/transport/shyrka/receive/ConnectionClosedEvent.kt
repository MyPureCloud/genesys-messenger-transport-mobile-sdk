package com.genesys.cloud.messenger.transport.shyrka.receive

import com.genesys.cloud.messenger.transport.core.events.Event.ConnectionClosed
import kotlinx.serialization.Serializable

@Serializable
internal class ConnectionClosedEvent(val reason: String? = null)

internal fun String?.toTransportConnectionClosedReason(clearingConversation: Boolean): ConnectionClosed.Reason {
    return when {
        this == "signedIn" -> ConnectionClosed.Reason.UserSignedIn
        clearingConversation -> ConnectionClosed.Reason.ConversationCleared
        else -> ConnectionClosed.Reason.SessionLimitReached
    }
}
