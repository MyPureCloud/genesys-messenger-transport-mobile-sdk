package com.genesys.cloud.messenger.transport.core

import com.genesys.cloud.messenger.transport.auth.AuthHandler
import com.genesys.cloud.messenger.transport.auth.AuthHandlerImpl
import com.genesys.cloud.messenger.transport.auth.NO_JWT
import com.genesys.cloud.messenger.transport.core.MessagingClient.State
import com.genesys.cloud.messenger.transport.core.events.Event
import com.genesys.cloud.messenger.transport.core.events.EventHandler
import com.genesys.cloud.messenger.transport.core.events.EventHandlerImpl
import com.genesys.cloud.messenger.transport.core.events.HealthCheckProvider
import com.genesys.cloud.messenger.transport.core.events.UserTypingProvider
import com.genesys.cloud.messenger.transport.network.PlatformSocket
import com.genesys.cloud.messenger.transport.network.PlatformSocketListener
import com.genesys.cloud.messenger.transport.network.ReconnectionHandler
import com.genesys.cloud.messenger.transport.network.SocketCloseCode
import com.genesys.cloud.messenger.transport.network.WebMessagingApi
import com.genesys.cloud.messenger.transport.push.DeviceTokenException
import com.genesys.cloud.messenger.transport.push.PushService
import com.genesys.cloud.messenger.transport.push.PushServiceImpl
import com.genesys.cloud.messenger.transport.shyrka.WebMessagingJson
import com.genesys.cloud.messenger.transport.shyrka.receive.AttachmentDeletedResponse
import com.genesys.cloud.messenger.transport.shyrka.receive.ConnectionClosedEvent
import com.genesys.cloud.messenger.transport.shyrka.receive.DeploymentConfig
import com.genesys.cloud.messenger.transport.shyrka.receive.GenerateUrlError
import com.genesys.cloud.messenger.transport.shyrka.receive.JwtResponse
import com.genesys.cloud.messenger.transport.shyrka.receive.LogoutEvent
import com.genesys.cloud.messenger.transport.shyrka.receive.PresignedUrlResponse
import com.genesys.cloud.messenger.transport.shyrka.receive.SessionClearedEvent
import com.genesys.cloud.messenger.transport.shyrka.receive.SessionExpiredEvent
import com.genesys.cloud.messenger.transport.shyrka.receive.SessionResponse
import com.genesys.cloud.messenger.transport.shyrka.receive.StructuredMessage
import com.genesys.cloud.messenger.transport.shyrka.receive.TooManyRequestsErrorMessage
import com.genesys.cloud.messenger.transport.shyrka.receive.UploadFailureEvent
import com.genesys.cloud.messenger.transport.shyrka.receive.UploadSuccessEvent
import com.genesys.cloud.messenger.transport.shyrka.receive.toTransportConnectionClosedReason
import com.genesys.cloud.messenger.transport.shyrka.send.AutoStartRequest
import com.genesys.cloud.messenger.transport.shyrka.send.Channel
import com.genesys.cloud.messenger.transport.shyrka.send.ClearConversationRequest
import com.genesys.cloud.messenger.transport.shyrka.send.CloseSessionRequest
import com.genesys.cloud.messenger.transport.shyrka.send.ConfigureAuthenticatedSessionRequest
import com.genesys.cloud.messenger.transport.shyrka.send.ConfigureSessionRequest
import com.genesys.cloud.messenger.transport.shyrka.send.GetAttachmentRequest
import com.genesys.cloud.messenger.transport.shyrka.send.JourneyContext
import com.genesys.cloud.messenger.transport.shyrka.send.JourneyCustomer
import com.genesys.cloud.messenger.transport.shyrka.send.JourneyCustomerSession
import com.genesys.cloud.messenger.transport.util.Platform
import com.genesys.cloud.messenger.transport.util.UNKNOWN
import com.genesys.cloud.messenger.transport.util.Vault
import com.genesys.cloud.messenger.transport.util.extensions.isHealthCheckResponseId
import com.genesys.cloud.messenger.transport.util.extensions.isOutbound
import com.genesys.cloud.messenger.transport.util.extensions.isRefreshUrl
import com.genesys.cloud.messenger.transport.util.extensions.sanitizeText
import com.genesys.cloud.messenger.transport.util.extensions.toFileAttachmentProfile
import com.genesys.cloud.messenger.transport.util.extensions.toMessage
import com.genesys.cloud.messenger.transport.util.logs.Log
import com.genesys.cloud.messenger.transport.util.logs.LogMessages
import com.genesys.cloud.messenger.transport.util.logs.LogTag
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.SerializationException
import kotlin.reflect.KProperty0

private const val MAX_RECONFIGURE_ATTEMPTS = 3

internal class MessagingClientImpl(
    private val vault: Vault,
    private val api: WebMessagingApi,
    private val webSocket: PlatformSocket,
    private val configuration: Configuration,
    private val log: Log,
    private val jwtHandler: JwtHandler,
    private var token: String,
    private val deploymentConfig: KProperty0<DeploymentConfig?>,
    private val attachmentHandler: AttachmentHandler,
    private val messageStore: MessageStore,
    private val reconnectionHandler: ReconnectionHandler,
    private val stateMachine: StateMachine = StateMachineImpl(log.withTag(LogTag.STATE_MACHINE)),
    private val eventHandler: EventHandler = EventHandlerImpl(log.withTag(LogTag.EVENT_HANDLER)),
    private val healthCheckProvider: HealthCheckProvider = HealthCheckProvider(log.withTag(LogTag.HEALTH_CHECK_PROVIDER)),
    private val userTypingProvider: UserTypingProvider =
        UserTypingProvider(
            log.withTag(LogTag.TYPING_INDICATOR_PROVIDER),
            { deploymentConfig.isShowUserTypingEnabled() },
        ),
    private val authHandler: AuthHandler =
        AuthHandlerImpl(
            configuration.autoRefreshTokenWhenExpired,
            eventHandler,
            api,
            vault,
            log.withTag(LogTag.AUTH_HANDLER),
            isAuthEnabled = { deploymentConfig.isAuthEnabled(api) }
        ),
    private val internalCustomAttributesStore: CustomAttributesStoreImpl =
        CustomAttributesStoreImpl(
            log.withTag(LogTag.CUSTOM_ATTRIBUTES_STORE),
            eventHandler
        ),
    private val pushService: PushService =
        PushServiceImpl(
            vault = vault,
            api = api,
            log = log.withTag(LogTag.PUSH_SERVICE),
        ),
    private val historyHandler: HistoryHandler =
        HistoryHandlerImpl(
            messageStore,
            api,
            eventHandler,
            log.withTag(LogTag.HISTORY_HANDLER),
            suspend { jwtHandler.withJwt(token) { it } }
        ),
    private val defaultDispatcher: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob()),
) : MessagingClient {
    private var connectAuthenticated = false
    private var isStartingANewSession = false
    private var reconfigureAttempts = 0
    private var sendingAutostart = false
    private var clearingConversation = false

    override val currentState: State
        get() {
            return stateMachine.currentState
        }

    override var stateChangedListener: ((StateChange) -> Unit)? = null
        set(value) {
            stateMachine.stateChangedListener = value
            field = value
        }

    override var messageListener: ((MessageEvent) -> Unit)? = null
        set(value) {
            messageStore.messageListener = value
            field = value
        }

    override var eventListener: ((Event) -> Unit)? = null
        set(value) {
            eventHandler.eventListener = value
            field = value
        }

    override val pendingMessage: Message
        get() = messageStore.pendingMessage

    override val conversation: List<Message>
        get() = messageStore.getConversation()

    override val customAttributesStore: CustomAttributesStore
        get() = internalCustomAttributesStore

    override val fileAttachmentProfile: FileAttachmentProfile?
        get() = attachmentHandler.fileAttachmentProfile

    override val wasAuthenticated: Boolean
        get() = vault.wasAuthenticated

    @Throws(IllegalStateException::class, TransportSDKException::class)
    override fun connect() {
        log.i { LogMessages.CONNECT }
        validateDeploymentConfig()
        connectAuthenticated = false
        stateMachine.onConnect()
        webSocket.openSocket(socketListener)
    }

    @Throws(IllegalStateException::class, TransportSDKException::class)
    override fun connectAuthenticatedSession() {
        log.i { LogMessages.CONNECT_AUTHENTICATED_SESSION }
        validateDeploymentConfig()
        connectAuthenticated = true
        stateMachine.onConnect()
        webSocket.openSocket(socketListener)
    }

    @Throws(TransportSDKException::class)
    private fun validateDeploymentConfig() {
        if (deploymentConfig.get() == null) {
            throw TransportSDKException(
                ErrorCode.MissingDeploymentConfig,
                ErrorMessage.MissingDeploymentConfig
            )
        }
    }

    @Throws(IllegalStateException::class)
    override fun stepUpToAuthenticatedSession() {
        log.i { LogMessages.STEP_UP_TO_AUTHENTICATED_SESSION }
        stateMachine.checkIfConfigured()
        if (connectAuthenticated) return
        connectAuthenticated = true
        configureSession()
    }

    @Throws(IllegalStateException::class)
    override fun startNewChat() {
        stateMachine.checkIfCanStartANewChat()
        isStartingANewSession = true
        closeAllConnectionsForTheSession()
    }

    @Throws(IllegalStateException::class)
    override fun disconnect() {
        log.i { LogMessages.DISCONNECT }
        val code = SocketCloseCode.NORMAL_CLOSURE.value
        val reason = "The user has closed the connection."
        reconnectionHandler.clear()
        stateMachine.onClosing(code, reason)
        webSocket.closeSocket(code, reason)
    }

    private fun configureSession(startNew: Boolean = false) {
        val encodedJson =
            if (connectAuthenticated) {
                log.i { LogMessages.configureAuthenticatedSession(token, startNew) }
                if (authHandler.jwt == NO_JWT) {
                    if (reconfigureAttempts < MAX_RECONFIGURE_ATTEMPTS) {
                        reconfigureAttempts++
                        refreshTokenAndPerform { configureSession(startNew) }
                        return
                    }
                    eventHandler.onEvent(Event.AuthorizationRequired)
                    transitionToStateError(ErrorCode.AuthFailed, ErrorMessage.FailedToConfigureSession)
                    return
                }
                encodeConfigureAuthenticatedSessionRequest(startNew)
            } else {
                log.i { LogMessages.configureSession(token, startNew) }
                encodeConfigureGuestSessionRequest(startNew)
            }
        webSocket.sendMessage(encodedJson)
    }

    @Throws(IllegalStateException::class)
    override fun sendMessage(
        text: String,
        customAttributes: Map<String, String>
    ) {
        stateMachine.checkIfConfigured()
        log.i { LogMessages.sendMessage(text.sanitizeText(), customAttributes) }
        internalCustomAttributesStore.add(customAttributes)
        val channel = prepareCustomAttributesForSending()
        val request = messageStore.prepareMessage(token, text, channel)
        attachmentHandler.onSending()
        val encodedJson = WebMessagingJson.json.encodeToString(request)
        send(encodedJson)
    }

    override fun sendQuickReply(buttonResponse: ButtonResponse) {
        stateMachine.checkIfConfigured()
        log.i { LogMessages.sendQuickReply(buttonResponse) }
        val channel = prepareCustomAttributesForSending()
        val request = messageStore.prepareMessageWith(token, buttonResponse, channel)
        val encodedJson = WebMessagingJson.json.encodeToString(request)
        send(encodedJson)
    }

    override fun sendCardReply(postbackResponse: ButtonResponse) {
        stateMachine.checkIfConfigured()
        log.i { LogMessages.sendCardReply(postbackResponse) }
        val channel = prepareCustomAttributesForSending()
        val request = messageStore.preparePostbackMessage(token, postbackResponse, channel)
        val encodedJson = WebMessagingJson.json.encodeToString(request)
        send(encodedJson)
    }

    @Throws(IllegalStateException::class)
    override fun sendHealthCheck() {
        healthCheckProvider.encodeRequest(token)?.let {
            log.i { LogMessages.SEND_HEALTH_CHECK }
            send(it)
        }
    }

    @Throws(IllegalStateException::class, IllegalArgumentException::class)
    override fun attach(
        byteArray: ByteArray,
        fileName: String,
        uploadProgress: ((Float) -> Unit)?,
    ): String {
        log.i { LogMessages.attach(fileName) }
        val request =
            attachmentHandler.prepare(
                token,
                Platform().randomUUID(),
                byteArray,
                fileName,
                uploadProgress,
            )
        val encodedJson = WebMessagingJson.json.encodeToString(request)
        send(encodedJson)
        return request.attachmentId
    }

    @Throws(IllegalStateException::class, IllegalArgumentException::class)
    override fun detach(attachmentId: String) {
        log.i { LogMessages.detach(attachmentId) }
        attachmentHandler.detach(token, attachmentId)?.let {
            val encodedJson = WebMessagingJson.json.encodeToString(it)
            send(encodedJson)
        }
    }

    @Throws(IllegalStateException::class)
    override fun refreshAttachmentUrl(attachmentId: String) {
        WebMessagingJson.json
            .encodeToString(
                GetAttachmentRequest(
                    token = token,
                    attachmentId = attachmentId
                )
            ).also {
                log.i { "getAttachmentRequest()" }
                send(it)
            }
    }

    @Throws(IllegalStateException::class)
    private fun send(message: String) {
        stateMachine.checkIfConfigured()
        log.i { LogMessages.WILL_SEND_MESSAGE }
        webSocket.sendMessage(message)
    }

    @Throws(Exception::class)
    override suspend fun fetchNextPage() {
        stateMachine.checkIfConfiguredOrReadOnly()
        historyHandler.fetchNextPage()
    }

    override fun logoutFromAuthenticatedSession() {
        authHandler.logout()
    }

    override fun shouldAuthorize(callback: (Boolean) -> Unit) {
        authHandler.shouldAuthorize(callback)
    }

    @Throws(IllegalStateException::class)
    override fun clearConversation() {
        stateMachine.checkIfConfiguredOrReadOnly()
        if (!deploymentConfig.isClearConversationEnabled()) {
            eventHandler.onEvent(
                Event.Error(
                    ErrorCode.ClearConversationFailure,
                    ErrorMessage.FailedToClearConversation,
                    CorrectiveAction.Forbidden
                )
            )
            return
        }
        WebMessagingJson.json.encodeToString(ClearConversationRequest(token)).let {
            log.i { LogMessages.SEND_CLEAR_CONVERSATION }
            webSocket.sendMessage(it)
        }
    }

    override fun invalidateConversationCache() {
        log.i { LogMessages.CLEAR_CONVERSATION_HISTORY }
        messageStore.invalidateConversationCache()
    }

    @Throws(IllegalStateException::class)
    override fun indicateTyping() {
        userTypingProvider.encodeRequest(token)?.let {
            log.i { LogMessages.INDICATE_TYPING }
            send(it)
        }
    }

    override fun authorize(
        authCode: String,
        redirectUri: String,
        codeVerifier: String?
    ) {
        invalidateSessionToken()
        authHandler.authorize(authCode, redirectUri, codeVerifier)
    }

    override fun authorizeImplicit(
        idToken: String,
        nonce: String
    ) {
        invalidateSessionToken()
        authHandler.authorizeImplicit(idToken, nonce)
    }

    @Throws(IllegalStateException::class)
    private fun sendAutoStart() {
        sendingAutostart = true
        val channel = prepareCustomAttributesForSending()
        WebMessagingJson.json.encodeToString(AutoStartRequest(token, channel)).let {
            log.i { LogMessages.SEND_AUTO_START }
            send(it)
        }
    }

    /**
     * This function executes [CloseSessionRequest]. This request will trigger closure of
     * all opened connections on all other devices that share the same session token,
     * by sending them a [ConnectionClosedEvent].
     * After successful closure an event [SessionResponse] with `connected=false` will be received,
     * indicating that new chat should be configured.
     */
    private fun closeAllConnectionsForTheSession() {
        WebMessagingJson.json
            .encodeToString(
                CloseSessionRequest(
                    token = token,
                    closeAllConnections = true
                )
            ).also {
                log.i { LogMessages.CLOSE_SESSION }
                webSocket.sendMessage(it)
            }
    }

    private fun handleSessionResponse(sessionResponse: SessionResponse) =
        sessionResponse.run {
            vault.wasAuthenticated = connectAuthenticated
            attachmentHandler.fileAttachmentProfile = createFileAttachmentProfile(this)
            reconnectionHandler.clear()
            jwtHandler.clear()
            internalCustomAttributesStore.maxCustomDataBytes = this.maxCustomDataBytes
            synchronizePushService()
            if (readOnly) {
                stateMachine.onReadOnly()
                if (!connected && isStartingANewSession) {
                    cleanUp()
                    configureSession(startNew = true)
                }
            } else {
                isStartingANewSession = false
                stateMachine.onSessionConfigured(connected, newSession)
                if (newSession && deploymentConfig.isAutostartEnabled()) {
                    sendAutoStart()
                }
            }
            if (clearedExistingSession) {
                eventHandler.onEvent(Event.ExistingAuthSessionCleared)
            }
        }

    private fun synchronizePushService() {
        if (!deploymentConfig.isPushServiceEnabled()) return
        log.i { LogMessages.SYNCHRONIZE_PUSH_SERVICE_ON_SESSION_CONFIGURE }
        vault.pushConfig.run {
            if (deviceToken != UNKNOWN && pushProvider != null) {
                defaultDispatcher.launch {
                    try {
                        pushService.synchronize(deviceToken, pushProvider)
                    } catch (deviceTokenException: DeviceTokenException) {
                        log.e {
                            LogMessages.failedToSynchronizeDeviceToken(
                                this@run,
                                deviceTokenException.errorCode
                            )
                        }
                    } catch (illegalArgumentException: IllegalArgumentException) {
                        log.e { LogMessages.NO_DEVICE_TOKEN_OR_PUSH_PROVIDER }
                    } catch (cancellationException: CancellationException) {
                        log.w { LogMessages.cancellationExceptionRequestName("pushService.synchronize()") }
                    }
                }
            } else {
                log.i { LogMessages.NO_DEVICE_TOKEN_OR_PUSH_PROVIDER }
            }
        }
    }

    private fun handleError(
        code: ErrorCode,
        message: String? = null
    ) {
        log.i{ "handleError: $code, message: $message"}
        when (code) {
            is ErrorCode.SessionHasExpired,
            is ErrorCode.SessionNotFound,
            -> transitionToStateError(code, message)

            is ErrorCode.MessageTooLong,
            is ErrorCode.RequestRateTooHigh,
            is ErrorCode.CustomAttributeSizeTooLarge,
            -> {
                if (code is ErrorCode.CustomAttributeSizeTooLarge) {
                    internalCustomAttributesStore.onError()
                    if (sendingAutostart) {
                        sendingAutostart = false
                        eventHandler.onEvent(
                            Event.Error(
                                code,
                                message,
                                code.toCorrectiveAction()
                            )
                        )
                    }
                } else {
                    internalCustomAttributesStore.onMessageError()
                }
                messageStore.onMessageError(code, message)
                attachmentHandler.onMessageError(code, message)
            }

            is ErrorCode.ClientResponseError,
            is ErrorCode.ServerResponseError,
            is ErrorCode.RedirectResponseError,
                -> {
                if ((code as? ErrorCode.ClientResponseError)?.value == 403) {
                    message?.run {
                        if (startsWith("Session authentication failed", true)) {
                            eventHandler.onEvent(Event.AuthorizationRequired)
                            // TODO move to the correct state plus remove logs
                            log.i { "implicit auth error message a: $this" }
                            return
                        } else if (startsWith("Try to authenticate again", true)) {
                            eventHandler.onEvent(Event.AuthorizationRequired)
                            // TODO move to the correct state plus remove logs
                            log.i { "implicit auth error message b: $this" }
                            return
                        } else {
                            // TODO remove logs
                            log.i { "implicit auth other error message: $this" }
                        }
                    }
                }
                if (stateMachine.isConnected() || stateMachine.isReconnecting() || isStartingANewSession) {
                    handleConfigureSessionErrorResponse(code, message)
                } else {
                    handleConventionalHttpErrorResponse(code, message)
                }
            }

            is ErrorCode.WebsocketError -> handleWebSocketError(ErrorCode.WebsocketError)
            is ErrorCode.CannotDowngradeToUnauthenticated -> {
                invalidateSessionToken()
                transitionToStateError(code, message)
            }

            else -> log.w { LogMessages.unhandledErrorCode(code, message) }
        }
    }

    private fun invalidateSessionToken() {
        log.i { LogMessages.INVALIDATE_SESSION_TOKEN }
        vault.remove(vault.keys.tokenKey)
        token = vault.token
    }

    private fun refreshTokenAndPerform(action: () -> Unit) {
        authHandler.refreshToken { result ->
            when (result) {
                is Result.Success -> action()
                is Result.Failure -> {
                    eventHandler.onEvent(Event.AuthorizationRequired)
                    transitionToStateError(ErrorCode.AuthFailed, ErrorMessage.FailedToConfigureSession)
                }
            }
        }
    }

    private fun handleWebSocketError(errorCode: ErrorCode) {
        considerForceClose()
        if (stateMachine.isInactive()) return
        invalidateConversationCache()
        when (errorCode) {
            is ErrorCode.WebsocketError -> {
                if (reconnectionHandler.shouldReconnect) {
                    stateMachine.onReconnect()
                    reconnectionHandler.reconnect { if (connectAuthenticated) connectAuthenticatedSession() else connect() }
                } else {
                    transitionToStateError(errorCode, ErrorMessage.FailedToReconnect)
                }
            }

            is ErrorCode.WebsocketAccessDenied -> {
                transitionToStateError(errorCode, CorrectiveAction.Forbidden.message)
            }

            is ErrorCode.NetworkDisabled -> {
                transitionToStateError(errorCode, ErrorMessage.InternetConnectionIsOffline)
            }

            else -> log.w { LogMessages.unhandledWebSocketError(errorCode) }
        }
    }

    private fun handleConfigureSessionErrorResponse(
        code: ErrorCode,
        message: String?
    ) {
        if (connectAuthenticated && code.isUnauthorized() && reconfigureAttempts < MAX_RECONFIGURE_ATTEMPTS) {
            reconfigureAttempts++
            if (stateMachine.isConnected() || stateMachine.isReadOnly()) {
                refreshTokenAndPerform { configureSession(isStartingANewSession) }
            } else {
                refreshTokenAndPerform { connectAuthenticatedSession() }
            }
        } else {
            transitionToStateError(code, message)
        }
    }

    private fun handleConventionalHttpErrorResponse(
        code: ErrorCode,
        message: String?
    ) {
        val errorCode =
            if (message.isClearConversationError()) ErrorCode.ClearConversationFailure else code
        eventHandler.onEvent(
            Event.Error(
                errorCode = errorCode,
                message = message,
                correctiveAction = code.toCorrectiveAction()
            )
        )
    }

    private fun Message.handleAsTextMessage() {
        if (id.isHealthCheckResponseId()) {
            eventHandler.onEvent(Event.HealthChecked)
            return
        }
        messageStore.update(this)
        if (direction == Message.Direction.Inbound) {
            internalCustomAttributesStore.onSent()
            attachmentHandler.onSent(attachments)
            userTypingProvider.clear()
        }
    }

    private fun Message.handleAsEvent(isReadOnly: Boolean) =
        this.run {
            if (isOutbound()) {
                // Every Outbound event should be reported to UI.
                events.forEach {
                    if (it is Event.ConversationDisconnect && isReadOnly) stateMachine.onReadOnly()
                    eventHandler.onEvent(it)
                }
            } else {
                events.forEach {
                    when (it) {
                        is Event.ConversationAutostart -> {
                            sendingAutostart = false
                            internalCustomAttributesStore.onSent()
                            eventHandler.onEvent(it)
                        }

                        is Event.SignedIn -> eventHandler.onEvent(it)
                        else -> {
                            // Do nothing. Autostart and SignedIn are the only Inbound events that should be reported to UI.
                            log.i { LogMessages.ignoreInboundEvent(it) }
                        }
                    }
                }
            }
        }

    private fun Message.handleAsStructuredMessage() {
        when (messageType) {
            Message.Type.QuickReply -> messageStore.update(this)
            Message.Type.Cards -> messageStore.update(this)
            Message.Type.Unknown -> log.w { LogMessages.unsupportedMessageType(messageType) }
            else -> log.w { "Should not happen." }
        }
    }

    private fun cleanUp() {
        invalidateConversationCache()
        messageStore.clear()
        userTypingProvider.clear()
        healthCheckProvider.clear()
        attachmentHandler.clearAll()
        reconnectionHandler.clear()
        jwtHandler.clear()
        reconfigureAttempts = 0
        sendingAutostart = false
        clearingConversation = false
        internalCustomAttributesStore.onSessionClosed()
    }

    private fun transitionToStateError(
        errorCode: ErrorCode,
        errorMessage: String?
    ) {
        stateMachine.onError(errorCode, errorMessage)
        reconnectionHandler.clear()
        jwtHandler.clear()
        reconfigureAttempts = 0
        sendingAutostart = false
        clearingConversation = false
    }

    private fun considerForceClose() {
        if (stateMachine.isClosing()) {
            log.i { LogMessages.FORCE_CLOSE_WEB_SOCKET }
            val closingState = stateMachine.currentState as State.Closing
            socketListener.onClosed(closingState.code, closingState.reason)
        }
    }

    private fun encodeConfigureGuestSessionRequest(startNew: Boolean) =
        WebMessagingJson.json.encodeToString(
            ConfigureSessionRequest(
                token = token,
                deploymentId = configuration.deploymentId,
                startNew = startNew,
                journeyContext =
                    JourneyContext(
                        JourneyCustomer(token, "cookie"),
                        JourneyCustomerSession("", "web")
                    )
            )
        )

    private fun encodeConfigureAuthenticatedSessionRequest(startNew: Boolean) =
        WebMessagingJson.json.encodeToString(
            ConfigureAuthenticatedSessionRequest(
                token = token,
                deploymentId = configuration.deploymentId,
                startNew = startNew,
                journeyContext =
                    JourneyContext(
                        JourneyCustomer(token, "cookie"),
                        JourneyCustomerSession("", "web")
                    ),
                data = ConfigureAuthenticatedSessionRequest.Data(authHandler.jwt)
            )
        )

    private fun prepareCustomAttributesForSending(): Channel? =
        internalCustomAttributesStore.getCustomAttributesToSend().asChannel()?.also {
            internalCustomAttributesStore.onSending()
        }

    private val socketListener =
        SocketListener(
            log = log.withTag(LogTag.WEBSOCKET)
        )

    private inner class SocketListener(
        private val log: Log,
    ) : PlatformSocketListener {
        override fun onOpen() {
            log.i { LogMessages.ON_OPEN }
            stateMachine.onConnectionOpened()
            configureSession()
        }

        override fun onFailure(
            t: Throwable,
            errorCode: ErrorCode
        ) {
            log.e(throwable = t) { LogMessages.onFailure(t) }
            handleWebSocketError(errorCode)
        }

        override fun onMessage(text: String) {
            log.i { LogMessages.onMessage(text) }
            try {
                val decoded = WebMessagingJson.decodeFromString(text)
                when (decoded.body) {
                    is String -> handleError(ErrorCode.mapFrom(decoded.code), decoded.body)
                    is SessionExpiredEvent -> handleError(ErrorCode.SessionHasExpired)
                    is TooManyRequestsErrorMessage -> {
                        handleError(
                            ErrorCode.RequestRateTooHigh,
                            "${decoded.body.errorMessage}. Retry after ${decoded.body.retryAfter} seconds."
                        )
                    }

                    is SessionResponse -> handleSessionResponse(decoded.body)
                    is JwtResponse ->
                        jwtHandler.jwtResponse = decoded.body

                    is PresignedUrlResponse -> {
                        if (decoded.body.isRefreshUrl()) {
                            attachmentHandler.onAttachmentRefreshed(decoded.body)
                        } else {
                            attachmentHandler.upload(decoded.body)
                        }
                    }

                    is UploadSuccessEvent ->
                        attachmentHandler.onUploadSuccess(decoded.body)

                    is StructuredMessage -> {
                        decoded.body.run {
                            val message = this.toMessage(decoded.tracingId)
                            when (type) {
                                StructuredMessage.Type.Text -> message.handleAsTextMessage()
                                StructuredMessage.Type.Event ->
                                    message.handleAsEvent(
                                        metadata["readOnly"].toBoolean()
                                    )

                                StructuredMessage.Type.Structured -> message.handleAsStructuredMessage()
                            }
                        }
                    }

                    is AttachmentDeletedResponse ->
                        attachmentHandler.onDetached(decoded.body.attachmentId)

                    is GenerateUrlError -> {
                        decoded.body.run {
                            attachmentHandler.onError(
                                attachmentId,
                                ErrorCode.mapFrom(errorCode),
                                errorMessage
                            )
                        }
                    }

                    is UploadFailureEvent -> {
                        decoded.body.run {
                            attachmentHandler.onError(
                                attachmentId,
                                ErrorCode.mapFrom(errorCode),
                                errorMessage
                            )
                        }
                    }

                    is ConnectionClosedEvent -> {
                        val reason =
                            decoded.body.reason.toTransportConnectionClosedReason(
                                clearingConversation
                            )
                        if (reason == Event.ConnectionClosed.Reason.UserSignedIn) {
                            invalidateSessionToken()
                            vault.wasAuthenticated = false
                        }
                        eventHandler.onEvent(Event.ConnectionClosed(reason))
                        disconnect()
                    }

                    is LogoutEvent -> {
                        invalidateSessionToken()
                        vault.wasAuthenticated = false
                        authHandler.clear()
                        eventHandler.onEvent(Event.Logout)
                        disconnect()
                    }

                    is SessionClearedEvent -> {
                        clearingConversation = true
                        eventHandler.onEvent(Event.ConversationCleared)
                    }

                    else -> {
                        log.i { LogMessages.unhandledMessage(decoded) }
                    }
                }
            } catch (exception: SerializationException) {
                log.e(throwable = exception) { LogMessages.FAILED_TO_DESERIALIZE }
            } catch (exception: IllegalArgumentException) {
                log.e(throwable = exception) { LogMessages.MESSAGE_DECODED_NULL }
            }
        }

        override fun onClosing(
            code: Int,
            reason: String
        ) {
            log.i { LogMessages.onClosing(code, reason) }
            stateMachine.onClosing(code, reason)
        }

        override fun onClosed(
            code: Int,
            reason: String
        ) {
            log.i { LogMessages.onClosed(code, reason) }
            stateMachine.onClosed(code, reason)
            cleanUp()
        }
    }

    private fun createFileAttachmentProfile(sessionResponse: SessionResponse): FileAttachmentProfile {
        val fileUpload = deploymentConfig.get()?.messenger?.fileUpload
        return fileUpload?.enableAttachments?.let { sessionResponse.toFileAttachmentProfile() }
            ?: fileUpload.toFileAttachmentProfile()
    }
}

/**
 * Checks if the string contains both the words "conversation clear" in exact order (case-insensitive).
 *
 * @return `true` if the string contains "conversation clear", `false` otherwise.
 */
private fun String?.isClearConversationError(): Boolean {
    val regex = Regex("Conversation Clear", RegexOption.IGNORE_CASE)
    return this?.let { regex.containsMatchIn(it) } ?: false
}

private fun KProperty0<DeploymentConfig?>.isAutostartEnabled(): Boolean =
    this
        .get()
        ?.messenger
        ?.apps
        ?.conversations
        ?.autoStart
        ?.enabled == true

private fun KProperty0<DeploymentConfig?>.isShowUserTypingEnabled(): Boolean =
    this
        .get()
        ?.messenger
        ?.apps
        ?.conversations
        ?.showUserTypingIndicator == true

private fun KProperty0<DeploymentConfig?>.isClearConversationEnabled(): Boolean =
    this
        .get()
        ?.messenger
        ?.apps
        ?.conversations
        ?.conversationClear
        ?.enabled == true

internal suspend fun KProperty0<DeploymentConfig?>.isAuthEnabled(api: WebMessagingApi): Boolean {
    val config = this.get()
    return config?.auth?.enabled
        ?: when (val result = api.fetchDeploymentConfig()) {
            is Result.Success -> result.value.auth.enabled
            is Result.Failure -> false
        }
}

private fun KProperty0<DeploymentConfig?>.isPushServiceEnabled(): Boolean =
    this
        .get()
        ?.messenger
        ?.apps
        ?.conversations
        ?.notifications
        ?.enabled == true

private fun Map<String, String>.asChannel(): Channel? {
    return if (this.isNotEmpty()) {
        Channel(Channel.Metadata(this))
    } else {
        null
    }
}
