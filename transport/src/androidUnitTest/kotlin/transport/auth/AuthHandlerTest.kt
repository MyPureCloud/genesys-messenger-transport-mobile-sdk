package transport.auth

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotEmpty
import assertk.assertions.isNull
import assertk.assertions.isTrue
import com.genesys.cloud.messenger.transport.auth.AuthHandlerImpl
import com.genesys.cloud.messenger.transport.auth.AuthJwt
import com.genesys.cloud.messenger.transport.auth.MAX_LOGOUT_ATTEMPTS
import com.genesys.cloud.messenger.transport.auth.NO_JWT
import com.genesys.cloud.messenger.transport.auth.NO_REFRESH_TOKEN
import com.genesys.cloud.messenger.transport.auth.RefreshToken
import com.genesys.cloud.messenger.transport.core.CorrectiveAction
import com.genesys.cloud.messenger.transport.core.Empty
import com.genesys.cloud.messenger.transport.core.ErrorCode
import com.genesys.cloud.messenger.transport.core.ErrorMessage
import com.genesys.cloud.messenger.transport.core.Result
import com.genesys.cloud.messenger.transport.core.events.Event
import com.genesys.cloud.messenger.transport.core.events.EventHandler
import com.genesys.cloud.messenger.transport.network.WebMessagingApi
import com.genesys.cloud.messenger.transport.shyrka.WebMessagingJson
import com.genesys.cloud.messenger.transport.util.logs.Log
import com.genesys.cloud.messenger.transport.util.logs.LogMessages
import com.genesys.cloud.messenger.transport.utility.AuthTest
import com.genesys.cloud.messenger.transport.utility.ErrorTest
import com.genesys.cloud.messenger.transport.utility.FakeVault
import com.genesys.cloud.messenger.transport.utility.TestValues
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.encodeToString
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertFalse

class AuthHandlerTest {

    @MockK(relaxed = true)
    private val mockEventHandler: EventHandler = mockk(relaxed = true)
    private val mockLogger: Log = mockk(relaxed = true)
    private val logSlot = slot<() -> String>()

    @MockK(relaxed = true)
    private val mockWebMessagingApi: WebMessagingApi = mockk {
        coEvery {
            fetchAuthJwt(
                AuthTest.AUTH_CODE,
                AuthTest.REDIRECT_URI,
                AuthTest.CODE_VERIFIER,
            )
        } returns Result.Success(AuthJwt(AuthTest.JWT_TOKEN, AuthTest.REFRESH_TOKEN))

        coEvery {
            logoutFromAuthenticatedSession(AuthTest.JWT_TOKEN)
        } returns Result.Success(Empty())

        coEvery {
            logoutFromAuthenticatedSession(NO_JWT)
        } returns Result.Failure(
            ErrorCode.AuthLogoutFailed,
            ErrorTest.MESSAGE,
        )

        coEvery { refreshAuthJwt(AuthTest.REFRESH_TOKEN) } returns Result.Success(
            AuthJwt(AuthTest.REFRESHED_JWT_TOKEN, null)
        )

        coEvery { refreshAuthJwt(NO_REFRESH_TOKEN) } returns Result.Failure(
            ErrorCode.RefreshAuthTokenFailure,
            ErrorTest.MESSAGE,
        )
    }

    private val fakeVault: FakeVault = FakeVault(TestValues.vaultKeys)
    private val dispatcher: CoroutineDispatcher = Dispatchers.Unconfined

    private lateinit var subject: AuthHandlerImpl

    @ExperimentalCoroutinesApi
    @Before
    fun setup() {
        MockKAnnotations.init(this)
        Dispatchers.setMain(dispatcher)
        subject = buildAuthHandler()
    }

    @ExperimentalCoroutinesApi
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `when authorize() success and autoRefreshTokenWhenExpired is enabled`() {
        val expectedAuthJwt = AuthJwt(AuthTest.JWT_TOKEN, AuthTest.REFRESH_TOKEN)

        subject.authorize(AuthTest.AUTH_CODE, AuthTest.REDIRECT_URI, AuthTest.CODE_VERIFIER)

        coVerify {
            mockWebMessagingApi.fetchAuthJwt(
                AuthTest.AUTH_CODE,
                AuthTest.REDIRECT_URI,
                AuthTest.CODE_VERIFIER
            )
        }
        verify { mockEventHandler.onEvent(Event.Authorized) }
        assertThat(subject.jwt).isEqualTo(expectedAuthJwt.jwt)
        assertThat(fakeVault.authRefreshToken).isEqualTo(expectedAuthJwt.refreshToken)
        assertThat(fakeVault.token).isNotEmpty()
    }

    @Test
    fun `when authorize() success and autoRefreshTokenWhenExpired is disabled`() {
        subject = buildAuthHandler(false)

        val expectedAuthJwt = AuthJwt(AuthTest.JWT_TOKEN, NO_REFRESH_TOKEN)

        subject.authorize(AuthTest.AUTH_CODE, AuthTest.REDIRECT_URI, AuthTest.CODE_VERIFIER)

        coVerify {
            mockWebMessagingApi.fetchAuthJwt(
                AuthTest.AUTH_CODE,
                AuthTest.REDIRECT_URI,
                AuthTest.CODE_VERIFIER
            )
        }
        verify { mockEventHandler.onEvent(Event.Authorized) }
        assertThat(subject.jwt).isEqualTo(expectedAuthJwt.jwt)
        assertThat(fakeVault.authRefreshToken).isEqualTo(expectedAuthJwt.refreshToken)
    }

    @Test
    fun `when authorize() success but refreshToken is null`() {
        coEvery { mockWebMessagingApi.fetchAuthJwt(any(), any(), any()) } returns
            Result.Success(AuthJwt(AuthTest.JWT_TOKEN, null))
        subject = buildAuthHandler()

        val expectedAuthJwt = AuthJwt(AuthTest.JWT_TOKEN, NO_REFRESH_TOKEN)

        subject.authorize(AuthTest.AUTH_CODE, AuthTest.REDIRECT_URI, AuthTest.CODE_VERIFIER)

        coVerify {
            mockWebMessagingApi.fetchAuthJwt(
                AuthTest.AUTH_CODE,
                AuthTest.REDIRECT_URI,
                AuthTest.CODE_VERIFIER
            )
        }
        verify { mockEventHandler.onEvent(Event.Authorized) }
        assertThat(subject.jwt).isEqualTo(expectedAuthJwt.jwt)
        assertThat(fakeVault.authRefreshToken).isEqualTo(expectedAuthJwt.refreshToken)
    }

    @Test
    fun `when authorize() failure`() {
        val expectedErrorCode = ErrorCode.AuthFailed
        val expectedErrorMessage = ErrorTest.MESSAGE
        val expectedCorrectiveAction = CorrectiveAction.ReAuthenticate

        coEvery { mockWebMessagingApi.fetchAuthJwt(any(), any(), any()) } returns Result.Failure(
            ErrorCode.AuthFailed,
            ErrorTest.MESSAGE
        )

        val expectedAuthJwt = AuthJwt(NO_JWT, NO_REFRESH_TOKEN)

        subject.authorize(AuthTest.AUTH_CODE, AuthTest.REDIRECT_URI, AuthTest.CODE_VERIFIER)

        coVerify {
            mockWebMessagingApi.fetchAuthJwt(
                AuthTest.AUTH_CODE,
                AuthTest.REDIRECT_URI,
                AuthTest.CODE_VERIFIER
            )
        }
        verify {
            mockLogger.e(capture(logSlot))
            mockEventHandler.onEvent(
                Event.Error(
                    expectedErrorCode,
                    expectedErrorMessage,
                    expectedCorrectiveAction
                )
            )
        }
        assertThat(subject.jwt).isEqualTo(expectedAuthJwt.jwt)
        assertThat(fakeVault.authRefreshToken).isEqualTo(expectedAuthJwt.refreshToken)
        assertThat(logSlot.captured.invoke()).isEqualTo(LogMessages.requestError("fetchAuthJwt()", ErrorCode.AuthFailed, expectedErrorMessage))
    }

    @Test
    fun `when authorize() failure with CancellationException`() {
        coEvery { mockWebMessagingApi.fetchAuthJwt(any(), any(), any()) } returns Result.Failure(
            ErrorCode.CancellationError,
            ErrorTest.MESSAGE
        )

        subject.authorize(AuthTest.AUTH_CODE, AuthTest.REDIRECT_URI, AuthTest.CODE_VERIFIER)

        verify { mockLogger.w(capture(logSlot)) }
        verify(exactly = 0) {
            mockEventHandler.onEvent(
                Event.Error(
                    ErrorCode.CancellationError,
                    ErrorTest.MESSAGE,
                    CorrectiveAction.ReAuthenticate
                )
            )
        }
        assertThat(logSlot.captured.invoke()).isEqualTo(LogMessages.cancellationExceptionRequestName("fetchAuthJwt()"))
    }

    @Test
    fun `when authorized and logout() success`() {
        authorize()

        subject.logout()

        coVerify {
            mockWebMessagingApi.logoutFromAuthenticatedSession(AuthTest.JWT_TOKEN)
        }
    }

    @Test
    fun `when logout() failed because of invalid jwt`() {
        val expectedErrorCode = ErrorCode.AuthLogoutFailed
        val expectedErrorMessage = ErrorTest.MESSAGE
        val expectedCorrectiveAction = CorrectiveAction.ReAuthenticate

        subject.logout()

        coVerify {
            mockWebMessagingApi.logoutFromAuthenticatedSession(NO_JWT)
        }
        verify {
            mockLogger.e(capture(logSlot))
            mockEventHandler.onEvent(
                Event.Error(
                    expectedErrorCode,
                    expectedErrorMessage,
                    expectedCorrectiveAction
                )
            )
        }
        assertThat(logSlot.captured.invoke()).isEqualTo(LogMessages.requestError("logout()", ErrorCode.AuthLogoutFailed, expectedErrorMessage))
    }

    @Test
    fun `when authorized and logout() failed because of 401 but autoRefreshTokenWhenExpired is disabled`() {
        subject = buildAuthHandler(givenAutoRefreshTokenWhenExpired = false)
        authorize()
        coEvery { mockWebMessagingApi.logoutFromAuthenticatedSession(AuthTest.JWT_TOKEN) } returns Result.Failure(
            ErrorCode.ClientResponseError(401),
            ErrorTest.MESSAGE
        )
        val expectedErrorCode = ErrorCode.ClientResponseError(401)
        val expectedErrorMessage = ErrorTest.MESSAGE
        val expectedCorrectiveAction = CorrectiveAction.ReAuthenticate

        subject.logout()

        coVerify {
            mockWebMessagingApi.logoutFromAuthenticatedSession(AuthTest.JWT_TOKEN)
        }
        verify {
            mockLogger.e(capture(logSlot))
            mockEventHandler.onEvent(
                Event.Error(
                    expectedErrorCode,
                    expectedErrorMessage,
                    expectedCorrectiveAction
                )
            )
        }
        assertThat(logSlot.captured.invoke()).isEqualTo(LogMessages.requestError("logout()", ErrorCode.ClientResponseError(401), expectedErrorMessage))
    }

    @Test
    fun `when authorized and logout() failed because of 401 and autoRefreshTokenWhenExpired is enabled but refreshToken() success`() {
        authorize()
        coEvery { mockWebMessagingApi.logoutFromAuthenticatedSession(any()) } returns Result.Failure(
            ErrorCode.ClientResponseError(401),
            ErrorTest.MESSAGE
        )
        val expectedErrorCode = ErrorCode.ClientResponseError(401)
        val expectedErrorMessage = ErrorTest.MESSAGE
        val expectedCorrectiveAction = CorrectiveAction.ReAuthenticate

        subject.logout()

        coVerify(exactly = 1) {
            mockWebMessagingApi.logoutFromAuthenticatedSession(AuthTest.JWT_TOKEN)
        }
        coVerify(exactly = MAX_LOGOUT_ATTEMPTS) {
            mockWebMessagingApi.logoutFromAuthenticatedSession(AuthTest.REFRESHED_JWT_TOKEN)
            mockWebMessagingApi.refreshAuthJwt(AuthTest.REFRESH_TOKEN)
        }
        verify(exactly = 1) {
            mockLogger.e(capture(logSlot))
            mockEventHandler.onEvent(
                Event.Error(
                    expectedErrorCode,
                    expectedErrorMessage,
                    expectedCorrectiveAction
                )
            )
        }
        assertThat(logSlot.captured.invoke()).isEqualTo(LogMessages.requestError("logout()", ErrorCode.ClientResponseError(401), expectedErrorMessage))
    }

    @Test
    fun `when authorized and logout() failed because of 401 and autoRefreshTokenWhenExpired is enabled but refreshToken() fails`() {
        authorize()
        coEvery { mockWebMessagingApi.logoutFromAuthenticatedSession(AuthTest.JWT_TOKEN) } returns Result.Failure(
            ErrorCode.ClientResponseError(401),
            ErrorTest.MESSAGE
        )
        coEvery { mockWebMessagingApi.refreshAuthJwt(AuthTest.REFRESH_TOKEN) } returns Result.Failure(
            ErrorCode.RefreshAuthTokenFailure,
            ErrorTest.MESSAGE,
        )
        val expectedAuthJwt = AuthJwt(NO_JWT, NO_REFRESH_TOKEN)
        val expectedErrorCode = ErrorCode.RefreshAuthTokenFailure
        val expectedErrorMessage = ErrorTest.MESSAGE
        val expectedCorrectiveAction = CorrectiveAction.ReAuthenticate

        subject.logout()

        coVerify(exactly = 1) {
            mockWebMessagingApi.logoutFromAuthenticatedSession(AuthTest.JWT_TOKEN)
            mockWebMessagingApi.refreshAuthJwt(AuthTest.REFRESH_TOKEN)
            mockEventHandler.onEvent(
                Event.Error(
                    expectedErrorCode,
                    expectedErrorMessage,
                    expectedCorrectiveAction
                )
            )
        }
        assertThat(subject.jwt).isEqualTo(expectedAuthJwt.jwt)
        assertThat(fakeVault.authRefreshToken).isEqualTo(expectedAuthJwt.refreshToken)
    }

    @Test
    fun `when not authorized and refreshToken()`() {
        val mockCallback = slot<Result<Empty>>()
        val expectedAuthJwt = AuthJwt(NO_JWT, NO_REFRESH_TOKEN)
        val expectedErrorCode = ErrorCode.RefreshAuthTokenFailure
        val expectedErrorMessage = ErrorMessage.NoRefreshToken

        subject.refreshToken { result -> mockCallback.captured = result }

        verify { mockLogger.e(capture(logSlot)) }
        coVerify(exactly = 0) { mockWebMessagingApi.refreshAuthJwt(AuthTest.REFRESH_TOKEN) }
        assertThat(mockCallback.captured).isEqualTo(Result.Failure(expectedErrorCode, expectedErrorMessage))
        assertThat(subject.jwt).isEqualTo(expectedAuthJwt.jwt)
        assertThat(fakeVault.authRefreshToken).isEqualTo(expectedAuthJwt.refreshToken)
        assertThat(logSlot.captured.invoke()).isEqualTo(LogMessages.couldNotRefreshAuthToken(expectedErrorMessage))
    }

    @Test
    fun `when authorized and refreshToken() but autoRefreshTokenWhenExpired is disabled`() {
        subject = buildAuthHandler(givenAutoRefreshTokenWhenExpired = false)
        authorize()
        val mockCallback = slot<Result<Empty>>()
        val expectedAuthJwt = AuthJwt(AuthTest.JWT_TOKEN, NO_REFRESH_TOKEN)
        val expectedErrorCode = ErrorCode.RefreshAuthTokenFailure
        val expectedErrorMessage = ErrorMessage.AutoRefreshTokenDisabled

        subject.refreshToken { result -> mockCallback.captured = result }

        verify { mockLogger.e(capture(logSlot)) }
        coVerify(exactly = 0) { mockWebMessagingApi.refreshAuthJwt(AuthTest.REFRESH_TOKEN) }
        assertThat(mockCallback.captured).isEqualTo(Result.Failure(expectedErrorCode, expectedErrorMessage))
        assertThat(subject.jwt).isEqualTo(expectedAuthJwt.jwt)
        assertThat(fakeVault.authRefreshToken).isEqualTo(expectedAuthJwt.refreshToken)
        assertThat(logSlot.captured.invoke()).isEqualTo(LogMessages.couldNotRefreshAuthToken(expectedErrorMessage))
    }

    @Test
    fun `when authorized and refreshToken() success`() {
        authorize()
        val mockCallback = slot<Result<Empty>>()
        val expectedAuthJwt = AuthJwt(AuthTest.REFRESHED_JWT_TOKEN, AuthTest.REFRESH_TOKEN)

        subject.refreshToken { result -> mockCallback.captured = result }

        coVerify {
            mockWebMessagingApi.refreshAuthJwt(AuthTest.REFRESH_TOKEN)
            mockLogger.i(capture(logSlot))
        }
        assertThat(mockCallback.captured).isInstanceOf(Result.Success::class.java)
        assertThat((mockCallback.captured as Result.Success<Empty>).value).isInstanceOf(Empty::class.java)
        assertThat(subject.jwt).isEqualTo(expectedAuthJwt.jwt)
        assertThat(fakeVault.authRefreshToken).isEqualTo(expectedAuthJwt.refreshToken)
        assertThat(logSlot.captured.invoke()).isEqualTo(LogMessages.REFRESH_AUTH_TOKEN_SUCCESS)
    }

    @Test
    fun `when authorized and refreshToken() failure`() {
        authorize()
        coEvery { mockWebMessagingApi.refreshAuthJwt(any()) } returns Result.Failure(
            ErrorCode.RefreshAuthTokenFailure,
            ErrorTest.MESSAGE,
        )
        val mockCallback = slot<Result<Empty>>()
        val expectedAuthJwt = AuthJwt(NO_JWT, NO_REFRESH_TOKEN)

        subject.refreshToken { result -> mockCallback.captured = result }

        coVerify {
            mockWebMessagingApi.refreshAuthJwt(AuthTest.REFRESH_TOKEN)
            mockLogger.e(capture(logSlot))
        }
        assertThat(mockCallback.captured).isEqualTo(Result.Failure(ErrorCode.RefreshAuthTokenFailure, ErrorTest.MESSAGE))
        assertThat(subject.jwt).isEqualTo(expectedAuthJwt.jwt)
        assertThat(fakeVault.authRefreshToken).isEqualTo(expectedAuthJwt.refreshToken)
        assertThat(logSlot.captured.invoke()).isEqualTo(LogMessages.couldNotRefreshAuthToken(ErrorTest.MESSAGE))
    }

    @Test
    fun `when authorized and then clear()`() {
        authorize()
        val expectedAuthJwt = AuthJwt(NO_JWT, NO_REFRESH_TOKEN)

        subject.clear()

        assertThat(subject.jwt).isEqualTo(expectedAuthJwt.jwt)
        assertThat(fakeVault.authRefreshToken).isEqualTo(expectedAuthJwt.refreshToken)
    }

    @Test
    fun `when serialize AuthJwt`() {
        val givenAuthJwt = AuthJwt(AuthTest.JWT_TOKEN, AuthTest.REFRESH_TOKEN)
        val givenAuthJwtWithoutRefreshToken = AuthJwt(AuthTest.JWT_TOKEN)
        val expectedAuthJwtAsJson = """{"jwt":"jwt_Token","refreshToken":"refresh_token"}"""
        val expectedAuthJwtWithoutRefreshTokenAsJson = """{"jwt":"jwt_Token"}"""

        val authJwtAsJson = WebMessagingJson.json.encodeToString(givenAuthJwt)
        val authJwtWithoutRefreshTokenAsJson = WebMessagingJson.json.encodeToString(givenAuthJwtWithoutRefreshToken)

        assertThat(authJwtAsJson).isEqualTo(expectedAuthJwtAsJson)
        assertThat(authJwtWithoutRefreshTokenAsJson).isEqualTo(expectedAuthJwtWithoutRefreshTokenAsJson)
    }

    @Test
    fun `when serialize RefreshToken`() {
        val refreshToken = RefreshToken(AuthTest.REFRESH_TOKEN)
        val expectedRefreshTokenAsJson = """{"refreshToken":"${AuthTest.REFRESH_TOKEN}"}"""

        val encoded = WebMessagingJson.json.encodeToString(refreshToken)
        val decoded = WebMessagingJson.json.decodeFromString<RefreshToken>(expectedRefreshTokenAsJson)

        assertThat(encoded).isEqualTo(expectedRefreshTokenAsJson)
        assertThat(decoded).isEqualTo(refreshToken)
    }

    @Test
    fun `validate default constructor of AuthJwt`() {
        val authJwt = AuthJwt(jwt = AuthTest.JWT_AUTH_URL)

        authJwt.run {
            assertThat(jwt).isEqualTo(AuthTest.JWT_AUTH_URL)
            assertThat(refreshToken).isNull()
        }
    }

    private fun buildAuthHandler(
        givenAutoRefreshTokenWhenExpired: Boolean = true,
        isAuthEnabled: suspend () -> Boolean = { true }
    ): AuthHandlerImpl {
        return AuthHandlerImpl(
            autoRefreshTokenWhenExpired = givenAutoRefreshTokenWhenExpired,
            eventHandler = mockEventHandler,
            api = mockWebMessagingApi,
            vault = fakeVault,
            log = mockLogger,
            isAuthEnabled = isAuthEnabled
        )
    }

    private fun authorize() {
        subject.authorize(AuthTest.AUTH_CODE, AuthTest.REDIRECT_URI, AuthTest.CODE_VERIFIER)
    }

    @Test
    fun `when shouldAuthorize() and no refresh token available`() {
        var callbackResult: Boolean? = null

        subject.shouldAuthorize { result -> callbackResult = result }

        assertThat(callbackResult!!).isTrue()
        coVerify(exactly = 0) { mockWebMessagingApi.refreshAuthJwt(any()) }
    }

    @Test
    fun `when shouldAuthorize() with refresh token and refresh succeeds`() {
        authorize()
        var callbackResult: Boolean? = null

        subject.shouldAuthorize { result -> callbackResult = result }

        coVerify { mockWebMessagingApi.refreshAuthJwt(AuthTest.REFRESH_TOKEN) }
        assertThat(callbackResult!!).isFalse()
        assertThat(subject.jwt).isEqualTo(AuthTest.REFRESHED_JWT_TOKEN)
    }

    @Test
    fun `when shouldAuthorize() with refresh token but refresh fails`() {
        authorize()
        coEvery { mockWebMessagingApi.refreshAuthJwt(any()) } returns Result.Failure(
            ErrorCode.RefreshAuthTokenFailure,
            ErrorTest.MESSAGE
        )
        var callbackResult: Boolean? = null
        val expectedAuthJwt = AuthJwt(NO_JWT, NO_REFRESH_TOKEN)

        subject.shouldAuthorize { result -> callbackResult = result }

        coVerify { mockWebMessagingApi.refreshAuthJwt(AuthTest.REFRESH_TOKEN) }
        assertThat(callbackResult!!).isTrue()
        assertThat(subject.jwt).isEqualTo(expectedAuthJwt.jwt)
        assertThat(fakeVault.authRefreshToken).isEqualTo(expectedAuthJwt.refreshToken)
    }

    @Test
    fun `when shouldAuthorize() and auth is disabled in deployment config`() = runTest {
        subject = buildAuthHandler(isAuthEnabled = { false })

        subject.shouldAuthorize { result ->
            assertFalse(result)
            coVerify(exactly = 0) { mockWebMessagingApi.refreshAuthJwt(any()) }
        }
    }

    @Test
    fun `when shouldAuthorize() and auth is enabled in deployment config`() {
        var callbackResult: Boolean? = null

        subject.shouldAuthorize { result -> callbackResult = result }

        assertThat(callbackResult!!).isTrue()
        coVerify(exactly = 0) { mockWebMessagingApi.refreshAuthJwt(any()) }
    }
}
