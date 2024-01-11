package com.genesys.cloud.messenger.transport.core.messagingclient

import com.genesys.cloud.messenger.transport.auth.AuthHandler
import com.genesys.cloud.messenger.transport.core.AttachmentHandler
import com.genesys.cloud.messenger.transport.core.Configuration
import com.genesys.cloud.messenger.transport.core.CustomAttributesStoreImpl
import com.genesys.cloud.messenger.transport.core.Empty
import com.genesys.cloud.messenger.transport.core.JwtHandler
import com.genesys.cloud.messenger.transport.core.MessageStore
import com.genesys.cloud.messenger.transport.core.MessagingClient
import com.genesys.cloud.messenger.transport.core.MessagingClientImpl
import com.genesys.cloud.messenger.transport.core.Result
import com.genesys.cloud.messenger.transport.core.StateChange
import com.genesys.cloud.messenger.transport.core.events.EventHandler
import com.genesys.cloud.messenger.transport.core.events.HealthCheckProvider
import com.genesys.cloud.messenger.transport.core.events.UserTypingProvider
import com.genesys.cloud.messenger.transport.network.PlatformSocket
import com.genesys.cloud.messenger.transport.network.PlatformSocketListener
import com.genesys.cloud.messenger.transport.network.ReconnectionHandlerImpl
import com.genesys.cloud.messenger.transport.network.TestWebMessagingApiResponses
import com.genesys.cloud.messenger.transport.network.WebMessagingApi
import com.genesys.cloud.messenger.transport.shyrka.receive.DeploymentConfig
import com.genesys.cloud.messenger.transport.shyrka.receive.createDeploymentConfigForTesting
import com.genesys.cloud.messenger.transport.shyrka.send.DeleteAttachmentRequest
import com.genesys.cloud.messenger.transport.shyrka.send.OnAttachmentRequest
import com.genesys.cloud.messenger.transport.shyrka.send.OnMessageRequest
import com.genesys.cloud.messenger.transport.shyrka.send.TextMessage
import com.genesys.cloud.messenger.transport.util.DefaultVault
import com.genesys.cloud.messenger.transport.util.Platform
import com.genesys.cloud.messenger.transport.util.Request
import com.genesys.cloud.messenger.transport.util.Response
import com.genesys.cloud.messenger.transport.util.fromConnectedToConfigured
import com.genesys.cloud.messenger.transport.util.fromConnectedToReadOnly
import com.genesys.cloud.messenger.transport.util.fromConnectingToConnected
import com.genesys.cloud.messenger.transport.util.fromIdleToConnecting
import com.genesys.cloud.messenger.transport.util.logs.Log
import com.genesys.cloud.messenger.transport.util.logs.LogTag
import com.genesys.cloud.messenger.transport.utility.AuthTest
import io.mockk.MockKVerificationScope
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.invoke
import io.mockk.mockk
import io.mockk.slot
import io.mockk.spyk
import kotlin.reflect.KProperty0
import kotlin.test.AfterTest

open class BaseMessagingClientTest {
    internal val slot = slot<PlatformSocketListener>()
    protected val mockStateChangedListener: (StateChange) -> Unit = spyk()
    internal val mockMessageStore: MessageStore = mockk(relaxed = true) {
        every { prepareMessage(any(), any()) } returns OnMessageRequest(
            token = Request.token,
            message = TextMessage("Hello world")
        )
    }
    internal val mockAttachmentHandler: AttachmentHandler = mockk(relaxed = true) {
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
    internal val mockPlatformSocket: PlatformSocket = mockk {
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
        } returns Result.Success(TestWebMessagingApiResponses.testMessageEntityList)
    }

    internal val mockReconnectionHandler: ReconnectionHandlerImpl = mockk(relaxed = true) {
        every { shouldReconnect } returns false
    }

    internal val mockEventHandler: EventHandler = mockk(relaxed = true)
    protected val mockTimestampFunction: () -> Long = spyk<() -> Long>().also {
        every { it.invoke() } answers { Platform().epochMillis() }
    }
    protected val mockShowUserTypingIndicatorFunction: () -> Boolean = spyk<() -> Boolean>().also {
        every { it.invoke() } returns true
    }
    protected val mockDeploymentConfig = mockk<KProperty0<DeploymentConfig?>> {
        every { get() } returns createDeploymentConfigForTesting()
    }
    internal val userTypingProvider = UserTypingProvider(
        log = mockk(relaxed = true),
        showUserTypingEnabled = mockShowUserTypingIndicatorFunction,
        getCurrentTimestamp = mockTimestampFunction,
    )
    internal val mockAuthHandler: AuthHandler = mockk(relaxed = true) {
        every { jwt } returns AuthTest.JwtToken
        every { refreshToken(captureLambda<(Result<Any>) -> Unit>()) } answers {
            lambda<(Result<Any>) -> Unit>().invoke(Result.Success(Empty()))
        }
    }

    internal val mockCustomAttributesStore: CustomAttributesStoreImpl = mockk(relaxed = true) {
        val dummyCustomAttributes = mutableMapOf("A" to "B")
        every { get() } returns dummyCustomAttributes
        every { getCustomAttributesToSend() } returns dummyCustomAttributes
        every { add(emptyMap()) } answers { dummyCustomAttributes.clear() }
    }

    private val mockVault: DefaultVault = mockk {
        every { fetch("token") } returns Request.token
        every { token } returns Request.token
    }
    internal val mockJwtHandler: JwtHandler = mockk(relaxed = true)

    internal val mockLogger: Log = mockk(relaxed = true)
    internal val logSlot = mutableListOf<() -> String>()

    internal val subject = MessagingClientImpl(
        log = mockLogger,
        configuration = Configuration("deploymentId", "inindca.com"),
        webSocket = mockPlatformSocket,
        api = mockWebMessagingApi,
        token = Request.token,
        jwtHandler = mockJwtHandler,
        vault = mockVault,
        attachmentHandler = mockAttachmentHandler,
        messageStore = mockMessageStore,
        reconnectionHandler = mockReconnectionHandler,
        eventHandler = mockEventHandler,
        userTypingProvider = userTypingProvider,
        healthCheckProvider = HealthCheckProvider(mockk(relaxed = true), mockTimestampFunction),
        deploymentConfig = mockDeploymentConfig,
        authHandler = mockAuthHandler,
        internalCustomAttributesStore = mockCustomAttributesStore,
    ).also {
        it.stateChangedListener = mockStateChangedListener
    }

    @AfterTest
    fun after() = clearAllMocks()

    protected fun MockKVerificationScope.connectSequence(shouldConfigureAuth: Boolean = false) {
        val configureRequest =
            if (shouldConfigureAuth) Request.configureAuthenticatedRequest() else Request.configureRequest()
        mockLogger.withTag(LogTag.STATE_MACHINE)
        mockLogger.withTag(LogTag.WEBSOCKET)
        mockLogger.i(capture(logSlot))
        mockStateChangedListener(fromIdleToConnecting)
        mockPlatformSocket.openSocket(any())
        mockStateChangedListener(fromConnectingToConnected)
        mockLogger.i(capture(logSlot))
        if (shouldConfigureAuth) {
            mockAuthHandler.jwt // check if jwt is valid
            mockAuthHandler.jwt // use jwt for request
        }
        mockPlatformSocket.sendMessage(configureRequest)
        mockReconnectionHandler.clear()
        mockStateChangedListener(fromConnectedToConfigured)
    }

    protected fun MockKVerificationScope.connectToReadOnlySequence() {
        mockLogger.withTag(LogTag.STATE_MACHINE)
        mockLogger.withTag(LogTag.WEBSOCKET)
        mockLogger.i(capture(logSlot))
        mockStateChangedListener(fromIdleToConnecting)
        mockPlatformSocket.openSocket(any())
        mockStateChangedListener(fromConnectingToConnected)
        mockLogger.i(capture(logSlot))
        mockPlatformSocket.sendMessage(Request.configureRequest())
        mockReconnectionHandler.clear()
        mockStateChangedListener(fromConnectedToReadOnly)
    }

    protected fun MockKVerificationScope.disconnectSequence(
        expectedCloseCode: Int = 1000,
        expectedCloseReason: String = "The user has closed the connection.",
    ) {
        val fromConfiguredToClosing = StateChange(
            oldState = MessagingClient.State.Configured(connected = true, newSession = true),
            newState = MessagingClient.State.Closing(expectedCloseCode, expectedCloseReason)
        )
        val fromClosingToClosed = StateChange(
            oldState = MessagingClient.State.Closing(expectedCloseCode, expectedCloseReason),
            newState = MessagingClient.State.Closed(expectedCloseCode, expectedCloseReason)
        )
        mockLogger.i(capture(logSlot))
        mockReconnectionHandler.clear()
        mockStateChangedListener(fromConfiguredToClosing)
        mockPlatformSocket.closeSocket(expectedCloseCode, expectedCloseReason)
        mockStateChangedListener(fromClosingToClosed)
        mockLogger.i(capture(logSlot))
        verifyCleanUp()
    }

    protected fun MockKVerificationScope.connectWithFailedConfigureSequence(shouldConfigureAuth: Boolean = false) {
        val configureRequest =
            if (shouldConfigureAuth) Request.configureAuthenticatedRequest() else Request.configureRequest()
        mockStateChangedListener(fromIdleToConnecting)
        mockPlatformSocket.openSocket(any())
        mockStateChangedListener(fromConnectingToConnected)
        mockPlatformSocket.sendMessage(configureRequest)
    }

    protected fun MockKVerificationScope.errorSequence(stateChange: StateChange) {
        mockStateChangedListener(stateChange)
        mockAttachmentHandler.clearAll()
        mockReconnectionHandler.clear()
    }

    protected fun verifyCleanUp() {
        mockMessageStore.invalidateConversationCache()
        mockAttachmentHandler.clearAll()
        mockReconnectionHandler.clear()
        mockCustomAttributesStore.onSessionClosed()
    }
}
