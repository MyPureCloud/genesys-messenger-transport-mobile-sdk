package com.genesys.cloud.messenger.transport.core.messagingclient

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.genesys.cloud.messenger.transport.auth.NO_JWT
import com.genesys.cloud.messenger.transport.core.Empty
import com.genesys.cloud.messenger.transport.core.ErrorCode
import com.genesys.cloud.messenger.transport.core.ErrorMessage
import com.genesys.cloud.messenger.transport.core.MessagingClient
import com.genesys.cloud.messenger.transport.core.Result
import com.genesys.cloud.messenger.transport.core.events.Event
import com.genesys.cloud.messenger.transport.core.isConfigured
import com.genesys.cloud.messenger.transport.core.isError
import com.genesys.cloud.messenger.transport.util.Request
import com.genesys.cloud.messenger.transport.util.Response
import com.genesys.cloud.messenger.transport.util.fromConfiguredToError
import com.genesys.cloud.messenger.transport.util.fromConfiguredToReconnecting
import com.genesys.cloud.messenger.transport.util.fromConnectedToConfigured
import com.genesys.cloud.messenger.transport.util.fromConnectedToError
import com.genesys.cloud.messenger.transport.util.fromConnectingToConnected
import com.genesys.cloud.messenger.transport.util.fromIdleToConnecting
import com.genesys.cloud.messenger.transport.util.fromReconnectingToError
import com.genesys.cloud.messenger.transport.utility.AuthTest
import com.genesys.cloud.messenger.transport.utility.ErrorTest
import com.genesys.cloud.messenger.transport.utility.LogMessages
import io.mockk.every
import io.mockk.invoke
import io.mockk.verify
import io.mockk.verifySequence
import org.junit.Test
import kotlin.test.assertFailsWith

class MCAuthTests : BaseMessagingClientTest() {

    @Test
    fun `when authorize is called`() {
        subject.authorize(AuthTest.AuthCode, AuthTest.JwtAuthUrl, AuthTest.CodeVerifier)

        verify {
            mockAuthHandler.authorize(AuthTest.AuthCode, AuthTest.JwtAuthUrl, AuthTest.CodeVerifier)
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
            every { mockAuthHandler.jwt } returns AuthTest.JwtToken
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
        val givenResult = Result.Failure(ErrorCode.AuthFailed, ErrorTest.Message)
        every { mockAuthHandler.refreshToken(captureLambda()) } answers {
            lambda<(Result<Empty>) -> Unit>().invoke(givenResult)
        }
        every { mockAuthHandler.jwt } returns NO_JWT

        val expectedErrorState =
            MessagingClient.State.Error(ErrorCode.AuthFailed, ErrorTest.Message)

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
        assertFailsWith<IllegalStateException> { subject.logoutFromAuthenticatedSession() }
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
            mockAuthHandler.clear()
            mockEventHandler.onEvent(eq(expectedEvent))
            disconnectSequence()
        }
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
    fun `when connectAuthenticatedSession fails and WebSocket respond with WebsocketError and there are no reconnection attempts left`() {
        val expectedException = Exception(ErrorMessage.FailedToReconnect)
        val expectedErrorState =
            MessagingClient.State.Error(ErrorCode.WebsocketError, ErrorMessage.FailedToReconnect)

        subject.connectAuthenticatedSession()
        slot.captured.onFailure(expectedException, ErrorCode.WebsocketError)

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
        assertThat(logSlot[0].invoke()).isEqualTo(LogMessages.ConnectAuthenticated)
        assertThat(logSlot[1].invoke()).isEqualTo(LogMessages.ConfigureAuthenticatedSession)
        assertThat(logSlot[2].invoke()).isEqualTo(LogMessages.ClearConversationHistory)
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
        assertThat(logSlot[0].invoke()).isEqualTo(LogMessages.ConnectAuthenticated)
        assertThat(logSlot[1].invoke()).isEqualTo(LogMessages.ConfigureAuthenticatedSession)
        assertThat(logSlot[2].invoke()).isEqualTo(LogMessages.ClearConversationHistory)
        assertThat(logSlot[3].invoke()).isEqualTo(LogMessages.ConnectAuthenticated)
        assertThat(logSlot[4].invoke()).isEqualTo(LogMessages.ConfigureAuthenticatedSession)
    }
}
