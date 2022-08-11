package com.genesys.cloud.messenger.transport.core

import io.ktor.http.URLBuilder
import io.ktor.http.Url

/**
 * @param deploymentId the ID of the Genesys Cloud Messenger deployment.
 * @param domain the regional base domain address for a Genesys Cloud Web Messaging service. For example, "mypurecloud.com".
 * @param tokenStoreKey the key to access local storage.
 * @param logging indicates if logging should be enabled.
 * @param reconnectionTimeoutInSeconds period of time during which Transport will try to reconnect to the web socket in case of connectivity lost.
 */
data class Configuration(
    val deploymentId: String,
    private val domain: String,
    val tokenStoreKey: String,
    val logging: Boolean = false,
    val reconnectionTimeoutInSeconds: Long = 60 * 5,
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
