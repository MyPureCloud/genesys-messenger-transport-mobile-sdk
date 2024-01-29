package com.genesys.cloud.messenger.transport.util.logs

import com.genesys.cloud.messenger.transport.core.Attachment
import com.genesys.cloud.messenger.transport.core.CustomAttributesStoreImpl
import com.genesys.cloud.messenger.transport.core.Message
import com.genesys.cloud.messenger.transport.core.MessagingClient
import com.genesys.cloud.messenger.transport.core.events.Event
import com.genesys.cloud.messenger.transport.shyrka.receive.StructuredMessage
import com.genesys.cloud.messenger.transport.shyrka.receive.WebMessagingMessage

object LogMessages {
    const val REFRESH_AUTH_TOKEN_SUCCESS = "refreshAuthToken success."
    fun presigningAttachment(attachment: Attachment) = "Presigning attachment: $attachment"
    fun uploadingAttachment(attachment: Attachment) = "Uploading attachment: $attachment"
    fun attachmentUploaded(attachment: Attachment) = "Attachment uploaded: $attachment"
    fun detachingAttachment(attachmentId: String) = "Detaching attachment: $attachmentId"
    fun attachmentDetached(attachmentId: String) = "Attachment detached: $attachmentId"
    fun sendingAttachment(attachmentId: String) = "Sending attachment: $attachmentId"
    fun attachmentSent(attachments: Map<String, Attachment>) = "Attachments sent: $attachments"
    fun addCustomState(customAttributes: Map<String, String>, state: String) =
        "add: $customAttributes | state = $state"
    const val ON_SENDING = "onSending"
    fun onSentState(state: String) = "onSent. state = $state"
    const val ON_ERROR = "onError"
    const val ON_MESSAGE_ERROR = "onMessageError()"
    const val ON_SESSION_CLOSED = "onSessionClosed"
    fun messagePreparedToSend(message: Message) = "Message prepared to send: $message"
    fun messageStateUpdated(message: Message) = "Message state updated: $message"
    fun attachmentStateUpdated(attachment: Attachment) = "Attachment state updated: $attachment"
    fun messageHistoryUpdated(messages: List<Message>) = "Message history updated with: $messages"
    const val CONNECT = "connect"
    const val CONNECT_AUTHENTICATED_SESSION = "connectAuthenticatedSession"
    const val DISCONNECT = "disconnect"
    fun configureAuthenticatedSession(token: String, startNew: Boolean) =
        "configureAuthenticatedSession(token = $token, startNew: $startNew)"
    fun configureSession(token: String, startNew: Boolean) =
        "configureSession (token = $token, startNew: $startNew)"
    //sendmessageca**********
//    fun sendMessageCustomAttributes(text: String, customAttributes: Map<String, String>) =
//        "sendMessage(text = $text, customAttributes = $customAttributes)"
    const val SEND_HEALTH_CHECK = "sendHealthCheck()"
    fun attach(fileName: String) = "attach(fileName = $fileName)"
    fun detach(attachmentId: String) = "detach(attachmentId = $attachmentId)"
    const val WILL_SEND_MESSAGE = "Will send message"
    const val ALL_HISTORY_FETCHED = "All history has been fetched."
    fun fetchingHistory(pageIndex: Int) = "fetching history for page index = $pageIndex"
    const val SEND_CLEAR_CONVERSATION = "sendClearConversation"
    const val CLEAR_CONVERSATION_HISTORY = "Clear conversation history."
    const val INDICATE_TYPING = "indicateTyping()"
    const val SEND_AUTO_START = "sendAutoStart()"
    const val CLOSE_SESSION = "closeSession"
    const val FORCE_CLOSE_WEB_SOCKET = "Force close web socket."
    const val ON_OPEN = "onOpen"
    fun onMessage(text: String) = "onMessage(text = $text)"
    // help *****************************
    fun unhandledMessage(decoded: String) = "Unhandled message received from Shyrka: $decoded"
    fun onClosing(code: Int, reason: String) = "onClosing(code = $code, reason = $reason)"
    fun onClosed(code: Int, reason: String) = "onClosed(code = $code, reason = $reason)"
    fun stateChanged(field: Any, value: Any) =
        "State changed from: ${field::class.simpleName}, to: ${value::class.simpleName}"
    const val UNKNOWN_EVENT_RECEIVED = "Unknown event received."
    fun onEvent(event: Event) = "on event: $event"
    fun socketDidOpen(active: Boolean) = "Socket did open. Active: $active."
    // help with this***********************
    fun socketDidClose(didCloseWithCode: Int, why: String, active: Boolean) =
        "Socket did close (code: $didCloseWithCode, reason: $why). Active: $active."
    fun closeSocket(code: Int, reason: String) = "closeSocket(code = $code, reason = $reason)"
    //sendmessage???*********
    fun sendMessage(text: String, customAttributes: Map<String, String> = emptyMap()) =
        "sendMessage(text = $text, customAttributes = $customAttributes)"
    const val SENDING_PING = "Sending ping"
    const val RECEIVED_PONG = "Received pong"
    const val DEACTIVATE = "deactivate()"
    fun deactivateWithCloseCode(code: Int, reason: String?) =
        "deactivateWithCloseCode(code = $code, reason = $reason)"
    fun tryingToReconnect(attempts: Int, maxAttempts: Int) =
        "Trying to reconnect. Attempt number: $attempts out of $maxAttempts"
    const val MESSAGE_DECODED_NULL = "Message decoded as null."
    fun cancellationExceptionRequestName(requestName: String) =
        "Cancellation exception was thrown, while running $requestName request."
    fun cancellationExceptionAttachmentUpload(attachment: String) =
        "Cancellation exception during attachment upload: $attachment"
    const val CUSTOM_ATTRIBUTES_EMPTY_OR_SAME = "custom attributes are empty or same."
    const val CANCELLATION_EXCEPTION_GET_MESSAGES =
        "Cancellation exception was thrown, while running getMessages() request."
    fun historyFetchFailed(error: String) = "History fetch failed with: $error"
    fun unhandledErrorCode(code: Int, message: String?) =
        "Unhandled ErrorCode: $code with optional message: $message"
    fun unhandledWebSocketError(errorCode: Int) =
        "Unhandled WebSocket errorCode. ErrorCode: $errorCode"
    fun unsupportedMessageType(type: String) = "Messages of type: $type are not supported."
    fun failedFetchDeploymentConfig(error: String) = "Failed to fetch deployment config: $error"
    fun healthCheckCoolDown(milliseconds: Long) =
        "Health check can be sent only once every $milliseconds milliseconds."
    fun typingIndicatorCoolDown(milliseconds: Long) =
        "Typing event can be sent only once every $milliseconds milliseconds."
    const val TYPING_INDICATOR_DISABLED = "typing indicator is disabled."
    fun onFailure(throwable: Throwable) = "onFailure(message: ${throwable.message})"
    const val FAILED_TO_DESERIALIZE = "Failed to deserialize message"
    const val MESSAGE_DECODED_AS_NULL = "Message decoded as null"
    fun couldNotRefreshAuthToken(message: String) = "Could not refreshAuthToken: $message"
    fun attachmentError(attachmentId: String, errorCode: Int, errorMessage: String) =
        "Attachment error with id: $attachmentId. ErrorCode: $errorCode, errorMessage: $errorMessage"
    const val CUSTOM_ATTRIBUTES_SIZE_EXCEEDED = "error: custom attributes size exceeded"
    fun receiveMessageError(nsError: Any) =
        "receiveMessageWithCompletionHandler error [$nsError.code] $nsError.localizedDescription"
    fun handleError(context: String?, errorCode: Int, error: String) =
        "handleError (${context ?: "no context"}) [$errorCode] $error"
}
