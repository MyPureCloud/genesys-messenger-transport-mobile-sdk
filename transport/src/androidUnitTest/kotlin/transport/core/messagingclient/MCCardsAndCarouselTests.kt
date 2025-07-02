package transport.core.messagingclient

import com.genesys.cloud.messenger.transport.core.Message
import com.genesys.cloud.messenger.transport.shyrka.WebMessagingJson
import com.genesys.cloud.messenger.transport.shyrka.send.OnMessageRequest
import com.genesys.cloud.messenger.transport.shyrka.send.StructuredMessage
import com.genesys.cloud.messenger.transport.utility.CardTestValues
import io.mockk.every
import io.mockk.verify
import io.mockk.verifySequence
import kotlinx.serialization.encodeToString
import org.junit.Test
import transport.util.Request
import kotlin.test.assertFailsWith

class MCCardsAndCarouselTests : BaseMessagingClientTest() {

    @Test
    fun `when connect() and then sendCardReply() but no custom attributes`() {
        val givenPostbackResponse = CardTestValues.postbackButtonResponse
        val expectedCustomMessageId = CardTestValues.customMessageId
        val expectedMessage = StructuredMessage(
            text = givenPostbackResponse.text,
            metadata = mapOf("customMessageId" to expectedCustomMessageId),
            content = listOf(
                Message.Content(
                    contentType = Message.Content.Type.ButtonResponse,
                    buttonResponse = givenPostbackResponse
                )
            ),
            channel = null
        )
        val expectedRequest = OnMessageRequest(
            token = Request.token,
            message = expectedMessage
        )

        every { mockCustomAttributesStore.getCustomAttributesToSend() } returns emptyMap()
        every {
            mockMessageStore.preparePostbackMessage(Request.token, givenPostbackResponse, null)
        } returns expectedRequest

        subject.connect()
        subject.sendCardReply(givenPostbackResponse)

        verifySequence {
            connectSequence()
            mockLogger.i(capture(logSlot))
            mockCustomAttributesStore.getCustomAttributesToSend()
            mockMessageStore.preparePostbackMessage(Request.token, givenPostbackResponse, null)
            mockLogger.i(capture(logSlot))
            mockPlatformSocket.sendMessage(WebMessagingJson.json.encodeToString(expectedRequest))
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
}
