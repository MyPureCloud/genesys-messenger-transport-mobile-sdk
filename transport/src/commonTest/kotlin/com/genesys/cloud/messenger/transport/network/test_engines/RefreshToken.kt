package com.genesys.cloud.messenger.transport.network.test_engines

import com.genesys.cloud.messenger.transport.auth.AuthJwt
import com.genesys.cloud.messenger.transport.auth.RefreshToken
import com.genesys.cloud.messenger.transport.respondNotFound
import com.genesys.cloud.messenger.transport.utility.AuthTest
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

private const val BASIC_REFRESH_TOKEN_PATH =
    "/api/v2/webdeployments/token/refresh"

internal fun HttpClientConfig<MockEngineConfig>.refreshTokenEngine() {
    engine {
        addHandler { request ->
            when (request.url.fullPath) {
                BASIC_REFRESH_TOKEN_PATH -> {
                    if (request.method == HttpMethod.Post && request.body is TextContent) {
                        val requestBody = Json.decodeFromString(
                            RefreshToken.serializer(),
                            (request.body as TextContent).text
                        )
                        if (requestBody.refreshToken == AuthTest.RefreshToken) {
                            respond(
                                status = HttpStatusCode.OK,
                                headers = headersOf(
                                    HttpHeaders.ContentType,
                                    "application/json"
                                ),
                                content = Json.encodeToString(
                                    AuthJwt.serializer(),
                                    AuthJwt(AuthTest.RefreshedJWTToken, null)
                                )
                            )
                        } else {
                            respondBadRequest()
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
