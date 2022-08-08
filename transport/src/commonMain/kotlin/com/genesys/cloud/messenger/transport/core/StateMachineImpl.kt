package com.genesys.cloud.messenger.transport.core

import com.genesys.cloud.messenger.transport.core.MessagingClient.State
import com.genesys.cloud.messenger.transport.util.logs.Log
import com.genesys.cloud.messenger.transport.util.logs.LogTag

internal class StateMachineImpl(
    val log: Log = Log(enableLogs = false, LogTag.STATE_MACHINE),
) : StateMachine {

    override var currentState: State = State.Idle
        set(value) {
            if (field != value) {
                log.i { "State changed from: ${field::class.simpleName}, to: ${value::class.simpleName}" }
                val oldState = field
                field = value
                stateListener?.invoke(value)
                stateChangedListener?.invoke(StateChange(oldState, value))
            }
        }

    override var stateListener: ((State) -> Unit)? = null

    override var stateChangedListener: ((StateChange) -> Unit)? = null

    override fun onConnectionOpened() {
        currentState = if (currentState.isReconnecting()) State.Reconnecting else State.Connected
    }

    @Throws(IllegalStateException::class)
    override fun onConnect() {
        check(currentState.canConnect()) { "MessagingClient state must be Closed, Idle or Error" }
        currentState = if (currentState.isReconnecting()) State.Reconnecting else State.Connecting
    }

    override fun onReconnect() {
        currentState = State.Reconnecting
    }

    @Throws(IllegalStateException::class)
    override fun onConfiguring() {
        check(currentState.canConfigure()) { "WebMessaging client is not connected." }
    }

    override fun onSessionConfigured(
        connected: Boolean,
        newSession: Boolean,
    ) {
        currentState = State.Configured(connected, newSession)
    }

    @Throws(IllegalStateException::class)
    override fun onClosing(code: Int, reason: String) {
        check(currentState.canDisconnect()) { "MessagingClient state must not already be Closed, Idle or Error" }
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

@Throws(IllegalStateException::class)
internal fun StateMachine.checkIfConfigured() =
    check(currentState is State.Configured) { "WebMessaging client is not configured." }

internal fun StateMachine.isClosed(): Boolean = currentState is State.Closed

private fun State.canConnect(): Boolean =
    this is State.Closed || this is State.Idle || this is State.Error || this is State.Reconnecting

private fun State.canConfigure(): Boolean = this is State.Connected || this is State.Reconnecting

private fun State.isReconnecting(): Boolean = this is State.Reconnecting

private fun State.canDisconnect(): Boolean =
    this !is State.Closed && this !is State.Idle && this !is State.Error
