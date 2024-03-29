package com.genesys.cloud.messenger.transport.core

import com.genesys.cloud.messenger.transport.core.MessagingClient.State
import com.genesys.cloud.messenger.transport.util.logs.Log
import com.genesys.cloud.messenger.transport.util.logs.LogMessages
import com.genesys.cloud.messenger.transport.util.logs.LogTag

internal class StateMachineImpl(
    internal val log: Log = Log(enableLogs = false, LogTag.STATE_MACHINE),
) : StateMachine {

    override var currentState: State = State.Idle
        set(value) {
            if (field != value) {
                log.i { LogMessages.stateChanged(field, value) }
                val oldState = field
                field = value
                stateListener?.invoke(value)
                stateChangedListener?.invoke(StateChange(oldState, value))
            }
        }

    override var stateListener: ((State) -> Unit)? = null

    override var stateChangedListener: ((StateChange) -> Unit)? = null

    override fun onConnectionOpened() {
        currentState = if (isReconnecting()) State.Reconnecting else State.Connected
    }

    @Throws(IllegalStateException::class)
    override fun onConnect() {
        check(currentState.canConnect()) { "MessagingClient state must be Closed, Idle or Error, but was: $currentState" }
        currentState = if (isReconnecting()) State.Reconnecting else State.Connecting
    }

    override fun onReconnect() {
        currentState = State.Reconnecting
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

    override fun onReadOnly() {
        currentState = State.ReadOnly
    }
}

internal fun StateMachine.isConnected(): Boolean = currentState is State.Connected

internal fun StateMachine.isReadOnly(): Boolean = currentState is State.ReadOnly

internal fun StateMachine.isReconnecting(): Boolean = currentState is State.Reconnecting

@Throws(IllegalStateException::class)
internal fun StateMachine.checkIfConfigured() =
    check(currentState is State.Configured) { "MessagingClient is not Configured or in ReadOnly state." }

@Throws(IllegalStateException::class)
internal fun StateMachine.checkIfConfiguredOrReadOnly() =
    check(currentState is State.Configured || isReadOnly()) { "To perform this action MessagingClient must be either Configured or in ReadOnly state. " }

internal fun StateMachine.isInactive(): Boolean =
    currentState is State.Idle || currentState is State.Closing || currentState is State.Closed || currentState is State.Error

internal fun StateMachine.isClosing(): Boolean = currentState is State.Closing

@Throws(IllegalStateException::class)
internal fun StateMachine.checkIfCanStartANewChat() =
    check(isReadOnly()) { "MessagingClient is not in ReadOnly state." }

private fun State.canConnect(): Boolean =
    this is State.Closed || this is State.Idle || this is State.Error || this is State.Reconnecting

private fun State.canDisconnect(): Boolean =
    this !is State.Closed && this !is State.Idle && this !is State.Error
