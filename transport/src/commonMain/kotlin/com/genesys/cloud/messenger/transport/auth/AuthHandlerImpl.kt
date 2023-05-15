package com.genesys.cloud.messenger.transport.auth

import com.genesys.cloud.messenger.transport.core.AuthConfiguration
import com.genesys.cloud.messenger.transport.core.CorrectiveAction
import com.genesys.cloud.messenger.transport.core.ErrorCode
import com.genesys.cloud.messenger.transport.core.events.Event
import com.genesys.cloud.messenger.transport.core.events.EventHandler
import com.genesys.cloud.messenger.transport.network.Empty
import com.genesys.cloud.messenger.transport.network.Result
import com.genesys.cloud.messenger.transport.network.WebMessagingApi
import com.genesys.cloud.messenger.transport.util.logs.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

internal class AuthHandlerImpl(
    private val configuration: AuthConfiguration,
    private val eventHandler: EventHandler,
    private val api: WebMessagingApi,
    private val log: Log,
    private val dispatcher: CoroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob()),
) : AuthHandler {
    override var authJwt: AuthJwt? = null

    override fun authenticate(authCode: String, redirectUri: String, codeVerifier: String?) {
        dispatcher.launch {
            when (val result = api.fetchAuthJwt(authCode, redirectUri, codeVerifier)) {
                is Result.Success -> {
                    result.value.let {
                        authJwt = it
                        eventHandler.onEvent(Event.Authenticated)
                    }
                }
                is Result.Failure -> handleRequestError(result, "fetchAuthJwt()")
            }
        }
    }

    override fun logout() {
        val authJwt = this.authJwt ?: run {
            log.w { "Logout from anonymous session is not supported." }
            return
        }
        dispatcher.launch {
            when (val result = api.logoutFromAuthenticatedSession(authJwt.jwt)) {
                is Result.Success -> log.i { "logout() request was successfully sent." }
                is Result.Failure -> handleRequestError(result, "logout()")
            }
        }
    }

    override fun refreshToken(callback: (Result<Empty>) -> Unit) {
        if (authJwt?.refreshToken == null) {
            log.w { "can not refreshAuthToken without authJwt.refreshAuthToken." }
            return
        }
        if (!configuration.autoRefreshWhenTokenExpire) {
            callback(Result.Failure(
                errorCode = ErrorCode.RefreshAuthTokenFailure,
                message = "Auto refresh token is disabled in AuthConfiguration.")
            )
            return
        }
        authJwt?.let {
            dispatcher.launch {
                when (val result = api.refreshAuthJwt(it.refreshToken!!)) {
                    is Result.Success -> {
                        log.i { "refreshAuthToken success." }
                        authJwt = it.copy(jwt = result.value.jwt, refreshToken = it.refreshToken)
                        callback(Result.Success(Empty()))
                    }
                    is Result.Failure -> {
                        // TODO("Failure to refresh token should result in transition to State.Error. Also, maybe remove the handleRequestError?")
                        handleRequestError(result, "refreshToken()")
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
            Event.Error(
                result.errorCode,
                result.message,
                CorrectiveAction.Reauthenticate
            )
        )
    }
}
