package com.genesys.cloud.messenger.transport.core

import com.genesys.cloud.messenger.transport.core.Message.Direction
import com.genesys.cloud.messenger.transport.shyrka.send.Channel
import com.genesys.cloud.messenger.transport.shyrka.send.OnMessageRequest
import com.genesys.cloud.messenger.transport.shyrka.send.TextMessage
import com.genesys.cloud.messenger.transport.util.extensions.getUploadedAttachments
import com.genesys.cloud.messenger.transport.util.logs.Log

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

    fun prepareMessage(
        text: String,
        customAttributes: Map<String, String> = emptyMap(),
    ): OnMessageRequest {
        val messageToSend = pendingMessage.copy(text = text, state = Message.State.Sending).also {
            log.i { "Message prepared to send: $it" }
            activeConversation.add(it)
            messageListener?.invoke(MessageEvent.MessageInserted(it))
            pendingMessage = Message()
        }
        val channel =
            if (customAttributes.isNotEmpty()) Channel(Channel.Metadata(customAttributes)) else null
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

    fun update(message: Message) {
        log.i { "Message state updated: $message" }
        when (message.direction) {
            Direction.Inbound -> {
                activeConversation.find { it.id == message.id }?.let {
                    activeConversation[it.getIndex()] = message
                    messageListener?.invoke(MessageEvent.MessageUpdated(message))
                }
            }
            Direction.Outbound -> {
                activeConversation.add(message)
                messageListener?.invoke(MessageEvent.MessageInserted(message))
            }
        }
        nextPage = activeConversation.getNextPage()
    }

    private fun update(attachment: Attachment) {
        log.i { "Attachment state updated: $attachment" }
        val attachments = pendingMessage.attachments.toMutableMap().also {
            it[attachment.id] = attachment
        }
        pendingMessage = pendingMessage.copy(attachments = attachments)
        messageListener?.invoke(MessageEvent.AttachmentUpdated(attachment))
    }

    fun updateMessageHistory(historyPage: List<Message>, total: Int) {
        startOfConversation = isAllHistoryFetched(total)
        with(historyPage.takeInactiveMessages().reversed()) {
            log.i { "Message history updated with: $this." }
            activeConversation.addAll(0, this)
            nextPage = activeConversation.getNextPage()
            messageListener?.invoke(MessageEvent.HistoryFetched(this, startOfConversation))
        }
    }

    fun getConversation(): List<Message> = activeConversation.toList()

    fun onMessageError(code: ErrorCode, message: String?) {
        activeConversation.find { it.state == Message.State.Sending }?.let {
            update(it.copy(state = Message.State.Error(code, message)))
        }
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

    fun invalidateConversationCache() {
        nextPage = 1
        activeConversation.clear()
        startOfConversation = false
    }
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
}
