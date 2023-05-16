package com.genesys.cloud.messenger.transport.auth

import com.genesys.cloud.messenger.transport.network.Empty
import com.genesys.cloud.messenger.transport.network.Result

internal const val NO_JWT = "ZW1wdHlfand0"
internal const val NO_REFRESH_TOKEN = "Tk9fUkVGUkVTSF9UT0tFTg=="

internal interface AuthHandler {
    val jwt: String

    fun authenticate(authCode: String, redirectUri: String, codeVerifier: String?)

    fun logout()

    fun refreshToken(callback: (Result<Empty>) -> Unit)
}
