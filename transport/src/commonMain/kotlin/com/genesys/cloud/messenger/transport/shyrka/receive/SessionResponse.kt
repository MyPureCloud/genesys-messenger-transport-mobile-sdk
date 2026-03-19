package com.genesys.cloud.messenger.transport.shyrka.receive

import com.genesys.cloud.messenger.transport.core.MAX_CUSTOM_DATA_BYTES_UNSET
import kotlinx.serialization.Serializable

@Serializable
internal data class SessionResponse(
    val connected: Boolean,
    val newSession: Boolean = false,
    val readOnly: Boolean = false,
    val maxCustomDataBytes: Int = MAX_CUSTOM_DATA_BYTES_UNSET,
    val allowedMedia: AllowedMedia? = null,
    val blockedExtensions: List<String> = emptyList(),
    val clearedExistingSession: Boolean = false,
    val durationSeconds: Long? = null,
    val expirationDate: Long? = null,
)

@Serializable
internal data class AllowedMedia(
    val inbound: Inbound? = null,
)

@Serializable
internal data class Inbound(
    val fileTypes: List<FileType> = emptyList(),
    val maxFileSizeKB: Long? = null,
)

@Serializable
internal data class FileType(
    val type: String,
)
