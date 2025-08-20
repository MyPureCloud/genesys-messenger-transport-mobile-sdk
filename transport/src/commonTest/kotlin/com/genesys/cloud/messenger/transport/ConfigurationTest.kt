package com.genesys.cloud.messenger.transport

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import com.genesys.cloud.messenger.transport.core.Configuration
import com.genesys.cloud.messenger.transport.utility.TestValues
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
}
