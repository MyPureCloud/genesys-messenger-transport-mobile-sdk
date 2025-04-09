package com.genesys.cloud.messenger.transport.shyrka.send

import kotlinx.serialization.Required
import kotlinx.serialization.Serializable

@Serializable
internal data class ConfigureSessionRequest(
    override val token: String,
    val deploymentId: String,
    val startNew: Boolean,
    val journeyContext: JourneyContext? = null,
) : WebMessagingRequest {
    @Required override val action: String = RequestAction.CONFIGURE_SESSION.value

    override fun toString(): String {
        return "(action='$action', journeyContext=$journeyContext, startNew=$startNew)"
    }
}
