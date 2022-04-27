package com.genesys.cloud.messenger.transport.core

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.genesys.cloud.messenger.transport.network.PlatformSocket
import com.genesys.cloud.messenger.transport.network.PlatformSocketListener
import com.genesys.cloud.messenger.transport.network.SocketCloseCode
import com.genesys.cloud.messenger.transport.network.TestWebMessagingApiResponses
import com.genesys.cloud.messenger.transport.network.WebMessagingApi
import com.genesys.cloud.messenger.transport.shyrka.receive.SessionResponse
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
import java.lang.UnsupportedOperationException
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

    private val subject = MessagingClientImpl(
        log = log,
        configuration = configuration,
        webSocket = mockPlatformSocket,
        api = mockWebMessagingApi,
        token = "00000000-0000-0000-0000-000000000000",
        jwtHandler = mockk(),
        attachmentHandler = mockAttachmentHandler,
        messageStore = mockMessageStore,
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
            mockAttachmentHandler.clearAll()
            mockPlatformSocket.closeSocket(expectedState.code, expectedState.reason)
            mockStateListener(expectedState)
            mockMessageStore.invalidateConversationCache()
            mockAttachmentHandler.clearAll()
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
        expectedCloseReason: String = any(),
    ) {
        mockStateListener(MessagingClient.State.Closing(expectedCloseCode, expectedCloseReason))
        mockPlatformSocket.closeSocket(expectedCloseCode, expectedCloseReason)
        mockStateListener(MessagingClient.State.Closed(expectedCloseCode, expectedCloseReason))
        mockMessageStore.invalidateConversationCache()
        mockAttachmentHandler.clearAll()
    }

    private fun MockKVerificationScope.configureSequence(expected: String = any()) {
        mockPlatformSocket.sendMessage(expected)
        mockStateListener(MessagingClient.State.Configured(connected = true, newSession = true))
    }
}
