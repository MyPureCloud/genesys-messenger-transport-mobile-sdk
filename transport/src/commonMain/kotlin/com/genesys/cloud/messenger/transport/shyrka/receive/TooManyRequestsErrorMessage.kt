package com.genesys.cloud.messenger.transport.shyrka.receive

import kotlinx.serialization.Serializable

@Serializable
internal data class TooManyRequestsErrorMessage(
    val retryAfter: Int,
    val errorCode: Int,
    val errorMessage: String
)
