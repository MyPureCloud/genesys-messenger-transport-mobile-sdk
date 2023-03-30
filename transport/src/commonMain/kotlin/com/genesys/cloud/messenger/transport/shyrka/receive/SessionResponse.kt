package com.genesys.cloud.messenger.transport.shyrka.receive

import kotlinx.serialization.Serializable

@Serializable
internal data class SessionResponse(
    val connected: Boolean,
    val newSession: Boolean = false,
    val readOnly: Boolean = false,
)
