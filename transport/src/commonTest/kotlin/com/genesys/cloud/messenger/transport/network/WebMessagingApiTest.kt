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
import com.genesys.cloud.messenger.transport.network.test_engines.pushNotificationEngine
import com.genesys.cloud.messenger.transport.network.test_engines.refreshTokenEngine
import com.genesys.cloud.messenger.transport.network.test_engines.uploadFileEngine
import com.genesys.cloud.messenger.transport.network.test_engines.validHeaders
import com.genesys.cloud.messenger.transport.push.DeviceTokenOperation
import com.genesys.cloud.messenger.transport.shyrka.receive.PresignedUrlResponse
import com.genesys.cloud.messenger.transport.util.Urls
import com.genesys.cloud.messenger.transport.utility.AuthTest
import com.genesys.cloud.messenger.transport.utility.DEFAULT_TIMEOUT
import com.genesys.cloud.messenger.transport.utility.ErrorTest
import com.genesys.cloud.messenger.transport.utility.InvalidValues
import com.genesys.cloud.messenger.transport.utility.PushTestValues
import com.genesys.cloud.messenger.transport.utility.TestValues
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.mock.MockEngineConfig
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlin.coroutines.cancellation.CancellationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
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
        val expectedEntityList = Result.Failure(ErrorCode.UnexpectedError, ErrorTest.MESSAGE)

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
        val expectedEntityList = Result.Failure(ErrorCode.CancellationError, ErrorTest.MESSAGE)

        val result = runBlocking {
            withTimeout(DEFAULT_TIMEOUT) {
                subject.getMessages(jwt = InvalidValues.CANCELLATION_EXCEPTION, pageNumber = -1)
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
        val expectedResult = Result.Success(AuthJwt(AuthTest.JWT_TOKEN, AuthTest.REFRESH_TOKEN))

        val result = runBlocking {
            subject.fetchAuthJwt(
                authCode = AuthTest.AUTH_CODE,
                redirectUri = AuthTest.REDIRECT_URI,
                codeVerifier = AuthTest.CODE_VERIFIER,
            )
        }

        assertEquals(expectedResult, result)
    }

    @Test
    fun `when fetchAuthJwt request body has invalid params`() {
        val brokenConfigurations = Configuration(
            deploymentId = InvalidValues.DEPLOYMENT_ID,
            domain = InvalidValues.DOMAIN,
            logging = false
        )
        subject = buildWebMessagingApiWith(brokenConfigurations) { authorizeEngine() }

        val expectedResult = Result.Failure(ErrorCode.AuthFailed, "Bad Request")

        val result = runBlocking {
            subject.fetchAuthJwt(
                authCode = AuthTest.AUTH_CODE,
                redirectUri = AuthTest.REDIRECT_URI,
                codeVerifier = AuthTest.CODE_VERIFIER,
            )
        }

        assertEquals(expectedResult, result)
    }

    @Test
    fun `fetch should return result Failure when CancellationException is thrown`() {
        val brokenConfigurations = Configuration(
            deploymentId = InvalidValues.CANCELLATION_EXCEPTION,
            domain = InvalidValues.DOMAIN,
            logging = false
        )
        subject = buildWebMessagingApiWith(brokenConfigurations) { authorizeEngine() }

        val expectedResult = Result.Failure(ErrorCode.CancellationError, ErrorTest.MESSAGE)

        val result = runBlocking {
            subject.fetchAuthJwt(
                authCode = AuthTest.AUTH_CODE,
                redirectUri = AuthTest.REDIRECT_URI,
                codeVerifier = AuthTest.CODE_VERIFIER,
            )
        }

        assertEquals(expectedResult, result)
    }

    @Test
    fun `fetch should return result Failure when UnknownException is thrown`() {
        val brokenConfigurations = Configuration(
            deploymentId = InvalidValues.UNKNOWN_EXCEPTION,
            domain = InvalidValues.DOMAIN,
            logging = false
        )
        subject = buildWebMessagingApiWith(brokenConfigurations) { authorizeEngine() }

        val expectedResult = Result.Failure(ErrorCode.AuthFailed, ErrorTest.MESSAGE)

        val result = runBlocking {
            subject.fetchAuthJwt(
                authCode = AuthTest.AUTH_CODE,
                redirectUri = AuthTest.REDIRECT_URI,
                codeVerifier = AuthTest.CODE_VERIFIER,
            )
        }

        assertEquals(expectedResult, result)
    }

    @Test
    fun `when logoutFromAuthenticatedSession with valid jwt`() {
        subject = buildWebMessagingApiWith { logoutEngine() }

        val result = runBlocking { subject.logoutFromAuthenticatedSession(jwt = AuthTest.JWT_TOKEN) }

        assertTrue(result is Result.Success)
    }

    @Test
    fun `when logoutFromAuthenticatedSession with unauthorized jwt`() {
        subject = buildWebMessagingApiWith { logoutEngine() }
        val expectedResult = Result.Failure(ErrorCode.ClientResponseError(401), "You are not authorized")

        val result =
            runBlocking { subject.logoutFromAuthenticatedSession(InvalidValues.UNAUTHORIZED_JWT) }

        assertEquals(expectedResult, result)
    }

    @Test
    fun `when logoutFromAuthenticatedSession result in CancellationException`() {
        subject = buildWebMessagingApiWith { logoutEngine() }
        val expectedResult = Result.Failure(ErrorCode.CancellationError, ErrorTest.MESSAGE)

        val result =
            runBlocking { subject.logoutFromAuthenticatedSession(InvalidValues.CANCELLATION_EXCEPTION) }

        assertEquals(expectedResult, result)
    }

    @Test
    fun `when logoutFromAuthenticatedSession result in UnknownException`() {
        subject = buildWebMessagingApiWith { logoutEngine() }
        val expectedResult = Result.Failure(ErrorCode.AuthLogoutFailed, ErrorTest.MESSAGE)

        val result =
            runBlocking { subject.logoutFromAuthenticatedSession(InvalidValues.UNKNOWN_EXCEPTION) }

        assertEquals(expectedResult, result)
    }

    @Test
    fun `when logoutFromAuthenticatedSession with invalid jwt`() {
        subject = buildWebMessagingApiWith { logoutEngine() }
        val expectedResult = Result.Failure(ErrorCode.AuthLogoutFailed, "Bad Request")

        val result =
            runBlocking { subject.logoutFromAuthenticatedSession(InvalidValues.INVALID_JWT) }

        assertEquals(expectedResult, result)
    }

    @Test
    fun `when refreshToken with valid refreshToken`() {
        subject = buildWebMessagingApiWith { refreshTokenEngine() }
        val expectedResult = Result.Success(AuthJwt(AuthTest.REFRESHED_JWT_TOKEN, null))

        val result = runBlocking { subject.refreshAuthJwt(AuthTest.REFRESH_TOKEN) }

        assertEquals(expectedResult, result)
    }

    @Test
    fun `when refreshToken with unauthorized refreshToken`() {
        subject = buildWebMessagingApiWith { refreshTokenEngine() }
        val expectedResult = Result.Failure(ErrorCode.RefreshAuthTokenFailure, "Bad Request")

        val result =
            runBlocking { subject.refreshAuthJwt(InvalidValues.INVALID_REFRESH_TOKEN) }

        assertEquals(expectedResult, result)
    }

    @Test
    fun `when refreshToken result in CancellationException`() {
        subject = buildWebMessagingApiWith { refreshTokenEngine() }
        val expectedResult = Result.Failure(ErrorCode.CancellationError, ErrorTest.MESSAGE)

        val result =
            runBlocking { subject.refreshAuthJwt(InvalidValues.CANCELLATION_EXCEPTION) }

        assertEquals(expectedResult, result)
    }

    @Test
    fun `when refreshToken result in UnknownException`() {
        subject = buildWebMessagingApiWith { refreshTokenEngine() }
        val expectedResult = Result.Failure(ErrorCode.RefreshAuthTokenFailure, ErrorTest.MESSAGE)

        val result =
            runBlocking { subject.refreshAuthJwt(InvalidValues.UNKNOWN_EXCEPTION) }

        assertEquals(expectedResult, result)
    }

    @Test
    fun `when performDeviceTokenOperation Register with valid userConfig data`() {
        subject = buildWebMessagingApiWith { pushNotificationEngine() }
        val givenUserPushConfig = PushTestValues.CONFIG
        val givenOperation = DeviceTokenOperation.Register

        val result = runBlocking { subject.performDeviceTokenOperation(givenUserPushConfig, givenOperation) }

        assertTrue(result is Result.Success<Empty>)
    }

    @Test
    fun `when performDeviceTokenOperation Update with valid userConfig data`() {
        subject = buildWebMessagingApiWith { pushNotificationEngine() }
        val givenUserPushConfig = PushTestValues.CONFIG
        val givenOperation = DeviceTokenOperation.Update

        val result = runBlocking { subject.performDeviceTokenOperation(givenUserPushConfig, givenOperation) }

        assertTrue(result is Result.Success<Empty>)
    }

    @Test
    fun `when performDeviceTokenOperation Delete with valid userConfig data`() {
        subject = buildWebMessagingApiWith { pushNotificationEngine() }
        val givenUserPushConfig = PushTestValues.CONFIG
        val givenOperation = DeviceTokenOperation.Delete

        val result = runBlocking { subject.performDeviceTokenOperation(givenUserPushConfig, givenOperation) }

        assertTrue(result is Result.Success<Empty>)
    }

    @Test
    fun `when performDeviceTokenOperation Register with uppercase token in userConfig data`() {
        subject = buildWebMessagingApiWith { pushNotificationEngine() }
        val givenUserPushConfig = PushTestValues.CONFIG.copy(token = TestValues.TOKEN.uppercase())
        val givenOperation = DeviceTokenOperation.Delete

        val result = runBlocking { subject.performDeviceTokenOperation(givenUserPushConfig, givenOperation) }

        assertTrue(result is Result.Success<Empty>)
    }

    @Test
    fun `when performDeviceTokenOperation register result in PushErrorResponse`() {
        subject = buildWebMessagingApiWith { pushNotificationEngine() }
        val givenUserPushConfig = PushTestValues.CONFIG.copy(pushProvider = null)
        val givenOperation = DeviceTokenOperation.Register
        val expectedResult = Result.Failure(ErrorCode.DeviceRegistrationFailure, ErrorTest.MESSAGE)

        val result = runBlocking { subject.performDeviceTokenOperation(givenUserPushConfig, givenOperation) }

        assertEquals(expectedResult, result)
    }

    @Test
    fun `when performDeviceTokenOperation any result in CancellationException`() {
        val brokenConfigurations = Configuration(
            deploymentId = InvalidValues.CANCELLATION_EXCEPTION,
            domain = InvalidValues.CANCELLATION_EXCEPTION,
            logging = false
        )
        subject = buildWebMessagingApiWith(brokenConfigurations) { pushNotificationEngine() }
        val givenUserPushConfig = PushTestValues.CONFIG.copy(token = InvalidValues.CANCELLATION_EXCEPTION)
        val givenOperation = DeviceTokenOperation.Register
        val expectedException = CancellationException(ErrorTest.MESSAGE)
        val expectedResult = Result.Failure(ErrorCode.CancellationError, ErrorTest.MESSAGE, expectedException)

        val result =
            runBlocking { subject.performDeviceTokenOperation(givenUserPushConfig, givenOperation) }

        (result as Result.Failure).run {
            assertEquals(expectedResult.errorCode, errorCode)
            assertEquals(expectedResult.message, message)
            assertIs<CancellationException>(throwable)
        }
    }

    @Test
    fun `when performDeviceTokenOperation any result in general Exception`() {
        val brokenConfigurations = Configuration(
            deploymentId = InvalidValues.UNKNOWN_EXCEPTION,
            domain = InvalidValues.UNKNOWN_EXCEPTION,
            logging = false
        )
        subject = buildWebMessagingApiWith(brokenConfigurations) { pushNotificationEngine() }
        val givenUserPushConfig = PushTestValues.CONFIG.copy(token = InvalidValues.UNKNOWN_EXCEPTION)
        val givenOperation = DeviceTokenOperation.Register
        val expectedException = CancellationException(ErrorTest.MESSAGE)
        val expectedResult = Result.Failure(ErrorCode.DeviceTokenOperationFailure, ErrorTest.MESSAGE, expectedException)

        val result =
            runBlocking { subject.performDeviceTokenOperation(givenUserPushConfig, givenOperation) }

        (result as Result.Failure).run {
            assertEquals(expectedResult.errorCode, errorCode)
            assertEquals(expectedResult.message, message)
            assertIs<Exception>(throwable)
        }
    }
}

private fun buildWebMessagingApiWith(
    configuration: Configuration = configuration(),
    engine: HttpClientConfig<MockEngineConfig>.() -> Unit,
): WebMessagingApi {
    return WebMessagingApi(
        urls = Urls(configuration.domain, configuration.deploymentId, configuration.application),
        configuration = configuration,
        client = mockHttpClientWith { engine() }
    )
}

private fun configuration(): Configuration = Configuration(
    deploymentId = TestValues.DEPLOYMENT_ID,
    domain = TestValues.DOMAIN,
    logging = false
)
