package com.genesys.cloud.messenger.transport.network

import com.genesys.cloud.messenger.transport.util.logs.Log
import com.genesys.cloud.messenger.transport.util.logs.LogMessages
import kotlin.concurrent.AtomicInt
import kotlinx.cinterop.ExperimentalForeignApi

internal const val TIMEOUT_INTERVAL = 30.0

@ExperimentalForeignApi
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
        log.i { LogMessages.tryingToReconnect(attempts.value, maxAttempts) }
        attempts.value++
        reconnectFun()
    }

    override fun clear() {
        attempts.value = 0
    }
}
