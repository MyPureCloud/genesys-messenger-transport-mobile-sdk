package com.genesys.cloud.messenger.transport.core

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
import com.genesys.cloud.messenger.transport.shyrka.WebMessagingJson
import com.genesys.cloud.messenger.transport.shyrka.receive.AttachmentDeletedResponse
import com.genesys.cloud.messenger.transport.shyrka.receive.ConnectionClosed
import com.genesys.cloud.messenger.transport.shyrka.receive.ConnectionClosedEvent
import com.genesys.cloud.messenger.transport.shyrka.receive.DeploymentConfig
import com.genesys.cloud.messenger.transport.shyrka.receive.ErrorEvent
import com.genesys.cloud.messenger.transport.shyrka.receive.GenerateUrlError
import com.genesys.cloud.messenger.transport.shyrka.receive.HealthCheckEvent
import com.genesys.cloud.messenger.transport.shyrka.receive.JwtResponse
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
) : MessagingClient {

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
    override fun disconnect() {
        log.i { "disconnect()" }
        val code = SocketCloseCode.NORMAL_CLOSURE.value
        val reason = "The user has closed the connection."
        reconnectionHandler.clear()
        stateMachine.onClosing(code, reason)
        webSocket.closeSocket(code, reason)
    }

    private fun configureSession() {
        log.i { "configureSession(token = $token)" }
        val request = ConfigureSessionRequest(
            token = token,
            deploymentId = configuration.deploymentId,
            journeyContext = JourneyContext(
                JourneyCustomer(token, "cookie"),
                JourneyCustomerSession("", "web")
            )
        )
        val encodedJson = WebMessagingJson.json.encodeToString(request)
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
        stateMachine.checkIfConfigured()
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

    @Throws(IllegalStateException::class)
    private fun sendAutoStart() {
        WebMessagingJson.json.encodeToString(AutoStartRequest(token)).let {
            log.i { "sendAutoStart()" }
            send(it)
        }
    }

    private fun handleError(code: ErrorCode, message: String? = null) {
        when (code) {
            is ErrorCode.SessionHasExpired,
            is ErrorCode.SessionNotFound,
            ->
                stateMachine.onError(code, message)
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
                if (stateMachine.isConnected()) {
                    stateMachine.onError(code, message)
                } else {
                    eventHandler.onEvent(ErrorEvent(errorCode = code, message = message))
                }
            }
            is ErrorCode.WebsocketError -> handleWebSocketError(ErrorCode.WebsocketError)
            else -> log.w { "Unhandled ErrorCode: $code with optional message: $message" }
        }
    }

    private fun handleWebSocketError(errorCode: ErrorCode) {
        if (stateMachine.isClosed()) return
        invalidateConversationCache()
        when (errorCode) {
            is ErrorCode.WebsocketError -> {
                if (reconnectionHandler.shouldReconnect) {
                    stateMachine.onReconnect()
                    reconnectionHandler.reconnect { connect() }
                } else {
                    stateMachine.onError(errorCode, ErrorMessage.FailedToReconnect)
                    attachmentHandler.clearAll()
                    reconnectionHandler.clear()
                }
            }
            is ErrorCode.WebsocketAccessDenied -> {
                stateMachine.onError(errorCode, CorrectiveAction.Forbidden.message)
                attachmentHandler.clearAll()
                reconnectionHandler.clear()
            }
            is ErrorCode.NetworkDisabled -> {
                stateMachine.onError(errorCode, ErrorMessage.InternetConnectionIsOffline)
                attachmentHandler.clearAll()
                reconnectionHandler.clear()
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
                        eventHandler.onEvent(it)
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
                            stateMachine.onSessionConfigured(connected, newSession, readOnly)
                            if (newSession && deploymentConfig.isAutostartEnabled()) {
                                sendAutoStart()
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
            invalidateConversationCache()
            userTypingProvider.clear()
            healthCheckProvider.clear()
            attachmentHandler.clearAll()
        }
    }
}

private fun KProperty0<DeploymentConfig?>.isAutostartEnabled(): Boolean =
    this.get()?.messenger?.apps?.conversations?.autoStart?.enabled == true

private fun KProperty0<DeploymentConfig?>.isShowUserTypingEnabled(): Boolean =
    this.get()?.messenger?.apps?.conversations?.showUserTypingIndicator == true
