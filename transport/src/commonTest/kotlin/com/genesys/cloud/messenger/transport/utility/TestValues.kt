package com.genesys.cloud.messenger.transport.utility

internal const val DEFAULT_TIMEOUT = 10000L

object TestValues {
    internal const val Domain: String = "inindca.com"
    internal const val DeploymentId = "deploymentId"
    internal const val MaxCustomDataBytes = 100
}

object AuthTest {
    internal const val AuthCode = "auth_code"
    internal const val RedirectUri = "https://example.com/redirect"
    internal const val JwtAuthUrl = "https://example.com/auth"
    internal const val CodeVerifier = "code_verifier"
    internal const val JwtToken = "jwt_Token"
    internal const val RefreshToken = "refresh_token"
    internal const val RefreshedJWTToken = "jwt_token_that_was_refreshed"
}

object ErrorTest {
    internal const val Message = "This is a generic error message for testing."
}

object InvalidValues {
    internal const val Domain = "invalid_domain"
    internal const val DeploymentId = "invalid_deploymentId"
    internal const val InvalidJwt = "invalid_jwt"
    internal const val UnauthorizedJwt = "unauthorized_jwt"
    internal const val InvalidRefreshToken = "invalid_refresh_token"
}
