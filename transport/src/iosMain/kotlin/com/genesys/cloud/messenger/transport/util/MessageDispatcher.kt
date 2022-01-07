package com.genesys.cloud.messenger.transport.util

import com.genesys.cloud.messenger.transport.MessageEvent
import kotlin.native.ref.WeakReference

internal actual class MessageDispatcher(listener: MessageListener) {
    private val listenerWeakRef = WeakReference(listener)

    actual fun dispatch(event: MessageEvent) {
        listenerWeakRef.value?.onEvent(event)
    }
}
