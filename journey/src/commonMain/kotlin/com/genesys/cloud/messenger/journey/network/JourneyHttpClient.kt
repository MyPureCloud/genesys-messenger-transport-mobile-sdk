package com.genesys.cloud.messenger.journey.network

import com.genesys.cloud.messenger.journey.util.logs.Log
import com.genesys.cloud.messenger.journey.util.logs.LogTag
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpCallValidator
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

private const val TIMEOUT_IN_MS = 30000L
private const val MAX_RETRIES_ON_SERVER_ERRORS = 3
private const val EXPONENTIAL_DELAY_BASE = 3.0

private val RETRYABLE_STATUS_CODES = setOf(
    HttpStatusCode.InternalServerError,
    HttpStatusCode.BadGateway,
    HttpStatusCode.ServiceUnavailable,
    HttpStatusCode.GatewayTimeout,
)

internal val journeyJson = Json {
    encodeDefaults = false
    ignoreUnknownKeys = true
    isLenient = true
}

internal fun HttpClientConfig<*>.applyJourneyConfig(logging: Boolean) {
    if (logging) {
        install(Logging) {
            logger = Log(logging, LogTag.HTTP_CLIENT).ktorLogger
            level = LogLevel.INFO
        }
    }
    install(ContentNegotiation) { json(journeyJson) }
    install(HttpCallValidator)
    install(HttpTimeout) {
        socketTimeoutMillis = TIMEOUT_IN_MS
        connectTimeoutMillis = TIMEOUT_IN_MS
    }
    install(HttpRequestRetry) {
        maxRetries = MAX_RETRIES_ON_SERVER_ERRORS
        retryIf { _, response ->
            response.status in RETRYABLE_STATUS_CODES
        }
        exponentialDelay(base = EXPONENTIAL_DELAY_BASE)
    }
}

internal fun defaultJourneyHttpClient(
    logging: Boolean = false,
    engine: HttpClientEngine? = null,
): HttpClient =
    if (engine != null) {
        HttpClient(engine) { applyJourneyConfig(logging) }
    } else {
        createJourneyPlatformHttpClient(logging)
    }

internal expect fun createJourneyPlatformHttpClient(logging: Boolean): HttpClient
