package com.genesys.cloud.messenger.transport

import com.genesys.cloud.messenger.transport.shyrka.receive.DeploymentConfig
import com.genesys.cloud.messenger.transport.util.defaultHttpClient
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.http.URLBuilder

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
        return client.get("$url")
    }
}
