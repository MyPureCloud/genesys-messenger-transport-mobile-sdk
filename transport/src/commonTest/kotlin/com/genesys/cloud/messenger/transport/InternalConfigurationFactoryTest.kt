package com.genesys.cloud.messenger.transport

import com.genesys.cloud.messenger.transport.core.ApplicationType
import com.genesys.cloud.messenger.transport.core.Configuration
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

    @Test
    fun `when creating configuration without customBaseUrl it should default to null`() {
        val config =
            InternalConfigurationFactory.create(
                deploymentId = "test-deployment",
                domain = "test.com",
                applicationType = ApplicationType.TRANSPORT_SDK,
                applicationVersion = "0.0.0"
            )

        assertEquals(null, config.customBaseUrl)
    }

    @Test
    fun `when creating configuration with customBaseUrl it should be set on Configuration`() {
        val config =
            InternalConfigurationFactory.create(
                deploymentId = "test-deployment",
                domain = "test.com",
                applicationType = ApplicationType.TRANSPORT_SDK,
                applicationVersion = "0.0.0",
                customBaseUrl = "localhost:8080"
            )

        assertEquals("localhost:8080", config.customBaseUrl)
    }

    @Test
    fun `when using backward compatible overload it should default customBaseUrl to null`() {
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

        assertEquals(null, config.customBaseUrl)
    }
}
