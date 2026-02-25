package com.genesys.cloud.messenger.transport

import com.genesys.cloud.messenger.transport.core.ApplicationType
import com.genesys.cloud.messenger.transport.core.InternalConfigurationFactory
import com.genesys.cloud.messenger.transport.core.MessengerTransportSDK
import com.genesys.cloud.messenger.transport.core.TlsVersion
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
    fun `when creating configuration without minimumWebSocketTlsVersion it should default to SYSTEM_DEFAULT`() {
        val config =
            InternalConfigurationFactory.create(
                deploymentId = "test-deployment",
                domain = "test.com",
                applicationType = ApplicationType.TRANSPORT_SDK,
                applicationVersion = "0.0.0"
            )

        assertEquals(TlsVersion.SYSTEM_DEFAULT, config.minimumWebSocketTlsVersion)
    }

    @Test
    fun `when creating configuration with minimumWebSocketTlsVersion it should be propagated to Configuration`() {
        val config =
            InternalConfigurationFactory.create(
                deploymentId = "test-deployment",
                domain = "test.com",
                applicationType = ApplicationType.TRANSPORT_SDK,
                applicationVersion = "0.0.0",
                minimumWebSocketTlsVersion = TlsVersion.TLS_1_3
            )

        assertEquals(TlsVersion.TLS_1_3, config.minimumWebSocketTlsVersion)
    }

    @Test
    fun `when using backward compatible overload it should default minimumWebSocketTlsVersion to SYSTEM_DEFAULT`() {
        val config =
            InternalConfigurationFactory.create(
                deploymentId = "test-deployment",
                domain = "test.com",
                applicationType = ApplicationType.TRANSPORT_SDK,
                applicationVersion = "0.0.0",
                logging = true,
                reconnectionTimeoutInSeconds = 300,
                autoRefreshTokenWhenExpired = true,
                encryptedVault = false
            )

        assertEquals(TlsVersion.SYSTEM_DEFAULT, config.minimumWebSocketTlsVersion)
    }
}
