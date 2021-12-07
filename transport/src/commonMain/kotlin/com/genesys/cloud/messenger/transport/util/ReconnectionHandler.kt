package com.genesys.cloud.messenger.transport.util

internal const val DEFAULT_MAX_ATTEMPTS = 30
internal const val DELAY_DELTA_IN_MILLISECONDS = 1500L

internal interface ReconnectionHandler {
    /**
     * Reconnects to the web socket every attempts * [DELAY_DELTA_IN_MILLISECONDS] milliseconds.
     */
    fun reconnect(reconnectFun: () -> Unit)

    /**
     * set amount of attempts to 0
     */
    fun resetAttempts()

    /**
     * @return true if [ReconnectionHandler] has room for another reconnect attempt, false otherwise.
     */
    fun shouldReconnect(): Boolean
}
