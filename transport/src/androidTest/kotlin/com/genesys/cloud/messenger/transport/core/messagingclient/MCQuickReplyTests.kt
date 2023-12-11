package com.genesys.cloud.messenger.transport.core.messagingclient

import com.genesys.cloud.messenger.transport.core.ButtonResponse
import com.genesys.cloud.messenger.transport.core.Message
import com.genesys.cloud.messenger.transport.util.Response
import io.mockk.verify
import io.mockk.verifySequence
import org.junit.Test

class MCQuickReplyTests: BaseMessagingClientTest() {

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
                ButtonResponse("text_a", "payload_a", "QuickReply"),
                ButtonResponse("text_b", "payload_b", "QuickReply"),
            ),
            from = Message.Participant(originatingEntity = Message.Participant.OriginatingEntity.Bot),
        )
        subject.connect()

        slot.captured.onMessage(Response.onMessageWithQuickReplies)

        verifySequence {
            connectSequence()
            mockMessageStore.onQuickRepliesReceived(expectedMessage)
        }
    }

    @Test
    fun `when SocketListener invoke onMessage with Structured message that does NOT contains QuickReplies`() {
        subject.connect()

        slot.captured.onMessage(Response.onMessageWithoutQuickReplies)

        verifySequence {
            connectSequence()
        }
        verify(exactly = 0) {
            mockMessageStore.onQuickRepliesReceived(any())
            mockCustomAttributesStore.onSent()
            mockAttachmentHandler.onSent(any())
        }
    }
}