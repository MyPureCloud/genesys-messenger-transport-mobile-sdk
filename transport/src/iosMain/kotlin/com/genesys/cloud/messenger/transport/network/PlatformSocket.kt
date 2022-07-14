package com.genesys.cloud.messenger.transport.network

import com.genesys.cloud.messenger.transport.util.extensions.string
import com.genesys.cloud.messenger.transport.util.extensions.toNSData
import com.genesys.cloud.messenger.transport.util.logs.Log
import io.ktor.http.Url
import kotlinx.cinterop.convert
import platform.Foundation.NSData
import platform.Foundation.NSError
import platform.Foundation.NSMutableURLRequest
import platform.Foundation.NSOperationQueue
import platform.Foundation.NSPOSIXErrorDomain
import platform.Foundation.NSTimer
import platform.Foundation.NSURL
import platform.Foundation.NSURLSession
import platform.Foundation.NSURLSessionConfiguration
import platform.Foundation.NSURLSessionWebSocketCloseCode
import platform.Foundation.NSURLSessionWebSocketDelegateProtocol
import platform.Foundation.NSURLSessionWebSocketMessage
import platform.Foundation.NSURLSessionWebSocketTask
import platform.Foundation.setValue
import platform.darwin.NSObject
import platform.posix.ETIMEDOUT

internal actual class PlatformSocket actual constructor(
    private val log: Log,
    private val url: Url,
    /**
     * Interval to automatically send pings while active.
     * Note that [pingInterval] should not be lower than [pongInterval]
     */
    actual val pingInterval: Int,
    /**
     * Interval to wait for successful pong after ping was sent. If pong not received within `interval`,
     * client assumes connectivity is lost and will notify [PlatformSocketListener.onFailure].
     */
    actual val pongInterval: Int,
) {
    private val socketEndpoint = NSURL.URLWithString(url.toString())!!
    private var webSocket: NSURLSessionWebSocketTask? = null
    private var pingTimer: NSTimer? = null
    private var pongTimer: NSTimer? = null
    private var listener: PlatformSocketListener? = null
    private val active: Boolean
        get() = webSocket != null
    private var pongReceived = false

    actual fun openSocket(listener: PlatformSocketListener) {
        val urlRequest = NSMutableURLRequest(socketEndpoint)
        urlRequest.setValue(url.host, forHTTPHeaderField = "Origin")
        val urlSession = NSURLSession.sessionWithConfiguration(
            configuration = NSURLSessionConfiguration.defaultSessionConfiguration(),
            delegate = object : NSObject(), NSURLSessionWebSocketDelegateProtocol {
                override fun URLSession(
                    session: NSURLSession,
                    webSocketTask: NSURLSessionWebSocketTask,
                    didOpenWithProtocol: String?,
                ) {
                    log.i { "Socket did open. Active: $active" }
                    if (active) {
                        listener.onOpen()
                        sendPing()
                        keepAlive()
                    }
                }

                override fun URLSession(
                    session: NSURLSession,
                    webSocketTask: NSURLSessionWebSocketTask,
                    didCloseWithCode: NSURLSessionWebSocketCloseCode,
                    reason: NSData?,
                ) {
                    val why = reason?.string() ?: "Reason not specified."
                    log.i { "Socket did close. code: $didCloseWithCode, reason: $why" }
                    deactivate()
                    listener.onClosed(code = didCloseWithCode.toInt(), reason = why)
                }
            },
            delegateQueue = NSOperationQueue.currentQueue()
        )
        webSocket = urlSession.webSocketTaskWithRequest(urlRequest)
        listenMessages(listener)
        webSocket?.resume()
        this.listener = listener
    }

    actual fun closeSocket(code: Int, reason: String) {
        log.i { "closeSocket(code = $code, reason = $reason)" }
        webSocket?.cancelWithCloseCode(code.toLong(), reason.toNSData())
        deactivate()
    }

    actual fun sendMessage(text: String) {
        log.i { "sendMessage(text = $text)" }
        val message = NSURLSessionWebSocketMessage(text)
        webSocket?.sendMessage(message) { nsError ->
            if (nsError != null) {
                handleErrorAndDeactivate(nsError, "Send message error")
            }
        }
    }

    private fun listenMessages(listener: PlatformSocketListener) {
        webSocket?.receiveMessageWithCompletionHandler { message, nsError ->
            when {
                nsError != null -> {
                    handleErrorAndDeactivate(nsError, "Receive handler error")
                    return@receiveMessageWithCompletionHandler
                }
                message != null -> {
                    message.string?.let { listener.onMessage(it) }
                }
            }
            listenMessages(listener)
        }
    }

    private fun keepAlive() {
        if (!pingTimer.isScheduled() && pingInterval > 0) {
            pingTimer = createActionableTimer(pingInterval, true) {
                sendPing()
            }
        }
    }

    private fun sendPing() {
        log.i { "Sending ping." }
        pongReceived = false
        schedulePong()
        webSocket?.sendPingWithPongReceiveHandler { nsError ->
            if (nsError != null) {
                handleErrorAndDeactivate(nsError, "Received pong error.")
            } else {
                log.i { "Received pong." }
                pongReceived = true
            }
        }
    }

    private fun schedulePong() {
        if (pingInterval <= pongInterval) {
            log.w { "Ping interval should NOT be lower than pong interval!" }
            return
        }
        if (!pongTimer.isScheduled() && pongInterval > 0) {
            log.i { "Waiting for pong." }
            createActionableTimer(pongInterval, false) {
                validatePongReceived()
            }
        }
    }

    private fun validatePongReceived() {
        if (!pongReceived) {
            // Prior pong not received within pingInterval. Assume connectivity is lost.
            val nsError = NSError(
                domain = NSPOSIXErrorDomain,
                code = ETIMEDOUT.convert(),
                userInfo = null
            )
            handleErrorAndDeactivate(
                nsError,
                "Pong not received within interval [$pingInterval] "
            )
        }
    }

    private fun handleErrorAndDeactivate(error: NSError, context: String? = null) {
        log.e { "${context ?: "NSError"}. [${error.code}] ${error.localizedDescription}" }
        if (active) {
            deactivate()
            listener?.onFailure(Throwable(error.localizedDescription))
            listener = null
        }
    }

    private fun deactivate() {
        log.i { "deactivate" }
        cancelPings()
        webSocket = null
    }

    private fun cancelPings() {
        pingTimer?.invalidate()
        pongTimer?.invalidate()
        pingTimer = null
        pongTimer = null
        pongReceived = false
    }

    private fun createActionableTimer(
        interval: Int,
        repeats: Boolean,
        action: () -> Unit,
    ): NSTimer {
        return NSTimer.scheduledTimerWithTimeInterval(
            interval = interval.toDouble(),
            repeats = repeats,
        ) {
            action()
        }
    }
}

internal fun NSTimer?.isScheduled(): Boolean {
    return this?.valid ?: false
}
