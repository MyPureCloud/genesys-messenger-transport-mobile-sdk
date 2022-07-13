package com.genesys.cloud.messenger.transport.core

import com.genesys.cloud.messenger.transport.core.MessagingClient.State
import com.genesys.cloud.messenger.transport.util.logs.Log
import com.genesys.cloud.messenger.transport.util.logs.LogTag

internal class StateMachineImpl(val log: Log = Log(enableLogs = false, LogTag.STATE_MACHINE)) : StateMachine {

    override var currentState: State = State.Idle
        set(value) {
            if (field != value) {
                log.i { "State changed from: $field, to: $value" }
                field = value
                stateListener?.invoke(value)
            }
        }

    override var stateListener: ((State) -> Unit)? = null

    override fun onConnectionOpened() {
        currentState = if (isReconnecting()) State.Reconnecting else State.Connected
    }

    override fun onConnect() {
        currentState = if (isReconnecting()) State.Reconnecting else State.Connecting
    }

    override fun onReconnect() {
        currentState = State.Reconnecting
    }

    override fun onSessionConfigured(
        connected: Boolean,
        newSession: Boolean,
    ) {
        currentState = State.Configured(connected, newSession, isReconnecting())
    }

    override fun onClosing(code: Int, reason: String) {
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

internal fun StateMachine.isReconnecting(): Boolean = currentState is State.Reconnecting

internal fun StateMachine.canConnect(): Boolean =
    currentState is State.Closed || currentState is State.Idle || currentState is State.Error || currentState is State.Reconnecting

internal fun StateMachine.canConfigure(): Boolean = currentState is State.Connected || currentState is State.Reconnecting

internal fun StateMachine.canDisconnect(): Boolean =
    currentState !is State.Closed && currentState !is State.Idle && currentState !is State.Error
