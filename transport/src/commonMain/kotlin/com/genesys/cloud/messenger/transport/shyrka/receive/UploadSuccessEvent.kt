package com.genesys.cloud.messenger.transport.shyrka.receive

import kotlinx.serialization.Serializable

@Serializable
internal data class UploadSuccessEvent(
    val attachmentId: String,
    val downloadUrl: String,
    val timestamp: String
)
