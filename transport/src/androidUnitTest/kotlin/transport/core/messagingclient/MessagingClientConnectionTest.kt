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
import com.genesys.cloud.messenger.transport.core.TransportSDKException
import com.genesys.cloud.messenger.transport.core.events.Event
import com.genesys.cloud.messenger.transport.core.isClosed
import com.genesys.cloud.messenger.transport.core.isConfigured
import com.genesys.cloud.messenger.transport.core.isConnected
import com.genesys.cloud.messenger.transport.core.isConnecting
import com.genesys.cloud.messenger.transport.core.isError
import com.genesys.cloud.messenger.transport.network.PlatformSocketListener
import io.mockk.every
import io.mockk.invoke
import io.mockk.slot
import io.mockk.verify
import org.junit.Test
import transport.util.Request
import transport.util.Response
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.assertFailsWith

class MessagingClientConnectionTest : BaseMessagingClientTest() {

    @BeforeTest
    fun setupTracingIdProvider() {
        initTracingIdProvider()
    }

    @AfterTest
    fun cleanupTracingIdProvider() {
        resetTracingIdProvider()
    }

    @Test
    fun `when stateListener is not set`() {
        subject.stateChangedListener = null

        assertThat(subject.stateChangedListener).isNull()
    }

    @Test
    fun `when connect`() {
        subject.connect()
        slot.captured.onMessage(Response.configureSuccess())

        (subject.currentState as MessagingClient.State.Configured).run {
            assertThat(this).isConfigured(connected = true, newSession = true)
            assertThat(connected).isTrue()
            assertThat(newSession).isTrue()
        }
    }

    @Test
    fun `when connect and then disconnect`() {
        val expectedState = MessagingClient.State.Closed(1000, "The user has closed the connection.")
        subject.connect()
        slot.captured.onMessage(Response.configureSuccess())

        subject.disconnect()

        assertThat(subject.currentState).isClosed(expectedState.code, expectedState.reason)
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
        slot.captured.onMessage(Response.configureSuccess())

        slot.captured.onFailure(expectedException, ErrorCode.WebsocketError)

        assertThat(subject.currentState).isError(
            expectedErrorState.code,
            expectedErrorState.message
        )
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
    }

    @Test
    fun `when connect has ClientResponseError`() {
        val expectedErrorCode = ErrorCode.ClientResponseError(400)
        val expectedErrorMessage = "Request failed."
        every { mockPlatformSocket.sendMessage(Request.configureRequest()) } answers {
            slot.captured.onMessage(Response.webSocketRequestFailed)
        }

        subject.connect()

        assertThat(subject.currentState).isError(expectedErrorCode, expectedErrorMessage)
    }

    @Test
    fun `when configure fails because SocketListener respond with NetworkDisabled error`() {
        val givenException = Exception(ErrorMessage.InternetConnectionIsOffline)
        val expectedErrorState =
            MessagingClient.State.Error(ErrorCode.NetworkDisabled, ErrorMessage.InternetConnectionIsOffline)

        subject.connect()
        slot.captured.onFailure(givenException, ErrorCode.NetworkDisabled)

        assertThat(subject.currentState).isError(
            expectedErrorState.code,
            expectedErrorState.message
        )
    }

    @Test
    fun `when SocketListener invoke onMessage with SessionExpired error message`() {
        subject.connect()
        slot.captured.onMessage(Response.configureSuccess())

        slot.captured.onMessage(Response.sessionExpired)
    }

    @Test
    fun `when SocketListener invoke onMessage with ClientResponseError while reconnecting`() {
        val expectedErrorCode = ErrorCode.ClientResponseError(400)
        val expectedErrorMessage = "Request failed."
        var callCount = 0
        every { mockPlatformSocket.sendMessage(Request.configureRequest()) } answers {
            if (callCount++ == 0) {
                slot.captured.onMessage(Response.configureSuccess())
            } else {
                slot.captured.onMessage(Response.webSocketRequestFailed)
            }
        }
        every { mockReconnectionHandler.shouldReconnect } returns true
        every { mockReconnectionHandler.reconnect(captureLambda()) } answers { lambda<() -> Unit>().invoke() }

        subject.connect()
        // Initiate reconnection flow.
        slot.captured.onFailure(Exception(), ErrorCode.WebsocketError)

        assertThat(subject.currentState).isError(expectedErrorCode, expectedErrorMessage)
    }

    @Test
    fun `when SocketListener invoke onMessage with SessionNotFound error message`() {
        every { mockPlatformSocket.sendMessage(Request.configureRequest()) } answers {
            slot.captured.onMessage(Response.sessionNotFound)
        }

        subject.connect()
    }

    @Test
    fun `when SocketListener invoke onFailure with unknown error code`() {
        subject.connect()
        slot.captured.onMessage(Response.configureSuccess())
        slot.captured.onFailure(Exception(), ErrorCode.UnexpectedError)
    }

    @Test
    fun `when SocketListener invoke onMessage with unknown error string`() {

        subject.connect()
        slot.captured.onMessage(Response.configureSuccess())
        slot.captured.onMessage(Response.unknownErrorEvent)
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
        slot.captured.onMessage(Response.configureSuccess())

        (subject.currentState as MessagingClient.State.Configured).run {
            assertThat(this).isConfigured(connected = true, newSession = true)
            assertThat(connected).isTrue()
            assertThat(newSession).isTrue()
        }
    }

    @Test
    fun `when configure response has clearedExistingSession=false`() {
        subject.connect()
        slot.captured.onMessage(Response.configureSuccess())

        (subject.currentState as MessagingClient.State.Configured).run {
            assertThat(this).isConfigured(connected = true, newSession = true)
            assertThat(connected).isTrue()
            assertThat(newSession).isTrue()
        }
        verify(exactly = 0) {
            mockEventHandler.onEvent(eq(Event.ExistingAuthSessionCleared))
        }
    }

    @Test
    fun `when unstructured error message with ErrorCode_CannotDowngradeToUnauthenticated is received`() {
        every { mockPlatformSocket.sendMessage(Request.configureRequest()) } answers {
            slot.captured.onMessage(Response.cannotDowngradeToUnauthenticated)
        }
        subject.connect()
    }

    @Test
    fun `when connect is called without deployment config`() {
        every { mockDeploymentConfig.get() } returns null

        val exception = assertFailsWith<TransportSDKException> {
            subject.connect()
        }

        assertThat(exception.errorCode).isEqualTo(ErrorCode.MissingDeploymentConfig)
        assertThat(exception.message).isEqualTo(ErrorMessage.MissingDeploymentConfig)
    }

    @Test
    fun `when connectAuthenticatedSession is called without deployment config`() {
        every { mockDeploymentConfig.get() } returns null

        val exception = assertFailsWith<TransportSDKException> {
            subject.connectAuthenticatedSession()
        }

        assertThat(exception.errorCode).isEqualTo(ErrorCode.MissingDeploymentConfig)
        assertThat(exception.message).isEqualTo(ErrorMessage.MissingDeploymentConfig)
    }
}
