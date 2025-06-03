package com.genesys.cloud.messenger.transport

import com.genesys.cloud.messenger.transport.core.ApplicationType
import com.genesys.cloud.messenger.transport.core.InternalConfigurationFactory
import com.genesys.cloud.messenger.transport.core.MessengerTransportSDK
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class InternalConfigurationFactoryTest {

    @Test
    fun `when creating configuration with TRANSPORT type it should format application parameter correctly`() {
        val config = InternalConfigurationFactory.create(
            deploymentId = "test-deployment",
            domain = "test.com",
            applicationType = ApplicationType.TRANSPORT_SDK
        )

        assertEquals(
            "TransportSDK-${MessengerTransportSDK.sdkVersion}",
            config.application
        )
    }

    @Test
    fun `when creating configuration with MESSENGER type it should format application parameter correctly`() {
        val messengerVersion = "1.0.0"
        val config = InternalConfigurationFactory.create(
            deploymentId = "test-deployment",
            domain = "test.com",
            applicationType = ApplicationType.MESSENGER_SDK,
            messengerSDKVersion = messengerVersion
        )

        assertEquals(
            "MessengerSDK-$messengerVersion/TransportSDK-${MessengerTransportSDK.sdkVersion}",
            config.application
        )
    }

    @Test
    fun `when creating configuration with MESSENGER type without version it should throw exception`() {
        assertFailsWith<IllegalArgumentException> {
            InternalConfigurationFactory.create(
                deploymentId = "test-deployment",
                domain = "test.com",
                applicationType = ApplicationType.MESSENGER_SDK
            )
        }
    }

    @Test
    fun `when creating configuration with default type it should use TRANSPORT format`() {
        val config = InternalConfigurationFactory.create(
            deploymentId = "test-deployment",
            domain = "test.com"
        )

        assertEquals(
            "TransportSDK-${MessengerTransportSDK.sdkVersion}",
            config.application
        )
    }
}
