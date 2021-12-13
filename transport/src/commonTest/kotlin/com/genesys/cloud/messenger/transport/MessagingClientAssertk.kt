package com.genesys.cloud.messenger.transport

import assertk.Assert
import assertk.assertions.isEqualTo
import assertk.assertions.prop
import com.genesys.cloud.messenger.transport.util.ErrorCode

fun Assert<MessagingClient>.currentState() = prop(MessagingClient::currentState)
fun Assert<MessagingClient>.isClosed(code: Int, reason: String) =
    currentState().isEqualTo(MessagingClient.State.Closed(code, reason))

fun Assert<MessagingClient>.isConnecting() =
    currentState().isEqualTo(MessagingClient.State.Connecting)

fun Assert<MessagingClient>.isConnected() =
    currentState().isEqualTo(MessagingClient.State.Connected)

fun Assert<MessagingClient>.isClosing(code: Int, reason: String) =
    currentState().isEqualTo(MessagingClient.State.Closing(code, reason))

fun Assert<MessagingClient>.isConfigured(connected: Boolean, newSession: Boolean?) =
    currentState().isEqualTo(MessagingClient.State.Configured(connected, newSession))

fun Assert<MessagingClient>.isReconnecting() =
    currentState().isEqualTo(MessagingClient.State.Reconnecting)

fun Assert<MessagingClient>.isError(errorCode: ErrorCode, message: String?) =
    currentState().isEqualTo(MessagingClient.State.Error(errorCode, message))
