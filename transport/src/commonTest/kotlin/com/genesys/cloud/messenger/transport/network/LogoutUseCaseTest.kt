package com.genesys.cloud.messenger.transport.network

import com.genesys.cloud.messenger.transport.mockHttpClientWith
import com.genesys.cloud.messenger.transport.respondNotFound
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.mock.MockEngineConfig
import io.ktor.client.engine.mock.respondBadRequest
import io.ktor.client.engine.mock.respondOk
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.Url
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class LogoutUseCaseTest {

    private var subject = LogoutUseCase(
        logoutUrl = Url("https://api.test/api/v2/webdeployments/token/revoke"),
        client = mockHttpClientWith { logoutEngine() }
    )

    @Test
    fun whenLogout() {
        runBlocking { subject.logout(TestJwtToken) }
    }

    @Test
    fun whenAuthJwtIsInvalidThenLogoutShouldThrowAnException() {
        val expectedException =
            Exception("Failed to logout: HttpResponse[https://api.test/api/v2/webdeployments/token/revoke, 400 Bad Request]")

        runBlocking {
            val actualException = assertFailsWith<Exception> {
                subject.logout(jwt = "InvalidAuthCode")
            }

            assertEquals(expectedException.message, actualException.message)
        }
    }

    @Test
    fun whenUseBadUrlThenLogoutShouldThrowAnException() {
        val expectedException =
            Exception("Failed to logout: HttpResponse[https://badurl.com, 404 Not Found]")
        subject = LogoutUseCase(
            logoutUrl = Url("https://badurl.com"),
            client = mockHttpClientWith { logoutEngine() }
        )

        runBlocking {
            val actualException = assertFailsWith<Exception> {
                subject.logout(jwt = "InvalidAuthCode")
            }

            assertEquals(expectedException.message, actualException.message)
        }
    }
}

private fun HttpClientConfig<MockEngineConfig>.logoutEngine() {
    engine {
        addHandler { request ->
            when (request.url.encodedPath) {
                "/api/v2/webdeployments/token/revoke" -> {
                    if (request.method == HttpMethod.Delete &&
                        request.headers[HttpHeaders.Authorization] == "bearer $TestJwtToken"
                    ) {
                        respondOk()
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
