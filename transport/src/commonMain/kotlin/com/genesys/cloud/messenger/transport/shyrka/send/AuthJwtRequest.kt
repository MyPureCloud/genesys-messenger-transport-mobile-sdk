package com.genesys.cloud.messenger.transport.shyrka.send

import kotlinx.serialization.Serializable

@Serializable
internal data class AuthJwtRequest(
    val deploymentId: String,
    val oauth: OAuth,
)

@Serializable
internal data class OAuth(
    val code: String,
    val redirectUri: String,
    val codeVerifier: String? = null,
)
