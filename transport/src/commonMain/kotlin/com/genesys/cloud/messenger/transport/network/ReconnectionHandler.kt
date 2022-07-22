package com.genesys.cloud.messenger.transport.network

internal const val DELAY_DELTA_IN_MILLISECONDS = 5000L // 5 Seconds
internal const val MAX_DELAY_DELTA_IN_MILLISECONDS = 60000L // 1 Minute
internal const val MIN_DELAY_DELTA_IN_MILLISECONDS = 1000L // 1 Second

internal interface ReconnectionHandler {

    /**
     * Reconnects to the web socket every attempts * [DELAY_DELTA_IN_MILLISECONDS] milliseconds.
     */
    fun reconnect(reconnectFun: () -> Unit)

    /**
     * @return true if [ReconnectionHandler] has room for another reconnect attempt, false otherwise.
     */
    val shouldReconnect: Boolean

    /**
     * Cancel and reset any ongoing reconnection attempt.
     */
    fun clear()
}
