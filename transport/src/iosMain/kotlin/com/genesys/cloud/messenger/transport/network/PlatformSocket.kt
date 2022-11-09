package com.genesys.cloud.messenger.transport.network

import com.genesys.cloud.messenger.transport.core.ErrorCode
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
import platform.Foundation.NSURLErrorBadServerResponse
import platform.Foundation.NSURLErrorNotConnectedToInternet
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
     * Interval to automatically send pings while active. If pong not received within `interval`,
     * client assumes connectivity is lost and will notify [PlatformSocketListener.onFailure].
     */
    actual val pingInterval: Int,
) {
    private val socketEndpoint = NSURL.URLWithString(url.toString())!!
    private var webSocket: NSURLSessionWebSocketTask? = null
    private var pingTimer: NSTimer? = null
    private var listener: PlatformSocketListener? = null
    private val active: Boolean
        get() = webSocket != null
    private var waitingOnPong = false

    actual fun openSocket(listener: PlatformSocketListener) {
        val urlRequest = NSMutableURLRequest(socketEndpoint)
        urlRequest.setValue(url.host, forHTTPHeaderField = "Origin")
        urlRequest.setTimeoutInterval(TIMEOUT_INTERVAL)
        val urlSession = NSURLSession.sessionWithConfiguration(
            configuration = NSURLSessionConfiguration.defaultSessionConfiguration(),
            delegate = object : NSObject(), NSURLSessionWebSocketDelegateProtocol {
                override fun URLSession(
                    session: NSURLSession,
                    webSocketTask: NSURLSessionWebSocketTask,
                    didOpenWithProtocol: String?,
                ) {
                    log.i { "Socket did open. Active: $active." }
                    if (webSocketTask == webSocket) {
                        keepAlive()
                        listener.onOpen()
                    }
                }
                override fun URLSession(
                    session: NSURLSession,
                    webSocketTask: NSURLSessionWebSocketTask,
                    didCloseWithCode: NSURLSessionWebSocketCloseCode,
                    reason: NSData?,
                ) {
                    val why = reason?.string() ?: "Reason not specified."
                    log.i { "Socket did close (code: $didCloseWithCode, reason: $why). Active: $active." }
                    if (webSocketTask == webSocket) {
                        deactivate()
                        listener.onClosed(code = didCloseWithCode.toInt(), reason = why)
                    }
                }
            },
            delegateQueue = NSOperationQueue.currentQueue()
        )
        webSocket = urlSession.webSocketTaskWithRequest(urlRequest)
        webSocket?.resume()
        this.listener = listener
        listenMessages(listener)
    }

    actual fun closeSocket(code: Int, reason: String) {
        log.i { "closeSocket(code = $code, reason = $reason)" }
        deactivateAndCancelWebSocket(code, reason)
        listener?.onClosed(code, reason)
    }

    actual fun sendMessage(text: String) {
        log.i { "sendMessage(text = $text)" }
        val message = NSURLSessionWebSocketMessage(text)
        webSocket?.sendMessage(message) { nsError ->
            if (nsError != null) {
                handleError(nsError, "Send message error")
            }
        }
    }

    private fun listenMessages(listener: PlatformSocketListener) {
        webSocket?.receiveMessageWithCompletionHandler { message, nsError ->
            when {
                nsError != null -> {
                    log.e { "receiveMessageWithCompletionHandler error [${nsError.code}] ${nsError.localizedDescription}" }
                    handleError(
                        nsError, "Receive handler error"
                    )
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
            waitingOnPong = false
            pingTimer = NSTimer.scheduledTimerWithTimeInterval(
                interval = pingInterval.toDouble(),
                repeats = true
            ) {
                if (waitingOnPong) {
                    // Prior pong not received within pingInterval. Assume connectivity is lost.
                    val nsError = NSError(
                        domain = NSPOSIXErrorDomain,
                        code = ETIMEDOUT.convert(),
                        userInfo = null
                    )
                    handleError(nsError, "Pong not received within interval [$pingInterval]")
                    return@scheduledTimerWithTimeInterval
                }
                sendPing()
            }
        }
    }

    private fun sendPing() {
        log.i { "Sending ping" }
        if (waitingOnPong) {
            log.w { "Trying to send ping while still waiting for pong." }
            return
        }
        waitingOnPong = true
        webSocket?.sendPingWithPongReceiveHandler { nsError ->
            waitingOnPong = false
            if (nsError != null) {
                handleError(nsError, "Received pong error")
            } else {
                log.i { "Received pong" }
            }
        }
    }

    private fun cancelPings() {
        pingTimer?.invalidate()
        pingTimer = null
        waitingOnPong = false
    }

    private fun deactivate() {
        log.i { "deactivate()" }
        cancelPings()
        webSocket = null
    }

    /**
     * Deactivates the webSocket connection per `deactivate()`.
     * Attempt to send a final close frame with the given code and reason without `listener.onClosed()` being called.
     */
    private fun deactivateAndCancelWebSocket(code: Int, reason: String?) {
        log.i { "deactivateWithCloseCode(code = $code, reason = $reason)" }
        val webSocketRef = webSocket
        deactivate()
        webSocketRef?.cancelWithCloseCode(code.toLong(), reason?.toNSData())
    }

    private fun handleError(error: NSError, context: String? = null) {
        log.e { "handleError (${context ?: "no context"}) [${error.code}] $error" }
        if (active) {
            deactivateAndCancelWebSocket(
                SocketCloseCode.GOING_AWAY.value,
                "Closing due to error code ${error.code}"
            )
            listener?.onFailure(
                Throwable("[${error.code}] ${error.localizedDescription}"),
                error.toTransportErrorCode()
            )
        }
    }
}

private fun NSError.toTransportErrorCode(): ErrorCode =
    when (this.code) {
        NSURLErrorNotConnectedToInternet -> ErrorCode.NetworkDisabled
        NSURLErrorBadServerResponse -> {
            if (this.userInfo.containsKey("_NSURLErrorWebSocketHandshakeFailureReasonKey")) {
                ErrorCode.WebsocketAccessDenied
            } else {
                ErrorCode.WebsocketError
            }
        }
        else -> ErrorCode.WebsocketError
    }

internal fun NSTimer?.isScheduled(): Boolean {
    return this?.valid ?: false
}
