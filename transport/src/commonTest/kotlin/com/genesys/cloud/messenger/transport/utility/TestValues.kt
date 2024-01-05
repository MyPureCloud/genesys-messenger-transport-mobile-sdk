package com.genesys.cloud.messenger.transport.utility

import com.genesys.cloud.messenger.transport.core.ErrorCode

internal const val DEFAULT_TIMEOUT = 10000L

object TestValues {
    internal const val Domain: String = "inindca.com"
    internal const val DeploymentId = "deploymentId"
    internal const val Timestamp = "2022-08-22T19:24:26.704Z"
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
    internal const val DownloadUrl = "https://downloadurl.png"
    internal const val PresignedHeaderKey = "x-amz-tagging"
    internal const val PresignedHeaderValue = "abc"
}

object LogMessages {
    internal const val Connect = "connect()"
    internal const val ConnectAuthenticated = "connectAuthenticatedSession()"
    internal const val ConfigureSession = """configureSession(token = 00000000-0000-0000-0000-000000000000, startNew: false)"""
    internal const val ConfigureAuthenticatedSession = """configureAuthenticatedSession(token = 00000000-0000-0000-0000-000000000000, startNew: false)"""
    internal const val ClearConversationHistory = "Clear conversation history."
    internal const val SendClearConversation = "sendClearConversation()"
    internal const val Autostart = "sendAutoStart()"
    internal const val HealthCheck = "sendHealthCheck()"
    internal const val Attach = "attach(fileName = test.png)"
    internal const val Detach = "detach(attachmentId = 88888888-8888-8888-8888-888888888888)"
    internal const val WillSendMessage = "Will send message"
    internal const val ForceClose = "Force close web socket."
    internal const val CloseSession = "closeSession()"
    internal const val Disconnect = "disconnect()"
    internal const val Typing = "indicateTyping()"
    internal const val TypingCoolDown = "Typing event can be sent only once every 5000 milliseconds."
    internal const val TypingDisabled = "typing indicator is disabled."
    internal const val HistoryFetched = "All history has been fetched."
    internal fun unhandledErrorCode(errorCode: ErrorCode, message: String) = "Unhandled ErrorCode: $errorCode with optional message: $message"
    internal fun unhandledWebSocketError(errorCode: ErrorCode) = "Unhandled WebSocket errorCode. ErrorCode: $errorCode"
    internal fun sendMessageWith(message: String = "Hello world", customAttributes: String = "{A=B}") = "sendMessage(text = $message, customAttributes = $customAttributes)"
    internal fun stateChangedFromTo(from: String, to: String) = "State changed from: $from, to: $to"
}
