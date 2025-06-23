package com.genesys.cloud.messenger.transport.shyrka

import com.genesys.cloud.messenger.transport.shyrka.receive.AttachmentDeletedResponse
import com.genesys.cloud.messenger.transport.shyrka.receive.ConnectionClosedEvent
import com.genesys.cloud.messenger.transport.shyrka.receive.GenerateUrlError
import com.genesys.cloud.messenger.transport.shyrka.receive.JwtResponse
import com.genesys.cloud.messenger.transport.shyrka.receive.LogoutEvent
import com.genesys.cloud.messenger.transport.shyrka.receive.MessageClassName
import com.genesys.cloud.messenger.transport.shyrka.receive.PreIdentifiedWebMessagingMessage
import com.genesys.cloud.messenger.transport.shyrka.receive.PresignedUrlResponse
import com.genesys.cloud.messenger.transport.shyrka.receive.SessionClearedEvent
import com.genesys.cloud.messenger.transport.shyrka.receive.SessionExpiredEvent
import com.genesys.cloud.messenger.transport.shyrka.receive.SessionResponse
import com.genesys.cloud.messenger.transport.shyrka.receive.StructuredMessage
import com.genesys.cloud.messenger.transport.shyrka.receive.TooManyRequestsErrorMessage
import com.genesys.cloud.messenger.transport.shyrka.receive.UploadFailureEvent
import com.genesys.cloud.messenger.transport.shyrka.receive.UploadSuccessEvent
import com.genesys.cloud.messenger.transport.shyrka.receive.WebMessagingMessage
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

internal object WebMessagingJson {

    val json = Json {
        ignoreUnknownKeys = true
        useAlternativeNames = false
        classDiscriminator = "messageClass"
    }

    /**
     * Decodes and deserializes the given string to a type of WebMessagingMessage.
     *
     * @param string a serialized json representation of a received Web Messaging WebSocket message.
     * @return a WebMessagingMessage or null if string contains an unknown class name
     * @throws SerializationException thrown when decoding the string encounters an exception.
     */
    fun decodeFromString(string: String): WebMessagingMessage<*> {
        val preIdentified = json.decodeFromString<PreIdentifiedWebMessagingMessage>(string)
        return when (preIdentified.className) {
            MessageClassName.STRING_MESSAGE.value ->
                json.decodeFromString<WebMessagingMessage<String>>(string)
            MessageClassName.SESSION_RESPONSE.value ->
                json.decodeFromString<WebMessagingMessage<SessionResponse>>(string)
            MessageClassName.STRUCTURED_MESSAGE.value ->
                json.decodeFromString<WebMessagingMessage<StructuredMessage>>(string)
            MessageClassName.PRESIGNED_URL_RESPONSE.value ->
                json.decodeFromString<WebMessagingMessage<PresignedUrlResponse>>(string)
            MessageClassName.ATTACHMENT_DELETED_RESPONSE.value ->
                json.decodeFromString<WebMessagingMessage<AttachmentDeletedResponse>>(string)
            MessageClassName.UPLOAD_FAILURE_EVENT.value ->
                json.decodeFromString<WebMessagingMessage<UploadFailureEvent>>(string)
            MessageClassName.UPLOAD_SUCCESS_EVENT.value ->
                json.decodeFromString<WebMessagingMessage<UploadSuccessEvent>>(string)
            MessageClassName.JWT_RESPONSE.value ->
                json.decodeFromString<WebMessagingMessage<JwtResponse>>(string)
            MessageClassName.GENERATE_URL_ERROR.value ->
                json.decodeFromString<WebMessagingMessage<GenerateUrlError>>(string)
            MessageClassName.SESSION_EXPIRED_EVENT.value ->
                json.decodeFromString<WebMessagingMessage<SessionExpiredEvent>>(string)
            MessageClassName.TOO_MANY_REQUESTS_ERROR_MESSAGE.value ->
                json.decodeFromString<WebMessagingMessage<TooManyRequestsErrorMessage>>(string)
            MessageClassName.CONNECTION_CLOSED_EVENT.value ->
                json.decodeFromString<WebMessagingMessage<ConnectionClosedEvent>>(string)
            MessageClassName.LOGOUT_EVENT.value ->
                json.decodeFromString<WebMessagingMessage<LogoutEvent>>(string)
            MessageClassName.SESSION_CLEARED_EVENT.value ->
                json.decodeFromString<WebMessagingMessage<SessionClearedEvent>>(string)
            else -> throw IllegalArgumentException()
        }
    }
}
