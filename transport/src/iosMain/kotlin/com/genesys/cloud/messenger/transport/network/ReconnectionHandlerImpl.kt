package com.genesys.cloud.messenger.transport.network

import com.genesys.cloud.messenger.transport.util.logs.Log
import kotlin.native.concurrent.AtomicInt

internal const val TIMEOUT_INTERVAL = 30.0

internal actual class ReconnectionHandlerImpl actual constructor(
    reconnectionTimeoutInSeconds: Long,
    private val log: Log,
) : ReconnectionHandler {
    private var attempts = AtomicInt(0)
    private var maxAttempts: Int = (reconnectionTimeoutInSeconds / TIMEOUT_INTERVAL).toInt()

    override val shouldReconnect: Boolean
        get() = attempts.value < maxAttempts

    override fun reconnect(reconnectFun: () -> Unit) {
        if (!shouldReconnect) return
        log.i { "Trying to reconnect. Attempt number: $attempts out of $maxAttempts" }
        attempts.value++
        reconnectFun()
    }

    override fun clear() {
        attempts.value = 0
    }
}
