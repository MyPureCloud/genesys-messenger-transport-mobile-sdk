package com.genesys.cloud.messenger.transport.network.test_engines

import com.genesys.cloud.messenger.transport.respondNotFound
import com.genesys.cloud.messenger.transport.utility.AuthTest
import com.genesys.cloud.messenger.transport.utility.ErrorTest
import com.genesys.cloud.messenger.transport.utility.InvalidValues
import com.genesys.cloud.messenger.transport.utility.respondUnauthorized
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.mock.MockEngineConfig
import io.ktor.client.engine.mock.respondBadRequest
import io.ktor.client.engine.mock.respondOk
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.fullPath
import kotlinx.coroutines.CancellationException

private const val BASIC_LOGOUT_PATH =
    "/api/v2/webdeployments/token/revoke"

internal fun HttpClientConfig<MockEngineConfig>.logoutEngine() {
    engine {
        addHandler { request ->
            when (request.url.fullPath) {
                BASIC_LOGOUT_PATH -> {
                    if (request.method == HttpMethod.Delete) {
                        when (request.headers[HttpHeaders.Authorization]) {
                            "bearer ${AuthTest.JWT_TOKEN}" -> {
                                respondOk()
                            }
                            "bearer ${InvalidValues.UNAUTHORIZED_JWT}" -> {
                                respondUnauthorized()
                            }
                            "bearer ${InvalidValues.INVALID_JWT}" -> {
                                respondBadRequest()
                            }
                            "bearer ${InvalidValues.CANCELLATION_EXCEPTION}" -> {
                                throw CancellationException(ErrorTest.MESSAGE)
                            }
                            "bearer ${InvalidValues.UNKNOWN_EXCEPTION}" -> {
                                error(ErrorTest.MESSAGE)
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
