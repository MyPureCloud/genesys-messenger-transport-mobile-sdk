package com.genesys.cloud.messenger.transport.core

import com.genesys.cloud.messenger.transport.network.DEFAULT_PING_INTERVAL_IN_SECONDS
import com.genesys.cloud.messenger.transport.network.DeploymentConfigUseCase
import com.genesys.cloud.messenger.transport.network.PlatformSocket
import com.genesys.cloud.messenger.transport.network.ReconnectionHandlerImpl
import com.genesys.cloud.messenger.transport.network.WebMessagingApi
import com.genesys.cloud.messenger.transport.shyrka.receive.DeploymentConfig
import com.genesys.cloud.messenger.transport.util.DefaultTokenStore
import com.genesys.cloud.messenger.transport.util.TokenStore
import com.genesys.cloud.messenger.transport.util.logs.Log
import com.genesys.cloud.messenger.transport.util.logs.LogTag

private const val TOKEN_STORE_KEY = "com.genesys.cloud.messenger"

/**
 * The entry point to the services provided by the transport SDK.
 */
class MessengerTransport(private val configuration: Configuration, private val tokenStore: TokenStore) {

    constructor(configuration: Configuration) : this(configuration, DefaultTokenStore(TOKEN_STORE_KEY))

    /**
     * Creates an instance of [MessagingClient] based on the provided configuration.
     */
    fun createMessagingClient(): MessagingClient {
        val log = Log(configuration.logging, LogTag.MESSAGING_CLIENT)
        val api = WebMessagingApi(configuration)
        val webSocket = PlatformSocket(log.withTag(LogTag.WEBSOCKET), configuration.webSocketUrl, DEFAULT_PING_INTERVAL_IN_SECONDS)
        val token = tokenStore.token
        val messageStore = MessageStore(token, log.withTag(LogTag.MESSAGE_STORE))
        val attachmentHandler = AttachmentHandlerImpl(
            api,
            token,
            log.withTag(LogTag.ATTACHMENT_HANDLER),
            messageStore.updateAttachmentStateWith,
        )
        return MessagingClientImpl(
            api = api,
            log = log,
            webSocket = webSocket,
            token = token,
            configuration = configuration,
            jwtHandler = JwtHandler(webSocket, token),
            attachmentHandler = attachmentHandler,
            messageStore = messageStore,
            reconnectionHandler = ReconnectionHandlerImpl(
                configuration.reconnectionTimeoutInSeconds,
                log.withTag(LogTag.RECONNECTION_HANDLER),
            )
        )
    }

    /**
     *  Fetch deployment configuration based on `Configuration`.
     *
     * @throws Exception
     */
    @Throws(Exception::class)
    suspend fun fetchDeploymentConfig(): DeploymentConfig {
        return DeploymentConfigUseCase(configuration.logging, configuration.deploymentConfigUrl.toString()).fetch()
    }
}
