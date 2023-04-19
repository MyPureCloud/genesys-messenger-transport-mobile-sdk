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
    private val dispatcher: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob()),
) : AuthHandler {

    override fun authenticate(authCode: String, redirectUri: String, codeVerifier: String?) {
        dispatcher.launch {
            val response = api.fetchAuthJwt(authCode, redirectUri, codeVerifier)
            when (response) {
                is Response.Success -> {
                    eventHandler.onEvent(Event.Authenticated(response.value))
                }
                is Response.Failure -> {
                    when (response.errorCode) {
                        is ErrorCode.CancellationError -> {
                            log.w { "Cancellation exception was thrown, while running fetchAuthJwt() request." }
                        }
                        is ErrorCode.AuthFailed -> {
                            log.e { "AuthFailed " }
                            eventHandler.onEvent(
                                Event.Error(
                                    response.errorCode,
                                    response.message,
                                    CorrectiveAction.Reauthenticate
                                )
                            )
                        }
                        else -> {
                            log.e { "Unknown error" }
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
        }
    }
}
