package com.genesys.cloud.messenger.transport.core.messagingclient

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.genesys.cloud.messenger.transport.core.Message
import com.genesys.cloud.messenger.transport.core.events.Event
import com.genesys.cloud.messenger.transport.shyrka.receive.Apps
import com.genesys.cloud.messenger.transport.shyrka.receive.Conversations
import com.genesys.cloud.messenger.transport.shyrka.receive.createConversationsVOForTesting
import com.genesys.cloud.messenger.transport.shyrka.receive.createDeploymentConfigForTesting
import com.genesys.cloud.messenger.transport.shyrka.receive.createMessengerVOForTesting
import com.genesys.cloud.messenger.transport.util.Request
import com.genesys.cloud.messenger.transport.util.Response
import com.genesys.cloud.messenger.transport.utility.LogMessages
import io.mockk.every
import io.mockk.verify
import io.mockk.verifySequence
import org.junit.Test

class MCAutostartTests : BaseMessagingClientTest() {

    @Test
    fun `when new session and autostart enabled`() {
        every { mockCustomAttributesStore.getCustomAttributesToSend() } returns emptyMap()
        every { mockDeploymentConfig.get() } returns createDeploymentConfigForTesting(
            messenger = createMessengerVOForTesting(
                apps = Apps(
                    conversations = createConversationsVOForTesting(
                        autoStart = Conversations.AutoStart(
                            enabled = true
                        )
                    )
                )
            )
        )

        subject.connect()

        verifySequence {
            connectSequence()
            mockCustomAttributesStore.getCustomAttributesToSend()
            mockLogger.i(capture(logSlot))
            mockLogger.i(capture(logSlot))
            mockPlatformSocket.sendMessage(Request.autostart(""))
        }
        assertThat(logSlot[0].invoke()).isEqualTo(LogMessages.Connect)
        assertThat(logSlot[1].invoke()).isEqualTo(LogMessages.ConfigureSession)
        assertThat(logSlot[2].invoke()).isEqualTo(LogMessages.Autostart)
        assertThat(logSlot[3].invoke()).isEqualTo(LogMessages.WillSendMessage)
    }

    @Test
    fun `when old session and autostart enabled`() {
        every { mockPlatformSocket.sendMessage(Request.configureRequest()) } answers {
            slot.captured.onMessage(Response.configureSuccessWithNewSessionFalse)
        }
        every { mockDeploymentConfig.get() } returns createDeploymentConfigForTesting(
            messenger = createMessengerVOForTesting(
                apps = Apps(
                    conversations = createConversationsVOForTesting(
                        autoStart = Conversations.AutoStart(
                            enabled = true
                        )
                    )
                )
            )
        )

        subject.connect()

        verify(exactly = 0) {
            mockCustomAttributesStore.getCustomAttributesToSend()
            mockCustomAttributesStore.onSending()
            mockPlatformSocket.sendMessage(Request.autostart())
        }
    }

    @Test
    fun `when old session and autostart disabled`() {
        every { mockPlatformSocket.sendMessage(Request.configureRequest()) } answers {
            slot.captured.onMessage(Response.configureSuccessWithNewSessionFalse)
        }

        subject.connect()

        verify(exactly = 0) { mockPlatformSocket.sendMessage(Request.autostart()) }
    }

    @Test
    fun `when new session and autostart disabled`() {
        subject.connect()

        verify(exactly = 0) {
            mockCustomAttributesStore.getCustomAttributesToSend()
            mockCustomAttributesStore.onSending()
            mockPlatformSocket.sendMessage(Request.autostart())
        }
    }

    @Test
    fun `when new session and deploymentConfig not set`() {
        every { mockDeploymentConfig.get() } returns null

        subject.connect()

        verify(exactly = 0) {
            mockCustomAttributesStore.getCustomAttributesToSend()
            mockCustomAttributesStore.onSending()
            mockPlatformSocket.sendMessage(Request.autostart())
        }
    }

    @Test
    fun `when event Presence Join received`() {
        val givenPresenceJoinEvent = """{"eventType":"Presence","presence":{"type":"Join"}}"""
        val expectedEvent = Event.ConversationAutostart

        subject.connect()
        slot.captured.onMessage(
            Response.structuredMessageWithEvents(
                direction = Message.Direction.Inbound,
                events = givenPresenceJoinEvent
            )
        )

        verify {
            mockCustomAttributesStore.onSent()
            mockEventHandler.onEvent(eq(expectedEvent))
        }
    }
}
