package com.genesys.cloud.messenger.transport.core

import assertk.Assert
import assertk.assertions.isEqualTo

fun Assert<MessagingClient.State>.isIdle() =
    this.isEqualTo(MessagingClient.State.Idle)

fun Assert<MessagingClient.State>.isConnecting() =
    this.isEqualTo(MessagingClient.State.Connecting)

fun Assert<MessagingClient.State>.isConnected() =
    this.isEqualTo(MessagingClient.State.Connected)

fun Assert<MessagingClient.State>.isConfigured(connected: Boolean, newSession: Boolean) =
    this.isEqualTo(MessagingClient.State.Configured(connected, newSession))

fun Assert<MessagingClient.State>.isReadOnly() =
    this.isEqualTo(MessagingClient.State.ReadOnly)

fun Assert<MessagingClient.State>.isReconnecting() =
    this.isEqualTo(MessagingClient.State.Reconnecting)

fun Assert<MessagingClient.State>.isClosing(code: Int, reason: String) =
    this.isEqualTo(MessagingClient.State.Closing(code, reason))

fun Assert<MessagingClient.State>.isClosed(code: Int, reason: String) =
    this.isEqualTo(MessagingClient.State.Closed(code, reason))

fun Assert<MessagingClient.State>.isError(code: ErrorCode, message: String?) =
    this.isEqualTo(MessagingClient.State.Error(code, message))
