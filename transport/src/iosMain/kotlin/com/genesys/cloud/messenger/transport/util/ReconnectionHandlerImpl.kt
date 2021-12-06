package com.genesys.cloud.messenger.transport.util

import com.genesys.cloud.messenger.transport.util.logs.Log
import kotlin.native.concurrent.AtomicInt
import kotlin.native.concurrent.Continuation0
import kotlin.native.concurrent.callContinuation0
import kotlinx.cinterop.staticCFunction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import platform.Foundation.NSThread
import platform.darwin.dispatch_get_main_queue
import platform.darwin.dispatch_sync_f

internal class ReconnectionHandlerImpl(
    private val maxReconnectionAttempts: Int,
    private val log: Log
): ReconnectionHandler {
    private var attempts = AtomicInt(0)
    private val dispatcher = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun reconnect(reconnectFun: () -> Unit) {
        if(!shouldReconnect()) return
        val wrappedReconnectFun = wrapReconnectFunWithContinuation(reconnectFun)
        dispatcher.launch {
            log.i { "Trying to reconnect. Attempts: $attempts" }
            delay(attempts.value * DELAY_DELTA_IN_MILLISECONDS)
            withContext(Dispatchers.Default) {
                attempts.value++
                wrappedReconnectFun()
            }
        }
    }

    override fun resetAttempts() {
        attempts.value = 0
    }

    override fun shouldReconnect(): Boolean = attempts.value < maxReconnectionAttempts

    private inline fun wrapReconnectFunWithContinuation(noinline reconnectFun: () -> Unit): () -> Unit =
        Continuation0(
            reconnectFun,
            staticCFunction { invokerArg ->
                if (NSThread.isMainThread()) {
                    invokerArg!!.callContinuation0()
                } else {
                    dispatch_sync_f(dispatch_get_main_queue(), invokerArg, staticCFunction { args ->
                        args!!.callContinuation0()
                    })
                }
            },
            true
        )
}
