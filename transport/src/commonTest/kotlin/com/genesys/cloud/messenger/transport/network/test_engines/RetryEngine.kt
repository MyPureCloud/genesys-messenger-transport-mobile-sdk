package com.genesys.cloud.messenger.transport.network.test_engines

import com.genesys.cloud.messenger.transport.respondNotFound
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.mock.MockEngineConfig
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode

/**
 * Test engine for 5xx server error retry scenarios
 */
internal fun HttpClientConfig<MockEngineConfig>.retryEngine() {
    engine {
        var requestCount = 0

        addHandler { request ->
            requestCount++

            when (request.url.toString()) {
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
