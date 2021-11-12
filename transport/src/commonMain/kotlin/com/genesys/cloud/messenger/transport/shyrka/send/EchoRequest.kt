package com.genesys.cloud.messenger.transport.shyrka.send

import kotlinx.serialization.Required
import kotlinx.serialization.Serializable

@Serializable
internal data class EchoRequest(
    override val token: String
) : WebMessagingRequest {
    @Required override val action: String = RequestAction.ECHO_MESSAGE.value
    @Required val message: TextMessage = TextMessage("ping")
}
