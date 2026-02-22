package com.genesys.cloud.messenger.transport.network

import com.genesys.cloud.messenger.transport.core.ErrorCode
import com.genesys.cloud.messenger.transport.core.TlsVersion
import com.genesys.cloud.messenger.transport.util.Platform
import com.genesys.cloud.messenger.transport.util.extensions.string
import com.genesys.cloud.messenger.transport.util.extensions.toNSData
import com.genesys.cloud.messenger.transport.util.logs.Log
import com.genesys.cloud.messenger.transport.util.logs.LogMessages
import io.ktor.http.Url
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
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
import platform.Foundation.NSURLSessionTaskStateRunning
import platform.Foundation.NSURLSessionWebSocketCloseCode
import platform.Foundation.NSURLSessionWebSocketDelegateProtocol
import platform.Foundation.NSURLSessionWebSocketMessage
import platform.Foundation.NSURLSessionWebSocketTask
import platform.Foundation.setValue
import platform.Security.tls_protocol_version_TLSv12
import platform.Security.tls_protocol_version_TLSv13
import platform.darwin.NSObject
import platform.posix.ENOTCONN
import platform.posix.ETIMEDOUT

@OptIn(ExperimentalForeignApi::class)
internal actual class PlatformSocket actual constructor(
    private val log: Log,
    private val url: Url,
    /**
     * Interval to automatically send pings while active. If pong not received within `interval`,
     * client assumes connectivity is lost and will notify [PlatformSocketListener.onFailure].
     */
    actual val pingInterval: Int,
    private val minimumTlsVersion: TlsVersion,
) {
    private val socketEndpoint = NSURL.URLWithString(url.toString())!!
    private var webSocket: NSURLSessionWebSocketTask? = null
    private var pingTimer: NSTimer? = null
    private var listener: PlatformSocketListener? = null
    private val active: Boolean
        get() = webSocket != null
    private var waitingOnPong = false

    @OptIn(BetaInteropApi::class)
    actual fun openSocket(listener: PlatformSocketListener) {
        val urlRequest = NSMutableURLRequest(socketEndpoint)
        urlRequest.setValue(url.host, forHTTPHeaderField = "Origin")
        urlRequest.setValue(Platform().platform, forHTTPHeaderField = "User-Agent")
        urlRequest.setTimeoutInterval(TIMEOUT_INTERVAL)
        val sessionConfig = NSURLSessionConfiguration.defaultSessionConfiguration()
        when (minimumTlsVersion) {
            TlsVersion.SYSTEM_DEFAULT -> {}
            TlsVersion.TLS_1_2 -> {
                log.i { "Configuring minimum TLS version: TLS 1.2" }
                sessionConfig.TLSMinimumSupportedProtocolVersion = tls_protocol_version_TLSv12
            }
            TlsVersion.TLS_1_3 -> {
                log.i { "Configuring minimum TLS version: TLS 1.3" }
                sessionConfig.TLSMinimumSupportedProtocolVersion = tls_protocol_version_TLSv13
            }
        }
        val urlSession =
            NSURLSession.sessionWithConfiguration(
                configuration = sessionConfig,
                delegate =
                    object : NSObject(), NSURLSessionWebSocketDelegateProtocol {
                        override fun URLSession(
                            session: NSURLSession,
                            webSocketTask: NSURLSessionWebSocketTask,
                            didOpenWithProtocol: String?,
                        ) {
                            log.i { LogMessages.socketDidOpen(active) }
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
                            log.i { LogMessages.socketDidClose(didCloseWithCode, why, active) }
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

    actual fun closeSocket(
        code: Int,
        reason: String
    ) {
        log.i { LogMessages.closeSocket(code, reason) }
        deactivateAndCancelWebSocket(code, reason)
        listener?.onClosed(code, reason)
    }

    actual fun sendMessage(text: String) {
        log.i { LogMessages.sendMessage(text) }
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
                    log.e { LogMessages.receiveMessageError(nsError.code, nsError.localizedDescription) }
                    handleError(
                        nsError,
                        "Receive handler error"
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
            pingTimer =
                NSTimer.scheduledTimerWithTimeInterval(
                    interval = pingInterval.toDouble(),
                    repeats = true
                ) {
                    if (waitingOnPong) {
                        // Prior pong not received within pingInterval. Assume connectivity is lost.
                        val nsError =
                            NSError(
                                domain = NSPOSIXErrorDomain,
                                code = ETIMEDOUT.convert(),
                                userInfo = null
                            )
                        handleError(nsError, "Pong not received within interval [$pingInterval]")
                        return@scheduledTimerWithTimeInterval
                    }

                    waitingOnPong = true
                    log.i { LogMessages.SENDING_PING }
                    sendPing { nsError ->
                        if (nsError != null) {
                            handleError(nsError, "Pong handler failure")
                            return@sendPing
                        }
                        waitingOnPong = false
                        log.i { LogMessages.RECEIVED_PONG }
                    }
                }
        }
    }

    private fun sendPing(pongHandler: (NSError?) -> Unit) {
        if (webSocket?.state != NSURLSessionTaskStateRunning) {
            pongHandler(
                NSError(
                    domain = NSPOSIXErrorDomain,
                    code = ENOTCONN.convert(),
                    userInfo = null
                )
            )
            return
        }
        webSocket?.sendPingWithPongReceiveHandler(pongHandler)
    }

    private fun cancelPings() {
        pingTimer?.invalidate()
        pingTimer = null
        waitingOnPong = false
    }

    private fun deactivate() {
        log.i { LogMessages.DEACTIVATE }
        cancelPings()
        webSocket = null
    }

    /**
     * Deactivates the webSocket connection per `deactivate()`.
     * Attempt to send a final close frame with the given code and reason without `listener.onClosed()` being called.
     */
    @OptIn(BetaInteropApi::class)
    private fun deactivateAndCancelWebSocket(
        code: Int,
        reason: String?
    ) {
        log.i { LogMessages.deactivateWithCloseCode(code, reason) }
        val webSocketRef = webSocket
        deactivate()
        webSocketRef?.cancelWithCloseCode(code.toLong(), reason?.toNSData())
    }

    private fun handleError(
        error: NSError,
        context: String? = null
    ) {
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

private fun NSTimer?.isScheduled(): Boolean {
    return this?.valid ?: false
}
