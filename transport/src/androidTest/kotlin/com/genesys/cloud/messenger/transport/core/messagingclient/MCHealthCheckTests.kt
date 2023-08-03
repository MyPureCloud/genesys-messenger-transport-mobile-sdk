package com.genesys.cloud.messenger.transport.core.messagingclient

import com.genesys.cloud.messenger.transport.core.events.HEALTH_CHECK_COOL_DOWN_MILLISECONDS
import com.genesys.cloud.messenger.transport.shyrka.send.HealthCheckID
import com.genesys.cloud.messenger.transport.util.Platform
import com.genesys.cloud.messenger.transport.util.Request
import com.genesys.cloud.messenger.transport.util.fromClosedToConnecting
import com.genesys.cloud.messenger.transport.util.fromConnectedToConfigured
import com.genesys.cloud.messenger.transport.util.fromConnectingToConnected
import io.mockk.every
import io.mockk.verify
import io.mockk.verifySequence
import org.junit.Test
import kotlin.test.assertFailsWith

class MCHealthCheckTests : BaseMessagingClientTest() {

    @Test
    fun `when send HealthCheck`() {
        val expectedMessage =
            """{"token":"${Request.token}","action":"echo","message":{"text":"ping","metadata":{"customMessageId":"$HealthCheckID"},"type":"Text"}}"""
        subject.connect()

        subject.sendHealthCheck()

        verifySequence {
            connectSequence()
            mockPlatformSocket.sendMessage(expectedMessage)
        }
    }

    @Test
    fun `when send HealthCheck twice without cool down`() {
        val expectedMessage = Request.echoRequest

        subject.connect()

        subject.sendHealthCheck()
        subject.sendHealthCheck()

        verify(exactly = 1) { mockPlatformSocket.sendMessage(expectedMessage) }
    }

    @Test
    fun `when send HealthCheck twice with cool down`() {
        val healthCheckCoolDownInMilliseconds = HEALTH_CHECK_COOL_DOWN_MILLISECONDS + 250
        val expectedMessage = Request.echoRequest

        subject.connect()

        subject.sendHealthCheck()
        // Fast forward epochMillis by healthCheckCoolDownInMilliseconds.
        every { mockTimestampFunction.invoke() } answers { Platform().epochMillis() + healthCheckCoolDownInMilliseconds }
        subject.sendHealthCheck()

        verify(exactly = 2) { mockPlatformSocket.sendMessage(expectedMessage) }
    }

    @Test
    fun `when connect send HealthCheck reconnect and send HealthCheck again without delay`() {
        val expectedMessage = Request.echoRequest

        subject.connect()
        subject.sendHealthCheck()
        subject.disconnect()
        subject.connect()
        subject.sendHealthCheck()

        verifySequence {
            connectSequence()
            mockPlatformSocket.sendMessage(expectedMessage)
            disconnectSequence()
            mockStateChangedListener(fromClosedToConnecting)
            mockPlatformSocket.openSocket(any())
            mockStateChangedListener(fromConnectingToConnected)
            mockPlatformSocket.sendMessage(Request.configureRequest())
            mockReconnectionHandler.clear()
            mockStateChangedListener(fromConnectedToConfigured)
            mockPlatformSocket.sendMessage(expectedMessage)
        }
    }

    @Test
    fun `when not connected and send HealthCheck`() {
        assertFailsWith<IllegalStateException> {
            subject.sendHealthCheck()
        }
    }
}
