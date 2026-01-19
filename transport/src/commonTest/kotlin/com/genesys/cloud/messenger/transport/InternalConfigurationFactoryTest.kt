package com.genesys.cloud.messenger.transport

import com.genesys.cloud.messenger.transport.core.ApplicationType
import com.genesys.cloud.messenger.transport.core.Configuration
import com.genesys.cloud.messenger.transport.core.InternalConfigurationFactory
import com.genesys.cloud.messenger.transport.core.MessengerTransportSDK
import kotlin.test.Test
import kotlin.test.assertEquals

class InternalConfigurationFactoryTest {
    @Test
    fun `when creating configuration with TRANSPORT type it should format application parameter correctly`() {
        val config =
            InternalConfigurationFactory.create(
                deploymentId = "test-deployment",
                domain = "test.com",
                applicationType = ApplicationType.TRANSPORT_SDK,
                applicationVersion = "0.0.0"
            )

        assertEquals(
            "TransportSDK-${MessengerTransportSDK.sdkVersion}",
            config.application
        )
    }

    @Test
    fun `when creating configuration with MESSENGER type it should format application parameter correctly`() {
        val messengerVersion = "1.0.0"
        val config =
            InternalConfigurationFactory.create(
                deploymentId = "test-deployment",
                domain = "test.com",
                applicationType = ApplicationType.MESSENGER_SDK,
                applicationVersion = messengerVersion
            )

        assertEquals(
            "MessengerSDK-$messengerVersion/TransportSDK-${MessengerTransportSDK.sdkVersion}",
            config.application
        )
    }

    @Test
    fun `when creating configuration it uses default sessionExpirationNoticeIntervalSeconds`() {
        val config =
            InternalConfigurationFactory.create(
                deploymentId = "test-deployment",
                domain = "test.com",
                applicationType = ApplicationType.TRANSPORT_SDK,
                applicationVersion = "0.0.0"
            )

        assertEquals(
            Configuration.DEFAULT_INTERVAL,
            config.sessionExpirationNoticeIntervalSeconds
        )
    }

    @Test
    fun `when creating configuration with custom sessionExpirationNoticeIntervalSeconds`() {
        val givenSessionExpirationNoticeIntervalSeconds = 120L
        val config =
            InternalConfigurationFactory.create(
                deploymentId = "test-deployment",
                domain = "test.com",
                applicationType = ApplicationType.TRANSPORT_SDK,
                applicationVersion = "0.0.0",
                sessionExpirationNoticeIntervalSeconds = givenSessionExpirationNoticeIntervalSeconds
            )

        assertEquals(
            givenSessionExpirationNoticeIntervalSeconds,
            config.sessionExpirationNoticeIntervalSeconds
        )
    }
}
