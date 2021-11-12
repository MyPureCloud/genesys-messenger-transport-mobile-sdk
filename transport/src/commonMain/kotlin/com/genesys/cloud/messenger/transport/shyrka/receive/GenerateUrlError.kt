package com.genesys.cloud.messenger.transport.shyrka.receive

import kotlinx.serialization.Serializable

@Serializable
internal data class GenerateUrlError(
    val attachmentId: String,
    val errorCode: Int,
    val errorMessage: String,
)
