package com.genesys.cloud.messenger.transport.network

import com.genesys.cloud.messenger.transport.core.ErrorCode
import com.genesys.cloud.messenger.transport.core.ErrorMessage
import com.genesys.cloud.messenger.transport.util.Platform
import com.genesys.cloud.messenger.transport.util.logs.Log
import com.genesys.cloud.messenger.transport.util.logs.LogMessages
import com.genesys.cloud.messenger.transport.util.logs.okHttpLogger
import io.ktor.http.Url
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit

internal actual class PlatformSocket actual constructor(
    private val log: Log,
    private val url: Url,
    actual val pingInterval: Int,
    @Suppress("UNUSED_PARAMETER") private val forceTLSv13: Boolean,
) {
    private var webSocket: WebSocket? = null
    private var listener: PlatformSocketListener? = null

    actual fun openSocket(listener: PlatformSocketListener) {
        this.listener = listener
        val socketRequest =
            Request.Builder()
                .url(url.toString())
                .header(name = "Origin", value = url.host)
                .header(name = "User-Agent", Platform().platform)
                .build()
        val webClient = OkHttpClient()
            .newBuilder()
            .pingInterval(pingInterval.toLong(), TimeUnit.SECONDS)
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
                    when (response?.code) {
                        403 -> listener.onFailure(Throwable(response.message, t), ErrorCode.WebsocketAccessDenied)
                        else -> listener.onFailure(Throwable(ErrorMessage.FailedToReconnect, t))
                    }

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
        log.i { LogMessages.closeSocket(code, reason) }
        val shutdownInitiatedByThisCall = webSocket?.close(code, reason) ?: false
        if (!shutdownInitiatedByThisCall) { listener?.onClosed(code, reason) }
        webSocket = null
    }

    actual fun sendMessage(text: String) {
        log.i { LogMessages.sendMessage(text) }
        webSocket?.send(text)
    }
}
