package com.genesys.cloud.messenger.transport

import assertk.assertThat
import com.genesys.cloud.messenger.transport.shyrka.receive.SessionResponse
import com.genesys.cloud.messenger.transport.shyrka.receive.TestShyrkaResponseMessages
import com.genesys.cloud.messenger.transport.shyrka.send.OnAttachmentRequest
import com.genesys.cloud.messenger.transport.shyrka.send.OnMessageRequest
import com.genesys.cloud.messenger.transport.shyrka.send.TestShyrkaSendMessages
import com.genesys.cloud.messenger.transport.shyrka.send.TextMessage
import com.genesys.cloud.messenger.transport.util.ErrorCode
import com.genesys.cloud.messenger.transport.util.PlatformSocket
import com.genesys.cloud.messenger.transport.util.PlatformSocketListener
import com.genesys.cloud.messenger.transport.util.ReconnectionHandler
import com.genesys.cloud.messenger.transport.util.logs.Log
import io.mockk.MockKVerificationScope
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifySequence
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.spyk
import io.mockk.verify
import io.mockk.verifySequence
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class MessagingClientImplTest {
    private val configuration = configuration()
    private val log: Log = Log(configuration.logging, "Log")
    private val platformSocketListenerSlot = slot<PlatformSocketListener>()
    private val mockStateListener: (MessagingClient.State) -> Unit = spyk()
    private val mockMessageStore: MessageStore = mockk(relaxed = true) {
        every { prepareMessage(any()) } returns OnMessageRequest(
            "00000000-0000-0000-0000-000000000000",
            TextMessage("Hello world")
        )
    }
    private val mockAttachmentHandler: AttachmentHandler = mockk(relaxed = true) {
        every {
            prepareAttachment(
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
    }
    private val mockPlatformSocket: PlatformSocket = mockk {
        every { openSocket(capture(platformSocketListenerSlot)) } answers {
            platformSocketListenerSlot.captured.onOpen()
        }
        every { closeSocket(any(), any()) } answers {
            platformSocketListenerSlot.captured.onClosed(
                1000,
                "The user has closed the connection."
            )
        }
        every { sendMessage(any()) } answers {
            platformSocketListenerSlot.captured.onMessage("")
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

        coEvery { fetchDeploymentConfig() } returns TestWebMessagingApiResponses.testDeploymentConfig
    }

    private val mockReconnectionHandler: ReconnectionHandler = mockk {
        every { resetAttempts() } answers { nothing }
        every { shouldReconnect() } returns true
        every { reconnect(captureLambda()) } answers { firstArg<() -> Unit>().invoke() }
    }
    private val testDispatcher = TestCoroutineDispatcher()
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
        it.stateListener = mockStateListener
    }

    @BeforeTest
    fun before() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun after() {
        Dispatchers.resetMain()
        clearAllMocks()
    }

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
        val expectedState =
            MessagingClient.State.Closed(1000, "The user has closed the connection.")
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

        verifySequence {
            connectSequence()
            mockPlatformSocket.sendMessage(TestShyrkaSendMessages.configureMessage())
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
            mockAttachmentHandler.clear()
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
            mockAttachmentHandler.prepareAttachment(any(), any(), any())
            mockPlatformSocket.sendMessage(expectedMessage)
        }
    }

    @Test
    fun whenDetach() {
        val expectedAttachmentId = "88888888-8888-8888-8888-888888888888"
        val attachmentIdSlot = slot<String>()

        subject.detach("88888888-8888-8888-8888-888888888888")

        verify {
            mockAttachmentHandler.detach(capture(attachmentIdSlot), any())
        }
        assertEquals(expectedAttachmentId, attachmentIdSlot.captured)
    }

    @Test
    fun whenDeleteAttachment() {
        val expectedMessage =
            """{"token":"00000000-0000-0000-0000-000000000000","attachmentId":"88888888-8888-8888-8888-888888888888","action":"deleteAttachment"}"""
        connectAndConfigure()

        subject.deleteAttachment("88888888-8888-8888-8888-888888888888")

        verifySequence {
            connectSequence()
            configureSequence()
            mockPlatformSocket.sendMessage(expectedMessage)
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
    fun whenSendMessageWithoutToken() {
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
    fun whenDeleteAttachmentWithoutConnection() {
        assertFailsWith<IllegalStateException> {
            subject.deleteAttachment("file.png")
        }
    }

    @Test
    fun whenSocketOnFailureAndShouldReconnectIsTrue() {
        subject.connect()

        platformSocketListenerSlot.captured.onFailure(Exception("Some error message"))

        verifySequence {
            connectSequence()
            reconnectSequence()
            mockPlatformSocket.openSocket(any())
            mockStateListener(MessagingClient.State.Connected)
            mockReconnectionHandler.resetAttempts()
            mockPlatformSocket.sendMessage(TestShyrkaSendMessages.configureMessage())
        }
    }

    @Test
    fun whenSocketOnFailureAndShouldReconnectIsFalse() {
        every { mockReconnectionHandler.shouldReconnect() } returns false
        subject.connect()

        platformSocketListenerSlot.captured.onFailure(Exception("Some error message"))

        assertThat(subject).isError(ErrorCode.WebsocketError, "Some error message")
        verifySequence {
            connectSequence()
            mockReconnectionHandler.shouldReconnect()
            mockStateListener(
                MessagingClient.State.Error(
                    ErrorCode.WebsocketError,
                    "Some error message"
                )
            )
        }
    }

    @Test
    fun whenSocketListenerInvokeOnMessageWithProperlyStructuredMessage() {
        val expectedSessionResponse = SessionResponse(connected = true, newSession = true)
        subject.connect()

        platformSocketListenerSlot.captured.onMessage(TestShyrkaResponseMessages.sessionResponseOk())

        assertThat(subject).isConfigured(
            expectedSessionResponse.connected,
            expectedSessionResponse.newSession
        )
        verifySequence {
            connectSequence()
            mockStateListener(MessagingClient.State.Configured(connected = true, newSession = true))
        }
    }

    @Test
    fun whenFetchDeploymentConfig() {
        runBlocking { subject.fetchDeploymentConfig() }

        coVerify { mockWebMessagingApi.fetchDeploymentConfig() }
    }

    @Test
    fun whenFetchDeploymentConfigThrowsCancellationException() {
        coEvery {
            mockWebMessagingApi.fetchDeploymentConfig()
        } throws CancellationException()

        runBlocking {
            assertFailsWith<CancellationException> { subject.fetchDeploymentConfig() }
        }
    }

    @Test
    fun whenSocketListenerInvokeOnMessageWithSessionExpiredStringMessage() {
        val expectedErrorState =
            MessagingClient.State.Error(
                ErrorCode.SessionHasExpired,
                "session expired error message"
            )
        subject.connect()

        platformSocketListenerSlot.captured.onMessage(
            TestShyrkaResponseMessages.stringResponseOk(
                4006,
                "session expired error message"
            )
        )

        verifySequence {
            connectSequence()
            mockStateListener(expectedErrorState)
        }
    }

    @Test
    fun whenSocketListenerInvokeOnMessageWithSessionNotFoundStringMessage() {
        val expectedErrorState =
            MessagingClient.State.Error(
                ErrorCode.SessionNotFound,
                "session not found error message"
            )
        subject.connect()

        platformSocketListenerSlot.captured.onMessage(
            TestShyrkaResponseMessages.stringResponseOk(
                4007,
                "session not found error message"
            )
        )

        verifySequence {
            connectSequence()
            mockStateListener(expectedErrorState)
        }
    }

    @Test
    fun whenSocketListenerInvokeOnMessageWithMessageTooLongStringMessage() {
        subject.connect()

        platformSocketListenerSlot.captured.onMessage(
            TestShyrkaResponseMessages.stringResponseOk(
                4011,
                "message too long"
            )
        )

        verifySequence {
            connectSequence()
            mockMessageStore.onMessageError(ErrorCode.MessageTooLong, "message too long")
        }
    }

    @Test
    fun whenSocketListenerInvokeOnMessageWitSessionExpiredEvent() {
        val expectedErrorState =
            MessagingClient.State.Error(ErrorCode.SessionHasExpired, null)
        subject.connect()

        platformSocketListenerSlot.captured.onMessage(TestShyrkaResponseMessages.sessionExpiredResponseOk)

        verifySequence {
            connectSequence()
            mockStateListener(expectedErrorState)
        }
    }

    @Test
    fun whenCurrentStateUpdatedMultipleTimesWithSameState() {
        subject.connect()
        doNotOpenSocket()

        platformSocketListenerSlot.captured.onFailure(Exception("Some error message"))
        platformSocketListenerSlot.captured.onFailure(Exception("Some other error message"))

        verify(exactly = 1) {
            mockStateListener(MessagingClient.State.Reconnecting)
        }
    }

    @Test
    fun whenConnectDuringReconnection() {
        subject.connect()
        doNotOpenSocket()
        platformSocketListenerSlot.captured.onFailure(Exception("Some error message"))

        assertThat(subject).isReconnecting()
    }

    @Test
    fun whenStartSessionWithHistoryCalled() {
        every { mockPlatformSocket.sendMessage(TestShyrkaSendMessages.configureMessage()) } answers {
            platformSocketListenerSlot.captured.onMessage(
                TestShyrkaResponseMessages.sessionResponseOk()
            )
        }

        subject.startSessionWithHistory()

        coVerifySequence {
            connectSequence()
            configureSequence()
            // jwtHandler.withJwt { } is expected to be invoked,
            // but Inline functions cannot be mocked: see the discussion on this issue: https://github.com/mockk/mockk/issues/27
        }
    }

    private fun doNotOpenSocket() =
        every { mockPlatformSocket.openSocket(any()) } answers { nothing }

    private fun connectAndConfigure() {
        subject.connect()
        subject.configureSession()
        platformSocketListenerSlot.captured.onMessage(TestShyrkaResponseMessages.sessionResponseOk())
    }

    private fun configuration(): Configuration = Configuration(
        deploymentId = "deploymentId",
        domain = "inindca.com",
        tokenStoreKey = "tokenStoreKey"
    )

    private fun MockKVerificationScope.connectSequence() {
        mockStateListener(MessagingClient.State.Connecting)
        mockPlatformSocket.openSocket(any())
        mockStateListener(MessagingClient.State.Connected)
        mockReconnectionHandler.resetAttempts()
    }

    private fun MockKVerificationScope.disconnectSequence(
        expectedCloseCode: Int = any(),
        expectedCloseReason: String = any()
    ) {
        mockStateListener(MessagingClient.State.Closing(expectedCloseCode, expectedCloseReason))
        mockPlatformSocket.closeSocket(expectedCloseCode, expectedCloseReason)
        mockStateListener(MessagingClient.State.Closed(expectedCloseCode, expectedCloseReason))
    }

    private fun MockKVerificationScope.configureSequence(expected: String = any()) {
        mockPlatformSocket.sendMessage(expected)
        mockStateListener(MessagingClient.State.Configured(connected = true, newSession = true))
    }

    private fun MockKVerificationScope.reconnectSequence() {
        mockReconnectionHandler.shouldReconnect()
        mockStateListener(MessagingClient.State.Reconnecting)
        mockMessageStore.reset()
        mockReconnectionHandler.reconnect(any())
    }
}
