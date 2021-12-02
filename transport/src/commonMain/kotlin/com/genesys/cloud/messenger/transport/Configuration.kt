package com.genesys.cloud.messenger.transport

import com.genesys.cloud.messenger.transport.util.DEFAULT_MAX_ATTEMPTS
import io.ktor.http.URLBuilder
import io.ktor.http.Url

/**
 * @param deploymentId the ID of the deployment containing configuration and routing information.
 * @param domain the regional base domain address for a Genesys Cloud Web Messaging service. For example, "mypurecloud.com".
 * @param tokenStoreKey the key to access local storage.
 * @param logging is a flag to indicate if logs should be enabled.
 * @param maxReconnectAttempts is the maximum amount of reconnection attempts, Messenger will make before entering [MessagingClient.State.Error]
 */
data class Configuration(
    val deploymentId: String,
    private val domain: String,
    val tokenStoreKey: String,
    val logging: Boolean = false,
    val maxReconnectAttempts: Int = DEFAULT_MAX_ATTEMPTS,
) {

    internal val webSocketUrl: Url by lazy {
        URLBuilder("wss://webmessaging.$domain")
            .apply {
                path("v1")
                parameters.append("deploymentId", deploymentId)
            }
            .build()
    }

    internal val apiBaseUrl: Url by lazy {
        URLBuilder("https://api.$domain").build()
    }

    internal val deploymentConfigUrl: Url by lazy {
        URLBuilder("https://api-cdn.$domain").apply {
            path("webdeployments/v1/deployments/$deploymentId/config.json")
        }.build()
    }
}
