package com.genesys.cloud.messenger.transport.core

internal actual class MessageDispatcher(private val listener: MessageListener) {

    actual fun dispatch(event: MessageEvent) {
        listener.onEvent(event)
    }
}
