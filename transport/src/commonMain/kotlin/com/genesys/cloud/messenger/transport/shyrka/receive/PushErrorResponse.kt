package com.genesys.cloud.messenger.transport.shyrka.receive

import kotlinx.serialization.Serializable

@Serializable
internal data class PushErrorResponse(
    val message: String,
    val code: String,
    val status: Int,
    val contextId: String,
    val details: List<String> = emptyList(),
)
