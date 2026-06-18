package transport.core.messagingclient

import com.genesys.cloud.messenger.transport.core.Message
import com.genesys.cloud.messenger.transport.shyrka.send.Channel
import com.genesys.cloud.messenger.transport.shyrka.send.OnMessageRequest
import com.genesys.cloud.messenger.transport.shyrka.send.TextMessage
import com.genesys.cloud.messenger.transport.utility.TimeSlotPickerTestValues
import io.mockk.every
import io.mockk.verify
import io.mockk.verifySequence
import org.junit.Test
import transport.util.Request
import kotlin.test.assertFailsWith

class MessagingClientTimeSlotTest : BaseMessagingClientTest() {

    private val givenTimeSlotResponse = TimeSlotPickerTestValues.timeSlotButtonResponse

    @Test
    fun `when submitTimeSlot() but client is not configured`() {
        assertFailsWith<IllegalStateException> {
            subject.submitTimeSlot(givenTimeSlotResponse)
        }
    }

    @Test
    fun `when connect() and then submitTimeSlot() but no custom attributes`() {
        every { mockCustomAttributesStore.getCustomAttributesToSend() } returns emptyMap()
        subject.connect()

        subject.submitTimeSlot(givenTimeSlotResponse)

        verifySequence {
            connectSequence()
            mockLogger.i(capture(logSlot))
            mockCustomAttributesStore.getCustomAttributesToSend()
            mockMessageStore.prepareTimeSlotSubmissionMessageWith(
                Request.token,
                givenTimeSlotResponse,
                null
            )
            mockLogger.i(capture(logSlot))
            mockPlatformSocket.sendMessage(match { Request.isTimeSlotRequest(it) })
        }

        verify(exactly = 0) {
            mockAttachmentHandler.onSending()
        }
    }

    @Test
    fun `when connect() and then submitTimeSlot() with custom attributes`() {
        val expectedCustomAttributes = mapOf("source" to "datepicker")
        val expectedChannel = Channel(Channel.Metadata(expectedCustomAttributes))

        every { mockCustomAttributesStore.getCustomAttributesToSend() } returns expectedCustomAttributes
        every {
            mockMessageStore.prepareTimeSlotSubmissionMessageWith(
                Request.token,
                givenTimeSlotResponse,
                expectedChannel
            )
        } returns OnMessageRequest(
            token = Request.token,
            message = TextMessage(
                text = "",
                content = listOf(
                    Message.Content(
                        contentType = Message.Content.Type.ButtonResponse,
                        buttonResponse = givenTimeSlotResponse
                    )
                ),
                channel = expectedChannel
            )
        )

        subject.connect()
        subject.submitTimeSlot(givenTimeSlotResponse)

        verifySequence {
            connectSequence()
            mockLogger.i(capture(logSlot))
            mockCustomAttributesStore.getCustomAttributesToSend()
            mockCustomAttributesStore.onSending()
            mockMessageStore.prepareTimeSlotSubmissionMessageWith(
                Request.token,
                givenTimeSlotResponse,
                expectedChannel
            )
            mockLogger.i(capture(logSlot))
            mockPlatformSocket.sendMessage(
                match {
                    it.contains(""""contentType":"ButtonResponse"""") &&
                        it.contains(""""action":"onMessage"""") &&
                        it.contains(""""customAttributes":{"source":"datepicker"}""")
                }
            )
        }
    }

    @Test
    fun `when error response received after submitTimeSlot() then onMessageError is called`() {
        every {
            mockPlatformSocket.sendMessage(match { Request.isTimeSlotRequest(it) })
        } answers {
            slot.captured.onMessage(transport.util.Response.tooManyRequests)
        }

        subject.connect()
        subject.submitTimeSlot(givenTimeSlotResponse)

        verify {
            mockMessageStore.onMessageError(any(), any())
        }
    }
}
