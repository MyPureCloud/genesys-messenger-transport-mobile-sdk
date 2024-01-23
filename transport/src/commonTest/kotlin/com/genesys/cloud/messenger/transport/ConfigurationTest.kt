package com.genesys.cloud.messenger.transport

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import com.genesys.cloud.messenger.transport.core.Configuration
import com.genesys.cloud.messenger.transport.core.MessengerTransportSDK
import com.genesys.cloud.messenger.transport.utility.TestValues
import io.ktor.http.Url
import kotlin.test.Test

class ConfigurationTest {

    @Test
    fun `validate default constructor`() {
        val expectedDeploymentId = TestValues.DeploymentId
        val expectedReconnectionTimeout = 300L

        val configuration = Configuration(
            deploymentId = TestValues.DeploymentId,
            domain = TestValues.Domain
        )

        configuration.run {
            assertThat(deploymentId).isEqualTo(expectedDeploymentId)
            assertThat(logging).isFalse()
            assertThat(reconnectionTimeoutInSeconds).isEqualTo(expectedReconnectionTimeout)
            assertThat(autoRefreshTokenWhenExpired).isTrue()
        }
    }

    @Test
    fun `validate secondary constructor`() {
        val expectedDeploymentId = TestValues.DeploymentId
        val expectedReconnectionTimeout = 1L

        val configuration = Configuration(
            deploymentId = TestValues.DeploymentId,
            domain = TestValues.Domain,
            logging = true,
            reconnectionTimeoutInSeconds = 1L
        )

        configuration.run {
            assertThat(deploymentId).isEqualTo(expectedDeploymentId)
            assertThat(logging).isTrue()
            assertThat(reconnectionTimeoutInSeconds).isEqualTo(expectedReconnectionTimeout)
            assertThat(autoRefreshTokenWhenExpired).isTrue()
        }
    }

    @Test
    fun `it should get webSocketUrl`() {
        val configuration = Configuration(
            deploymentId = "foo",
            domain = "mypurecloud.com",
        )

        val actual = configuration.webSocketUrl
        val expected = Url("wss://webmessaging.mypurecloud.com/v1?deploymentId=foo&application=TransportSDK-${MessengerTransportSDK.sdkVersion}")

        assertThat(actual, "WebSocket URL").isEqualTo(expected)
    }

    @Test
    fun `it should get apiBaseUrl`() {
        val configuration = Configuration(
            deploymentId = "foo",
            domain = "mypurecloud.com",
        )
        val expected = Url("https://api.mypurecloud.com")

        val result = configuration.apiBaseUrl

        assertThat(result, "API Base URL").isEqualTo(expected)
    }

    @Test
    fun `it should get deploymentConfigUrl`() {
        val configuration = Configuration(
            deploymentId = "foo",
            domain = "mypurecloud.com",
        )
        val expected = Url("https://api-cdn.mypurecloud.com/webdeployments/v1/deployments/foo/config.json")

        val result = configuration.deploymentConfigUrl

        assertThat(result, "Deployment config URL").isEqualTo(expected)
    }

    @Test
    fun `it should get jwtAuthUrl`() {
        val configuration = Configuration(
            deploymentId = "foo",
            domain = "mypurecloud.com",
        )
        val expected = Url("https://api.mypurecloud.com/api/v2/webdeployments/token/oauthcodegrantjwtexchange")

        val result = configuration.jwtAuthUrl

        assertThat(result, "jwtAuth config URL").isEqualTo(expected)
    }

    @Test
    fun `it should get logoutUrl`() {
        val configuration = Configuration(
            deploymentId = "foo",
            domain = "mypurecloud.com",
        )
        val expected = Url("https://api.mypurecloud.com/api/v2/webdeployments/token/revoke")

        val result = configuration.logoutUrl

        assertThat(result, "jwtAuth config URL").isEqualTo(expected)
    }

    @Test
    fun `it should get refreshAuthTokenUrl`() {
        val configuration = Configuration(
            deploymentId = "foo",
            domain = "mypurecloud.com",
        )
        val expected = Url("https://api.mypurecloud.com/api/v2/webdeployments/token/refresh")

        val result = configuration.refreshAuthTokenUrl

        assertThat(result, "jwtAuth config URL").isEqualTo(expected)
    }
}
