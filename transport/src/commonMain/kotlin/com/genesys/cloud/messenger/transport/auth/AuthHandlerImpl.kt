package com.genesys.cloud.messenger.transport.auth

import com.genesys.cloud.messenger.transport.core.CorrectiveAction
import com.genesys.cloud.messenger.transport.core.ErrorCode
import com.genesys.cloud.messenger.transport.core.events.Event
import com.genesys.cloud.messenger.transport.core.events.EventHandler
import com.genesys.cloud.messenger.transport.network.Response
import com.genesys.cloud.messenger.transport.network.WebMessagingApi
import com.genesys.cloud.messenger.transport.util.logs.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

internal class AuthHandlerImpl(
    private val eventHandler: EventHandler,
    private val api: WebMessagingApi,
    private val log: Log,
    private val dispatcher: CoroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob()),
) : AuthHandler {
    override var authJwt: AuthJwt? = null

    override fun authenticate(authCode: String, redirectUri: String, codeVerifier: String?) {
        dispatcher.launch {
            when (val response = api.fetchAuthJwt(authCode, redirectUri, codeVerifier)) {
                is Response.Success -> {
                    response.value.let {
                        authJwt = it
                        eventHandler.onEvent(Event.Authenticated(it))
                    }
                }
                is Response.Failure -> handleRequestError(response, "fetchAuthJwt()")
            }
        }
    }

    override fun logout() {
        val authJwt = this.authJwt ?: run {
            log.w { "Logout from anonymous session is not supported." }
            return
        }
        dispatcher.launch {
            when (val response = api.logoutFromAuthenticatedSession(authJwt.jwt)) {
                is Response.Success -> log.i { "logout() request was successfully sent." }
                is Response.Failure -> handleRequestError(response, "logout()")
            }
        }
    }

    override fun refreshToken() {
        if (authJwt?.refreshToken == null) {
            log.w { "can not refreshAuthToken without authJwt.refreshAuthToken." }
            return
        }
        authJwt?.let {
            dispatcher.launch {
                when (val response = api.refreshAuthJwt(it.refreshToken!!)) {
                    is Response.Success -> authJwt =
                        it.copy(jwt = response.value.jwt, refreshToken = it.refreshToken)
                    is Response.Failure -> handleRequestError(response, "refreshToken()")
                }
            }
        }
    }

    private fun handleRequestError(response: Response.Failure, requestName: String) {
        when (response.errorCode) {
            is ErrorCode.CancellationError -> {
                log.w { "Cancellation exception was thrown, while running $requestName request." }
            }
            else -> {
                log.e { "$requestName respond with error: ${response.errorCode}, and message: ${response.message}" }
                eventHandler.onEvent(
                    Event.Error(
                        response.errorCode,
                        response.message,
                        CorrectiveAction.Reauthenticate
                    )
                )
            }
        }
    }
}
