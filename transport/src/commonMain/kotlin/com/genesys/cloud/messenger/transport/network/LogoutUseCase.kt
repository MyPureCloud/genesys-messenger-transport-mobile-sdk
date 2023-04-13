package com.genesys.cloud.messenger.transport.network

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.delete
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.http.Url
import io.ktor.http.isSuccess

internal class LogoutUseCase(
    private val logoutUrl: Url,
    val client: HttpClient = defaultHttpClient(true),
) {

    suspend fun logout(jwt: String) {
        val response = client.delete(logoutUrl.toString()) {
            headerAuthorizationBearer(jwt)
        }

        if (!response.status.isSuccess()) throw Exception("Failed to logout: ${response.body<Any>()}")
    }
}

private fun HttpRequestBuilder.headerAuthorizationBearer(jwt: String) =
    header(HttpHeaders.Authorization, "bearer $jwt")
