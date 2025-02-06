package com.genesys.cloud.messenger.transport.util

import com.genesys.cloud.messenger.transport.core.MessagingClient
import com.genesys.cloud.messenger.transport.core.StateChange

internal val fromIdleToConnecting =
    StateChange(oldState = MessagingClient.State.Idle, newState = MessagingClient.State.Connecting)

internal val fromClosedToConnecting =
    StateChange(
        oldState = MessagingClient.State.Closed(1000, "The user has closed the connection."),
        newState = MessagingClient.State.Connecting
    )

internal val fromConnectingToConnected =
    StateChange(
        oldState = MessagingClient.State.Connecting,
        newState = MessagingClient.State.Connected
    )

internal val fromConnectedToConfigured =
    StateChange(
        oldState = MessagingClient.State.Connected,
        newState = MessagingClient.State.Configured(connected = true, newSession = true)
    )

internal fun fromConnectedToError(errorState: MessagingClient.State) =
    StateChange(oldState = MessagingClient.State.Connected, newState = errorState)

internal fun fromConfiguredToError(errorState: MessagingClient.State) =
    StateChange(
        oldState = MessagingClient.State.Configured(connected = true, newSession = true),
        newState = errorState,
    )

internal fun fromErrorToConnecting(errorState: MessagingClient.State) =
    StateChange(oldState = errorState, newState = MessagingClient.State.Connecting)

internal fun fromReadOnlyToError(errorState: MessagingClient.State) =
    StateChange(
        oldState = MessagingClient.State.ReadOnly,
        newState = errorState,
    )

internal fun fromConfiguredToReadOnly() =
    StateChange(
        oldState = MessagingClient.State.Configured(connected = true, newSession = true),
        newState = MessagingClient.State.ReadOnly,
    )

internal val fromConnectedToReadOnly =
    StateChange(
        oldState = MessagingClient.State.Connected,
        newState = MessagingClient.State.ReadOnly,
    )

internal fun fromReconnectingToError(errorState: MessagingClient.State) =
    StateChange(oldState = MessagingClient.State.Reconnecting, newState = errorState)

internal fun fromConfiguredToReconnecting(): StateChange =
    StateChange(
        oldState = MessagingClient.State.Configured(connected = true, newSession = true),
        newState = MessagingClient.State.Reconnecting
    )

internal val fromReadOnlyToConfigured =
    StateChange(
        oldState = MessagingClient.State.ReadOnly,
        newState = MessagingClient.State.Configured(connected = true, newSession = true)
    )
