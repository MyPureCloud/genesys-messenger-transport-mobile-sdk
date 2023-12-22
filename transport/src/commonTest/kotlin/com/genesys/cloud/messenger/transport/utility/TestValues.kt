package com.genesys.cloud.messenger.transport.utility

internal const val DEFAULT_TIMEOUT = 10000L

object TestValues {
    internal const val Domain: String = "inindca.com"
    internal const val DeploymentId = "deploymentId"
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

object MessageValues {
    internal const val Id = "test_message_id"
    internal const val ParticipantName = "participant_name"
    internal const val ParticipantImageUrl = "http://participant.image"
    internal const val Text = "test_text"
    internal const val Type = "Text"
    internal const val TimeStamp = 1L
}

object AttachmentValues {
    internal const val Id = "test_attachment_id"
}
