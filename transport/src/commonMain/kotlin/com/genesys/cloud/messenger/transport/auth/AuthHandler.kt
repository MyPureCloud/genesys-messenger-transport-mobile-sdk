package com.genesys.cloud.messenger.transport.auth

internal interface AuthHandler {
    var authJwt: AuthJwt?

    fun authenticate(authCode: String, redirectUri: String, codeVerifier: String?)

    fun logout(authJwt: AuthJwt)

    fun refreshToken()
}
