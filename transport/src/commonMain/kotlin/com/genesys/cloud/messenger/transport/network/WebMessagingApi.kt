package com.genesys.cloud.messenger.transport.network

import com.genesys.cloud.messenger.transport.auth.AuthJwt
import com.genesys.cloud.messenger.transport.auth.RefreshToken
import com.genesys.cloud.messenger.transport.core.Configuration
import com.genesys.cloud.messenger.transport.core.DEFAULT_PAGE_SIZE
import com.genesys.cloud.messenger.transport.core.Empty
import com.genesys.cloud.messenger.transport.core.ErrorCode
import com.genesys.cloud.messenger.transport.core.Result
import com.genesys.cloud.messenger.transport.push.DeviceTokenOperation
import com.genesys.cloud.messenger.transport.push.DeviceTokenRequestBody
import com.genesys.cloud.messenger.transport.push.PushConfig
import com.genesys.cloud.messenger.transport.shyrka.WebMessagingJson
import com.genesys.cloud.messenger.transport.shyrka.receive.MessageEntityList
import com.genesys.cloud.messenger.transport.shyrka.receive.PresignedUrlResponse
import com.genesys.cloud.messenger.transport.shyrka.send.AuthJwtRequest
import com.genesys.cloud.messenger.transport.shyrka.send.OAuth
import com.genesys.cloud.messenger.transport.util.Urls
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.onUpload
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.defaultForFilePath
import io.ktor.http.isSuccess
import kotlinx.serialization.encodeToString
import kotlin.coroutines.cancellation.CancellationException

internal class WebMessagingApi(
    private val urls: Urls,
    private val configuration: Configuration,
    private val client: HttpClient = defaultHttpClient(configuration.logging),
) {

    /**
     * Returns Result.Success<MessageEntityList> upon successful response and parsing.
     * Otherwise, return Result.Failure with description the failure.
     */
    suspend fun getMessages(
        jwt: String,
        pageNumber: Int,
        pageSize: Int = DEFAULT_PAGE_SIZE,
    ): Result<MessageEntityList> = try {
        val response = client.get(urls.history.toString()) {
            headerAuthorizationBearer(jwt)
            headerOrigin(configuration.domain)
            parameter("pageNumber", pageNumber)
            parameter("pageSize", pageSize)
        }
        if (response.status.isSuccess()) {
            Result.Success(response.body())
        } else {
            Result.Failure(ErrorCode.mapFrom(response.status.value), response.body())
        }
    } catch (cancellationException: CancellationException) {
        Result.Failure(ErrorCode.CancellationError, cancellationException.message)
    } catch (e: Exception) {
        Result.Failure(ErrorCode.UnexpectedError, e.message)
    }

    suspend fun uploadFile(
        presignedUrlResponse: PresignedUrlResponse,
        byteArray: ByteArray,
        progressCallback: ((Float) -> Unit)?,
    ): Result<Empty> = try {
        val response = client.put(presignedUrlResponse.url) {
            presignedUrlResponse.headers.forEach {
                header(it.key, it.value)
            }
            presignedUrlResponse.fileName?.let {
                contentType(ContentType.defaultForFilePath(it).withoutParameters())
            }
            onUpload { bytesSendTotal: Long, contentLength: Long ->
                progressCallback?.let { it((bytesSendTotal / contentLength.toFloat()) * 100) }
            }
            setBody(byteArray)
        }
        if (response.status.isSuccess()) {
            Result.Success(Empty())
        } else {
            Result.Failure(ErrorCode.mapFrom(response.status.value), response.body<String>())
        }
    } catch (cancellationException: CancellationException) {
        Result.Failure(ErrorCode.CancellationError, cancellationException.message)
    } catch (e: Exception) {
        Result.Failure(ErrorCode.UnexpectedError, e.message)
    }

    suspend fun fetchAuthJwt(
        authCode: String,
        redirectUri: String,
        codeVerifier: String?,
    ): Result<AuthJwt> = try {
        val requestBody = AuthJwtRequest(
            deploymentId = configuration.deploymentId,
            oauth = OAuth(
                code = authCode,
                redirectUri = redirectUri,
                codeVerifier = codeVerifier,
            )
        )
        val response = client.post(urls.jwtAuthUrl.toString()) {
            header("content-type", ContentType.Application.Json)
            setBody(requestBody)
        }
        if (response.status.isSuccess()) {
            Result.Success(response.body())
        } else {
            Result.Failure(ErrorCode.AuthFailed, response.body<String>())
        }
    } catch (cancellationException: CancellationException) {
        Result.Failure(ErrorCode.CancellationError, cancellationException.message)
    } catch (exception: Exception) {
        Result.Failure(ErrorCode.AuthFailed, exception.message)
    }

    suspend fun logoutFromAuthenticatedSession(jwt: String): Result<Empty> = try {
        val response = client.delete(urls.logoutUrl.toString()) {
            headerAuthorizationBearer(jwt)
        }
        if (response.status.isSuccess()) {
            Result.Success(Empty())
        } else {
            val errorCode =
                if (response.status.isUnauthorized()) ErrorCode.ClientResponseError(401) else ErrorCode.AuthLogoutFailed
            Result.Failure(errorCode, response.body<String>())
        }
    } catch (cancellationException: CancellationException) {
        Result.Failure(ErrorCode.CancellationError, cancellationException.message)
    } catch (exception: Exception) {
        Result.Failure(ErrorCode.AuthLogoutFailed, exception.message)
    }

    suspend fun refreshAuthJwt(refreshToken: String): Result<AuthJwt> = try {
        val response = client.post(urls.refreshAuthTokenUrl.toString()) {
            header("content-type", ContentType.Application.Json)
            setBody(RefreshToken(refreshToken))
        }
        if (response.status.isSuccess()) {
            Result.Success(response.body())
        } else {
            Result.Failure(ErrorCode.RefreshAuthTokenFailure, response.body<String>())
        }
    } catch (cancellationException: CancellationException) {
        Result.Failure(ErrorCode.CancellationError, cancellationException.message)
    } catch (exception: Exception) {
        Result.Failure(ErrorCode.RefreshAuthTokenFailure, exception.message)
    }

    suspend fun performDeviceTokenOperation(
        userPushConfig: PushConfig,
        operation: DeviceTokenOperation,
    ): Result<Empty> = try {
        val url = urls.deviceTokenUrl(configuration.deploymentId, userPushConfig.token)
        val response = client.request(url) {
            this.method = operation.httpMethod
            header("content-type", ContentType.Application.Json)
            setBody(userPushConfig.toDeviceTokenRequestBody(operation))
        }

        if (response.status.isSuccess()) {
            Result.Success(Empty())
        } else {
            TODO("Not yet implemented: MTSDK-416")
            Result.Failure(ErrorCode.UnexpectedError, "TODO(Not yet implemented: MTSDK-416)")
        }
    } catch (cancellationException: CancellationException) {
        TODO("Not yet implemented: MTSDK-416")
        Result.Failure(ErrorCode.CancellationError, cancellationException.message)
    } catch (exception: Exception) {
        TODO("Not yet implemented: MTSDK-416")
        Result.Failure(ErrorCode.UnexpectedError, exception.message)
    }
}

private fun HttpStatusCode.isUnauthorized(): Boolean = this == HttpStatusCode.Unauthorized

private fun HttpRequestBuilder.headerAuthorizationBearer(jwt: String) =
    header(HttpHeaders.Authorization, "bearer $jwt")

private fun HttpRequestBuilder.headerOrigin(origin: String) =
    header(HttpHeaders.Origin, origin)

internal fun PushConfig.toDeviceTokenRequestBody(operation: DeviceTokenOperation): String = when (operation) {
    DeviceTokenOperation.Register -> WebMessagingJson.json.encodeToString(
        DeviceTokenRequestBody(
            deviceToken = deviceToken,
            notificationProvider = pushProvider,
            language = preferredLanguage,
            deviceType = deviceType,
        )
    )

    DeviceTokenOperation.Update -> WebMessagingJson.json.encodeToString(
        DeviceTokenRequestBody(
            deviceToken = deviceToken,
            language = preferredLanguage,
        )
    )

    DeviceTokenOperation.Delete -> ""
}
