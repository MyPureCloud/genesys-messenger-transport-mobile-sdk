package com.genesys.cloud.messenger.transport.network.test_engines

import com.genesys.cloud.messenger.transport.respondNotFound
import com.genesys.cloud.messenger.transport.utility.MockEngineValues
import com.genesys.cloud.messenger.transport.utility.TestValues
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.mock.MockEngineConfig
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondBadRequest
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.content.TextContent
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.fullPath
import io.ktor.http.headersOf

private const val BASIC_DEVICE_TOKEN_PATH =
    "/api/v2/webmessaging/deployments/${TestValues.DeploymentId}/pushdevices/${TestValues.Token}"

internal fun HttpClientConfig<MockEngineConfig>.pushNotificationEngine() {
    engine {
        addHandler { request ->
            when (request.url.fullPath) {
                BASIC_DEVICE_TOKEN_PATH -> {
                    when (request.method) {
                        HttpMethod.Post -> respondToRegisterDeviceToken(request)
                        HttpMethod.Patch -> respondToUpdateDeviceToken(request)
                        else -> {
                            TODO("Not yet implemented: MTSDK-416")
                            respondBadRequest()
                        }
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

private fun MockRequestHandleScope.respondToRegisterDeviceToken(request: HttpRequestData): HttpResponseData {
    val expectedRegisterRequestBody = """{"deviceToken":"${TestValues.DEVICE_TOKEN}","language":"${TestValues.PREFERRED_LANGUAGE}","deviceType":"${TestValues.DEVICE_TYPE}","notificationProvider":"${TestValues.PUSH_PROVIDER}"}"""
    return if ((request.body as TextContent).text == expectedRegisterRequestBody) {
        respondSuccess()
    } else {
        TODO("Not yet implemented: MTSDK-416")
        respondBadRequest()
    }
}

private fun MockRequestHandleScope.respondToUpdateDeviceToken(request: HttpRequestData): HttpResponseData {
    val expectedUpdateRequestBody =
        """{"deviceToken":"${TestValues.DEVICE_TOKEN}","language":"${TestValues.PREFERRED_LANGUAGE}"}"""
    return if ((request.body as TextContent).text == expectedUpdateRequestBody) {
        respondSuccess()
    } else {
        TODO("Not yet implemented: MTSDK-416")
        respondBadRequest()
    }
}

private fun MockRequestHandleScope.respondSuccess(): HttpResponseData = respond(
    status = HttpStatusCode.NoContent,
    headers = headersOf(
        HttpHeaders.ContentType,
        MockEngineValues.CONTENT_TYPE_JSON
    ),
    content = MockEngineValues.NO_CONTENT
)
