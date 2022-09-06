package com.genesys.cloud.messenger.transport.core

import com.genesys.cloud.messenger.transport.network.DeploymentConfigUseCase
import com.genesys.cloud.messenger.transport.network.PlatformSocket
import com.genesys.cloud.messenger.transport.network.ReconnectionHandlerImpl
import com.genesys.cloud.messenger.transport.network.WebMessagingApi
import com.genesys.cloud.messenger.transport.shyrka.receive.DeploymentConfig
import com.genesys.cloud.messenger.transport.util.logs.Log
import com.genesys.cloud.messenger.transport.util.logs.LogTag

internal class MessengerTransportImpl(private val configuration: Configuration) {

    fun createMessagingClient(token: String): MessagingClient {
        val log = Log(configuration.logging, LogTag.MESSAGING_CLIENT)
        val api = WebMessagingApi(configuration)
        val webSocket = PlatformSocket(log.withTag(LogTag.WEBSOCKET), configuration.webSocketUrl, 300000)
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
            reconnectionHandler = ReconnectionHandlerImpl(configuration.reconnectionTimeoutInSeconds, log.withTag(LogTag.RECONNECTION_HANDLER))
        )
    }

    suspend fun fetchDeploymentConfig(): DeploymentConfig {
        return DeploymentConfigUseCase(configuration.logging, configuration.deploymentConfigUrl.toString()).fetch()
    }
}