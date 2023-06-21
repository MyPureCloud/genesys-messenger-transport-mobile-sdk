package com.genesys.cloud.messenger.transport.network

import com.genesys.cloud.messenger.transport.auth.AuthJwt
import com.genesys.cloud.messenger.transport.core.Configuration
import com.genesys.cloud.messenger.transport.core.ErrorCode
import com.genesys.cloud.messenger.transport.core.Result
import com.genesys.cloud.messenger.transport.mockHttpClientWith
import com.genesys.cloud.messenger.transport.network.test_engines.authorizeEngine
import com.genesys.cloud.messenger.transport.network.test_engines.historyEngine
import com.genesys.cloud.messenger.transport.network.test_engines.logoutEngine
import com.genesys.cloud.messenger.transport.network.test_engines.refreshTokenEngine
import com.genesys.cloud.messenger.transport.utility.AuthTest
import com.genesys.cloud.messenger.transport.utility.DEFAULT_TIMEOUT
import com.genesys.cloud.messenger.transport.utility.InvalidValues
import com.genesys.cloud.messenger.transport.utility.TestValues
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.mock.MockEngineConfig
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WebMessagingApiTest {
    private lateinit var subject: WebMessagingApi

    @Test
    fun whenFetchHistory() {
        subject = buildWebMessagingApiWith { historyEngine() }
        val expectedEntityList = TestWebMessagingApiResponses.testMessageEntityList

        val result = runBlocking {
            withTimeout(DEFAULT_TIMEOUT) {
                subject.getMessages(jwt = "abc-123", pageNumber = 1)
            }
        }

        assertEquals(expectedEntityList, result)
    }

    @Test
    fun whenFetchHistoryAndThereAreNoHistory() {
        subject = buildWebMessagingApiWith { historyEngine() }

        val expectedEntityList = TestWebMessagingApiResponses.emptyMessageEntityList

        val result = runBlocking {
            withTimeout(DEFAULT_TIMEOUT) {
                subject.getMessages(jwt = "abc-123", pageNumber = 0, pageSize = 0)
            }
        }

        assertEquals(expectedEntityList, result)
    }

    @Test
    fun fetchAuthJwtShouldReturnResultSuccessWithAuthJwtWhenRequestIsSuccessful() {
        subject = buildWebMessagingApiWith { authorizeEngine() }
        val expectedResult = Result.Success(AuthJwt(AuthTest.JwtToken, AuthTest.RefreshToken))

        val result = runBlocking {
            subject.fetchAuthJwt(
                authCode = AuthTest.AuthCode,
                redirectUri = AuthTest.RedirectUri,
                codeVerifier = AuthTest.CodeVerifier,
            )
        }

        assertEquals(expectedResult, result)
    }

    @Test
    fun fetchShouldReturnResultFailureWhenRequestBodyHasBrokenParams() {
        val brokenConfigurations = Configuration(
            deploymentId = InvalidValues.DeploymentId,
            domain = InvalidValues.Domain,
            logging = false
        )
        subject = buildWebMessagingApiWith(brokenConfigurations) { authorizeEngine() }

        val expectedResult = Result.Failure(ErrorCode.AuthFailed, "Bad Request")

        val result = runBlocking {
            subject.fetchAuthJwt(
                authCode = AuthTest.AuthCode,
                redirectUri = AuthTest.RedirectUri,
                codeVerifier = AuthTest.CodeVerifier,
            )
        }

        assertEquals(expectedResult, result)
    }

    @Test
    fun whenLogoutFromAuthenticatedSessionWithValidJwt() {
        subject = buildWebMessagingApiWith { logoutEngine() }

        val result = runBlocking { subject.logoutFromAuthenticatedSession(jwt = AuthTest.JwtToken) }

        assertTrue(result is Result.Success)
    }

    @Test
    fun whenLogoutFromAuthenticatedSessionWithUnauthorizedJwt() {
        subject = buildWebMessagingApiWith { logoutEngine() }
        val expectedResult = Result.Failure(ErrorCode.ClientResponseError(401), "You are not authorized")

        val result =
            runBlocking { subject.logoutFromAuthenticatedSession(InvalidValues.UnauthorizedJwt) }

        assertEquals(expectedResult, result)
    }

    @Test
    fun whenLogoutFromAuthenticatedSessionWithInvalidJwt() {
        subject = buildWebMessagingApiWith { logoutEngine() }
        val expectedResult = Result.Failure(ErrorCode.AuthLogoutFailed, "Bad Request")

        val result =
            runBlocking { subject.logoutFromAuthenticatedSession(InvalidValues.InvalidJwt) }

        assertEquals(expectedResult, result)
    }

    @Test
    fun whenRefreshTokenWithValidRefreshToken() {
        subject = buildWebMessagingApiWith { refreshTokenEngine() }
        val expectedResult = Result.Success(AuthJwt(AuthTest.RefreshedJWTToken, null))

        val result = runBlocking { subject.refreshAuthJwt(AuthTest.RefreshToken) }

        assertEquals(expectedResult, result)
    }

    @Test
    fun whenRefreshTokenWithUnauthorizedRefreshToken() {
        subject = buildWebMessagingApiWith { refreshTokenEngine() }
        val expectedResult = Result.Failure(ErrorCode.RefreshAuthTokenFailure, "Bad Request")

        val result =
            runBlocking { subject.refreshAuthJwt(InvalidValues.InvalidRefreshToken) }

        assertEquals(expectedResult, result)
    }
}

private fun buildWebMessagingApiWith(
    configuration: Configuration = configuration(),
    engine: HttpClientConfig<MockEngineConfig>.() -> Unit,
): WebMessagingApi {
    return WebMessagingApi(
        configuration = configuration,
        client = mockHttpClientWith { engine() }
    )
}

private fun configuration(): Configuration = Configuration(
    deploymentId = TestValues.DeploymentId,
    domain = TestValues.Domain,
    logging = false
)
