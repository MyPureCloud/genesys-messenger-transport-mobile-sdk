package com.genesys.cloud.messenger.journey.network

import io.ktor.http.URLBuilder
import io.ktor.http.Url
import io.ktor.http.path

private const val BASE_JOURNEY_PATH = "api/v2/journey"

internal class JourneyUrls(
    val domain: String,
    val deploymentId: String,
) {
    internal val deploymentConfigUrl: Url by lazy {
        URLBuilder("https://api-cdn.$domain")
            .apply {
                path("webdeployments/v1/deployments/$deploymentId/config.json")
            }.build()
    }

    internal val appEventsUrl: Url by lazy {
        URLBuilder("https://api.$domain")
            .apply {
                path("$BASE_JOURNEY_PATH/deployments/$deploymentId/appevents")
            }.build()
    }
}
