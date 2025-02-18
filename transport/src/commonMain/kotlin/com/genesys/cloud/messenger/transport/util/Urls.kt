package com.genesys.cloud.messenger.transport.util

import com.genesys.cloud.messenger.transport.core.MessengerTransportSDK
import io.ktor.http.URLBuilder
import io.ktor.http.Url
import io.ktor.http.path

internal class Urls(val domain: String, val deploymentId: String) {

    internal val webSocketUrl: Url by lazy {
        URLBuilder("wss://webmessaging.$domain")
            .apply {
                path("v1")
                parameters.append("deploymentId", deploymentId)
                parameters.append("application", "TransportSDK-${MessengerTransportSDK.sdkVersion}")
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

    internal val registerDeviceToken: (String, String) -> Url = { deploymentId, token ->
        URLBuilder("https://api.$domain").apply {
            path("/api/v2/webmessaging/deployments/$deploymentId/pushdevices/$token")
        }.build()
    }
}
