package com.genesys.cloud.messenger.transport.network.test_engines

import com.genesys.cloud.messenger.transport.auth.AuthJwt
import com.genesys.cloud.messenger.transport.auth.RefreshToken
import com.genesys.cloud.messenger.transport.respondNotFound
import com.genesys.cloud.messenger.transport.utility.AuthTest
import com.genesys.cloud.messenger.transport.utility.ErrorTest
import com.genesys.cloud.messenger.transport.utility.InvalidValues
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.mock.MockEngineConfig
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondBadRequest
import io.ktor.content.TextContent
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.fullPath
import io.ktor.http.headersOf
import kotlinx.serialization.json.Json
import kotlin.coroutines.cancellation.CancellationException

private const val BASIC_REFRESH_TOKEN_PATH =
    "/api/v2/webdeployments/token/refresh"

internal fun HttpClientConfig<MockEngineConfig>.refreshTokenEngine() {
    engine {
        addHandler { request ->
            when (request.url.fullPath) {
                BASIC_REFRESH_TOKEN_PATH -> {
                    if (request.method == HttpMethod.Post && request.body is TextContent) {
                        val requestBody =
                            Json.decodeFromString(
                                RefreshToken.serializer(),
                                (request.body as TextContent).text
                            )
                        if (requestBody.refreshToken == AuthTest.REFRESH_TOKEN) {
                            respond(
                                status = HttpStatusCode.OK,
                                headers =
                                    headersOf(
                                        HttpHeaders.ContentType,
                                        "application/json"
                                    ),
                                content =
                                    Json.encodeToString(
                                        AuthJwt.serializer(),
                                        AuthJwt(AuthTest.REFRESHED_JWT_TOKEN, null)
                                    )
                            )
                        } else {
                            when (requestBody.refreshToken) {
                                InvalidValues.CANCELLATION_EXCEPTION -> {
                                    throw CancellationException(ErrorTest.MESSAGE)
                                }
                                InvalidValues.NETWORK_EXCEPTION -> {
                                    throw NetworkExceptionForTesting(ErrorTest.MESSAGE)
                                }
                                InvalidValues.UNKNOWN_EXCEPTION -> {
                                    error(ErrorTest.MESSAGE)
                                }
                                else -> {
                                    respondBadRequest()
                                }
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
