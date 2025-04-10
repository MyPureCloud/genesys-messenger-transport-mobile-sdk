package transport.core.messagingclient

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import assertk.assertions.isTrue
import com.genesys.cloud.messenger.transport.core.CorrectiveAction
import com.genesys.cloud.messenger.transport.core.ErrorCode
import com.genesys.cloud.messenger.transport.core.ErrorMessage
import com.genesys.cloud.messenger.transport.core.MessagingClient
import com.genesys.cloud.messenger.transport.core.StateChange
import com.genesys.cloud.messenger.transport.core.events.Event
import com.genesys.cloud.messenger.transport.core.isClosed
import com.genesys.cloud.messenger.transport.core.isConfigured
import com.genesys.cloud.messenger.transport.core.isConnected
import com.genesys.cloud.messenger.transport.core.isConnecting
import com.genesys.cloud.messenger.transport.core.isError
import com.genesys.cloud.messenger.transport.network.PlatformSocketListener
import com.genesys.cloud.messenger.transport.util.logs.LogMessages
import com.genesys.cloud.messenger.transport.utility.ErrorTest
import io.mockk.every
import io.mockk.invoke
import io.mockk.slot
import io.mockk.verify
import io.mockk.verifySequence
import org.junit.Test
import transport.util.Request
import transport.util.Response
import transport.util.fromConfiguredToError
import transport.util.fromConfiguredToReconnecting
import transport.util.fromConnectedToError
import transport.util.fromIdleToConnecting
import transport.util.fromReconnectingToError
import kotlin.test.assertFailsWith

class MCConnectionTests : BaseMessagingClientTest() {

    @Test
    fun `when stateListener is not set`() {
        subject.stateChangedListener = null

        assertThat(subject.stateChangedListener).isNull()
    }

    @Test
    fun `when connect`() {
        subject.connect()

        (subject.currentState as MessagingClient.State.Configured).run {
            assertThat(this).isConfigured(connected = true, newSession = true)
            assertThat(connected).isTrue()
            assertThat(newSession).isTrue()
        }
        verifySequence {
            connectSequence()
        }
        assertThat(logSlot[0].invoke()).isEqualTo(LogMessages.CONNECT)
        assertThat(logSlot[1].invoke()).isEqualTo(LogMessages.configureSession())
    }

    @Test
    fun `when connect and then disconnect`() {
        val expectedState = MessagingClient.State.Closed(1000, "The user has closed the connection.")
        subject.connect()

        subject.disconnect()

        assertThat(subject.currentState).isClosed(expectedState.code, expectedState.reason)
        verifySequence {
            connectSequence()
            disconnectSequence(expectedState.code, expectedState.reason)
        }
    }

    @Test
    fun `when not connected and disconnect`() {
        assertFailsWith<IllegalStateException> {
            subject.disconnect()
        }
    }

    @Test
    fun `when connect fails and socketListener respond with WebsocketError and there are no reconnection attempts left`() {
        val expectedException = Exception(ErrorMessage.FailedToReconnect)
        val expectedErrorState =
            MessagingClient.State.Error(ErrorCode.WebsocketError, ErrorMessage.FailedToReconnect)

        subject.connect()

        slot.captured.onFailure(expectedException, ErrorCode.WebsocketError)

        assertThat(subject.currentState).isError(
            expectedErrorState.code,
            expectedErrorState.message
        )
        verifySequence {
            connectSequence()
            mockLogger.i(capture(logSlot))
            mockMessageStore.invalidateConversationCache()
            mockReconnectionHandler.shouldReconnect
            errorSequence(fromConfiguredToError(expectedErrorState))
        }
        assertThat(logSlot[0].invoke()).isEqualTo(LogMessages.CONNECT)
        assertThat(logSlot[1].invoke()).isEqualTo(LogMessages.configureSession())
        assertThat(logSlot[2].invoke()).isEqualTo(LogMessages.CLEAR_CONVERSATION_HISTORY)
    }

    @Test
    fun `when connect fails with WebsocketAccessDenied`() {
        val message = CorrectiveAction.Forbidden.message
        val expectedErrorState = MessagingClient.State.Error(ErrorCode.WebsocketAccessDenied, message)
        val accessException = Exception(message)
        val socketListenerSlot = slot<PlatformSocketListener>()
        every { mockPlatformSocket.openSocket(capture((socketListenerSlot))) } answers {
            socketListenerSlot.captured.onFailure(accessException, ErrorCode.WebsocketAccessDenied)
        }

        subject.connect()

        assertThat(subject.currentState).isError(
            expectedErrorState.code,
            expectedErrorState.message
        )
        verifySequence {
            mockStateChangedListener(fromIdleToConnecting)
            mockPlatformSocket.openSocket(any())
            mockMessageStore.invalidateConversationCache()
            errorSequence(
                StateChange(
                    oldState = MessagingClient.State.Connecting,
                    newState = expectedErrorState
                )
            )
        }
    }

    @Test
    fun `when connect has ClientResponseError`() {
        val expectedErrorCode = ErrorCode.ClientResponseError(400)
        val expectedErrorMessage = "Request failed."
        val expectedErrorState = MessagingClient.State.Error(expectedErrorCode, expectedErrorMessage)
        every { mockPlatformSocket.sendMessage(Request.configureRequest()) } answers {
            slot.captured.onMessage(Response.webSocketRequestFailed)
        }

        subject.connect()

        assertThat(subject.currentState).isError(expectedErrorCode, expectedErrorMessage)
        verifySequence {
            connectWithFailedConfigureSequence()
            errorSequence(fromConnectedToError(expectedErrorState))
        }
    }

    @Test
    fun `when configure fails because SocketListener respond with NetworkDisabled error`() {
        val givenException = Exception(ErrorMessage.InternetConnectionIsOffline)
        val expectedErrorState =
            MessagingClient.State.Error(ErrorCode.NetworkDisabled, ErrorMessage.InternetConnectionIsOffline)
        every { mockPlatformSocket.sendMessage(Request.configureRequest()) } answers {
            slot.captured.onFailure(givenException, ErrorCode.NetworkDisabled)
        }

        subject.connect()

        assertThat(subject.currentState).isError(
            expectedErrorState.code,
            expectedErrorState.message
        )
        verifySequence {
            connectWithFailedConfigureSequence()
            errorSequence(fromConnectedToError(expectedErrorState))
        }
    }

    @Test
    fun `when SocketListener invoke onMessage with SessionExpired error message`() {
        val expectedErrorState =
            MessagingClient.State.Error(ErrorCode.SessionHasExpired, "session expired error message")
        subject.connect()

        slot.captured.onMessage(Response.sessionExpired)

        verifySequence {
            connectSequence()
            errorSequence(fromConfiguredToError(expectedErrorState))
        }
    }

    @Test
    fun `when SocketListener invoke onMessage with ClientResponseError while reconnecting`() {
        val expectedErrorCode = ErrorCode.ClientResponseError(400)
        val expectedErrorMessage = "Request failed."
        val expectedErrorState = MessagingClient.State.Error(expectedErrorCode, expectedErrorMessage)
        every { mockPlatformSocket.sendMessage(Request.configureRequest()) } answers {
            if (subject.currentState == MessagingClient.State.Reconnecting) {
                slot.captured.onMessage(Response.webSocketRequestFailed)
            } else {
                slot.captured.onMessage(Response.configureSuccess())
            }
        }
        every { mockReconnectionHandler.shouldReconnect } returns true
        every { mockReconnectionHandler.reconnect(captureLambda()) } answers { lambda<() -> Unit>().invoke() }

        subject.connect()
        // Initiate reconnection flow.
        slot.captured.onFailure(Exception(), ErrorCode.WebsocketError)

        assertThat(subject.currentState).isError(expectedErrorCode, expectedErrorMessage)
        verifySequence {
            connectSequence()
            mockLogger.i(capture(logSlot))
            mockReconnectionHandler.shouldReconnect
            mockStateChangedListener(fromConfiguredToReconnecting())
            mockReconnectionHandler.reconnect(any())
            mockLogger.i(capture(logSlot))
            mockPlatformSocket.openSocket(any())
            mockLogger.i(capture(logSlot))
            mockPlatformSocket.sendMessage(eq(Request.configureRequest()))
            errorSequence(fromReconnectingToError(expectedErrorState))
        }
        assertThat(logSlot[0].invoke()).isEqualTo(LogMessages.CONNECT)
        assertThat(logSlot[1].invoke()).isEqualTo(LogMessages.configureSession())
        assertThat(logSlot[2].invoke()).isEqualTo(LogMessages.CLEAR_CONVERSATION_HISTORY)
        assertThat(logSlot[3].invoke()).isEqualTo(LogMessages.CONNECT)
        assertThat(logSlot[4].invoke()).isEqualTo(LogMessages.configureSession())
    }

    @Test
    fun `when SocketListener invoke onMessage with SessionNotFound error message`() {
        val expectedErrorState =
            MessagingClient.State.Error(ErrorCode.SessionNotFound, "session not found error message")
        every { mockPlatformSocket.sendMessage(Request.configureRequest()) } answers {
            slot.captured.onMessage(Response.sessionNotFound)
        }

        subject.connect()

        verifySequence {
            connectWithFailedConfigureSequence()
            errorSequence(fromConnectedToError(expectedErrorState))
        }
    }

    @Test
    fun `when SocketListener invoke onFailure with unknown error code`() {
        subject.connect()
        slot.captured.onFailure(Exception(), ErrorCode.UnexpectedError)

        verifySequence {
            connectSequence()
            mockLogger.i(capture(logSlot))
            mockLogger.w(capture(logSlot))
        }
        assertThat(logSlot[0].invoke()).isEqualTo(LogMessages.CONNECT)
        assertThat(logSlot[1].invoke()).isEqualTo(LogMessages.configureSession())
        assertThat(logSlot[2].invoke()).isEqualTo(LogMessages.CLEAR_CONVERSATION_HISTORY)
    }

    @Test
    fun `when SocketListener invoke onMessage with unknown error string`() {

        subject.connect()
        slot.captured.onMessage(Response.unknownErrorEvent)

        verifySequence {
            connectSequence()
            mockLogger.w(capture(logSlot))
        }
        assertThat(logSlot[0].invoke()).isEqualTo(LogMessages.CONNECT)
        assertThat(logSlot[1].invoke()).isEqualTo(LogMessages.configureSession())
        assertThat(logSlot[2].invoke()).isEqualTo(LogMessages.unhandledErrorCode(ErrorCode.UnexpectedError, "Request failed."))
    }

    @Test
    fun `when StateChange is tested`() {
        val subject = StateChange(
            oldState = MessagingClient.State.Connecting,
            newState = MessagingClient.State.Connected,
        )

        assertThat(subject.oldState).isConnecting()
        assertThat(subject.newState).isConnected()
    }

    @Test
    fun `when configure response has clearedExistingSession=true`() {
        every { mockPlatformSocket.sendMessage(Request.configureRequest()) } answers {
            slot.captured.onMessage(Response.configureSuccess(clearedExistingSession = true))
        }
        subject.connect()

        (subject.currentState as MessagingClient.State.Configured).run {
            assertThat(this).isConfigured(connected = true, newSession = true)
            assertThat(connected).isTrue()
            assertThat(newSession).isTrue()
        }
        verifySequence {
            connectSequence()
            mockEventHandler.onEvent(eq(Event.ExistingAuthSessionCleared))
        }
        assertThat(logSlot[0].invoke()).isEqualTo(LogMessages.CONNECT)
        assertThat(logSlot[1].invoke()).isEqualTo(LogMessages.configureSession())
    }

    @Test
    fun `when configure response has clearedExistingSession=false`() {
        subject.connect()

        (subject.currentState as MessagingClient.State.Configured).run {
            assertThat(this).isConfigured(connected = true, newSession = true)
            assertThat(connected).isTrue()
            assertThat(newSession).isTrue()
        }
        verifySequence {
            connectSequence()
        }
        verify(exactly = 0) {
            mockEventHandler.onEvent(eq(Event.ExistingAuthSessionCleared))
        }
        assertThat(logSlot[0].invoke()).isEqualTo(LogMessages.CONNECT)
        assertThat(logSlot[1].invoke()).isEqualTo(LogMessages.configureSession())
    }

    @Test
    fun `when unstructured error message with ErrorCode_CannotDowngradeToUnauthenticated is received`() {
        every { mockPlatformSocket.sendMessage(Request.configureRequest()) } answers {
            slot.captured.onMessage(Response.cannotDowngradeToUnauthenticated)
        }
        subject.connect()

        verifySequence {
            fromIdleToConnectedSequence()
            mockLogger.i(capture(logSlot))
            mockPlatformSocket.sendMessage(Request.configureRequest())
            invalidateSessionTokenSequence()
            mockStateChangedListener(fromConnectedToError(MessagingClient.State.Error(ErrorCode.CannotDowngradeToUnauthenticated, ErrorTest.MESSAGE)))
        }
    }
}
