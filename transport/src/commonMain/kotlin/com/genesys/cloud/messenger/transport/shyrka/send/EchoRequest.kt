package com.genesys.cloud.messenger.transport.shyrka.send

import com.genesys.cloud.messenger.transport.util.Platform
import kotlinx.serialization.Required
import kotlinx.serialization.Serializable

internal const val HealthCheckID = "SGVhbHRoQ2hlY2tNZXNzYWdlSWQ="

@Serializable
internal data class EchoRequest(
    override val token: String,
    override val tracingId: String = Platform().randomUUID(),
) : WebMessagingRequest {
    @Required override val action: String = RequestAction.ECHO_MESSAGE.value
    @Required val message: TextMessage = TextMessage("ping", metadata = mapOf("customMessageId" to HealthCheckID))
}
