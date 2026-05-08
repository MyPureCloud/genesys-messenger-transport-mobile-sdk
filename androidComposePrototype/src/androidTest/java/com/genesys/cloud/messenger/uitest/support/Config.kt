package com.genesys.cloud.messenger.uitest.support

import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

val testConfig by lazy { pullConfig() }

@Serializable
data class Config(
    val agentToken: String,
    val agentEmail: String,
    val agentUserId: String,
    val deploymentId: String,
    val humanizeDisableDeploymentId: String,
    val botDeploymentId: String,
    val quickReplyDeploymentId: String,
    val agentDisconnectDeploymentId: String,
    val authDeploymentId: String,
    val botName: String,
    val password: String,
    val domain: String,
    val apiBaseAddress: String,
    val oktaUsername: String,
    val oktaPassword: String,
    val oktaUser2name: String,
    val oktaPassword2: String
)

private val configJson = Json { ignoreUnknownKeys = true }

private fun pullConfig(): Config {
    val configInputStream =
        InstrumentationRegistry
            .getInstrumentation()
            .context.assets
            .open("testConfig.json")
    return configInputStream.bufferedReader().use { configJson.decodeFromString(it.readText()) }
}
