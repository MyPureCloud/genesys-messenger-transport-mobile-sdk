package transport.core.messagingclient

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import com.genesys.cloud.messenger.transport.core.ErrorCode
import com.genesys.cloud.messenger.transport.core.Message
import com.genesys.cloud.messenger.transport.core.Message.Direction
import com.genesys.cloud.messenger.transport.core.Message.Participant
import com.genesys.cloud.messenger.transport.core.Message.State
import com.genesys.cloud.messenger.transport.core.Message.Type
import com.genesys.cloud.messenger.transport.core.MessageEvent
import com.genesys.cloud.messenger.transport.core.MessagingClient
import com.genesys.cloud.messenger.transport.core.events.Event
import com.genesys.cloud.messenger.transport.core.isClosed
import com.genesys.cloud.messenger.transport.util.extensions.sanitizeText
import com.genesys.cloud.messenger.transport.util.logs.LogMessages
import com.genesys.cloud.messenger.transport.utility.MessageValues
import io.mockk.every
import io.mockk.verify
import io.mockk.verifySequence
import org.junit.Test
import transport.util.Request
import transport.util.Response
import kotlin.test.assertFailsWith

class MCMessageTests : BaseMessagingClientTest() {

    @Test
    fun `when messageListener is set`() {
        val givenMessageListener: (MessageEvent) -> Unit = {}

        subject.messageListener = givenMessageListener

        verify {
            mockMessageStore.messageListener = givenMessageListener
        }
    }

    @Test
    fun `when messageListener is not set`() {
        assertThat(subject.messageListener).isNull()
    }

    @Test
    fun `when getPendingMessage`() {
        subject.pendingMessage

        verify {
            mockMessageStore.pendingMessage
        }
    }

    @Test
    fun `when connect and then sendMessage()`() {
        every { mockPlatformSocket.sendMessage(Request.textMessage()) } answers {
            slot.captured.onMessage(Response.onMessage())
        }
        val expectedMessageRequest =
            """{"token":"${Request.token}","message":{"text":"${MessageValues.TEXT.sanitizeText()}","type":"Text"},"action":"onMessage"}"""
        val expectedMessage = Message(
            id = "some_custom_message_id",
            state = State.Sent,
            messageType = Type.Text,
            type = "Text",
            text = MessageValues.TEXT,
            timeStamp = 1661196266704,
        )
        subject.connect()

        subject.sendMessage(MessageValues.TEXT)

        verifySequence {
            connectSequence()
            mockLogger.i(capture(logSlot))
            mockCustomAttributesStore.add(emptyMap())
            mockCustomAttributesStore.getCustomAttributesToSend()
            mockMessageStore.prepareMessage(Request.token, MessageValues.TEXT)
            mockAttachmentHandler.onSending()
            mockLogger.i(capture(logSlot))
            mockPlatformSocket.sendMessage(expectedMessageRequest)
            mockMessageStore.update(expectedMessage)
            mockCustomAttributesStore.onSent()
            mockAttachmentHandler.onSent(emptyMap())
        }

        verify(exactly = 0) {
            mockCustomAttributesStore.onSending()
            mockEventHandler.onEvent(Event.HealthChecked)
        }

        val sanitizedText = MessageValues.TEXT.sanitizeText()

        assertThat(logSlot[0].invoke()).isEqualTo(LogMessages.CONNECT)
        assertThat(logSlot[1].invoke()).isEqualTo(LogMessages.configureSession(Request.token, false))
        assertThat(logSlot[2].invoke()).isEqualTo(LogMessages.sendMessage(sanitizedText))
        assertThat(logSlot[3].invoke()).isEqualTo(LogMessages.WILL_SEND_MESSAGE)
    }

    @Test
    fun `when invalidateConversationCache`() {
        subject.invalidateConversationCache()

        verify {
            mockMessageStore.invalidateConversationCache()
        }
    }

    @Test
    fun `when sendMessage without connection`() {
        assertFailsWith<IllegalStateException> {
            subject.sendMessage("foo")
        }
    }

    @Test
    fun `when SocketListener invoke onMessage with MessageTooLong error message`() {
        subject.connect()

        slot.captured.onMessage(Response.messageTooLong)

        verifySequence {
            connectSequence()
            mockCustomAttributesStore.onMessageError()
            mockMessageStore.onMessageError(ErrorCode.MessageTooLong, "message too long")
            mockAttachmentHandler.onMessageError(ErrorCode.MessageTooLong, "message too long")
        }
    }

    @Test
    fun `when SocketListener invoke onMessage with TooManyRequests error message`() {
        subject.connect()

        slot.captured.onMessage(Response.tooManyRequests)

        verifySequence {
            connectSequence()
            mockCustomAttributesStore.onMessageError()
            mockMessageStore.onMessageError(
                ErrorCode.RequestRateTooHigh,
                "Message rate too high for this session. Retry after 3 seconds."
            )
            mockAttachmentHandler.onMessageError(
                ErrorCode.RequestRateTooHigh,
                "Message rate too high for this session. Retry after 3 seconds."
            )
        }
        verify(exactly = 0) {
            mockCustomAttributesStore.onError()
        }
    }

    @Test
    fun `when SocketListener invoke onMessage with CustomAttributesTooLarge error message`() {
        val expectedErrorMessage = "Custom Attributes in channel metadata is larger than 2048 bytes"
        subject.connect()

        slot.captured.onMessage(Response.customAttributeSizeTooLarge)

        verifySequence {
            connectSequence()
            mockCustomAttributesStore.onError()
            mockMessageStore.onMessageError(
                ErrorCode.CustomAttributeSizeTooLarge,
                expectedErrorMessage
            )
            mockAttachmentHandler.onMessageError(
                ErrorCode.CustomAttributeSizeTooLarge,
                expectedErrorMessage
            )
        }
        verify(exactly = 0) {
            mockEventHandler.onEvent(any())
            mockCustomAttributesStore.onMessageError()
        }
    }

    @Test
    fun `when socket throws an error while current state is Closing`() {
        val expectedCode = 1000
        val expectedReason = "The user has closed the connection."
        every { mockPlatformSocket.closeSocket(any(), any()) } answers {
            if (subject.currentState is MessagingClient.State.Closing) {
                slot.captured.onFailure(Exception(), ErrorCode.WebsocketError)
            }
        }

        subject.connect()
        subject.disconnect()

        assertThat(subject.currentState).isClosed(expectedCode, expectedReason)
        verify {
            connectSequence()
            disconnectSequence()
        }

        assertThat(logSlot[0].invoke()).isEqualTo(LogMessages.CONNECT)
        assertThat(logSlot[1].invoke()).isEqualTo(LogMessages.configureSession(Request.token, false))
        assertThat(logSlot[2].invoke()).isEqualTo(LogMessages.DISCONNECT)
        assertThat(logSlot[3].invoke()).isEqualTo(LogMessages.FORCE_CLOSE_WEB_SOCKET)
        assertThat(logSlot[4].invoke()).isEqualTo(LogMessages.CLEAR_CONVERSATION_HISTORY)
    }

    @Test
    fun `when SocketListener invoke onMessage with Outbound text message`() {
        val expectedMessage = Message(
            id = "some_custom_message_id",
            direction = Direction.Outbound,
            state = State.Sent,
            messageType = Type.Text,
            type = "Text",
            text = "Hello world!",
            timeStamp = 1661196266704,
            from = Participant(originatingEntity = Participant.OriginatingEntity.Unknown)
        )
        subject.connect()

        slot.captured.onMessage(Response.onMessage(Direction.Outbound))

        verifySequence {
            connectSequence()
            mockMessageStore.update(expectedMessage)
        }
        verify(exactly = 0) {
            mockCustomAttributesStore.onSent()
            mockAttachmentHandler.onSent(any())
        }
    }
}
