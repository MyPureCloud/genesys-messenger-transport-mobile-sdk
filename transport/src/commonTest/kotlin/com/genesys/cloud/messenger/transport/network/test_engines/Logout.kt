package com.genesys.cloud.messenger.transport.network.test_engines

import com.genesys.cloud.messenger.transport.respondNotFound
import com.genesys.cloud.messenger.transport.utility.AuthTest
import com.genesys.cloud.messenger.transport.utility.InvalidValues
import com.genesys.cloud.messenger.transport.utility.respondUnauthorized
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.mock.MockEngineConfig
import io.ktor.client.engine.mock.respondBadRequest
import io.ktor.client.engine.mock.respondOk
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.fullPath

private const val BASIC_LOGOUT_PATH =
    "/api/v2/webdeployments/token/revoke"

internal fun HttpClientConfig<MockEngineConfig>.logoutEngine() {
    engine {
        addHandler { request ->
            when (request.url.fullPath) {
                BASIC_LOGOUT_PATH -> {
                    if (request.method == HttpMethod.Delete) {
                        when (request.headers[HttpHeaders.Authorization]) {
                            "bearer ${AuthTest.JwtToken}" -> {
                                respondOk()
                            }
                            "bearer ${InvalidValues.UnauthorizedJwt}" -> {
                                respondUnauthorized()
                            }
                            "bearer ${InvalidValues.InvalidJwt}" -> {
                                respondBadRequest()
                            }
                            else -> {
                                respondBadRequest()
                            }
                        }
                    } else {
                        respondBadRequest()
                    }
                }
                else -> {
                    respondNotFound()
                }
            }
        }
    }
}
