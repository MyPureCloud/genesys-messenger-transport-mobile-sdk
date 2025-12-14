package com.genesys.cloud.messenger.transport.shyrka.send

import com.genesys.cloud.messenger.transport.util.Platform
import kotlinx.serialization.Required
import kotlinx.serialization.Serializable

@Serializable
internal data class ConfigureAuthenticatedSessionRequest(
    override val token: String,
    val deploymentId: String,
    val startNew: Boolean,
    val journeyContext: JourneyContext? = null,
    val data: Data,
    override val tracingId: String = Platform().randomUUID(),
) : WebMessagingRequest {
    @Required
    override val action: String = RequestAction.CONFIGURE_AUTHENTICATED_SESSION.value

    @Serializable
    internal data class Data(val code: String)
}
