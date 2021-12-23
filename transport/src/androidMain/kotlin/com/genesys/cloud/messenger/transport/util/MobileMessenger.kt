package com.genesys.cloud.messenger.transport.util

import android.content.Context
import com.genesys.cloud.messenger.transport.AttachmentHandler
import com.genesys.cloud.messenger.transport.Configuration
import com.genesys.cloud.messenger.transport.DeploymentConfigUseCase
import com.genesys.cloud.messenger.transport.JwtHandler
import com.genesys.cloud.messenger.transport.MessageStore
import com.genesys.cloud.messenger.transport.MessagingClient
import com.genesys.cloud.messenger.transport.MessagingClientImpl
import com.genesys.cloud.messenger.transport.WebMessagingApi
import com.genesys.cloud.messenger.transport.shyrka.receive.DeploymentConfig
import com.genesys.cloud.messenger.transport.util.logs.Log
import com.genesys.cloud.messenger.transport.util.logs.LogTag

object MobileMessenger {

    fun createMessagingClient(
        context: Context,
        configuration: Configuration,
        listener: MessageListener
    ): MessagingClient {
        val log = Log(configuration.logging, LogTag.MESSAGING_CLIENT)
        val token =
            TokenStoreImpl(context = context, configuration.tokenStoreKey).token
        val api = WebMessagingApi(configuration)
        val webSocket = PlatformSocket(log.withTag(LogTag.WEBSOCKET), configuration, 300000)
        val messageStore = MessageStore(
            MessageDispatcher(listener = listener),
            token,
            log.withTag(LogTag.MESSAGE_STORE)
        )
        val attachmentHandler = AttachmentHandler(
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
        )
    }

    /**
     *  Fetch deployment configuration based on deployment id and domain.
     *
     * @param deploymentId the ID of the deployment containing configuration and routing information.
     * @param domain the regional base domain address for a Genesys Cloud Web Messaging service. For example, "mypurecloud.com".
     * @param logging indicates if logging should be enabled. False by default.
     *
     * @throws Exception
     */
    @Throws(Exception::class)
    suspend fun fetchDeploymentConfig(
        domain: String,
        deploymentId: String,
        logging: Boolean = false
    ): DeploymentConfig {
        return DeploymentConfigUseCase(logging).fetch(domain, deploymentId)
    }
}
