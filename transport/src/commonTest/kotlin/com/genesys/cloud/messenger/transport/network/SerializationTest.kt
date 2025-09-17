package com.genesys.cloud.messenger.transport.network

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.hasClass
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import com.genesys.cloud.messenger.transport.core.Action
import com.genesys.cloud.messenger.transport.core.Attachment
import com.genesys.cloud.messenger.transport.core.ButtonResponse
import com.genesys.cloud.messenger.transport.core.Card
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
import com.genesys.cloud.messenger.transport.utility.CardTestValues
import com.genesys.cloud.messenger.transport.utility.QuickReplyTestValues
import com.genesys.cloud.messenger.transport.utility.StructuredMessageValues
import com.genesys.cloud.messenger.transport.utility.TestValues
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
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
    fun whenEchoRequestThenEncodes() {
        val echoRequest = EchoRequest("<token>")

        val encodedString = WebMessagingJson.json.encodeToString(echoRequest)

        assertThat(encodedString, "encoded EchoRequest")
            .isEqualTo("""{"token":"<token>","action":"echo","message":{"text":"ping","metadata":{"customMessageId":"$HealthCheckID"},"type":"Text"}}""")
    }

    @Test
    fun whenOnMessageRequestThenEncodes() {
        val messageRequest = OnMessageRequest("<token>", TextMessage("Hello world"))

        var encodedString = WebMessagingJson.json.encodeToString(messageRequest)

        assertThat(encodedString, "encoded OnMessageRequest")
            .isEqualTo("""{"token":"<token>","message":{"text":"Hello world","type":"Text"},"action":"onMessage"}""")

        val messageWithAttachmentAndCustomAttributesRequest = OnMessageRequest(
            token = "<token>",
            message = TextMessage(
                text = "Hello world", metadata = mapOf("id" to "aaa-bbb-ccc"),
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
            .isEqualTo(
                """{"token":"<token>","message":{"text":"Hello world","metadata":{"id":"aaa-bbb-ccc"},"content":[{"contentType":"Attachment","attachment":{"id":"abcd-1234"}}],"channel":{"metadata":{"customAttributes":{"A":"B"}}},"type":"Text"},"action":"onMessage"}"""
            )
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

    @Test
    fun whenConnectionClosedEventThenDecodes() {
        val json = """{"type":"message","class":"ConnectionClosedEvent","code":200,"body":{}}"""

        val message = decode(json)

        assertThat(message.body, "WebMessagingMessage body").isNotNull()
            .hasClass(ConnectionClosedEvent::class)
    }

    @Test
    fun whenCloseSessionRequestThenEncodes() {
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
    fun whenConfigureAuthenticatedSessionRequestThenEncodes() {
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
    fun whenLogoutEventThenDecodes() {
        val json = """{"type":"message","class":"LogoutEvent","code":200,"body":{}}"""

        val message = decode(json)

        assertThat(message.body, "WebMessagingMessage body").isNotNull()
            .hasClass(LogoutEvent::class)
    }

    @Test
    fun whenStructuredMessageWithAttachmentAndUnknownContent() {
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
    fun whenClearConversationRequestThenEncodes() {
        val encodedString = WebMessagingJson.json.encodeToString(
            ClearConversationRequest(
                token = "<token>",
            )
        )

        assertThat(encodedString, "encoded ClearConversationRequest")
            .isEqualTo("""{"token":"<token>","action":"onMessage","message":{"events":[{"eventType":"Presence","presence":{"type":"Clear"}}],"type":"Event"}}""")
    }

    @Test
    fun whenSessionClearedEventThenDecodes() {
        val json = """{"type":"message","class":"SessionClearedEvent","code":200,"body":{}}"""

        val message = decode(json)

        assertThat(message.body, "WebMessagingMessage body").isNotNull()
            .hasClass(SessionClearedEvent::class)
    }

    @Test
    fun `when StructuredMessage with Carousel then decodes`() {
        val json = """{"type":"message","class":"StructuredMessage","code":200,"body":{"id":"carousel-id","type":"Structured","text":"Carousel content test","direction":"Outbound","content":[{"contentType":"Carousel","carousel":{"cards":[{"title":"Card 1","description":"D1","actions":[{"type":"Link","text":"Open","url":"http://example.org"}]},{"title":"Card 2","description":"D2","actions":[]}]} }],"originatingEntity":"Human"}}"""

        val card1 = CardTestValues.createCard(
            title = "Card 1",
            description = "D1",
            actionText = "Open",
            linkUrl = "http://example.org"
        )

        val card2 = StructuredMessage.Content.CardContent.Card(
            title = "Card 2",
            description = "D2",
            image = null,
            defaultAction = null,
            actions = emptyList()
        )

        val expectedBody = StructuredMessageValues.createStructuredMessageForTesting(
            id = "carousel-id",
            type = StructuredMessage.Type.Structured,
            direction = "Outbound",
            text = "Carousel content test",
            content = listOf(
                StructuredMessage.Content.CarouselContent(
                    contentType = "Carousel",
                    carousel = StructuredMessage.Content.CarouselContent.Carousel(
                        cards = listOf(card1, card2)
                    )
                )
            )
        ).copy(originatingEntity = "Human")

        val expected = StructuredMessageValues.expectedWebMessage(expectedBody)
        val message = WebMessagingJson.decodeFromString(json)

        assertThat(message.body, "WebMessagingMessage body").isNotNull().hasClass(StructuredMessage::class)
        assertThat(message).isEqualTo(expected)
    }

    @Test
    fun `when StructuredMessage with Card then decodes`() {
        val json = """{"type":"message","class":"StructuredMessage","code":200,"body":{"id":"card-id","type":"Structured","text":"Card content test","direction":"Outbound","content":[{"contentType":"Card","card":{"title":"One Card","description":"Single card","actions":[{"type":"Link","text":"Open","url":"http://example.org"}]}}],"originatingEntity":"Human"}}"""

        val givenCard = CardTestValues.createCard(
            title = "One Card",
            description = "Single card",
            actionText = "Open",
            linkUrl = "http://example.org"
        )

        val expectedBody = StructuredMessageValues.createStructuredMessageForTesting(
            id = "card-id",
            type = StructuredMessage.Type.Structured,
            direction = "Outbound",
            text = "Card content test",
            content = listOf(CardTestValues.createCardContent(givenCard)),
        ).copy(originatingEntity = "Human")

        val expected = StructuredMessageValues.expectedWebMessage(expectedBody)

        val message = WebMessagingJson.decodeFromString(json)

        assertThat(message.body, "WebMessagingMessage body").isNotNull()
            .hasClass(StructuredMessage::class)
        assertThat(message).isEqualTo(expected)
    }

    @Test
    fun `when Link Action then encodes and decodes`() {
        val givenActionLink = Action.Link(
            text = CardTestValues.text,
            url = CardTestValues.url
        )

        val encoded = WebMessagingJson.json.encodeToString(Action.serializer(), givenActionLink)
        val decoded = WebMessagingJson.json.decodeFromString(Action.serializer(), encoded)

        val expectedActionLink = Action.Link(
            text = CardTestValues.text,
            url = CardTestValues.url
        )

        assertThat(decoded).isEqualTo(expectedActionLink)
    }

    @Test
    fun `when Postback Action then encodes and decodes`() {
        val givenActionPostback = Action.Postback(
            text = CardTestValues.POSTBACK_TEXT,
            payload = CardTestValues.POSTBACK_PAYLOAD
        )

        val encoded = WebMessagingJson.json.encodeToString(Action.serializer(), givenActionPostback)
        val decoded = WebMessagingJson.json.decodeFromString(Action.serializer(), encoded)

        val expectedActionPostback = Action.Postback(
            text = CardTestValues.POSTBACK_TEXT,
            payload = CardTestValues.POSTBACK_PAYLOAD
        )

        assertThat(decoded).isEqualTo(expectedActionPostback)
    }

    @Test
    fun `when listOf Actions then encodes and decodes`() {
        val givenList = listOf(
            Action.Link(text = "Open", url = CardTestValues.url),
            Action.Postback(
                text = CardTestValues.POSTBACK_TEXT,
                payload = CardTestValues.POSTBACK_PAYLOAD
            )
        )

        val encoded = WebMessagingJson.json.encodeToString(ListSerializer(Action.serializer()), givenList)
        val decoded = WebMessagingJson.json.decodeFromString(ListSerializer(Action.serializer()), encoded)

        val expectedList = listOf(
            Action.Link(text = "Open", url = CardTestValues.url),
            Action.Postback(
                text = CardTestValues.POSTBACK_TEXT,
                payload = CardTestValues.POSTBACK_PAYLOAD
            )
        )

        assertThat(decoded).isEqualTo(expectedList)
    }

    @Test
    fun `when Action Link then serializes and decodes`() {
        val givenActionLink = Action.Link(
            text = "Open",
            url = CardTestValues.url
        )

        val encoded = WebMessagingJson.json.encodeToString(Action.serializer(), givenActionLink)
        val decoded = WebMessagingJson.json.decodeFromString(Action.serializer(), encoded)

        val expectedActionLink = Action.Link(
            text = "Open",
            url = CardTestValues.url
        )

        assertThat(decoded).isEqualTo(expectedActionLink)
    }

    @Test
    fun `when Action Postback then serializes and decodes`() {
        val givenActionPostback = Action.Postback(
            text = CardTestValues.POSTBACK_TEXT,
            payload = CardTestValues.POSTBACK_PAYLOAD
        )

        val encoded = WebMessagingJson.json.encodeToString(Action.serializer(), givenActionPostback)
        val decoded = WebMessagingJson.json.decodeFromString(Action.serializer(), encoded)

        val expectedActionPostback = Action.Postback(
            text = CardTestValues.POSTBACK_TEXT,
            payload = CardTestValues.POSTBACK_PAYLOAD
        )

        assertThat(decoded).isEqualTo(expectedActionPostback)
    }

    @Test
    fun `when Action enum values then covers all`() {
        val expectedTypeNames = listOf("Link", "Postback")

        val typeNames = Action.Type.entries.map { it.name }

        assertThat(typeNames).containsExactly(*expectedTypeNames.toTypedArray())
        assertThat(
            Action.Link(url = CardTestValues.url, text = CardTestValues.text).type
        ).isEqualTo(Action.Type.Link)
        assertThat(
            Action.Postback(
                text = CardTestValues.POSTBACK_TEXT,
                payload = CardTestValues.POSTBACK_PAYLOAD
            ).type
        ).isEqualTo(Action.Type.Postback)
    }

    @Test
    fun `when Action Link then serializes and deserializes`() {
        val givenAction = Action.Link(
            text = CardTestValues.text,
            url = CardTestValues.url
        )

        val encoded = WebMessagingJson.json.encodeToString(Action.serializer(), givenAction)
        val decoded = WebMessagingJson.json.decodeFromString(Action.serializer(), encoded)

        val expectedAction = Action.Link(
            text = CardTestValues.text,
            url = CardTestValues.url
        )

        assertThat(decoded).isEqualTo(expectedAction)
    }

    @Test
    fun `when Action Link with null text then serializes and deserializes`() {
        val givenAction = Action.Link(
            text = null,
            url = CardTestValues.url
        )

        val encoded = WebMessagingJson.json.encodeToString(Action.serializer(), givenAction)
        val decoded = WebMessagingJson.json.decodeFromString(Action.serializer(), encoded)

        val expectedAction = Action.Link(
            text = null,
            url = CardTestValues.url
        )

        assertThat(decoded).isEqualTo(expectedAction)
    }

    @Test
    fun `when Action Postback then serializes and deserializes`() {
        val givenAction = Action.Postback(
            text = CardTestValues.POSTBACK_TEXT,
            payload = CardTestValues.POSTBACK_PAYLOAD
        )

        val encoded = WebMessagingJson.json.encodeToString(Action.serializer(), givenAction)
        val decoded = WebMessagingJson.json.decodeFromString<Action>(encoded)

        val expectedAction = Action.Postback(
            text = CardTestValues.POSTBACK_TEXT,
            payload = CardTestValues.POSTBACK_PAYLOAD
        )

        assertThat(decoded).isEqualTo(expectedAction)
    }

    @Test
    fun `when Card with defaultAction and Actions then encodes and decodes`() {
        val givenCard = Card(
            title = CardTestValues.title,
            description = CardTestValues.description,
            image = CardTestValues.image,
            defaultAction = Action.Link(text = "Open", url = CardTestValues.url),
            actions = listOf(
                Action.Link(text = "Open", url = CardTestValues.url),
                Action.Postback(
                    text = CardTestValues.POSTBACK_TEXT,
                    payload = CardTestValues.POSTBACK_PAYLOAD
                )
            )
        )

        val encoded = WebMessagingJson.json.encodeToString(Card.serializer(), givenCard)
        val decoded = WebMessagingJson.json.decodeFromString(Card.serializer(), encoded)

        val expectedCard = givenCard.copy()
        assertThat(decoded).isEqualTo(expectedCard)
    }

    @Test
    fun `when Card with only title then encodes and decodes`() {
        val givenCard = Card(title = CardTestValues.title)

        val encoded = WebMessagingJson.json.encodeToString(Card.serializer(), givenCard)
        val decoded = WebMessagingJson.json.decodeFromString(Card.serializer(), encoded)

        val expectedCard = Card(
            title = CardTestValues.title,
            description = null,
            image = null,
            defaultAction = null,
            actions = emptyList()
        )
        assertThat(decoded).isEqualTo(expectedCard)
    }

    @Test
    fun `when listOf Cards then encodes and decodes`() {
        val givenCards = listOf(
            Card(
                title = CardTestValues.title,
                description = CardTestValues.description,
                image = CardTestValues.image,
                defaultAction = Action.Link(text = "Open", url = CardTestValues.url),
                actions = listOf(
                    Action.Postback(
                        text = CardTestValues.POSTBACK_TEXT,
                        payload = CardTestValues.POSTBACK_PAYLOAD
                    )
                )
            ),
            Card(title = "${CardTestValues.title} 2")
        )

        val encoded = WebMessagingJson.json.encodeToString(
            ListSerializer(Card.serializer()),
            givenCards
        )
        val decoded = WebMessagingJson.json.decodeFromString(
            ListSerializer(Card.serializer()),
            encoded
        )

        val expectedCards = listOf(
            givenCards[0].copy(),
            givenCards[1].copy()
        )
        assertThat(decoded).isEqualTo(expectedCards)
    }

    @Test
    fun `when Card created then getters return values`() {
        val givenCard = Card(
            title = CardTestValues.title,
            description = CardTestValues.description,
            image = CardTestValues.image,
            defaultAction = Action.Link(text = "Open", url = CardTestValues.url),
            actions = listOf(Action.Postback(CardTestValues.POSTBACK_TEXT, CardTestValues.POSTBACK_PAYLOAD))
        )

        assertThat(givenCard.title).isEqualTo(CardTestValues.title)
        assertThat(givenCard.description).isEqualTo(CardTestValues.description)
        assertThat(givenCard.image).isEqualTo(CardTestValues.image)
        assertThat(givenCard.defaultAction).isEqualTo(Action.Link(text = "Open", url = CardTestValues.url))
        assertThat(givenCard.actions).isEqualTo(listOf(Action.Postback(CardTestValues.POSTBACK_TEXT, CardTestValues.POSTBACK_PAYLOAD)))
    }

    @Test
    fun `when Card with Link default and Postback Action list then encodes and decodes`() {
        val givenCard = Card(
            title = CardTestValues.title,
            description = CardTestValues.description,
            image = CardTestValues.image,
            defaultAction = Action.Link(text = "Open", url = CardTestValues.url),
            actions = listOf(
                Action.Postback(
                    text = CardTestValues.POSTBACK_TEXT,
                    payload = CardTestValues.POSTBACK_PAYLOAD
                )
            )
        )

        val encoded = WebMessagingJson.json.encodeToString(Card.serializer(), givenCard)
        val decoded = WebMessagingJson.json.decodeFromString(Card.serializer(), encoded)

        val expectedCard = Card(
            title = CardTestValues.title,
            description = CardTestValues.description,
            image = CardTestValues.image,
            defaultAction = Action.Link(text = "Open", url = CardTestValues.url),
            actions = listOf(
                Action.Postback(
                    text = CardTestValues.POSTBACK_TEXT,
                    payload = CardTestValues.POSTBACK_PAYLOAD
                )
            )
        )
        assertThat(decoded).isEqualTo(expectedCard)
    }

    @Test
    fun `when Card with only required title then encodes and decodes`() {
        val givenCard = Card(title = CardTestValues.title)

        val encoded = WebMessagingJson.json.encodeToString(Card.serializer(), givenCard)
        val decoded = WebMessagingJson.json.decodeFromString(Card.serializer(), encoded)

        val expectedCard = Card(
            title = CardTestValues.title,
            description = null,
            image = null,
            defaultAction = null,
            actions = emptyList()
        )
        assertThat(decoded).isEqualTo(expectedCard)
    }

    @Test
    fun `when Message Card with default and Actions then encodes and decodes`() {
        val givenCard = Message.Card(
            title = CardTestValues.title,
            description = CardTestValues.description,
            imageUrl = CardTestValues.image,
            actions = listOf(
                ButtonResponse(
                    type = QuickReplyTestValues.QUICK_REPLY,
                    text = CardTestValues.POSTBACK_TEXT,
                    payload = CardTestValues.POSTBACK_PAYLOAD
                )
            ),
            defaultAction = ButtonResponse(
                type = CardTestValues.LINK_TYPE,
                text = "Open",
                payload = CardTestValues.url
            )
        )

        val encoded = WebMessagingJson.json.encodeToString(Message.Card.serializer(), givenCard)
        val decoded = WebMessagingJson.json.decodeFromString(Message.Card.serializer(), encoded)

        val expected = givenCard.copy()
        assertThat(decoded).isEqualTo(expected)
    }

    @Test
    fun `when Message Card with only required fields then encodes and decodes`() {
        val givenCard = Message.Card(
            title = CardTestValues.title,
            description = CardTestValues.description,
            actions = emptyList()
        )

        val encoded = WebMessagingJson.json.encodeToString(Message.Card.serializer(), givenCard)
        val decoded = WebMessagingJson.json.decodeFromString(Message.Card.serializer(), encoded)

        val expected = Message.Card(
            title = CardTestValues.title,
            description = CardTestValues.description,
            imageUrl = null,
            actions = emptyList(),
            defaultAction = null
        )
        assertThat(decoded).isEqualTo(expected)
    }

    @Test
    fun `when Message with Cards then encodes and decodes`() {
        val givenMsg = Message(
            id = TestValues.TOKEN,
            direction = Message.Direction.Outbound,
            messageType = Message.Type.Cards,
            text = "Cards payload",
            cards = listOf(
                Message.Card(
                    title = CardTestValues.title,
                    description = CardTestValues.description,
                    imageUrl = CardTestValues.image,
                    actions = listOf(
                        ButtonResponse(
                            type = QuickReplyTestValues.QUICK_REPLY,
                            text = CardTestValues.POSTBACK_TEXT,
                            payload = CardTestValues.POSTBACK_PAYLOAD
                        )
                    ),
                    defaultAction = ButtonResponse(
                        type = CardTestValues.LINK_TYPE,
                        text = "Open",
                        payload = CardTestValues.url
                    )
                )
            )
        )

        val encoded = WebMessagingJson.json.encodeToString(Message.serializer(), givenMsg)
        val decoded = WebMessagingJson.json.decodeFromString(Message.serializer(), encoded)

        val expected = Message(
            id = TestValues.TOKEN,
            direction = Message.Direction.Outbound,
            messageType = Message.Type.Cards,
            text = "Cards payload",
            cards = listOf(
                Message.Card(
                    title = CardTestValues.title,
                    description = CardTestValues.description,
                    imageUrl = CardTestValues.image,
                    actions = listOf(
                        ButtonResponse(
                            type = QuickReplyTestValues.QUICK_REPLY,
                            text = CardTestValues.POSTBACK_TEXT,
                            payload = CardTestValues.POSTBACK_PAYLOAD
                        )
                    ),
                    defaultAction = ButtonResponse(
                        type = CardTestValues.LINK_TYPE,
                        text = "Open",
                        payload = CardTestValues.url
                    )
                )
            )
        )
        assertThat(decoded).isEqualTo(expected)
    }

    @Test
    fun `when Message with Cards and no defaultActionThenEncodesAndDecodes`() {
        val givenMsg = Message(
            id = TestValues.SECONDARY_TOKEN,
            direction = Message.Direction.Outbound,
            messageType = Message.Type.Cards,
            text = "Cards payload (no defaultAction)",
            cards = listOf(
                Message.Card(
                    title = "${CardTestValues.title} 2",
                    description = CardTestValues.description,
                    imageUrl = null,
                    actions = listOf(
                        ButtonResponse(
                            type = QuickReplyTestValues.QUICK_REPLY,
                            text = CardTestValues.POSTBACK_TEXT,
                            payload = CardTestValues.POSTBACK_PAYLOAD
                        )
                    ),
                    defaultAction = null
                )
            )
        )

        val encoded = WebMessagingJson.json.encodeToString(Message.serializer(), givenMsg)
        val decoded = WebMessagingJson.json.decodeFromString(Message.serializer(), encoded)

        val expected = Message(
            id = TestValues.SECONDARY_TOKEN,
            direction = Message.Direction.Outbound,
            messageType = Message.Type.Cards,
            text = "Cards payload (no defaultAction)",
            cards = listOf(
                Message.Card(
                    title = "${CardTestValues.title} 2",
                    description = CardTestValues.description,
                    imageUrl = null,
                    actions = listOf(
                        ButtonResponse(
                            type = QuickReplyTestValues.QUICK_REPLY,
                            text = CardTestValues.POSTBACK_TEXT,
                            payload = CardTestValues.POSTBACK_PAYLOAD
                        )
                    ),
                    defaultAction = null
                )
            )
        )
        assertThat(decoded).isEqualTo(expected)
    }

    @Test
    fun `when Action Link serializer encodes and decodes`() {
        val given = Action.Link(
            text = CardTestValues.text,
            url = CardTestValues.url
        )

        val encoded = WebMessagingJson.json.encodeToString(Action.Link.serializer(), given)
        val decoded = WebMessagingJson.json.decodeFromString(Action.Link.serializer(), encoded)

        val expected = Action.Link(text = CardTestValues.text, url = CardTestValues.url)
        assertThat(decoded).isEqualTo(expected)
    }

    @Test
    fun `when Action Link serializer with null text then encodes and decodes`() {
        val given = Action.Link(
            text = null,
            url = CardTestValues.url
        )

        val encoded = WebMessagingJson.json.encodeToString(Action.Link.serializer(), given)
        val decoded = WebMessagingJson.json.decodeFromString(Action.Link.serializer(), encoded)

        val expected = Action.Link(text = null, url = CardTestValues.url)
        assertThat(decoded).isEqualTo(expected)
    }

    @Test
    fun `when listOf Action Links then encodes and decodes using link serializer`() {
        val given = listOf(
            Action.Link(text = "Open", url = CardTestValues.url),
            Action.Link(text = CardTestValues.text, url = CardTestValues.url)
        )

        val ser = ListSerializer(Action.Link.serializer())
        val encoded = WebMessagingJson.json.encodeToString(ser, given)
        val decoded = WebMessagingJson.json.decodeFromString(ser, encoded)

        val expected = listOf(
            Action.Link(text = "Open", url = CardTestValues.url),
            Action.Link(text = CardTestValues.text, url = CardTestValues.url)
        )
        assertThat(decoded).isEqualTo(expected)
    }

    @Test
    fun `when Structured Action Link then encodes and decodes`() {
        val given = StructuredMessage.Content.Action(
            type = CardTestValues.LINK_TYPE,
            text = CardTestValues.text,
            url = CardTestValues.url,
            payload = null
        )

        val encoded = WebMessagingJson.json.encodeToString(
            StructuredMessage.Content.Action.serializer(), given
        )
        val decoded = WebMessagingJson.json.decodeFromString(
            StructuredMessage.Content.Action.serializer(), encoded
        )

        val expected = StructuredMessage.Content.Action(
            type = CardTestValues.LINK_TYPE,
            text = CardTestValues.text,
            url = CardTestValues.url,
            payload = null
        )
        assertThat(decoded).isEqualTo(expected)
    }

    @Test
    fun `when Structured Action Postback then encodes and decodes`() {
        val given = StructuredMessage.Content.Action(
            type = CardTestValues.POSTBACK_TYPE,
            text = CardTestValues.POSTBACK_TEXT,
            url = null,
            payload = CardTestValues.POSTBACK_PAYLOAD
        )

        val encoded = WebMessagingJson.json.encodeToString(
            StructuredMessage.Content.Action.serializer(), given
        )
        val decoded = WebMessagingJson.json.decodeFromString(
            StructuredMessage.Content.Action.serializer(), encoded
        )

        val expected = StructuredMessage.Content.Action(
            type = CardTestValues.POSTBACK_TYPE,
            text = CardTestValues.POSTBACK_TEXT,
            url = null,
            payload = CardTestValues.POSTBACK_PAYLOAD
        )
        assertThat(decoded).isEqualTo(expected)
    }

    @Test
    fun `when listOf Structured Actions then encodes and decodes`() {
        val given = listOf(
            StructuredMessage.Content.Action(
                type = CardTestValues.LINK_TYPE, text = "Open", url = CardTestValues.url, payload = null
            ),
            StructuredMessage.Content.Action(
                type = CardTestValues.POSTBACK_TYPE,
                text = CardTestValues.POSTBACK_TEXT,
                url = null,
                payload = CardTestValues.POSTBACK_PAYLOAD
            )
        )

        val actionListSerializer = ListSerializer(StructuredMessage.Content.Action.serializer())
        val encoded = WebMessagingJson.json.encodeToString(actionListSerializer, given)
        val decoded = WebMessagingJson.json.decodeFromString(actionListSerializer, encoded)

        val expected = listOf(
            StructuredMessage.Content.Action(
                type = CardTestValues.LINK_TYPE, text = "Open", url = CardTestValues.url, payload = null
            ),
            StructuredMessage.Content.Action(
                type = CardTestValues.POSTBACK_TYPE,
                text = CardTestValues.POSTBACK_TEXT,
                url = null,
                payload = CardTestValues.POSTBACK_PAYLOAD
            )
        )
        assertThat(decoded).isEqualTo(expected)
    }

    @Test fun `when ButtonResponse Content then deserializes`() {
        val json = """{"contentType":"ButtonResponse","buttonResponse":{"text":"t","payload":"p","type":"QuickReply"}}"""
        val decoded = WebMessagingJson.json.decodeFromString(StructuredMessage.Content.serializer(), json)
        assertThat(decoded).isEqualTo(
            StructuredMessage.Content.ButtonResponseContent(
                contentType = "ButtonResponse",
                buttonResponse = StructuredMessage.Content.ButtonResponseContent.ButtonResponse("t", "p", "QuickReply")
            )
        )
    }

    @Test fun `when CardContent then deserializes`() {
        val json = """{"contentType":"Card","card":{"title":"T","description":"D","actions":[]}}"""
        val decoded = WebMessagingJson.json.decodeFromString(StructuredMessage.Content.serializer(), json)
        assertThat(decoded).isEqualTo(
            StructuredMessage.Content.CardContent(
                contentType = "Card",
                card = StructuredMessage.Content.CardContent.Card("T", "D", null, null, emptyList())
            )
        )
    }

    @Test fun `when CarouselContent then deserializes`() {
        val json = """{"contentType":"Carousel","carousel":{"cards":[{"title":"T","description":"D","actions":[]}]} }"""
        val decoded = WebMessagingJson.json.decodeFromString(StructuredMessage.Content.serializer(), json)
        assertThat(decoded).isEqualTo(
            StructuredMessage.Content.CarouselContent(
                contentType = "Carousel",
                carousel = StructuredMessage.Content.CarouselContent.Carousel(
                    listOf(StructuredMessage.Content.CardContent.Card("T", "D", null, null, emptyList()))
                )
            )
        )
    }
}
