package com.genesys.cloud.messenger.transport.network.test_engines

import com.genesys.cloud.messenger.transport.network.toRegisterDeviceTokenRequestBody
import com.genesys.cloud.messenger.transport.push.RegisterDeviceTokenRequestBody
import com.genesys.cloud.messenger.transport.respondNotFound
import com.genesys.cloud.messenger.transport.utility.MockEngineValues
import com.genesys.cloud.messenger.transport.utility.PushTestValues
import com.genesys.cloud.messenger.transport.utility.TestValues
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.mock.MockEngineConfig
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondBadRequest
import io.ktor.content.TextContent
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.fullPath
import io.ktor.http.headersOf
import kotlinx.serialization.json.Json

private const val BASIC_REGISTER_DEVICE_TOKEN_PATH =
    "/api/v2/webmessaging/deployments/${TestValues.DeploymentId}/pushdevices/${TestValues.Token}"

internal fun HttpClientConfig<MockEngineConfig>.registerDeviceTokenEngine() {
    engine {
        addHandler { request ->
            when (request.url.fullPath) {
                BASIC_REGISTER_DEVICE_TOKEN_PATH -> {
                    if (request.method == HttpMethod.Post && request.body is TextContent) {
                        val requestBody = Json.decodeFromString(
                            RegisterDeviceTokenRequestBody.serializer(),
                            (request.body as TextContent).text
                        )
                        if (requestBody == PushTestValues.CONFIG.toRegisterDeviceTokenRequestBody()) {
                            respond(
                                status = HttpStatusCode.NoContent,
                                headers = headersOf(
                                    HttpHeaders.ContentType,
                                    MockEngineValues.CONTENT_TYPE_JSON
                                ),
                                content = MockEngineValues.NO_CONTENT
                            )
                        } else {
                            TODO("Not yet implemented: MTSDK-416")
                            respondBadRequest()
                        }
                    } else {
                        TODO("Not yet implemented: MTSDK-416")
                        respondBadRequest()
                    }
                }

                else -> {
                    TODO("Not yet implemented: MTSDK-416")
                    respondNotFound()
                }
            }
        }
    }
}
