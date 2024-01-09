package com.genesys.cloud.messenger.transport.utility

import com.genesys.cloud.messenger.transport.core.ErrorCode
import com.genesys.cloud.messenger.transport.core.events.Event
import com.genesys.cloud.messenger.transport.shyrka.receive.DeploymentConfig

internal const val DEFAULT_TIMEOUT = 10000L

object TestValues {
    internal const val Domain: String = "inindca.com"
    internal const val DeploymentId = "deploymentId"
    internal const val Timestamp = "2022-08-22T19:24:26.704Z"
    internal const val Token = "<token>"
}

object AuthTest {
    internal const val AuthCode = "auth_code"
    internal const val RedirectUri = "https://example.com/redirect"
    internal const val JwtAuthUrl = "https://example.com/auth"
    internal const val CodeVerifier = "code_verifier"
    internal const val JwtToken = "jwt_Token"
    internal const val RefreshToken = "refresh_token"
    internal const val RefreshedJWTToken = "jwt_token_that_was_refreshed"
    internal const val JwtExpiry = 100L
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
    internal const val CancellationException = "cancellation_exception"
    internal const val UnknownException = "unknown_exception"
}

object MessageValues {
    internal const val Id = "test_message_id"
    internal const val ParticipantName = "participant_name"
    internal const val ParticipantLastName = "participant_last_name"
    internal const val ParticipantNickname = "participant_nickname"
    internal const val ParticipantImageUrl = "http://participant.image"
    internal const val Text = "test_text"
    internal const val Type = "Text"
    internal const val TimeStamp = 1L
    internal const val PageSize = 25
    internal const val PageNumber = 1
    internal const val Total = 25
    internal const val PageCount = 1
    internal const val PreIdentifiedMessageType = "type"
    internal const val PreIdentifiedMessageCode = 200
    internal const val PreIdentifiedMessageClass = "clazz"
}

object AttachmentValues {
    internal const val Id = "test_attachment_id"
    internal const val DownloadUrl = "https://downloadurl.png"
    internal const val PresignedHeaderKey = "x-amz-tagging"
    internal const val PresignedHeaderValue = "abc"
    internal const val FileName = "fileName"
    internal const val FileSize = 100
    internal const val FileMD5 = "file_md5"
    internal const val FileType = "png"
}

object LogMessages {
    internal const val LogTag = "TestLogTag"
    internal const val Connect = "connect()"
    internal const val ConnectAuthenticated = "connectAuthenticatedSession()"
    internal const val ConfigureSession = """configureSession(token = 00000000-0000-0000-0000-000000000000, startNew: false)"""
    internal const val ConfigureAuthenticatedSession = """configureAuthenticatedSession(token = 00000000-0000-0000-0000-000000000000, startNew: false)"""
    internal const val ClearConversationHistory = "Clear conversation history."
    internal const val SendClearConversation = "sendClearConversation()"
    internal const val Autostart = "sendAutoStart()"
    internal const val HealthCheck = "sendHealthCheck()"
    internal const val HealthCheckWarning = "Health check can be sent only once every 30000 milliseconds."
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
    internal const val UnknownEvent = "Unknown event received."
    internal fun unhandledErrorCode(errorCode: ErrorCode, message: String) = "Unhandled ErrorCode: $errorCode with optional message: $message"
    internal fun unhandledWebSocketError(errorCode: ErrorCode) = "Unhandled WebSocket errorCode. ErrorCode: $errorCode"
    internal fun sendMessageWith(message: String = "Hello world", customAttributes: String = "{A=B}") = "sendMessage(text = $message, customAttributes = $customAttributes)"
    internal fun stateChangedFromTo(from: String, to: String) = "State changed from: $from, to: $to"
    internal fun onEvent(event: Event): String = "on event: $event"
}

object DeploymentConfigValues {
    internal const val ApiEndPoint = "api_endpoint"
    internal const val DefaultLanguage = "en-us"
    internal const val SecondaryLanguage = "zh-cn"
    internal const val Id = "id"
    internal const val MessagingEndpoint = "messaging_endpoint"
    internal const val PrimaryColor = "red"
    internal const val LauncherButtonVisibility = "On"
    internal const val MaxFileSize = 100L
    internal const val FileType = "png"
    internal val Status = DeploymentConfig.Status.Active
    internal const val Version = "1"
}

object Journey {
    internal const val CustomerId = "customer_id"
    internal const val CustomerIdType = "customer_id_type"
    internal const val CustomerSessionId = "customer_session_id"
    internal const val CustomerSessionType = "customer_session_type"
    internal const val ActionId = "action_id"
    internal const val ActionMapId = "action_map_id"
    internal const val ActionMapVersion = 1.0f
}
