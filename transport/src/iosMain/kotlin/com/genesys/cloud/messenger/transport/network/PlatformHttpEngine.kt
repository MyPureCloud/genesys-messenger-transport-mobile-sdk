package com.genesys.cloud.messenger.transport.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin

internal actual fun createPlatformHttpClient(logging: Boolean): HttpClient =
    HttpClient(Darwin) {
        applyDefaultConfig(logging)
    }