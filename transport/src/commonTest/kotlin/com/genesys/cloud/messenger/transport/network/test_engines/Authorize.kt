package com.genesys.cloud.messenger.transport.network.test_engines

import com.genesys.cloud.messenger.transport.auth.AuthJwt
import com.genesys.cloud.messenger.transport.respondNotFound
import com.genesys.cloud.messenger.transport.shyrka.send.AuthJwtRequest
import com.genesys.cloud.messenger.transport.utility.AuthTest
import com.genesys.cloud.messenger.transport.utility.ErrorTest
import com.genesys.cloud.messenger.transport.utility.InvalidValues
import com.genesys.cloud.messenger.transport.utility.TestValues
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
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json

private const val BASIC_OAUTH_CODE_EXCHANGE_PATH =
    "/api/v2/webdeployments/token/oauthcodegrantjwtexchange"

internal fun HttpClientConfig<MockEngineConfig>.authorizeEngine() {
    engine {
        addHandler { request ->
            when (request.url.fullPath) {
                BASIC_OAUTH_CODE_EXCHANGE_PATH -> {
                    if (request.method == HttpMethod.Post && request.body is TextContent) {
                        val requestBody = Json.decodeFromString(
                            AuthJwtRequest.serializer(),
                            (request.body as TextContent).text
                        )
                        if (requestBody.deploymentId == TestValues.DeploymentId && requestBody.oauth.code == AuthTest.AuthCode) {
                            respond(
                                status = HttpStatusCode.OK,
                                headers = headersOf(
                                    HttpHeaders.ContentType,
                                    "application/json"
                                ),
                                content = Json.encodeToString(
                                    AuthJwt.serializer(),
                                    AuthJwt(AuthTest.JwtToken, AuthTest.RefreshToken)
                                )
                            )
                        } else {
                            when (requestBody.deploymentId) {
                                InvalidValues.CancellationException -> {
                                    throw CancellationException(ErrorTest.Message)
                                }

                                InvalidValues.UnknownException -> {
                                    error(ErrorTest.Message)
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
