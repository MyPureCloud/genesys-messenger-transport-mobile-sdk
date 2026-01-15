package com.genesys.cloud.messenger.transport.network

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import okhttp3.Protocol

internal actual fun createPlatformHttpEngine(): HttpClientEngine = OkHttp.create {
    config {
        protocols(listOf(Protocol.HTTP_2, Protocol.HTTP_1_1))
    }
}