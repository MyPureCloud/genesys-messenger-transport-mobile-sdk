package com.genesys.cloud.messenger.transport.shyrka.send

import com.genesys.cloud.messenger.transport.util.TracingIds
import kotlinx.serialization.Required
import kotlinx.serialization.Serializable

@Serializable
internal class OnMessageRequest(
    override val token: String,
    val message: TextMessage,
    val time: String? = null,
    @Required
    override val tracingId: String = TracingIds.newId(),
) : BaseWebMessagingRequest() {
    @Required override val action: String = RequestAction.ON_MESSAGE.value
}
