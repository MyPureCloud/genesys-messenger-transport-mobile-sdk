package transport.core.messagingclient

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.genesys.cloud.messenger.transport.core.ErrorCode
import com.genesys.cloud.messenger.transport.core.ErrorMessage
import com.genesys.cloud.messenger.transport.core.MessagingClient
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
import com.genesys.cloud.messenger.transport.utility.TestValues
import io.mockk.every
import io.mockk.verify
import io.mockk.verifySequence
import org.junit.Test
import transport.util.Request
import transport.util.Response
import transport.util.fromConfiguredToReadOnly
import transport.util.fromConnectedToReadOnly
import transport.util.fromReadOnlyToError
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class MessagingClientConversationDisconnectTest : BaseMessagingClientTest() {

    @Test
    fun `when event Presence Disconnect received and there is no readOnly field in metadata`() {
        val expectedEvent = Event.ConversationDisconnect

        subject.connect()
        slot.captured.onMessage(Response.structuredMessageWithEvents(events = Response.StructuredEvent.presenceDisconnect))

        assertThat(subject.currentState).isConfigured(connected = true, newSession = true)
        verify { mockEventHandler.onEvent(eq(expectedEvent)) }
        verify(exactly = 0) {
            mockStateChangedListener.invoke(fromConfiguredToReadOnly())
            mockStateChangedListener.invoke(fromConnectedToReadOnly)
        }
    }

    @Test
    fun `when event Presence Disconnect received and readOnly field in metadata is true`() {
        val expectedEvent = Event.ConversationDisconnect

        subject.connect()
        slot.captured.onMessage(
            Response.structuredMessageWithEvents(
                events = Response.StructuredEvent.presenceDisconnect,
                metadata = mapOf("readOnly" to "true")
            )
        )

        assertTrue(subject.currentState is MessagingClient.State.ReadOnly)
        verifySequence {
            connectSequence()
            mockStateChangedListener.invoke(fromConfiguredToReadOnly())
            mockEventHandler.onEvent(eq(expectedEvent))
        }
    }

    @Test
    fun `when event Presence Disconnect received and readOnly field in metadata is false`() {
        val expectedEvent = Event.ConversationDisconnect

        subject.connect()
        slot.captured.onMessage(
            Response.structuredMessageWithEvents(
                events = Response.StructuredEvent.presenceDisconnect,
                metadata = mapOf("readOnly" to "false")
            )
        )

        verify { mockEventHandler.onEvent(eq(expectedEvent)) }
        verify(exactly = 0) { mockStateChangedListener.invoke(fromConfiguredToReadOnly()) }
    }

    @Test
    fun `when configured session is ReadOnly`() {
        every { mockPlatformSocket.sendMessage(Request.configureRequest()) } answers {
            slot.captured.onMessage(Response.configureSuccess(readOnly = true))
        }

        subject.connect()

        assertThat(subject.currentState).isReadOnly()
        verifySequence {
            connectToReadOnlySequence()
        }
    }

    @Test
    fun `when configured Session is ReadOnly and autostart enabled`() {
        every { mockPlatformSocket.sendMessage(Request.configureRequest()) } answers {
            slot.captured.onMessage(Response.configureSuccess(readOnly = true))
        }
        every { mockDeploymentConfig.get() } returns createDeploymentConfigForTesting(
            messenger = createMessengerVOForTesting(
                apps = Apps(
                    conversations = createConversationsVOForTesting(
                        autoStart = Conversations.AutoStart(enabled = true),
                    )
                )
            )
        )

        subject.connect()

        assertThat(subject.currentState).isReadOnly()
        verifySequence {
            connectToReadOnlySequence()
        }
        verify(exactly = 0) { mockPlatformSocket.sendMessage(Request.autostart()) }
    }

    @Test
    fun `when configured session is ReadOnly and send actions are performed`() {
        every { mockPlatformSocket.sendMessage(Request.configureRequest()) } answers {
            slot.captured.onMessage(Response.configureSuccess(readOnly = true))
        }

        subject.connect()

        assertThat(subject.currentState).isReadOnly()
        assertFailsWith<IllegalStateException> { subject.sendMessage("Hello!") }
        assertFailsWith<IllegalStateException> { subject.attach(ByteArray(0), "test") }
        assertFailsWith<IllegalStateException> { subject.sendHealthCheck() }
        assertFailsWith<IllegalStateException> { subject.detach("abc") }
        assertFailsWith<IllegalStateException> { subject.indicateTyping() }
    }

    @Test
    fun `when startNewChat from any non ReadOnly state`() {
        // currentState = Idle
        assertThat(subject.currentState).isIdle()
        assertFailsWith<IllegalStateException>("MessagingClient is not in ReadOnly state.") { subject.startNewChat() }

        // currentState = Configured
        subject.connect()

        assertThat(subject.currentState).isConfigured(connected = true, newSession = true)
        assertFailsWith<IllegalStateException>("MessagingClient is not in ReadOnly state.") { subject.startNewChat() }

        // currentState = Reconnecting
        every { mockReconnectionHandler.shouldReconnect } returns true
        val givenException = Exception(ErrorMessage.InternetConnectionIsOffline)
        slot.captured.onFailure(givenException, ErrorCode.WebsocketError)

        assertThat(subject.currentState).isReconnecting()
        assertFailsWith<IllegalStateException>("MessagingClient is not in ReadOnly state.") { subject.startNewChat() }

        // currentState = Error
        subject.connect()
        every { mockReconnectionHandler.shouldReconnect } returns false
        val givenException2 = Exception(ErrorMessage.FailedToReconnect)
        slot.captured.onFailure(givenException2, ErrorCode.WebsocketError)

        assertThat(subject.currentState).isError(
            ErrorCode.WebsocketError,
            ErrorMessage.FailedToReconnect
        )
        assertFailsWith<IllegalStateException>("MessagingClient is not in ReadOnly state.") { subject.startNewChat() }

        // currentState = Closed
        subject.connect()
        subject.disconnect()

        assertThat(subject.currentState).isClosed(1000, "The user has closed the connection.")
        assertFailsWith<IllegalStateException>("MessagingClient is not in ReadOnly state.") { subject.startNewChat() }
    }

    @Test
    fun `when startNewChat from ReadOnly state`() {
        every { mockPlatformSocket.sendMessage(Request.configureRequest()) } answers {
            slot.captured.onMessage(Response.configureSuccess(readOnly = true))
        }

        subject.connect()
        subject.startNewChat()

        assertThat(subject.currentState).isReadOnly()
        verifySequence {
            connectToReadOnlySequence()
            mockLogger.i(capture(logSlot))
            mockPlatformSocket.sendMessage(Request.closeAllConnections)
        }
        assertThat(logSlot[0].invoke()).isEqualTo(LogMessages.CONNECT)
        assertThat(logSlot[1].invoke()).isEqualTo(LogMessages.configureSession(Request.token, false))
        assertThat(logSlot[2].invoke()).isEqualTo(LogMessages.CLOSE_SESSION)
    }

    @Test
    fun `when NetworkRequestError during startNewChat`() {
        every { mockPlatformSocket.sendMessage(Request.configureRequest()) } answers {
            slot.captured.onMessage(Response.configureSuccess(readOnly = true))
        }
        val expectedErrorCode = ErrorCode.ClientResponseError(400)
        val expectedErrorMessage = "Request failed."
        val expectedErrorState = MessagingClient.State.Error(expectedErrorCode, expectedErrorMessage)
        every { mockPlatformSocket.sendMessage(Request.closeAllConnections) } answers {
            slot.captured.onMessage(Response.webSocketRequestFailed)
        }

        subject.connect()
        subject.startNewChat()

        assertThat(subject.currentState).isError(expectedErrorCode, expectedErrorMessage)
        verifySequence {
            connectToReadOnlySequence()
            mockLogger.i(capture(logSlot))
            mockPlatformSocket.sendMessage(Request.closeAllConnections)
            errorSequence(fromReadOnlyToError(errorState = expectedErrorState))
        }
    }

    @Test
    fun `when startNewChat and WebSocket receives a SessionResponse that has connected=false and readOnly=true`() {
        every { mockPlatformSocket.sendMessage(Request.configureRequest()) } answers {
            slot.captured.onMessage(Response.configureSuccess(readOnly = true))
        }
        every { mockPlatformSocket.sendMessage(Request.closeAllConnections) } answers {
            slot.captured.onMessage(Response.configureSuccess(connected = false, readOnly = true))
        }

        subject.connect()
        subject.startNewChat()

        assertThat(subject.currentState).isReadOnly()
        verifySequence {
            connectToReadOnlySequence()
            mockLogger.i(capture(logSlot))
            mockPlatformSocket.sendMessage(Request.closeAllConnections)
            mockAttachmentHandler.fileAttachmentProfile = any()
            mockReconnectionHandler.clear()
            mockJwtHandler.clear()
            mockCustomAttributesStore.maxCustomDataBytes = TestValues.MAX_CUSTOM_DATA_BYTES
            mockLogger.i(capture(logSlot))
            verifyCleanUp()
            mockLogger.i(capture(logSlot))
            mockPlatformSocket.sendMessage(Request.configureRequest(startNew = true))
        }
    }

    @Test
    fun `when startNewChat and WebSocket receives a SessionResponse that has connected=true and readOnly=true`() {
        every { mockPlatformSocket.sendMessage(Request.configureRequest()) } answers {
            slot.captured.onMessage(Response.configureSuccess(readOnly = true))
        }
        every { mockPlatformSocket.sendMessage(Request.closeAllConnections) } answers {
            slot.captured.onMessage(Response.configureSuccess(connected = true, readOnly = true))
        }

        subject.connect()
        subject.startNewChat()

        assertThat(subject.currentState).isReadOnly()
        verifySequence {
            connectToReadOnlySequence()
            mockLogger.i(capture(logSlot))
            mockPlatformSocket.sendMessage(Request.closeAllConnections)
            mockAttachmentHandler.fileAttachmentProfile = any()
            mockReconnectionHandler.clear()
            mockJwtHandler.clear()
            mockCustomAttributesStore.maxCustomDataBytes = TestValues.MAX_CUSTOM_DATA_BYTES
        }
        verify(exactly = 0) {
            mockMessageStore.invalidateConversationCache()
            mockAttachmentHandler.clearAll()
            mockPlatformSocket.sendMessage(Request.configureRequest(startNew = true))
        }
    }

    @Test
    fun `when WebSocket receives a SessionResponse that has connected=false and readOnly=true but startNewChat was not invoked`() {
        subject.connect()

        slot.captured.onMessage(Response.configureSuccess(connected = false, readOnly = true))

        assertThat(subject.currentState).isReadOnly()
        verifySequence {
            connectSequence()
            mockVault.wasAuthenticated = false
            mockAttachmentHandler.fileAttachmentProfile = any()
            mockReconnectionHandler.clear()
            mockJwtHandler.clear()
            mockCustomAttributesStore.maxCustomDataBytes = TestValues.MAX_CUSTOM_DATA_BYTES
            mockStateChangedListener(fromConfiguredToReadOnly())
        }
        verify(exactly = 0) {
            mockMessageStore.invalidateConversationCache()
            mockAttachmentHandler.clearAll()
            mockPlatformSocket.sendMessage(Request.configureRequest(startNew = true))
        }
    }
}
