package com.genesys.cloud.messenger.transport.core

import com.genesys.cloud.messenger.transport.core.Message.Direction
import com.genesys.cloud.messenger.transport.shyrka.send.Channel
import com.genesys.cloud.messenger.transport.shyrka.send.OnMessageRequest
import com.genesys.cloud.messenger.transport.shyrka.send.TextMessage
import com.genesys.cloud.messenger.transport.util.extensions.getUploadedAttachments
import com.genesys.cloud.messenger.transport.util.logs.Log
import com.genesys.cloud.messenger.transport.util.logs.LogMessages

internal const val DEFAULT_PAGE_SIZE = 25

internal class MessageStore(
    private val token: String,
    private val log: Log,
) {
    var nextPage: Int = 1
        private set
    var startOfConversation = false
        private set
    var pendingMessage = Message()
        private set
    private val activeConversation = mutableListOf<Message>()
    val updateAttachmentStateWith = { attachment: Attachment -> update(attachment) }
    var messageListener: ((MessageEvent) -> Unit)? = null

    fun prepareMessage(text: String, channel: Channel? = null): OnMessageRequest {
        val messageToSend = pendingMessage.copy(text = text, state = Message.State.Sending).also {
            log.i { LogMessages.messagePreparedToSend(it) }
            activeConversation.add(it)
            publish(MessageEvent.MessageInserted(it))
            pendingMessage = Message()
        }
        return OnMessageRequest(
            token = token,
            message = TextMessage(
                text,
                metadata = mapOf("customMessageId" to messageToSend.id),
                content = messageToSend.getUploadedAttachments(),
                channel = channel,
            )
        )
    }

    fun prepareMessageWith(
        buttonResponse: ButtonResponse,
        channel: Channel? = null,
    ): OnMessageRequest {
        val type = Message.Type.QuickReply
        val messageToSend = pendingMessage.copy(
            messageType = type,
            type = type.name,
            state = Message.State.Sending,
            quickReplies = listOf(buttonResponse),
        ).also {
            log.i { LogMessages.quickReplyPrepareToSend(it) }
            activeConversation.add(it)
            publish(MessageEvent.MessageInserted(it))
            pendingMessage = Message(attachments = it.attachments)
        }
        val content = listOf(
            Message.Content(
                contentType = Message.Content.Type.ButtonResponse,
                buttonResponse = buttonResponse,
            )
        )
        return OnMessageRequest(
            token = token,
            message = TextMessage(
                text = "",
                metadata = mapOf("customMessageId" to messageToSend.id),
                content = content,
                channel = channel,
            )
        )
    }

    fun update(message: Message) = message.run {
        log.i { LogMessages.messageStateUpdated(this) }
        when (direction) {
            Direction.Inbound -> findAndPublish(this)
            Direction.Outbound -> {
                activeConversation.add(this)
                publish(this.toMessageEvent())
            }
        }
        nextPage = activeConversation.getNextPage()
    }

    private fun update(attachment: Attachment) {
        log.i { LogMessages.attachmentStateUpdated(attachment) }
        val attachments = pendingMessage.attachments.toMutableMap().also {
            it[attachment.id] = attachment
        }
        pendingMessage = pendingMessage.copy(attachments = attachments)
        publish(MessageEvent.AttachmentUpdated(attachment))
    }

    fun updateMessageHistory(historyPage: List<Message>, total: Int) {
        startOfConversation = isAllHistoryFetched(total)
        with(historyPage.takeInactiveMessages().reversed()) {
            log.i { LogMessages.messageHistoryUpdated(this) }
            activeConversation.addAll(0, this)
            nextPage = activeConversation.getNextPage()
            publish(MessageEvent.HistoryFetched(this, startOfConversation))
        }
    }

    fun getConversation(): List<Message> = activeConversation.toList()

    fun onMessageError(code: ErrorCode, message: String?) {
        activeConversation.find { it.state == Message.State.Sending }?.let {
            update(it.copy(state = Message.State.Error(code, message)))
        }
    }

    fun invalidateConversationCache() {
        nextPage = 1
        activeConversation.clear()
        startOfConversation = false
    }

    private fun findAndPublish(message: Message) {
        activeConversation.find { it.id == message.id }?.let {
            activeConversation[it.getIndex()] = message
            publish(MessageEvent.MessageUpdated(message))
        } ?: run {
            activeConversation.add(message)
            publish(MessageEvent.MessageInserted(message))
        }
    }

    private fun publish(event: MessageEvent) {
        messageListener?.invoke(event)
    }

    private fun Message.getIndex(): Int = activeConversation.indexOf(this)

    private fun <E> MutableList<E>.getNextPage(): Int = (this.size / DEFAULT_PAGE_SIZE) + 1

    private fun isAllHistoryFetched(totalInStash: Int) =
        totalInStash - activeConversation.size <= DEFAULT_PAGE_SIZE

    private fun List<Message>.takeInactiveMessages(): List<Message> {
        return this.filter { message ->
            activeConversation.none { activeMessage ->
                message.timeStamp == activeMessage.timeStamp
            }
        }
    }
}

private fun Message.toMessageEvent(): MessageEvent =
    if (messageType == Message.Type.QuickReply) {
        MessageEvent.QuickReplyReceived(this)
    } else {
        MessageEvent.MessageInserted(this)
    }

/**
 * Communicates conversation related updates to the UI.
 */
sealed class MessageEvent {
    /**
     *
     * Dispatched when new message is added to the conversation either by user or agent.
     * @property message is the [Message] object with all the details.
     */
    class MessageInserted(val message: Message) : MessageEvent()

    /**
     * Dispatched when message that is already present in conversation changes it state.
     * For instance: when backend confirms that message that was sent from the user
     * was successfully delivered.
     *
     * @property message is the updated [Message] object with details.
     */
    class MessageUpdated(val message: Message) : MessageEvent()

    /**
     * Dispatched when Attachment changes its state.
     *
     * @property attachment is the [Attachment] object that contains an updated state.
     */
    class AttachmentUpdated(val attachment: Attachment) : MessageEvent()

    /**
     * Dispatched when [MessagingClient.fetchNextPage] returns a successful result.
     *
     * @property messages is a the list of messages returned by the Shyrka. Note! Messages that
     * already present in [MessageStore.activeConversation] will be filtered out, making sure that only unique
     * messages are dispatched.
     * @property startOfConversation is a flag that indicated if user has fetched all messages in the conversation history.
     * When true - no more [com.genesys.cloud.messenger.transport.network.WebMessagingApi.getMessages] requests will be executed.
     */
    class HistoryFetched(val messages: List<Message>, val startOfConversation: Boolean) :
        MessageEvent()

    /**
     * Dispatched when message with quick replies was sent by the Bot. To get the actual quick reply
     * options refer to [Message.quickReplies] list of [ButtonResponse].
     *
     * @property message is the [Message] object with all the details.
     */
    class QuickReplyReceived(val message: Message) : MessageEvent()
}
