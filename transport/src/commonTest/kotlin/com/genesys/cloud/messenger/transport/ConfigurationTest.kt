package com.genesys.cloud.messenger.transport

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isInstanceOf
import assertk.assertions.isTrue
import com.genesys.cloud.messenger.transport.core.Configuration
import com.genesys.cloud.messenger.transport.core.MessengerTransportSDK
import com.genesys.cloud.messenger.transport.utility.MockVault
import com.genesys.cloud.messenger.transport.utility.TestValues
import io.ktor.http.Url
import kotlin.test.Test

class ConfigurationTest {

    @Test
    fun `validate default constructor`() {
        val expectedDeploymentId = TestValues.DEPLOYMENT_ID
        val expectedReconnectionTimeout = 300L

        val configuration = Configuration(
            deploymentId = TestValues.DEPLOYMENT_ID,
            domain = TestValues.DOMAIN
        )

        configuration.run {
            assertThat(deploymentId).isEqualTo(expectedDeploymentId)
            assertThat(logging).isFalse()
            assertThat(reconnectionTimeoutInSeconds).isEqualTo(expectedReconnectionTimeout)
            assertThat(autoRefreshTokenWhenExpired).isTrue()
            assertThat(encryptedVault).isFalse()
        }
    }

    @Test
    fun `validate secondary constructor`() {
        val expectedDeploymentId = TestValues.DEPLOYMENT_ID
        val expectedReconnectionTimeout = 1L

        val configuration = Configuration(
            deploymentId = TestValues.DEPLOYMENT_ID,
            domain = TestValues.DOMAIN,
            logging = true,
            reconnectionTimeoutInSeconds = 1L
        )

        configuration.run {
            assertThat(deploymentId).isEqualTo(expectedDeploymentId)
            assertThat(logging).isTrue()
            assertThat(reconnectionTimeoutInSeconds).isEqualTo(expectedReconnectionTimeout)
            assertThat(autoRefreshTokenWhenExpired).isTrue()
            assertThat(encryptedVault).isFalse()
        }
    }

    @Test
    fun `validate constructor with encryptedVault parameter`() {
        val configuration = Configuration(
            deploymentId = TestValues.DEPLOYMENT_ID,
            domain = TestValues.DOMAIN,
            encryptedVault = true
        )

        configuration.run {
            assertThat(deploymentId).isEqualTo(TestValues.DEPLOYMENT_ID)
            assertThat(logging).isFalse()
            assertThat(reconnectionTimeoutInSeconds).isEqualTo(300L)
            assertThat(autoRefreshTokenWhenExpired).isTrue()
            assertThat(encryptedVault).isTrue()
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

    @Test
    fun `should use custom vault when provided regardless of encryptedVault setting`() {
        // Given
        val configuration = Configuration(
            deploymentId = TestValues.DEPLOYMENT_ID,
            domain = TestValues.DOMAIN,
            encryptedVault = true
        )
        val mockVault = MockVault()

        // When
        val sdk = MessengerTransportSDK(configuration, mockVault)

        // Then
        assertThat(sdk.vault).isEqualTo(mockVault)
        assertThat(sdk.vault).isInstanceOf(MockVault::class)
    }
}
