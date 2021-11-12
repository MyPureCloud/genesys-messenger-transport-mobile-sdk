package com.genesys.cloud.messenger.transport.shyrka.receive

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * This class can be used to identify received Web Messaging messages based on the "class" property.
 */
@Serializable
internal data class PreIdentifiedWebMessagingMessage(
    override val type: String,
    override val code: Int,
    @SerialName("class")
    val className: String,
) : WebMessagingMessageProtocol
