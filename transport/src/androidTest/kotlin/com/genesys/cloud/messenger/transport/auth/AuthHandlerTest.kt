package com.genesys.cloud.messenger.transport.auth

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import com.genesys.cloud.messenger.transport.core.CorrectiveAction
import com.genesys.cloud.messenger.transport.core.Empty
import com.genesys.cloud.messenger.transport.core.ErrorCode
import com.genesys.cloud.messenger.transport.core.ErrorMessage
import com.genesys.cloud.messenger.transport.core.FakeVault
import com.genesys.cloud.messenger.transport.core.Result
import com.genesys.cloud.messenger.transport.core.events.Event
import com.genesys.cloud.messenger.transport.core.events.EventHandler
import com.genesys.cloud.messenger.transport.network.WebMessagingApi
import com.genesys.cloud.messenger.transport.shyrka.WebMessagingJson
import com.genesys.cloud.messenger.transport.util.AUTH_REFRESH_TOKEN_KEY
import com.genesys.cloud.messenger.transport.util.TOKEN_KEY
import com.genesys.cloud.messenger.transport.util.VAULT_KEY
import com.genesys.cloud.messenger.transport.util.Vault
import com.genesys.cloud.messenger.transport.utility.AuthTest
import com.genesys.cloud.messenger.transport.utility.ErrorTest
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
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import org.junit.After
import org.junit.Before
import org.junit.Test

class AuthHandlerTest {

    @MockK(relaxed = true)
    private val mockEventHandler: EventHandler = mockk(relaxed = true)

    @MockK(relaxed = true)
    private val mockWebMessagingApi: WebMessagingApi = mockk {
        coEvery {
            fetchAuthJwt(
                AuthTest.AuthCode,
                AuthTest.RedirectUri,
                AuthTest.CodeVerifier,
            )
        } returns Result.Success(AuthJwt(AuthTest.JwtToken, AuthTest.RefreshToken))

        coEvery {
            logoutFromAuthenticatedSession(AuthTest.JwtToken)
        } returns Result.Success(Empty())

        coEvery {
            logoutFromAuthenticatedSession(NO_JWT)
        } returns Result.Failure(
            ErrorCode.AuthLogoutFailed,
            ErrorTest.Message,
        )

        coEvery { refreshAuthJwt(AuthTest.RefreshToken) } returns Result.Success(
            AuthJwt(AuthTest.RefreshedJWTToken, null)
        )

        coEvery { refreshAuthJwt(NO_REFRESH_TOKEN) } returns Result.Failure(
            ErrorCode.RefreshAuthTokenFailure,
            ErrorTest.Message,
        )
    }

    private val fakeVault: FakeVault = FakeVault(
        Vault.Keys(
            vaultKey = VAULT_KEY,
            tokenKey = TOKEN_KEY,
            authRefreshTokenKey = AUTH_REFRESH_TOKEN_KEY,
        )
    )
    private val dispatcher: CoroutineDispatcher = Dispatchers.Unconfined

    private var subject = buildAuthHandler()

    @ExperimentalCoroutinesApi
    @Before
    fun setup() {
        MockKAnnotations.init(this)
        Dispatchers.setMain(dispatcher)
    }

    @ExperimentalCoroutinesApi
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `when authorize() success and autoRefreshTokenWhenExpired is enabled`() {
        val expectedAuthJwt = AuthJwt(AuthTest.JwtToken, AuthTest.RefreshToken)

        subject.authorize(AuthTest.AuthCode, AuthTest.RedirectUri, AuthTest.CodeVerifier)

        coVerify {
            mockWebMessagingApi.fetchAuthJwt(
                AuthTest.AuthCode,
                AuthTest.RedirectUri,
                AuthTest.CodeVerifier
            )
        }
        verify { mockEventHandler.onEvent(Event.Authorized) }
        assertThat(subject.jwt).isEqualTo(expectedAuthJwt.jwt)
        assertThat(fakeVault.authRefreshToken).isEqualTo(expectedAuthJwt.refreshToken)
    }

    @Test
    fun `when authorize() success and autoRefreshTokenWhenExpired is disabled`() {
        subject = buildAuthHandler(false)

        val expectedAuthJwt = AuthJwt(AuthTest.JwtToken, NO_REFRESH_TOKEN)

        subject.authorize(AuthTest.AuthCode, AuthTest.RedirectUri, AuthTest.CodeVerifier)

        coVerify {
            mockWebMessagingApi.fetchAuthJwt(
                AuthTest.AuthCode,
                AuthTest.RedirectUri,
                AuthTest.CodeVerifier
            )
        }
        verify { mockEventHandler.onEvent(Event.Authorized) }
        assertThat(subject.jwt).isEqualTo(expectedAuthJwt.jwt)
        assertThat(fakeVault.authRefreshToken).isEqualTo(expectedAuthJwt.refreshToken)
    }

    @Test
    fun `when authorize() success but refreshToken is null`() {
        coEvery { mockWebMessagingApi.fetchAuthJwt(any(), any(), any()) } returns
            Result.Success(AuthJwt(AuthTest.JwtToken, null))
        subject = buildAuthHandler()

        val expectedAuthJwt = AuthJwt(AuthTest.JwtToken, NO_REFRESH_TOKEN)

        subject.authorize(AuthTest.AuthCode, AuthTest.RedirectUri, AuthTest.CodeVerifier)

        coVerify {
            mockWebMessagingApi.fetchAuthJwt(
                AuthTest.AuthCode,
                AuthTest.RedirectUri,
                AuthTest.CodeVerifier
            )
        }
        verify { mockEventHandler.onEvent(Event.Authorized) }
        assertThat(subject.jwt).isEqualTo(expectedAuthJwt.jwt)
        assertThat(fakeVault.authRefreshToken).isEqualTo(expectedAuthJwt.refreshToken)
    }

    @Test
    fun `when authorize() failure`() {
        val expectedErrorCode = ErrorCode.AuthFailed
        val expectedErrorMessage = ErrorTest.Message
        val expectedCorrectiveAction = CorrectiveAction.ReAuthenticate

        coEvery { mockWebMessagingApi.fetchAuthJwt(any(), any(), any()) } returns Result.Failure(
            ErrorCode.AuthFailed,
            ErrorTest.Message
        )

        val expectedAuthJwt = AuthJwt(NO_JWT, NO_REFRESH_TOKEN)

        subject.authorize(AuthTest.AuthCode, AuthTest.RedirectUri, AuthTest.CodeVerifier)

        coVerify {
            mockWebMessagingApi.fetchAuthJwt(
                AuthTest.AuthCode,
                AuthTest.RedirectUri,
                AuthTest.CodeVerifier
            )
        }
        verify {
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
    fun `when authorize() failure with CancellationException`() {
        coEvery { mockWebMessagingApi.fetchAuthJwt(any(), any(), any()) } returns Result.Failure(
            ErrorCode.CancellationError,
            ErrorTest.Message
        )

        subject.authorize(AuthTest.AuthCode, AuthTest.RedirectUri, AuthTest.CodeVerifier)

        verify(exactly = 0) {
            mockEventHandler.onEvent(
                Event.Error(
                    ErrorCode.CancellationError,
                    ErrorTest.Message,
                    CorrectiveAction.ReAuthenticate
                )
            )
        }
    }

    @Test
    fun `when authorized and logout() success`() {
        authorize()

        subject.logout()

        coVerify {
            mockWebMessagingApi.logoutFromAuthenticatedSession(AuthTest.JwtToken)
        }
    }

    @Test
    fun `when logout() failed because of invalid jwt`() {
        val expectedErrorCode = ErrorCode.AuthLogoutFailed
        val expectedErrorMessage = ErrorTest.Message
        val expectedCorrectiveAction = CorrectiveAction.ReAuthenticate

        subject.logout()

        coVerify {
            mockWebMessagingApi.logoutFromAuthenticatedSession(NO_JWT)
        }
        verify {
            mockEventHandler.onEvent(
                Event.Error(
                    expectedErrorCode,
                    expectedErrorMessage,
                    expectedCorrectiveAction
                )
            )
        }
    }

    @Test
    fun `when authorized and logout() failed because of 401 but autoRefreshTokenWhenExpired is disabled`() {
        subject = buildAuthHandler(givenAutoRefreshTokenWhenExpired = false)
        authorize()
        coEvery { mockWebMessagingApi.logoutFromAuthenticatedSession(AuthTest.JwtToken) } returns Result.Failure(
            ErrorCode.ClientResponseError(401),
            ErrorTest.Message
        )
        val expectedErrorCode = ErrorCode.ClientResponseError(401)
        val expectedErrorMessage = ErrorTest.Message
        val expectedCorrectiveAction = CorrectiveAction.ReAuthenticate

        subject.logout()

        coVerify {
            mockWebMessagingApi.logoutFromAuthenticatedSession(AuthTest.JwtToken)
        }
        verify {
            mockEventHandler.onEvent(
                Event.Error(
                    expectedErrorCode,
                    expectedErrorMessage,
                    expectedCorrectiveAction
                )
            )
        }
    }

    @Test
    fun `when authorized and logout() failed because of 401 and autoRefreshTokenWhenExpired is enabled but refreshToken() success`() {
        authorize()
        coEvery { mockWebMessagingApi.logoutFromAuthenticatedSession(any()) } returns Result.Failure(
            ErrorCode.ClientResponseError(401),
            ErrorTest.Message
        )
        val expectedErrorCode = ErrorCode.ClientResponseError(401)
        val expectedErrorMessage = ErrorTest.Message
        val expectedCorrectiveAction = CorrectiveAction.ReAuthenticate

        subject.logout()

        coVerify(exactly = 1) {
            mockWebMessagingApi.logoutFromAuthenticatedSession(AuthTest.JwtToken)
        }
        coVerify(exactly = MAX_LOGOUT_ATTEMPTS) {
            mockWebMessagingApi.logoutFromAuthenticatedSession(AuthTest.RefreshedJWTToken)
            mockWebMessagingApi.refreshAuthJwt(AuthTest.RefreshToken)
        }
        verify(exactly = 1) {
            mockEventHandler.onEvent(
                Event.Error(
                    expectedErrorCode,
                    expectedErrorMessage,
                    expectedCorrectiveAction
                )
            )
        }
    }

    @Test
    fun `when authorized and logout() failed because of 401 and autoRefreshTokenWhenExpired is enabled but refreshToken() fails`() {
        authorize()
        coEvery { mockWebMessagingApi.logoutFromAuthenticatedSession(AuthTest.JwtToken) } returns Result.Failure(
            ErrorCode.ClientResponseError(401),
            ErrorTest.Message
        )
        coEvery { mockWebMessagingApi.refreshAuthJwt(AuthTest.RefreshToken) } returns Result.Failure(
            ErrorCode.RefreshAuthTokenFailure,
            ErrorTest.Message,
        )
        val expectedAuthJwt = AuthJwt(NO_JWT, NO_REFRESH_TOKEN)
        val expectedErrorCode = ErrorCode.RefreshAuthTokenFailure
        val expectedErrorMessage = ErrorTest.Message
        val expectedCorrectiveAction = CorrectiveAction.ReAuthenticate

        subject.logout()

        coVerify(exactly = 1) {
            mockWebMessagingApi.logoutFromAuthenticatedSession(AuthTest.JwtToken)
            mockWebMessagingApi.refreshAuthJwt(AuthTest.RefreshToken)
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

        coVerify(exactly = 0) { mockWebMessagingApi.refreshAuthJwt(AuthTest.RefreshToken) }
        assertThat(mockCallback.captured).isEqualTo(Result.Failure(expectedErrorCode, expectedErrorMessage))
        assertThat(subject.jwt).isEqualTo(expectedAuthJwt.jwt)
        assertThat(fakeVault.authRefreshToken).isEqualTo(expectedAuthJwt.refreshToken)
    }

    @Test
    fun `when authorized and refreshToken() but autoRefreshTokenWhenExpired is disabled`() {
        subject = buildAuthHandler(givenAutoRefreshTokenWhenExpired = false)
        authorize()
        val mockCallback = slot<Result<Empty>>()
        val expectedAuthJwt = AuthJwt(AuthTest.JwtToken, NO_REFRESH_TOKEN)
        val expectedErrorCode = ErrorCode.RefreshAuthTokenFailure
        val expectedErrorMessage = ErrorMessage.AutoRefreshTokenDisabled

        subject.refreshToken { result -> mockCallback.captured = result }

        coVerify(exactly = 0) { mockWebMessagingApi.refreshAuthJwt(AuthTest.RefreshToken) }
        assertThat(mockCallback.captured).isEqualTo(Result.Failure(expectedErrorCode, expectedErrorMessage))
        assertThat(subject.jwt).isEqualTo(expectedAuthJwt.jwt)
        assertThat(fakeVault.authRefreshToken).isEqualTo(expectedAuthJwt.refreshToken)
    }

    @Test
    fun `when authorized and refreshToken() success`() {
        authorize()
        val mockCallback = slot<Result<Empty>>()
        val expectedAuthJwt = AuthJwt(AuthTest.RefreshedJWTToken, AuthTest.RefreshToken)

        subject.refreshToken { result -> mockCallback.captured = result }

        coVerify { mockWebMessagingApi.refreshAuthJwt(AuthTest.RefreshToken) }
        assertThat(mockCallback.captured).isInstanceOf(Result.Success::class.java)
        assertThat((mockCallback.captured as Result.Success<Empty>).value).isInstanceOf(Empty::class.java)
        assertThat(subject.jwt).isEqualTo(expectedAuthJwt.jwt)
        assertThat(fakeVault.authRefreshToken).isEqualTo(expectedAuthJwt.refreshToken)
    }

    @Test
    fun `when authorized and refreshToken() failure`() {
        authorize()
        coEvery { mockWebMessagingApi.refreshAuthJwt(any()) } returns Result.Failure(
            ErrorCode.RefreshAuthTokenFailure,
            ErrorTest.Message,
        )
        val mockCallback = slot<Result<Empty>>()
        val expectedAuthJwt = AuthJwt(NO_JWT, NO_REFRESH_TOKEN)

        subject.refreshToken { result -> mockCallback.captured = result }

        coVerify { mockWebMessagingApi.refreshAuthJwt(AuthTest.RefreshToken) }
        assertThat(mockCallback.captured).isEqualTo(Result.Failure(ErrorCode.RefreshAuthTokenFailure, ErrorTest.Message))
        assertThat(subject.jwt).isEqualTo(expectedAuthJwt.jwt)
        assertThat(fakeVault.authRefreshToken).isEqualTo(expectedAuthJwt.refreshToken)
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
    fun `AuthJwt serialization test`() {
        val givenAuthJwt = AuthJwt("jwt", "refreshToken")
        val expectedAuthJwt = AuthJwt("jwt", "refreshToken")
        val expectedAuthJwtAsJson = """{"jwt":"jwt","refreshToken":"refreshToken"}"""

        val json = WebMessagingJson.json.encodeToString(givenAuthJwt)
        val deserializedAuthJwt = WebMessagingJson.json.decodeFromString<AuthJwt>(json)

        assertThat(json).isEqualTo(expectedAuthJwtAsJson)
        assertThat(deserializedAuthJwt.jwt).isEqualTo(expectedAuthJwt.jwt)
        assertThat(deserializedAuthJwt.refreshToken).isEqualTo(expectedAuthJwt.refreshToken)
    }

    @Test
    fun `RefreshToken serialization test`() {
        val givenRefreshToken = RefreshToken("refreshToken")
        val expectedRefreshToken = RefreshToken("refreshToken")
        val expectedRefreshTokenAsJson = """{"refreshToken":"refreshToken"}"""

        val json = WebMessagingJson.json.encodeToString(givenRefreshToken)
        val deserializedRefreshToken = WebMessagingJson.json.decodeFromString<RefreshToken>(json)

        assertThat(json).isEqualTo(expectedRefreshTokenAsJson)
        assertThat(deserializedRefreshToken.refreshToken).isEqualTo(expectedRefreshToken.refreshToken)
    }

    private fun buildAuthHandler(givenAutoRefreshTokenWhenExpired: Boolean = true): AuthHandlerImpl {
        return AuthHandlerImpl(
            autoRefreshTokenWhenExpired = givenAutoRefreshTokenWhenExpired,
            eventHandler = mockEventHandler,
            api = mockWebMessagingApi,
            vault = fakeVault,
            log = mockk(relaxed = true),
        )
    }

    private fun authorize() {
        subject.authorize(AuthTest.AuthCode, AuthTest.RedirectUri, AuthTest.CodeVerifier)
    }
}
