package transport.core.messagingclient

import com.genesys.cloud.messenger.transport.auth.AuthHandler
import com.genesys.cloud.messenger.transport.core.AttachmentHandler
import com.genesys.cloud.messenger.transport.core.ButtonResponse
import com.genesys.cloud.messenger.transport.core.CustomAttributesStoreImpl
import com.genesys.cloud.messenger.transport.core.Empty
import com.genesys.cloud.messenger.transport.core.HistoryHandler
import com.genesys.cloud.messenger.transport.core.JwtHandler
import com.genesys.cloud.messenger.transport.core.Message
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
import com.genesys.cloud.messenger.transport.push.DEFAULT_PUSH_CONFIG
import com.genesys.cloud.messenger.transport.push.PushService
import com.genesys.cloud.messenger.transport.shyrka.receive.DeploymentConfig
import com.genesys.cloud.messenger.transport.shyrka.receive.createDeploymentConfigForTesting
import com.genesys.cloud.messenger.transport.shyrka.send.DeleteAttachmentRequest
import com.genesys.cloud.messenger.transport.shyrka.send.OnAttachmentRequest
import com.genesys.cloud.messenger.transport.shyrka.send.OnMessageRequest
import com.genesys.cloud.messenger.transport.shyrka.send.StructuredMessage
import com.genesys.cloud.messenger.transport.shyrka.send.TextMessage
import com.genesys.cloud.messenger.transport.util.DefaultVault
import com.genesys.cloud.messenger.transport.util.Platform
import com.genesys.cloud.messenger.transport.util.logs.Log
import com.genesys.cloud.messenger.transport.util.logs.LogTag
import com.genesys.cloud.messenger.transport.utility.AuthTest
import com.genesys.cloud.messenger.transport.utility.QuickReplyTestValues
import com.genesys.cloud.messenger.transport.utility.TestValues
import com.genesys.cloud.messenger.transport.utility.TestValues.TOKEN_KEY
import io.mockk.MockKVerificationScope
import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.invoke
import io.mockk.just
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.slot
import io.mockk.spyk
import transport.util.Request
import transport.util.Response
import transport.util.fromConnectedToConfigured
import transport.util.fromConnectedToReadOnly
import transport.util.fromConnectingToConnected
import transport.util.fromIdleToConnecting
import kotlin.reflect.KProperty0
import kotlin.test.AfterTest

open class BaseMessagingClientTest {
    private var testToken = Request.token
    internal val slot = slot<PlatformSocketListener>()
    protected val mockStateChangedListener: (StateChange) -> Unit = spyk()
    internal val mockMessageStore: MessageStore =
        mockk(relaxed = true) {
            every { prepareMessage(any(), any(), any()) } returns
                OnMessageRequest(
                    token = testToken,
                    message = TextMessage("Hello world!")
                )
            every { prepareMessageWith(any(), any(), null) } returns
                OnMessageRequest(
                    token = testToken,
                    message =
                        TextMessage(
                            text = "",
                            content =
                                listOf(
                                    Message.Content(
                                        contentType = Message.Content.Type.ButtonResponse,
                                        buttonResponse = QuickReplyTestValues.buttonResponse_a,
                                    )
                                ),
                        ),
                )
            every { preparePostbackMessage(any(), any(), any()) } returns
                OnMessageRequest(
                    token = Request.token,
                    message =
                        StructuredMessage(
                            text = "Postback button text",
                            metadata = mapOf("customMessageId" to "card-123"),
                            content =
                                listOf(
                                    Message.Content(
                                        contentType = Message.Content.Type.ButtonResponse,
                                        buttonResponse =
                                            ButtonResponse(
                                                text = "Postback button text",
                                                payload = "some_payload_value",
                                                type = "Postback"
                                            )
                                    )
                                )
                        )
                )
        }
    internal val mockAttachmentHandler: AttachmentHandler =
        mockk(relaxed = true) {
            every {
                prepare(
                    any(),
                    any(),
                    any(),
                    any(),
                    any()
                )
            } returns
                OnAttachmentRequest(
                    token = Request.token,
                    attachmentId = "88888888-8888-8888-8888-888888888888",
                    fileName = "test_attachment.png",
                    fileType = "image/png",
                    errorsAsJson = true,
                )

            every { detach(any(), any()) } returns
                DeleteAttachmentRequest(
                    token = Request.token,
                    attachmentId = "88888888-8888-8888-8888-888888888888"
                )
            every { fileAttachmentProfile } returns null
        }
    internal val mockPlatformSocket: PlatformSocket =
        mockk {
            every { openSocket(capture(slot)) } answers {
                slot.captured.onOpen()
            }
            every { closeSocket(any(), any()) } answers {
                slot.captured.onClosed(1000, "The user has closed the connection.")
            }
            every { sendMessage(any()) } answers {
                slot.captured.onMessage("")
            }
            every { sendMessage(match { Request.isConfigureRequest(it) }) } answers {
                slot.captured.onMessage(Response.configureSuccess())
            }
            every { sendMessage(match { Request.isConfigureAuthenticatedRequest(it) }) } answers {
                slot.captured.onMessage(Response.configureSuccess())
            }
        }

    private val mockWebMessagingApi: WebMessagingApi =
        mockk {
            coEvery {
                getMessages(
                    any(),
                    any(),
                    any()
                )
            } returns Result.Success(TestWebMessagingApiResponses.testMessageEntityList)
        }

    internal val mockReconnectionHandler: ReconnectionHandlerImpl =
        mockk(relaxed = true) {
            every { shouldReconnect } returns false
        }

    internal val mockEventHandler: EventHandler = mockk(relaxed = true)
    protected val mockTimestampFunction: () -> Long =
        spyk<() -> Long>().also {
            every { it.invoke() } answers { Platform().epochMillis() }
        }
    protected val mockShowUserTypingIndicatorFunction: () -> Boolean =
        spyk<() -> Boolean>().also {
            every { it.invoke() } returns true
        }
    protected val mockDeploymentConfig =
        mockk<KProperty0<DeploymentConfig?>> {
            every { get() } returns createDeploymentConfigForTesting()
        }
    internal val userTypingProvider =
        UserTypingProvider(
            log = mockk(relaxed = true),
            showUserTypingEnabled = mockShowUserTypingIndicatorFunction,
            getCurrentTimestamp = mockTimestampFunction,
        )
    internal val mockAuthHandler: AuthHandler =
        mockk(relaxed = true) {
            every { jwt } returns AuthTest.JWT_TOKEN
            every { refreshToken(captureLambda<(Result<Any>) -> Unit>()) } answers {
                lambda<(Result<Any>) -> Unit>().invoke(Result.Success(Empty()))
            }
        }

    internal val mockCustomAttributesStore: CustomAttributesStoreImpl =
        mockk(relaxed = true) {
            val dummyCustomAttributes = mutableMapOf("A" to "B")
            every { get() } returns dummyCustomAttributes
            every { getCustomAttributesToSend() } returns dummyCustomAttributes
            every { add(eq(emptyMap())) } returns true.also { dummyCustomAttributes.clear() }
        }

    internal val mockVault: DefaultVault =
        mockk {
            every { fetch(TOKEN_KEY) } returns testToken
            every { token } returns testToken
            every { remove(TOKEN_KEY) } answers { testToken = TestValues.SECONDARY_TOKEN }
            every { keys } returns TestValues.vaultKeys
            justRun { wasAuthenticated = any() }
            every { pushConfig } returns DEFAULT_PUSH_CONFIG
        }
    internal val mockJwtHandler: JwtHandler = mockk(relaxed = true)
    internal val mockHistoryHandler: HistoryHandler = mockk(relaxed = true)

    internal val mockLogger: Log = mockk(relaxed = true)
    internal val logSlot = mutableListOf<() -> String>()
    internal val mockPushService: PushService =
        mockk {
            coEvery { synchronize(any(), any()) } just Runs
        }

    internal val subject =
        MessagingClientImpl(
            log = mockLogger,
            configuration = TestValues.configuration,
            webSocket = mockPlatformSocket,
            api = mockWebMessagingApi,
            token = testToken,
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
            pushService = mockPushService,
            historyHandler = mockHistoryHandler,
        ).also {
            it.stateChangedListener = mockStateChangedListener
        }

    @AfterTest
    fun after() = clearAllMocks()

    protected fun MockKVerificationScope.fromIdleToConnectedSequence() {
        mockLogger.withTag(LogTag.STATE_MACHINE)
        mockLogger.withTag(LogTag.WEBSOCKET)
        mockLogger.i(capture(logSlot))
        mockStateChangedListener(fromIdleToConnecting)
        mockPlatformSocket.openSocket(any())
        mockStateChangedListener(fromConnectingToConnected)
    }

    protected fun MockKVerificationScope.connectSequence(shouldConfigureAuth: Boolean = false) {
        fromIdleToConnectedSequence()
        configureSequence(shouldConfigureAuth)
        mockStateChangedListener(fromConnectedToConfigured)
    }

    protected fun MockKVerificationScope.configureSequence(shouldConfigureAuth: Boolean = false, startNew: Boolean = false) {
        mockLogger.i(capture(logSlot))
        if (shouldConfigureAuth) {
            mockAuthHandler.jwt // check if jwt is valid
            mockAuthHandler.jwt // use jwt for request
        }
        mockPlatformSocket.sendMessage(
            match {
                if (shouldConfigureAuth) {
                    Request.isConfigureAuthenticatedRequest(it, startNew)
                } else {
                    Request.isConfigureRequest(it, startNew)
                }
            }
        )
        mockVault.wasAuthenticated = shouldConfigureAuth
        mockAttachmentHandler.fileAttachmentProfile = any()
        mockReconnectionHandler.clear()
        mockJwtHandler.clear()
        mockCustomAttributesStore.maxCustomDataBytes = TestValues.MAX_CUSTOM_DATA_BYTES
    }

    protected fun MockKVerificationScope.connectToReadOnlySequence() {
        fromIdleToConnectedSequence()
        mockLogger.i(capture(logSlot))
        mockPlatformSocket.sendMessage(match { Request.isConfigureRequest(it) })
        mockAttachmentHandler.fileAttachmentProfile = any()
        mockReconnectionHandler.clear()
        mockJwtHandler.clear()
        mockCustomAttributesStore.maxCustomDataBytes = TestValues.MAX_CUSTOM_DATA_BYTES
        mockStateChangedListener(fromConnectedToReadOnly)
    }

    protected fun MockKVerificationScope.disconnectSequence(
        expectedCloseCode: Int = 1000,
        expectedCloseReason: String = "The user has closed the connection.",
    ) {
        val fromConfiguredToClosing =
            StateChange(
                oldState = MessagingClient.State.Configured(connected = true, newSession = true),
                newState = MessagingClient.State.Closing(expectedCloseCode, expectedCloseReason)
            )
        val fromClosingToClosed =
            StateChange(
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
        mockStateChangedListener(fromIdleToConnecting)
        mockPlatformSocket.openSocket(any())
        mockStateChangedListener(fromConnectingToConnected)
        mockPlatformSocket.sendMessage(
            match {
                if (shouldConfigureAuth) {
                    Request.isConfigureAuthenticatedRequest(it)
                } else {
                    Request.isConfigureRequest(it)
                }
            }
        )
    }

    protected fun errorSequence(stateChange: StateChange) {
        this.mockStateChangedListener(stateChange)
        mockReconnectionHandler.clear()
        mockJwtHandler.clear()
    }

    protected fun MockKVerificationScope.invalidateSessionTokenSequence() {
        mockLogger.i(capture(logSlot))
        mockVault.keys
        mockVault.remove(TOKEN_KEY)
        mockVault.token
    }

    protected fun verifyCleanUp() {
        mockMessageStore.invalidateConversationCache()
        mockMessageStore.clear()
        mockAttachmentHandler.clearAll()
        mockReconnectionHandler.clear()
        mockJwtHandler.clear()
        mockCustomAttributesStore.onSessionClosed()
    }
}
