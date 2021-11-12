package com.genesys.cloud.messenger.transport.util

import com.genesys.cloud.messenger.transport.Configuration
import com.genesys.cloud.messenger.transport.util.logs.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit.MILLISECONDS

internal actual class PlatformSocket actual constructor(
    private val log: Log,
    configuration: Configuration,
    actual val pingInterval: Long
) {
    private val url = configuration.webSocketUrl
    private var webSocket: WebSocket? = null

    actual fun openSocket(listener: PlatformSocketListener) {
        val socketRequest =
            Request.Builder().url(url.toString()).header(name = "Origin", value = url.host).build()
        val webClient = OkHttpClient()
            .newBuilder()
            .pingInterval(pingInterval, MILLISECONDS)
            .addInterceptor(
                HttpLoggingInterceptor(logger = log.okHttpLogger()).apply {
                    level = HttpLoggingInterceptor.Level.BODY
                }
            )
            .build()
        webSocket = webClient.newWebSocket(
            socketRequest,
            object : okhttp3.WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) = listener.onOpen()
                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) =
                    listener.onFailure(t)

                override fun onMessage(webSocket: WebSocket, text: String) =
                    listener.onMessage(text)

                override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                    listener.onClosing(code, reason)
                    webSocket.close(code, reason)
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) =
                    listener.onClosed(code, reason)
            }
        )
    }

    actual fun closeSocket(code: Int, reason: String) {
        log.i { "closeSocket(code = $code, reason = $reason)" }
        webSocket?.close(code, reason)
        webSocket = null
    }

    actual fun sendMessage(text: String) {
        log.i { "sendMessage(text = $text)" }
        webSocket?.send(text)
    }
}
