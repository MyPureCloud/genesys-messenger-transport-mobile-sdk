package com.genesys.cloud.messenger.transport.core.messagingclient

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.genesys.cloud.messenger.transport.core.events.Event
import com.genesys.cloud.messenger.transport.core.events.HEALTH_CHECK_COOL_DOWN_MILLISECONDS
import com.genesys.cloud.messenger.transport.util.Platform
import com.genesys.cloud.messenger.transport.util.Request
import com.genesys.cloud.messenger.transport.util.Response
import com.genesys.cloud.messenger.transport.util.fromClosedToConnecting
import com.genesys.cloud.messenger.transport.util.fromConnectedToConfigured
import com.genesys.cloud.messenger.transport.util.fromConnectingToConnected
import com.genesys.cloud.messenger.transport.util.logs.LogMessages
import com.genesys.cloud.messenger.transport.utility.TestValues
import io.mockk.every
import io.mockk.verify
import io.mockk.verifySequence
import org.junit.Test
import kotlin.test.assertFailsWith

class MCHealthCheckTests : BaseMessagingClientTest() {

    @Test
    fun `when send HealthCheck`() {
        every { mockPlatformSocket.sendMessage(Request.echo) } answers {
            slot.captured.onMessage(Response.echo)
        }
        val expectedMessage = Request.echo
        subject.connect()

        subject.sendHealthCheck()

        verifySequence {
            connectSequence()
            mockLogger.i(capture(logSlot))
            mockLogger.i(capture(logSlot))
            mockPlatformSocket.sendMessage(expectedMessage)
            mockEventHandler.onEvent(Event.HealthChecked)
        }
        verify(exactly = 0) {
            mockMessageStore.update(any())
            mockCustomAttributesStore.onSent()
            mockAttachmentHandler.onSent(any())
            userTypingProvider.clear()
        }
    }

    @Test
    fun `when send HealthCheck twice without cool down`() {
        val expectedMessage = Request.echo

        subject.connect()

        subject.sendHealthCheck()
        subject.sendHealthCheck()

        verify(exactly = 1) { mockPlatformSocket.sendMessage(expectedMessage) }
    }

    @Test
    fun `when send HealthCheck twice with cool down`() {
        val healthCheckCoolDownInMilliseconds = HEALTH_CHECK_COOL_DOWN_MILLISECONDS + 250
        val expectedMessage = Request.echo

        subject.connect()

        subject.sendHealthCheck()
        // Fast forward epochMillis by healthCheckCoolDownInMilliseconds.
        every { mockTimestampFunction.invoke() } answers { Platform().epochMillis() + healthCheckCoolDownInMilliseconds }
        subject.sendHealthCheck()

        verify(exactly = 2) { mockPlatformSocket.sendMessage(expectedMessage) }
    }

    @Test
    fun `when connect send HealthCheck reconnect and send HealthCheck again without delay`() {
        val expectedMessage = Request.echo

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
            mockAttachmentHandler.fileAttachmentProfile = any()
            mockReconnectionHandler.clear()
            mockJwtHandler.clear()
            mockCustomAttributesStore.maxCustomDataBytes = TestValues.MaxCustomDataBytes
            mockStateChangedListener(fromConnectedToConfigured)
            mockLogger.i(capture(logSlot))
            mockLogger.i(capture(logSlot))
            mockPlatformSocket.sendMessage(expectedMessage)
        }
        assertThat(logSlot[0].invoke()).isEqualTo(LogMessages.CONNECT)
        assertThat(logSlot[1].invoke()).isEqualTo(LogMessages.configureSession(Request.token, false))
        assertThat(logSlot[2].invoke()).isEqualTo(LogMessages.SEND_HEALTH_CHECK)
        assertThat(logSlot[3].invoke()).isEqualTo(LogMessages.WILL_SEND_MESSAGE)
        assertThat(logSlot[4].invoke()).isEqualTo(LogMessages.DISCONNECT)
        assertThat(logSlot[5].invoke()).isEqualTo(LogMessages.CLEAR_CONVERSATION_HISTORY)
        assertThat(logSlot[6].invoke()).isEqualTo(LogMessages.CONNECT)
        assertThat(logSlot[7].invoke()).isEqualTo(LogMessages.configureSession(Request.token, false))
        assertThat(logSlot[8].invoke()).isEqualTo(LogMessages.SEND_HEALTH_CHECK)
        assertThat(logSlot[9].invoke()).isEqualTo(LogMessages.WILL_SEND_MESSAGE)
    }

    @Test
    fun `when not connected and send HealthCheck`() {
        assertFailsWith<IllegalStateException> {
            subject.sendHealthCheck()
        }
    }

    @Test
    fun `when SocketListener invoke onMessage with HealthCheck response message`() {
        subject.connect()

        slot.captured.onMessage(Response.healthCheckResponse)

        verifySequence {
            connectSequence()
            mockEventHandler.onEvent(Event.HealthChecked)
        }
    }
}
