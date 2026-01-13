package com.genesys.cloud.messenger.transport.core

import io.ktor.http.URLBuilder
import io.ktor.http.Url
import io.ktor.http.path

/**
 * @param deploymentId the ID of the Genesys Cloud Messenger deployment.
 * @param domain the regional base domain address for a Genesys Cloud Web Messaging service. For example, "mypurecloud.com".
 * @param logging indicates if logging should be enabled.
 * @param reconnectionTimeoutInSeconds period of time during which Transport will try to reconnect to the web socket in case of connectivity lost.
 * @param autoRefreshTokenWhenExpired indicates if Transport should auto refresh auth token if it was expired.
 * @param forceTLSv13 indicates if TLS 1.3 should be forced for WebSocket connections (iOS only). Default is true.
 */
data class Configuration(
    val deploymentId: String,
    val domain: String,
    val logging: Boolean = false,
    val reconnectionTimeoutInSeconds: Long = 60 * 5,
    val autoRefreshTokenWhenExpired: Boolean = true,
    val encryptedVault: Boolean = false,
    val forceTLSv13: Boolean = true
) {

    /**
     * Secondary constructor to avoid breaking changes on iOS platform.
     *
     * @param deploymentId the ID of the Genesys Cloud Messenger deployment.
     * @param domain the regional base domain address for a Genesys Cloud Web Messaging service. For example, "mypurecloud.com".
     * @param logging indicates if logging should be enabled.
     * @param reconnectionTimeoutInSeconds period of time during which Transport will try to reconnect to the web socket in case of connectivity lost.
     */
    constructor(
        deploymentId: String,
        domain: String,
        logging: Boolean,
        reconnectionTimeoutInSeconds: Long,
    ) : this(
        deploymentId = deploymentId,
        domain = domain,
        logging = logging,
        reconnectionTimeoutInSeconds = reconnectionTimeoutInSeconds,
        autoRefreshTokenWhenExpired = true,
        encryptedVault = false,
        forceTLSv13 = true
    )

    internal var application: String = "TransportSDK-${MessengerTransportSDK.sdkVersion}"

    internal val webSocketUrl: Url by lazy {
        URLBuilder("wss://webmessaging.$domain")
            .apply {
                path("v1")
                parameters.append("deploymentId", deploymentId)
                parameters.append("application", application)
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

    internal val jwtAuthUrl: Url by lazy {
        URLBuilder("https://api.$domain").apply {
            path("api/v2/webdeployments/token/oauthcodegrantjwtexchange")
        }.build()
    }

    internal val logoutUrl: Url by lazy {
        URLBuilder("https://api.$domain").apply {
            path("api/v2/webdeployments/token/revoke")
        }.build()
    }

    internal val refreshAuthTokenUrl: Url by lazy {
        URLBuilder("https://api.$domain").apply {
            path("api/v2/webdeployments/token/refresh")
        }.build()
    }
}
