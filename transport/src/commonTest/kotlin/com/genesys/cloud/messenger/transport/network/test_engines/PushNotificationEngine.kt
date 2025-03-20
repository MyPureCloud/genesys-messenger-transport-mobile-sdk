package com.genesys.cloud.messenger.transport.network.test_engines

import com.genesys.cloud.messenger.transport.respondNotFound
import com.genesys.cloud.messenger.transport.shyrka.WebMessagingJson
import com.genesys.cloud.messenger.transport.shyrka.receive.PushErrorResponse
import com.genesys.cloud.messenger.transport.utility.ErrorTest
import com.genesys.cloud.messenger.transport.utility.InvalidValues
import com.genesys.cloud.messenger.transport.utility.MockEngineValues
import com.genesys.cloud.messenger.transport.utility.PushTestValues
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
import kotlinx.serialization.encodeToString
import kotlin.coroutines.cancellation.CancellationException

private const val DEVICE_TOKEN_PATH =
    "/api/v2/webmessaging/deployments/${TestValues.DEPLOYMENT_ID}/pushdevices/${TestValues.TOKEN}"
private const val DEVICE_TOKEN_PATH_TO_TRIGGER_CANCELLATION_EXCEPTION =
    "/api/v2/webmessaging/deployments/${InvalidValues.CANCELLATION_EXCEPTION}/pushdevices/${InvalidValues.CANCELLATION_EXCEPTION}"
private const val DEVICE_TOKEN_PATH_TO_TRIGGER_UNKNOWN_EXCEPTION =
    "/api/v2/webmessaging/deployments/${InvalidValues.UNKNOWN_EXCEPTION}/pushdevices/${InvalidValues.UNKNOWN_EXCEPTION}"

internal fun HttpClientConfig<MockEngineConfig>.pushNotificationEngine() {
    engine {
        addHandler { request ->
            when (request.url.fullPath) {
                DEVICE_TOKEN_PATH -> {
                    when (request.method) {
                        HttpMethod.Post -> respondToRegisterDeviceToken(request)
                        HttpMethod.Patch -> respondToUpdateDeviceToken(request)
                        HttpMethod.Delete -> respondToDeleteDeviceToken(request)
                        else -> respondBadRequest()
                    }
                }

                DEVICE_TOKEN_PATH_TO_TRIGGER_CANCELLATION_EXCEPTION -> throw CancellationException(
                    ErrorTest.MESSAGE
                )

                DEVICE_TOKEN_PATH_TO_TRIGGER_UNKNOWN_EXCEPTION -> throw Exception(ErrorTest.MESSAGE)

                else -> respondNotFound()
            }
        }
    }
}

private fun MockRequestHandleScope.respondToRegisterDeviceToken(request: HttpRequestData): HttpResponseData {
    val expectedRegisterRequestBody =
        """{"deviceToken":"${TestValues.DEVICE_TOKEN}","language":"${TestValues.PREFERRED_LANGUAGE}","deviceType":"${TestValues.DEVICE_TYPE}","notificationProvider":"${TestValues.PUSH_PROVIDER}"}"""
    return if ((request.body as TextContent).text == expectedRegisterRequestBody) {
        respondSuccess()
    } else {
        respondWithPushErrorResponse(
            statusCode = HttpStatusCode.InternalServerError,
            content = PushTestValues.pushErrorResponseWith(
                PushTestValues.PUSH_CODE_DEVICE_REGISTRATION_FAILURE
            )
        )
    }
}

private fun MockRequestHandleScope.respondToUpdateDeviceToken(request: HttpRequestData): HttpResponseData {
    val expectedUpdateRequestBody =
        """{"deviceToken":"${TestValues.DEVICE_TOKEN}","language":"${TestValues.PREFERRED_LANGUAGE}"}"""
    return if ((request.body as TextContent).text == expectedUpdateRequestBody) {
        respondSuccess()
    } else {
        respondWithPushErrorResponse(
            statusCode = HttpStatusCode.InternalServerError,
            content = PushTestValues.pushErrorResponseWith(
                PushTestValues.PUSH_CODE_DEVICE_UPDATE_FAILURE
            )
        )
    }
}

private fun MockRequestHandleScope.respondToDeleteDeviceToken(request: HttpRequestData): HttpResponseData {
    return if ((request.body as TextContent).text == "") {
        respondSuccess()
    } else {
        respondWithPushErrorResponse(
            statusCode = HttpStatusCode.InternalServerError,
            content = PushTestValues.pushErrorResponseWith(
                PushTestValues.PUSH_CODE_DEVICE_DELETE_FAILURE
            )
        )
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

private fun MockRequestHandleScope.respondWithPushErrorResponse(
    statusCode: HttpStatusCode = HttpStatusCode.NotFound,
    content: PushErrorResponse = PushTestValues.pushErrorResponseWith(PushTestValues.PUSH_CODE_DEPLOYMENT_NOT_FOUND),
): HttpResponseData = respond(
    status = statusCode,
    headers = headersOf(
        HttpHeaders.ContentType,
        MockEngineValues.CONTENT_TYPE_JSON
    ),
    content = WebMessagingJson.json.encodeToString(content)
)
