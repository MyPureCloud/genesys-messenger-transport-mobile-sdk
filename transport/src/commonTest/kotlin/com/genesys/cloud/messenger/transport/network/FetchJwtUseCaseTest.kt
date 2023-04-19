package com.genesys.cloud.messenger.transport.network

import com.genesys.cloud.messenger.transport.auth.AuthJwt
import com.genesys.cloud.messenger.transport.mockHttpClientWith
import com.genesys.cloud.messenger.transport.respondNotFound
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.mock.MockEngineConfig
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondBadRequest
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

internal const val TestDeploymentId = "validDeploymentId"
internal const val TestJwtAuthUrl = "https://example.com/auth"
internal const val TestAuthCode = "validAuthCode"
internal const val TestRedirectUri = "https://example.com/redirect"
internal const val TestCodeVerifier = "validCodeVerifier"
internal const val TestJwtToken = "validJwtToken"
internal const val TestRefreshToken = "validRefreshToken"

class FetchJwtUseCaseTest {

    private var subject = FetchJwtUseCase(
        logging = false,
        deploymentId = TestDeploymentId,
        jwtAuthUrl = Url(TestJwtAuthUrl),
        client = mockHttpClientWith { authJwtEngine() },
    )

    @Test
    fun fetchShouldReturnExpectedAuthJwtWhenRequestIsSuccessful() =
        runBlocking {
            val expectedAuthJwt = AuthJwt(TestJwtToken, TestRefreshToken)

            val actualAuthJwt = subject.fetch(
                authCode = TestAuthCode,
                redirectUri = TestRedirectUri,
                codeVerifier = TestCodeVerifier,
            )

            assertEquals(expectedAuthJwt, actualAuthJwt)
        }

    @Test
    fun fetchShouldThrowExceptionWhenRequestIsSuccessfulWithInvalidParameters() {
        val expectedException = Exception("Auth JWT fetch failed: Not found")
        subject = FetchJwtUseCase(
            logging = false,
            deploymentId = "InvalidDeploymentId",
            jwtAuthUrl = Url("InvalidAuthUrl"),
            client = mockHttpClientWith { authJwtEngine() }
        )

        runBlocking {
            val actualException = assertFailsWith<Exception> {
                subject.fetch(
                    authCode = "InvalidAuthCode",
                    redirectUri = "InvalidRedirectUri",
                    codeVerifier = TestCodeVerifier,
                )
            }
            assertEquals(expectedException.message, actualException.message)
        }
    }

    private fun HttpClientConfig<MockEngineConfig>.authJwtEngine() {
        engine {
            addHandler { request ->
                when (request.url.encodedPath) {
                    "/auth" -> {
                        if (request.method == HttpMethod.Post && request.body is TextContent) {
                            val requestBody = Json.decodeFromString(
                                AuthJWTRequest.serializer(),
                                (request.body as TextContent).text
                            )
                            if (requestBody.deploymentId == TestDeploymentId && requestBody.oauth.code == TestAuthCode) {
                                respond(
                                    status = HttpStatusCode.OK,
                                    headers = headersOf(
                                        HttpHeaders.ContentType,
                                        "application/json"
                                    ),
                                    content = Json.encodeToString(
                                        AuthJwt.serializer(),
                                        AuthJwt(TestJwtToken, TestRefreshToken)
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
}
