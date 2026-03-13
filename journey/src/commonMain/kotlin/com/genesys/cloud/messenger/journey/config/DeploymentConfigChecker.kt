package com.genesys.cloud.messenger.journey.config

import com.genesys.cloud.messenger.journey.network.JourneyUrls
import com.genesys.cloud.messenger.journey.util.logs.Log
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

private val lenientJson = Json {
    ignoreUnknownKeys = true
    isLenient = true
}

@Serializable
internal data class JourneyEventsConfig(val enabled: Boolean)

@Serializable
internal data class MinimalDeploymentConfig(val journeyEvents: JourneyEventsConfig)

internal class DeploymentConfigChecker(
    private val urls: JourneyUrls,
    private val client: HttpClient,
    private val log: Log,
) {
    internal var isTrackingEnabled: Boolean? = null
        private set

    suspend fun check(): Boolean {
        return try {
            val response = client.get(urls.deploymentConfigUrl)
            val body = response.bodyAsText()
            val config = lenientJson.decodeFromString<MinimalDeploymentConfig>(body)
            isTrackingEnabled = config.journeyEvents.enabled
            if (!config.journeyEvents.enabled) {
                log.i {
                    "Digital User Tracking is disabled in the Messenger Configuration. " +
                        "Enable journeyEvents in deployment configuration to use tracking methods."
                }
            }
            config.journeyEvents.enabled
        } catch (e: Exception) {
            log.e(e) { "Failed to fetch deployment config to check journeyEvents status" }
            isTrackingEnabled = false
            false
        }
    }
}
