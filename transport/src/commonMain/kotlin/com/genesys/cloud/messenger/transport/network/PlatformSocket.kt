package com.genesys.cloud.messenger.transport.network

import com.genesys.cloud.messenger.transport.util.logs.Log
import io.ktor.http.Url

const val DEFAULT_PING_INTERVAL_IN_SECONDS = 15

/**
 * Common WebSocket class
 *
 * @param log the logger
 * @param url the WS endpoint.
 * @param pingInterval the interval in seconds for sending ping frames until the connection fails or is closed. Pinging may help keep the connection from timing out. The default value of 0 disables pinging.
 * @param forceTLSv13 indicates if TLS 1.3 should be forced for WebSocket connections (iOS only). Default is true.
 */
internal expect class PlatformSocket(
    log: Log,
    url: Url,
    pingInterval: Int = DEFAULT_PING_INTERVAL_IN_SECONDS,
    forceTLSv13: Boolean = true,
) {
    val pingInterval: Int

    fun openSocket(listener: PlatformSocketListener)
    fun closeSocket(code: Int, reason: String)
    fun sendMessage(text: String)
}
