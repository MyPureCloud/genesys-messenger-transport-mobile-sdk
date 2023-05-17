package com.genesys.cloud.messenger.transport.core

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.genesys.cloud.messenger.transport.auth.AuthHandler
import com.genesys.cloud.messenger.transport.core.MessagingClient.State
import com.genesys.cloud.messenger.transport.core.events.Event
import com.genesys.cloud.messenger.transport.core.events.EventHandler
import com.genesys.cloud.messenger.transport.core.events.HEALTH_CHECK_COOL_DOWN_MILLISECONDS
import com.genesys.cloud.messenger.transport.core.events.HealthCheckProvider
import com.genesys.cloud.messenger.transport.core.events.TYPING_INDICATOR_COOL_DOWN_MILLISECONDS
import com.genesys.cloud.messenger.transport.core.events.UserTypingProvider
import com.genesys.cloud.messenger.transport.network.PlatformSocket
import com.genesys.cloud.messenger.transport.network.PlatformSocketListener
import com.genesys.cloud.messenger.transport.network.ReconnectionHandlerImpl
import com.genesys.cloud.messenger.transport.network.TestWebMessagingApiResponses
import com.genesys.cloud.messenger.transport.network.WebMessagingApi
import com.genesys.cloud.messenger.transport.shyrka.receive.Apps
import com.genesys.cloud.messenger.transport.shyrka.receive.Conversations
import com.genesys.cloud.messenger.transport.shyrka.receive.DeploymentConfig
import com.genesys.cloud.messenger.transport.shyrka.receive.createConversationsVOForTesting
import com.genesys.cloud.messenger.transport.shyrka.receive.createDeploymentConfigForTesting
import com.genesys.cloud.messenger.transport.shyrka.receive.createMessengerVOForTesting
import com.genesys.cloud.messenger.transport.shyrka.send.Channel
import com.genesys.cloud.messenger.transport.shyrka.send.Channel.Metadata
import com.genesys.cloud.messenger.transport.shyrka.send.DeleteAttachmentRequest
import com.genesys.cloud.messenger.transport.shyrka.send.HealthCheckID
import com.genesys.cloud.messenger.transport.shyrka.send.OnAttachmentRequest
import com.genesys.cloud.messenger.transport.shyrka.send.OnMessageRequest
import com.genesys.cloud.messenger.transport.shyrka.send.TextMessage
import com.genesys.cloud.messenger.transport.util.DefaultTokenStore
import com.genesys.cloud.messenger.transport.util.Platform
import com.genesys.cloud.messenger.transport.util.Request
import com.genesys.cloud.messenger.transport.util.TestAuthCode
import com.genesys.cloud.messenger.transport.util.TestCodeVerifier
import com.genesys.cloud.messenger.transport.util.TestJwtAuthUrl
import com.genesys.cloud.messenger.transport.util.logs.Log
import io.mockk.Called
import io.mockk.MockKVerificationScope
import io.mockk.clearAllMocks
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.invoke
import io.mockk.mockk
import io.mockk.slot
import io.mockk.spyk
import io.mockk.verify
import io.mockk.verifySequence
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.reflect.KProperty0
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class MessagingClientImplTest {
    private val configuration = configuration()
    private val log: Log = Log(configuration.logging, "Log")
    private val slot = slot<PlatformSocketListener>()
    private val mockStateChangedListener: (StateChange) -> Unit = spyk()
    private val mockMessageStore: MessageStore = mockk(relaxed = true) {
        every { prepareMessage(any(), any()) } returns OnMessageRequest(
            token = Request.token,
            message = TextMessage("Hello world")
        )
    }
    private val mockAttachmentHandler: AttachmentHandler = mockk(relaxed = true) {
        every {
            prepare(
                any(),
                any(),
                any(),
                any()
            )
        } returns OnAttachmentRequest(
            token = Request.token,
            attachmentId = "88888888-8888-8888-8888-888888888888",
            fileName = "test_attachment.png",
            fileType = "image/png",
            errorsAsJson = true,
        )

        every { detach(any()) } returns DeleteAttachmentRequest(
            token = Request.token,
            attachmentId = "88888888-8888-8888-8888-888888888888"
        )
    }
    private val mockPlatformSocket: PlatformSocket = mockk {
        every { openSocket(capture(slot)) } answers {
            slot.captured.onOpen()
        }
        every { closeSocket(any(), any()) } answers {
            slot.captured.onClosed(1000, "The user has closed the connection.")
        }
        every { sendMessage(any()) } answers {
            slot.captured.onMessage("")
        }
        every { sendMessage(Request.configureRequest()) } answers {
            slot.captured.onMessage(Response.configureSuccess())
        }
        every { sendMessage(Request.configureAuthenticatedRequest()) } answers {
            slot.captured.onMessage(Response.configureSuccess())
        }
    }

    private val mockWebMessagingApi: WebMessagingApi = mockk {
        coEvery {
            getMessages(
                any(),
                any(),
                any()
            )
        } returns TestWebMessagingApiResponses.testMessageEntityList
    }

    private val mockReconnectionHandler: ReconnectionHandlerImpl = mockk(relaxed = true) {
        every { shouldReconnect } returns false
    }

    private val mockEventHandler: EventHandler = mockk(relaxed = true)
    private val mockTimestampFunction: () -> Long = spyk<() -> Long>().also {
        every { it.invoke() } answers { Platform().epochMillis() }
    }
    private val mockShowUserTypingIndicatorFunction: () -> Boolean = spyk<() -> Boolean>().also {
        every { it.invoke() } returns true
    }
    private val mockDeploymentConfig = mockk<KProperty0<DeploymentConfig?>> {
        every { get() } returns createDeploymentConfigForTesting()
    }
    private val userTypingProvider = UserTypingProvider(
        log = mockk(relaxed = true),
        showUserTypingEnabled = mockShowUserTypingIndicatorFunction,
        getCurrentTimestamp = mockTimestampFunction,
    )
    private val mockAuthHandler: AuthHandler = mockk(relaxed = true)
    private val mockTokenStore: DefaultTokenStore = mockk {
        every { fetch() } returns Request.token
        every { token } returns Request.token
    }

    private val subject = MessagingClientImpl(
        log = log,
        configuration = configuration,
        webSocket = mockPlatformSocket,
        api = mockWebMessagingApi,
        tokenStore = mockTokenStore,
        jwtHandler = mockk(),
        attachmentHandler = mockAttachmentHandler,
        messageStore = mockMessageStore,
        reconnectionHandler = mockReconnectionHandler,
        eventHandler = mockEventHandler,
        userTypingProvider = userTypingProvider,
        healthCheckProvider = HealthCheckProvider(mockk(relaxed = true), mockTimestampFunction),
        deploymentConfig = mockDeploymentConfig,
        authHandler = mockAuthHandler,
    ).also {
        it.stateChangedListener = mockStateChangedListener
    }

    @AfterTest
    fun after() = clearAllMocks()

    @Test
    fun whenConnect() {
        subject.connect()

        assertThat(subject.currentState).isConfigured(connected = true, newSession = true)
        verifySequence {
            connectSequence()
        }
    }

    @Test
    fun whenConnectAndThenDisconnect() {
        val expectedState = State.Closed(1000, "The user has closed the connection.")
        subject.connect()

        subject.disconnect()

        assertThat(subject.currentState).isClosed(expectedState.code, expectedState.reason)
        verifySequence {
            connectSequence()
            disconnectSequence(expectedState.code, expectedState.reason)
        }
    }

    @Test
    fun whenConnectAndThenSendMessage() {
        val expectedMessage =
            """{"token":"${Request.token}","message":{"text":"Hello world","type":"Text"},"action":"onMessage"}"""
        val expectedText = "Hello world"
        subject.connect()

        subject.sendMessage("Hello world")

        verifySequence {
            connectSequence()
            mockMessageStore.prepareMessage(expectedText)
            mockAttachmentHandler.onSending()
            mockPlatformSocket.sendMessage(expectedMessage)
        }
    }

    @Test
    fun whenSendHealthCheckWithToken() {
        val expectedMessage =
            """{"token":"${Request.token}","action":"echo","message":{"text":"ping","metadata":{"customMessageId":"$HealthCheckID"},"type":"Text"}}"""
        subject.connect()

        subject.sendHealthCheck()

        verifySequence {
            connectSequence()
            mockPlatformSocket.sendMessage(expectedMessage)
        }
    }

    @Test
    fun whenSendHealthCheckTwiceWithoutCoolDown() {
        val expectedMessage = Request.echoRequest

        subject.connect()

        subject.sendHealthCheck()
        subject.sendHealthCheck()

        verify(exactly = 1) { mockPlatformSocket.sendMessage(expectedMessage) }
    }

    @Test
    fun whenSendHealthCheckTwiceWithCoolDown() {
        val healthCheckCoolDownInMilliseconds = HEALTH_CHECK_COOL_DOWN_MILLISECONDS + 250
        val expectedMessage = Request.echoRequest

        subject.connect()

        subject.sendHealthCheck()
        // Fast forward epochMillis by healthCheckCoolDownInMilliseconds.
        every { mockTimestampFunction.invoke() } answers { Platform().epochMillis() + healthCheckCoolDownInMilliseconds }
        subject.sendHealthCheck()

        verify(exactly = 2) { mockPlatformSocket.sendMessage(expectedMessage) }
    }

    @Test
    fun whenConnectSendHealthCheckReconnectAndSendHealthCheckAgainWithoutDelay() {
        val expectedMessage = Request.echoRequest

        subject.connect()
        subject.sendHealthCheck()
        subject.disconnect()
        subject.connect()
        subject.sendHealthCheck()

        verifySequence {
            connectSequence()
            mockPlatformSocket.sendMessage(expectedMessage)
            disconnectSequence()
            mockStateChangedListener(fromClosedToConnecting)
            mockPlatformSocket.openSocket(any())
            mockStateChangedListener(fromConnectingToConnected)
            mockPlatformSocket.sendMessage(Request.configureRequest())
            mockReconnectionHandler.clear()
            mockStateChangedListener(fromConnectedToConfigured)
            mockPlatformSocket.sendMessage(expectedMessage)
        }
    }

    @Test
    fun whenAttach() {
        val expectedAttachmentId = "88888888-8888-8888-8888-888888888888"
        val expectedMessage =
            """{"token":"${Request.token}","attachmentId":"88888888-8888-8888-8888-888888888888","fileName":"test_attachment.png","fileType":"image/png","errorsAsJson":true,"action":"onAttachment"}"""
        subject.connect()

        val result = subject.attach(ByteArray(1), "test.png")

        assertEquals(expectedAttachmentId, result)
        verifySequence {
            connectSequence()
            mockAttachmentHandler.prepare(any(), any(), any())
            mockPlatformSocket.sendMessage(expectedMessage)
        }
    }

    @Test
    fun whenDetach() {
        val expectedAttachmentId = "88888888-8888-8888-8888-888888888888"
        val expectedMessage =
            """{"token":"${Request.token}","attachmentId":"88888888-8888-8888-8888-888888888888","action":"deleteAttachment"}"""
        val attachmentIdSlot = slot<String>()
        subject.connect()

        subject.detach("88888888-8888-8888-8888-888888888888")

        verify {
            mockAttachmentHandler.detach(capture(attachmentIdSlot))
            mockPlatformSocket.sendMessage(expectedMessage)
        }
        assertThat(attachmentIdSlot.captured).isEqualTo(expectedAttachmentId)
    }

    @Test
    fun whenDetachNonExistingAttachmentId() {
        subject.connect()
        clearMocks(mockPlatformSocket)
        every { mockAttachmentHandler.detach(any()) } returns null

        subject.detach("88888888-8888-8888-8888-888888888888")

        verify {
            mockAttachmentHandler.detach("88888888-8888-8888-8888-888888888888")
            mockPlatformSocket wasNot Called
        }
    }

    @Test
    fun whenNotConnectedAndSendHealthCheckVerifyIllegalStateExceptionThrown() {
        assertFailsWith<IllegalStateException> {
            subject.sendHealthCheck()
        }
    }

    @Test
    fun whenNotConnectedAndDisconnectVerifyIllegalStateExceptionThrown() {
        assertFailsWith<IllegalStateException> {
            subject.disconnect()
        }
    }

    @Test
    fun whenSendMessageWithoutConnection() {
        assertFailsWith<IllegalStateException> {
            subject.sendMessage("foo")
        }
    }

    @Test
    fun whenAttachWithoutConnection() {
        assertFailsWith<IllegalStateException> {
            subject.attach(ByteArray(1), "file.png")
        }
    }

    @Test
    fun whenDetachAttachmentWithoutConnection() {
        assertFailsWith<IllegalStateException> {
            subject.detach("attachmentId")
        }
    }

    @Test
    fun whenConnectFailsAndSocketListenerRespondWithWebsocketErrorAndThereAreNoReconnectionAttemptsLeft() {
        val expectedException = Exception(ErrorMessage.FailedToReconnect)
        val expectedErrorState =
            State.Error(ErrorCode.WebsocketError, ErrorMessage.FailedToReconnect)

        subject.connect()

        slot.captured.onFailure(expectedException, ErrorCode.WebsocketError)

        assertThat(subject.currentState).isError(
            expectedErrorState.code,
            expectedErrorState.message
        )
        verifySequence {
            connectSequence()
            mockMessageStore.invalidateConversationCache()
            mockReconnectionHandler.shouldReconnect
            errorSequence(fromConfiguredToError(expectedErrorState))
        }
    }

    @Test
    fun whenConnectFailsWithWebsocketAccessDenied() {
        val message = CorrectiveAction.Forbidden.message
        val expectedErrorState = State.Error(ErrorCode.WebsocketAccessDenied, message)
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
            mockStateChangedListener(
                StateChange(
                    oldState = State.Connecting,
                    newState = expectedErrorState
                )
            )
            mockAttachmentHandler.clearAll()
            mockReconnectionHandler.clear()
        }
    }

    @Test
    fun whenConfigureFailsBecauseSocketListenerRespondWithNetworkDisabledError() {
        val givenException = Exception(ErrorMessage.InternetConnectionIsOffline)
        val expectedErrorState =
            State.Error(ErrorCode.NetworkDisabled, ErrorMessage.InternetConnectionIsOffline)
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
    fun whenSocketListenerInvokeOnMessageWithSessionExpiredStringMessage() {
        val expectedErrorState =
            State.Error(ErrorCode.SessionHasExpired, "session expired error message")
        subject.connect()

        slot.captured.onMessage(Response.sessionExpired)

        verifySequence {
            connectSequence()
            errorSequence(fromConfiguredToError(expectedErrorState))
        }
    }

    @Test
    fun whenSocketListenerInvokeOnMessageWithClientResponseErrorWhileReconnecting() {
        val expectedErrorCode = ErrorCode.ClientResponseError(400)
        val expectedErrorMessage = "Request failed."
        val expectedErrorState = State.Error(expectedErrorCode, expectedErrorMessage)
        every { mockPlatformSocket.sendMessage(Request.configureRequest()) } answers {
            if (subject.currentState == State.Reconnecting) {
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
            mockReconnectionHandler.shouldReconnect
            mockStateChangedListener(fromConfiguredToReconnecting())
            mockReconnectionHandler.reconnect(any())
            mockPlatformSocket.openSocket(any())
            mockPlatformSocket.sendMessage(eq(Request.configureRequest()))
            errorSequence(fromReconnectingToError(expectedErrorState))
        }
    }

    @Test
    fun whenSocketListenerInvokeOnMessageWithSessionNotFoundStringMessage() {
        val expectedErrorState =
            State.Error(ErrorCode.SessionNotFound, "session not found error message")
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
    fun whenSocketListenerInvokeOnMessageWithMessageTooLongStringMessage() {
        subject.connect()

        slot.captured.onMessage(Response.messageTooLong)

        verifySequence {
            connectSequence()
            mockMessageStore.onMessageError(ErrorCode.MessageTooLong, "message too long")
        }
    }

    @Test
    fun whenSocketListenerInvokeTooManyRequestsErrorMessage() {
        subject.connect()

        slot.captured.onMessage(Response.tooManyRequests)

        verifySequence {
            connectSequence()
            mockMessageStore.onMessageError(
                ErrorCode.RequestRateTooHigh,
                "Message rate too high for this session. Retry after 3 seconds."
            )
            mockAttachmentHandler.onMessageError(
                ErrorCode.RequestRateTooHigh,
                "Message rate too high for this session. Retry after 3 seconds."
            )
        }
    }

    @Test
    fun whenSocketListenerInvokeOnMessageWitSessionExpiredEvent() {
        val expectedErrorState = State.Error(ErrorCode.SessionHasExpired, null)

        subject.connect()

        slot.captured.onMessage(Response.sessionExpiredEvent)

        verifySequence {
            connectSequence()
            errorSequence(fromConfiguredToError(expectedErrorState))
        }
    }

    @Test
    fun whenMessageListenerSet() {
        val givenMessageListener: (MessageEvent) -> Unit = {}

        subject.messageListener = givenMessageListener

        verify {
            mockMessageStore.messageListener = givenMessageListener
        }
    }

    @Test
    fun whenSocketListenerInvokeOnMessageWithStructuredMessage() {
        val expectedAttachment = Attachment(
            "attachment_id",
            "image.png",
            Attachment.State.Sent("https://downloadurl.com")
        )
        val expectedMessage = Message(
            "msg_id",
            Message.Direction.Outbound,
            Message.State.Sent,
            "Text",
            "Hi",
            null,
            mapOf("attachment_id" to expectedAttachment)
        )

        subject.connect()

        slot.captured.onMessage(Response.onMessageWithAttachment)

        verifySequence {
            connectSequence()
            mockMessageStore.update(expectedMessage)
            mockAttachmentHandler.onSent(mapOf("attachment_id" to expectedAttachment))
        }
    }

    @Test
    fun whenSocketListenerInvokeOnMessageWithAttachmentDeletedResponse() {
        val expectedAttachmentId = "attachment_id"

        subject.connect()

        slot.captured.onMessage(Response.attachmentDeleted)

        verifySequence {
            connectSequence()
            mockAttachmentHandler.onDetached(expectedAttachmentId)
        }
    }

    @Test
    fun whenInvalidateConversationCache() {
        subject.invalidateConversationCache()

        verify {
            mockMessageStore.invalidateConversationCache()
        }
    }

    @Test
    fun whenFetchNextPageButClientIsNotConfigured() {
        assertFailsWith<IllegalStateException> { runBlocking { subject.fetchNextPage() } }
    }

    @Test
    fun whenFetchNextPageButAllHistoryWasAlreadyFetched() {
        every { mockMessageStore.startOfConversation } returns true
        every { mockMessageStore.getConversation() } returns List(DEFAULT_PAGE_SIZE) { Message() }
        subject.connect()

        runBlocking { subject.fetchNextPage() }

        verify {
            mockMessageStore.updateMessageHistory(emptyList(), DEFAULT_PAGE_SIZE)
        }
    }

    @Test
    fun whenSendMessageWithCustomAttributes() {
        val expectedMessage =
            """{"token":"${Request.token}","message":{"text":"Hello world","channel":{"metadata":{"customAttributes":{"A":"B"}}},"type":"Text"},"action":"onMessage"}"""
        val expectedText = "Hello world"
        val expectedCustomAttributes = mapOf("A" to "B")
        every { mockMessageStore.prepareMessage(any(), any()) } returns OnMessageRequest(
            token = Request.token,
            message = TextMessage(
                text = "Hello world",
                channel = Channel(Metadata(expectedCustomAttributes)),
            ),
        )
        subject.connect()

        subject.sendMessage(text = "Hello world", customAttributes = mapOf("A" to "B"))

        verifySequence {
            connectSequence()
            mockMessageStore.prepareMessage(expectedText, expectedCustomAttributes)
            mockAttachmentHandler.onSending()
            mockPlatformSocket.sendMessage(expectedMessage)
        }
    }

    @Test
    fun whenMessageWithCustomAttributesIsTooLarge() {
        val expectedErrorCode = ErrorCode.CustomAttributeSizeTooLarge
        val expectedErrorMessage = "Custom Attributes in channel metadata is larger than 2048 bytes"
        subject.connect()

        slot.captured.onMessage(Response.customAttributeSizeTooLarge)

        verifySequence {
            connectSequence()
            mockMessageStore.onMessageError(expectedErrorCode, expectedErrorMessage)
        }
    }

    @Test
    fun whenConnectHasClientResponseError() {
        val expectedErrorCode = ErrorCode.ClientResponseError(400)
        val expectedErrorMessage = "Request failed."
        val expectedErrorState = State.Error(expectedErrorCode, expectedErrorMessage)
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
    fun whenStructuredMessageWithMultipleEventsReceived() {
        val firstExpectedEvent = Event.AgentTyping(1000)
        val secondsExpectedEvent = Event.AgentTyping(5000)

        subject.connect()

        slot.captured.onMessage(Response.structuredMessageWithEvents())

        verifySequence {
            mockEventHandler.onEvent(eq(firstExpectedEvent))
            mockEventHandler.onEvent(eq(secondsExpectedEvent))
        }
        verify(exactly = 0) { mockMessageStore.update(any()) }
        verify(exactly = 0) { mockAttachmentHandler.onSent(any()) }
    }

    @Test
    fun whenStructuredMessageWithUnknownEventTypeReceived() {
        val givenUnknownEvent = """{"eventType": "Fake","bloop": {"bip": "bop"}}"""
        subject.connect()

        slot.captured.onMessage(Response.structuredMessageWithEvents(givenUnknownEvent))

        verify { mockEventHandler.onEvent(null) }
        verify(exactly = 0) { mockMessageStore.update(any()) }
        verify(exactly = 0) { mockAttachmentHandler.onSent(any()) }
    }

    @Test
    fun whenStructuredMessageWithInboundEventReceived() {
        subject.connect()

        slot.captured.onMessage(Response.structuredMessageWithEvents(direction = Message.Direction.Inbound))

        verify(exactly = 0) { mockEventHandler.onEvent(any()) }
        verify(exactly = 0) { mockMessageStore.update(any()) }
        verify(exactly = 0) { mockAttachmentHandler.onSent(any()) }
    }

    @Test
    fun whenIndicateTypingAndShowUserTypingIsEnabled() {
        val expectedMessage = Request.userTypingRequest
        subject.connect()

        subject.indicateTyping()

        verifySequence {
            connectSequence()
            mockPlatformSocket.sendMessage(expectedMessage)
        }
    }

    @Test
    fun whenIndicateTypingAndShowUserTypingIsDisabled() {
        every { mockShowUserTypingIndicatorFunction.invoke() } returns false
        val expectedMessage = Request.userTypingRequest
        subject.connect()

        subject.indicateTyping()

        verify(exactly = 0) {
            mockPlatformSocket.sendMessage(expectedMessage)
        }
        assertNull(userTypingProvider.encodeRequest(token = Request.token))
    }

    @Test
    fun whenNotConnectedAndIndicateTypingInvoked() {
        assertFailsWith<IllegalStateException> {
            subject.indicateTyping()
        }
    }

    @Test
    fun whenIndicateTypingTwiceWithoutCoolDown() {
        val expectedMessage = Request.userTypingRequest
        subject.connect()

        subject.indicateTyping()
        subject.indicateTyping()

        verify(exactly = 1) { mockPlatformSocket.sendMessage(expectedMessage) }
    }

    @Test
    fun whenIndicateTypingTwiceWithCoolDown() {
        val typingIndicatorCoolDownInMilliseconds = TYPING_INDICATOR_COOL_DOWN_MILLISECONDS + 250
        val expectedMessage = Request.userTypingRequest

        subject.connect()

        subject.indicateTyping()
        // Fast forward epochMillis by typingIndicatorCoolDownInMilliseconds.
        every { mockTimestampFunction.invoke() } answers { Platform().epochMillis() + typingIndicatorCoolDownInMilliseconds }
        subject.indicateTyping()

        verify(exactly = 2) { mockPlatformSocket.sendMessage(expectedMessage) }
    }

    @Test
    fun whenIndicateTypingTwiceWithoutCoolDownButAfterMessageWasSent() {
        val expectedMessage = Request.userTypingRequest
        subject.connect()

        subject.indicateTyping()
        slot.captured.onMessage(Response.onMessage)
        subject.indicateTyping()

        verify(exactly = 2) { mockPlatformSocket.sendMessage(expectedMessage) }
    }

    @Test
    fun whenWebSocketRespondWithUnstructuredTypingIndicatorForbiddenError() {
        val expectedErrorCode = ErrorCode.ClientResponseError(403)
        val expectedEvent = Event.Error(
            errorCode = expectedErrorCode,
            message = "Turn on the Feature Toggle or fix the configuration.",
            correctiveAction = expectedErrorCode.toCorrectiveAction(),
        )
        subject.connect()

        slot.captured.onMessage(Response.typingIndicatorForbidden)

        verify { mockEventHandler.onEvent(expectedEvent) }
    }

    @Test
    fun whenNewSessionAndAutostartEnabled() {
        every { mockDeploymentConfig.get() } returns createDeploymentConfigForTesting(
            messenger = createMessengerVOForTesting(
                apps = Apps(
                    conversations = createConversationsVOForTesting(
                        autoStart = Conversations.AutoStart(enabled = true)
                    )
                )
            )
        )

        subject.connect()

        verifySequence {
            connectSequence()
            mockPlatformSocket.sendMessage(Request.autostart)
        }
    }

    @Test
    fun whenOldSessionAndAutostartEnabled() {
        every { mockPlatformSocket.sendMessage(Request.configureRequest()) } answers {
            slot.captured.onMessage(Response.configureSuccessWithNewSessionFalse)
        }
        every { mockDeploymentConfig.get() } returns createDeploymentConfigForTesting(
            messenger = createMessengerVOForTesting(
                apps = Apps(
                    conversations = createConversationsVOForTesting(
                        autoStart = Conversations.AutoStart(enabled = true)
                    )
                )
            )
        )

        subject.connect()

        verify(exactly = 0) { mockPlatformSocket.sendMessage(Request.autostart) }
    }

    @Test
    fun whenOldSessionAndAutostartDisabled() {
        every { mockPlatformSocket.sendMessage(Request.configureRequest()) } answers {
            slot.captured.onMessage(Response.configureSuccessWithNewSessionFalse)
        }

        subject.connect()

        verify(exactly = 0) { mockPlatformSocket.sendMessage(Request.autostart) }
    }

    @Test
    fun whenNewSessionAndAutostartDisabled() {
        subject.connect()

        verify(exactly = 0) { mockPlatformSocket.sendMessage(Request.autostart) }
    }

    @Test
    fun whenNewSessionAndDeploymentConfigNotSet() {
        every { mockDeploymentConfig.get() } returns null

        subject.connect()

        verify(exactly = 0) { mockPlatformSocket.sendMessage(Request.autostart) }
    }

    @Test
    fun whenEventPresenceJoinReceived() {
        val givenPresenceJoinEvent = """{"eventType":"Presence","presence":{"type":"Join"}}"""
        val expectedEvent = Event.ConversationAutostart

        subject.connect()
        slot.captured.onMessage(Response.structuredMessageWithEvents(events = givenPresenceJoinEvent))

        verify {
            mockEventHandler.onEvent(eq(expectedEvent))
        }
    }

    @Test
    fun whenEventConnectionClosedReceived() {
        val expectedEvent = Event.ConnectionClosed

        subject.connect()
        slot.captured.onMessage(Response.connectionClosedEvent)

        verifySequence {
            connectSequence()
            disconnectSequence()
            mockEventHandler.onEvent(eq(expectedEvent))
        }
    }

    @Test
    fun whenDisconnectEventReceivedAndThereAreNoReadOnlyInMetadataAndConversationDisconnectInDeploymentConfigIsSetToSendAndEnabled() {
        every { mockDeploymentConfig.get() } returns createDeploymentConfigForTesting(
            messenger = createMessengerVOForTesting(
                apps = Apps(
                    conversations = createConversationsVOForTesting(
                        conversationDisconnect = Conversations.ConversationDisconnect(
                            enabled = true,
                            type = Conversations.ConversationDisconnect.Type.Send
                        ),
                    )
                )
            )
        )
        val givenPresenceDisconnectEvent =
            """{"eventType":"Presence","presence":{"type":"Disconnect"}}"""
        val expectedEvent = Event.ConversationDisconnect

        subject.connect()
        slot.captured.onMessage(Response.structuredMessageWithEvents(events = givenPresenceDisconnectEvent))

        assertThat(subject.currentState).isConfigured(connected = true, newSession = true)
        verify { mockEventHandler.onEvent(eq(expectedEvent)) }
        verify(exactly = 0) {
            mockStateChangedListener.invoke(fromConfiguredToReadOnly())
            mockStateChangedListener.invoke(fromConnectedToReadOnly)
        }
    }

    @Test
    fun whenDisconnectEventReceivedAndThereAreNoReadOnlyInMetadataAndConversationDisconnectInDeploymentConfigIsSetToSendAndDisabled() {
        val givenPresenceDisconnectEvent =
            """{"eventType":"Presence","presence":{"type":"Disconnect"}}"""
        val expectedEvent = Event.ConversationDisconnect

        subject.connect()
        slot.captured.onMessage(Response.structuredMessageWithEvents(events = givenPresenceDisconnectEvent))

        verify(exactly = 0) {
            mockEventHandler.onEvent(eq(expectedEvent))
            mockStateChangedListener.invoke(fromConfiguredToReadOnly())
            mockStateChangedListener.invoke(fromConnectedToReadOnly)
        }
    }

    @Test
    fun whenDisconnectEventReceivedAndThereAreNoReadOnlyInMetadataAndConversationDisconnectInDeploymentConfigIsSetToReadOnlyAndEnabled() {
        every { mockDeploymentConfig.get() } returns createDeploymentConfigForTesting(
            messenger = createMessengerVOForTesting(
                apps = Apps(
                    conversations = createConversationsVOForTesting(
                        conversationDisconnect = Conversations.ConversationDisconnect(
                            enabled = true,
                            type = Conversations.ConversationDisconnect.Type.ReadOnly
                        ),
                    )
                )
            )
        )

        val givenPresenceDisconnectEvent =
            """{"eventType":"Presence","presence":{"type":"Disconnect"}}"""
        val expectedEvent = Event.ConversationDisconnect

        subject.connect()
        slot.captured.onMessage(Response.structuredMessageWithEvents(events = givenPresenceDisconnectEvent))

        verify { mockEventHandler.onEvent(eq(expectedEvent)) }
        verify { mockStateChangedListener.invoke(fromConfiguredToReadOnly()) }
    }

    @Test
    fun whenDisconnectEventReceivedAndThereAreNoReadOnlyInMetadataAndConversationDisconnectInDeploymentConfigIsSetToReadOnlyAndDisabled() {
        every { mockDeploymentConfig.get() } returns createDeploymentConfigForTesting(
            messenger = createMessengerVOForTesting(
                apps = Apps(
                    conversations = createConversationsVOForTesting(
                        conversationDisconnect = Conversations.ConversationDisconnect(
                            enabled = false,
                            type = Conversations.ConversationDisconnect.Type.ReadOnly
                        ),
                    )
                )
            )
        )

        val givenPresenceDisconnectEvent =
            """{"eventType":"Presence","presence":{"type":"Disconnect"}}"""
        val expectedEvent = Event.ConversationDisconnect

        subject.connect()
        slot.captured.onMessage(Response.structuredMessageWithEvents(events = givenPresenceDisconnectEvent))

        verify(exactly = 0) {
            mockEventHandler.onEvent(eq(expectedEvent))
            mockStateChangedListener.invoke(fromConfiguredToReadOnly())
            mockStateChangedListener.invoke(fromConnectedToReadOnly)
        }
    }

    @Test
    fun whenDisconnectEventReceivedAndReadOnlyFieldInMetadataIsTrue() {
        every { mockDeploymentConfig.get() } returns createDeploymentConfigForTesting(
            messenger = createMessengerVOForTesting(
                apps = Apps(
                    conversations = createConversationsVOForTesting(
                        conversationDisconnect = Conversations.ConversationDisconnect(
                            enabled = true,
                            type = Conversations.ConversationDisconnect.Type.ReadOnly
                        ),
                    )
                )
            )
        )

        val givenPresenceDisconnectEvent =
            """{"eventType":"Presence","presence":{"type":"Disconnect"}}"""
        val expectedEvent = Event.ConversationDisconnect

        subject.connect()
        slot.captured.onMessage(
            Response.structuredMessageWithEvents(
                events = givenPresenceDisconnectEvent,
                metadata = mapOf("readOnly" to "true")
            )
        )

        verify { mockEventHandler.onEvent(eq(expectedEvent)) }
        verify { mockStateChangedListener.invoke(fromConfiguredToReadOnly()) }
    }

    @Test
    fun whenDisconnectEventReceivedAndReadOnlyFieldInMetadataIsFalse() {
        every { mockDeploymentConfig.get() } returns createDeploymentConfigForTesting(
            messenger = createMessengerVOForTesting(
                apps = Apps(
                    conversations = createConversationsVOForTesting(
                        conversationDisconnect = Conversations.ConversationDisconnect(
                            enabled = true,
                            type = Conversations.ConversationDisconnect.Type.ReadOnly
                        ),
                    )
                )
            )
        )

        val givenPresenceDisconnectEvent =
            """{"eventType":"Presence","presence":{"type":"Disconnect"}}"""
        val expectedEvent = Event.ConversationDisconnect

        subject.connect()
        slot.captured.onMessage(
            Response.structuredMessageWithEvents(
                events = givenPresenceDisconnectEvent,
                metadata = mapOf("readOnly" to "false")
            )
        )

        verify { mockEventHandler.onEvent(eq(expectedEvent)) }
        verify(exactly = 0) { mockStateChangedListener.invoke(fromConfiguredToReadOnly()) }
    }

    @Test
    fun whenConfigureSessionInReadOnly() {
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
    fun whenConfigureSessionInReadOnlyAndAutostartEnabled() {
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
        verify(exactly = 0) { mockPlatformSocket.sendMessage(Request.autostart) }
    }

    @Test
    fun whenSessionInReadOnlyAndSendActionsArePerformed() {
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
    fun whenStartNewChatFromAnyNonReadOnlyState() {
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
    fun whenStartNewChatFromReadOnlyState() {
        every { mockPlatformSocket.sendMessage(Request.configureRequest()) } answers {
            slot.captured.onMessage(Response.configureSuccess(readOnly = true))
        }

        subject.connect()
        subject.startNewChat()

        assertThat(subject.currentState).isReadOnly()
        verifySequence {
            connectToReadOnlySequence()
            mockPlatformSocket.sendMessage(Request.closeAllConnections)
        }
    }

    @Test
    fun whenNetworkRequestErrorDuringStartNewChat() {
        every { mockPlatformSocket.sendMessage(Request.configureRequest()) } answers {
            slot.captured.onMessage(Response.configureSuccess(readOnly = true))
        }
        val expectedErrorCode = ErrorCode.ClientResponseError(400)
        val expectedErrorMessage = "Request failed."
        val expectedErrorState = State.Error(expectedErrorCode, expectedErrorMessage)
        every { mockPlatformSocket.sendMessage(Request.closeAllConnections) } answers {
            slot.captured.onMessage(Response.webSocketRequestFailed)
        }

        subject.connect()
        subject.startNewChat()

        assertThat(subject.currentState).isError(expectedErrorCode, expectedErrorMessage)
        verifySequence {
            connectToReadOnlySequence()
            mockPlatformSocket.sendMessage(Request.closeAllConnections)
            mockStateChangedListener(fromReadOnlyToError(errorState = expectedErrorState))
            mockReconnectionHandler.clear()
        }
    }

    @Test
    fun whenStartNewChatAndWebSocketReceivesASessionResponseThatNotConnectedAndReadOnly() {
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
            mockPlatformSocket.sendMessage(Request.closeAllConnections)
            mockReconnectionHandler.clear()
            verifyCleanUp()
            mockPlatformSocket.sendMessage(Request.configureRequest(startNew = true))
        }
    }

    @Test
    fun whenStartNewChatAndWebSocketReceivesASessionResponseThatConnectedAndReadOnly() {
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
            mockPlatformSocket.sendMessage(Request.closeAllConnections)
            mockReconnectionHandler.clear()
        }
        verify(exactly = 0) {
            mockMessageStore.invalidateConversationCache()
            mockAttachmentHandler.clearAll()
            mockPlatformSocket.sendMessage(Request.configureRequest(startNew = true))
        }
    }

    @Test
    fun whenWebSocketReceivesASessionResponseThatNotConnectedAndReadOnlyButStartNewChatWasNotInvoked() {
        subject.connect()

        slot.captured.onMessage(Response.configureSuccess(connected = false, readOnly = true))

        assertThat(subject.currentState).isReadOnly()
        verifySequence {
            connectSequence()
            mockReconnectionHandler.clear()
            mockStateChangedListener(fromConfiguredToReadOnly())
        }
        verify(exactly = 0) {
            mockMessageStore.invalidateConversationCache()
            mockAttachmentHandler.clearAll()
            mockPlatformSocket.sendMessage(Request.configureRequest(startNew = true))
        }
    }

    @Test
    fun whenAuthenticateIsCalled() {
        subject.authenticate(TestAuthCode, TestJwtAuthUrl, TestCodeVerifier)

        verify {
            mockAuthHandler.authenticate(TestAuthCode, TestJwtAuthUrl, TestCodeVerifier)
        }
    }

    @Test
    fun whenEventLogoutReceived() {
        val expectedEvent = Event.Logout

        subject.connect()
        slot.captured.onMessage(Response.logoutEvent)

        verifySequence {
            connectSequence()
            mockEventHandler.onEvent(eq(expectedEvent))
        }
    }

    private fun configuration(): Configuration = Configuration(
        deploymentId = "deploymentId",
        domain = "inindca.com",
    )

    private fun MockKVerificationScope.connectSequence(shouldConfigureAuth: Boolean = false) {
        val configureRequest =
            if (shouldConfigureAuth) Request.configureAuthenticatedRequest() else Request.configureRequest()
        mockStateChangedListener(fromIdleToConnecting)
        mockPlatformSocket.openSocket(any())
        mockStateChangedListener(fromConnectingToConnected)
        mockPlatformSocket.sendMessage(configureRequest)
        mockReconnectionHandler.clear()
        mockStateChangedListener(fromConnectedToConfigured)
    }

    private fun MockKVerificationScope.connectToReadOnlySequence() {
        mockStateChangedListener(fromIdleToConnecting)
        mockPlatformSocket.openSocket(any())
        mockStateChangedListener(fromConnectingToConnected)
        mockPlatformSocket.sendMessage(Request.configureRequest())
        mockReconnectionHandler.clear()
        mockStateChangedListener(fromConnectedToReadOnly)
    }

    private fun disconnectSequence(
        expectedCloseCode: Int = 1000,
        expectedCloseReason: String = "The user has closed the connection.",
    ) {
        val fromConfiguredToClosing = StateChange(
            oldState = State.Configured(connected = true, newSession = true),
            newState = State.Closing(expectedCloseCode, expectedCloseReason)
        )
        val fromClosingToClosed = StateChange(
            oldState = State.Closing(expectedCloseCode, expectedCloseReason),
            newState = State.Closed(expectedCloseCode, expectedCloseReason)
        )
        mockReconnectionHandler.clear()
        this.mockStateChangedListener(fromConfiguredToClosing)
        mockPlatformSocket.closeSocket(expectedCloseCode, expectedCloseReason)
        this.mockStateChangedListener(fromClosingToClosed)
        verifyCleanUp()
    }

    private fun MockKVerificationScope.connectWithFailedConfigureSequence(shouldConfigureAuth: Boolean = false) {
        val configureRequest =
            if (shouldConfigureAuth) Request.configureAuthenticatedRequest() else Request.configureRequest()
        mockStateChangedListener(fromIdleToConnecting)
        mockPlatformSocket.openSocket(any())
        mockStateChangedListener(fromConnectingToConnected)
        mockPlatformSocket.sendMessage(configureRequest)
    }

    private fun errorSequence(stateChange: StateChange) {
        this.mockStateChangedListener(stateChange)
        mockAttachmentHandler.clearAll()
        mockReconnectionHandler.clear()
    }

    private val fromIdleToConnecting =
        StateChange(oldState = State.Idle, newState = State.Connecting)

    private val fromClosedToConnecting =
        StateChange(
            oldState = State.Closed(1000, "The user has closed the connection."),
            newState = State.Connecting
        )

    private val fromConnectingToConnected =
        StateChange(oldState = State.Connecting, newState = State.Connected)

    private val fromConnectedToConfigured =
        StateChange(
            oldState = State.Connected,
            newState = State.Configured(connected = true, newSession = true)
        )

    private fun fromConnectedToError(errorState: State) =
        StateChange(oldState = State.Connected, newState = errorState)

    private fun fromConfiguredToError(errorState: State) =
        StateChange(
            oldState = State.Configured(connected = true, newSession = true),
            newState = errorState,
        )

    private fun fromErrorToConnecting(errorState: State) =
        StateChange(oldState = errorState, newState = State.Connecting)

    private fun fromReadOnlyToError(errorState: State) =
        StateChange(
            oldState = State.ReadOnly,
            newState = errorState,
        )

    private fun fromConfiguredToReadOnly() =
        StateChange(
            oldState = State.Configured(connected = true, newSession = true),
            newState = State.ReadOnly,
        )

    private val fromConnectedToReadOnly =
        StateChange(
            oldState = State.Connected,
            newState = State.ReadOnly,
        )

    private fun fromReconnectingToError(errorState: State) =
        StateChange(oldState = State.Reconnecting, newState = errorState)

    private fun fromConfiguredToReconnecting(): StateChange =
        StateChange(
            oldState = State.Configured(connected = true, newSession = true),
            newState = State.Reconnecting
        )

    private fun verifyCleanUp() {
        mockMessageStore.invalidateConversationCache()
        mockAttachmentHandler.clearAll()
        mockReconnectionHandler.clear()
    }
}

private object Response {
    fun configureSuccess(connected: Boolean = true, readOnly: Boolean = false): String =
        """{"type":"response","class":"SessionResponse","code":200,"body":{"connected":$connected,"newSession":true,"readOnly":$readOnly}}"""

    const val configureSuccessWithNewSessionFalse =
        """{"type":"response","class":"SessionResponse","code":200,"body":{"connected":true,"newSession":false}}"""
    const val webSocketRequestFailed =
        """{"type":"response","class":"string","code":400,"body":"Request failed."}"""
    const val defaultStructuredEvents =
        """{"eventType": "Typing","typing": {"type": "Off","duration": 1000}},{"eventType": "Typing","typing": {"type": "On","duration": 5000}}"""
    const val onMessage =
        """{"type":"message","class":"StructuredMessage","code":200,"body":{"text":"Hi!","direction":"Inbound","id":"test_id","channel":{"time":"2022-08-22T19:24:26.704Z","messageId":"message_id"},"type":"Text","metadata":{"customMessageId":"some_custom_message_id"}}}"""
    const val onMessageWithAttachment =
        """{"type":"message","class":"StructuredMessage","code":200,"body":{"direction":"Outbound","id":"msg_id","channel":{"time":"some_time","type":"Private"},"type":"Text","text":"Hi","content":[{"attachment":{"id":"attachment_id","filename":"image.png","mediaType":"Image","mime":"image/png","url":"https://downloadurl.com"},"contentType":"Attachment"}],"originatingEntity":"Human"}}"""
    const val attachmentDeleted =
        """{"type":"message","class":"AttachmentDeletedResponse","code":200,"body":{"attachmentId":"attachment_id"}}"""
    const val typingIndicatorForbidden =
        """{"type":"response","class":"string","code":403,"body":"Turn on the Feature Toggle or fix the configuration."}"""
    const val sessionNotFound =
        """{"type": "response","class": "string","code": 4007,"body": "session not found error message"}"""
    const val sessionExpired =
        """{"type": "response","class": "string","code": 4006,"body": "session expired error message"}"""
    const val sessionExpiredEvent =
        """{"type": "response","class": "SessionExpiredEvent","code": 200,"body": {}}"""
    const val messageTooLong =
        """{"type": "response","class": "string","code": 4011,"body": "message too long"}"""
    const val tooManyRequests =
        """{"type":"response","class":"TooManyRequestsErrorMessage","code":429,"body":{"retryAfter":3,"errorCode":4029,"errorMessage":"Message rate too high for this session"}}"""
    const val customAttributeSizeTooLarge =
        """{"type": "response","class": "string","code": 4013,"body": "Custom Attributes in channel metadata is larger than 2048 bytes"}"""
    const val connectionClosedEvent =
        """{"type":"message","class":"ConnectionClosedEvent","code":200,"body":{}}"""
    const val logoutEvent =
        """{"type":"message","class":"LogoutEvent","code":200,"body":{}}"""

    fun structuredMessageWithEvents(
        events: String = defaultStructuredEvents,
        direction: Message.Direction = Message.Direction.Outbound,
        metadata: Map<String, String> = mapOf("correlationId" to "0000000-0000-0000-0000-0000000000"),
    ): String {
        return """{"type": "message","class": "StructuredMessage","code": 200,"body": {"direction": "${direction.name}","id": "0000000-0000-0000-0000-0000000000","channel": {"time": "2022-03-09T13:35:31.104Z","messageId": "0000000-0000-0000-0000-0000000000"},"type": "Event","metadata": ${
        Json.encodeToString(metadata)
        },"events": [$events]}}"""
    }
}
