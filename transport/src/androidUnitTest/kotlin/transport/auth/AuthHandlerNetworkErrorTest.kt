package transport.auth

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.genesys.cloud.messenger.transport.auth.AuthHandlerImpl
import com.genesys.cloud.messenger.transport.auth.AuthJwt
import com.genesys.cloud.messenger.transport.auth.NO_JWT
import com.genesys.cloud.messenger.transport.auth.NO_REFRESH_TOKEN
import com.genesys.cloud.messenger.transport.core.CorrectiveAction
import com.genesys.cloud.messenger.transport.core.Empty
import com.genesys.cloud.messenger.transport.core.ErrorCode
import com.genesys.cloud.messenger.transport.core.Result
import com.genesys.cloud.messenger.transport.core.events.Event
import com.genesys.cloud.messenger.transport.core.events.EventHandler
import com.genesys.cloud.messenger.transport.core.toCorrectiveAction
import com.genesys.cloud.messenger.transport.network.WebMessagingApi
import com.genesys.cloud.messenger.transport.util.logs.Log
import com.genesys.cloud.messenger.transport.util.logs.LogMessages
import com.genesys.cloud.messenger.transport.utility.AuthTest
import com.genesys.cloud.messenger.transport.utility.ErrorTest
import com.genesys.cloud.messenger.transport.utility.FakeVault
import com.genesys.cloud.messenger.transport.utility.TestValues
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

class AuthHandlerNetworkErrorTest {
    @MockK(relaxed = true)
    private val mockEventHandler: EventHandler = mockk(relaxed = true)
    private val mockLogger: Log = mockk(relaxed = true)
    private val logSlot = slot<() -> String>()

    @MockK(relaxed = true)
    private val mockWebMessagingApi: WebMessagingApi = mockk()

    private val fakeVault: FakeVault = FakeVault(TestValues.vaultKeys)
    private val dispatcher: CoroutineDispatcher = Dispatchers.Unconfined

    private lateinit var subject: AuthHandlerImpl

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
    fun `when fetchAuthJwt fails with NetworkDisabled error`() {
        coEvery { mockWebMessagingApi.fetchAuthJwt(any(), any(), any()) } returns
            Result.Failure(ErrorCode.NetworkDisabled, ErrorTest.MESSAGE)
        subject = buildAuthHandler()

        val expectedErrorCode = ErrorCode.NetworkDisabled
        val expectedErrorMessage = ErrorTest.MESSAGE
        val expectedCorrectiveAction = CorrectiveAction.CheckNetwork

        subject.authorize(AuthTest.AUTH_CODE, AuthTest.REDIRECT_URI, AuthTest.CODE_VERIFIER)

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
        assertThat(logSlot.captured.invoke()).isEqualTo(
            LogMessages.requestError("fetchAuthJwt()", expectedErrorCode, expectedErrorMessage)
        )
    }

    @Test
    fun `when fetchAuthJwt with idToken fails with NetworkDisabled error`() {
        coEvery { mockWebMessagingApi.fetchAuthJwt(idToken = any(), nonce = any()) } returns
                Result.Failure(ErrorCode.NetworkDisabled, ErrorTest.MESSAGE)
        subject = buildAuthHandler()

        val expectedErrorCode = ErrorCode.NetworkDisabled
        val expectedErrorMessage = ErrorTest.MESSAGE
        val expectedCorrectiveAction = CorrectiveAction.CheckNetwork

        subject.authorizeImplicit(idToken = AuthTest.ID_TOKEN, nonce = AuthTest.NONCE)

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
        assertThat(logSlot.captured.invoke()).isEqualTo(
            LogMessages.requestError("authorizeImplicit()", expectedErrorCode, expectedErrorMessage)
        )
    }

    @Test
    fun `when refreshAuthJwt fails with NetworkDisabled error - auth tokens are preserved`() {
        // First authorize successfully
        coEvery {
            mockWebMessagingApi.fetchAuthJwt(
                AuthTest.AUTH_CODE,
                AuthTest.REDIRECT_URI,
                AuthTest.CODE_VERIFIER
            )
        } returns Result.Success(AuthJwt(AuthTest.JWT_TOKEN, AuthTest.REFRESH_TOKEN))
        subject = buildAuthHandler()
        subject.authorize(AuthTest.AUTH_CODE, AuthTest.REDIRECT_URI, AuthTest.CODE_VERIFIER)

        // Then refresh fails with network error
        coEvery { mockWebMessagingApi.refreshAuthJwt(AuthTest.REFRESH_TOKEN) } returns
            Result.Failure(ErrorCode.NetworkDisabled, ErrorTest.MESSAGE)

        val mockCallback = slot<Result<Empty>>()
        val expectedErrorCode = ErrorCode.NetworkDisabled
        val expectedErrorMessage = ErrorTest.MESSAGE
        val expectedCorrectiveAction = CorrectiveAction.CheckNetwork

        subject.refreshToken { result -> mockCallback.captured = result }

        // Auth tokens should NOT be cleared
        assertThat(subject.jwt).isEqualTo(AuthTest.JWT_TOKEN)
        assertThat(fakeVault.authRefreshToken).isEqualTo(AuthTest.REFRESH_TOKEN)

        // Error event should be fired with CheckNetwork corrective action
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
        assertThat(mockCallback.captured).isEqualTo(Result.Failure(expectedErrorCode, expectedErrorMessage))
        assertThat(logSlot.captured.invoke()).isEqualTo(LogMessages.couldNotRefreshAuthToken(expectedErrorMessage))
    }

    @Test
    fun `when refreshAuthJwt fails with RefreshAuthTokenFailure error - auth tokens are cleared`() {
        // First authorize successfully
        coEvery {
            mockWebMessagingApi.fetchAuthJwt(
                AuthTest.AUTH_CODE,
                AuthTest.REDIRECT_URI,
                AuthTest.CODE_VERIFIER
            )
        } returns Result.Success(AuthJwt(AuthTest.JWT_TOKEN, AuthTest.REFRESH_TOKEN))
        subject = buildAuthHandler()
        subject.authorize(AuthTest.AUTH_CODE, AuthTest.REDIRECT_URI, AuthTest.CODE_VERIFIER)

        // Then refresh fails with auth failure (not network)
        coEvery { mockWebMessagingApi.refreshAuthJwt(AuthTest.REFRESH_TOKEN) } returns
            Result.Failure(ErrorCode.RefreshAuthTokenFailure, ErrorTest.MESSAGE)

        val mockCallback = slot<Result<Empty>>()
        val expectedAuthJwt = AuthJwt(NO_JWT, NO_REFRESH_TOKEN)
        val expectedErrorCode = ErrorCode.RefreshAuthTokenFailure
        val expectedErrorMessage = ErrorTest.MESSAGE
        val expectedCorrectiveAction = CorrectiveAction.ReAuthenticate

        subject.refreshToken { result -> mockCallback.captured = result }

        // Auth tokens SHOULD be cleared for non-network errors
        assertThat(subject.jwt).isEqualTo(expectedAuthJwt.jwt)
        assertThat(fakeVault.authRefreshToken).isEqualTo(expectedAuthJwt.refreshToken)

        // Error event should be fired with ReAuthenticate corrective action
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
        assertThat(mockCallback.captured).isEqualTo(Result.Failure(expectedErrorCode, expectedErrorMessage))
    }

    @Test
    fun `when ErrorCode toCorrectiveAction maps NetworkDisabled to CheckNetwork`() {
        val correctiveAction = ErrorCode.NetworkDisabled.toCorrectiveAction()

        assertThat(correctiveAction).isEqualTo(CorrectiveAction.CheckNetwork)
        assertThat(correctiveAction.message).isEqualTo("Check your internet connection and try again.")
    }

    @Test
    fun `when ErrorCode toCorrectiveAction maps RefreshAuthTokenFailure to ReAuthenticate`() {
        val correctiveAction = ErrorCode.RefreshAuthTokenFailure.toCorrectiveAction()

        assertThat(correctiveAction).isEqualTo(CorrectiveAction.ReAuthenticate)
        assertThat(correctiveAction.message).isEqualTo("User re-authentication is required.")
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
}
