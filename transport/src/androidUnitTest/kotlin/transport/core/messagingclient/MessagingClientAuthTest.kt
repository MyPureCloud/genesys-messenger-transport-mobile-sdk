package transport.core.messagingclient

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.genesys.cloud.messenger.transport.auth.NO_JWT
import com.genesys.cloud.messenger.transport.core.Empty
import com.genesys.cloud.messenger.transport.core.ErrorCode
import com.genesys.cloud.messenger.transport.core.ErrorMessage
import com.genesys.cloud.messenger.transport.core.MessagingClient
import com.genesys.cloud.messenger.transport.core.Result
import com.genesys.cloud.messenger.transport.core.events.Event
import com.genesys.cloud.messenger.transport.core.isClosed
import com.genesys.cloud.messenger.transport.core.isConfigured
import com.genesys.cloud.messenger.transport.core.isError
import com.genesys.cloud.messenger.transport.core.isIdle
import com.genesys.cloud.messenger.transport.core.isReadOnly
import com.genesys.cloud.messenger.transport.core.isReconnecting
import com.genesys.cloud.messenger.transport.util.logs.LogMessages
import com.genesys.cloud.messenger.transport.utility.AuthTest
import com.genesys.cloud.messenger.transport.utility.ErrorTest
import io.mockk.every
import io.mockk.invoke
import io.mockk.verify
import io.mockk.verifySequence
import org.junit.Test
import transport.util.Request
import transport.util.Response
import transport.util.fromConfiguredToError
import transport.util.fromConfiguredToReconnecting
import transport.util.fromConnectedToConfigured
import transport.util.fromConnectedToError
import transport.util.fromConnectingToConnected
import transport.util.fromIdleToConnecting
import transport.util.fromReconnectingToError
import kotlin.test.assertFailsWith

class MessagingClientAuthTest : BaseMessagingClientTest() {

    @Test
    fun `when authorize is called`() {
        subject.authorize(AuthTest.AUTH_CODE, AuthTest.JWT_AUTH_URL, AuthTest.CODE_VERIFIER)

        verify {
            mockAuthHandler.authorize(AuthTest.AUTH_CODE, AuthTest.JWT_AUTH_URL, AuthTest.CODE_VERIFIER)
        }
    }

    @Test
    fun `when shouldAuthorize is called`() {
        subject.shouldAuthorize(
            callback = { true }
        )

        verify {
            mockAuthHandler.shouldAuthorize(any())
        }
    }

    @Test
    fun `when connectAuthenticatedSession`() {
        subject.connectAuthenticatedSession()

        assertThat(subject.currentState).isConfigured(connected = true, newSession = true)
        verifySequence {
            connectSequence(shouldConfigureAuth = true)
        }
    }

    @Test
    fun `when connectAuthenticatedSession and AuthHandler has no Jwt and refreshToken was successful`() {
        every { mockAuthHandler.jwt } returns NO_JWT
        every { mockAuthHandler.refreshToken(captureLambda()) } answers {
            every { mockAuthHandler.jwt } returns AuthTest.JWT_TOKEN
            lambda<(Result<Empty>) -> Unit>().invoke(Result.Success(Empty()))
        }

        subject.connectAuthenticatedSession()

        assertThat(subject.currentState).isConfigured(connected = true, newSession = true)
        verifySequence {
            mockStateChangedListener(fromIdleToConnecting)
            mockPlatformSocket.openSocket(any())
            mockStateChangedListener(fromConnectingToConnected)
            mockAuthHandler.jwt
            mockAuthHandler.refreshToken(any())
            mockAuthHandler.jwt
            mockAuthHandler.jwt
            mockPlatformSocket.sendMessage(Request.configureAuthenticatedRequest())
            mockReconnectionHandler.clear()
            mockStateChangedListener(fromConnectedToConfigured)
        }
    }

    @Test
    fun `when connectAuthenticatedSession and AuthHandler has no Jwt and refreshToken fails`() {
        val givenFailure = Result.Failure(ErrorCode.AuthFailed, ErrorTest.MESSAGE)
        every { mockAuthHandler.refreshToken(captureLambda()) } answers {
            lambda<(Result<Empty>) -> Unit>().invoke(givenFailure)
        }
        every { mockAuthHandler.jwt } returns NO_JWT

        val expectedErrorState =
            MessagingClient.State.Error(ErrorCode.AuthFailed, ErrorTest.MESSAGE)

        subject.connectAuthenticatedSession()

        assertThat(subject.currentState).isError(
            expectedErrorState.code,
            expectedErrorState.message
        )
        verifySequence {
            mockStateChangedListener(fromIdleToConnecting)
            mockPlatformSocket.openSocket(any())
            mockStateChangedListener(fromConnectingToConnected)
            mockAuthHandler.jwt
            mockAuthHandler.refreshToken(any())
            errorSequence(fromConnectedToError(expectedErrorState))
        }
    }

    @Test
    fun `when logoutFromAuthenticatedSession and WebSocket is not configured`() {
        subject.logoutFromAuthenticatedSession()

        verify {
            mockAuthHandler.logout()
        }
    }

    @Test
    fun `when logoutFromAuthenticatedSession from any state`() {
        // Test from Idle state
        assertThat(subject.currentState).isIdle()
        subject.logoutFromAuthenticatedSession()
        verify { mockAuthHandler.logout() }

        // Test from Error state
        every { mockPlatformSocket.sendMessage(Request.configureRequest()) } answers {
            slot.captured.onMessage(Response.sessionNotFound)
        }
        subject.connect()
        assertThat(subject.currentState).isError(ErrorCode.SessionNotFound, "session not found error message")
        subject.logoutFromAuthenticatedSession()
        verify(exactly = 2) { mockAuthHandler.logout() }

        // Test from ReadOnly state
        every { mockPlatformSocket.sendMessage(Request.configureRequest()) } answers {
            slot.captured.onMessage(Response.configureSuccess(readOnly = true))
        }
        subject.connect()
        assertThat(subject.currentState).isReadOnly()
        subject.logoutFromAuthenticatedSession()
        verify(exactly = 3) { mockAuthHandler.logout() }
    }

    @Test
    fun `when logoutFromAuthenticatedSession and authenticated session is configured`() {
        subject.connectAuthenticatedSession()

        subject.logoutFromAuthenticatedSession()

        verifySequence {
            connectSequence(shouldConfigureAuth = true)
            mockAuthHandler.logout()
        }
    }

    @Test
    fun `when event Logout received`() {
        val expectedEvent = Event.Logout

        subject.connect()
        slot.captured.onMessage(Response.logoutEvent)

        verifySequence {
            connectSequence()
            invalidateSessionTokenSequence()
            mockVault.wasAuthenticated = false
            mockAuthHandler.clear()
            mockEventHandler.onEvent(eq(expectedEvent))
            disconnectSequence()
        }
        assertThat(logSlot[0].invoke()).isEqualTo(LogMessages.CONNECT)
        assertThat(logSlot[1].invoke()).isEqualTo(LogMessages.configureSession(Request.token, false))
        assertThat(logSlot[2].invoke()).isEqualTo(LogMessages.INVALIDATE_SESSION_TOKEN)
    }

    @Test
    fun `when connectAuthenticatedSession respond with Unauthorized all the time`() {
        every {
            mockPlatformSocket.sendMessage(Request.configureAuthenticatedRequest())
        } answers {
            slot.captured.onMessage(Response.unauthorized)
        }
        val expectedErrorState =
            MessagingClient.State.Error(ErrorCode.ClientResponseError(401), "User is unauthorized")

        subject.connectAuthenticatedSession()

        assertThat(subject.currentState).isError(
            expectedErrorState.code,
            expectedErrorState.message
        )
        verifySequence {
            mockStateChangedListener(fromIdleToConnecting)
            mockPlatformSocket.openSocket(any())
            mockStateChangedListener(fromConnectingToConnected)
            mockAuthHandler.jwt
            mockAuthHandler.jwt
            mockPlatformSocket.sendMessage(Request.configureAuthenticatedRequest())
            mockAuthHandler.refreshToken(any())
            mockAuthHandler.jwt
            mockAuthHandler.jwt
            mockPlatformSocket.sendMessage(Request.configureAuthenticatedRequest())
            mockAuthHandler.refreshToken(any())
            mockAuthHandler.jwt
            mockAuthHandler.jwt
            mockPlatformSocket.sendMessage(Request.configureAuthenticatedRequest())
            mockAuthHandler.refreshToken(any())
            mockAuthHandler.jwt
            mockAuthHandler.jwt
            mockPlatformSocket.sendMessage(Request.configureAuthenticatedRequest())
            errorSequence(fromConnectedToError(expectedErrorState))
        }
    }

    @Test
    fun `when connectAuthenticatedSession fails with WebsocketError and no reconnection attempts left`() {
        val givenException = Exception(ErrorMessage.FailedToReconnect)
        val expectedErrorState =
            MessagingClient.State.Error(ErrorCode.WebsocketError, ErrorMessage.FailedToReconnect)

        subject.connectAuthenticatedSession()
        slot.captured.onFailure(givenException, ErrorCode.WebsocketError)

        assertThat(subject.currentState).isError(
            expectedErrorState.code,
            expectedErrorState.message
        )
        verifySequence {
            connectSequence(shouldConfigureAuth = true)
            mockLogger.i(capture(logSlot))
            mockMessageStore.invalidateConversationCache()
            mockReconnectionHandler.shouldReconnect
            errorSequence(fromConfiguredToError(expectedErrorState))
        }
        assertThat(logSlot[0].invoke()).isEqualTo(LogMessages.CONNECT_AUTHENTICATED_SESSION)
        assertThat(logSlot[1].invoke()).isEqualTo(LogMessages.configureAuthenticatedSession(Request.token, false))
        assertThat(logSlot[2].invoke()).isEqualTo(LogMessages.CLEAR_CONVERSATION_HISTORY)
    }

    @Test
    fun `when SocketListener invoke onMessage with ClientResponseError while reconnecting authenticated session`() {
        val expectedErrorCode = ErrorCode.ClientResponseError(400)
        val expectedErrorMessage = "Request failed."
        val expectedErrorState = MessagingClient.State.Error(expectedErrorCode, expectedErrorMessage)
        every { mockPlatformSocket.sendMessage(Request.configureAuthenticatedRequest()) } answers {
            if (subject.currentState == MessagingClient.State.Reconnecting) {
                slot.captured.onMessage(Response.webSocketRequestFailed)
            } else {
                slot.captured.onMessage(Response.configureSuccess())
            }
        }
        every { mockReconnectionHandler.shouldReconnect } returns true
        every { mockReconnectionHandler.reconnect(captureLambda()) } answers { lambda<() -> Unit>().invoke() }

        subject.connectAuthenticatedSession()
        // Initiate reconnection flow.
        slot.captured.onFailure(Exception(), ErrorCode.WebsocketError)

        assertThat(subject.currentState).isError(expectedErrorCode, expectedErrorMessage)
        verifySequence {
            connectSequence(shouldConfigureAuth = true)
            mockLogger.i(capture(logSlot))
            mockMessageStore.invalidateConversationCache()
            mockReconnectionHandler.shouldReconnect
            mockStateChangedListener(fromConfiguredToReconnecting())
            mockReconnectionHandler.reconnect(any())
            mockLogger.i(capture(logSlot))
            mockPlatformSocket.openSocket(any())
            mockLogger.i(capture(logSlot))
            mockAuthHandler.jwt
            mockAuthHandler.jwt
            mockPlatformSocket.sendMessage(eq(Request.configureAuthenticatedRequest()))
            errorSequence(fromReconnectingToError(expectedErrorState))
        }
        assertThat(logSlot[0].invoke()).isEqualTo(LogMessages.CONNECT_AUTHENTICATED_SESSION)
        assertThat(logSlot[1].invoke()).isEqualTo(LogMessages.configureAuthenticatedSession(Request.token, false))
        assertThat(logSlot[2].invoke()).isEqualTo(LogMessages.CLEAR_CONVERSATION_HISTORY)
        assertThat(logSlot[3].invoke()).isEqualTo(LogMessages.CONNECT_AUTHENTICATED_SESSION)
        assertThat(logSlot[4].invoke()).isEqualTo(LogMessages.configureAuthenticatedSession(Request.token, false))
    }

    @Test
    fun `when authenticated conversation was disconnected with ReadOnly and startNewChat resulted in ClientResponseError(401)`() {
        var configureSessionResponsesCounter = 0 // Needed to exit the retry attempts from failing Response.unauthorized
        every {
            mockPlatformSocket.sendMessage(
                match {
                    it.contains("\"action\":\"configureAuthenticatedSession\"") &&
                        it.contains("\"startNew\":false") &&
                        it.contains("\"data\":{\"code\":\"${AuthTest.JWT_TOKEN}\"}")
                }
            )
        } answers {
            slot.captured.onMessage(Response.configureSuccess())
        }

        every {
            mockPlatformSocket.sendMessage(
                match {
                    it.contains("\"action\":\"closeSession\"") &&
                        it.contains("\"closeAllConnections\":true")
                }
            )
        } answers {
            slot.captured.onMessage(Response.configureSuccess(connected = false, readOnly = true))
        }

        every {
            mockPlatformSocket.sendMessage(
                match {
                    it.contains("\"action\":\"configureAuthenticatedSession\"") &&
                        it.contains("\"startNew\":true") &&
                        it.contains("\"data\":{\"code\":\"${AuthTest.JWT_TOKEN}\"}")
                }
            )
        } answers {
            if (configureSessionResponsesCounter < 1) {
                configureSessionResponsesCounter++
                slot.captured.onMessage(Response.unauthorized)
            } else {
                slot.captured.onMessage(Response.configureSuccess())
            }
        }

        every { mockAuthHandler.refreshToken(captureLambda()) } answers {
            every { mockAuthHandler.jwt } returns AuthTest.JWT_TOKEN
            lambda<(Result<Empty>) -> Unit>().invoke(Result.Success(Empty()))
        }

        subject.connectAuthenticatedSession()
        slot.captured.onMessage(
            Response.structuredMessageWithEvents(
                events = Response.StructuredEvent.presenceDisconnect,
                metadata = mapOf("readOnly" to "true"),
            )
        )

        subject.startNewChat()

        assertThat(subject.currentState).isConfigured(connected = true, newSession = true)

        verify(exactly = 2) {
            mockPlatformSocket.sendMessage(
                match {
                    it.contains("\"action\":\"configureAuthenticatedSession\"") &&
                        it.contains("\"startNew\":true") &&
                        it.contains("\"data\":{\"code\":\"${AuthTest.JWT_TOKEN}\"}")
                }
            )
        }

        verify(exactly = 1) {
            mockPlatformSocket.sendMessage(
                match {
                    it.contains("\"action\":\"closeSession\"") &&
                        it.contains("\"closeAllConnections\":true")
                }
            )
        }

        verify(exactly = 1) {
            mockAuthHandler.refreshToken(any())
        }
    }

    @Test
    fun `when stepUpToAuthenticatedSession and currentState is Configured and AuthHandler has jwt`() {
        subject.connect()

        subject.stepUpToAuthenticatedSession()

        verifySequence {
            connectSequence()
            mockLogger.i(capture(logSlot))
            configureSequence(shouldConfigureAuth = true)
        }
        assertThat(logSlot[0].invoke()).isEqualTo(LogMessages.CONNECT)
        assertThat(logSlot[1].invoke()).isEqualTo(LogMessages.configureSession(Request.token, false))
        assertThat(logSlot[2].invoke()).isEqualTo(LogMessages.STEP_UP_TO_AUTHENTICATED_SESSION)
        assertThat(logSlot[3].invoke()).isEqualTo(LogMessages.configureAuthenticatedSession(Request.token, false))
    }

    @Test
    fun `when stepUpToAuthenticatedSession but current session is already Configured as authenticated`() {
        subject.connectAuthenticatedSession()

        subject.stepUpToAuthenticatedSession()

        verifySequence {
            connectSequence(shouldConfigureAuth = true)
            mockLogger.i(capture(logSlot))
        }
        assertThat(logSlot[0].invoke()).isEqualTo(LogMessages.CONNECT_AUTHENTICATED_SESSION)
        assertThat(logSlot[1].invoke()).isEqualTo(LogMessages.configureAuthenticatedSession(Request.token, false))
        assertThat(logSlot[2].invoke()).isEqualTo(LogMessages.STEP_UP_TO_AUTHENTICATED_SESSION)
    }

    @Test
    fun `when stepUpToAuthenticatedSession and currentState is NOT Configured`() {
        // currentState = Idle
        assertThat(subject.currentState).isIdle()
        assertFailsWith<IllegalStateException>("MessagingClient is not Configured or in ReadOnly state.") { subject.stepUpToAuthenticatedSession() }

        subject.connect()
        // currentState = Reconnecting
        every { mockReconnectionHandler.shouldReconnect } returns true
        val givenException = Exception(ErrorMessage.InternetConnectionIsOffline)
        slot.captured.onFailure(givenException, ErrorCode.WebsocketError)

        assertThat(subject.currentState).isReconnecting()
        assertFailsWith<IllegalStateException>("MessagingClient is not Configured or in ReadOnly state.") { subject.stepUpToAuthenticatedSession() }

        // currentState = Closed
        subject.disconnect()
        assertThat(subject.currentState).isClosed(1000, "The user has closed the connection.")
        assertFailsWith<IllegalStateException>("MessagingClient is not Configured or in ReadOnly state.") { subject.stepUpToAuthenticatedSession() }

        // currentState = Error
        every { mockPlatformSocket.sendMessage(Request.configureRequest()) } answers {
            slot.captured.onMessage(Response.sessionNotFound)
        }
        subject.connect()
        assertThat(subject.currentState).isError(ErrorCode.SessionNotFound, "session not found error message")
        assertFailsWith<IllegalStateException>("MessagingClient is not Configured or in ReadOnly state.") { subject.stepUpToAuthenticatedSession() }

        // currentState = ReadOnly
        every { mockPlatformSocket.sendMessage(Request.configureRequest()) } answers {
            slot.captured.onMessage(Response.configureSuccess(readOnly = true))
        }
        subject.connect()
        assertThat(subject.currentState).isReadOnly()
        assertFailsWith<IllegalStateException>("MessagingClient is not Configured or in ReadOnly state.") { subject.stepUpToAuthenticatedSession() }
    }
}
