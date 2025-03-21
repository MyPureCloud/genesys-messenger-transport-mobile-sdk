package com.genesys.cloud.messenger.transport.network

import com.genesys.cloud.messenger.transport.shyrka.WebMessagingJson
import com.genesys.cloud.messenger.transport.util.logs.Log
import com.genesys.cloud.messenger.transport.util.logs.LogTag
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpCallValidator
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.retry
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json

private const val TIMEOUT_IN_MS = 30000L
private const val MAX_RETRIES_ON_INTERNAL_SERVER_ERRORS = 3
private const val DELAY_BETWEEN_RETRIES_IN_MILLISECONDS = 5000L

internal fun defaultHttpClient(logging: Boolean = false): HttpClient = HttpClient {
    if (logging) {
        install(Logging) {
            this.logger = Log(logging, LogTag.HTTP_CLIENT).ktorLogger
            level = LogLevel.ALL
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

internal fun HttpRequestBuilder.retryOnInternalServerErrors() {
    retry {
        maxRetries = MAX_RETRIES_ON_INTERNAL_SERVER_ERRORS
        constantDelay(DELAY_BETWEEN_RETRIES_IN_MILLISECONDS)
        retryIf { _, response ->
            response.status == HttpStatusCode.InternalServerError
        }
    }
}
