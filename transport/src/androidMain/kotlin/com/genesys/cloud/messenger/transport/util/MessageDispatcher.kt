package com.genesys.cloud.messenger.transport.util

import com.genesys.cloud.messenger.transport.MessageEvent

internal actual class MessageDispatcher(private val listener: MessageListener) {

    actual fun dispatch(event: MessageEvent) {
        listener.onEvent(event)
    }
}
