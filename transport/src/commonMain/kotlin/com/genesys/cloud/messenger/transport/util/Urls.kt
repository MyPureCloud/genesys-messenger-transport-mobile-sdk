package com.genesys.cloud.messenger.transport.util

import com.genesys.cloud.messenger.transport.core.MessengerTransportSDK
import io.ktor.http.ContentType
import io.ktor.http.URLBuilder
import io.ktor.http.Url
import io.ktor.http.path

private const val BASE_WEBDEPLOYMENTS_PATH = "api/v2/webdeployments"
private const val BASE_WEBMESSAGING_PATH = "api/v2/webmessaging"

internal class Urls(val domain: String, val deploymentId: String, val application: String) {

    internal val webSocketUrl: Url by lazy {
        URLBuilder("wss://webmessaging.$domain")
            .apply {
                path("v1")
                parameters.append("deploymentId", deploymentId)
                parameters.append("application", application)
            }
            .build()
    }

    private val apiBaseUrl: Url by lazy {
        URLBuilder("https://api.$domain").build()
    }

    internal val deploymentConfigUrl: Url by lazy {
        URLBuilder("https://api-cdn.$domain").apply {
            path("webdeployments/v1/deployments/$deploymentId/config.json")
        }.build()
    }

    internal val history: Url by lazy {
        URLBuilder(apiBaseUrl).apply { path("$BASE_WEBMESSAGING_PATH/messages") }.build()
    }

    internal val jwtAuthUrl: Url by lazy {
        URLBuilder(apiBaseUrl).apply { path("$BASE_WEBDEPLOYMENTS_PATH/token/oauthcodegrantjwtexchange") }.build()
    }

    internal val logoutUrl: Url by lazy {
        URLBuilder(apiBaseUrl).apply { path("$BASE_WEBDEPLOYMENTS_PATH/token/revoke") }.build()
    }

    internal val refreshAuthTokenUrl: Url by lazy {
        URLBuilder(apiBaseUrl).apply { path("$BASE_WEBDEPLOYMENTS_PATH/token/refresh") }.build()
    }

    internal val deviceTokenUrl: (String, String) -> Url = { deploymentId, token ->
        URLBuilder(apiBaseUrl).apply { path("$BASE_WEBMESSAGING_PATH/deployments/$deploymentId/pushdevices/$token") }
            .build()
    }
}
