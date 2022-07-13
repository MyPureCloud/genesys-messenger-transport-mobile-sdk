package com.genesys.cloud.messenger.transport.core

import assertk.Assert
import assertk.assertions.isEqualTo
import assertk.assertions.prop

fun Assert<MessagingClient>.currentState() = prop(MessagingClient::currentState)
fun Assert<MessagingClient>.isClosed(code: Int, reason: String) =
    currentState().isEqualTo(MessagingClient.State.Closed(code, reason))

fun Assert<MessagingClient>.isConnecting() =
    currentState().isEqualTo(MessagingClient.State.Connecting)

fun Assert<MessagingClient>.isConnected() =
    currentState().isEqualTo(MessagingClient.State.Connected)

fun Assert<MessagingClient>.isClosing(code: Int, reason: String) =
    currentState().isEqualTo(MessagingClient.State.Closing(code, reason))

fun Assert<MessagingClient>.isConfigured(connected: Boolean, newSession: Boolean, wasReconnecting: Boolean) =
    currentState().isEqualTo(MessagingClient.State.Configured(connected, newSession, wasReconnecting))

fun Assert<MessagingClient>.isError(code: ErrorCode, message: String?) =
    currentState().isEqualTo(MessagingClient.State.Error(code, message))
