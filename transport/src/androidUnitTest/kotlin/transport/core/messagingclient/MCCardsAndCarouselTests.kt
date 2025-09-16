package transport.core.messagingclient

import assertk.assertThat
import assertk.assertions.isNotEmpty
import assertk.assertions.isTrue
import com.genesys.cloud.messenger.transport.core.ButtonResponse
import com.genesys.cloud.messenger.transport.core.Message
import com.genesys.cloud.messenger.transport.shyrka.send.Channel
import com.genesys.cloud.messenger.transport.shyrka.send.OnMessageRequest
import com.genesys.cloud.messenger.transport.shyrka.send.StructuredMessage
import com.genesys.cloud.messenger.transport.utility.CardTestValues
import io.mockk.every
import io.mockk.verify
import io.mockk.verifySequence
import org.junit.Test
import transport.util.Request
import transport.util.Response
import kotlin.test.assertFailsWith

class MCCardsAndCarouselTests : BaseMessagingClientTest() {

    @Test
    fun `when connect() and then sendCardReply() but no custom attributes`() {
        val givenPostbackResponse = CardTestValues.postbackButtonResponse
        val expectedRequest = Request.expectedPostbackRequestJson

        subject.connect()
        subject.sendCardReply(givenPostbackResponse)

        verifySequence {
            connectSequence()
            mockLogger.i(capture(logSlot))
            mockCustomAttributesStore.getCustomAttributesToSend()
            mockMessageStore.preparePostbackMessage(Request.token, givenPostbackResponse, null)
            mockLogger.i(capture(logSlot))
            mockPlatformSocket.sendMessage(expectedRequest)
        }

        verify(exactly = 0) {
            mockAttachmentHandler.onSending()
        }
    }

    @Test
    fun `when sendCardReply() but client is not configured`() {
        assertFailsWith<IllegalStateException> {
            subject.sendCardReply(CardTestValues.postbackButtonResponse)
        }
    }

    @Test
    fun `when sendCardReply then StructuredMessage is sent to Shyrka`() {
        val expectedButtonResponse = CardTestValues.postbackButtonResponse
        val expectedCustomAttributes = mapOf("source" to "card")
        val expectedChannel = Channel(Channel.Metadata(expectedCustomAttributes))
        val expectedRequest =
            """{"token":"${Request.token}","message":{"text":"${expectedButtonResponse.text}","metadata":{"customMessageId":"card-123"},"content":[{"contentType":"ButtonResponse","buttonResponse":{"text":"${expectedButtonResponse.text}","payload":"${expectedButtonResponse.payload}","type":"${expectedButtonResponse.type}"}}],"channel":{"metadata":{"customAttributes":{"source":"card"}}},"type":"Structured"},"action":"onMessage"}"""

        every {
            mockMessageStore.preparePostbackMessage(
                Request.token,
                expectedButtonResponse,
                expectedChannel
            )
        } returns OnMessageRequest(
            token = Request.token,
            message = StructuredMessage(
                text = expectedButtonResponse.text,
                metadata = mapOf("customMessageId" to "card-123"),
                content = listOf(
                    Message.Content(
                        contentType = Message.Content.Type.ButtonResponse,
                        buttonResponse = expectedButtonResponse
                    )
                ),
                channel = expectedChannel
            )
        )

        every { mockCustomAttributesStore.getCustomAttributesToSend() } returns expectedCustomAttributes

        subject.connect()
        subject.sendCardReply(CardTestValues.postbackButtonResponse)

        verifySequence {
            connectSequence()
            mockLogger.i(any())
            mockCustomAttributesStore.getCustomAttributesToSend()
            mockCustomAttributesStore.onSending()
            mockMessageStore.preparePostbackMessage(Request.token, expectedButtonResponse, expectedChannel)
            mockLogger.i(any())
            mockPlatformSocket.sendMessage(expectedRequest)
        }
    }

    @Test
    fun `when SocketListener invoke onMessage with Structured message that contains Postback card reply`() {
        val expectedCard = Message.Card(
            title = "Title",
            description = "Description",
            imageUrl = "http://image.com/image.png",
            actions = listOf(
                ButtonResponse(
                    type = "QuickReply",
                    text = "Select this option",
                    payload = "postback_payload"
                )
            )
        )

        val expectedMessage = Message(
            id = "msg_id",
            direction = Message.Direction.Outbound,
            state = Message.State.Sent,
            messageType = Message.Type.Cards,
            text = "You selected this card option",
            cards = listOf(expectedCard),
            from = Message.Participant(originatingEntity = Message.Participant.OriginatingEntity.Bot),
        )

        subject.connect()

        slot.captured.onMessage(Response.onMessageWithPostbackCardReply)

        verifySequence {
            connectSequence()
            mockMessageStore.update(expectedMessage)
        }
    }

    @Test
    fun `when error response received after sendCardReply then onMessageError is called`() {
        val givenButtonResponse = CardTestValues.postbackButtonResponse
        val expectedRequestJson = Request.expectedPostbackRequestJson

        every {
            mockPlatformSocket.sendMessage(expectedRequestJson)
        } answers {
            slot.captured.onMessage(Response.tooManyRequests)
        }

        subject.connect()
        subject.sendCardReply(givenButtonResponse)

        verify {
            mockMessageStore.onMessageError(any(), any())
        }
    }

    @Test
    fun `when sendCardReply() with Link then no non-empty payload value is serialized`() {
        val givenLinkButton = ButtonResponse(
            text = "Open",
            payload = "",
            type = CardTestValues.LINK_TYPE
        )
        val expectedAttrs = mapOf("source" to "card")
        val expectedChannel = Channel(Channel.Metadata(expectedAttrs))

        every { mockCustomAttributesStore.getCustomAttributesToSend() } returns expectedAttrs
        every {
            mockMessageStore.preparePostbackMessage(
                Request.token,
                match { it.type == CardTestValues.LINK_TYPE },
                expectedChannel
            )
        } returns OnMessageRequest(
            token = Request.token,
            message = StructuredMessage(
                text = givenLinkButton.text,
                metadata = mapOf("customMessageId" to "card-123"),
                content = listOf(
                    Message.Content(
                        contentType = Message.Content.Type.ButtonResponse,
                        buttonResponse = givenLinkButton
                    )
                ),
                channel = expectedChannel
            )
        )

        subject.connect()
        subject.sendCardReply(givenLinkButton)

        verifySequence {
            connectSequence()
            mockLogger.i(any())
            mockCustomAttributesStore.getCustomAttributesToSend()
            mockCustomAttributesStore.onSending()
            mockMessageStore.preparePostbackMessage(
                Request.token,
                match { it.type == CardTestValues.LINK_TYPE },
                expectedChannel
            )
            mockLogger.i(any())
            mockPlatformSocket.sendMessage(
                match { json ->
                    if (!json.contains("\"type\":\"Structured\"")) return@match false
                    if (!json.contains("\"type\":\"Link\"")) return@match false
                    val nonEmptyPayload = Regex("\"payload\":\"[^\"]+\"")
                    !nonEmptyPayload.containsMatchIn(json)
                }
            )
        }
    }

    @Test
    fun `sendCardReply logs success and then error messages`() {
        every { mockLogger.i(capture(logSlot)) } answers { }
        every { mockLogger.e(capture(logSlot)) } answers { }

        subject.connect()

        every { mockPlatformSocket.sendMessage(match { it.contains("\"action\":\"onMessage\"") }) } returns Unit

        subject.sendCardReply(CardTestValues.postbackButtonResponse)

        assertThat(logSlot).isNotEmpty()
        assertThat(logSlot.any { it.invoke().contains("sendCardReply") }).isTrue()

        every { mockPlatformSocket.sendMessage(match { it.contains("\"action\":\"onMessage\"") }) } throws RuntimeException("boom")

        runCatching { subject.sendCardReply(CardTestValues.postbackButtonResponse) }

        assertThat(logSlot.any { it.invoke().contains("requestError") || it.invoke().contains("sendCardReply") }).isTrue()
    }
}
