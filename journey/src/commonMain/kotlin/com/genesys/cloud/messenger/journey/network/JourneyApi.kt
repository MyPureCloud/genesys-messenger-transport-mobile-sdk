package com.genesys.cloud.messenger.journey.network

import com.genesys.cloud.messenger.journey.model.AppEventRequest
import com.genesys.cloud.messenger.journey.util.logs.Log
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType

internal class JourneyApi(
    private val urls: JourneyUrls,
    private val client: HttpClient,
    private val log: Log,
) {
    suspend fun sendAppEvent(request: AppEventRequest): Result<Unit> {
        return try {
            val jsonBody = journeyJson.encodeToString(request)
            log.i { "Sending app event to ${urls.appEventsUrl}: $jsonBody" }
            val response: HttpResponse = client.post(urls.appEventsUrl) {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
            if (response.status.value in 200..299) {
                Result.success(Unit)
            } else {
                val body = response.bodyAsText()
                log.e { "App event request failed with status ${response.status}: $body" }
                Result.failure(AppEventApiException(response.status.value, body))
            }
        } catch (e: Exception) {
            log.e(e) { "App event request failed" }
            Result.failure(e)
        }
    }
}

internal class AppEventApiException(val statusCode: Int, val responseBody: String) :
    Exception("App event API error $statusCode: $responseBody")
