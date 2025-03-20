package com.genesys.cloud.messenger.transport.utility

import com.genesys.cloud.messenger.transport.core.ButtonResponse
import com.genesys.cloud.messenger.transport.core.ErrorCode
import com.genesys.cloud.messenger.transport.core.Message
import com.genesys.cloud.messenger.transport.push.DeviceTokenException
import com.genesys.cloud.messenger.transport.push.PushConfig
import com.genesys.cloud.messenger.transport.push.PushProvider
import com.genesys.cloud.messenger.transport.shyrka.receive.DeploymentConfig
import com.genesys.cloud.messenger.transport.shyrka.receive.PushErrorResponse
import com.genesys.cloud.messenger.transport.shyrka.receive.StructuredMessage
import com.genesys.cloud.messenger.transport.shyrka.receive.StructuredMessage.Content.ButtonResponseContent
import com.genesys.cloud.messenger.transport.shyrka.receive.StructuredMessage.Content.QuickReplyContent
import com.genesys.cloud.messenger.transport.util.PUSH_CONFIG_KEY
import com.genesys.cloud.messenger.transport.util.Vault

internal const val DEFAULT_TIMEOUT = 10000L

object TestValues {
    internal const val DOMAIN: String = "inindca.com"
    internal const val DEPLOYMENT_ID = "deploymentId"
    internal const val MAX_CUSTOM_DATA_BYTES = 100
    internal const val DEFAULT_NUMBER = 1
    internal const val TIME_STAMP = "2022-08-22T19:24:26.704Z"
    internal const val TOKEN = "token"
    internal const val SECONDARY_TOKEN = "<secondary_token>"
    internal const val RECONNECTION_TIMEOUT = 5000L
    internal const val NO_RECONNECTION_ATTEMPTS = 0L
    internal const val VAULT_KEY = "vault_key"
    internal const val TOKEN_KEY = "token_key"
    internal const val AUTH_REFRESH_TOKEN_KEY = "auth_refresh_token_key"
    internal const val WAS_AUTHENTICATED = "was_authenticated"
    internal const val LOG_TAG = "TestLogTag"
    internal val defaultMap = mapOf("A" to "B")
    internal const val DEFAULT_STRING = "any string"
    internal const val DEVICE_TOKEN = "<device_token>"
    internal const val PUSH_SYNC_TIMESTAMP = 10000000L
    internal val PUSH_PROVIDER = PushProvider.APNS
    internal const val DEVICE_TYPE = "android"
    internal const val PREFERRED_LANGUAGE = "Eng"
    internal val vaultKeys = Vault.Keys(
        vaultKey = VAULT_KEY,
        tokenKey = TOKEN_KEY,
        authRefreshTokenKey = AUTH_REFRESH_TOKEN_KEY,
        wasAuthenticated = WAS_AUTHENTICATED,
        pushConfigKey = PUSH_CONFIG_KEY,
    )
}

object AuthTest {
    internal const val AUTH_CODE = "auth_code"
    internal const val REDIRECT_URI = "https://example.com/redirect"
    internal const val JWT_AUTH_URL = "https://example.com/auth"
    internal const val CODE_VERIFIER = "code_verifier"
    internal const val JWT_TOKEN = "jwt_Token"
    internal const val REFRESH_TOKEN = "refresh_token"
    internal const val REFRESHED_JWT_TOKEN = "jwt_token_that_was_refreshed"
    internal const val JWT_EXPIRY = 100L
}

object ErrorTest {
    internal const val MESSAGE = "This is a generic error message for testing."
    internal const val RETRY_AFTER = 1
    internal const val CODE_404 = 404L
}

object InvalidValues {
    internal const val DOMAIN = "invalid_domain"
    internal const val DEPLOYMENT_ID = "invalid_deploymentId"
    internal const val INVALID_JWT = "invalid_jwt"
    internal const val UNAUTHORIZED_JWT = "unauthorized_jwt"
    internal const val INVALID_REFRESH_TOKEN = "invalid_refresh_token"
    internal const val CANCELLATION_EXCEPTION = "cancellation_exception"
    internal const val UNKNOWN_EXCEPTION = "unknown_exception"
}

object MessageValues {
    internal const val ID = "test_message_id"
    internal const val PARTICIPANT_NAME = "participant_name"
    internal const val PARTICIPANT_LAST_NAME = "participant_last_name"
    internal const val PARTICIPANT_NICKNAME = "participant_nickname"
    internal const val PARTICIPANT_IMAGE_URL = "http://participant.image"
    internal const val TEXT = "Hello world!"
    internal const val TYPE = "Text"
    internal const val TIME_STAMP = 1L
    internal const val PAGE_SIZE = 25
    internal const val PAGE_NUMBER = 1
    internal const val TOTAL = 25
    internal const val PAGE_COUNT = 1
    internal const val PRE_IDENTIFIED_MESSAGE_TYPE = "type"
    internal const val PRE_IDENTIFIED_MESSAGE_CODE = 200
    internal const val PRE_IDENTIFIED_MESSAGE_CLASS = "clazz"
}

object AttachmentValues {
    internal const val ID = "test_attachment_id"
    internal const val DOWNLOAD_URL = "https://downloadurl.png"
    internal const val PRESIGNED_HEADER_KEY = "x-amz-tagging"
    internal const val PRESIGNED_HEADER_VALUE = "abc"
    internal const val FILE_NAME = "fileName.png"
    internal const val FILE_SIZE = 100
    internal const val FILE_MD5 = "file_md5"
    internal const val FILE_TYPE = "png"
    internal const val MEDIA_TYPE = "png"
    internal const val ATTACHMENT_CONTENT_TYPE = "Attachment"
    internal const val TXT_FILE_NAME = "fileName.txt"
}

object DeploymentConfigValues {
    internal const val API_ENDPOINT = "api_endpoint"
    internal const val DEFAULT_LANGUAGE = "en-us"
    internal const val SECONDARY_LANGUAGE = "zh-cn"
    internal const val ID = "id"
    internal const val MESSAGING_ENDPOINT = "messaging_endpoint"
    internal const val PRIMARY_COLOR = "red"
    internal const val LAUNCHER_BUTTON_VISIBILITY = "On"
    internal const val MAX_FILE_SIZE = 100L
    internal const val FILE_TYPE = "png"
    internal val Status = DeploymentConfig.Status.Active
    internal const val VERSION = "1"
}

object Journey {
    internal const val CUSTOMER_ID = "customer_id"
    internal const val CUSTOMER_ID_TYPE = "customer_id_type"
    internal const val CUSTOMER_SESSION_ID = "customer_session_id"
    internal const val CUSTOMER_SESSION_TYPE = "customer_session_type"
    internal const val ACTION_ID = "action_id"
    internal const val ACTION_MAP_ID = "action_map_id"
    internal const val ACTION_MAP_VERSION = 1.0f
}

object QuickReplyTestValues {
    internal const val TEXT_A = "text_a"
    internal const val TEXT_B = "text_b"
    internal const val PAYLOAD_A = "payload_a"
    internal const val PAYLOAD_B = "payload_b"
    internal const val QUICK_REPLY = "QuickReply"
    internal const val BUTTON_RESPONSE = "ButtonResponse"

    internal val buttonResponse_a = ButtonResponse(
        text = TEXT_A,
        payload = PAYLOAD_A,
        type = QUICK_REPLY
    )

    internal val buttonResponse_b = ButtonResponse(
        text = TEXT_B,
        payload = PAYLOAD_B,
        type = QUICK_REPLY
    )

    internal fun createQuickReplyContentForTesting(
        text: String = TEXT_A,
        payload: String = PAYLOAD_A,
    ) = QuickReplyContent(
        contentType = StructuredMessage.Content.Type.QuickReply.name,
        quickReply = QuickReplyContent.QuickReply(
            text = text,
            payload = payload,
            action = "action"
        )
    )

    internal fun createButtonResponseContentForTesting(
        text: String = TEXT_A,
        payload: String = PAYLOAD_A,
    ) = ButtonResponseContent(
        contentType = StructuredMessage.Content.Type.ButtonResponse.name,
        buttonResponse = ButtonResponseContent.ButtonResponse(
            text = text,
            payload = payload,
            type = QUICK_REPLY,
        )
    )
}

object StructuredMessageValues {
    internal fun createStructuredMessageForTesting(
        id: String = MessageValues.ID,
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

object PushTestValues {
    internal val CONFIG = PushConfig(
        token = TestValues.TOKEN,
        deviceToken = TestValues.DEVICE_TOKEN,
        preferredLanguage = TestValues.PREFERRED_LANGUAGE,
        lastSyncTimestamp = TestValues.PUSH_SYNC_TIMESTAMP,
        deviceType = TestValues.DEVICE_TYPE,
        pushProvider = TestValues.PUSH_PROVIDER,
    )

    internal fun pushErrorResponseWith(code: String) = PushErrorResponse(
        message = ErrorTest.MESSAGE,
        code = code,
        status = ErrorTest.CODE_404.toInt(),
        contextId = TestValues.DEFAULT_STRING,
    )

    internal val DEVICE_TOKEN_EXCEPTION = DeviceTokenException(
        errorCode = ErrorCode.DeviceTokenOperationFailure,
        message = TestValues.DEFAULT_STRING,
        cause = null,
    )
    const val PUSH_CODE_DEPLOYMENT_NOT_FOUND = "deployment.not.found"
    const val PUSH_CODE_DEVICE_REGISTRATION_FAILURE = "device.registration.failure"
    const val PUSH_CODE_DEVICE_UPDATE_FAILURE = "device.update.failure"
    const val PUSH_CODE_DEVICE_DELETE_FAILURE = "device.delete.failure"
}

object MockEngineValues {
    const val NO_CONTENT = "No Content"
    const val CONTENT_TYPE_JSON = "application/json"
}
