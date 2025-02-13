package com.genesys.cloud.messenger.transport

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.genesys.cloud.messenger.transport.core.MessengerTransportSDK
import com.genesys.cloud.messenger.transport.util.Urls
import com.genesys.cloud.messenger.transport.utility.TestValues
import io.ktor.http.Url
import kotlin.test.Test

class UrlsTest {

    private val subject = Urls(TestValues.Domain, TestValues.DeploymentId)

    @Test
    fun `it should get webSocketUrl`() {
        val expected = Url("wss://webmessaging.${TestValues.Domain}/v1?deploymentId=${TestValues.DeploymentId}&application=TransportSDK-${MessengerTransportSDK.sdkVersion}")

        val result = subject.webSocketUrl

        assertThat(result, "WebSocket URL").isEqualTo(expected)
    }

    @Test
    fun `it should get apiBaseUrl`() {
        val expected = Url("https://api.${TestValues.Domain}")

        val result = subject.apiBaseUrl

        assertThat(result, "API Base URL").isEqualTo(expected)
    }

    @Test
    fun `it should get deploymentConfigUrl`() {
        val expected = Url("https://api-cdn.${TestValues.Domain}/webdeployments/v1/deployments/${TestValues.DeploymentId}/config.json")

        val result = subject.deploymentConfigUrl

        assertThat(result, "Deployment config URL").isEqualTo(expected)
    }

    @Test
    fun `it should get jwtAuthUrl`() {
        val expected = Url("https://api.${TestValues.Domain}/api/v2/webdeployments/token/oauthcodegrantjwtexchange")

        val result = subject.jwtAuthUrl

        assertThat(result, "jwtAuth config URL").isEqualTo(expected)
    }

    @Test
    fun `it should get logoutUrl`() {
        val expected = Url("https://api.${TestValues.Domain}/api/v2/webdeployments/token/revoke")

        val result = subject.logoutUrl

        assertThat(result, "jwtAuth config URL").isEqualTo(expected)
    }

    @Test
    fun `it should get refreshAuthTokenUrl`() {
        val expected = Url("https://api.${TestValues.Domain}/api/v2/webdeployments/token/refresh")

        val result = subject.refreshAuthTokenUrl

        assertThat(result, "jwtAuth config URL").isEqualTo(expected)
    }
}
