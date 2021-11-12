package com.genesys.cloud.messenger.transport.shyrka.receive

internal interface WebMessagingMessageProtocol {
    val type: String
    val code: Int
}
