package com.genesys.cloud.messenger.transport.util

import cocoapods.jetfire.JFRWebSocket
import com.genesys.cloud.messenger.transport.Configuration
import com.genesys.cloud.messenger.transport.util.logs.Log
import platform.Foundation.NSData
import platform.Foundation.NSTimer
import platform.Foundation.NSURL

internal actual class PlatformSocket actual constructor(
    private val log: Log,
    configuration: Configuration,
    actual val pingInterval: Long
) {
    private val url = configuration.webSocketUrl
    private val socketEndpoint = NSURL.URLWithString(url.toString())!!
    private var socket: JFRWebSocket? = null
    private var pingTimer: NSTimer? = null

    actual fun openSocket(listener: PlatformSocketListener) {
        if (socket?.isConnected == true) {
            listener.onFailure(Throwable("Socket is already connected."))
            return
        }

        socket = JFRWebSocket(socketEndpoint, null)
        socket?.onConnect = {
            log.i { "onConnect()" }
            listener.onOpen()
        }
        socket?.onDisconnect = { nsError ->
            log.i { "onDisconnect(): $nsError" }
            when {
                nsError != null -> {
                    listener.onFailure(Throwable(nsError.description))
                }
            }
            val closeCode =
                if (nsError == null) SocketCloseCode.NORMAL_CLOSURE.value else SocketCloseCode.NO_STATUS_RECEIVED.value
            listener.onClosed(closeCode, "disconnected")
        }
        socket?.onText = { text ->
            text?.let { listener.onMessage(it) }
        }
        socket?.connect()
        schedulePings()
    }

    private fun schedulePings() {
        if (pingTimer == null && pingInterval > 0) {
            pingTimer = NSTimer.scheduledTimerWithTimeInterval(
                interval = pingInterval / 1000.0,
                repeats = true
            ) {
                it?.let {
                    log.i { "sending ping" }
                    socket?.writePing(NSData())
                }
            }
        }
    }

    private fun cancelPings() {
        pingTimer?.invalidate()
        pingTimer = null
    }

    actual fun closeSocket(code: Int, reason: String) {
        log.i { "closeSocket(code = $code, reason = $reason)" }
        cancelPings()
        socket?.disconnect()
        // onDisconnect doesn't get called on the Jetfire socket when we call disconnect, so explicitly invoke it
        socket?.onDisconnect?.let { it(null) }
        socket = null
    }

    actual fun sendMessage(text: String) {
        log.i { "sendMessage(text = $text)" }
        socket?.writeString(text)
    }
}
