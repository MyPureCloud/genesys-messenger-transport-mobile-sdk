package com.genesys.cloud.messenger.transport.shyrka.receive

import kotlinx.serialization.Serializable

/**
 * The base message type for messages received from the Web Messaging WebSocket service.
 */
@Serializable
internal data class WebMessagingMessage<T>(
    override val type: String,
    override val code: Int,
    val body: T
) : WebMessagingMessageProtocol {

//    override fun toString(): String {
//        return "WebMessagingMessage(type='$type', code=$code, body=${body.toString()})"
//    }
}
