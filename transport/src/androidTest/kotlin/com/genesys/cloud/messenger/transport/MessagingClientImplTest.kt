package com.genesys.cloud.messenger.transport

import assertk.assertThat
import com.genesys.cloud.messenger.transport.shyrka.receive.SessionResponse
import com.genesys.cloud.messenger.transport.shyrka.send.OnAttachmentRequest
import com.genesys.cloud.messenger.transport.shyrka.send.OnMessageRequest
import com.genesys.cloud.messenger.transport.shyrka.send.TextMessage
import com.genesys.cloud.messenger.transport.util.ErrorCode
import com.genesys.cloud.messenger.transport.util.PlatformSocket
import com.genesys.cloud.messenger.transport.util.PlatformSocketListener
import com.genesys.cloud.messenger.transport.util.SocketCloseCode
import com.genesys.cloud.messenger.transport.util.logs.Log
import io.mockk.MockKVerificationScope
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.spyk
import io.mockk.verify
import io.mockk.verifySequence
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class MessagingClientImplTest {
    private val configuration = configuration()
    private val log: Log = Log(configuration.logging, "Log")
    private val slot = slot<PlatformSocketListener>()
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
        every { openSocket(capture(slot)) } answers {
            slot.captured.onOpen()
        }
        every { closeSocket(any(), any()) } answers {
            slot.captured.onClosed(1000, "The user has closed the connection.")
        }
        every { sendMessage(any()) } answers {
            slot.captured.onMessage("")
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

    private val subject = MessagingClientImpl(
        log = log,
        configuration = configuration,
        webSocket = mockPlatformSocket,
        api = mockWebMessagingApi,
        token = "00000000-0000-0000-0000-000000000000",
        jwtHandler = mockk(),
        attachmentHandler = mockAttachmentHandler,
        messageStore = mockMessageStore,
        reconnectionManager = mockk(),
    ).also {
        it.stateListener = mockStateListener
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
        val expectedConfigureMessage =
            """{"token":"00000000-0000-0000-0000-000000000000","deploymentId":"deploymentId","journeyContext":{"customer":{"id":"00000000-0000-0000-0000-000000000000","idType":"cookie"},"customerSession":{"id":"","type":"web"}},"action":"configureSession"}"""
        subject.connect()

        subject.configureSession()

        verifySequence {
            connectSequence()
            mockPlatformSocket.sendMessage(expectedConfigureMessage)
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
    fun whenConfigureFailsAndSocketListenerRespondWithOnFailure() {
        val expectedException = Exception("Some error message")
        val expectedErrorState =
            MessagingClient.State.Error(ErrorCode.WebsocketError, "Some error message")
        val expectedState =
            MessagingClient.State.Closed(SocketCloseCode.GOING_AWAY.value, "Going away.")
        every { mockPlatformSocket.closeSocket(any(), any()) } answers {
            slot.captured.onClosed(1001, "Going away.")
        }
        subject.connect()

        slot.captured.onFailure(expectedException)

        assertThat(subject).isClosed(expectedState.code, expectedState.reason)
        verifySequence {
            connectSequence()
            mockStateListener(expectedErrorState)
            mockPlatformSocket.closeSocket(expectedState.code, expectedState.reason)
            mockStateListener(expectedState)
        }
    }

    @Test
    fun whenSocketListenerInvokeOnMessageWithProperlyStructuredMessage() {
        val expectedSessionResponse = SessionResponse(connected = true, newSession = true)
        val expectedRawMessage =
            """
            {
              "type": "response",
              "class": "SessionResponse",
              "code": 200,
              "body": {
                "connected": true,
                "newSession": true
              }
            }
            """
        subject.connect()

        slot.captured.onMessage(expectedRawMessage)

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
            mockStateListener(expectedErrorState)
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
    fun whenSocketListenerInvokeOnMessageWitSessionExpiredEvent() {
        val expectedErrorState =
            MessagingClient.State.Error(ErrorCode.SessionHasExpired, null)
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
            mockStateListener(expectedErrorState)
        }
    }

    private fun connectAndConfigure() {
        val sessionResponseMessage =
            """
            {
              "type": "response",
              "class": "SessionResponse",
              "code": 200,
              "body": {
                "connected": true,
                "newSession": true
              }
            }
            """
        subject.connect()
        subject.configureSession()
        slot.captured.onMessage(sessionResponseMessage)
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
}
