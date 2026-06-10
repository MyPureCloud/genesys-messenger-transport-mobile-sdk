package com.genesys.cloud.messenger.transport.network

import com.genesys.cloud.messenger.transport.util.logs.Log
import com.genesys.cloud.messenger.transport.util.logs.LogMessages
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val TIMEOUT_INTERVAL_IN_SECONDS: Long = 5

internal actual class ReconnectionHandlerImpl actual constructor(
    reconnectionTimeoutInSeconds: Long,
    private val log: Log,
) : ReconnectionHandler {
    private var attempts = 0
    private var maxAttempts: Int = (reconnectionTimeoutInSeconds / TIMEOUT_INTERVAL_IN_SECONDS).toInt()
    private var reconnectJob: Job? = null
    private val dispatcher = CoroutineScope(Dispatchers.Default)

    actual override val shouldReconnect: Boolean
        get() = attempts < maxAttempts

    actual override fun reconnect(reconnectFun: () -> Unit) {
        if (!shouldReconnect) return
        resetDispatcher()
        reconnectJob =
            dispatcher.launch {
                log.i { LogMessages.tryingToReconnect(attempts, maxAttempts) }
                delay(TIMEOUT_INTERVAL_IN_SECONDS.toMilliseconds())
                attempts++
                reconnectFun()
            }
    }

    actual override fun clear() {
        attempts = 0
        resetDispatcher()
    }

    private fun resetDispatcher() {
        reconnectJob?.cancel()
        reconnectJob = null
    }
}

private fun Long.toMilliseconds(): Long = this * 1000
