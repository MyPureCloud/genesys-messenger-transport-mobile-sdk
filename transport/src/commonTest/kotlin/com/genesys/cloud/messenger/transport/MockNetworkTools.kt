package com.genesys.cloud.messenger.transport

import com.genesys.cloud.messenger.transport.shyrka.WebMessagingJson
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockEngineConfig
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json

fun mockHttpClientWith(
    engineBlock: HttpClientConfig<MockEngineConfig>.() -> Unit
): HttpClient =
    HttpClient(MockEngine) {
        install(ContentNegotiation) {
            json(WebMessagingJson.json)
        }
        engineBlock()
    }

fun MockRequestHandleScope.respondNotFound(): HttpResponseData = respond("Not found", HttpStatusCode.NotFound)
