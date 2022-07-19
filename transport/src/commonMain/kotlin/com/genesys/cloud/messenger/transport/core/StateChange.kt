package com.genesys.cloud.messenger.transport.core

/**
 * Data class that represents state transition.
 *
 * @param oldState the previous MessagingClient state.
 * @param newState the new MessagingClient state.
 */
data class StateChange(
    val oldState: MessagingClient.State,
    val newState: MessagingClient.State,
)
