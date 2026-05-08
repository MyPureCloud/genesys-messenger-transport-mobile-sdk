package com.genesys.cloud.messenger.transport.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp

internal actual fun createPlatformHttpClient(logging: Boolean): HttpClient =
    HttpClient(OkHttp) {
        engine {
            config {
                protocols(PLATFORM_HTTP_ENGINE_PROTOCOLS)
            }
        }
        applyDefaultConfig(logging)
    }