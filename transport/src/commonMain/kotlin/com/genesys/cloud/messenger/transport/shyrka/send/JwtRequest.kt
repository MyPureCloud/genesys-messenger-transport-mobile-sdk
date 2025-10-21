package com.genesys.cloud.messenger.transport.shyrka.send

import com.genesys.cloud.messenger.transport.util.TracingIds
import kotlinx.serialization.Required
import kotlinx.serialization.Serializable

@Serializable
internal data class JwtRequest(
    override val token: String,
    @Required
    override val tracingId: String = TracingIds.newId(),
) : BaseWebMessagingRequest() {
    @Required
    override val action: String = RequestAction.GET_JWT.value
}
