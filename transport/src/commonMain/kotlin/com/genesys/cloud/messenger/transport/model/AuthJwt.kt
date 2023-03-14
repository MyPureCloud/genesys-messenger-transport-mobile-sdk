package com.genesys.cloud.messenger.transport.model

import kotlinx.serialization.Serializable

@Serializable
data class AuthJwt(
    val jwt: String,
    val refreshToken: String,
)
