package com.genesys.cloud.messenger.transport.network

import com.genesys.cloud.messenger.transport.util.logs.Log
import com.genesys.cloud.messenger.transport.util.logs.LogTag
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpCallValidator
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

private const val TIMEOUT_IN_MS = 30000L

internal fun defaultHttpClient(logging: Boolean = false): HttpClient = HttpClient {
    if (logging) {
        install(Logging) {
            this.logger = Log(logging, LogTag.HTTP_CLIENT).ktorLogger
            level = LogLevel.ALL
        }
    }
    install(ContentNegotiation) {
        json(
            Json {
                ignoreUnknownKeys = true
                useAlternativeNames = false
            }
        )

    }
    install(HttpCallValidator)
    install(HttpTimeout) {
        socketTimeoutMillis = TIMEOUT_IN_MS
        connectTimeoutMillis = TIMEOUT_IN_MS
    }
}
