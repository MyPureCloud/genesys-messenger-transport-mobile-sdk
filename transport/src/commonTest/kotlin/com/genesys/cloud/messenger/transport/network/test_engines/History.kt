package com.genesys.cloud.messenger.transport.network.test_engines

import com.genesys.cloud.messenger.transport.network.TestWebMessagingApiResponses
import com.genesys.cloud.messenger.transport.utility.ErrorTest
import com.genesys.cloud.messenger.transport.utility.InvalidValues
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.mock.MockEngineConfig
import io.ktor.client.engine.mock.respond
import io.ktor.http.ContentType
import io.ktor.http.fullPath
import io.ktor.http.headersOf
import kotlinx.coroutines.CancellationException

private const val EMPTY_MESSAGE_ENTITY_RESPONSE_PATH =
    "/api/v2/webmessaging/messages?pageNumber=0&pageSize=0"
private const val BASIC_MESSAGE_ENTITY_RESPONSE_PATH =
    "/api/v2/webmessaging/messages?pageNumber=1&pageSize=25"

fun HttpClientConfig<MockEngineConfig>.historyEngine() {
    engine {
        val responseHeaders =
            headersOf("Content-Type" to listOf(ContentType.Application.Json.toString()))
        addHandler { request ->
            when (request.url.fullPath) {
                BASIC_MESSAGE_ENTITY_RESPONSE_PATH -> {
                    respond(
                        TestWebMessagingApiResponses.messageEntityResponseWith3Messages,
                        headers = responseHeaders
                    )
                }
                EMPTY_MESSAGE_ENTITY_RESPONSE_PATH -> {
                    respond(
                        TestWebMessagingApiResponses.messageEntityListResponseWithoutMessages,
                        headers = responseHeaders
                    )
                }
                else -> {
                    if (request.headers["Authorization"].equals("bearer ${InvalidValues.CANCELLATION_EXCEPTION}")) {
                        throw CancellationException(ErrorTest.MESSAGE)
                    } else {
                        error(ErrorTest.MESSAGE)
                    }
                }
            }
        }
    }
}
