package com.genesys.cloud.messenger.transport.core

import assertk.assertThat
import com.genesys.cloud.messenger.transport.shyrka.send.OnMessageRequest
import com.genesys.cloud.messenger.transport.shyrka.send.TextMessage
import io.mockk.Called
import io.mockk.clearMocks
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

internal class MessageStoreTest {
    private val givenToken = "00000000-0000-0000-0000-000000000000"
    private val messageSlot = slot<MessageEvent>()
    private val mockMessageDispatcher: MessageDispatcher = mockk(relaxed = true)

    private val subject = MessageStore(mockMessageDispatcher, givenToken, mockk(relaxed = true))

    @Test
    fun whenPrepareMessage() {
        val expectedMessage =
            subject.pendingMessage.copy(state = Message.State.Sending, text = "test message")
        val expectedOnMessageRequest = OnMessageRequest(
            givenToken,
            message = TextMessage(
                "test message",
                metadata = mapOf("customMessageId" to expectedMessage.id)
            ),
        )

        subject.prepareMessage("test message").run {
            assertEquals(expectedOnMessageRequest.token, token)
            assertEquals(expectedOnMessageRequest.message, message)
            assertNull(time)
            assertNotNull(attachmentIds)
            assertTrue { attachmentIds.isEmpty() }
        }

        assertEquals(expectedMessage, subject.getConversation()[0])
        assertNotEquals(expectedMessage.id, subject.pendingMessage.id)
        verify { mockMessageDispatcher.dispatch(capture(messageSlot)) }
        assertEquals(
            expectedMessage,
            (messageSlot.captured as MessageEvent.MessageInserted).message
        )
    }

    @Test
    fun whenPrepareTwoMessages() {

        subject.prepareMessage("message 1")
        subject.prepareMessage("message 2")

        assertTrue { subject.getConversation().size == 2 }
    }

    @Test
    fun whenPrepareMessageWithUploadedAttachment() {
        subject.updateAttachmentStateWith(
            attachment(
                state = Attachment.State.Uploaded("http://someurl.com")
            )
        )

        subject.prepareMessage("test message").run {
            assertNotNull(attachmentIds)
            assertTrue { attachmentIds.contains("given id") }
        }
        assertTrue { subject.pendingMessage.attachments.isEmpty() }
    }

    @Test
    fun whenPrepareMessageWithNotUploadedAttachment() {
        subject.updateAttachmentStateWith(
            attachment()
        )

        subject.prepareMessage("test message").run {
            assertNotNull(attachmentIds)
            assertTrue { attachmentIds.isEmpty() }
        }
    }

    @Test
    fun whenUpdateInboundMessage() {
        val sentMessageId =
            subject.prepareMessage("test message").message.metadata?.get("customMessageId")
                ?: "empty"
        val givenMessage =
            Message(id = sentMessageId, state = Message.State.Sent, text = "test message")
        clearMocks(mockMessageDispatcher)

        subject.update(givenMessage)

        assertEquals(givenMessage, subject.getConversation()[0])
        verify { mockMessageDispatcher.dispatch(capture(messageSlot)) }
        assertEquals(
            givenMessage,
            (messageSlot.captured as MessageEvent.MessageUpdated).message
        )
    }

    @Test
    fun whenUpdateOutboundMessage() {
        val givenMessage = outboundMessage()

        subject.update(givenMessage)

        assertEquals(givenMessage, subject.getConversation()[0])
        assertEquals(1, subject.nextPage)
        verify { mockMessageDispatcher.dispatch(capture(messageSlot)) }
        assertEquals(
            givenMessage,
            (messageSlot.captured as MessageEvent.MessageInserted).message
        )
    }

    @Test
    fun whenUpdateInboundAndThenOutboundMessage() {
        val sentMessageId =
            subject.prepareMessage("test message").message.metadata?.get("customMessageId")
                ?: "empty"
        val expectedConversationSize = 2
        val givenMessage =
            Message(id = sentMessageId, state = Message.State.Sent, text = "test message")
        subject.update(outboundMessage())
        clearMocks(mockMessageDispatcher)

        subject.update(givenMessage)

        assertEquals(expectedConversationSize, subject.getConversation().size)
        assertEquals(givenMessage, subject.getConversation()[1])
        verify { mockMessageDispatcher.dispatch(capture(messageSlot)) }
        assertEquals(
            givenMessage,
            (messageSlot.captured as MessageEvent.MessageUpdated).message
        )
    }

    @Test
    fun whenUpdateInboundWithIdThatDoesNotPresentInConversation() {
        subject.update(message = Message("some id"))

        assertTrue { subject.getConversation().isEmpty() }
        verify(exactly = 0) { mockMessageDispatcher.dispatch(any()) }
    }

    @Test
    fun whenInsertMoreThanDefaultPageSizeMessages() {
        val expectedConversationSize = 25
        val expectedNextPageIndex = 2
        for (i in 0 until DEFAULT_PAGE_SIZE) {
            subject.update(outboundMessage(messageId = i))
        }

        assertEquals(expectedConversationSize, subject.getConversation().size)
        assertEquals(expectedNextPageIndex, subject.nextPage)
    }

    @Test
    fun whenUpdateWithNewAttachment() {
        val givenAttachment = attachment()
        subject.updateAttachmentStateWith(givenAttachment)

        assertEquals(givenAttachment, subject.pendingMessage.attachments["given id"])
        verify { mockMessageDispatcher.dispatch(capture(messageSlot)) }
        assertEquals(
            givenAttachment,
            (messageSlot.captured as MessageEvent.AttachmentUpdated).attachment
        )
    }

    @Test
    fun whenUpdateExistingAttachment() {
        val initialAttachment = attachment(state = Attachment.State.Presigning)
        val updatedAttachment = attachment(state = Attachment.State.Uploading)
        subject.updateAttachmentStateWith(initialAttachment)
        clearMocks(mockMessageDispatcher)

        subject.updateAttachmentStateWith(updatedAttachment)

        assertEquals(updatedAttachment, subject.pendingMessage.attachments["given id"])
        verify { mockMessageDispatcher.dispatch(capture(messageSlot)) }
        assertEquals(
            updatedAttachment,
            (messageSlot.captured as MessageEvent.AttachmentUpdated).attachment
        )
    }

    @Test
    fun whenUpdateMessageHistory() {
        val givenMessageHistory = messageList(5)
        val givenTotal = 5
        val expectedConversationSize = 5
        val expectedNextPageIndex = 1

        subject.updateMessageHistory(givenMessageHistory, givenTotal)

        assertTrue { subject.startOfConversation }
        assertEquals(expectedConversationSize, subject.getConversation().size)
        assertEquals(expectedNextPageIndex, subject.nextPage)
        verify { mockMessageDispatcher.dispatch(capture(messageSlot)) }
        assertEquals(
            givenMessageHistory,
            (messageSlot.captured as MessageEvent.HistoryFetched).messages
        )
    }

    @Test
    fun whenUpdateMessageHistoryHasMultiplePages() {
        val givenMessageHistory = messageList(25)
        val givenTotal = DEFAULT_PAGE_SIZE * 2
        val expectedNextPageIndex = 2

        subject.updateMessageHistory(givenMessageHistory, givenTotal)

        assertFalse { subject.startOfConversation }
        assertEquals(expectedNextPageIndex, subject.nextPage)
    }

    @Test
    fun whenUpdateMessageHistoryContainsMessageThatAlreadyPresentInActiveConversation() {
        val givenMessageHistory = messageList(2).toMutableList()
        val expectedMessageHistory = messageList(2)
        subject.update(outboundMessage(messageId = 123).also { givenMessageHistory.add(0, it) })
        clearMocks(mockMessageDispatcher)

        subject.updateMessageHistory(givenMessageHistory, givenMessageHistory.size)

        assertEquals(givenMessageHistory, subject.getConversation())
        verify { mockMessageDispatcher.dispatch(capture(messageSlot)) }
        assertEquals(
            expectedMessageHistory,
            (messageSlot.captured as MessageEvent.HistoryFetched).messages
        )
        assertTrue { (messageSlot.captured as MessageEvent.HistoryFetched).startOfConversation }
    }

    @Test
    fun whenOnMessageErrorAndActiveConversationIsEmpty() {
        subject.onMessageError(ErrorCode.MessageTooLong, "some message")

        verify { mockMessageDispatcher wasNot Called }
        assertEquals(1, subject.nextPage)
        assertTrue { subject.getConversation().isEmpty() }
    }

    @Test
    fun whenOnMessageErrorAndActiveConversationDoesNotHaveAnyMessagesWithStateSending() {
        subject.update(outboundMessage())
        clearMocks(mockMessageDispatcher)

        subject.onMessageError(ErrorCode.MessageTooLong, "some message")

        verify { mockMessageDispatcher wasNot Called }
        assertTrue { subject.getConversation().isNotEmpty() }
    }

    @Test
    fun whenOnMessageErrorHappensAfterMessageBeingSent() {
        val errorMessage = "some test error message"
        val testMessage = "test message"
        val expectedMessage =
            subject.pendingMessage.copy(
                state = Message.State.Error(
                    ErrorCode.MessageTooLong,
                    errorMessage
                ),
                text = testMessage
            )
        subject.prepareMessage(testMessage)
        clearMocks(mockMessageDispatcher)

        subject.onMessageError(ErrorCode.MessageTooLong, errorMessage)

        assertThat { subject.getConversation().contains(expectedMessage) }
        verify { mockMessageDispatcher.dispatch(capture(messageSlot)) }
        assertEquals(
            expectedMessage,
            (messageSlot.captured as MessageEvent.MessageUpdated).message
        )
    }

    private fun outboundMessage(messageId: Int = 0): Message = Message(
        id = "$messageId",
        direction = Message.Direction.Outbound,
        state = Message.State.Sent,
        text = "message from agent number $messageId",
        timeStamp = "$messageId-09-02T15:05:57.293Z",
    )

    private fun attachment(
        id: String = "given id",
        state: Attachment.State = Attachment.State.Presigning
    ) = Attachment(id, "file.png", state)

    private fun messageList(size: Int = 5): List<Message> {
        val messageList = mutableListOf<Message>()
        for (i in 0 until size) {
            messageList.add(outboundMessage(i))
        }
        return messageList
    }
}
