package com.genesys.cloud.messenger.transport.shyrka.send

import com.genesys.cloud.messenger.transport.util.TracingIds
import kotlinx.serialization.Required
import kotlinx.serialization.Serializable

@Serializable
internal data class ConfigureAuthenticatedSessionRequest(
    override val token: String,
    val deploymentId: String,
    val startNew: Boolean,
    val journeyContext: JourneyContext? = null,
    val data: Data,
    override val tracingId: String,
) : BaseWebMessagingRequest() {
    @Required
    override val action: String = RequestAction.CONFIGURE_AUTHENTICATED_SESSION.value
    constructor(token: String, deploymentId: String, startNew: Boolean, journeyContext: JourneyContext? = null, data: Data) :
        this(token, deploymentId, startNew, journeyContext, data, TracingIds.newId())

    @Serializable
    internal data class Data(val code: String)
}
