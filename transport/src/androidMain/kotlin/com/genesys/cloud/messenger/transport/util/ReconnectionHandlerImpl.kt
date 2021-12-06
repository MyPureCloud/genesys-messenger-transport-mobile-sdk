package com.genesys.cloud.messenger.transport.util

import com.genesys.cloud.messenger.transport.util.logs.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal class ReconnectionHandlerImpl(
    private val maxReconnectionAttempts: Int,
    private val log: Log
) : ReconnectionHandler {
    private var attempts = 0
    private val dispatcher = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun reconnect(reconnectFun: () -> Unit) {
        if(!shouldReconnect()) return
        dispatcher.launch {
            log.i { "Trying to reconnect. Attempts: $attempts" }
            delay(attempts * DELAY_DELTA_IN_MILLISECONDS)
            attempts++
            reconnectFun()
        }
    }

    override fun resetAttempts() {
        attempts = 0
    }

    override fun shouldReconnect(): Boolean = attempts < maxReconnectionAttempts
}
