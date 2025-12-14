package transport.core.messagingclient

import assertk.assertThat
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

        subject.connect()
        subject.sendCardReply(givenPostbackResponse)

        verifySequence {
            connectSequence()
            mockLogger.i(capture(logSlot))
            mockCustomAttributesStore.getCustomAttributesToSend()
            mockMessageStore.preparePostbackMessage(Request.token, givenPostbackResponse, null)
            mockLogger.i(capture(logSlot))
            mockPlatformSocket.sendMessage(match { Request.isPostbackRequest(it) })
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

        every {
            mockMessageStore.preparePostbackMessage(
                Request.token,
                expectedButtonResponse,
                expectedChannel
            )
        } returns
            OnMessageRequest(
                token = Request.token,
                message =
                    StructuredMessage(
                        text = expectedButtonResponse.text,
                        content =
                            listOf(
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
            mockPlatformSocket.sendMessage(
                match {
                    it.contains(""""text":"${expectedButtonResponse.text}"""") &&
                        it.contains(""""type":"Structured"""") &&
                        it.contains(""""action":"onMessage"""") &&
                        it.contains(""""customAttributes":{"source":"card"}""")
                }
            )
        }
    }

    @Test
    fun `when SocketListener invoke onMessage with Structured message that contains Postback card reply`() {
        val expectedCard =
            Message.Card(
                title = "Title",
                description = "Description",
                imageUrl = "http://image.com/image.png",
                actions =
                    listOf(
                        ButtonResponse(
                            type = "Button",
                            text = "Select this option",
                            payload = "postback_payload"
                        )
                    )
            )

        val expectedMessage =
            Message(
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

        every {
            mockPlatformSocket.sendMessage(match { Request.isPostbackRequest(it) })
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
    fun `when sendCardReply() with Link button then payload value is not serialized`() {
        val givenLinkButton =
            ButtonResponse(
                text = CardTestValues.text,
                payload = "",
                type = CardTestValues.LINK_TYPE
            )
        val givenCustomAttributes = mapOf("source" to "card")
        val givenChannel = Channel(Channel.Metadata(givenCustomAttributes))

        every { mockCustomAttributesStore.getCustomAttributesToSend() } returns givenCustomAttributes
        every {
            mockMessageStore.preparePostbackMessage(
                Request.token,
                match { it.type == CardTestValues.LINK_TYPE },
                givenChannel
            )
        } returns
            OnMessageRequest(
                token = Request.token,
                message =
                    StructuredMessage(
                        text = givenLinkButton.text,
                        content =
                            listOf(
                                Message.Content(
                                    contentType = Message.Content.Type.ButtonResponse,
                                    buttonResponse = givenLinkButton
                                )
                            ),
                        channel = givenChannel
                    )
            )

        val expectedJson =
            listOf(
                "\"type\":\"Structured\"",
                "\"type\":\"${CardTestValues.LINK_TYPE}\"",
                "\"text\":\"${CardTestValues.text}\""
            )
        val nonEmptyPayloadRegex = Regex("\"payload\":\"[^\"]+\"")

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
                givenChannel
            )
            mockLogger.i(any())
            mockPlatformSocket.sendMessage(
                match { json ->
                    expectedJson.all { json.contains(it) } &&
                        !nonEmptyPayloadRegex.containsMatchIn(json)
                }
            )
        }
    }

    @Test
    fun `when sendCardReply then logs success and then error`() {
        every { mockLogger.i(capture(logSlot)) } answers { }
        every { mockLogger.e(capture(logSlot)) } answers { }
        subject.connect()

        val expectedSuccessMarker = "sendCardReply"
        val expectedErrorMarker = "requestError"

        every { mockPlatformSocket.sendMessage(match { it.contains("\"action\":\"onMessage\"") }) } returns Unit

        subject.sendCardReply(CardTestValues.postbackButtonResponse)

        verify(atLeast = 1) { mockLogger.i(any()) }
        var allLogs = logSlot.joinToString("\n") { it.invoke() }
        assertThat(allLogs.contains(expectedSuccessMarker)).isTrue()

        every { mockPlatformSocket.sendMessage(match { it.contains("\"action\":\"onMessage\"") }) } throws RuntimeException("boom")

        runCatching { subject.sendCardReply(CardTestValues.postbackButtonResponse) }

        verify(atLeast = 1) { mockLogger.i(any()) }

        allLogs = logSlot.joinToString("\n") { it.invoke() }
        assertThat(allLogs.contains(expectedErrorMarker) || allLogs.contains(expectedSuccessMarker)).isTrue()
    }
}
