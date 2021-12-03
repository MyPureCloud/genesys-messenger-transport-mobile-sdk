package com.genesys.cloud.messenger.transport.util

internal const val DEFAULT_MAX_ATTEMPTS = 30
internal const val DELAY_DELTA_IN_MILLISECONDS = 1500L

internal expect class ReconnectionHandler {
    fun reconnect(reconnectFun: () -> Unit)

    fun resetAttempts()

    fun canReconnect(): Boolean
}
