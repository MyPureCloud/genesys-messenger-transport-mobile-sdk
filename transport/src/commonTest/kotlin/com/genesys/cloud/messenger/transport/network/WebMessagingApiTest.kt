package com.genesys.cloud.messenger.transport.network

import com.genesys.cloud.messenger.transport.DEFAULT_TIMEOUT
import com.genesys.cloud.messenger.transport.core.Configuration
import com.genesys.cloud.messenger.transport.mockHttpClientWith
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.mock.MockEngineConfig
import io.ktor.client.engine.mock.respond
import io.ktor.http.ContentType
import io.ktor.http.fullPath
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlin.test.Test
import kotlin.test.assertEquals

private const val EMPTY_MESSAGE_ENTITY_RESPONSE_PATH =
    "/api/v2/webmessaging/messages?pageNumber=0&pageSize=0"
private const val BASIC_MESSAGE_ENTITY_RESPONSE_PATH =
    "/api/v2/webmessaging/messages?pageNumber=1&pageSize=25"

class WebMessagingApiTest {
    private val subject: WebMessagingApi
        get() {
            val configuration = configuration()
            return WebMessagingApi(
                configuration = configuration,
                client = mockHttpClientWith { historyEngine() }
            )
        }

    @Test
    fun whenFetchHistory() {
        val expectedEntityList = TestWebMessagingApiResponses.testMessageEntityList

        val result = runBlocking {
            withTimeout(DEFAULT_TIMEOUT) {
                subject.getMessages(jwt = "abc-123", pageNumber = 1)
            }
        }

        assertEquals(expectedEntityList, result)
    }

    @Test
    fun whenFetchHistoryAndThereAreNoHistory() {
        val expectedEntityList = TestWebMessagingApiResponses.emptyMessageEntityList

        val result = runBlocking {
            withTimeout(DEFAULT_TIMEOUT) {
                subject.getMessages(jwt = "abc-123", pageNumber = 0, pageSize = 0)
            }
        }

        assertEquals(expectedEntityList, result)
    }

    private fun configuration(): Configuration = Configuration(
        deploymentId = "deploymentId",
        domain = "inindca.com",
        logging = false
    )

    private fun HttpClientConfig<MockEngineConfig>.historyEngine() {
        engine {
            val responseHeaders =
                headersOf("Content-Type" to listOf(ContentType.Application.Json.toString()))
            addHandler { request ->
                when (request.url.fullPath) {
                    BASIC_MESSAGE_ENTITY_RESPONSE_PATH -> {
                        respond(
                            TestWebMessagingApiResponses.messageEntityResponseWith2Messages,
                            headers = responseHeaders
                        )
                    }
                    EMPTY_MESSAGE_ENTITY_RESPONSE_PATH -> {
                        respond(
                            TestWebMessagingApiResponses.messageEntityListResponseWithoutMessages,
                            headers = responseHeaders
                        )
                    }
                    else -> error("Unhandled ${request.url.fullPath}")
                }
            }
        }
    }
}
