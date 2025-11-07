package com.genesys.cloud.messenger.transport.auth

import com.genesys.cloud.messenger.transport.core.CorrectiveAction
import com.genesys.cloud.messenger.transport.core.Empty
import com.genesys.cloud.messenger.transport.core.ErrorCode
import com.genesys.cloud.messenger.transport.core.ErrorMessage
import com.genesys.cloud.messenger.transport.core.Result
import com.genesys.cloud.messenger.transport.core.events.Event
import com.genesys.cloud.messenger.transport.core.events.EventHandler
import com.genesys.cloud.messenger.transport.core.isUnauthorized
import com.genesys.cloud.messenger.transport.network.WebMessagingApi
import com.genesys.cloud.messenger.transport.util.Vault
import com.genesys.cloud.messenger.transport.util.logs.Log
import com.genesys.cloud.messenger.transport.util.logs.LogMessages
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

internal const val MAX_LOGOUT_ATTEMPTS = 3

internal class AuthHandlerImpl(
    private val autoRefreshTokenWhenExpired: Boolean,
    private val eventHandler: EventHandler,
    private val api: WebMessagingApi,
    private val vault: Vault,
    private val log: Log,
    private val isAuthEnabled: suspend () -> Boolean,
    private val dispatcher: CoroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob()),
) : AuthHandler {
    private var logoutAttempts = 0
    private var authJwt: AuthJwt = AuthJwt(NO_JWT, vault.authRefreshToken)

    override val jwt: String
        get() = authJwt.jwt

    override fun authorize(
        authCode: String,
        redirectUri: String,
        codeVerifier: String?
    ) {
        dispatcher.launch {
            when (val result = api.fetchAuthJwt(authCode, redirectUri, codeVerifier)) {
                is Result.Success -> {
                    result.value.let {
                        authJwt = it
                        if (autoRefreshTokenWhenExpired) {
                            vault.authRefreshToken = it.refreshToken ?: NO_REFRESH_TOKEN
                        }
                        eventHandler.onEvent(Event.Authorized)
                    }
                }

                is Result.Failure -> handleRequestError(result, "fetchAuthJwt()")
            }
        }
    }

    override fun logout() {
        dispatcher.launch {
            when (val result = api.logoutFromAuthenticatedSession(authJwt.jwt)) {
                is Result.Success -> logoutAttempts = 0
                is Result.Failure -> {
                    if (eligibleToRefresh(result.errorCode) && logoutAttempts < MAX_LOGOUT_ATTEMPTS) {
                        logoutAttempts++
                        refreshToken {
                            when (it) {
                                is Result.Success -> logout()
                                is Result.Failure -> handleRequestError(it, "logout()")
                            }
                        }
                    } else {
                        logoutAttempts = 0
                        handleRequestError(result, "logout()")
                    }
                }
            }
        }
    }

    override fun refreshToken(callback: (Result<Empty>) -> Unit) {
        if (!autoRefreshTokenWhenExpired || !authJwt.hasRefreshToken()) {
            val message =
                if (!autoRefreshTokenWhenExpired) {
                    ErrorMessage.AutoRefreshTokenDisabled
                } else {
                    ErrorMessage.NoRefreshToken
                }
            log.e { LogMessages.couldNotRefreshAuthToken(message) }
            callback(Result.Failure(ErrorCode.RefreshAuthTokenFailure, message))
            return
        }
        performTokenRefresh(callback)
    }

    override fun shouldAuthorize(callback: (Boolean) -> Unit) {
        dispatcher.launch {
            if (!isAuthEnabled()) {
                callback(false)
                return@launch
            }

            if (!authJwt.hasRefreshToken()) {
                callback(true)
                return@launch
            }
            performTokenRefresh { result ->
                when (result) {
                    is Result.Success -> callback(false)
                    is Result.Failure -> callback(true)
                }
            }
        }
    }

    private fun performTokenRefresh(callback: (Result<Empty>) -> Unit) {
        authJwt.let { jwt ->
            dispatcher.launch {
                when (val result = api.refreshAuthJwt(jwt.refreshToken!!)) {
                    is Result.Success -> {
                        log.i { LogMessages.REFRESH_AUTH_TOKEN_SUCCESS }
                        authJwt = jwt.copy(jwt = result.value.jwt, refreshToken = jwt.refreshToken)
                        callback(Result.Success(Empty()))
                    }

                    is Result.Failure -> {
                        log.e { LogMessages.couldNotRefreshAuthToken(result.message) }
                        clear()
                        callback(result)
                    }
                }
            }
        }
    }

    override fun clear() {
        authJwt = AuthJwt(NO_JWT, NO_REFRESH_TOKEN)
        vault.authRefreshToken = NO_REFRESH_TOKEN
    }

    private fun handleRequestError(
        result: Result.Failure,
        requestName: String
    ) {
        if (result.errorCode is ErrorCode.CancellationError) {
            log.w { LogMessages.cancellationExceptionRequestName(requestName) }
            return
        }
        log.e { LogMessages.requestError(requestName, result.errorCode, result.message) }
        eventHandler.onEvent(
            Event.Error(result.errorCode, result.message, CorrectiveAction.ReAuthenticate)
        )
    }

    private fun eligibleToRefresh(errorCode: ErrorCode): Boolean =
        autoRefreshTokenWhenExpired &&
            errorCode.isUnauthorized() &&
            authJwt.hasRefreshToken()
}

private fun AuthJwt.hasRefreshToken(): Boolean = refreshToken != null && refreshToken != NO_REFRESH_TOKEN
