package com.genesys.cloud.messenger.transport.core

import assertk.assertThat
import assertk.assertions.contains
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
import com.genesys.cloud.messenger.transport.core.Message.Content
import com.genesys.cloud.messenger.transport.core.Message.Direction
import com.genesys.cloud.messenger.transport.core.Message.Participant
import com.genesys.cloud.messenger.transport.core.Message.State
import com.genesys.cloud.messenger.transport.core.Message.Type
import com.genesys.cloud.messenger.transport.shyrka.send.Channel
import com.genesys.cloud.messenger.transport.shyrka.send.OnMessageRequest
import com.genesys.cloud.messenger.transport.shyrka.send.TextMessage
import com.genesys.cloud.messenger.transport.util.logs.Log
import com.genesys.cloud.messenger.transport.util.logs.LogMessages
import com.genesys.cloud.messenger.transport.utility.AttachmentValues
import com.genesys.cloud.messenger.transport.utility.QuickReplyTestValues
import com.genesys.cloud.messenger.transport.utility.TestValues
import io.mockk.Called
import io.mockk.clearMocks
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Test

internal class MessageStoreTest {
    private val givenToken = TestValues.Token
    private val messageSlot = slot<MessageEvent>()
    private val mockLogger: Log = mockk(relaxed = true)
    private val logSlot = mutableListOf<() -> String>()
    private val mockMessageListener: ((MessageEvent) -> Unit) = mockk(relaxed = true)

    private val subject = MessageStore(mockLogger).also {
        it.messageListener = mockMessageListener
    }

    @Test
    fun `when prepareMessage()`() {
        val expectedMessage =
            subject.pendingMessage.copy(state = State.Sending, text = "test message")
        val expectedOnMessageRequest = OnMessageRequest(
            givenToken,
            message = TextMessage(
                "test message",
                metadata = mapOf("customMessageId" to expectedMessage.id)
            ),
        )

        subject.prepareMessage(TestValues.Token, "test message").run {
            assertThat(token).isEqualTo(expectedOnMessageRequest.token)
            assertThat(message).isEqualTo(expectedOnMessageRequest.message)
            assertThat(time).isNull()
        }

        verify {
            mockLogger.i(capture(logSlot))
            mockMessageListener.invoke(capture(messageSlot))
        }
        subject.run {
            assertThat(getConversation().first()).isEqualTo(expectedMessage)
            assertThat(pendingMessage.id).isNotEqualTo(expectedMessage.id)
            assertThat((messageSlot.captured as MessageEvent.MessageInserted).message).isEqualTo(
                expectedMessage
            )
            assertThat(logSlot[0].invoke()).isEqualTo(LogMessages.messagePreparedToSend(expectedMessage))
        }
    }

    @Test
    fun `when prepareMessage() is called twice`() {

        subject.prepareMessage(TestValues.Token, "message 1")
        subject.prepareMessage(TestValues.Token, "message 2")

        assertThat(subject.getConversation().size).isEqualTo(2)
    }

    @Test
    fun `when prepareMessage() with uploaded attachment`() {
        subject.updateAttachmentStateWith(
            attachment(
                state = Attachment.State.Uploaded("http://someurl.com")
            )
        )

        subject.prepareMessage(TestValues.Token, "test message").run {
            assertThat(this.message.content).containsOnly(
                Content(
                    contentType = Content.Type.Attachment,
                    attachment(state = Attachment.State.Uploaded("http://someurl.com"))
                )
            )
        }
        assertThat(subject.pendingMessage.attachments).isEmpty()
    }

    @Test
    fun `when prepareMessage() with NOT uploaded attachment`() {
        subject.updateAttachmentStateWith(attachment())

        subject.prepareMessage(TestValues.Token, "test message").run {
            assertThat(message.content).isNotNull()
            assertThat(message.content).isEmpty()
        }
    }

    @Test
    fun `when update() inbound message`() {
        val sentMessageId =
            subject.prepareMessage(TestValues.Token, "test message").message.metadata?.get("customMessageId")
                ?: "empty"
        val givenMessage =
            Message(id = sentMessageId, state = State.Sent, text = "test message")
        clearMocks(mockMessageListener)

        subject.update(givenMessage)

        verify {
            mockLogger.i(capture(logSlot))
            mockMessageListener.invoke(capture(messageSlot))
        }
        assertThat(subject.getConversation().first()).isEqualTo(givenMessage)
        assertThat((messageSlot.captured as MessageEvent.MessageUpdated).message).isEqualTo(
            givenMessage
        )
        assertThat(logSlot[1].invoke()).isEqualTo(LogMessages.messageStateUpdated(givenMessage))
    }

    @Test
    fun `when update outbound message`() {
        val givenMessage = outboundMessage()

        subject.update(givenMessage)

        verify { mockMessageListener.invoke(capture(messageSlot)) }
        assertThat(subject.getConversation().first()).isEqualTo(givenMessage)
        assertThat(subject.nextPage).isEqualTo(1)
        assertThat((messageSlot.captured as MessageEvent.MessageInserted).message).isEqualTo(
            givenMessage
        )
    }

    @Test
    fun `when update() inbound and then outbound messages`() {
        val sentMessageId =
            subject.prepareMessage(TestValues.Token, "test message").message.metadata?.get("customMessageId")
                ?: "empty"
        val expectedConversationSize = 2
        val givenMessage =
            Message(id = sentMessageId, state = State.Sent, text = "test message")
        subject.update(outboundMessage())
        clearMocks(mockMessageListener)

        subject.update(givenMessage)

        verify { mockMessageListener.invoke(capture(messageSlot)) }
        assertThat(subject.getConversation().size).isEqualTo(expectedConversationSize)
        assertThat(subject.getConversation().first()).isEqualTo(givenMessage)
        assertThat((messageSlot.captured as MessageEvent.MessageUpdated).message).isEqualTo(
            givenMessage
        )
    }

    @Test
    fun `when update() inbound message with id that does NOT exist in conversation`() {
        val givenMessage =
            Message(id = "randomId", state = State.Sent, text = "test message")

        subject.update(message = givenMessage)

        verify { mockMessageListener.invoke(capture(messageSlot)) }
        assertThat(subject.getConversation().size).isEqualTo(1)
        assertThat((messageSlot.captured as MessageEvent.MessageInserted).message).isEqualTo(
            givenMessage
        )
    }

    @Test
    fun `when inserting more messages than the DEFAULT_PAGE_SIZE`() {
        val expectedConversationSize = 25
        val expectedNextPageIndex = 2
        for (i in 0 until DEFAULT_PAGE_SIZE) {
            subject.update(outboundMessage(messageId = i))
        }

        assertThat(subject.getConversation().size).isEqualTo(expectedConversationSize)
        assertThat(subject.nextPage).isEqualTo(expectedNextPageIndex)
    }

    @Test
    fun `when update() with new attachment`() {
        val givenAttachment = attachment()

        subject.updateAttachmentStateWith(givenAttachment)

        verify {
            mockLogger.i(capture(logSlot))
            mockMessageListener.invoke(capture(messageSlot))
        }

        // attachment is not sent yet, so it is part of the pending message
        assertThat(subject.pendingMessage.attachments["given id"]).isEqualTo(givenAttachment)
        assertThat((messageSlot.captured as MessageEvent.AttachmentUpdated).attachment).isEqualTo(
            givenAttachment
        )
        assertThat(logSlot[0].invoke()).isEqualTo(LogMessages.attachmentStateUpdated(givenAttachment))
    }

    @Test
    fun `when update() with Sent attachment state`() {
        val givenAttachment = attachment().copy(state = Attachment.State.Sent("http://someurl.com"))

        subject.updateAttachmentStateWith(givenAttachment)

        verify {
            mockLogger.i(capture(logSlot))
            mockMessageListener.invoke(capture(messageSlot))
        }

        // sent attachment with "Sent" state should not be part of the pending message
        assertThat(subject.pendingMessage.attachments.containsValue(givenAttachment)).isFalse()

        assertThat((messageSlot.captured as MessageEvent.AttachmentUpdated).attachment).isEqualTo(
            givenAttachment
        )
        assertThat(logSlot[0].invoke()).isEqualTo(LogMessages.attachmentStateUpdated(givenAttachment))
    }

    @Test
    fun `when update() existing attachment`() {
        val initialAttachment = attachment(state = Attachment.State.Presigning)
        val updatedAttachment = attachment(state = Attachment.State.Uploading)
        subject.updateAttachmentStateWith(initialAttachment)
        clearMocks(mockMessageListener)

        subject.updateAttachmentStateWith(updatedAttachment)

        verify { mockMessageListener.invoke(capture(messageSlot)) }
        assertThat(subject.pendingMessage.attachments["given id"]).isEqualTo(updatedAttachment)
        assertThat((messageSlot.captured as MessageEvent.AttachmentUpdated).attachment).isEqualTo(
            updatedAttachment
        )
    }

    @Test
    fun `when updateMessageHistory()`() {
        val expectedMessageHistory = messageList(2).reversed()
        val expectedConversationSize = 2
        val expectedNextPageIndex = 1

        subject.updateMessageHistory(messageList(2), 2)

        verify {
            mockLogger.i(capture(logSlot))
            mockMessageListener.invoke(capture(messageSlot))
        }
        subject.run {
            assertThat(startOfConversation).isTrue()
            assertThat(getConversation().size).isEqualTo(expectedConversationSize)
            assertThat(nextPage).isEqualTo(expectedNextPageIndex)
            assertThat((messageSlot.captured as MessageEvent.HistoryFetched).messages).isEqualTo(
                expectedMessageHistory
            )
            assertThat(logSlot[0].invoke()).isEqualTo(LogMessages.messageHistoryUpdated(expectedMessageHistory))
        }
    }

    @Test
    fun `when updateMessageHistory() has multiple pages`() {
        val givenMessageHistory = messageList(25)
        val givenTotal = DEFAULT_PAGE_SIZE * 2
        val expectedNextPageIndex = 2

        subject.updateMessageHistory(givenMessageHistory, givenTotal)

        assertThat(subject.startOfConversation).isFalse()
        assertThat(subject.nextPage).isEqualTo(expectedNextPageIndex)
    }

    @Test
    fun `when updateMessageHistory() contains message that already exist in conversation`() {
        val expectedMessage1 = outboundMessage(0)
        val expectedMessage2 = outboundMessage(1)
        val givenMessageHistory = messageList(2).toMutableList()
        subject.update(givenMessageHistory.first())
        clearMocks(mockMessageListener)

        subject.updateMessageHistory(givenMessageHistory, givenMessageHistory.size)

        assertThat(subject.getConversation()).containsExactly(expectedMessage2, expectedMessage1)
        verify { mockMessageListener.invoke(capture(messageSlot)) }
        val captured = messageSlot.captured as MessageEvent.HistoryFetched
        assertThat(captured.messages).containsExactly(expectedMessage2)
        assertThat(captured.startOfConversation).isTrue()
    }

    @Test
    fun `when onMessageError() and conversation is empty`() {
        subject.onMessageError(ErrorCode.MessageTooLong, "some message")

        verify { mockMessageListener wasNot Called }
        assertThat(subject.nextPage).isEqualTo(1)
        assertThat(subject.getConversation()).isEmpty()
    }

    @Test
    fun `when onMessageError() and conversation does not have any messages with state Sending`() {
        subject.update(outboundMessage())
        clearMocks(mockMessageListener)

        subject.onMessageError(ErrorCode.MessageTooLong, "some message")

        verify { mockMessageListener wasNot Called }
        assertThat(subject.getConversation()).isNotEmpty()
    }

    @Test
    fun `when onMessageError() happens after message being Sent`() {
        val errorMessage = "some test error message"
        val testMessage = "test message"
        val expectedState = State.Error(
            ErrorCode.MessageTooLong,
            errorMessage
        )
        val expectedMessage =
            subject.pendingMessage.copy(
                state = expectedState,
                text = testMessage
            )
        subject.prepareMessage(TestValues.Token, testMessage)
        clearMocks(mockMessageListener)

        subject.onMessageError(ErrorCode.MessageTooLong, errorMessage)

        assertThat { subject.getConversation().contains(expectedMessage) }
        verify { mockMessageListener.invoke(capture(messageSlot)) }
        (messageSlot.captured as MessageEvent.MessageUpdated).message.run {
            assertThat(this).isEqualTo(expectedMessage)
            assertThat((state as State.Error).code).isEqualTo(expectedState.code)
            assertThat((state as State.Error).message).isEqualTo(expectedState.message)
        }
    }

    @Test
    fun `when messageListener not set`() {
        subject.messageListener = null

        subject.prepareMessage(TestValues.Token, "test message")

        verify { mockMessageListener wasNot Called }
        assertThat(subject.messageListener).isNull()
    }

    @Test
    fun `when invalidateConversationCache()`() {
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
    fun `when prepareMessage() with channel that has customAttributes`() {
        val expectedMessage =
            subject.pendingMessage.copy(state = State.Sending, text = "test message")
        val expectedOnMessageRequest = OnMessageRequest(
            givenToken,
            message = TextMessage(
                "test message",
                metadata = mapOf("customMessageId" to expectedMessage.id),
                channel = Channel(Channel.Metadata(mapOf("A" to "B"))),
            ),
        )

        val onMessageRequest =
            subject.prepareMessage(TestValues.Token, "test message", Channel(Channel.Metadata(mapOf("A" to "B"))))

        verify { mockMessageListener.invoke(capture(messageSlot)) }
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

    @Test
    fun `when update() message with Direction=Outbound and quick replies`() {
        val expectedMessage = Message(
            id = "0",
            direction = Direction.Outbound,
            state = State.Sent,
            messageType = Message.Type.QuickReply,
            text = "message from bot",
            timeStamp = 0,
            attachments = emptyMap(),
            events = emptyList(),
            quickReplies = listOf(
                QuickReplyTestValues.buttonResponse_a,
                QuickReplyTestValues.buttonResponse_b,
            ),
            from = Participant(originatingEntity = Participant.OriginatingEntity.Bot),
        )

        subject.update(expectedMessage)

        assertThat(subject.getConversation()).contains(expectedMessage)
        assertThat(subject.nextPage).isEqualTo(1)
        verify { mockMessageListener(capture(messageSlot)) }
        assertThat((messageSlot.captured as MessageEvent.QuickReplyReceived).message).isEqualTo(expectedMessage)
    }

    @Test
    fun `when update() message with Direction=Inbound and quick replies`() {
        val expectedMessage = Message(
            id = "0",
            direction = Direction.Inbound,
            state = State.Sent,
            messageType = Message.Type.QuickReply,
            timeStamp = 0,
            attachments = emptyMap(),
            events = emptyList(),
            quickReplies = listOf(
                QuickReplyTestValues.buttonResponse_a,
                QuickReplyTestValues.buttonResponse_b,
            ),
            from = Participant(originatingEntity = Participant.OriginatingEntity.Bot),
        )

        subject.update(expectedMessage)

        assertThat(subject.getConversation()).contains(expectedMessage)
        assertThat(subject.nextPage).isEqualTo(1)
        verify { mockMessageListener(capture(messageSlot)) }
        assertThat((messageSlot.captured as MessageEvent.MessageInserted).message).isEqualTo(expectedMessage)
    }

    @Test
    fun `when prepareMessageWith() ButtonResponse and channel`() {
        val givenButtonResponse = QuickReplyTestValues.buttonResponse_a
        val givenChannel = Channel(Channel.Metadata(mapOf("A" to "B")))
        val expectedMessage =
            subject.pendingMessage.copy(
                state = State.Sending,
                messageType = Type.QuickReply,
                type = Type.QuickReply.name,
                quickReplies = listOf(givenButtonResponse)
            )
        val expectedOnMessageRequest = OnMessageRequest(
            givenToken,
            message = TextMessage(
                text = "",
                content = listOf(
                    Content(
                        contentType = Content.Type.ButtonResponse,
                        buttonResponse = givenButtonResponse
                    )
                ),
                metadata = mapOf("customMessageId" to expectedMessage.id),
                channel = givenChannel
            ),
        )

        subject.prepareMessageWith(TestValues.Token, givenButtonResponse, givenChannel).run {
            assertThat(token).isEqualTo(expectedOnMessageRequest.token)
            assertThat(message).isEqualTo(expectedOnMessageRequest.message)
            assertThat(message.content).isEqualTo(expectedOnMessageRequest.message.content)
            assertThat(message.channel).isEqualTo(expectedOnMessageRequest.message.channel)
            assertThat(time).isNull()
        }
        assertThat(subject.getConversation()[0]).isEqualTo(expectedMessage)
        assertThat(subject.pendingMessage.id).isNotEqualTo(expectedMessage.id)
        verify { mockMessageListener(capture(messageSlot)) }
        assertThat((messageSlot.captured as MessageEvent.MessageInserted).message).isEqualTo(expectedMessage)
    }

    @Test
    fun `when prepareMessageWith() ButtonResponse and no channel`() {
        val givenButtonResponse = QuickReplyTestValues.buttonResponse_a
        val expectedMessage =
            subject.pendingMessage.copy(
                state = State.Sending,
                messageType = Type.QuickReply,
                type = Type.QuickReply.name,
                quickReplies = listOf(givenButtonResponse)
            )
        val expectedOnMessageRequest = OnMessageRequest(
            givenToken,
            message = TextMessage(
                text = "",
                content = listOf(
                    Content(
                        contentType = Content.Type.ButtonResponse,
                        buttonResponse = givenButtonResponse
                    )
                ),
                metadata = mapOf("customMessageId" to expectedMessage.id)
            ),
        )

        subject.prepareMessageWith(TestValues.Token, givenButtonResponse).run {
            assertThat(token).isEqualTo(expectedOnMessageRequest.token)
            assertThat(message).isEqualTo(expectedOnMessageRequest.message)
            assertThat(message.content).isEqualTo(expectedOnMessageRequest.message.content)
            assertThat(message.channel).isNull()
            assertThat(time).isNull()
        }
        assertThat(subject.getConversation()[0]).isEqualTo(expectedMessage)
        assertThat(subject.pendingMessage.id).isNotEqualTo(expectedMessage.id)
        verify {
            mockLogger.i(capture(logSlot))
            mockMessageListener(capture(messageSlot))
        }
        assertThat((messageSlot.captured as MessageEvent.MessageInserted).message).isEqualTo(expectedMessage)
        assertThat(logSlot[0].invoke()).isEqualTo(LogMessages.quickReplyPrepareToSend(expectedMessage))
    }

    @Test
    fun `when pending message has uploaded attachment and prepareMessageWith() ButtonResponse`() {
        val givenButtonResponse = QuickReplyTestValues.buttonResponse_a
        val givenAttachment = attachment(state = Attachment.State.Uploaded("http://someurl.com"))
        val expectedContent = listOf(
            Content(
                contentType = Content.Type.ButtonResponse,
                buttonResponse = givenButtonResponse
            )
        )
        subject.updateAttachmentStateWith(givenAttachment)

        val result = subject.prepareMessageWith(TestValues.Token, givenButtonResponse)

        assertThat(result.message.content).containsOnly(*expectedContent.toTypedArray())
        assertThat(subject.pendingMessage.attachments).isNotEmpty()
        assertThat(subject.pendingMessage.attachments).contains(givenAttachment.id to givenAttachment)
    }

    private fun outboundMessage(messageId: Int = 0): Message = Message(
        id = "$messageId",
        direction = Direction.Outbound,
        state = State.Sent,
        text = "message from agent number $messageId",
        timeStamp = 100 * messageId.toLong(),
    )

    private fun attachment(
        id: String = "given id",
        state: Attachment.State = Attachment.State.Presigning,
    ) = Attachment(id, "file.png", AttachmentValues.FileSize, state)

    private fun messageList(size: Int = 5): List<Message> {
        val messageList = mutableListOf<Message>()
        for (i in 0 until size) {
            messageList.add(outboundMessage(i))
        }
        return messageList
    }
}
