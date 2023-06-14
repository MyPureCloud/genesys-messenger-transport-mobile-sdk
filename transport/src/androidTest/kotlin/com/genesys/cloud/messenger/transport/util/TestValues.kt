package com.genesys.cloud.messenger.transport.util

internal const val TestDeploymentId = "validDeploymentId"

object AuthTest {
    internal const val Code = "validAuthCode"
    internal const val RedirectUri = "https://example.com/redirect"
    internal const val JwtAuthUrl = "https://example.com/auth"
    internal const val CodeVerifier = "validCodeVerifier"
    internal const val JwtToken = "validJwtToken"
    internal const val RefreshToken = "validRefreshToken"
    internal const val RefreshedJWTToken = "validJwtTokenThatWasRefreshed"
}

object ErrorTest {
    internal const val Message = "This is a generic error message for testing."
}
