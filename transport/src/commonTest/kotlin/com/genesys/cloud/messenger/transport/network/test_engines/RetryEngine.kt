package com.genesys.cloud.messenger.transport.network.test_engines

import com.genesys.cloud.messenger.transport.respondNotFound
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.mock.MockEngineConfig
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf

/**
 * Test engine for retry scenarios including 429 rate limits and 5xx server errors
 */
internal fun HttpClientConfig<MockEngineConfig>.retryEngine() {
    engine {
        var requestCount = 0

        addHandler { request ->
            requestCount++

            when (request.url.toString()) {
                "https://test.com/rate-limit-with-retry-after" -> {
                    if (requestCount == 1) {
                        respond(
                            status = HttpStatusCode.TooManyRequests,
                            headers = headersOf(HttpHeaders.RetryAfter, "2"),
                            content = "Rate limited"
                        )
                    } else {
                        respond("Success after retry")
                    }
                }

                "https://test.com/rate-limit-exceeds-cap" -> {
                    respond(
                        status = HttpStatusCode.TooManyRequests,
                        headers = headersOf(HttpHeaders.RetryAfter, "60"),
                        content = "Rate limited - exceeds cap"
                    )
                }

                "https://test.com/rate-limit-no-retry-after" -> {
                    respond(
                        status = HttpStatusCode.TooManyRequests,
                        content = "Rate limited - no retry after"
                    )
                }

                "https://test.com/rate-limit-invalid-retry-after" -> {
                    respond(
                        status = HttpStatusCode.TooManyRequests,
                        headers = headersOf(HttpHeaders.RetryAfter, "invalid"),
                        content = "Rate limited - invalid retry after"
                    )
                }

                "https://test.com/server-error-502" -> {
                    if (requestCount <= 3) {
                        respond(
                            status = HttpStatusCode.BadGateway,
                            content = "Bad Gateway"
                        )
                    } else {
                        respond("Success after server error retry")
                    }
                }

                "https://test.com/server-error-503" -> {
                    if (requestCount <= 3) {
                        respond(
                            status = HttpStatusCode.ServiceUnavailable,
                            content = "Service Unavailable"
                        )
                    } else {
                        respond("Success after service unavailable retry")
                    }
                }

                "https://test.com/server-error-504" -> {
                    if (requestCount <= 3) {
                        respond(
                            status = HttpStatusCode.GatewayTimeout,
                            content = "Gateway Timeout"
                        )
                    } else {
                        respond("Success after gateway timeout retry")
                    }
                }

                "https://test.com/server-error-max-retries" -> {
                    respond(
                        status = HttpStatusCode.BadGateway,
                        content = "Bad Gateway - max retries"
                    )
                }

                "https://test.com/success" -> {
                    respond("Success")
                }

                else -> {
                    respondNotFound()
                }
            }
        }
    }
}
