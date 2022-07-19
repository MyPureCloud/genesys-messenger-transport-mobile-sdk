package com.genesys.cloud.messenger.transport.network

internal interface ReconnectionHandler {

    fun reconnect(reconnectFun: () -> Unit)

    fun resetAttempts()

    fun shouldReconnect(): Boolean
}
