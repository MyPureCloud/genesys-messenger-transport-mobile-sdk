package com.genesys.cloud.messenger.transport.network

import com.genesys.cloud.messenger.transport.core.TlsVersion
import com.genesys.cloud.messenger.transport.util.logs.Log
import io.ktor.http.Url

const val DEFAULT_PING_INTERVAL_IN_SECONDS = 15

/**
 * Common WebSocket class
 *
 * @param log the logger
 * @param url the WS endpoint.
 * @param pingInterval the interval in seconds for sending ping frames until the connection fails or is closed. Pinging may help keep the connection from timing out. The default value of 0 disables pinging.
 * @param minimumTlsVersion the minimum TLS protocol version for secure connections (iOS only).
 */
internal expect class PlatformSocket(
    log: Log,
    url: Url,
    pingInterval: Int = DEFAULT_PING_INTERVAL_IN_SECONDS,
    minimumTlsVersion: TlsVersion = TlsVersion.SYSTEM_DEFAULT,
) {
    val pingInterval: Int

    fun openSocket(listener: PlatformSocketListener)

    fun closeSocket(
        code: Int,
        reason: String
    )

    fun sendMessage(text: String)
}
