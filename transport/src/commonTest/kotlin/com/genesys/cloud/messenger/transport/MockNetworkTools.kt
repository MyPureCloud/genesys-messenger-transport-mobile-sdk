package com.genesys.cloud.messenger.transport

import com.genesys.cloud.messenger.transport.shyrka.WebMessagingJson
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockEngineConfig
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.SerializationException
import kotlin.coroutines.cancellation.CancellationException

private const val TEST_MAX_RETRIES = 3
private const val TEST_EXPONENTIAL_DELAY_BASE = 3.0

fun mockHttpClientWith(
    enableRetry: Boolean = false,
    engineBlock: HttpClientConfig<MockEngineConfig>.() -> Unit,
): HttpClient =
    HttpClient(MockEngine) {
        install(ContentNegotiation) {
            json(WebMessagingJson.json)
        }
        if (enableRetry) {
            install(HttpRequestRetry) {
                maxRetries = TEST_MAX_RETRIES
                retryIf { request, response ->
                    when (response.status) {
                        HttpStatusCode.BadGateway,
                        HttpStatusCode.ServiceUnavailable,
                        HttpStatusCode.GatewayTimeout -> true
                        else -> false
                    }
                }
                retryOnExceptionIf { request, cause ->
                    cause !is CancellationException && cause !is SerializationException
                }
                exponentialDelay(base = TEST_EXPONENTIAL_DELAY_BASE)
            }
        }
        engineBlock()
    }

fun MockRequestHandleScope.respondNotFound(): HttpResponseData = respond("Not found", HttpStatusCode.NotFound)
