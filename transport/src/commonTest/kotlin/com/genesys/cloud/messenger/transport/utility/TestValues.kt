package com.genesys.cloud.messenger.transport.utility

import com.genesys.cloud.messenger.transport.core.ButtonResponse
import com.genesys.cloud.messenger.transport.core.Message
import com.genesys.cloud.messenger.transport.shyrka.receive.DeploymentConfig
import com.genesys.cloud.messenger.transport.shyrka.receive.StructuredMessage
import com.genesys.cloud.messenger.transport.shyrka.receive.StructuredMessage.Content.ButtonResponseContent
import com.genesys.cloud.messenger.transport.shyrka.receive.StructuredMessage.Content.QuickReplyContent

internal const val DEFAULT_TIMEOUT = 10000L

object TestValues {
    internal const val Domain: String = "inindca.com"
    internal const val DeploymentId = "deploymentId"
    internal const val MaxCustomDataBytes = 100
    internal const val DefaultNumber = 1
    internal const val Timestamp = "2022-08-22T19:24:26.704Z"
    internal const val Token = "<token>"
    internal const val ReconnectionTimeout = 5000L
    internal const val NoReconnectionAttempts = 0L
    internal const val VaultKey = "vault_key"
    internal const val TokenKey = "token_key"
    internal const val AuthRefreshTokenKey = "auth_refresh_token_key"
    internal const val LogTag = "TestLogTag"
    internal val defaultMap = mapOf("A" to "B")
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
    internal const val RetryAfter = 1
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
    internal const val Text = "Hello world!"
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
    internal const val FileName = "fileName.png"
    internal const val FileSize = 100
    internal const val FileMD5 = "file_md5"
    internal const val FileType = "png"
    internal const val MediaType = "png"
    internal const val AttachmentContentType = "Attachment"
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

object QuickReplyTestValues {
    internal const val Text_A = "text_a"
    internal const val Text_B = "text_b"
    internal const val Payload_A = "payload_a"
    internal const val Payload_B = "payload_b"
    internal const val QuickReply = "QuickReply"
    internal const val ButtonResponse = "ButtonResponse"

    internal val buttonResponse_a = ButtonResponse(
        text = Text_A,
        payload = Payload_A,
        type = QuickReply
    )

    internal val buttonResponse_b = ButtonResponse(
        text = Text_B,
        payload = Payload_B,
        type = QuickReply
    )

    internal fun createQuickReplyContentForTesting(
        text: String = Text_A,
        payload: String = Payload_A,
    ) = QuickReplyContent(
        contentType = StructuredMessage.Content.Type.QuickReply.name,
        quickReply = QuickReplyContent.QuickReply(
            text = text,
            payload = payload,
            action = "action"
        )
    )

    internal fun createButtonResponseContentForTesting(
        text: String = Text_A,
        payload: String = Payload_A,
    ) = ButtonResponseContent(
        contentType = StructuredMessage.Content.Type.ButtonResponse.name,
        buttonResponse = ButtonResponseContent.ButtonResponse(
            text = text,
            payload = payload,
            type = QuickReply,
        )
    )
}

object StructuredMessageValues {
    internal fun createStructuredMessageForTesting(
        id: String = MessageValues.Id,
        type: StructuredMessage.Type = StructuredMessage.Type.Text,
        direction: String = Message.Direction.Inbound.name,
        content: List<StructuredMessage.Content> = emptyList(),
    ) = StructuredMessage(
        id = id,
        type = type,
        direction = direction,
        content = content,
    )
}
