package com.genesys.cloud.messenger.transport.shyrka.send

import com.genesys.cloud.messenger.transport.util.TracingIds
import kotlinx.serialization.Required
import kotlinx.serialization.Serializable

@Serializable
internal data class ConfigureSessionRequest(
    override val token: String,
    val deploymentId: String,
    val startNew: Boolean,
    val journeyContext: JourneyContext? = null,
    @Required
    override val tracingId: String = TracingIds.newId(),
) : BaseWebMessagingRequest() {
    @Required override val action: String = RequestAction.CONFIGURE_SESSION.value
}
