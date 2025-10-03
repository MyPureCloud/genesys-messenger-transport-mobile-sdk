package com.genesys.cloud.messenger.transport.shyrka.send

import com.genesys.cloud.messenger.transport.util.Platform
import kotlinx.serialization.Required
import kotlinx.serialization.Serializable

@Serializable
internal data class CloseSessionRequest(
    override val token: String,
    val closeAllConnections: Boolean,
    override val tracingId: String = Platform().randomUUID(),
) : BaseWebMessagingRequest() {
    @Required
    override val action: String = RequestAction.CLOSE_SESSION.value
}
