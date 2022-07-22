package com.genesys.cloud.messenger.transport.network

import com.genesys.cloud.messenger.transport.util.logs.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal class ReconnectionHandlerImpl(
    private val maxReconnectionAttempts: Int,
    private val log: Log,
) : ReconnectionHandler {
    private var attempts = 0
    private val dispatcher = CoroutineScope(Dispatchers.Default)
    private var reconnectJob: Job? = null

    override val shouldReconnect: Boolean
        get() = attempts < maxReconnectionAttempts


    override fun reconnect(reconnectFun: () -> Unit) {
        if (!shouldReconnect) return
        resetDispatcher()
        reconnectJob = dispatcher.launch {
            log.i { "Trying to reconnect. Attempts: $attempts." }
            delay(getDelay())
            attempts++
            reconnectFun()
        }
    }

    override fun clear() {
        attempts = 0
        resetDispatcher()
    }

    private fun resetDispatcher() {
        reconnectJob?.cancel()
        reconnectJob = null
    }

    private fun getDelay(): Long {
        val delay = attempts * DELAY_DELTA_IN_MILLISECONDS
        return when {
            delay <= 0 -> MIN_DELAY_DELTA_IN_MILLISECONDS
            delay < MAX_DELAY_DELTA_IN_MILLISECONDS -> delay
            else -> MAX_DELAY_DELTA_IN_MILLISECONDS
        }
    }
}
