package com.genesys.cloud.messenger.transport.auth

import com.genesys.cloud.messenger.transport.network.Empty
import com.genesys.cloud.messenger.transport.network.Result

internal interface AuthHandler {
    var authJwt: AuthJwt?

    fun authenticate(authCode: String, redirectUri: String, codeVerifier: String?)

    fun logout()

    fun refreshToken(callback: (Result<Empty>) -> Unit)
}
