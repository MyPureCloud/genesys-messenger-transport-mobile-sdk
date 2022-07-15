package com.genesys.cloud.messenger.transport.core

import com.genesys.cloud.messenger.transport.core.MessagingClient.State
import com.genesys.cloud.messenger.transport.util.logs.Log
import com.genesys.cloud.messenger.transport.util.logs.LogTag

internal class StateMachineImpl(
    val log: Log = Log(enableLogs = false, LogTag.STATE_MACHINE),
) : StateMachine {
    private val canConnect: Boolean by lazy { currentState is State.Closed || currentState is State.Idle || currentState is State.Error || currentState is State.Reconnecting }
    private val canConfigure: Boolean by lazy { currentState is State.Connected || currentState is State.Reconnecting }
    private val isReconnecting: Boolean by lazy { currentState is State.Reconnecting }
    private val canDisconnect: Boolean by lazy { currentState !is State.Closed && currentState !is State.Idle && currentState !is State.Error }

    override var currentState: State = State.Idle
        set(value) {
            if (field != value) {
                log.i { "State changed from: $field, to: $value" }
                stateListener?.invoke(value)
                onStateChanged?.invoke(StateChange(field, value))
                field = value
            }
        }

    override var stateListener: ((State) -> Unit)? = null

    override var onStateChanged: ((StateChange) -> Unit)? = null

    override fun onConnectionOpened() {
        currentState = if (isReconnecting) State.Reconnecting else State.Connected
    }

    @Throws(IllegalStateException::class)
    override fun onConnect() {
        check(canConnect) { "MessagingClient state must be Closed, Idle or Error" }
        currentState = if (isReconnecting) State.Reconnecting else State.Connecting
    }

    override fun onReconnect() {
        currentState = State.Reconnecting
    }

    @Throws(IllegalStateException::class)
    override fun onConfiguring() {
        check(canConfigure) { "WebMessaging client is not connected." }
    }

    override fun onSessionConfigured(
        connected: Boolean,
        newSession: Boolean,
    ) {
        currentState = State.Configured(connected, newSession)
    }

    @Throws(IllegalStateException::class)
    override fun onClosing(code: Int, reason: String) {
        check(canDisconnect) { "MessagingClient state must not already be Closed, Idle or Error" }
        currentState = State.Closing(code, reason)
    }

    override fun onClosed(code: Int, reason: String) {
        currentState = State.Closed(code, reason)
    }

    override fun onError(code: ErrorCode, message: String?) {
        currentState = State.Error(code, message)
    }
}

internal fun StateMachine.isConnected(): Boolean = currentState is State.Connected

internal fun StateMachine.isConfigured(): Boolean = currentState is State.Configured
