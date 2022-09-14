package com.genesys.cloud.messenger.transport.network

import com.genesys.cloud.messenger.transport.DEFAULT_TIMEOUT
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.features.json.serializer.KotlinxSerializer
import io.ktor.http.ContentType
import io.ktor.http.fullPath
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlin.test.Test
import kotlin.test.assertEquals

private const val BASIC_DEPLOYMENT_CONFIG_RESPONSE_PATH =
    "/webdeployments/v1/deployments/deploymentId/config.json"

class DeploymentConfigUseCaseTest {
    private val subject = DeploymentConfigUseCase(false, BASIC_DEPLOYMENT_CONFIG_RESPONSE_PATH, mockHttpClient())

    @Test
    fun whenFetchDeploymentConfig() {
        val expectedTestDeploymentConfig = TestWebMessagingApiResponses.testDeploymentConfig

        val result = runBlocking {
            withTimeout(DEFAULT_TIMEOUT) {
                subject.fetch()
            }
        }
        assertEquals(expectedTestDeploymentConfig, result)
    }

    private fun mockHttpClient(): HttpClient = HttpClient(MockEngine) {
        install(JsonFeature) {
            serializer = KotlinxSerializer()
        }
        engine {
            val responseHeaders =
                headersOf("Content-Type" to listOf(ContentType.Application.Json.toString()))
            addHandler { request ->
                when (request.url.fullPath) {
                    BASIC_DEPLOYMENT_CONFIG_RESPONSE_PATH -> {
                        respond(
                            TestWebMessagingApiResponses.deploymentConfigResponse,
                            headers = responseHeaders
                        )
                    }
                    else -> error("Unhandled ${request.url.fullPath}")
                }
            }
        }
    }
}
