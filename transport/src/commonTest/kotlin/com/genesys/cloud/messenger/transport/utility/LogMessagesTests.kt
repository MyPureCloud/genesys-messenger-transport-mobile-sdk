package com.genesys.cloud.messenger.transport.utility

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.genesys.cloud.messenger.transport.core.Attachment
import com.genesys.cloud.messenger.transport.core.ErrorCode
import com.genesys.cloud.messenger.transport.core.Message
import com.genesys.cloud.messenger.transport.core.MessagingClient
import com.genesys.cloud.messenger.transport.core.Result
import com.genesys.cloud.messenger.transport.core.events.Event
import com.genesys.cloud.messenger.transport.shyrka.receive.WebMessagingMessage
import com.genesys.cloud.messenger.transport.util.logs.LogMessages
import kotlin.test.Test

class LogMessagesTests {

    // Attachment
    @Test
    fun `when presigningAttachment is called then it logs correctly`() {
        val givenAttachment = Attachment(id = AttachmentValues.ID, fileName = AttachmentValues.FILE_NAME, fileSizeInBytes = AttachmentValues.FILE_SIZE)
        val expectedMessage = "Presigning attachment: $givenAttachment"

        val result = LogMessages.presigningAttachment(givenAttachment)

        assertThat(result).isEqualTo(expectedMessage)
    }

    @Test
    fun `when uploadingAttachment is called then it logs correctly`() {
        val givenAttachment = Attachment(id = AttachmentValues.ID, fileName = AttachmentValues.FILE_NAME, fileSizeInBytes = AttachmentValues.FILE_SIZE)
        val expectedMessage = "Uploading attachment: $givenAttachment"

        val result = LogMessages.uploadingAttachment(givenAttachment)

        assertThat(result).isEqualTo(expectedMessage)
    }

    @Test
    fun `when attachmentUploaded is called then it logs correctly`() {
        val givenAttachment = Attachment(id = AttachmentValues.ID, fileName = AttachmentValues.FILE_NAME, fileSizeInBytes = AttachmentValues.FILE_SIZE)
        val expectedMessage = "Attachment uploaded: $givenAttachment"

        val result = LogMessages.attachmentUploaded(givenAttachment)

        assertThat(result).isEqualTo(expectedMessage)
    }

    @Test
    fun `when detachingAttachment is called then it logs correctly`() {
        val givenAttachmentId = AttachmentValues.ID
        val expectedMessage = "Detaching attachment: ${AttachmentValues.ID}"

        val result = LogMessages.detachingAttachment(givenAttachmentId)

        assertThat(result).isEqualTo(expectedMessage)
    }

    @Test
    fun `when attachmentDetached is called then it logs correctly`() {
        val givenAttachmentId = AttachmentValues.ID
        val expectedMessage = "Attachment detached: ${AttachmentValues.ID}"

        val result = LogMessages.attachmentDetached(givenAttachmentId)

        assertThat(result).isEqualTo(expectedMessage)
    }

    @Test
    fun `when sendingAttachment is called then it logs correctly`() {
        val givenAttachmentId = AttachmentValues.ID
        val expectedMessage = "Sending attachment: ${AttachmentValues.ID}"

        val result = LogMessages.sendingAttachment(givenAttachmentId)

        assertThat(result).isEqualTo(expectedMessage)
    }

    @Test
    fun `when attachmentSent is called then it logs correctly`() {
        val givenAttachments = mapOf(AttachmentValues.ID to Attachment(id = AttachmentValues.ID, fileName = AttachmentValues.FILE_NAME, fileSizeInBytes = AttachmentValues.FILE_SIZE))
        val expectedMessage = "Attachments sent: $givenAttachments"

        val result = LogMessages.attachmentSent(givenAttachments)

        assertThat(result).isEqualTo(expectedMessage)
    }

    @Test
    fun `when attachmentStateUpdated is called then it logs correctly`() {
        val givenAttachment = Attachment(id = AttachmentValues.ID, fileName = AttachmentValues.FILE_NAME, fileSizeInBytes = AttachmentValues.FILE_SIZE)
        val expectedMessage = "Attachment state updated: $givenAttachment"

        val result = LogMessages.attachmentStateUpdated(givenAttachment)

        assertThat(result).isEqualTo(expectedMessage)
    }

    @Test
    fun `when attach is called then it logs correctly`() {
        val givenFileName = AttachmentValues.FILE_NAME
        val expectedMessage = "attach(fileName = $givenFileName)"

        val result = LogMessages.attach(givenFileName)

        assertThat(result).isEqualTo(expectedMessage)
    }

    @Test
    fun `when detach is called then it logs correctly`() {
        val givenAttachmentId = AttachmentValues.ID
        val expectedMessage = "detach(attachmentId = $givenAttachmentId)"

        val result = LogMessages.detach(givenAttachmentId)

        assertThat(result).isEqualTo(expectedMessage)
    }

    @Test
    fun `when attachmentError is called then it logs correctly`() {
        val givenAttachmentId = AttachmentValues.ID
        val givenErrorCode = ErrorCode.AttachmentNotFound
        val givenErrorMessage = ErrorTest.MESSAGE
        val expectedMessage = "Attachment error with id: $givenAttachmentId. ErrorCode: $givenErrorCode, errorMessage: $givenErrorMessage"

        val result = LogMessages.attachmentError(givenAttachmentId, givenErrorCode, givenErrorMessage)

        assertThat(result).isEqualTo(expectedMessage)
    }

    @Test
    fun `when invalidAttachmentId is called then it logs correctly`() {
        val givenAttachmentId = AttachmentValues.ID
        val expectedMessage = "Invalid attachment ID: $givenAttachmentId. Detach failed."

        val result = LogMessages.invalidAttachmentId(givenAttachmentId)

        assertThat(result).isEqualTo(expectedMessage)
    }

    // Authentication
    @Test
    fun `when configureAuthenticatedSession is called then it logs correctly`() {
        val givenStartNew = true
        val expectedMessage = "configureAuthenticatedSession(startNew: $givenStartNew)"

        val result = LogMessages.configureAuthenticatedSession(givenStartNew)

        assertThat(result).isEqualTo(expectedMessage)
    }

    @Test
    fun `when configureSession is called then it logs correctly`() {
        val givenStartNew = false
        val expectedMessage = "configureSession (startNew: $givenStartNew)"

        val result = LogMessages.configureSession(givenStartNew)

        assertThat(result).isEqualTo(expectedMessage)
    }

    // Message
    @Test
    fun `when messagePreparedToSend is called then it logs correctly`() {
        val givenMessage = Message()
        val expectedMessage = "Message prepared to send: $givenMessage"

        val result = LogMessages.messagePreparedToSend(givenMessage)

        assertThat(result).isEqualTo(expectedMessage)
    }

    @Test
    fun `when messageStateUpdated is called then it logs correctly`() {
        val givenMessage = Message()
        val expectedMessage = "Message state updated: $givenMessage"

        val result = LogMessages.messageStateUpdated(givenMessage)

        assertThat(result).isEqualTo(expectedMessage)
    }

    @Test
    fun `when messageHistoryUpdated is called then it logs correctly`() {
        val givenMessages = listOf(Message())
        val expectedMessage = "Message history updated with: $givenMessages."

        val result = LogMessages.messageHistoryUpdated(givenMessages)

        assertThat(result).isEqualTo(expectedMessage)
    }

    @Test
    fun `when receiveMessageError is called then it logs correctly`() {
        val givenCode = ErrorTest.CODE_404
        val givenLocalizedDescription = ErrorTest.MESSAGE
        val expectedMessage = "receiveMessageWithCompletionHandler error [$givenCode] $givenLocalizedDescription"

        val result = LogMessages.receiveMessageError(givenCode, givenLocalizedDescription)

        assertThat(result).isEqualTo(expectedMessage)
    }

    @Test
    fun `when fetchingHistory is called then it logs correctly`() {
        val givenPageIndex = MessageValues.PAGE_NUMBER
        val expectedMessage = "fetching history for page index = $givenPageIndex"

        val result = LogMessages.fetchingHistory(givenPageIndex)

        assertThat(result).isEqualTo(expectedMessage)
    }

    @Test
    fun `when onMessage is called then it logs correctly`() {
        val givenText = TestValues.DEFAULT_STRING
        val expectedMessage = "onMessage(text = $givenText)"

        val result = LogMessages.onMessage(givenText)

        assertThat(result).isEqualTo(expectedMessage)
    }

    @Test
    fun `when sendMessage is called then it logs correctly`() {
        val givenText = TestValues.DEFAULT_STRING
        val givenCustomAttributes = TestValues.defaultMap
        val expectedMessage = "sendMessage(text = $givenText, customAttributes = $givenCustomAttributes)"

        val result = LogMessages.sendMessage(givenText, givenCustomAttributes)

        assertThat(result).isEqualTo(expectedMessage)
    }

    @Test
    fun `when unhandledMessage is called then it logs correctly`() {
        val givenDecoded = WebMessagingMessage(type = MessageValues.TYPE, code = MessageValues.PRE_IDENTIFIED_MESSAGE_CODE, body = MessageValues.TEXT)
        val expectedMessage = "Unhandled message received from Shyrka: $givenDecoded"

        val result = LogMessages.unhandledMessage(givenDecoded)

        assertThat(result).isEqualTo(expectedMessage)
    }

    @Test
    fun `when historyFetchFailed is called then it logs correctly`() {
        val givenError = Result.Failure(ErrorCode.UnexpectedError, ErrorTest.MESSAGE)
        val expectedMessage = "History fetch failed with: $givenError"

        val result = LogMessages.historyFetchFailed(givenError)

        assertThat(result).isEqualTo(expectedMessage)
    }

    @Test
    fun `when onFailure is called then it logs correctly`() {
        val givenThrowable = Throwable("Some error")
        val expectedMessage = "onFailure(message: ${givenThrowable.message})"

        val result = LogMessages.onFailure(givenThrowable)

        assertThat(result).isEqualTo(expectedMessage)
    }

    @Test
    fun `when unsupportedMessageType is called then it logs correctly`() {
        val givenMessageType = Message.Type.Text
        val expectedMessage = "Messages of type: $givenMessageType are not supported."

        val result = LogMessages.unsupportedMessageType(givenMessageType)

        assertThat(result).isEqualTo(expectedMessage)
    }

    @Test
    fun `when typingIndicatorCoolDown is called then it logs correctly`() {
        val givenMilliseconds = 5000L
        val expectedMessage = "Typing event can be sent only once every $givenMilliseconds milliseconds."

        val result = LogMessages.typingIndicatorCoolDown(givenMilliseconds)

        assertThat(result).isEqualTo(expectedMessage)
    }

    // Session State
    @Test
    fun `when onSentState is called then it logs correctly`() {
        val givenState = "Sent"
        val expectedMessage = "onSent. state = $givenState"

        val result = LogMessages.onSentState(givenState)

        assertThat(result).isEqualTo(expectedMessage)
    }

    @Test
    fun `when onClosing is called then it logs correctly`() {
        val givenCode = 1001
        val givenReason = "Going away"
        val expectedMessage = "onClosing(code = $givenCode, reason = $givenReason)"

        val result = LogMessages.onClosing(givenCode, givenReason)

        assertThat(result).isEqualTo(expectedMessage)
    }

    @Test
    fun `when onClosed is called then it logs correctly`() {
        val givenCode = 1000
        val givenReason = "Normal closure"
        val expectedMessage = "onClosed(code = $givenCode, reason = $givenReason)"

        val result = LogMessages.onClosed(givenCode, givenReason)

        assertThat(result).isEqualTo(expectedMessage)
    }

    @Test
    fun `when stateChanged is called then it logs correctly`() {
        val givenOldState = MessagingClient.State.Connecting
        val givenNewState = MessagingClient.State.Connected
        val expectedMessage = "State changed from: ${givenOldState::class.simpleName}, to: ${givenNewState::class.simpleName}"

        val result = LogMessages.stateChanged(givenOldState, givenNewState)

        assertThat(result).isEqualTo(expectedMessage)
    }

    @Test
    fun `when onEvent is called then it logs correctly`() {
        val givenEvent = Event.Logout
        val expectedMessage = "on event: $givenEvent"

        val result = LogMessages.onEvent(givenEvent)

        assertThat(result).isEqualTo(expectedMessage)
    }

    @Test
    fun `when socketDidOpen is called then it logs correctly`() {
        val givenActive = true
        val expectedMessage = "Socket did open. Active: $givenActive."

        val result = LogMessages.socketDidOpen(givenActive)

        assertThat(result).isEqualTo(expectedMessage)
    }

    @Test
    fun `when socketDidClose is called then it logs correctly`() {
        val givenCode = 1006L
        val givenReason = "Abnormal closure"
        val givenActive = false
        val expectedMessage = "Socket did close (code: $givenCode, reason: $givenReason). Active: $givenActive."

        val result = LogMessages.socketDidClose(givenCode, givenReason, givenActive)

        assertThat(result).isEqualTo(expectedMessage)
    }

    @Test
    fun `when closeSocket is called then it logs correctly`() {
        val givenCode = 4000
        val givenReason = "Manual close"
        val expectedMessage = "closeSocket(code = $givenCode, reason = $givenReason)"

        val result = LogMessages.closeSocket(givenCode, givenReason)

        assertThat(result).isEqualTo(expectedMessage)
    }

    @Test
    fun `when failedFetchDeploymentConfig is called then it logs correctly`() {
        val givenError = Exception("Deployment fetch error")
        val expectedMessage = "Failed to fetch deployment config: $givenError"

        val result = LogMessages.failedFetchDeploymentConfig(givenError)

        assertThat(result).isEqualTo(expectedMessage)
    }

    @Test
    fun `when healthCheckCoolDown is called then it logs correctly`() {
        val givenMilliseconds = 3000L
        val expectedMessage = "Health check can be sent only once every $givenMilliseconds milliseconds."

        val result = LogMessages.healthCheckCoolDown(givenMilliseconds)

        assertThat(result).isEqualTo(expectedMessage)
    }

    @Test
    fun `when deactivateWithCloseCode is called then it logs correctly`() {
        val givenCode = 1000
        val givenReason = "Session ended"
        val expectedMessage = "deactivateWithCloseCode(code = $givenCode, reason = $givenReason)"

        val result = LogMessages.deactivateWithCloseCode(givenCode, givenReason)

        assertThat(result).isEqualTo(expectedMessage)
    }

    @Test
    fun `when tryingToReconnect is called then it logs correctly`() {
        val givenAttempts = 2
        val givenMaxAttempts = 5
        val expectedMessage = "Trying to reconnect. Attempt number: $givenAttempts out of $givenMaxAttempts"

        val result = LogMessages.tryingToReconnect(givenAttempts, givenMaxAttempts)

        assertThat(result).isEqualTo(expectedMessage)
    }

    @Test
    fun `when cancellationExceptionRequestName is called then it logs correctly`() {
        val givenRequestName = "Reconnect"
        val expectedMessage = "Cancellation exception was thrown, while running $givenRequestName request."

        val result = LogMessages.cancellationExceptionRequestName(givenRequestName)

        assertThat(result).isEqualTo(expectedMessage)
    }

    @Test
    fun `when cancellationExceptionAttachmentUpload is called then it logs correctly`() {
        val givenAttachmentId = TestValues.DEFAULT_STRING
        val expectedMessage = "Cancellation exception during attachment upload: $givenAttachmentId"

        val result = LogMessages.cancellationExceptionAttachmentUpload(givenAttachmentId)

        assertThat(result).isEqualTo(expectedMessage)
    }

    @Test
    fun `when unhandledErrorCode is called then it logs correctly`() {
        val givenErrorCode = ErrorCode.FeatureUnavailable
        val givenMessage = "Feature not supported"
        val expectedMessage = "Unhandled ErrorCode: $givenErrorCode with optional message: $givenMessage"

        val result = LogMessages.unhandledErrorCode(givenErrorCode, givenMessage)

        assertThat(result).isEqualTo(expectedMessage)
    }

    @Test
    fun `when unhandledWebSocketError is called then it logs correctly`() {
        val givenErrorCode = ErrorCode.FileSizeInvalid
        val expectedMessage = "Unhandled WebSocket errorCode. ErrorCode: $givenErrorCode"

        val result = LogMessages.unhandledWebSocketError(givenErrorCode)

        assertThat(result).isEqualTo(expectedMessage)
    }

    @Test
    fun `when couldNotRefreshAuthToken is called then it logs correctly`() {
        val givenMessage = "Invalid token"
        val expectedMessage = "Could not refreshAuthToken: $givenMessage"

        val result = LogMessages.couldNotRefreshAuthToken(givenMessage)

        assertThat(result).isEqualTo(expectedMessage)
    }

    @Test
    fun `when requestError is called then it logs correctly`() {
        val givenRequestName = "SendMessage"
        val givenErrorCode = ErrorCode.AuthFailed
        val givenMessage = "Connection timeout"
        val expectedMessage = "$givenRequestName responded with error: $givenErrorCode, and message: $givenMessage"

        val result = LogMessages.requestError(givenRequestName, givenErrorCode, givenMessage)

        assertThat(result).isEqualTo(expectedMessage)
    }

    // Custom Attributes
    @Test
    fun `when addCustomAttribute is called then it logs correctly`() {
        val givenCustomAttributes = TestValues.defaultMap
        val givenState = "ACTIVE"
        val expectedMessage = "add: $givenCustomAttributes | state = $givenState"

        val result = LogMessages.addCustomAttribute(givenCustomAttributes, givenState)

        assertThat(result).isEqualTo(expectedMessage)
    }

    // Quick Replies
    @Test
    fun `when quickReplyPrepareToSend is called then it logs correctly`() {
        val givenMessage = Message()
        val expectedMessage = "Message with quick reply prepared to send: $givenMessage"

        val result = LogMessages.quickReplyPrepareToSend(givenMessage)

        assertThat(result).isEqualTo(expectedMessage)
    }

    @Test
    fun `when sendQuickReply is called then it logs correctly`() {
        val givenButtonResponse = QuickReplyTestValues.buttonResponse_a
        val expectedMessage = "sendQuickReply(buttonResponse: $givenButtonResponse)"

        val result = LogMessages.sendQuickReply(givenButtonResponse)

        assertThat(result).isEqualTo(expectedMessage)
    }

    @Test
    fun `when ignoreInboundEvent is called then it logs correctly`() {
        val givenEvent = Event.ConversationDisconnect
        val expectedMessage = "Ignore inbound event: $givenEvent."

        val result = LogMessages.ignoreInboundEvent(givenEvent)

        assertThat(result).isEqualTo(expectedMessage)
    }
}
