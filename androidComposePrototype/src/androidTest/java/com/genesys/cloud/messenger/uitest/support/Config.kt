package com.genesys.cloud.messenger.uitest.support

import androidx.test.platform.app.InstrumentationRegistry
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue

val testConfig by lazy { pullConfig() }

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

private fun pullConfig(): Config {
    val mapper = jacksonObjectMapper()
    val configInputStream = InstrumentationRegistry
        .getInstrumentation()
        .context.assets
        .open("testConfig.json")
    return mapper.readValue(configInputStream)
}
