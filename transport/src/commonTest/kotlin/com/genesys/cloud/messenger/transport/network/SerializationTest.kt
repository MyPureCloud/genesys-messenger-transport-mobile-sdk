package com.genesys.cloud.messenger.transport.network

import assertk.assertThat
import assertk.assertions.hasClass
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import com.genesys.cloud.messenger.transport.core.Attachment
import com.genesys.cloud.messenger.transport.core.Message
import com.genesys.cloud.messenger.transport.shyrka.WebMessagingJson
import com.genesys.cloud.messenger.transport.shyrka.receive.AllowedMedia
import com.genesys.cloud.messenger.transport.shyrka.receive.ConnectionClosedEvent
import com.genesys.cloud.messenger.transport.shyrka.receive.FileType
import com.genesys.cloud.messenger.transport.shyrka.receive.GenerateUrlError
import com.genesys.cloud.messenger.transport.shyrka.receive.Inbound
import com.genesys.cloud.messenger.transport.shyrka.receive.JwtResponse
import com.genesys.cloud.messenger.transport.shyrka.receive.LogoutEvent
import com.genesys.cloud.messenger.transport.shyrka.receive.MessageType
import com.genesys.cloud.messenger.transport.shyrka.receive.PresignedUrlResponse
import com.genesys.cloud.messenger.transport.shyrka.receive.SessionClearedEvent
import com.genesys.cloud.messenger.transport.shyrka.receive.SessionExpiredEvent
import com.genesys.cloud.messenger.transport.shyrka.receive.SessionResponse
import com.genesys.cloud.messenger.transport.shyrka.receive.StructuredMessage
import com.genesys.cloud.messenger.transport.shyrka.receive.TooManyRequestsErrorMessage
import com.genesys.cloud.messenger.transport.shyrka.receive.UploadFailureEvent
import com.genesys.cloud.messenger.transport.shyrka.receive.UploadSuccessEvent
import com.genesys.cloud.messenger.transport.shyrka.receive.WebMessagingMessage
import com.genesys.cloud.messenger.transport.shyrka.send.Channel
import com.genesys.cloud.messenger.transport.shyrka.send.ClearConversationRequest
import com.genesys.cloud.messenger.transport.shyrka.send.CloseSessionRequest
import com.genesys.cloud.messenger.transport.shyrka.send.ConfigureAuthenticatedSessionRequest
import com.genesys.cloud.messenger.transport.shyrka.send.ConfigureSessionRequest
import com.genesys.cloud.messenger.transport.shyrka.send.EchoRequest
import com.genesys.cloud.messenger.transport.shyrka.send.HealthCheckID
import com.genesys.cloud.messenger.transport.shyrka.send.JourneyContext
import com.genesys.cloud.messenger.transport.shyrka.send.JourneyCustomer
import com.genesys.cloud.messenger.transport.shyrka.send.JourneyCustomerSession
import com.genesys.cloud.messenger.transport.shyrka.send.OnAttachmentRequest
import com.genesys.cloud.messenger.transport.shyrka.send.OnMessageRequest
import com.genesys.cloud.messenger.transport.shyrka.send.TextMessage
import com.genesys.cloud.messenger.transport.utility.TestValues
import kotlinx.serialization.SerializationException
import kotlin.test.Test
import kotlin.test.assertFailsWith

class SerializationTest {

    @Test
    fun `when ConfigureSessionRequest then encodes`() {
        val journeyContext = JourneyContext(
            JourneyCustomer("00000000-0000-0000-0000-000000000000", "cookie"),
            JourneyCustomerSession("", "web"),
        )
        val encodedString = WebMessagingJson.json.encodeToString(
            ConfigureSessionRequest(
                token = "<token>",
                deploymentId = "<deploymentId>",
                startNew = false,
                journeyContext = journeyContext,
            )
        )

        assertThat(encodedString, "encoded ConfigureSessionRequest")
            .isEqualTo("""{"token":"<token>","deploymentId":"<deploymentId>","startNew":false,"journeyContext":{"customer":{"id":"00000000-0000-0000-0000-000000000000","idType":"cookie"},"customerSession":{"id":"","type":"web"}},"action":"configureSession"}""")
    }

    @Test
    fun `when EchoRequest then encodes`() {
        val echoRequest = EchoRequest("<token>")

        val encodedString = WebMessagingJson.json.encodeToString(echoRequest)

        assertThat(encodedString, "encoded EchoRequest")
            .isEqualTo("""{"token":"<token>","action":"echo","message":{"text":"ping","metadata":{"customMessageId":"$HealthCheckID"},"type":"Text"}}""")
    }

    @Test
    fun `when OnMessageRequest then encodes`() {
        val messageRequest = OnMessageRequest("<token>", TextMessage("Hello world"))

        var encodedString = WebMessagingJson.json.encodeToString(messageRequest)

        assertThat(encodedString, "encoded OnMessageRequest")
            .isEqualTo("""{"token":"<token>","message":{"text":"Hello world","type":"Text"},"action":"onMessage"}""")

        val messageWithAttachmentAndCustomAttributesRequest = OnMessageRequest(
            token = "<token>",
            message = TextMessage(
                text = "Hello world",
                metadata = mapOf("id" to "aaa-bbb-ccc"),
                content = listOf(
                    Message.Content(
                        contentType = Message.Content.Type.Attachment,
                        attachment = Attachment("abcd-1234"),
                    )
                ),
                channel = Channel(Channel.Metadata(mapOf("A" to "B"))),
            ),
        )

        encodedString =
            WebMessagingJson.json.encodeToString(messageWithAttachmentAndCustomAttributesRequest)

        assertThat(encodedString, "encoded OnMessageRequest with attachment and custom attributes")
            .isEqualTo("""{"token":"<token>","message":{"text":"Hello world","metadata":{"id":"aaa-bbb-ccc"},"content":[{"contentType":"Attachment","attachment":{"id":"abcd-1234"}}],"channel":{"metadata":{"customAttributes":{"A":"B"}}},"type":"Text"},"action":"onMessage"}""")
    }

    @Test
    fun `when OnAttachmentRequest then encodes`() {
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
    fun `when SessionExpiredEvent then decodes`() {
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
    fun `when EchoMessage then decoding as WebMessagingMessage throws exception`() {
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
    fun `when SessionResponse then decodes`() {
        val json =
            """
            {
              "type": "response",
              "class": "SessionResponse",
              "code": 200,
              "body": {
                "connected": true,
                "newSession": true,
                "readOnly": false,
                "maxCustomDataBytes": 100,
                "allowedMedia": {
                    "inbound": {
                        "fileTypes": [{"type": "*/*"},{"type": "video/3gpp"}],
                        "maxFileSizeKB": 10240
                    }
                },
                "blockedExtensions": [".ade"],
                "clearedExistingSession":false
              }
            }
            """.trimIndent()
        val expectedSessionResponseMessage = WebMessagingMessage(
            type = MessageType.Response.value,
            code = 200,
            body = SessionResponse(
                connected = true,
                newSession = true,
                readOnly = false,
                maxCustomDataBytes = TestValues.MAX_CUSTOM_DATA_BYTES,
                allowedMedia = AllowedMedia(
                    Inbound(
                        fileTypes = listOf(FileType("*/*"), FileType("video/3gpp")),
                        maxFileSizeKB = 10240,
                    ),
                ),
                blockedExtensions = listOf(".ade"),
                clearedExistingSession = false,
            )
        )

        val message = decode(json)

        assertThat(message.body, "WebMessagingMessage body").isNotNull()
            .hasClass(SessionResponse::class)
        assertThat(message).isEqualTo(expectedSessionResponseMessage)
    }

    @Test
    fun `when UnsolicitedMessage then decodes`() {
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
    fun `when PresignedUrlResponse then decodes`() {
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
    fun `when UploadSuccessEvent then decodes`() {
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
    fun `when JwtResponse then decodes`() {
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
    fun `when message class name does not match supported types`() {
        val json = """{"type":"response","class":"NotSupportedClassName","code":200,"body":{}}"""
        assertFailsWith<IllegalArgumentException> { decode(json) }
    }

    @Test
    fun `when UploadFailureEvent then decodes`() {
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
    fun `when GenerateUrlErrorEvent then decodes`() {
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
    fun `when TooManyRequestsErrorMessage then decodes`() {
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

    @Test
    fun `when ConnectionClosedEvent then decodes`() {
        val json = """{"type":"message","class":"ConnectionClosedEvent","code":200,"body":{}}"""

        val message = decode(json)

        assertThat(message.body, "WebMessagingMessage body").isNotNull()
            .hasClass(ConnectionClosedEvent::class)
    }

    @Test
    fun `when CloseSessionRequest then encodes`() {
        val encodedString = WebMessagingJson.json.encodeToString(
            CloseSessionRequest(
                token = "<token>",
                closeAllConnections = true,
            )
        )

        assertThat(encodedString, "encoded CloseSessionRequest")
            .isEqualTo("""{"token":"<token>","closeAllConnections":true,"action":"closeSession"}""")
    }

    @Test
    fun `when ConfigureAuthenticatedSessionRequest then encodes`() {
        val journeyContext = JourneyContext(
            JourneyCustomer("00000000-0000-0000-0000-000000000000", "cookie"),
            JourneyCustomerSession("", "web"),
        )
        val data = ConfigureAuthenticatedSessionRequest.Data("<auth_token>")
        val encodedString = WebMessagingJson.json.encodeToString(
            ConfigureAuthenticatedSessionRequest(
                token = "<token>",
                deploymentId = "<deploymentId>",
                startNew = false,
                journeyContext = journeyContext,
                data = data,
            )
        )

        assertThat(encodedString, "encoded ConfigureAuthenticatedSessionRequest")
            .isEqualTo("""{"token":"<token>","deploymentId":"<deploymentId>","startNew":false,"journeyContext":{"customer":{"id":"00000000-0000-0000-0000-000000000000","idType":"cookie"},"customerSession":{"id":"","type":"web"}},"data":{"code":"<auth_token>"},"action":"configureAuthenticatedSession"}""")
    }

    @Test
    fun `when LogoutEvent then decodes`() {
        val json = """{"type":"message","class":"LogoutEvent","code":200,"body":{}}"""

        val message = decode(json)

        assertThat(message.body, "WebMessagingMessage body").isNotNull()
            .hasClass(LogoutEvent::class)
    }

    @Test
    fun `when StructuredMessage with attachment and unknown content`() {
        val givenStructuredMessage = """{"type":"message","class":"StructuredMessage","code":200,"body":{"direction":"Outbound","id":"msg_id","type":"Text","text":"Hi","content":[{"fakeContent":{"foo":"bar"},"contentType":"FakeContent"},{"attachment":{"id":"attachment_id","filename":"image.png","mediaType":"Image","url":"https://downloadurl.com"},"contentType":"Attachment"}],"originatingEntity":"Human"}}"""
        val expectedStructuredMessage = WebMessagingMessage(
            type = "message",
            code = 200,
            body = StructuredMessage(
                id = "msg_id",
                type = StructuredMessage.Type.Text,
                text = "Hi",
                direction = "Outbound",
                content = listOf(
                    StructuredMessage.Content.UnknownContent,
                    StructuredMessage.Content.AttachmentContent(
                        contentType = "Attachment",
                        attachment = StructuredMessage.Content.AttachmentContent.Attachment(
                            id = "attachment_id",
                            url = "https://downloadurl.com",
                            filename = "image.png",
                            mediaType = "Image",
                        )
                    )
                ),
                originatingEntity = "Human"
            )
        )
        val result = WebMessagingJson.decodeFromString(givenStructuredMessage)

        assertThat(result).isEqualTo(expectedStructuredMessage)
    }

    @Test
    fun `when ClearConversationRequest then encodes`() {
        val encodedString = WebMessagingJson.json.encodeToString(
            ClearConversationRequest(
                token = "<token>",
            )
        )

        assertThat(encodedString, "encoded ClearConversationRequest")
            .isEqualTo("""{"token":"<token>","action":"onMessage","message":{"events":[{"eventType":"Presence","presence":{"type":"Clear"}}],"type":"Event"}}""")
    }

    @Test
    fun `when SessionClearedEvent then decodes`() {
        val json = """{"type":"message","class":"SessionClearedEvent","code":200,"body":{}}"""

        val message = decode(json)

        assertThat(message.body, "WebMessagingMessage body").isNotNull()
            .hasClass(SessionClearedEvent::class)
    }
}
