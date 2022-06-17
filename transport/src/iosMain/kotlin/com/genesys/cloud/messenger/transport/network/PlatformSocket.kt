package com.genesys.cloud.messenger.transport.network

import com.genesys.cloud.messenger.transport.core.Configuration
import com.genesys.cloud.messenger.transport.util.extensions.string
import com.genesys.cloud.messenger.transport.util.extensions.toNSData
import com.genesys.cloud.messenger.transport.util.logs.Log
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
    configuration: Configuration,
    /**
     * Interval to automatically send pings while active. If pong not received within `interval`,
     * client assumes connectivity is lost and will notify [PlatformSocketListener.onFailure].
     */
    actual val pingInterval: Long
) {
    private val url = configuration.webSocketUrl
    private val socketEndpoint = NSURL.URLWithString(url.toString())!!
    private var webSocket: NSURLSessionWebSocketTask? = null
    private var pingTimer: NSTimer? = null
    private var listener: PlatformSocketListener? = null
    private val active: Boolean
        get() = webSocket != null

    actual fun openSocket(listener: PlatformSocketListener) {
        val urlRequest = NSMutableURLRequest(socketEndpoint)
        urlRequest.setValue(url.host, forHTTPHeaderField = "Origin")
        val urlSession = NSURLSession.sessionWithConfiguration(
            configuration = NSURLSessionConfiguration.defaultSessionConfiguration(),
            delegate = object : NSObject(), NSURLSessionWebSocketDelegateProtocol {
                override fun URLSession(
                    session: NSURLSession,
                    webSocketTask: NSURLSessionWebSocketTask,
                    didOpenWithProtocol: String?
                ) {
                    log.i { "Socket did open." }
                    listener.onOpen()
                    keepAlive()
                }
                override fun URLSession(
                    session: NSURLSession,
                    webSocketTask: NSURLSessionWebSocketTask,
                    didCloseWithCode: NSURLSessionWebSocketCloseCode,
                    reason: NSData?
                ) {
                    val why = reason?.string() ?: ""
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

    private fun deactivate() {
        log.i { "deactivate" }
        cancelPings()
        webSocket = null
    }

    private fun handleErrorAndDeactivate(error: NSError, context: String? = null) {
        log.e { "${context ?: "NSError"}. [${error.code}] ${error.localizedDescription}" }
        if (active) {
            deactivate()
            listener?.onFailure(Throwable(error.localizedDescription))
        }
    }

    private var waitingOnPong = false

    private fun keepAlive() {
        val isTimerScheduled = pingTimer?.valid ?: false
        if (!isTimerScheduled && pingInterval > 0) {
            waitingOnPong = false
            pingTimer = NSTimer.scheduledTimerWithTimeInterval(
                interval = pingInterval / 1000.0,
                repeats = true
            ) {
                if (waitingOnPong) {
                    // Prior pong not received within pingInterval. Assume connectivity is lost.
                    val nsError = NSError(
                        domain = NSPOSIXErrorDomain,
                        code = ETIMEDOUT.convert(),
                        userInfo = null
                    )
                    handleErrorAndDeactivate(nsError, "Pong not received within interval [$pingInterval]")
                    return@scheduledTimerWithTimeInterval
                }

                log.i { "Sending ping" }
                waitingOnPong = true
                webSocket?.sendPingWithPongReceiveHandler { nsError ->
                    waitingOnPong = false
                    if (nsError != null) {
                        handleErrorAndDeactivate(nsError, "Received pong error")
                    } else {
                        log.i { "Received pong" }
                    }
                }
            }
        }
    }

    private fun cancelPings() {
        pingTimer?.invalidate()
        pingTimer = null
        waitingOnPong = false
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
}
