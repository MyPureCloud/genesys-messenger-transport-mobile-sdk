package com.genesys.cloud.messenger.transport.auth

import com.genesys.cloud.messenger.transport.core.CorrectiveAction
import com.genesys.cloud.messenger.transport.core.ErrorCode
import com.genesys.cloud.messenger.transport.core.ErrorMessage
import com.genesys.cloud.messenger.transport.core.events.Event
import com.genesys.cloud.messenger.transport.core.events.EventHandler
import com.genesys.cloud.messenger.transport.core.isUnauthorized
import com.genesys.cloud.messenger.transport.network.Empty
import com.genesys.cloud.messenger.transport.network.Result
import com.genesys.cloud.messenger.transport.network.WebMessagingApi
import com.genesys.cloud.messenger.transport.util.TokenStore
import com.genesys.cloud.messenger.transport.util.logs.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

internal class AuthHandlerImpl(
    private val autoRefreshTokenWhenExpired: Boolean,
    private val eventHandler: EventHandler,
    private val api: WebMessagingApi,
    private val tokenStore: TokenStore,
    private val log: Log,
    private val dispatcher: CoroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob()),
) : AuthHandler {

    private var authJwt: AuthJwt = AuthJwt(NO_JWT, tokenStore.fetchAuthRefreshToken())

    override val jwt: String
        get() = authJwt.jwt

    override fun authenticate(authCode: String, redirectUri: String, codeVerifier: String?) {
        dispatcher.launch {
            when (val result = api.fetchAuthJwt(authCode, redirectUri, codeVerifier)) {
                is Result.Success -> {
                    result.value.let {
                        authJwt = it
                        if (autoRefreshTokenWhenExpired) {
                            tokenStore.storeAuthRefreshToken(it.refreshToken ?: NO_REFRESH_TOKEN)
                        }
                        eventHandler.onEvent(Event.Authenticated)
                    }
                }
                is Result.Failure -> handleRequestError(result, "fetchAuthJwt()")
            }
        }
    }

    override fun logout() {
        dispatcher.launch {
            when (val result = api.logoutFromAuthenticatedSession(authJwt.jwt)) {
                is Result.Success -> log.i { "logout() request was successfully sent." }
                is Result.Failure -> {
                    if (eligibleToRefresh(result.errorCode)) {
                        refreshToken {
                            when (it) {
                                is Result.Success -> logout()
                                is Result.Failure -> handleRequestError(result, "logout()")
                            }
                        }
                    } else {
                        handleRequestError(result, "logout()")
                    }
                }
            }
        }
    }

    override fun refreshToken(callback: (Result<Empty>) -> Unit) {
        if (!autoRefreshTokenWhenExpired || !authJwt.hasRefreshToken()) {
            val message = if (!autoRefreshTokenWhenExpired) {
                ErrorMessage.AutoRefreshTokenDisabled
            } else {
                ErrorMessage.NoRefreshToken
            }
            log.e { "Could not refreshAuthToken: $message" }
            callback(Result.Failure(ErrorCode.RefreshAuthTokenFailure, message))
            return
        }
        authJwt.let {
            dispatcher.launch {
                when (val result = api.refreshAuthJwt(it.refreshToken!!)) {
                    is Result.Success -> {
                        log.i { "refreshAuthToken success." }
                        authJwt = it.copy(jwt = result.value.jwt, refreshToken = it.refreshToken)
                        callback(Result.Success(Empty()))
                    }
                    is Result.Failure -> {
                        log.e { "Could not refreshAuthToken: ${result.message}" }
                        authJwt = AuthJwt(NO_JWT, NO_REFRESH_TOKEN)
                        tokenStore.storeAuthRefreshToken(NO_REFRESH_TOKEN)
                        callback(result)
                    }
                }
            }
        }
    }

    private fun handleRequestError(result: Result.Failure, requestName: String) {
        if (result.errorCode is ErrorCode.CancellationError) {
            log.w { "Cancellation exception was thrown, while running $requestName request." }
            return
        }
        log.e { "$requestName respond with error: ${result.errorCode}, and message: ${result.message}" }
        eventHandler.onEvent(
            Event.Error(result.errorCode, result.message, CorrectiveAction.ReAuthenticate)
        )
    }

    private fun eligibleToRefresh(errorCode: ErrorCode): Boolean =
        autoRefreshTokenWhenExpired &&
                errorCode.isUnauthorized() &&
                authJwt.hasRefreshToken()
}

private fun AuthJwt.hasRefreshToken(): Boolean =
    refreshToken != null && refreshToken != NO_REFRESH_TOKEN
