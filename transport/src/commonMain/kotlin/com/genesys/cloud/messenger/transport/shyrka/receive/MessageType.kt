package com.genesys.cloud.messenger.transport.shyrka.receive

internal enum class MessageType(val value: String) {
    Message("message"),
    Response("response")
}
