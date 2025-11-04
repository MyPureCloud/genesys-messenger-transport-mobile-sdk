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
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json

private const val TIMEOUT_IN_MS = 30000L
private const val MAX_RETRIES_ON_SERVER_ERRORS = 3
private const val EXPONENTIAL_DELAY_BASE = 3.0

private val RETRYABLE_STATUS_CODES =
    setOf(
        HttpStatusCode.InternalServerError,
        HttpStatusCode.BadGateway,
        HttpStatusCode.ServiceUnavailable,
        HttpStatusCode.GatewayTimeout,
    )

internal fun defaultHttpClient(logging: Boolean = false): HttpClient =
    HttpClient {
        if (logging) {
            install(Logging) {
                logger = Log(logging, LogTag.HTTP_CLIENT).ktorLogger
                level = LogLevel.INFO
            }
        }
        install(ContentNegotiation) { json(WebMessagingJson.json) }
        install(HttpCallValidator)
        install(HttpTimeout) {
            socketTimeoutMillis = TIMEOUT_IN_MS
            connectTimeoutMillis = TIMEOUT_IN_MS
        }
        install(HttpRequestRetry) {
            maxRetries = MAX_RETRIES_ON_SERVER_ERRORS

            retryIf { request, response ->
                response.status in RETRYABLE_STATUS_CODES
            }

            exponentialDelay(base = EXPONENTIAL_DELAY_BASE)
        }
    }
