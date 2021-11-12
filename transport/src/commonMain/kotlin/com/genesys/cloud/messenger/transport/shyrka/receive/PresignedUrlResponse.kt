package com.genesys.cloud.messenger.transport.shyrka.receive

import kotlinx.serialization.Serializable

@Serializable
internal data class PresignedUrlResponse(
    val attachmentId: String,
    val headers: Map<String, String>,
    val url: String,
    val fileName: String? = null,
    val fileSize: Int? = null,
    val fileType: String? = null
)
