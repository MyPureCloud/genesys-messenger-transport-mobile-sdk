package com.genesys.cloud.messenger.transport.util.logs

import com.genesys.cloud.messenger.transport.core.Attachment
import com.genesys.cloud.messenger.transport.core.ButtonResponse
import com.genesys.cloud.messenger.transport.core.ErrorCode
import com.genesys.cloud.messenger.transport.core.Message
import com.genesys.cloud.messenger.transport.core.MessagingClient
import com.genesys.cloud.messenger.transport.core.Result
import com.genesys.cloud.messenger.transport.core.events.Event
import com.genesys.cloud.messenger.transport.shyrka.receive.WebMessagingMessage

internal object LogMessages {
    // Attachment
    fun presigningAttachment(attachment: Attachment) = "Presigning attachment: $attachment"
    fun uploadingAttachment(attachment: Attachment) = "Uploading attachment: $attachment"
    fun attachmentUploaded(attachment: Attachment) = "Attachment uploaded: $attachment"
    fun detachingAttachment(attachmentId: String) = "Detaching attachment: $attachmentId"
    fun attachmentDetached(attachmentId: String) = "Attachment detached: $attachmentId"
    fun sendingAttachment(attachmentId: String) = "Sending attachment: $attachmentId"
    fun attachmentSent(attachments: Map<String, Attachment>) = "Attachments sent: $attachments"
    fun attachmentStateUpdated(attachment: Attachment) = "Attachment state updated: $attachment"
    fun attach(fileName: String) = "attach(fileName = $fileName)"
    fun detach(attachmentId: String) = "detach(attachmentId = $attachmentId)"
    fun attachmentError(attachmentId: String, errorCode: ErrorCode, errorMessage: String) =
        "Attachment error with id: $attachmentId. ErrorCode: $errorCode, errorMessage: $errorMessage"
    fun invalidAttachmentId(attachmentId: String) = "Invalid attachment ID: $attachmentId. Detach failed."

    // Authentication
    const val REFRESH_AUTH_TOKEN_SUCCESS = "refreshAuthToken success."
    fun configureAuthenticatedSession(token: String, startNew: Boolean) =
        "configureAuthenticatedSession(token = $token, startNew: $startNew)"
    fun configureSession(token: String, startNew: Boolean = false) =
        "configureSession (token = $token, startNew: $startNew)"
    // Message
    fun messagePreparedToSend(message: Message) = "Message prepared to send: $message"
    fun messageStateUpdated(message: Message) = "Message state updated: $message"
    fun messageHistoryUpdated(messages: List<Message>) = "Message history updated with: $messages."
    fun receiveMessageError(code: Long, localizedDescription: String) =
        "receiveMessageWithCompletionHandler error [$code] $localizedDescription"
    const val ON_ERROR = "onError"
    const val ON_MESSAGE_ERROR = "onMessageError"
    const val ON_SENDING = "onSending"
    const val WILL_SEND_MESSAGE = "Will send message"
    const val ALL_HISTORY_FETCHED = "All history has been fetched."
    fun fetchingHistory(pageIndex: Int) = "fetching history for page index = $pageIndex"
    const val SEND_CLEAR_CONVERSATION = "sendClearConversation"
    const val CLEAR_CONVERSATION_HISTORY = "Clear conversation history."
    const val INDICATE_TYPING = "indicateTyping()"
    fun onMessage(text: String) = "onMessage(text = $text)"
    fun sendMessage(text: String, customAttributes: Map<String, String> = emptyMap()) =
        "sendMessage(text = $text, customAttributes = $customAttributes)"
    fun unhandledMessage(decoded: WebMessagingMessage<*>) = "Unhandled message received from Shyrka: $decoded"
    fun historyFetchFailed(error: Result.Failure) = "History fetch failed with: $error"
    fun onFailure(throwable: Throwable) = "onFailure(message: ${throwable.message})"
    fun unsupportedMessageType(type: Message.Type) = "Messages of type: $type are not supported."
    fun typingIndicatorCoolDown(milliseconds: Long) =
        "Typing event can be sent only once every $milliseconds milliseconds."
    const val TYPING_INDICATOR_DISABLED = "typing indicator is disabled."
    // Session State
    const val ON_SESSION_CLOSED = "onSessionClosed"
    const val CONNECT = "connect"
    const val CONNECT_AUTHENTICATED_SESSION = "connectAuthenticatedSession"
    const val STEP_UP_TO_AUTHENTICATED_SESSION = "stepUpToAuthenticatedSession"
    const val DISCONNECT = "disconnect"
    fun onSentState(state: String) = "onSent. state = $state"
    const val CLOSE_SESSION = "closeSession"
    const val FAILED_TO_DESERIALIZE = "Failed to deserialize message"
    fun couldNotRefreshAuthToken(message: String?) = "Could not refreshAuthToken: $message"
    fun requestError(requestName: String, errorCode: ErrorCode, message: String?) =
        "$requestName responded with error: $errorCode, and message: $message"
    fun onClosing(code: Int, reason: String) = "onClosing(code = $code, reason = $reason)"
    fun onClosed(code: Int, reason: String) = "onClosed(code = $code, reason = $reason)"
    fun stateChanged(field: MessagingClient.State, value: MessagingClient.State) =
        "State changed from: ${field::class.simpleName}, to: ${value::class.simpleName}"
    fun onEvent(event: Event) = "on event: $event"
    fun socketDidOpen(active: Boolean) = "Socket did open. Active: $active."
    fun socketDidClose(didCloseWithCode: Long, why: String, active: Boolean) =
        "Socket did close (code: $didCloseWithCode, reason: $why). Active: $active."
    fun closeSocket(code: Int, reason: String) = "closeSocket(code = $code, reason = $reason)"
    const val SENDING_PING = "Sending ping"
    const val RECEIVED_PONG = "Received pong"
    const val DEACTIVATE = "deactivate()"
    const val SEND_HEALTH_CHECK = "sendHealthCheck()"
    const val SEND_AUTO_START = "sendAutoStart()"
    const val FORCE_CLOSE_WEB_SOCKET = "Force close web socket."
    const val ON_OPEN = "onOpen"
    const val INVALIDATE_SESSION_TOKEN = "invalidate session token"
    fun failedFetchDeploymentConfig(error: Throwable) = "Failed to fetch deployment config: $error"
    fun healthCheckCoolDown(milliseconds: Long) =
        "Health check can be sent only once every $milliseconds milliseconds."
    fun deactivateWithCloseCode(code: Int, reason: String?) =
        "deactivateWithCloseCode(code = $code, reason = $reason)"
    fun tryingToReconnect(attempts: Int, maxAttempts: Int) =
        "Trying to reconnect. Attempt number: $attempts out of $maxAttempts"
    const val MESSAGE_DECODED_NULL = "Message decoded as null."
    fun cancellationExceptionRequestName(requestName: String) =
        "Cancellation exception was thrown, while running $requestName request."
    fun cancellationExceptionAttachmentUpload(attachmentId: String) =
        "Cancellation exception during attachment upload: $attachmentId"
    fun unhandledErrorCode(code: ErrorCode, message: String?) =
        "Unhandled ErrorCode: $code with optional message: $message"
    fun unhandledWebSocketError(errorCode: ErrorCode) =
        "Unhandled WebSocket errorCode. ErrorCode: $errorCode"
    // Custom Attributes
    fun addCustomAttribute(customAttributes: Map<String, String>, state: String) =
        "add: $customAttributes | state = $state"
    const val CUSTOM_ATTRIBUTES_SIZE_EXCEEDED = "Error: Custom attributes size exceeded"
    const val CUSTOM_ATTRIBUTES_EMPTY_OR_SAME = "custom attributes are empty or same."
    const val CANCELLATION_EXCEPTION_GET_MESSAGES =
        "Cancellation exception was thrown, while running getMessages() request."
    // Quick Replies
    fun quickReplyPrepareToSend(message: Message) = "Message with quick reply prepared to send: $message"
    fun sendQuickReply(buttonResponse: ButtonResponse) = "sendQuickReply(buttonResponse: $buttonResponse)"
    fun ignoreInboundEvent(event: Event) = "Ignore inbound event: $event."
}
