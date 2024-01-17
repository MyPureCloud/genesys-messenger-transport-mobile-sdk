package com.genesys.cloud.messenger.transport.utility

import com.genesys.cloud.messenger.transport.core.ButtonResponse
import com.genesys.cloud.messenger.transport.core.Message
import com.genesys.cloud.messenger.transport.shyrka.receive.StructuredMessage
import com.genesys.cloud.messenger.transport.shyrka.receive.StructuredMessage.Content.ButtonResponseContent
import com.genesys.cloud.messenger.transport.shyrka.receive.StructuredMessage.Content.QuickReplyContent

internal const val DEFAULT_TIMEOUT = 10000L

object TestValues {
    internal const val Domain: String = "inindca.com"
    internal const val DeploymentId = "deploymentId"
}

object MessageValues {
    internal const val Id = "test_message_id"
    internal const val Text = "some text."
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

object QuickReplyTestValues {
    internal val buttonResponse_a = ButtonResponse(
        text = "text_a",
        payload = "payload_a",
        type = "QuickReply"
    )

    internal val buttonResponse_b = ButtonResponse(
        text = "text_b",
        payload = "payload_b",
        type = "QuickReply"
    )
}

object StructuredMessageValues {
    internal const val Payload = "payload"
    internal const val QuickReply = "QuickReply"

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

    internal fun createQuickReplyContentForTesting(
        text: String = MessageValues.Text,
        payload: String = Payload,
    ) = QuickReplyContent(
        contentType = StructuredMessage.Content.Type.QuickReply.name,
        quickReply = QuickReplyContent.QuickReply(
            text = text,
            payload = payload,
            action = "action"
        )
    )

    internal fun createButtonResponseContentForTesting(
        text: String = MessageValues.Text,
        payload: String = Payload,
    ) = ButtonResponseContent (
        contentType = StructuredMessage.Content.Type.ButtonResponse.name,
        buttonResponse = ButtonResponseContent.ButtonResponse(
            text = text,
            payload = payload,
            type = QuickReply,
        )
    )
}
