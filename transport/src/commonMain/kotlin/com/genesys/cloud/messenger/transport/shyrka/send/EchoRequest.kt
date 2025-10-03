package com.genesys.cloud.messenger.transport.shyrka.send

import kotlinx.serialization.Required
import kotlinx.serialization.Serializable

internal const val HealthCheckID = "SGVhbHRoQ2hlY2tNZXNzYWdlSWQ="

@Serializable
internal data class EchoRequest(
    @Required override val token: String,
    @Required override val tracingId: String = HealthCheckID
) : BaseWebMessagingRequest() {
    @Required override val action: String = RequestAction.ECHO_MESSAGE.value
    @Required val message: TextMessage = TextMessage("ping")
}
