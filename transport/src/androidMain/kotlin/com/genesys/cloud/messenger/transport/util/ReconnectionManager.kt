package com.genesys.cloud.messenger.transport.util

import com.genesys.cloud.messenger.transport.util.logs.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal actual class ReconnectionManager(
    private val maxReconnectionAttempts: Int,
    private val log: Log
) {
    private var attempts = 0
    private val dispatcher = CoroutineScope(Dispatchers.Main + SupervisorJob())

    /**
     * Reconnects to the web socket every [attempts] * [DELAY_DELTA_IN_MILLISECONDS] milliseconds.
     */
    actual fun reconnect(reconnectFun: () -> Unit) {
        dispatcher.launch {
            log.i { "Trying to reconnect. Attempts: $attempts" }
            delay(attempts * DELAY_DELTA_IN_MILLISECONDS)
            withContext(Dispatchers.Default) {
                attempts++
                reconnectFun()
            }
        }
    }

    actual fun resetAttempts() {
        attempts = 0
    }

    /**
     * @return true if [ReconnectionManager] has room for another reconnect attempt, false otherwise.
     */
    actual fun canReconnect(): Boolean = attempts < maxReconnectionAttempts
}
