package com.genesys.cloud.messenger.transport.shyrka.receive

import com.genesys.cloud.messenger.transport.core.MAX_CUSTOM_DATA_BYTES_UNSET
import kotlinx.serialization.Serializable

@Serializable
internal data class SessionResponse(
    val connected: Boolean,
    val newSession: Boolean = false,
    val readOnly: Boolean = false,
    val maxCustomDataBytes: Int = MAX_CUSTOM_DATA_BYTES_UNSET,
)
