package com.genesys.cloud.messenger.transport.shyrka.send

import kotlinx.serialization.Required
import kotlinx.serialization.Serializable

@Serializable
internal abstract class BaseWebMessagingRequest : WebMessagingRequest {
    @Required abstract override val action: String
    @Required abstract override val token: String
    @Required abstract override val tracingId: String
}
