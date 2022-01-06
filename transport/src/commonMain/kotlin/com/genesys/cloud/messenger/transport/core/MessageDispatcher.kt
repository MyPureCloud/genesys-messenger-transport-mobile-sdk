package com.genesys.cloud.messenger.transport.core

internal expect class MessageDispatcher {
    fun dispatch(event: MessageEvent)
}

interface MessageListener {
    fun onEvent(event: MessageEvent)
}
