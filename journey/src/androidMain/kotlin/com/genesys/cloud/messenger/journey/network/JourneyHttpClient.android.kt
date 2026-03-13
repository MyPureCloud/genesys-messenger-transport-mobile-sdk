package com.genesys.cloud.messenger.journey.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp

internal actual fun createJourneyPlatformHttpClient(logging: Boolean): HttpClient =
    HttpClient(OkHttp) {
        applyJourneyConfig(logging)
    }
