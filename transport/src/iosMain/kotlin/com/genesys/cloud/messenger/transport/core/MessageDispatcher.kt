package com.genesys.cloud.messenger.transport.core

import kotlin.native.ref.WeakReference

internal actual class MessageDispatcher(listener: MessageListener) {
    private val listenerWeakRef = WeakReference(listener)

    actual fun dispatch(event: MessageEvent) {
        listenerWeakRef.value?.onEvent(event)
    }
}
