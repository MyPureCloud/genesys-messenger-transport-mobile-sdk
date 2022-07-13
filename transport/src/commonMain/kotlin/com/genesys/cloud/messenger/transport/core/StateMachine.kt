package com.genesys.cloud.messenger.transport.core

internal interface StateMachine {
    var currentState: MessagingClient.State
    var stateListener: ((MessagingClient.State) -> Unit)?

    fun onConnectionOpened()
    fun onConnect()
    fun onReconnect()
    fun onSessionConfigured(connected: Boolean, newSession: Boolean)
    fun onClosing(code: Int, reason: String)
    fun onClosed(code: Int, reason: String)
    fun onError(code: ErrorCode, message: String?)
}
