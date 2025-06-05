package transport.core.messagingclient

import com.genesys.cloud.messenger.transport.core.ButtonResponse
import com.genesys.cloud.messenger.transport.utility.QuickReplyTestValues
import io.mockk.every
import io.mockk.verifySequence
import org.junit.Test
import transport.util.Request
import kotlin.test.assertFailsWith

class MCCardsAndCarouselTests : BaseMessagingClientTest() {

    @Test
    fun `when connect and sendPostback with valid button text`() {
        val expectedButtonResponse = ButtonResponse(
            text = "Book Now",
            type = "Postback",
            payload = "Payload value"
        )

        every { mockCustomAttributesStore.getCustomAttributesToSend() } returns emptyMap()
        subject.connect()
        subject.sendPostback(expectedButtonResponse)

        verifySequence {
            connectSequence()
            mockLogger.i(capture(logSlot))
            mockCustomAttributesStore.getCustomAttributesToSend()
            mockMessageStore.preparePostbackMessage(Request.token, expectedButtonResponse, null)
            mockLogger.i(capture(logSlot))
            mockPlatformSocket.sendMessage(any())
        }
    }

    @Test
    fun `when sendPostback without connecting first, throws IllegalStateException`() {
        assertFailsWith<IllegalStateException> {
            subject.sendPostback(QuickReplyTestValues.postbackButtonResponse)
        }
    }
}
