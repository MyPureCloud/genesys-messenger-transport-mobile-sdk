package com.genesys.cloud.messenger.transport.network

import com.genesys.cloud.messenger.transport.shyrka.WebMessagingJson
import com.genesys.cloud.messenger.transport.util.logs.Log
import com.genesys.cloud.messenger.transport.util.logs.LogTag
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpCallValidator
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json

private const val TIMEOUT_IN_MS = 30000L
private const val MAX_RETRIES_ON_SERVER_ERRORS = 3
private const val DELAY_BETWEEN_RETRIES_IN_MILLISECONDS = 5000L
private val RETRYABLE_STATUS_CODES = setOf(
    HttpStatusCode.InternalServerError,
    HttpStatusCode.BadGateway,
    HttpStatusCode.ServiceUnavailable,
    HttpStatusCode.GatewayTimeout,
)

internal fun defaultHttpClient(logging: Boolean = false): HttpClient = HttpClient {
    if (logging) {
        install(Logging) {
            this.logger = Log(logging, LogTag.HTTP_CLIENT).ktorLogger
            level = LogLevel.INFO
        }
    }
    install(ContentNegotiation) {
        json(WebMessagingJson.json)
    }
    install(HttpCallValidator)
    install(HttpTimeout) {
        socketTimeoutMillis = TIMEOUT_IN_MS
        connectTimeoutMillis = TIMEOUT_IN_MS
    }
    install(HttpRequestRetry)
}

internal fun HttpRequestBuilder.retryOnServerErrors() {
    retry {
        maxRetries = MAX_RETRIES_ON_SERVER_ERRORS
        constantDelay(DELAY_BETWEEN_RETRIES_IN_MILLISECONDS)
        retryIf { _, response -> response.status in RETRYABLE_STATUS_CODES }
    }
}
