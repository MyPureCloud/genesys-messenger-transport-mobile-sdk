package com.genesys.cloud.messenger.transport.shyrka.send

import com.genesys.cloud.messenger.transport.util.TracingIds
import kotlinx.serialization.Required
import kotlinx.serialization.Serializable

@Serializable
internal class OnMessageRequest(
    override val token: String,
    val message: TextMessage,
    val time: String? = null,
    override val tracingId: String,
) : BaseWebMessagingRequest() {
    @Required override val action: String = RequestAction.ON_MESSAGE.value
    constructor(token: String, message: TextMessage, time: String? = null) : this(token, message, time, TracingIds.newId())
}
