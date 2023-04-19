package com.genesys.cloud.messenger.transport.network

import com.genesys.cloud.messenger.transport.auth.AuthJwt
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.Url
import io.ktor.http.isSuccess
import kotlinx.serialization.Serializable

internal class FetchJwtUseCase(
    logging: Boolean,
    private val deploymentId: String,
    private val jwtAuthUrl: Url,
    private val client: HttpClient = defaultHttpClient(logging),
) {

    suspend fun fetch(
        authCode: String,
        redirectUri: String,
        codeVerifier: String?,
    ): AuthJwt {
        val requestBody = AuthJWTRequest(
            deploymentId = deploymentId,
            oauth = OAuth(
                code = authCode,
                redirectUri = redirectUri,
                codeVerifier = codeVerifier,
            )
        )
        val response = client.post(jwtAuthUrl.toString()) {
            header("content-type", ContentType.Application.Json)
            setBody(requestBody)
        }
        return if (response.status.isSuccess()) {
            response.body()
        } else {
            throw Exception("Auth JWT fetch failed: ${response.body<String>()}")
        }
    }
}

@Serializable
internal data class AuthJWTRequest(
    val deploymentId: String,
    val oauth: OAuth,
)

@Serializable
internal data class OAuth(
    val code: String,
    val redirectUri: String,
    val codeVerifier: String? = null,
)
