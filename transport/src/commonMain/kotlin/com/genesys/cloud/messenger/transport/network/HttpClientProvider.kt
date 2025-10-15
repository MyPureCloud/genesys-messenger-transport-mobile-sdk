package com.genesys.cloud.messenger.transport.network

import com.genesys.cloud.messenger.transport.core.Configuration
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
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.util.AttributeKey

private const val TIMEOUT_IN_MS = 30000L
private val SkipRetry = AttributeKey<Boolean>("transport.retry.skip")

internal fun HttpRequestBuilder.noRetry() {
    attributes.put(SkipRetry, true)
}

internal fun defaultHttpClient(configuration: Configuration): HttpClient =
    HttpClient {
        if (configuration.logging) {
            install(Logging) {
                logger = Log(configuration.logging, LogTag.HTTP_CLIENT).ktorLogger
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
            if (!configuration.retryEnabled) {
                maxRetries = 0
                return@install
            }

            maxRetries = 3

            retryIf { request, response ->
                if (request.attributes.getOrNull(SkipRetry) == true) return@retryIf false

                when (response.status) {
                    HttpStatusCode.TooManyRequests -> {
                        val ms = parseRetryAfterMillis(response.headers[HttpHeaders.RetryAfter])
                        ms != null && ms <= configuration.maxRetryAfterWaitSeconds * 1000L
                    }

                    HttpStatusCode.BadGateway,
                    HttpStatusCode.ServiceUnavailable,
                    HttpStatusCode.GatewayTimeout -> configuration.backoffEnabled

                    else -> false
                }
            }

            retryOnExceptionIf { request, cause ->
                if (request.attributes.getOrNull(SkipRetry) == true) return@retryOnExceptionIf false
                configuration.backoffEnabled && cause !is kotlin.coroutines.cancellation.CancellationException
            }
            exponentialDelay(base = 3.0)
        }
    }

private fun parseRetryAfterMillis(raw: String?): Long? {
    if (raw.isNullOrBlank()) return null
    return raw.toLongOrNull()?.let { it * 1000L }
}
