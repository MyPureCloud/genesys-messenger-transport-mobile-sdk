package com.genesys.cloud.messenger.transport.utility

import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpStatusCode

internal fun MockRequestHandleScope.respondUnauthorized(): HttpResponseData =
    respond("You are not authorized", HttpStatusCode.Unauthorized)
