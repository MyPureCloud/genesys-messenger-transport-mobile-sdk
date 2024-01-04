package com.genesys.cloud.messenger.transport.core.messagingclient

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.genesys.cloud.messenger.transport.core.events.HEALTH_CHECK_COOL_DOWN_MILLISECONDS
import com.genesys.cloud.messenger.transport.shyrka.send.HealthCheckID
import com.genesys.cloud.messenger.transport.util.Platform
import com.genesys.cloud.messenger.transport.util.Request
import com.genesys.cloud.messenger.transport.util.fromClosedToConnecting
import com.genesys.cloud.messenger.transport.util.fromConnectedToConfigured
import com.genesys.cloud.messenger.transport.util.fromConnectingToConnected
import com.genesys.cloud.messenger.transport.utility.LogMessages
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
            mockLogger.i(capture(logSlot))
            mockLogger.i(capture(logSlot))
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
            mockLogger.i(capture(logSlot))
            mockLogger.i(capture(logSlot))
            mockPlatformSocket.sendMessage(expectedMessage)
            disconnectSequence()
            mockLogger.i(capture(logSlot))
            mockStateChangedListener(fromClosedToConnecting)
            mockPlatformSocket.openSocket(any())
            mockStateChangedListener(fromConnectingToConnected)
            mockLogger.i(capture(logSlot))
            mockPlatformSocket.sendMessage(Request.configureRequest())
            mockReconnectionHandler.clear()
            mockStateChangedListener(fromConnectedToConfigured)
            mockLogger.i(capture(logSlot))
            mockLogger.i(capture(logSlot))
            mockPlatformSocket.sendMessage(expectedMessage)
        }
        assertThat(logSlot[0].invoke()).isEqualTo(LogMessages.Connect)
        assertThat(logSlot[1].invoke()).isEqualTo(LogMessages.ConfigureSession)
        assertThat(logSlot[2].invoke()).isEqualTo(LogMessages.HealthCheck)
        assertThat(logSlot[3].invoke()).isEqualTo(LogMessages.WillSendMessage)
        assertThat(logSlot[4].invoke()).isEqualTo(LogMessages.Disconnect)
        assertThat(logSlot[5].invoke()).isEqualTo(LogMessages.ClearConversationHistory)
        assertThat(logSlot[6].invoke()).isEqualTo(LogMessages.Connect)
        assertThat(logSlot[7].invoke()).isEqualTo(LogMessages.ConfigureSession)
        assertThat(logSlot[8].invoke()).isEqualTo(LogMessages.HealthCheck)
        assertThat(logSlot[9].invoke()).isEqualTo(LogMessages.WillSendMessage)
    }

    @Test
    fun `when not connected and send HealthCheck`() {
        assertFailsWith<IllegalStateException> {
            subject.sendHealthCheck()
        }
    }
}
