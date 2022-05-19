package com.genesys.cloud.messenger.transport.network

import com.genesys.cloud.messenger.transport.shyrka.receive.DeploymentConfig
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.URLBuilder
import io.ktor.http.path

internal class DeploymentConfigUseCase(
    logging: Boolean,
    private val client: HttpClient = defaultHttpClient(logging),
) {
    suspend fun fetch(
        domain: String,
        deploymentId: String,
    ): DeploymentConfig {
        val url = URLBuilder("https://api-cdn.$domain").apply {
            path("webdeployments/v1/deployments/$deploymentId/config.json")
        }.build()
        return client.get("$url").body()
    }
}
