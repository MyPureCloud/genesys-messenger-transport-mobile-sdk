package com.genesys.cloud.messenger.transport.network

import com.genesys.cloud.messenger.transport.core.Configuration
import com.genesys.cloud.messenger.transport.util.logs.Log

/**
 * Common WebSocket class
 *
 * @param log the logger
 * @param configuration the transport configuration
 * @param pingInterval the interval in milliseconds for sending ping frames until the connection fails or is closed. Pinging may help keep the connection from timing out. The default value of 0 disables pinging.
 */
internal expect class PlatformSocket(
    log: Log,
    configuration: Configuration,
    pingInterval: Long = 0
) {
    val pingInterval: Long

    fun openSocket(listener: PlatformSocketListener)
    fun closeSocket(code: Int, reason: String)
    fun sendMessage(text: String)
    fun sendPing()
}
