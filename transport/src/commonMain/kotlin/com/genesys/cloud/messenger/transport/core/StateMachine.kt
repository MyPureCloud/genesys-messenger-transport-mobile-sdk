package com.genesys.cloud.messenger.transport.core

internal interface StateMachine {
    var currentState: MessagingClient.State
    var stateListener: ((MessagingClient.State) -> Unit)?
    var onStateChanged: ((StateChange) -> Unit)?

    fun onConnectionOpened()
    @Throws(IllegalStateException::class)
    fun onConnect()
    fun onReconnect()
    @Throws(IllegalStateException::class)
    fun onConfiguring()
    fun onSessionConfigured(connected: Boolean, newSession: Boolean)
    @Throws(IllegalStateException::class)
    fun onClosing(code: Int, reason: String)
    fun onClosed(code: Int, reason: String)
    fun onError(code: ErrorCode, message: String?)
}
