package com.genesys.cloud.messenger.transport.util

import com.genesys.cloud.messenger.transport.AttachmentHandler
import com.genesys.cloud.messenger.transport.Configuration
import com.genesys.cloud.messenger.transport.JwtHandler
import com.genesys.cloud.messenger.transport.MessageStore
import com.genesys.cloud.messenger.transport.MessagingClient
import com.genesys.cloud.messenger.transport.MessagingClientImpl
import com.genesys.cloud.messenger.transport.WebMessagingApi
import com.genesys.cloud.messenger.transport.util.logs.Log
import com.genesys.cloud.messenger.transport.util.logs.LogTag

object MobileMessenger {

    fun createMessagingClient(
        configuration: Configuration,
        listener: MessageListener
    ): MessagingClient {
        val log = Log(configuration.logging, LogTag.MESSAGING_CLIENT)
        val api = WebMessagingApi(log.withTag(LogTag.API), configuration)
        val webSocket = PlatformSocket(log.withTag(LogTag.WEBSOCKET), configuration, 300000)
        val token = TokenStoreImpl("configuration.tokenStoreKey23", log.withTag(LogTag.TOKEN_STORE)).token
        val messageStore =
            MessageStore(MessageDispatcher(listener), token, log.withTag(LogTag.MESSAGE_STORE))
        val attachmentHandler =
            AttachmentHandler(
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
}
