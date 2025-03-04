package transport.core.messagingclient

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.genesys.cloud.messenger.transport.core.CorrectiveAction
import com.genesys.cloud.messenger.transport.core.ErrorCode
import com.genesys.cloud.messenger.transport.core.ErrorMessage
import com.genesys.cloud.messenger.transport.core.events.Event
import com.genesys.cloud.messenger.transport.core.isClosed
import com.genesys.cloud.messenger.transport.core.isConfigured
import com.genesys.cloud.messenger.transport.core.isError
import com.genesys.cloud.messenger.transport.core.isIdle
import com.genesys.cloud.messenger.transport.core.isReadOnly
import com.genesys.cloud.messenger.transport.core.isReconnecting
import com.genesys.cloud.messenger.transport.shyrka.receive.Apps
import com.genesys.cloud.messenger.transport.shyrka.receive.Conversations
import com.genesys.cloud.messenger.transport.shyrka.receive.createConversationsVOForTesting
import com.genesys.cloud.messenger.transport.shyrka.receive.createDeploymentConfigForTesting
import com.genesys.cloud.messenger.transport.shyrka.receive.createMessengerVOForTesting
import com.genesys.cloud.messenger.transport.util.logs.LogMessages
import io.mockk.every
import io.mockk.verify
import io.mockk.verifySequence
import org.junit.Test
import transport.util.Request
import transport.util.Response
import kotlin.test.assertFailsWith

class MCClearConversationTests : BaseMessagingClientTest() {

    @Test
    fun `when clearConversation from any non ReadOnly or Configured states`() {
        // currentState = Idle
        assertThat(subject.currentState).isIdle()
        assertFailsWith<IllegalStateException>("MessagingClient is not in Configured or ReadOnly state.") { subject.clearConversation() }

        subject.connect()
        // currentState = Reconnecting
        every { mockReconnectionHandler.shouldReconnect } returns true
        val givenException = Exception(ErrorMessage.InternetConnectionIsOffline)
        slot.captured.onFailure(givenException, ErrorCode.WebsocketError)

        assertThat(subject.currentState).isReconnecting()
        assertFailsWith<IllegalStateException>("MessagingClient is not in Configured or ReadOnly state.") { subject.clearConversation() }

        // currentState = Closed
        subject.disconnect()
        assertThat(subject.currentState).isClosed(1000, "The user has closed the connection.")
        assertFailsWith<IllegalStateException>("MessagingClient is not in Configured or ReadOnly state.") { subject.clearConversation() }

        // currentState = Error
        every { mockPlatformSocket.sendMessage(Request.configureRequest()) } answers {
            slot.captured.onMessage(Response.sessionNotFound)
        }
        subject.connect()
        assertThat(subject.currentState).isError(ErrorCode.SessionNotFound, "session not found error message")
        assertFailsWith<IllegalStateException>("MessagingClient is not in Configured or ReadOnly state.") { subject.clearConversation() }
    }

    @Test
    fun `when clearConversation api is called but clearConversation is disabled in deploymentConfig`() {
        every { mockDeploymentConfig.get() } returns createDeploymentConfigForTesting(
            messenger = createMessengerVOForTesting(
                apps = Apps(
                    conversations = createConversationsVOForTesting(
                        conversationClear = Conversations.ConversationClear(enabled = false)
                    )
                )
            )
        )
        val expectedEvent = Event.Error(
            errorCode = ErrorCode.ClearConversationFailure,
            message = ErrorMessage.FailedToClearConversation,
            correctiveAction = CorrectiveAction.Forbidden,
        )
        subject.connect()

        subject.clearConversation()

        verifySequence {
            connectSequence()
            mockEventHandler.onEvent(eq(expectedEvent))
        }
        verify(exactly = 0) {
            mockPlatformSocket.sendMessage(eq(Request.clearConversation))
        }
    }

    @Test
    fun `when session is configured and clearConversation api is called`() {
        subject.connect()

        subject.clearConversation()

        assertThat(subject.currentState).isConfigured(connected = true, newSession = true)
        verifySequence {
            connectSequence()
            mockLogger.i(capture(logSlot))
            mockPlatformSocket.sendMessage(eq(Request.clearConversation))
        }
        assertThat(logSlot[0].invoke()).isEqualTo(LogMessages.CONNECT)
        assertThat(logSlot[1].invoke()).isEqualTo(LogMessages.configureSession(Request.token))
        assertThat(logSlot[2].invoke()).isEqualTo(LogMessages.SEND_CLEAR_CONVERSATION)
    }

    @Test
    fun `when session is readOnly and clearConversation api is called`() {
        every { mockPlatformSocket.sendMessage(Request.configureRequest()) } answers {
            slot.captured.onMessage(Response.configureSuccess(readOnly = true))
        }
        subject.connect()

        subject.clearConversation()

        assertThat(subject.currentState).isReadOnly()
        verifySequence {
            connectToReadOnlySequence()
            mockLogger.i(capture(logSlot))
            mockPlatformSocket.sendMessage(eq(Request.clearConversation))
        }
    }

    @Test
    fun `when event SessionCleared received`() {
        val expectedEvent = Event.ConversationCleared

        subject.connect()
        slot.captured.onMessage(Response.sessionClearedEvent)

        verifySequence {
            connectSequence()
            mockEventHandler.onEvent(eq(expectedEvent))
        }
    }

    @Test
    fun `when clearConversation request fails and error message contains Conversation Clear String`() {
        every { mockPlatformSocket.sendMessage(Request.clearConversation) } answers {
            slot.captured.onMessage(Response.clearConversationForbidden())
        }
        val expectedEvent = Event.Error(
            errorCode = ErrorCode.ClearConversationFailure,
            message = "Presence events Conversation Clear are not supported",
            correctiveAction = CorrectiveAction.Forbidden,
        )
        subject.connect()

        subject.clearConversation()

        assertThat(subject.currentState).isConfigured(connected = true, newSession = true)
        verifySequence {
            connectSequence()
            mockLogger.i(capture(logSlot))
            mockPlatformSocket.sendMessage(eq(Request.clearConversation))
            mockEventHandler.onEvent(expectedEvent)
        }
        assertThat(logSlot[0].invoke()).isEqualTo(LogMessages.CONNECT)
        assertThat(logSlot[1].invoke()).isEqualTo(LogMessages.configureSession(Request.token))
        assertThat(logSlot[2].invoke()).isEqualTo(LogMessages.SEND_CLEAR_CONVERSATION)
    }

    @Test
    fun `when clearConversation request fails and error message contains Conversation Clear String in wrong order`() {
        // Test incorrect order of "Conversation" and "Clear".
        every { mockPlatformSocket.sendMessage(Request.clearConversation) } answers {
            slot.captured.onMessage(Response.clearConversationForbidden("Presence events Clear Conversation are not supported"))
        }
        val expectedEventCase1 = Event.Error(
            errorCode = ErrorCode.ClientResponseError(403),
            message = "Presence events Clear Conversation are not supported",
            correctiveAction = CorrectiveAction.Forbidden,
        )
        subject.connect()

        subject.clearConversation()

        verify {
            mockPlatformSocket.sendMessage(eq(Request.clearConversation))
            mockEventHandler.onEvent(expectedEventCase1)
        }

        // Test "Conversation" and "Clear" Strings have word between them.
        every { mockPlatformSocket.sendMessage(Request.clearConversation) } answers {
            slot.captured.onMessage(Response.clearConversationForbidden("Presence events Clear THE Conversation are not supported"))
        }
        val expectedEventCase2 = Event.Error(
            errorCode = ErrorCode.ClientResponseError(403),
            message = "Presence events Clear THE Conversation are not supported",
            correctiveAction = CorrectiveAction.Forbidden,
        )
        subject.clearConversation()

        verify {
            mockPlatformSocket.sendMessage(eq(Request.clearConversation))
            mockEventHandler.onEvent(expectedEventCase2)
        }

        // Test "Conversation" and "Clear" has no space between them.
        every { mockPlatformSocket.sendMessage(Request.clearConversation) } answers {
            slot.captured.onMessage(Response.clearConversationForbidden("Presence events ClearConversation are not supported"))
        }
        val expectedEventCase3 = Event.Error(
            errorCode = ErrorCode.ClientResponseError(403),
            message = "Presence events ClearConversation are not supported",
            correctiveAction = CorrectiveAction.Forbidden,
        )
        subject.clearConversation()

        verify {
            mockPlatformSocket.sendMessage(eq(Request.clearConversation))
            mockEventHandler.onEvent(expectedEventCase3)
        }
    }
}
