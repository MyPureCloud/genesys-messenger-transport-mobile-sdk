package com.genesys.cloud.messenger.transport

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.genesys.cloud.messenger.transport.core.Configuration
import com.genesys.cloud.messenger.transport.core.MessengerTransport
import io.ktor.http.Url
import kotlin.test.Test

class RegionConfigurationTest {

    @Test
    fun itShouldGetWebSocketUrl() {
        val configuration = Configuration(
            deploymentId = "foo",
            domain = "mypurecloud.com",
        )

        val actual = configuration.webSocketUrl
        val expected = Url("wss://webmessaging.mypurecloud.com/v1?deploymentId=foo&application=TransportSDK-${MessengerTransport.sdkVersion}")

        assertThat(actual, "WebSocket URL").isEqualTo(expected)
    }

    @Test
    fun itShouldGetApiBaseUrl() {
        val configuration = Configuration(
            deploymentId = "foo",
            domain = "mypurecloud.com",
        )

        val actual = configuration.apiBaseUrl
        val expected = Url("https://api.mypurecloud.com")

        assertThat(actual, "API Base URL").isEqualTo(expected)
    }

    @Test
    fun itShouldGetDeploymentConfigUrl() {
        val configuration = Configuration(
            deploymentId = "foo",
            domain = "mypurecloud.com",
        )

        val actual = configuration.deploymentConfigUrl
        val expected = Url("https://api-cdn.mypurecloud.com/webdeployments/v1/deployments/foo/config.json")

        assertThat(actual, "Deployment config URL").isEqualTo(expected)
    }
}
