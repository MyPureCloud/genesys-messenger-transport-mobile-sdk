package com.genesys.cloud.messenger.transport.network

internal interface ReconnectionHandler {
    /**
     * Attempt to reconnect to the web socket.
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
