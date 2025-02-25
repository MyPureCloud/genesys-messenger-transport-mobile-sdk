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
}
