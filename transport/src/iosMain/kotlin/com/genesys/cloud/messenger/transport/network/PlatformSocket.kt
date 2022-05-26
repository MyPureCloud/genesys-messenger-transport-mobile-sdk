package com.genesys.cloud.messenger.transport.network

import com.genesys.cloud.messenger.transport.core.Configuration
import com.genesys.cloud.messenger.transport.util.logs.Log
import platform.Foundation.HTTPMethod
import platform.Foundation.NSData
import platform.Foundation.NSHTTPURLResponse
import platform.Foundation.NSMutableURLRequest
import platform.Foundation.NSOperationQueue
import platform.Foundation.NSPOSIXErrorDomain
import platform.Foundation.NSTimer
import platform.Foundation.NSURL
import platform.Foundation.NSURLRequest
import platform.Foundation.NSURLSession
import platform.Foundation.NSURLSessionConfiguration
import platform.Foundation.NSURLSessionWebSocketCloseCode
import platform.Foundation.NSURLSessionWebSocketDelegateProtocol
import platform.Foundation.NSURLSessionWebSocketMessage
import platform.Foundation.NSURLSessionWebSocketTask
import platform.Foundation.allHTTPHeaderFields
import platform.Foundation.setValue
import platform.darwin.NSObject

internal actual class PlatformSocket actual constructor(
    private val log: Log,
    configuration: Configuration,
    actual val pingInterval: Long
) {
    private val url = configuration.webSocketUrl
    private val socketEndpoint = NSURL.URLWithString(url.toString())!!
    private var webSocket: NSURLSessionWebSocketTask? = null
    private var pingTimer: NSTimer? = null
    private var listener: PlatformSocketListener? = null

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
                    webSocketTask.currentRequest?.also { log(it) }
                    when (val response = webSocketTask.response) {
                        is NSHTTPURLResponse -> log(response)
                    }
                    listener.onOpen()
                }
                override fun URLSession(
                    session: NSURLSession,
                    webSocketTask: NSURLSessionWebSocketTask,
                    didCloseWithCode: NSURLSessionWebSocketCloseCode,
                    reason: NSData?
                ) {
                    listener.onClosed(didCloseWithCode.toInt(), reason.toString())
                }
            },
            delegateQueue = NSOperationQueue.currentQueue()
        )
        webSocket = urlSession.webSocketTaskWithRequest(urlRequest)
        listenMessages(listener)
        webSocket?.resume()
        schedulePings()
        this.listener = listener
    }

    private fun cleanup() {
        cancelPings()
        listener = null
        webSocket = null
    }

    private fun schedulePings() {
        if (pingTimer == null && pingInterval > 0) {
            pingTimer = NSTimer.scheduledTimerWithTimeInterval(
                interval = pingInterval / 1000.0,
                repeats = true
            ) {
                it?.let {
                    log.i { "sending ping" }
                    webSocket?.sendPingWithPongReceiveHandler { nsError ->
                        log.i { "received pong" }
                        if (nsError != null) {
                            log.e { nsError.description ?: "Unknown pong error" }
                        }
                    }
                }
            }
        }
    }

    private fun cancelPings() {
        pingTimer?.invalidate()
        pingTimer = null
    }

    private fun listenMessages(listener: PlatformSocketListener) {
        webSocket?.receiveMessageWithCompletionHandler { message, nsError ->
            when {
                nsError != null -> {
                    listener.onFailure(Throwable(nsError.description))
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
        webSocket?.cancelWithCloseCode(code.toLong(), null)
        // there are sometimes states, like if the app is backgrounded and resumed, where the socket may have already closed without invoking the didCloseWithCode delegate and future calls to cancel the socket don't invoke it either
        // so call the onClosed listener callback here?
        listener?.onClosed(code, reason)
        cleanup()
    }

    actual fun sendMessage(text: String) {
        log.i { "sendMessage(text = $text)" }
        val message = NSURLSessionWebSocketMessage(text)
        webSocket?.sendMessage(message) { err ->
            if (err != null) {
                log.e { "Failed to send message. Error: $err" }
            }
        }
    }

    private fun log(request: NSURLRequest) {
        log.i {
"""
--> HTTP ${request.HTTPMethod} ${request.URL?.absoluteString}
${buildHeaderLogLines(request.allHTTPHeaderFields)}
--> END ${request.HTTPMethod}
""".trim()
        }
    }

    private fun log(response: NSHTTPURLResponse) {
        log.i {
"""
<-- HTTP ${response.statusCode} ${response.URL?.absoluteString}
${buildHeaderLogLines(response.allHeaderFields)}
<-- END HTTP
""".trim()
        }
    }

    private fun buildHeaderLogLines(headers: Map<Any?, *>?) = headers
        ?.map { (key, value) -> "$key: $value" }
        ?.joinToString(separator = "\n")
}
