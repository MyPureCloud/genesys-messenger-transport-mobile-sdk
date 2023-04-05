package com.genesys.cloud.messenger.transport.core

import com.genesys.cloud.messenger.transport.core.MessagingClient.State
import com.genesys.cloud.messenger.transport.core.events.Event
import com.genesys.cloud.messenger.transport.core.events.EventHandler
import com.genesys.cloud.messenger.transport.core.events.EventHandlerImpl
import com.genesys.cloud.messenger.transport.core.events.HealthCheckProvider
import com.genesys.cloud.messenger.transport.core.events.UserTypingProvider
import com.genesys.cloud.messenger.transport.model.AuthJwt
import com.genesys.cloud.messenger.transport.network.FetchJwtUseCase
import com.genesys.cloud.messenger.transport.network.LogoutUseCase
import com.genesys.cloud.messenger.transport.network.PlatformSocket
import com.genesys.cloud.messenger.transport.network.PlatformSocketListener
import com.genesys.cloud.messenger.transport.network.ReconnectionHandler
import com.genesys.cloud.messenger.transport.network.SocketCloseCode
import com.genesys.cloud.messenger.transport.network.WebMessagingApi
import com.genesys.cloud.messenger.transport.shyrka.WebMessagingJson
import com.genesys.cloud.messenger.transport.shyrka.receive.AttachmentDeletedResponse
import com.genesys.cloud.messenger.transport.shyrka.receive.ConnectionClosed
import com.genesys.cloud.messenger.transport.shyrka.receive.ConnectionClosedEvent
import com.genesys.cloud.messenger.transport.shyrka.receive.Conversations
import com.genesys.cloud.messenger.transport.shyrka.receive.DeploymentConfig
import com.genesys.cloud.messenger.transport.shyrka.receive.ErrorEvent
import com.genesys.cloud.messenger.transport.shyrka.receive.GenerateUrlError
import com.genesys.cloud.messenger.transport.shyrka.receive.HealthCheckEvent
import com.genesys.cloud.messenger.transport.shyrka.receive.JwtResponse
import com.genesys.cloud.messenger.transport.shyrka.receive.Logout
import com.genesys.cloud.messenger.transport.shyrka.receive.LogoutEvent
import com.genesys.cloud.messenger.transport.shyrka.receive.PresenceEvent
import com.genesys.cloud.messenger.transport.shyrka.receive.PresignedUrlResponse
import com.genesys.cloud.messenger.transport.shyrka.receive.SessionExpiredEvent
import com.genesys.cloud.messenger.transport.shyrka.receive.SessionResponse
import com.genesys.cloud.messenger.transport.shyrka.receive.StructuredMessage
import com.genesys.cloud.messenger.transport.shyrka.receive.StructuredMessageEvent
import com.genesys.cloud.messenger.transport.shyrka.receive.TooManyRequestsErrorMessage
import com.genesys.cloud.messenger.transport.shyrka.receive.UploadFailureEvent
import com.genesys.cloud.messenger.transport.shyrka.receive.UploadSuccessEvent
import com.genesys.cloud.messenger.transport.shyrka.receive.isHealthCheckResponse
import com.genesys.cloud.messenger.transport.shyrka.receive.isOutbound
import com.genesys.cloud.messenger.transport.shyrka.send.AutoStartRequest
import com.genesys.cloud.messenger.transport.shyrka.send.CloseSessionRequest
import com.genesys.cloud.messenger.transport.shyrka.send.ConfigureAuthenticatedSessionRequest
import com.genesys.cloud.messenger.transport.shyrka.send.ConfigureSessionRequest
import com.genesys.cloud.messenger.transport.shyrka.send.JourneyContext
import com.genesys.cloud.messenger.transport.shyrka.send.JourneyCustomer
import com.genesys.cloud.messenger.transport.shyrka.send.JourneyCustomerSession
import com.genesys.cloud.messenger.transport.util.Platform
import com.genesys.cloud.messenger.transport.util.extensions.toMessage
import com.genesys.cloud.messenger.transport.util.extensions.toMessageList
import com.genesys.cloud.messenger.transport.util.logs.Log
import com.genesys.cloud.messenger.transport.util.logs.LogTag
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlin.reflect.KProperty0

internal class MessagingClientImpl(
    private val api: WebMessagingApi,
    private val webSocket: PlatformSocket,
    private val configuration: Configuration,
    private val log: Log,
    private val jwtHandler: JwtHandler,
    private val token: String,
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
    private val fetchJwtUseCase: FetchJwtUseCase = FetchJwtUseCase(
        configuration.logging,
        configuration.deploymentId,
        configuration.jwtAuthUrl,
    ),
    private val logoutUseCase: LogoutUseCase = LogoutUseCase(configuration.logoutUrl),
) : MessagingClient {

    private var authJwt: AuthJwt? = null
    private var isStartingANewSession = false

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

    @Throws(IllegalStateException::class)
    override fun connect() {
        log.i { "connect()" }
        stateMachine.onConnect()
        webSocket.openSocket(socketListener)
    }

    @Throws(IllegalStateException::class)
    override fun connectAuthenticatedSession(jwt: AuthJwt) {
        authJwt = jwt
        log.i { "connect()" }
        stateMachine.onConnect()
        webSocket.openSocket(socketListener)
    }

    @Throws(IllegalStateException::class)
    override fun startNewChat() {
        stateMachine.checkIfCanStartANewChat()
        isStartingANewSession = true
        closeAllConnectionsForTheSession()
    }

    @Throws(IllegalStateException::class)
    override fun disconnect() {
        log.i { "disconnect()" }
        val code = SocketCloseCode.NORMAL_CLOSURE.value
        val reason = "The user has closed the connection."
        reconnectionHandler.clear()
        stateMachine.onClosing(code, reason)
        webSocket.closeSocket(code, reason)
    }

    private fun configureSession(startNew: Boolean = false) {
        log.i { "configureSession(token = $token, startNew: $startNew)" }
        val encodedJson = if (authJwt != null) {
            encodeAuthenticatedConfigureSessionRequest(startNew)
        } else {
            encodeAnonymousConfigureSessionRequest(startNew)
        }
        webSocket.sendMessage(encodedJson)
    }

    @Throws(IllegalStateException::class)
    override fun sendMessage(text: String, customAttributes: Map<String, String>) {
        stateMachine.checkIfConfigured()
        log.i { "sendMessage(text = $text, customAttributes = $customAttributes)" }
        val request = messageStore.prepareMessage(text, customAttributes)
        attachmentHandler.onSending()
        val encodedJson = WebMessagingJson.json.encodeToString(request)
        send(encodedJson)
    }

    @Throws(IllegalStateException::class)
    override fun sendHealthCheck() {
        healthCheckProvider.encodeRequest(token)?.let {
            log.i { "sendHealthCheck()" }
            send(it)
        }
    }

    override fun attach(
        byteArray: ByteArray,
        fileName: String,
        uploadProgress: ((Float) -> Unit)?,
    ): String {
        log.i { "attach(fileName = $fileName)" }
        val request = attachmentHandler.prepare(
            Platform().randomUUID(),
            byteArray,
            fileName,
            uploadProgress,
        )
        val encodedJson = WebMessagingJson.json.encodeToString(request)
        send(encodedJson)
        return request.attachmentId
    }

    @Throws(IllegalStateException::class)
    override fun detach(attachmentId: String) {
        log.i { "detach(attachmentId = $attachmentId)" }
        attachmentHandler.detach(attachmentId)?.let {
            val encodedJson = WebMessagingJson.json.encodeToString(it)
            send(encodedJson)
        }
    }

    @Throws(IllegalStateException::class)
    private fun send(message: String) {
        stateMachine.checkIfConfigured()
        log.i { "Will send message" }
        webSocket.sendMessage(message)
    }

    @Throws(Exception::class)
    override suspend fun fetchNextPage() {
        stateMachine.checkIfConfiguredOrReadOnly()
        if (messageStore.startOfConversation) {
            log.i { "All history has been fetched." }
            messageStore.updateMessageHistory(emptyList(), conversation.size)
            return
        }
        log.i { "fetching history for page index = ${messageStore.nextPage}" }
        jwtHandler.withJwt { jwt -> api.getMessages(jwt, messageStore.nextPage) }
            .also {
                messageStore.updateMessageHistory(
                    it.toMessageList(),
                    it.total,
                )
            }
    }

    @Throws(Exception::class)
    override suspend fun logoutFromAuthenticatedSession() {
        val authJwt = authJwt ?: run {
            log.w { "Logout from anonymous session is not supported." }
            return
        }
        logoutUseCase.logout(authJwt.jwt)
    }

    override fun invalidateConversationCache() {
        log.i { "Clear conversation history." }
        messageStore.invalidateConversationCache()
    }

    @Throws(IllegalStateException::class)
    override fun indicateTyping() {
        userTypingProvider.encodeRequest(token)?.let {
            log.i { "indicateTyping()" }
            send(it)
        }
    }

    @Throws(Exception::class)
    override suspend fun fetchAuthJwt(
        authCode: String,
        redirectUri: String,
        codeVerifier: String?,
    ): AuthJwt {
        return fetchJwtUseCase.fetch(authCode, redirectUri, codeVerifier)
    }

    @Throws(IllegalStateException::class)
    private fun sendAutoStart() {
        WebMessagingJson.json.encodeToString(AutoStartRequest(token)).let {
            log.i { "sendAutoStart()" }
            send(it)
        }
    }

    /**
     * This function executes [CloseSessionRequest]. This request will trigger closure of
     * all opened connections on all other devices that share the same session token,
     * by sending them a [ConnectionClosedEvent].
     * After successful closure an event [SessionResponse] with `connected=false` will be received,
     * indicating that new chat should be configured.
     *
     */
    private fun closeAllConnectionsForTheSession() {
        WebMessagingJson.json.encodeToString(
            CloseSessionRequest(
                token = token,
                closeAllConnections = true
            )
        ).also {
            log.i { "closeSession()" }
            webSocket.sendMessage(it)
        }
    }

    private fun handleError(code: ErrorCode, message: String? = null) {
        when (code) {
            is ErrorCode.SessionHasExpired,
            is ErrorCode.SessionNotFound,
            -> transitionToStateError(code, message)
            is ErrorCode.MessageTooLong,
            is ErrorCode.RequestRateTooHigh,
            is ErrorCode.CustomAttributeSizeTooLarge,
            -> {
                messageStore.onMessageError(code, message)
                attachmentHandler.onMessageError(code, message)
            }
            is ErrorCode.ClientResponseError,
            is ErrorCode.ServerResponseError,
            is ErrorCode.RedirectResponseError,
            -> {
                if (stateMachine.isConnected() || stateMachine.isReconnecting() || isStartingANewSession) {
                    transitionToStateError(code, message)
                } else {
                    eventHandler.onEvent(ErrorEvent(errorCode = code, message = message))
                }
            }
            is ErrorCode.WebsocketError -> handleWebSocketError(ErrorCode.WebsocketError)
            else -> log.w { "Unhandled ErrorCode: $code with optional message: $message" }
        }
    }

    private fun handleWebSocketError(errorCode: ErrorCode) {
        if (stateMachine.isInactive()) return
        invalidateConversationCache()
        when (errorCode) {
            is ErrorCode.WebsocketError -> {
                if (reconnectionHandler.shouldReconnect) {
                    stateMachine.onReconnect()
                    reconnectionHandler.reconnect { connect() }
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
            else -> log.w { "Unhandled WebSocket errorCode. ErrorCode: $errorCode" }
        }
    }

    private fun handleStructuredMessage(structuredMessage: StructuredMessage) {
        when (structuredMessage.type) {
            StructuredMessage.Type.Text -> {
                with(structuredMessage.toMessage()) {
                    messageStore.update(this)
                    attachmentHandler.onSent(this.attachments)
                    userTypingProvider.clear()
                }
            }

            StructuredMessage.Type.Event -> {
                if (structuredMessage.isOutbound()) {
                    structuredMessage.events.forEach {
                        if (it.isDisconnectionEvent()) {
                            if (deploymentConfig.isConversationDisconnectEnabled()) {
                                eventHandler.onEvent(it)
                                // Prefer readOnly value provided by Shyrka and as fallback use DeploymentConfig.
                                if (structuredMessage.metadata.containsKey("readOnly")) {
                                    if (structuredMessage.metadata["readOnly"].toBoolean()) stateMachine.onReadOnly()
                                } else {
                                    if (deploymentConfig.isReadOnly()) stateMachine.onReadOnly()
                                }
                            }
                        } else {
                            eventHandler.onEvent(it)
                        }
                    }
                } else {
                    structuredMessage.events.forEach {
                        if (it.eventType == StructuredMessageEvent.Type.Presence) {
                            eventHandler.onEvent(it)
                        }
                    }
                }
            }
        }
    }

    private fun cleanUp() {
        invalidateConversationCache()
        userTypingProvider.clear()
        healthCheckProvider.clear()
        attachmentHandler.clearAll()
        reconnectionHandler.clear()
        authJwt = null
    }

    private fun transitionToStateError(errorCode: ErrorCode, errorMessage: String?) {
        stateMachine.onError(errorCode, errorMessage)
        attachmentHandler.clearAll()
        reconnectionHandler.clear()
        authJwt = null
    }

    private fun encodeAnonymousConfigureSessionRequest(startNew: Boolean) = WebMessagingJson.json.encodeToString(
        ConfigureSessionRequest(
            token = token,
            deploymentId = configuration.deploymentId,
            startNew = startNew,
            journeyContext = JourneyContext(
                JourneyCustomer(token, "cookie"),
                JourneyCustomerSession("", "web")
            )
        )
    )

    private fun encodeAuthenticatedConfigureSessionRequest(startNew: Boolean) = WebMessagingJson.json.encodeToString(
        ConfigureAuthenticatedSessionRequest(
            token = token,
            deploymentId = configuration.deploymentId,
            startNew = startNew,
            journeyContext = JourneyContext(
                JourneyCustomer(token, "cookie"),
                JourneyCustomerSession("", "web")
            ),
            data = ConfigureAuthenticatedSessionRequest.Data(authJwt?.jwt ?: "")
        )
    )

    private val socketListener = SocketListener(
        log = log.withTag(LogTag.WEBSOCKET)
    )

    private inner class SocketListener(
        private val log: Log,
    ) : PlatformSocketListener {

        override fun onOpen() {
            log.i { "onOpen()" }
            stateMachine.onConnectionOpened()
            configureSession()
        }

        override fun onFailure(t: Throwable, errorCode: ErrorCode) {
            log.e(throwable = t) { "onFailure(message: ${t.message})" }
            handleWebSocketError(errorCode)
        }

        override fun onMessage(text: String) {
            log.i { "onMessage(text = $text)" }
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
                    is SessionResponse -> {
                        decoded.body.run {
                            reconnectionHandler.clear()
                            if (readOnly) {
                                stateMachine.onReadOnly()
                                if (!connected && isStartingANewSession) {
                                    cleanUp()
                                    configureSession(startNew = true)
                                } else {
                                    // Normally should not happen.
                                    log.w {
                                        "Unexpected SessionResponse configuration: connected: $connected, readOnly: $readOnly, isStartingANewSession: $isStartingANewSession"
                                    }
                                }
                            } else {
                                isStartingANewSession = false
                                stateMachine.onSessionConfigured(connected, newSession)
                                if (newSession && deploymentConfig.isAutostartEnabled()) {
                                    sendAutoStart()
                                }
                            }
                        }
                    }
                    is JwtResponse ->
                        jwtHandler.jwtResponse = decoded.body
                    is PresignedUrlResponse ->
                        attachmentHandler.upload(decoded.body)
                    is UploadSuccessEvent ->
                        attachmentHandler.onUploadSuccess(decoded.body)
                    is StructuredMessage -> {
                        if (decoded.body.isHealthCheckResponse()) {
                            eventHandler.onEvent(HealthCheckEvent())
                        } else {
                            handleStructuredMessage(decoded.body)
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
                        disconnect()
                        eventHandler.onEvent(ConnectionClosed())
                    }
                    is LogoutEvent -> { eventHandler.onEvent(Logout()) }
                    else -> { log.i { "Unhandled message received from Shyrka: $decoded " } }
                }
            } catch (exception: SerializationException) {
                log.e(throwable = exception) { "Failed to deserialize message" }
            } catch (exception: IllegalArgumentException) {
                log.e(throwable = exception) { "Message decoded as null" }
            }
        }

        override fun onClosing(code: Int, reason: String) {
            log.i { "onClosing(code = $code, reason = $reason)" }
            stateMachine.onClosing(code, reason)
        }

        override fun onClosed(code: Int, reason: String) {
            log.i { "onClosed(code = $code, reason = $reason)" }
            stateMachine.onClosed(code, reason)
            cleanUp()
        }
    }
}

private fun StructuredMessageEvent.isDisconnectionEvent(): Boolean =
    this is PresenceEvent && presence.type == PresenceEvent.Presence.Type.Disconnect

private fun KProperty0<DeploymentConfig?>.isConversationDisconnectEnabled(): Boolean =
    this.get()?.messenger?.apps?.conversations?.conversationDisconnect?.enabled == true

private fun KProperty0<DeploymentConfig?>.isReadOnly(): Boolean =
    this.get()?.messenger?.apps?.conversations?.conversationDisconnect?.type == Conversations.ConversationDisconnect.Type.ReadOnly

private fun KProperty0<DeploymentConfig?>.isAutostartEnabled(): Boolean =
    this.get()?.messenger?.apps?.conversations?.autoStart?.enabled == true

private fun KProperty0<DeploymentConfig?>.isShowUserTypingEnabled(): Boolean =
    this.get()?.messenger?.apps?.conversations?.showUserTypingIndicator == true
