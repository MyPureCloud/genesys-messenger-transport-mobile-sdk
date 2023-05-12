package com.genesys.cloud.messenger.transport.auth

import kotlinx.serialization.Serializable

@Serializable
data class AuthJwt(
    val jwt: String,
    val refreshToken: String? = null,
)
