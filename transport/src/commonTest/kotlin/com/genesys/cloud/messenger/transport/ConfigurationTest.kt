package com.genesys.cloud.messenger.transport

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import com.genesys.cloud.messenger.transport.core.Configuration
import com.genesys.cloud.messenger.transport.core.TlsVersion
import com.genesys.cloud.messenger.transport.utility.TestValues
import kotlin.test.Test

class ConfigurationTest {
    @Test
    fun `validate default constructor`() {
        val expectedDeploymentId = TestValues.DEPLOYMENT_ID
        val expectedReconnectionTimeout = 300L

        TestValues.configuration.run {
            assertThat(deploymentId).isEqualTo(expectedDeploymentId)
            assertThat(logging).isFalse()
            assertThat(reconnectionTimeoutInSeconds).isEqualTo(expectedReconnectionTimeout)
            assertThat(autoRefreshTokenWhenExpired).isTrue()
            assertThat(encryptedVault).isFalse()
            assertThat(minimumWebSocketTlsVersion).isEqualTo(TlsVersion.SYSTEM_DEFAULT)
        }
    }

    @Test
    fun `validate secondary constructor`() {
        val expectedDeploymentId = TestValues.DEPLOYMENT_ID
        val expectedReconnectionTimeout = 1L

        val configuration =
            TestValues.configuration.copy(logging = true, reconnectionTimeoutInSeconds = 1L)

        configuration.run {
            assertThat(deploymentId).isEqualTo(expectedDeploymentId)
            assertThat(logging).isTrue()
            assertThat(reconnectionTimeoutInSeconds).isEqualTo(expectedReconnectionTimeout)
            assertThat(autoRefreshTokenWhenExpired).isTrue()
            assertThat(encryptedVault).isFalse()
            assertThat(minimumWebSocketTlsVersion).isEqualTo(TlsVersion.SYSTEM_DEFAULT)
        }
    }

    @Test
    fun `validate constructor with encryptedVault parameter`() {
        val configuration = TestValues.configuration.copy(encryptedVault = true)

        configuration.run {
            assertThat(deploymentId).isEqualTo(TestValues.DEPLOYMENT_ID)
            assertThat(logging).isFalse()
            assertThat(reconnectionTimeoutInSeconds).isEqualTo(300L)
            assertThat(autoRefreshTokenWhenExpired).isTrue()
            assertThat(encryptedVault).isTrue()
            assertThat(minimumWebSocketTlsVersion).isEqualTo(TlsVersion.SYSTEM_DEFAULT)
        }
    }

    @Test
    fun `validate configuration with non-default TLS version`() {
        val configuration = TestValues.configuration.copy(minimumWebSocketTlsVersion = TlsVersion.TLS_1_2)

        configuration.run {
            assertThat(deploymentId).isEqualTo(TestValues.DEPLOYMENT_ID)
            assertThat(minimumWebSocketTlsVersion).isEqualTo(TlsVersion.TLS_1_2)
        }
    }

    @Test
    fun `when using 4-param secondary constructor then minimumWebSocketTlsVersion defaults to SYSTEM_DEFAULT`() {
        val configuration = Configuration(
            deploymentId = TestValues.DEPLOYMENT_ID,
            domain = TestValues.DOMAIN,
            logging = true,
            reconnectionTimeoutInSeconds = 60,
        )

        assertThat(configuration.minimumWebSocketTlsVersion).isEqualTo(TlsVersion.SYSTEM_DEFAULT)
    }

    @Test
    fun `when using 6-param secondary constructor then minimumWebSocketTlsVersion defaults to SYSTEM_DEFAULT`() {
        val configuration = Configuration(
            deploymentId = TestValues.DEPLOYMENT_ID,
            domain = TestValues.DOMAIN,
            logging = true,
            reconnectionTimeoutInSeconds = 60,
            autoRefreshTokenWhenExpired = false,
            encryptedVault = true,
        )

        assertThat(configuration.minimumWebSocketTlsVersion).isEqualTo(TlsVersion.SYSTEM_DEFAULT)
    }
}
