package com.genesys.cloud.messenger.transport.core

import com.genesys.cloud.messenger.transport.network.*
import com.genesys.cloud.messenger.transport.network.DeploymentConfigUseCase
import com.genesys.cloud.messenger.transport.network.PlatformSocket
import com.genesys.cloud.messenger.transport.network.ReconnectionHandlerImpl
import com.genesys.cloud.messenger.transport.network.WebMessagingApi
import com.genesys.cloud.messenger.transport.shyrka.receive.DeploymentConfig
import com.genesys.cloud.messenger.transport.util.DefaultTokenStore
import com.genesys.cloud.messenger.transport.util.TokenStore
import com.genesys.cloud.messenger.transport.util.logs.Log
import com.genesys.cloud.messenger.transport.util.logs.LogTag

/**
 * The entry point to the services provided by the transport SDK.
 */
class MessengerTransport(private val configuration: Configuration, private val tokenStore: TokenStore) {

    constructor(configuration: Configuration, storeKey: String) : this(configuration, DefaultTokenStore(storeKey))

    constructor(configuration: Configuration) : this(configuration, "com.genesys.cloud.messenger")

    /**
     * Creates an instance of [MessagingClient] based on the provided configuration.
     */
    fun createMessagingClient(): MessagingClient {
        val log = Log(configuration.logging, LogTag.MESSAGING_CLIENT)
        val api = WebMessagingApi(configuration)
        val webSocket = PlatformSocket(log.withTag(LogTag.WEBSOCKET), configuration.webSocketUrl, DEFAULT_PING_INTERVAL_IN_SECONDS)
        val messageStore = MessageStore(tokenStore.token, log.withTag(LogTag.MESSAGE_STORE))
        val attachmentHandler = AttachmentHandlerImpl(
            api,
            tokenStore.token,
            log.withTag(LogTag.ATTACHMENT_HANDLER),
            messageStore.updateAttachmentStateWith,
        )
        return MessagingClientImpl(
            api = api,
            log = log,
            webSocket = webSocket,
            token = tokenStore.token,
            configuration = configuration,
            jwtHandler = JwtHandler(webSocket, tokenStore.token),
            attachmentHandler = attachmentHandler,
            messageStore = messageStore,
            reconnectionHandler = ReconnectionHandlerImpl(configuration.reconnectionTimeoutInSeconds, log.withTag(
                LogTag.RECONNECTION_HANDLER))
        )
    }

    /**
     *  Fetch deployment configuration based on deployment id and domain.
     *
     * @throws Exception
     */
    @Throws(Exception::class)
    suspend fun fetchDeploymentConfig(): DeploymentConfig {
        return DeploymentConfigUseCase(configuration.logging, configuration.deploymentConfigUrl.toString()).fetch()
    }
}