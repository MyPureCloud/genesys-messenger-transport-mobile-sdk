package com.genesys.cloud.messenger.transport.network

import assertk.assertThat
import assertk.assertions.isEqualToWithGivenProperties
import com.genesys.cloud.messenger.transport.auth.AuthJwt
import com.genesys.cloud.messenger.transport.core.Configuration
import com.genesys.cloud.messenger.transport.core.Empty
import com.genesys.cloud.messenger.transport.core.ErrorCode
import com.genesys.cloud.messenger.transport.core.Result
import com.genesys.cloud.messenger.transport.mockHttpClientWith
import com.genesys.cloud.messenger.transport.network.test_engines.UPLOAD_FILE_PATH
import com.genesys.cloud.messenger.transport.network.test_engines.UPLOAD_FILE_SIZE
import com.genesys.cloud.messenger.transport.network.test_engines.authorizeEngine
import com.genesys.cloud.messenger.transport.network.test_engines.historyEngine
import com.genesys.cloud.messenger.transport.network.test_engines.invalidHeaders
import com.genesys.cloud.messenger.transport.network.test_engines.logoutEngine
import com.genesys.cloud.messenger.transport.network.test_engines.refreshTokenEngine
import com.genesys.cloud.messenger.transport.network.test_engines.uploadFileEngine
import com.genesys.cloud.messenger.transport.network.test_engines.validHeaders
import com.genesys.cloud.messenger.transport.shyrka.receive.PresignedUrlResponse
import com.genesys.cloud.messenger.transport.util.Urls
import com.genesys.cloud.messenger.transport.utility.AuthTest
import com.genesys.cloud.messenger.transport.utility.DEFAULT_TIMEOUT
import com.genesys.cloud.messenger.transport.utility.ErrorTest
import com.genesys.cloud.messenger.transport.utility.InvalidValues
import com.genesys.cloud.messenger.transport.utility.TestValues
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.mock.MockEngineConfig
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WebMessagingApiTest {
    private lateinit var subject: WebMessagingApi

    @Test
    fun `when fetchHistory`() {
        subject = buildWebMessagingApiWith { historyEngine() }
        val expectedEntityList = Result.Success(TestWebMessagingApiResponses.testMessageEntityList)

        val result = runBlocking {
            withTimeout(DEFAULT_TIMEOUT) {
                subject.getMessages(jwt = "abc-123", pageNumber = 1)
            }
        }

        assertEquals(expectedEntityList, result)
    }

    @Test
    fun `when fetchHistory but there is no history`() {
        subject = buildWebMessagingApiWith { historyEngine() }

        val expectedEntityList = Result.Success(TestWebMessagingApiResponses.emptyMessageEntityList)

        val result = runBlocking {
            withTimeout(DEFAULT_TIMEOUT) {
                subject.getMessages(jwt = "abc-123", pageNumber = 0, pageSize = 0)
            }
        }

        assertEquals(expectedEntityList, result)
    }

    @Test
    fun `when fetchHistory results in Unexpected Error`() {
        subject = buildWebMessagingApiWith { historyEngine() }
        val expectedEntityList = Result.Failure(ErrorCode.UnexpectedError, ErrorTest.Message)

        val result = runBlocking {
            withTimeout(DEFAULT_TIMEOUT) {
                subject.getMessages(jwt = "abc-123", pageNumber = -1)
            }
        }

        assertEquals(expectedEntityList, result)
    }

    @Test
    fun `when fetchHistory results in CancellationException`() {
        subject = buildWebMessagingApiWith { historyEngine() }
        val expectedEntityList = Result.Failure(ErrorCode.CancellationError, ErrorTest.Message)

        val result = runBlocking {
            withTimeout(DEFAULT_TIMEOUT) {
                subject.getMessages(jwt = InvalidValues.CancellationException, pageNumber = -1)
            }
        }

        assertEquals(expectedEntityList, result)
    }

    @Test
    fun `when uploadFile with valid headers`() {
        subject = buildWebMessagingApiWith { uploadFileEngine() }
        val expectedResult = Result.Success(Empty())

        val givenPresignedUrlResponse = PresignedUrlResponse(
            attachmentId = "99999999-9999-9999-9999-999999999999",
            url = UPLOAD_FILE_PATH,
            fileName = "image.png",
            headers = validHeaders
        )

        val result = runBlocking {
            subject.uploadFile(
                presignedUrlResponse = givenPresignedUrlResponse,
                byteArray = ByteArray(UPLOAD_FILE_SIZE.toInt()),
                progressCallback = { /* mock progress callback */ }
            )
        }

        assertThat(result).isEqualToWithGivenProperties(expectedResult)
    }

    @Test
    fun `when uploadFile with invalid headers`() {
        subject = buildWebMessagingApiWith { uploadFileEngine() }
        val givenPresignedUrlResponse = PresignedUrlResponse(
            attachmentId = "99999999-9999-9999-9999-999999999999",
            url = UPLOAD_FILE_PATH,
            fileName = "image.png",
            headers = invalidHeaders
        )

        val expectedResult = Result.Failure(ErrorCode.mapFrom(HttpStatusCode.NotFound.value), "Not found")

        val result = runBlocking {
            subject.uploadFile(
                presignedUrlResponse = givenPresignedUrlResponse,
                byteArray = ByteArray(UPLOAD_FILE_SIZE.toInt()),
                progressCallback = { /* mock progress callback */ }
            )
        }

        assertThat(result).isEqualToWithGivenProperties(expectedResult)
    }

    @Test
    fun `when fetchAuthJwt is successful`() {
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
    fun `when fetchAuthJwt request body has invalid params`() {
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
    fun `fetch should return result Failure when CancellationException is thrown`() {
        val brokenConfigurations = Configuration(
            deploymentId = InvalidValues.CancellationException,
            domain = InvalidValues.Domain,
            logging = false
        )
        subject = buildWebMessagingApiWith(brokenConfigurations) { authorizeEngine() }

        val expectedResult = Result.Failure(ErrorCode.CancellationError, ErrorTest.Message)

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
    fun `fetch should return result Failure when UnknownException is thrown`() {
        val brokenConfigurations = Configuration(
            deploymentId = InvalidValues.UnknownException,
            domain = InvalidValues.Domain,
            logging = false
        )
        subject = buildWebMessagingApiWith(brokenConfigurations) { authorizeEngine() }

        val expectedResult = Result.Failure(ErrorCode.AuthFailed, ErrorTest.Message)

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
    fun `when logoutFromAuthenticatedSession with valid jwt`() {
        subject = buildWebMessagingApiWith { logoutEngine() }

        val result = runBlocking { subject.logoutFromAuthenticatedSession(jwt = AuthTest.JwtToken) }

        assertTrue(result is Result.Success)
    }

    @Test
    fun `when logoutFromAuthenticatedSession with unauthorized jwt`() {
        subject = buildWebMessagingApiWith { logoutEngine() }
        val expectedResult = Result.Failure(ErrorCode.ClientResponseError(401), "You are not authorized")

        val result =
            runBlocking { subject.logoutFromAuthenticatedSession(InvalidValues.UnauthorizedJwt) }

        assertEquals(expectedResult, result)
    }

    @Test
    fun `when logoutFromAuthenticatedSession result in CancellationException`() {
        subject = buildWebMessagingApiWith { logoutEngine() }
        val expectedResult = Result.Failure(ErrorCode.CancellationError, ErrorTest.Message)

        val result =
            runBlocking { subject.logoutFromAuthenticatedSession(InvalidValues.CancellationException) }

        assertEquals(expectedResult, result)
    }

    @Test
    fun `when logoutFromAuthenticatedSession result in UnknownException`() {
        subject = buildWebMessagingApiWith { logoutEngine() }
        val expectedResult = Result.Failure(ErrorCode.AuthLogoutFailed, ErrorTest.Message)

        val result =
            runBlocking { subject.logoutFromAuthenticatedSession(InvalidValues.UnknownException) }

        assertEquals(expectedResult, result)
    }

    @Test
    fun `when logoutFromAuthenticatedSession with invalid jwt`() {
        subject = buildWebMessagingApiWith { logoutEngine() }
        val expectedResult = Result.Failure(ErrorCode.AuthLogoutFailed, "Bad Request")

        val result =
            runBlocking { subject.logoutFromAuthenticatedSession(InvalidValues.InvalidJwt) }

        assertEquals(expectedResult, result)
    }

    @Test
    fun `when refreshToken with valid refreshToken`() {
        subject = buildWebMessagingApiWith { refreshTokenEngine() }
        val expectedResult = Result.Success(AuthJwt(AuthTest.RefreshedJWTToken, null))

        val result = runBlocking { subject.refreshAuthJwt(AuthTest.RefreshToken) }

        assertEquals(expectedResult, result)
    }

    @Test
    fun `when refreshToken with unauthorized refreshToken`() {
        subject = buildWebMessagingApiWith { refreshTokenEngine() }
        val expectedResult = Result.Failure(ErrorCode.RefreshAuthTokenFailure, "Bad Request")

        val result =
            runBlocking { subject.refreshAuthJwt(InvalidValues.InvalidRefreshToken) }

        assertEquals(expectedResult, result)
    }

    @Test
    fun `when refreshToken result in CancellationException`() {
        subject = buildWebMessagingApiWith { refreshTokenEngine() }
        val expectedResult = Result.Failure(ErrorCode.CancellationError, ErrorTest.Message)

        val result =
            runBlocking { subject.refreshAuthJwt(InvalidValues.CancellationException) }

        assertEquals(expectedResult, result)
    }

    @Test
    fun `when refreshToken result in UnknownException`() {
        subject = buildWebMessagingApiWith { refreshTokenEngine() }
        val expectedResult = Result.Failure(ErrorCode.RefreshAuthTokenFailure, ErrorTest.Message)

        val result =
            runBlocking { subject.refreshAuthJwt(InvalidValues.UnknownException) }

        assertEquals(expectedResult, result)
    }
}

private fun buildWebMessagingApiWith(
    configuration: Configuration = configuration(),
    engine: HttpClientConfig<MockEngineConfig>.() -> Unit,
): WebMessagingApi {
    return WebMessagingApi(
        urls = Urls(configuration.domain, configuration.deploymentId),
        configuration = configuration,
        client = mockHttpClientWith { engine() }
    )
}

private fun configuration(): Configuration = Configuration(
    deploymentId = TestValues.DeploymentId,
    domain = TestValues.Domain,
    logging = false
)
