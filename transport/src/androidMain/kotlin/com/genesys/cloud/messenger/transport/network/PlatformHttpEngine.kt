package com.genesys.cloud.messenger.transport.network

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp

internal actual fun createPlatformHttpEngine(): HttpClientEngine = OkHttp.create {
    config {
        protocols(PLATFORM_HTTP_ENGINE_PROTOCOLS)
    }
}