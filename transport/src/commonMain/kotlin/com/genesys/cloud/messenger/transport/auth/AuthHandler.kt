package com.genesys.cloud.messenger.transport.auth

internal interface AuthHandler {
    fun authenticate(authCode: String, redirectUri: String, codeVerifier: String?)
}
