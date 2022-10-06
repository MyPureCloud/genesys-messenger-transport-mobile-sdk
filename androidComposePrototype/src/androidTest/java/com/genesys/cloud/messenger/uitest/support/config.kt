package com.genesys.cloud.messenger.uitest.support

import androidx.test.platform.app.InstrumentationRegistry
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue

val testConfig by lazy { pullConfig() }

data class Config(
    val token: String,
    val agentId: String,
    val deploymentId: String,
    val password: String
)

private fun pullConfig(): Config {
    val mapper = jacksonObjectMapper()
    val configInputStream = InstrumentationRegistry.getInstrumentation().context.assets.open("testConfig.json")
    return mapper.readValue(configInputStream)
}
