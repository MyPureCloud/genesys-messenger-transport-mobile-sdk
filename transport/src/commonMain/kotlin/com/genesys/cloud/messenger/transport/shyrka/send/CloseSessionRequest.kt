package com.genesys.cloud.messenger.transport.shyrka.send

import kotlinx.serialization.Required
import kotlinx.serialization.Serializable

@Serializable
internal data class CloseSessionRequest(
    override val token: String,
    val closeAllConnections: Boolean,
) : WebMessagingRequest {
    @Required
    override val action: String = RequestAction.CLOSE_SESSION.value

    override fun toString(): String {
        return "(action='$action', closeAllConnections=$closeAllConnections)"
    }
}
