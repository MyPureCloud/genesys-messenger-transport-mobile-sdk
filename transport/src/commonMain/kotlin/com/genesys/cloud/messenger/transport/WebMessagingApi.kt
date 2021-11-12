package com.genesys.cloud.messenger.transport

import com.genesys.cloud.messenger.transport.shyrka.receive.DeploymentConfig
import com.genesys.cloud.messenger.transport.shyrka.receive.MessageEntityList
import com.genesys.cloud.messenger.transport.shyrka.receive.PresignedUrlResponse
import com.genesys.cloud.messenger.transport.util.logs.Log
import io.ktor.client.HttpClient
import io.ktor.client.features.HttpCallValidator
import io.ktor.client.features.ResponseException
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.features.json.serializer.KotlinxSerializer
import io.ktor.client.features.logging.LogLevel
import io.ktor.client.features.logging.Logging
import io.ktor.client.features.onUpload
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.put
import io.ktor.http.HttpHeaders
import kotlinx.serialization.json.Json
import kotlin.coroutines.cancellation.CancellationException

internal class WebMessagingApi(
    log: Log,
    private val configuration: Configuration,
    private val client: HttpClient = defaultHttpClient(log, configuration),
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
        }
    }

    @Throws(ResponseException::class, CancellationException::class)
    suspend fun uploadFile(
        presignedUrlResponse: PresignedUrlResponse,
        byteArray: ByteArray,
        progressCallback: ((Float) -> Unit)?
    ) {
        client.put<Unit>(presignedUrlResponse.url) {
            presignedUrlResponse.headers.forEach {
                header(it.key, it.value)
            }
            onUpload { bytesSendTotal: Long, contentLength: Long ->
                progressCallback?.let { it((bytesSendTotal / contentLength.toFloat()) * 100) }
            }
            body = byteArray
        }
    }

    /**
     * @throws ResponseException if unsuccessful response from the service
     */
    suspend fun fetchDeploymentConfig(): DeploymentConfig {
        return client.get("${configuration.deploymentConfigUrl}")
    }
}

private fun HttpRequestBuilder.headerAuthorizationBearer(jwt: String) =
    header(HttpHeaders.Authorization, "bearer $jwt")

private fun defaultHttpClient(log: Log, configuration: Configuration): HttpClient = HttpClient {
    if (configuration.logging) {
        install(Logging) {
            this.logger = log.ktorLogger
            level = LogLevel.ALL
        }
    }
    install(JsonFeature) {
        serializer = KotlinxSerializer(
            Json {
                ignoreUnknownKeys = true
                useAlternativeNames = false
            }
        )
    }
    install(HttpCallValidator)
}
