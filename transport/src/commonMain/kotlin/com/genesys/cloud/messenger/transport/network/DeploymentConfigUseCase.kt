package com.genesys.cloud.messenger.transport.network

import com.genesys.cloud.messenger.transport.shyrka.receive.DeploymentConfig
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.URLBuilder
import io.ktor.http.path
import io.ktor.util.reflect.TypeInfo

internal class DeploymentConfigUseCase(
    logging: Boolean,
    private val deploymentConfigUrl: String,
    private val client: HttpClient = defaultHttpClient(logging),
) {
    suspend fun fetch(): DeploymentConfig {
        return client.get(deploymentConfigUrl).body()
    }
}
