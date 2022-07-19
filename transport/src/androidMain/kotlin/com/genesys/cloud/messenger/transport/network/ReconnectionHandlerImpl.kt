package com.genesys.cloud.messenger.transport.network

internal class ReconnectionHandlerImpl() : ReconnectionHandler {
    override fun reconnect(reconnectFun: () -> Unit) {
        TODO("Implement in the upcoming pr.")
    }

    override fun resetAttempts() {
        TODO("Implement in the upcoming pr.")
    }

    override fun shouldReconnect(): Boolean = false
}
