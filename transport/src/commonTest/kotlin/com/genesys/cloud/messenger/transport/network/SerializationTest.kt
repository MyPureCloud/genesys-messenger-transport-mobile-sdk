package com.genesys.cloud.messenger.transport.network

import assertk.assertThat
import assertk.assertions.hasClass
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import com.genesys.cloud.messenger.transport.core.Attachment
import com.genesys.cloud.messenger.transport.core.Message
import com.genesys.cloud.messenger.transport.shyrka.WebMessagingJson
import com.genesys.cloud.messenger.transport.shyrka.receive.GenerateUrlError
import com.genesys.cloud.messenger.transport.shyrka.receive.JwtResponse
import com.genesys.cloud.messenger.transport.shyrka.receive.MessageType
import com.genesys.cloud.messenger.transport.shyrka.receive.PresignedUrlResponse
import com.genesys.cloud.messenger.transport.shyrka.receive.SessionExpiredEvent
import com.genesys.cloud.messenger.transport.shyrka.receive.SessionResponse
import com.genesys.cloud.messenger.transport.shyrka.receive.StructuredMessage
import com.genesys.cloud.messenger.transport.shyrka.receive.TooManyRequestsErrorMessage
import com.genesys.cloud.messenger.transport.shyrka.receive.UploadFailureEvent
import com.genesys.cloud.messenger.transport.shyrka.receive.UploadSuccessEvent
import com.genesys.cloud.messenger.transport.shyrka.receive.WebMessagingMessage
import com.genesys.cloud.messenger.transport.shyrka.send.ConfigureSessionRequest
import com.genesys.cloud.messenger.transport.shyrka.send.EchoRequest
import com.genesys.cloud.messenger.transport.shyrka.send.JourneyContext
import com.genesys.cloud.messenger.transport.shyrka.send.JourneyCustomer
import com.genesys.cloud.messenger.transport.shyrka.send.JourneyCustomerSession
import com.genesys.cloud.messenger.transport.shyrka.send.OnAttachmentRequest
import com.genesys.cloud.messenger.transport.shyrka.send.OnMessageRequest
import com.genesys.cloud.messenger.transport.shyrka.send.TextMessage
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlin.test.Test
import kotlin.test.assertFailsWith

class SerializationTest {

    @Test
    fun whenConfigureSessionRequestThenEncodes() {
        val journeyContext = JourneyContext(
            JourneyCustomer("00000000-0000-0000-0000-000000000000", "cookie"),
            JourneyCustomerSession("", "web"),
        )
        val encodedString = WebMessagingJson.json.encodeToString(
            ConfigureSessionRequest(
                "<token>",
                "<deploymentId>",
                journeyContext,
            )
        )

        assertThat(encodedString, "encoded ConfigureSessionRequest")
            .isEqualTo("""{"token":"<token>","deploymentId":"<deploymentId>","journeyContext":{"customer":{"id":"00000000-0000-0000-0000-000000000000","idType":"cookie"},"customerSession":{"id":"","type":"web"}},"action":"configureSession"}""")
    }

    @Test
    fun whenEchoRequestThenEncodes() {
        val echoRequest = EchoRequest("<token>")

        val encodedString = WebMessagingJson.json.encodeToString(echoRequest)

        assertThat(encodedString, "encoded EchoRequest")
            .isEqualTo("""{"token":"<token>","action":"echo","message":{"text":"ping","type":"Text"}}""")
    }

    @Test
    fun whenOnMessageRequestThenEncodes() {
        val messageRequest = OnMessageRequest("<token>", TextMessage("Hello world"))

        var encodedString = WebMessagingJson.json.encodeToString(messageRequest)

        assertThat(encodedString, "encoded OnMessageRequest")
            .isEqualTo("""{"token":"<token>","message":{"text":"Hello world","type":"Text"},"action":"onMessage"}""")

        val messageWithAttachmentRequest = OnMessageRequest(
            token = "<token>",
            message = TextMessage(
                text = "Hello world", metadata = mapOf("id" to "aaa-bbb-ccc"),
                content = listOf(
                    Message.Content(
                        contentType = Message.Content.Type.Attachment,
                        attachment = Attachment("abcd-1234")
                    )
                )
            ),
        )

        encodedString = WebMessagingJson.json.encodeToString(messageWithAttachmentRequest)

        assertThat(encodedString, "encoded OnMessageRequest with attachment")
            .isEqualTo("""{"token":"<token>","message":{"text":"Hello world","metadata":{"id":"aaa-bbb-ccc"},"content":[{"contentType":"Attachment","attachment":{"id":"abcd-1234"}}],"type":"Text"},"action":"onMessage"}""")
    }

    @Test
    fun whenOnAttachmentRequestThenEncodes() {
        val attachmentRequest = OnAttachmentRequest(
            token = "<token>",
            attachmentId = "00000000-0000-0000-0000-000000000001",
            fileName = "foo.png",
            fileType = "image/png",
            fileSize = 424242,
            errorsAsJson = true,
        )

        val encodedString = WebMessagingJson.json.encodeToString(attachmentRequest)

        assertThat(encodedString, "encoded OnAttachmentRequest")
            .isEqualTo("""{"token":"<token>","attachmentId":"00000000-0000-0000-0000-000000000001","fileName":"foo.png","fileType":"image/png","fileSize":424242,"errorsAsJson":true,"action":"onAttachment"}""")
    }

    @Test
    fun whenSessionExpiredEventThenDecodes() {
        val json =
            """
            {
              "type": "response",
              "class": "SessionExpiredEvent",
              "code": 200,
              "body": {}
            }
            """.trimIndent()

        val message = decode(json)

        assertThat(message.body, "WebMessagingMessage body").isNotNull()
            .hasClass(SessionExpiredEvent::class)
    }

    private fun decode(json: String): WebMessagingMessage<*> {
        return WebMessagingJson.decodeFromString(json)
    }

    @Test
    fun whenEchoMessageThenDecodingAsWebMessagingMessageThrowsException() {
        val json =
            """
            {
              "action": "echo",
              "message": {
                "type": "Text",
                "text": "ping"
              }
            }
            """.trimIndent()

        assertFailsWith<SerializationException> { decode(json) }
    }

    @Test
    fun whenSessionResponseThenDecodes() {
        val json =
            """
            {
              "type": "response",
              "class": "SessionResponse",
              "code": 200,
              "body": {
                "connected": true,
                "newSession": true
              }
            }
            """.trimIndent()
        val expectedSessionResponseMessage = WebMessagingMessage(
            type = MessageType.Response.value,
            code = 200,
            body = SessionResponse(
                connected = true,
                newSession = true,
            )
        )

        val message = decode(json)

        assertThat(message.body, "WebMessagingMessage body").isNotNull()
            .hasClass(SessionResponse::class)
        assertThat(message).isEqualTo(expectedSessionResponseMessage)
    }

    @Test
    fun whenUnsolicitedMessageThenDecodes() {
        val json =
            """
            {
              "type": "message",
              "class": "StructuredMessage",
              "code": 200,
              "body": {
                "text": "This is the message from another participant",
                "direction": "Outbound",
                "id": "00000000-0000-0000-0000-000000000000",
                "channel": {
                  "time": "2020-12-09T15:40:07.247Z",
                  "type": "Private",
                  "to": {
                    "firstName": "Peter",
                    "lastName": "Parker"
                  },
                  "from": {
                    "firstName": "Tony",
                    "lastName": "Stark"
                  }
                },
                "type": "Text",
                "content": []
              }
            }
            """.trimIndent()
        val channel = StructuredMessage.Channel(
            time = "2020-12-09T15:40:07.247Z",
            type = "Private",
            to = StructuredMessage.Participant(firstName = "Peter", lastName = "Parker"),
            from = StructuredMessage.Participant(firstName = "Tony", lastName = "Stark")
        )
        val structuredMessage = StructuredMessage(
            text = "This is the message from another participant",
            direction = "Outbound",
            id = "00000000-0000-0000-0000-000000000000",
            channel = channel,
            type = StructuredMessage.Type.Text,
            content = listOf()
        )
        val expectedUnsolicitedMessage = WebMessagingMessage(
            type = MessageType.Message.value,
            code = 200,
            body = structuredMessage
        )

        val message = decode(json)

        assertThat(message.body, "WebMessagingMessage body").isNotNull()
            .hasClass(StructuredMessage::class)
        assertThat(message).isEqualTo(expectedUnsolicitedMessage)
    }

    @Test
    fun whenPresignedUrlResponseThenDecodes() {
        val json =
            """{"type":"response","class":"PresignedUrlResponse","code":200,"body":{"attachmentId":"abcd-1234","headers":{"x-amz-tagging":"organizationId=1234&originPlatform=PureCloud&role=Darth&owner=Dev-CloudAppsDarth@genesys.com"},"url":"https://uploads.url/foo.png"}}"""
        val presignedUrlResponse = PresignedUrlResponse(
            attachmentId = "abcd-1234",
            headers = mapOf("x-amz-tagging" to "organizationId=1234&originPlatform=PureCloud&role=Darth&owner=Dev-CloudAppsDarth@genesys.com"),
            url = "https://uploads.url/foo.png"
        )
        val expectedPresignedUrlResponseMessage = WebMessagingMessage(
            type = MessageType.Response.value,
            code = 200,
            body = presignedUrlResponse
        )

        val message = decode(json)

        assertThat(message.body, "WebMessagingMessage body").isNotNull()
            .hasClass(PresignedUrlResponse::class)
        assertThat(message).isEqualTo(expectedPresignedUrlResponseMessage)
    }

    @Test
    fun whenUploadSuccessEventThenDecodes() {
        val json =
            """{"type":"message","class":"UploadSuccessEvent","code":200,"body":{"attachmentId":"abcd-1234","downloadUrl":"https://uploads.url/foo.png","timestamp":"2021-04-21T14:03:09.581Z"}}"""
        val uploadSuccessEvent = UploadSuccessEvent(
            attachmentId = "abcd-1234",
            downloadUrl = "https://uploads.url/foo.png",
            timestamp = "2021-04-21T14:03:09.581Z"
        )
        val expectedUploadSuccessEventMessage = WebMessagingMessage(
            type = MessageType.Message.value,
            code = 200,
            body = uploadSuccessEvent
        )

        val message = decode(json)

        assertThat(message.body, "WebMessagingMessage body").isNotNull()
            .hasClass(UploadSuccessEvent::class)
        assertThat(message).isEqualTo(expectedUploadSuccessEventMessage)
    }

    @Test
    fun whenJwtResponseThenDecodes() {
        val json =
            """{"type":"response","class":"JwtResponse","code":200,"body":{"jwt":"expected-jwt-token","exp":1623675564}}"""
        val jwtResponse = JwtResponse(
            jwt = "expected-jwt-token",
            exp = 1623675564
        )
        val expectedJwtResponse = WebMessagingMessage(
            type = MessageType.Response.value,
            code = 200,
            body = jwtResponse
        )

        val message = decode(json)

        assertThat(message.body, "WebMessagingMessage body").isNotNull()
            .hasClass(JwtResponse::class)
        assertThat(message).isEqualTo(expectedJwtResponse)
    }

    @Test
    fun whenMessageClassNameDoesNotMatchSupportedTypes() {
        val json = """{"type":"response","class":"NotSupportedClassName","code":200,"body":{}}"""
        assertFailsWith<IllegalArgumentException> { decode(json) }
    }

    @Test
    fun whenUploadFailureEventThenDecodes() {
        val json =
            """{"type":"message","class":"UploadFailureEvent","code":200,"body":{"attachmentId":"abcd-1234","errorCode":4001,"errorMessage":"error message", "timestamp":"2021-04-21T14:03:09.581Z"}}"""
        val uploadFailureEvent = UploadFailureEvent(
            attachmentId = "abcd-1234",
            errorCode = 4001,
            errorMessage = "error message",
            timestamp = "2021-04-21T14:03:09.581Z"
        )
        val expectedUploadFailureEventMessage = WebMessagingMessage(
            type = MessageType.Message.value,
            code = 200,
            body = uploadFailureEvent
        )

        val message = decode(json)

        assertThat(message.body, "WebMessagingMessage body").isNotNull()
            .hasClass(UploadFailureEvent::class)
        assertThat(message).isEqualTo(expectedUploadFailureEventMessage)
    }

    @Test
    fun whenGenerateUrlErrorEventThenDecodes() {
        val json =
            """{"type":"message","class":"GenerateUrlError","code":200,"body":{"attachmentId":"abcd-1234","errorCode":4001,"errorMessage":"error message"}}"""
        val generateUrlError = GenerateUrlError(
            attachmentId = "abcd-1234",
            errorCode = 4001,
            errorMessage = "error message",
        )
        val expectedGenerateUrlErrorMessage = WebMessagingMessage(
            type = MessageType.Message.value,
            code = 200,
            body = generateUrlError
        )

        val message = decode(json)

        assertThat(message.body, "WebMessagingMessage body").isNotNull()
            .hasClass(GenerateUrlError::class)
        assertThat(message).isEqualTo(expectedGenerateUrlErrorMessage)
    }

    @Test
    fun whenTooManyRequestsErrorMessageThenDecodes() {
        val json =
            """
            {"type":"response","class":"TooManyRequestsErrorMessage","code":429,"body":{"retryAfter":3,"errorCode":4029,"errorMessage":"Message rate too high for this session"}}
            """.trimIndent()
        val expectedTooManyRequestsErrorBody = TooManyRequestsErrorMessage(
            retryAfter = 3,
            errorCode = 4029,
            errorMessage = "Message rate too high for this session"
        )
        val expectedTooManyRequestsError = WebMessagingMessage(
            type = MessageType.Response.value,
            code = 429,
            body = expectedTooManyRequestsErrorBody
        )

        val message = decode(json)

        assertThat(message).isEqualTo(expectedTooManyRequestsError)
    }
}
