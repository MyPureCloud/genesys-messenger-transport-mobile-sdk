package com.genesys.cloud.messenger.transport.shyrka.send

import com.genesys.cloud.messenger.transport.util.Platform
import kotlinx.serialization.Required
import kotlinx.serialization.Serializable

@Serializable
internal class OnMessageRequest(
    override val token: String,
    val message: BaseMessageProtocol,
    val time: String? = null,
    override val tracingId: String = Platform().randomUUID(),
) : WebMessagingRequest {
    @Required override val action: String = RequestAction.ON_MESSAGE.value
}
