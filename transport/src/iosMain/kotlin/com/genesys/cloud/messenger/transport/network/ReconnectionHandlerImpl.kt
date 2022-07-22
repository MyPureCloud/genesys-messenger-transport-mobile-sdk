package com.genesys.cloud.messenger.transport.network

import com.genesys.cloud.messenger.transport.util.logs.Log
import kotlinx.cinterop.staticCFunction
import kotlinx.coroutines.*
import platform.Foundation.NSThread
import platform.darwin.dispatch_get_main_queue
import platform.darwin.dispatch_sync_f
import kotlin.native.concurrent.AtomicInt
import kotlin.native.concurrent.AtomicReference
import kotlin.native.concurrent.Continuation0
import kotlin.native.concurrent.callContinuation0

internal class ReconnectionHandlerImpl(
    private val maxReconnectionAttempts: Int,
    private val log: Log,
) : ReconnectionHandler {
    private var attempts = AtomicInt(0)
    override val shouldReconnect: Boolean
        get() = attempts.value < maxReconnectionAttempts


    override fun reconnect(reconnectFun: () -> Unit) {
        if (!shouldReconnect) return
        log.i { "Trying to reconnect. Attempts: $attempts" }
        attempts.value++
        reconnectFun()
    }

    override fun clear() {
        attempts.value = 0
    }
}
