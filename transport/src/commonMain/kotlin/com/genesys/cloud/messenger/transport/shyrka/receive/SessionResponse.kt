package com.genesys.cloud.messenger.transport.shyrka.receive

import kotlinx.serialization.Serializable

@Serializable
internal data class SessionResponse(
    val connected: Boolean,
    val newSession: Boolean = false,
    val readOnly: Boolean = false,
    val allowedMedia: AllowedMedia? = null,
    val blockedExtensions: List<String> = emptyList()
)

@Serializable
internal data class AllowedMedia(
    val inbound: Inbound,
)

@Serializable
internal data class Inbound(
    val fileTypes: List<FileType>,
    val maxFileSizeKB: Long,
)

@Serializable
internal data class FileType(
    val type: String,
)