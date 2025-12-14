package com.genesys.cloud.messenger.transport.shyrka.send

import com.genesys.cloud.messenger.transport.util.Platform
import kotlinx.serialization.Required
import kotlinx.serialization.Serializable

@Serializable
internal data class JwtRequest(
    override val token: String,
    override val tracingId: String = Platform().randomUUID(),
) : WebMessagingRequest {
    @Required
    override val action: String = RequestAction.GET_JWT.value
}
