package com.genesys.cloud.messenger.transport.network

import com.genesys.cloud.messenger.transport.core.Configuration
import com.genesys.cloud.messenger.transport.core.DEFAULT_PAGE_SIZE
import com.genesys.cloud.messenger.transport.shyrka.receive.MessageEntityList
import com.genesys.cloud.messenger.transport.shyrka.receive.PresignedUrlResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ResponseException
import io.ktor.client.plugins.onUpload
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.HttpHeaders
import kotlin.coroutines.cancellation.CancellationException

internal class WebMessagingApi(
    private val configuration: Configuration,
    private val client: HttpClient = defaultHttpClient(configuration.logging),
) {

    /**
     * @throws ResponseException if unsuccessful response from the service
     */
    suspend fun getMessages(
        jwt: String,
        pageNumber: Int,
        pageSize: Int = DEFAULT_PAGE_SIZE,
    ): MessageEntityList {
        return client.get("${configuration.apiBaseUrl}/api/v2/webmessaging/messages") {
            headerAuthorizationBearer(jwt)
            parameter("pageNumber", pageNumber)
            parameter("pageSize", pageSize)
        }.body()
    }

    @Throws(ResponseException::class, CancellationException::class)
    suspend fun uploadFile(
        presignedUrlResponse: PresignedUrlResponse,
        byteArray: ByteArray,
        progressCallback: ((Float) -> Unit)?
    ) {
        client.put(presignedUrlResponse.url) {
            presignedUrlResponse.headers.forEach {
                header(it.key, it.value)
            }
            onUpload { bytesSendTotal: Long, contentLength: Long ->
                progressCallback?.let { it((bytesSendTotal / contentLength.toFloat()) * 100) }
            }
            setBody(byteArray)
        }
    }
}

private fun HttpRequestBuilder.headerAuthorizationBearer(jwt: String) =
    header(HttpHeaders.Authorization, "bearer $jwt")
