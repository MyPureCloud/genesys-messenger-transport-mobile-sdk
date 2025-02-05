package transport.core.messagingclient

import com.genesys.cloud.messenger.transport.core.Message
import com.genesys.cloud.messenger.transport.core.messagingclient.BaseMessagingClientTest
import com.genesys.cloud.messenger.transport.utility.QuickReplyTestValues
import io.mockk.every
import io.mockk.verify
import io.mockk.verifySequence
import org.junit.Test
import transport.util.Request
import transport.util.Response
import kotlin.test.assertFailsWith

class MCQuickReplyTests : BaseMessagingClientTest() {

    @Test
    fun `when SocketListener invoke onMessage with Structured message that contains QuickReplies`() {
        val expectedMessage = Message(
            id = "msg_id",
            direction = Message.Direction.Outbound,
            state = Message.State.Sent,
            messageType = Message.Type.QuickReply,
            text = "Hi",
            timeStamp = null,
            quickReplies = listOf(
                QuickReplyTestValues.buttonResponse_a,
                QuickReplyTestValues.buttonResponse_b,
            ),
            from = Message.Participant(originatingEntity = Message.Participant.OriginatingEntity.Bot),
        )
        subject.connect()

        slot.captured.onMessage(Response.onMessageWithQuickReplies)

        verifySequence {
            connectSequence()
            mockMessageStore.update(expectedMessage)
        }
    }

    @Test
    fun `when SocketListener invoke onMessage with Structured message that does NOT contains QuickReplies`() {
        subject.connect()

        slot.captured.onMessage(Response.onMessageWithoutQuickReplies)

        verifySequence {
            connectSequence()
            mockLogger.w(capture(logSlot))
        }
        verify(exactly = 0) {
            mockMessageStore.update(any())
            mockCustomAttributesStore.onSent()
            mockAttachmentHandler.onSent(any())
        }
    }

    @Test
    fun `when connect() and then sendQuickReply() but no custom attributes`() {
        val expectedButtonResponse = QuickReplyTestValues.buttonResponse_a
        every { mockCustomAttributesStore.getCustomAttributesToSend() } returns emptyMap()
        subject.connect()

        subject.sendQuickReply(QuickReplyTestValues.buttonResponse_a)

        verifySequence {
            connectSequence()
            mockLogger.i(capture(logSlot))
            mockCustomAttributesStore.getCustomAttributesToSend()
            mockMessageStore.prepareMessageWith(Request.token, expectedButtonResponse, null)
            mockLogger.i(capture(logSlot))
            mockPlatformSocket.sendMessage(Request.quickReplyWith())
        }

        verify(exactly = 0) {
            mockAttachmentHandler.onSending()
        }
    }

    @Test
    fun `when sendQuickReply() but client is not configured`() {
        assertFailsWith<IllegalStateException> {
            subject.sendQuickReply(QuickReplyTestValues.buttonResponse_a)
        }
    }
}
