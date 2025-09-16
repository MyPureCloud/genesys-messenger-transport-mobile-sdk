package com.genesys.cloud.messenger.transport.network.test_engines

import com.genesys.cloud.messenger.transport.respondNotFound
import com.genesys.cloud.messenger.transport.shyrka.receive.DeploymentConfig
import com.genesys.cloud.messenger.transport.shyrka.receive.createDeploymentConfigForTesting
import com.genesys.cloud.messenger.transport.utility.ErrorTest
import com.genesys.cloud.messenger.transport.utility.InvalidValues
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.mock.MockEngineConfig
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondBadRequest
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.fullPath
import io.ktor.http.headersOf
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json

private const val DEPLOYMENT_CONFIG_PATH = "/webdeployments/v1/deployments/deploymentId/config.json"

internal fun HttpClientConfig<MockEngineConfig>.deploymentConfigEngine() {
    engine {
        addHandler { request ->
            when (request.url.fullPath) {
                DEPLOYMENT_CONFIG_PATH -> {
                    if (request.method == HttpMethod.Get) {
                        when {
                            request.url.host.contains(InvalidValues.CANCELLATION_EXCEPTION) -> {
                                throw CancellationException(ErrorTest.MESSAGE)
                            }
                            request.url.host.contains(InvalidValues.UNKNOWN_EXCEPTION) -> {
                                error(ErrorTest.MESSAGE)
                            }
                            request.url.host.contains(InvalidValues.DOMAIN) -> {
                                respondBadRequest()
                            }
                            else -> {
                                respond(
                                    status = HttpStatusCode.OK,
                                    headers = headersOf(
                                        HttpHeaders.ContentType,
                                        "application/json"
                                    ),
                                    content = Json.encodeToString(
                                        DeploymentConfig.serializer(),
                                        createDeploymentConfigForTesting()
                                    )
                                )
                            }
                        }
                    } else {
                        respondBadRequest()
                    }
                }
                else -> {
                    respondNotFound()
                }
            }
        }
    }
}
