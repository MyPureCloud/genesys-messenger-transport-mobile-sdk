package com.genesys.cloud.messenger.transport.auth

import com.genesys.cloud.messenger.transport.core.Empty
import com.genesys.cloud.messenger.transport.core.Result

internal const val NO_JWT = "ZW1wdHlfand0"
internal const val NO_REFRESH_TOKEN = "Tk9fUkVGUkVTSF9UT0tFTg=="

internal interface AuthHandler {
    val jwt: String

    fun authorize(
        authCode: String,
        redirectUri: String,
        codeVerifier: String?
    )

    fun authorizeImplicit(
        idToken: String,
        nonce: String
    )

    fun logout()

    fun refreshToken(callback: (Result<Empty>) -> Unit)

    fun shouldAuthorize(callback: (Boolean) -> Unit)

    fun clear()
}
