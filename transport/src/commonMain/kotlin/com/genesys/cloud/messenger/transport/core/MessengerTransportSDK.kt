package com.genesys.cloud.messenger.transport.core

import com.genesys.cloud.messenger.transport.config.BuildKonfig
import com.genesys.cloud.messenger.transport.network.DEFAULT_PING_INTERVAL_IN_SECONDS
import com.genesys.cloud.messenger.transport.network.DeploymentConfigUseCase
import com.genesys.cloud.messenger.transport.network.PlatformSocket
import com.genesys.cloud.messenger.transport.network.ReconnectionHandlerImpl
import com.genesys.cloud.messenger.transport.network.WebMessagingApi
import com.genesys.cloud.messenger.transport.network.defaultHttpClient
import com.genesys.cloud.messenger.transport.shyrka.receive.DeploymentConfig
import com.genesys.cloud.messenger.transport.util.DefaultVault
import com.genesys.cloud.messenger.transport.util.EncryptedVault
import com.genesys.cloud.messenger.transport.util.TokenStore
import com.genesys.cloud.messenger.transport.util.Vault
import com.genesys.cloud.messenger.transport.util.logs.Log
import com.genesys.cloud.messenger.transport.util.logs.LogMessages
import com.genesys.cloud.messenger.transport.util.logs.LogTag
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * The entry point to the services provided by the transport SDK.
 *
 * @param vault the storage mechanism for managing session-related data.
 */
class MessengerTransportSDK(
    private val configuration: Configuration,
    @Deprecated("Use Vault instead.") private val tokenStore: TokenStore?,
    val vault: Vault,
) {
    private var deploymentConfig: DeploymentConfig? = null

    companion object {
        /**
         * The SDK version.
         */
        val sdkVersion = BuildKonfig.sdkVersion

        /**
         * Creates a vault instance based on the configuration.
         *
         * @return A vault instance based on the configuration's encryptedVault property.
         */
        private fun getVault(configuration: Configuration): Vault {
            return if (configuration.encryptedVault) {
                EncryptedVault()
            } else {
                DefaultVault()
            }
        }
    }

    constructor(configuration: Configuration) : this(
        configuration,
        null,
        getVault(configuration),
    )

    @Deprecated("Use Vault instead.")
    constructor(configuration: Configuration, tokenStore: TokenStore) : this(
        configuration = configuration,
        tokenStore = tokenStore,
        vault = DefaultVault(),
    )

    constructor(configuration: Configuration, vault: Vault) : this(
        configuration = configuration,
        vault = vault,
        tokenStore = null,
    )

    /**
     * Creates an instance of [MessagingClient] based on the provided configuration.
     */
    fun createMessagingClient(): MessagingClient {
        val log = Log(configuration.logging, LogTag.MESSAGING_CLIENT)
        if (deploymentConfig == null) {
            CoroutineScope(Dispatchers.Main + SupervisorJob()).launch {
                try {
                    fetchDeploymentConfig()
                } catch (t: Throwable) {
                    log.w { LogMessages.failedFetchDeploymentConfig(t) }
                }
            }
        }
        val api = WebMessagingApi(configuration)
        val webSocket = PlatformSocket(
            log.withTag(LogTag.WEBSOCKET),
            configuration.webSocketUrl,
            DEFAULT_PING_INTERVAL_IN_SECONDS,
        )
        // Support old TokenStore. If TokenStore not present fallback to the Vault.
        val token = tokenStore?.token ?: vault.token
        val messageStore = MessageStore(log.withTag(LogTag.MESSAGE_STORE))
        val attachmentHandler = AttachmentHandlerImpl(
            api,
            log.withTag(LogTag.ATTACHMENT_HANDLER),
            messageStore.updateAttachmentStateWith,
        )

        return MessagingClientImpl(
            api = api,
            log = log,
            webSocket = webSocket,
            token = token,
            vault = vault,
            configuration = configuration,
            jwtHandler = JwtHandler(webSocket),
            attachmentHandler = attachmentHandler,
            messageStore = messageStore,
            reconnectionHandler = ReconnectionHandlerImpl(
                configuration.reconnectionTimeoutInSeconds,
                log.withTag(LogTag.RECONNECTION_HANDLER),
            ),
            deploymentConfig = this::deploymentConfig,
        )
    }

    /**
     *  Fetch deployment configuration based on `Configuration`.
     *
     * @throws Exception
     */
    @Throws(Exception::class)
    suspend fun fetchDeploymentConfig(): DeploymentConfig {
        return DeploymentConfigUseCase(
            configuration.deploymentConfigUrl.toString(),
            defaultHttpClient(configuration.logging)
        ).fetch().also {
            deploymentConfig = it
        }
    }
}
