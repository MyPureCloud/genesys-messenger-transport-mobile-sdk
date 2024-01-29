package com.genesys.cloud.messenger.transport.util.logs

object LogMessages {
    const val UNHANDLED_MESSAGE = "Unhandled message received from Shyrka: "
    const val REFRESH_AUTH_TOKEN_SUCCESS = "refreshAuthToken success."
    fun PRESIGNING_ATTACHMENT(attachment: String) = "Presigning attachment: $attachment"
    fun UPLOADING_ATTACHMENT(attachment: String) = "Uploading attachment: $attachment"
    fun ATTACHMENT_UPLOADED(attachment: String) = "Attachment uploaded: $attachment"
    fun DETACHING_ATTACHMENT(attachmentId: String) = "Detaching attachment: $attachmentId"
    fun ATTACHMENT_DETACHED(attachmentId: String) = "Attachment detached: $attachmentId"
    fun SENDING_ATTACHMENT(attachmentId: String) = "Sending attachment: $attachmentId"
    fun ATTACHMENTS_SENT(attachments: String) = "Attachments sent: $attachments"
    fun ADD_CUSTOM_STATE(customAttributes: String, state: String) =
        "add: $customAttributes | state = $state"
    const val ON_SENDING = "onSending"
    fun ON_SENT_STATE(state: String) = "onSent. state = $state"
    const val ON_ERROR = "onError"
    const val ON_MESSAGE_ERROR = "onMessageError()"
    const val ON_SESSION_CLOSED = "onSessionClosed"
    fun MESSAGE_PREPARED_TO_SEND(message: String) = "Message prepared to send: $message"
    fun MESSAGE_STATE_UPDATED(message: String) = "Message state updated: $message"
    fun ATTACHMENT_STATE_UPDATED(attachment: String) = "Attachment state updated: $attachment"
    fun MESSAGE_HISTORY_UPDATED(thisMessage: String) = "Message history updated with: $thisMessage"
    const val CONNECT = "connect"
    const val CONNECT_AUTHENTICATED_SESSION = "connectAuthenticatedSession"
    const val DISCONNECT = "disconnect"
    fun CONFIGURE_AUTHENTICATED_SESSION(token: String, startNew: Boolean) =
        "configureAuthenticatedSession(token = $token, startNew: $startNew)"
    fun CONFIGURE_SESSION(token: String, startNew: Boolean) =
        "configureSession (token = $token, startNew: $startNew)"
    fun SEND_MESSAGE(text: String, customAttributes: String) =
        "sendMessage(text = $text, customAttributes = $customAttributes)"
    const val SEND_HEALTH_CHECK = "sendHealthCheck()"
    fun ATTACH(fileName: String) = "attach(fileName = $fileName)"
    fun DETACH(attachmentId: String) = "detach(attachmentId = $attachmentId)"
    const val WILL_SEND_MESSAGE = "Will send message"
    const val ALL_HISTORY_FETCHED = "All history has been fetched."
    fun FETCHING_HISTORY(pageIndex: Int) = "fetching history for page index = $pageIndex"
    const val SEND_CLEAR_CONVERSATION = "sendClearConversation"
    const val CLEAR_CONVERSATION_HISTORY = "Clear conversation history."
    const val INDICATE_TYPING = "indicateTyping()"
    const val SEND_AUTO_START = "sendAutoStart()"
    const val CLOSE_SESSION = "closeSession"
    const val FORCE_CLOSE_WEB_SOCKET = "Force close web socket."
    const val ON_OPEN = "onOpen"
    fun ON_MESSAGE(text: String) = "onMessage(text = $text)"
    fun UNHANDLED_MESSAGE(decoded: String) = "Unhandled message received from Shyrka: $decoded"
    fun ON_CLOSING(code: Int, reason: String) = "onClosing(code = $code, reason = $reason)"
    fun ON_CLOSED(code: Int, reason: String) = "onClosed(code = $code, reason = $reason)"
    fun STATE_CHANGED(field: Any, value: Any) =
        "State changed from: ${field::class.simpleName}, to: ${value::class.simpleName}"
    const val UNKNOWN_EVENT_RECEIVED = "Unknown event received."
    fun ON_EVENT(event: Any) = "on event: $event"
    fun SOCKET_DID_OPEN(active: Boolean) = "Socket did open. Active: $active."
    fun SOCKET_DID_CLOSE(didCloseWithCode: Int, why: String, active: Boolean) =
        "Socket did close (code: $didCloseWithCode, reason: $why). Active: $active."
    fun CLOSE_SOCKET(code: Int, reason: String) = "closeSocket(code = $code, reason = $reason)"
    fun SEND_MESSAGE(text: String) = "sendMessage(text = $text)"
    const val SENDING_PING = "Sending ping"
    const val RECEIVED_PONG = "Received pong"
    const val DEACTIVATE = "deactivate()"
    fun DEACTIVATE_WITH_CLOSE_CODE(code: Int, reason: String) =
        "deactivateWithCloseCode(code = $code, reason = $reason)"
    fun TRYING_TO_RECONNECT(attempts: Int, maxAttempts: Int) =
        "Trying to reconnect. Attempt number: $attempts out of $maxAttempts"
    const val MESSAGE_DECODED_NULL = "Message decoded as null."
    fun CANCELLATION_EXCEPTION_REQUEST_NAME(requestName: String) =
        "Cancellation exception was thrown, while running $requestName request."
    fun CANCELLATION_EXCEPTION_ATTACHMENT_UPLOAD(attachment: String) =
        "Cancellation exception during attachment upload: $attachment"
    const val CUSTOM_ATTRIBUTES_EMPTY_OR_SAME = "custom attributes are empty or same."
    const val CANCELLATION_EXCEPTION_GET_MESSAGES =
        "Cancellation exception was thrown, while running getMessages() request."
    fun HISTORY_FETCH_FAILED(error: String) = "History fetch failed with: $error"
    fun UNHANDLED_ERROR_CODE(code: Int, message: String?) =
        "Unhandled ErrorCode: $code with optional message: $message"
    fun UNHANDLED_WEB_SOCKET_ERROR(errorCode: Int) =
        "Unhandled WebSocket errorCode. ErrorCode: $errorCode"
    fun UNSUPPORTED_MESSAGE_TYPE(type: String) = "Messages of type: $type are not supported."
    fun FAILED_FETCH_DEPLOYMENT_CONFIG(error: String) = "Failed to fetch deployment config: $error"
    fun HEALTH_CHECK_COOL_DOWN(milliseconds: Long) =
        "Health check can be sent only once every $milliseconds milliseconds."
    fun TYPING_INDICATOR_COOL_DOWN(milliseconds: Long) =
        "Typing event can be sent only once every $milliseconds milliseconds."
    const val TYPING_INDICATOR_DISABLED = "typing indicator is disabled."
    fun ON_FAILURE(throwable: Throwable) = "onFailure(message: ${throwable.message})"
    const val FAILED_TO_DESERIALIZE = "Failed to deserialize message"
    const val MESSAGE_DECODED_AS_NULL = "Message decoded as null"
    fun COULD_NOT_REFRESH_AUTH_TOKEN(message: String) = "Could not refreshAuthToken: $message"
    fun ATTACHMENT_ERROR(attachmentId: String, errorCode: Int, errorMessage: String) =
        "Attachment error with id: $attachmentId. ErrorCode: $errorCode, errorMessage: $errorMessage"
    const val CUSTOM_ATTRIBUTES_SIZE_EXCEEDED = "error: custom attributes size exceeded"
    fun RECEIVE_MESSAGE_ERROR(nsError: Any) =
        "receiveMessageWithCompletionHandler error [$nsError.code] $nsError.localizedDescription"
    fun HANDLE_ERROR(context: String?, errorCode: Int, error: String) =
        "handleError (${context ?: "no context"}) [$errorCode] $error"
}
