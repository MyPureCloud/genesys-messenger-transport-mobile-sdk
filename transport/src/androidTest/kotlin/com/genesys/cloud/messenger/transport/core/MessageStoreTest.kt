package com.genesys.cloud.messenger.transport.core

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.containsOnly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNotEmpty
import assertk.assertions.isNotEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.isTrue
import com.genesys.cloud.messenger.transport.shyrka.send.Channel
import com.genesys.cloud.messenger.transport.shyrka.send.OnMessageRequest
import com.genesys.cloud.messenger.transport.shyrka.send.TextMessage
import com.genesys.cloud.messenger.transport.util.Request
import io.mockk.Called
import io.mockk.clearMocks
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Test

internal class MessageStoreTest {
    private val givenToken = Request.token
    private val messageSlot = slot<MessageEvent>()
    private val mockMessageListener: ((MessageEvent) -> Unit) = mockk(relaxed = true)

    private val subject = MessageStore(
        token = givenToken,
        log = mockk(relaxed = true),
    ).also {
        it.messageListener = mockMessageListener
    }

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
            assertThat(token).isEqualTo(expectedOnMessageRequest.token)
            assertThat(message).isEqualTo(expectedOnMessageRequest.message)
            assertThat(time).isNull()
        }

        verify { mockMessageListener(capture(messageSlot)) }
        subject.run {
            assertThat(getConversation().first()).isEqualTo(expectedMessage)
            assertThat(pendingMessage.id).isNotEqualTo(expectedMessage.id)
            assertThat((messageSlot.captured as MessageEvent.MessageInserted).message).isEqualTo(
                expectedMessage
            )
        }
    }

    @Test
    fun whenPrepareTwoMessages() {

        subject.prepareMessage("message 1")
        subject.prepareMessage("message 2")

        assertThat(subject.getConversation().size).isEqualTo(2)
    }

    @Test
    fun whenPrepareMessageWithUploadedAttachment() {
        subject.updateAttachmentStateWith(
            attachment(
                state = Attachment.State.Uploaded("http://someurl.com")
            )
        )

        subject.prepareMessage("test message").run {
            assertThat(this.message.content).containsOnly(
                Message.Content(
                    contentType = Message.Content.Type.Attachment,
                    attachment(state = Attachment.State.Uploaded("http://someurl.com"))
                )
            )
        }

        assertThat(subject.pendingMessage.attachments).isEmpty()
    }

    @Test
    fun whenPrepareMessageWithNotUploadedAttachment() {
        subject.updateAttachmentStateWith(
            attachment()
        )

        subject.prepareMessage("test message").run {
            assertThat(message.content).isNotNull()
            assertThat(message.content).isEmpty()
        }
    }

    @Test
    fun whenUpdateInboundMessage() {
        val sentMessageId =
            subject.prepareMessage("test message").message.metadata?.get("customMessageId")
                ?: "empty"
        val givenMessage =
            Message(id = sentMessageId, state = Message.State.Sent, text = "test message")
        clearMocks(mockMessageListener)

        subject.update(givenMessage)

        verify { mockMessageListener(capture(messageSlot)) }
        assertThat(subject.getConversation().first()).isEqualTo(givenMessage)
        assertThat((messageSlot.captured as MessageEvent.MessageUpdated).message).isEqualTo(
            givenMessage
        )
    }

    @Test
    fun whenUpdateOutboundMessage() {
        val givenMessage = outboundMessage()

        subject.update(givenMessage)

        verify { mockMessageListener(capture(messageSlot)) }
        assertThat(subject.getConversation().first()).isEqualTo(givenMessage)
        assertThat(subject.nextPage).isEqualTo(1)
        assertThat((messageSlot.captured as MessageEvent.MessageInserted).message).isEqualTo(
            givenMessage
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
        clearMocks(mockMessageListener)

        subject.update(givenMessage)

        verify { mockMessageListener(capture(messageSlot)) }
        assertThat(subject.getConversation().size).isEqualTo(expectedConversationSize)
        assertThat(subject.getConversation().first()).isEqualTo(givenMessage)
        assertThat((messageSlot.captured as MessageEvent.MessageUpdated).message).isEqualTo(
            givenMessage
        )
    }

    @Test
    fun whenUpdateInboundWithIdThatDoesNotPresentInConversation() {
        val givenMessage =
            Message(id = "randomId", state = Message.State.Sent, text = "test message")

        subject.update(message = givenMessage)

        verify { mockMessageListener(capture(messageSlot)) }
        assertThat(subject.getConversation().size).isEqualTo(1)
        assertThat((messageSlot.captured as MessageEvent.MessageInserted).message).isEqualTo(
            givenMessage
        )
    }

    @Test
    fun whenInsertMoreThanDefaultPageSizeMessages() {
        val expectedConversationSize = 25
        val expectedNextPageIndex = 2
        for (i in 0 until DEFAULT_PAGE_SIZE) {
            subject.update(outboundMessage(messageId = i))
        }

        assertThat(subject.getConversation().size).isEqualTo(expectedConversationSize)
        assertThat(subject.nextPage).isEqualTo(expectedNextPageIndex)
    }

    @Test
    fun whenUpdateWithNewAttachment() {
        val givenAttachment = attachment()
        subject.updateAttachmentStateWith(givenAttachment)

        verify { mockMessageListener(capture(messageSlot)) }
        assertThat(subject.pendingMessage.attachments["given id"]).isEqualTo(givenAttachment)
        assertThat((messageSlot.captured as MessageEvent.AttachmentUpdated).attachment).isEqualTo(
            givenAttachment
        )
    }

    @Test
    fun whenUpdateExistingAttachment() {
        val initialAttachment = attachment(state = Attachment.State.Presigning)
        val updatedAttachment = attachment(state = Attachment.State.Uploading)
        subject.updateAttachmentStateWith(initialAttachment)
        clearMocks(mockMessageListener)

        subject.updateAttachmentStateWith(updatedAttachment)

        verify { mockMessageListener(capture(messageSlot)) }
        assertThat(subject.pendingMessage.attachments["given id"]).isEqualTo(updatedAttachment)
        assertThat((messageSlot.captured as MessageEvent.AttachmentUpdated).attachment).isEqualTo(
            updatedAttachment
        )
    }

    @Test
    fun whenUpdateMessageHistory() {
        val expectedMessageHistory = messageList(2).reversed()
        val expectedConversationSize = 2
        val expectedNextPageIndex = 1

        subject.updateMessageHistory(messageList(2), 2)

        assertThat(subject.startOfConversation).isTrue()
        assertThat(subject.getConversation().size).isEqualTo(expectedConversationSize)
        assertThat(subject.nextPage).isEqualTo(expectedNextPageIndex)
        verify { mockMessageListener(capture(messageSlot)) }
        assertThat((messageSlot.captured as MessageEvent.HistoryFetched).messages).isEqualTo(
            expectedMessageHistory
        )
    }

    @Test
    fun whenUpdateMessageHistoryHasMultiplePages() {
        val givenMessageHistory = messageList(25)
        val givenTotal = DEFAULT_PAGE_SIZE * 2
        val expectedNextPageIndex = 2

        subject.updateMessageHistory(givenMessageHistory, givenTotal)

        assertThat(subject.startOfConversation).isFalse()
        assertThat(subject.nextPage).isEqualTo(expectedNextPageIndex)
    }

    @Test
    fun whenUpdateMessageHistoryContainsMessageThatAlreadyPresentInActiveConversation() {
        val expectedMessage1 = outboundMessage(0)
        val expectedMessage2 = outboundMessage(1)
        val givenMessageHistory = messageList(2).toMutableList()
        subject.update(givenMessageHistory.first())
        clearMocks(mockMessageListener)

        subject.updateMessageHistory(givenMessageHistory, givenMessageHistory.size)

        assertThat(subject.getConversation()).containsExactly(expectedMessage2, expectedMessage1)
        verify { mockMessageListener(capture(messageSlot)) }
        val captured = messageSlot.captured as MessageEvent.HistoryFetched
        assertThat(captured.messages).containsExactly(expectedMessage2)
        assertThat(captured.startOfConversation).isTrue()
    }

    @Test
    fun whenOnMessageErrorAndActiveConversationIsEmpty() {
        subject.onMessageError(ErrorCode.MessageTooLong, "some message")

        verify { mockMessageListener wasNot Called }
        assertThat(subject.nextPage).isEqualTo(1)
        assertThat(subject.getConversation()).isEmpty()
    }

    @Test
    fun whenOnMessageErrorAndActiveConversationDoesNotHaveAnyMessagesWithStateSending() {
        subject.update(outboundMessage())
        clearMocks(mockMessageListener)

        subject.onMessageError(ErrorCode.MessageTooLong, "some message")

        verify { mockMessageListener wasNot Called }
        assertThat(subject.getConversation()).isNotEmpty()
    }

    @Test
    fun whenOnMessageErrorHappensAfterMessageBeingSent() {
        val errorMessage = "some test error message"
        val testMessage = "test message"
        val expectedState = Message.State.Error(
            ErrorCode.MessageTooLong,
            errorMessage
        )
        val expectedMessage =
            subject.pendingMessage.copy(
                state = expectedState,
                text = testMessage
            )
        subject.prepareMessage(testMessage)
        clearMocks(mockMessageListener)

        subject.onMessageError(ErrorCode.MessageTooLong, errorMessage)

        assertThat { subject.getConversation().contains(expectedMessage) }
        verify { mockMessageListener(capture(messageSlot)) }
        (messageSlot.captured as MessageEvent.MessageUpdated).message.run {
            assertThat(this).isEqualTo(expectedMessage)
            assertThat((state as Message.State.Error).code).isEqualTo(expectedState.code)
            assertThat((state as Message.State.Error).message).isEqualTo(expectedState.message)
        }
    }

    @Test
    fun whenMessageListenerNotSet() {
        subject.messageListener = null

        subject.prepareMessage("test message")

        verify { mockMessageListener wasNot Called }
    }

    @Test
    fun whenInvalidateConversationCache() {
        val expectedNextPage = 1
        subject.update(outboundMessage())

        subject.invalidateConversationCache()

        subject.run {
            assertThat(startOfConversation).isFalse()
            assertThat(getConversation()).isEmpty()
            assertThat(nextPage).isEqualTo(expectedNextPage)
        }
    }

    @Test
    fun whenPrepareMessageWithChannelThatHasCustomAttributes() {
        val expectedMessage =
            subject.pendingMessage.copy(state = Message.State.Sending, text = "test message")
        val expectedOnMessageRequest = OnMessageRequest(
            givenToken,
            message = TextMessage(
                "test message",
                metadata = mapOf("customMessageId" to expectedMessage.id),
                channel = Channel(Channel.Metadata(mapOf("A" to "B"))),
            ),
        )

        val onMessageRequest =
            subject.prepareMessage("test message", Channel(Channel.Metadata(mapOf("A" to "B"))))

        verify { mockMessageListener(capture(messageSlot)) }
        onMessageRequest.run {
            assertThat(token).isEqualTo(expectedOnMessageRequest.token)
            assertThat(message).isEqualTo(expectedOnMessageRequest.message)
            assertThat(message.channel).isEqualTo(expectedOnMessageRequest.message.channel)
            assertThat(time).isNull()
        }
        subject.run {
            assertThat(getConversation().first()).isEqualTo(expectedMessage)
            assertThat(pendingMessage.id).isNotEqualTo(expectedMessage)
            assertThat((messageSlot.captured as MessageEvent.MessageInserted).message).isEqualTo(
                expectedMessage
            )
        }
    }

    private fun outboundMessage(messageId: Int = 0): Message = Message(
        id = "$messageId",
        direction = Message.Direction.Outbound,
        state = Message.State.Sent,
        text = "message from agent number $messageId",
        timeStamp = 100 * messageId.toLong(),
    )

    private fun attachment(
        id: String = "given id",
        state: Attachment.State = Attachment.State.Presigning,
    ) = Attachment(id, "file.png", state)

    private fun messageList(size: Int = 5): List<Message> {
        val messageList = mutableListOf<Message>()
        for (i in 0 until size) {
            messageList.add(outboundMessage(i))
        }
        return messageList
    }
}
