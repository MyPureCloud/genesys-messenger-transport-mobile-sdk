package com.genesys.cloud.messenger.transport.core

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.genesys.cloud.messenger.transport.core.MessagingClient.State
import com.genesys.cloud.messenger.transport.network.PlatformSocket
import com.genesys.cloud.messenger.transport.network.PlatformSocketListener
import com.genesys.cloud.messenger.transport.network.ReconnectionHandlerImpl
import com.genesys.cloud.messenger.transport.network.TestWebMessagingApiResponses
import com.genesys.cloud.messenger.transport.network.WebMessagingApi
import com.genesys.cloud.messenger.transport.shyrka.receive.SessionResponse
import com.genesys.cloud.messenger.transport.shyrka.send.Channel
import com.genesys.cloud.messenger.transport.shyrka.send.Channel.Metadata
import com.genesys.cloud.messenger.transport.shyrka.send.DeleteAttachmentRequest
import com.genesys.cloud.messenger.transport.shyrka.send.OnAttachmentRequest
import com.genesys.cloud.messenger.transport.shyrka.send.OnMessageRequest
import com.genesys.cloud.messenger.transport.shyrka.send.TextMessage
import com.genesys.cloud.messenger.transport.util.logs.Log
import io.mockk.Called
import io.mockk.MockKVerificationScope
import io.mockk.clearAllMocks
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.spyk
import io.mockk.verify
import io.mockk.verifySequence
import kotlinx.coroutines.runBlocking
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class MessagingClientImplTest {
    private val configuration = configuration()
    private val log: Log = Log(configuration.logging, "Log")
    private val slot = slot<PlatformSocketListener>()
    private val mockStateChangedListener: (StateChange) -> Unit = spyk()
    private val mockMessageStore: MessageStore = mockk(relaxed = true) {
        every { prepareMessage(any(), any()) } returns OnMessageRequest(
            "00000000-0000-0000-0000-000000000000",
            TextMessage("Hello world")
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
            token = "00000000-0000-0000-0000-000000000000",
            attachmentId = "88888888-8888-8888-8888-888888888888",
            fileName = "test_attachment.png",
            fileType = "image/png",
            errorsAsJson = true,
        )

        every { detach(any()) } returns DeleteAttachmentRequest(
            "00000000-0000-0000-0000-000000000000",
            "88888888-8888-8888-8888-888888888888"
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
        every { sendMessage(Request.configureRequest) } answers {
            slot.captured.onMessage(Response.configureSuccess)
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

    private val subject = MessagingClientImpl(
        log = log,
        configuration = configuration,
        webSocket = mockPlatformSocket,
        api = mockWebMessagingApi,
        token = "00000000-0000-0000-0000-000000000000",
        jwtHandler = mockk(),
        attachmentHandler = mockAttachmentHandler,
        messageStore = mockMessageStore,
        reconnectionHandler = mockReconnectionHandler,
    ).also {
        it.stateChangedListener = mockStateChangedListener
    }

    @AfterTest
    fun after() = clearAllMocks()

    @Test
    fun whenConnect() {
        subject.connect()

        assertThat(subject).isConnected()
        verifySequence {
            connectSequence()
        }
    }

    @Test
    fun whenConnectAndThenDisconnect() {
        val expectedState = State.Closed(1000, "The user has closed the connection.")
        subject.connect()

        subject.disconnect()

        assertThat(subject).isClosed(expectedState.code, expectedState.reason)
        verifySequence {
            connectSequence()
            disconnectSequence(expectedState.code, expectedState.reason)
        }
    }

    @Test
    fun whenConnectAndThenConfigureSession() {
        subject.connect()

        subject.configureSession()

        assertThat(subject).isConfigured(true, true)
        verifySequence {
            connectSequence()
            configureSequence()
        }
    }

    @Test
    fun whenConnectConfigureSessionAndThenSendMessage() {
        val expectedMessage =
            """{"token":"00000000-0000-0000-0000-000000000000","message":{"text":"Hello world","type":"Text"},"action":"onMessage"}"""
        val expectedText = "Hello world"
        connectAndConfigure()

        subject.sendMessage("Hello world")

        verifySequence {
            connectSequence()
            configureSequence()
            mockMessageStore.prepareMessage(expectedText)
            mockAttachmentHandler.onSending()
            mockPlatformSocket.sendMessage(expectedMessage)
        }
    }

    @Test
    fun whenSendHealthCheckWithToken() {
        val expectedMessage =
            """{"token":"00000000-0000-0000-0000-000000000000","action":"echo","message":{"text":"ping","type":"Text"}}"""
        connectAndConfigure()

        subject.sendHealthCheck()

        verifySequence {
            connectSequence()
            configureSequence()
            mockPlatformSocket.sendMessage(expectedMessage)
        }
    }

    @Test
    fun whenAttach() {
        val expectedAttachmentId = "88888888-8888-8888-8888-888888888888"
        val expectedMessage =
            """{"token":"00000000-0000-0000-0000-000000000000","attachmentId":"88888888-8888-8888-8888-888888888888","fileName":"test_attachment.png","fileType":"image/png","errorsAsJson":true,"action":"onAttachment"}"""
        connectAndConfigure()

        val result = subject.attach(ByteArray(1), "test.png")

        assertEquals(expectedAttachmentId, result)
        verifySequence {
            connectSequence()
            configureSequence()
            mockAttachmentHandler.prepare(any(), any(), any())
            mockPlatformSocket.sendMessage(expectedMessage)
        }
    }

    @Test
    fun whenDetach() {
        val expectedAttachmentId = "88888888-8888-8888-8888-888888888888"
        val expectedMessage =
            """{"token":"00000000-0000-0000-0000-000000000000","attachmentId":"88888888-8888-8888-8888-888888888888","action":"deleteAttachment"}"""
        val attachmentIdSlot = slot<String>()
        connectAndConfigure()

        subject.detach("88888888-8888-8888-8888-888888888888")

        verify {
            mockAttachmentHandler.detach(capture(attachmentIdSlot))
            mockPlatformSocket.sendMessage(expectedMessage)
        }
        assertThat(attachmentIdSlot.captured).isEqualTo(expectedAttachmentId)
    }

    @Test
    fun whenDetachNonExistingAttachmentId() {
        connectAndConfigure()
        clearMocks(mockPlatformSocket)
        every { mockAttachmentHandler.detach(any()) } returns null

        subject.detach("88888888-8888-8888-8888-888888888888")

        verify {
            mockAttachmentHandler.detach("88888888-8888-8888-8888-888888888888")
            mockPlatformSocket wasNot Called
        }
    }

    @Test
    fun whenNotConnectedAndConfigureVerifyIllegalStateExceptionThrown() {
        assertFailsWith<IllegalStateException> {
            subject.configureSession()
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
    fun whenConfigureFailsAndSocketListenerRespondWithWebsocketErrorAndThereAreNoReconnectionAttemptsLeft() {
        val expectedException = Exception(ErrorMessage.FailedToReconnect)
        val expectedErrorState = State.Error(ErrorCode.WebsocketError, ErrorMessage.FailedToReconnect)

        subject.connect()

        slot.captured.onFailure(expectedException, ErrorCode.WebsocketError)

        assertThat(subject).isError(expectedErrorState.code, expectedErrorState.message)
        verifySequence {
            connectSequence()
            mockMessageStore.invalidateConversationCache()
            mockReconnectionHandler.shouldReconnect
            mockStateChangedListener(fromConnectedToError(expectedErrorState))
            mockAttachmentHandler.clearAll()
            mockReconnectionHandler.clear()
        }
    }

    @Test
    fun whenConfigureFailsAndSocketListenerRespondWithNetworkDisabledError() {
        val expectedException = Exception(ErrorMessage.InternetConnectionIsOffline)
        val expectedErrorState = State.Error(ErrorCode.NetworkDisabled, ErrorMessage.InternetConnectionIsOffline)

        subject.connect()

        slot.captured.onFailure(expectedException, ErrorCode.NetworkDisabled)

        assertThat(subject).isError(expectedErrorState.code, expectedErrorState.message)
        verifySequence {
            connectSequence()
            mockStateChangedListener(fromConnectedToError(expectedErrorState))
            mockAttachmentHandler.clearAll()
            mockReconnectionHandler.clear()
        }
    }

    @Test
    fun whenSocketListenerInvokeOnMessageWithProperlyStructuredMessage() {
        val expectedSessionResponse = SessionResponse(connected = true, newSession = true)
        val expectedConfigureState = State.Configured(connected = true, newSession = true)
        subject.connect()

        slot.captured.onMessage(Response.configureSuccess)

        assertThat(subject).isConfigured(
            expectedSessionResponse.connected,
            expectedSessionResponse.newSession,
        )
        verifySequence {
            connectSequence()
            mockStateChangedListener(fromConnectedToConfigured(expectedConfigureState))
        }
    }

    @Test
    fun whenSocketListenerInvokeOnMessageWithSessionExpiredStringMessage() {
        val expectedErrorState =
            State.Error(ErrorCode.SessionHasExpired, "session expired error message")
        val givenRawMessage =
            """
            {
              "type": "response",
              "class": "string",
              "code": 4006,
              "body": "session expired error message"
            }
            """
        subject.connect()

        slot.captured.onMessage(givenRawMessage)

        verifySequence {
            connectSequence()
            mockStateChangedListener(fromConnectedToError(expectedErrorState))
        }
    }

    @Test
    fun whenSocketListenerInvokeOnMessageWithSessionNotFoundStringMessage() {
        val expectedErrorState =
            State.Error(ErrorCode.SessionNotFound, "session not found error message")
        val givenRawMessage =
            """
            {
              "type": "response",
              "class": "string",
              "code": 4007,
              "body": "session not found error message"
            }
            """
        subject.connect()

        slot.captured.onMessage(givenRawMessage)

        verifySequence {
            connectSequence()
            mockStateChangedListener(fromConnectedToError(expectedErrorState))
        }
    }

    @Test
    fun whenSocketListenerInvokeOnMessageWithMessageTooLongStringMessage() {
        val givenRawMessage =
            """
            {
              "type": "response",
              "class": "string",
              "code": 4011,
              "body": "message too long"
            }
            """
        subject.connect()

        slot.captured.onMessage(givenRawMessage)

        verifySequence {
            connectSequence()
            mockMessageStore.onMessageError(ErrorCode.MessageTooLong, "message too long")
        }
    }

    @Test
    fun whenSocketListenerInvokeTooManyRequestsErrorMessage() {
        val givenRawMessage =
            """{"type":"response","class":"TooManyRequestsErrorMessage","code":429,"body":{"retryAfter":3,"errorCode":4029,"errorMessage":"Message rate too high for this session"}}""".trimIndent()
        subject.connect()

        slot.captured.onMessage(givenRawMessage)

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
        val givenRawMessage =
            """
            {
              "type": "response",
              "class": "SessionExpiredEvent",
              "code": 200,
              "body": {}
            }
            """
        subject.connect()

        slot.captured.onMessage(givenRawMessage)

        verifySequence {
            connectSequence()
            mockStateChangedListener(fromConnectedToError(expectedErrorState))
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

        val givenRawMessage =
            """{"type":"message","class":"StructuredMessage","code":200,"body":{"direction":"Outbound","id":"msg_id","channel":{"time":"some_time","type":"Private"},"type":"Text","text":"Hi","content":[{"attachment":{"id":"attachment_id","filename":"image.png","mediaType":"Image","mime":"image/png","url":"https://downloadurl.com"},"contentType":"Attachment"}],"originatingEntity":"Human"}}"""
        subject.connect()

        slot.captured.onMessage(givenRawMessage)

        verifySequence {
            connectSequence()
            mockMessageStore.update(expectedMessage)
            mockAttachmentHandler.onSent(mapOf("attachment_id" to expectedAttachment))
        }
    }

    @Test
    fun whenSocketListenerInvokeOnMessageWithAttachmentDeletedResponse() {
        val expectedAttachmentId = "attachment_id"
        val givenRawMessage =
            """{"type":"message","class":"AttachmentDeletedResponse","code":200,"body":{"attachmentId":"attachment_id"}}"""
        subject.connect()

        slot.captured.onMessage(givenRawMessage)

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
        connectAndConfigure()

        runBlocking { subject.fetchNextPage() }

        verify {
            mockMessageStore.updateMessageHistory(emptyList(), DEFAULT_PAGE_SIZE)
        }
    }

    @Test
    fun whenSendMessageWithCustomAttributes() {
        val expectedMessage =
            """{"token":"00000000-0000-0000-0000-000000000000","message":{"text":"Hello world","type":"Text"},"channel":{"metadata":{"customAttributes":{"A":"B"}}},"action":"onMessage"}"""
        val expectedText = "Hello world"
        val expectedCustomAttributes = mapOf("A" to "B")
        every { mockMessageStore.prepareMessage(any(), any()) } returns OnMessageRequest(
            token = "00000000-0000-0000-0000-000000000000",
            message = TextMessage("Hello world"),
            channel = Channel(Metadata(expectedCustomAttributes)),
        )
        connectAndConfigure()

        subject.sendMessage(text = "Hello world", customAttributes = mapOf("A" to "B"))

        verifySequence {
            connectSequence()
            configureSequence()
            mockMessageStore.prepareMessage(expectedText, expectedCustomAttributes)
            mockAttachmentHandler.onSending()
            mockPlatformSocket.sendMessage(expectedMessage)
        }
    }

    @Test
    fun whenMessageWithCustomAttributesIsTooLarge() {
        val givenRawMessage =
            """
            {
              "type": "response",
              "class": "string",
              "code": 4013,
              "body": "Custom Attributes in channel metadata is larger than 2048 bytes"
            }
            """
        val expectedErrorCode = ErrorCode.CustomAttributeSizeTooLarge
        val expectedErrorMessage = "Custom Attributes in channel metadata is larger than 2048 bytes"
        subject.connect()

        slot.captured.onMessage(givenRawMessage)

        verifySequence {
            connectSequence()
            mockMessageStore.onMessageError(expectedErrorCode, expectedErrorMessage)
        }
    }

    @Test
    fun whenConnectWithConfigureSetToTrue() {
        subject.connect(shouldConfigure = true)

        assertThat(subject).isConfigured(true, true)
        verifySequence {
            connectSequence()
            configureSequence()
        }
    }

    @Test
    fun whenConnectWithConfigureSetToFalse() {
        subject.connect(shouldConfigure = false)

        assertThat(subject).isConnected()
        verifySequence {
            connectSequence()
        }
    }

    @Test
    fun whenConnectWithConfigureHasClientResponseError() {
        val expectedErrorCode = ErrorCode.ClientResponseError(400)
        val expectedErrorMessage = "Deployment not found"
        val expectedErrorState = State.Error(expectedErrorCode, expectedErrorMessage)
        every { mockPlatformSocket.sendMessage(Request.configureRequest) } answers {
            slot.captured.onMessage(Response.configureFail)
        }

        subject.connect(shouldConfigure = true)

        assertThat(subject).isError(expectedErrorCode, expectedErrorMessage)
        verifySequence {
            connectSequence()
            mockPlatformSocket.sendMessage(Request.configureRequest)
            mockStateChangedListener(fromConnectedToError(expectedErrorState))
        }
    }

    private fun connectAndConfigure() {
        subject.connect()
        subject.configureSession()
        slot.captured.onMessage(Response.configureSuccess)
    }

    private fun configuration(): Configuration = Configuration(
        deploymentId = "deploymentId",
        domain = "inindca.com",
    )

    private fun MockKVerificationScope.connectSequence() {
        val fromIdleToConnecting =
            StateChange(oldState = State.Idle, newState = State.Connecting)
        val fromConnectingToConnected =
            StateChange(oldState = State.Connecting, newState = State.Connected)
        mockStateChangedListener(fromIdleToConnecting)
        mockPlatformSocket.openSocket(any())
        mockStateChangedListener(fromConnectingToConnected)
    }

    private fun MockKVerificationScope.disconnectSequence(
        expectedCloseCode: Int = any(),
        expectedCloseReason: String = any(),
    ) {
        val fromConnectedToClosing = StateChange(
            oldState = State.Connected,
            newState = State.Closing(expectedCloseCode, expectedCloseReason)
        )
        val fromClosingToClosed = StateChange(
            oldState = State.Closing(expectedCloseCode, expectedCloseReason),
            newState = State.Closed(expectedCloseCode, expectedCloseReason)
        )
        mockStateChangedListener(fromConnectedToClosing)
        mockPlatformSocket.closeSocket(expectedCloseCode, expectedCloseReason)
        mockStateChangedListener(fromClosingToClosed)
        mockMessageStore.invalidateConversationCache()
        mockAttachmentHandler.clearAll()
    }

    private fun MockKVerificationScope.configureSequence() {
        val expectedConfigureState = State.Configured(connected = true, newSession = true)
        mockPlatformSocket.sendMessage(Request.configureRequest)
        mockStateChangedListener(fromConnectedToConfigured(expectedConfigureState))
    }

    private fun fromConnectedToConfigured(configured: State) =
        StateChange(oldState = State.Connected, newState = configured)

    private fun fromConnectedToError(errorState: State) =
        StateChange(oldState = State.Connected, newState = errorState)
}

private object Request {
    const val configureRequest =
        """{"token":"00000000-0000-0000-0000-000000000000","deploymentId":"deploymentId","journeyContext":{"customer":{"id":"00000000-0000-0000-0000-000000000000","idType":"cookie"},"customerSession":{"id":"","type":"web"}},"action":"configureSession"}"""
}

private object Response {
    const val configureSuccess =
        """{"type":"response","class":"SessionResponse","code":200,"body":{"connected":true,"newSession":true}}"""
    const val configureFail =
        """{"type":"response","class":"string","code":400,"body":"Deployment not found"}"""
}
