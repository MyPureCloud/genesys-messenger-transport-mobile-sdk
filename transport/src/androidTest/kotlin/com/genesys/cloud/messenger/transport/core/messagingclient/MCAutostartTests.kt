package com.genesys.cloud.messenger.transport.core.messagingclient

import com.genesys.cloud.messenger.transport.core.events.Event
import com.genesys.cloud.messenger.transport.shyrka.receive.Apps
import com.genesys.cloud.messenger.transport.shyrka.receive.Conversations
import com.genesys.cloud.messenger.transport.shyrka.receive.createConversationsVOForTesting
import com.genesys.cloud.messenger.transport.shyrka.receive.createDeploymentConfigForTesting
import com.genesys.cloud.messenger.transport.shyrka.receive.createMessengerVOForTesting
import com.genesys.cloud.messenger.transport.util.Request
import com.genesys.cloud.messenger.transport.util.Response
import io.mockk.every
import io.mockk.verify
import io.mockk.verifySequence
import org.junit.Test

class MCAutostartTests : BaseMessagingClientTest() {

    @Test
    fun `when new session and autostart enabled`() {
        every { mockDeploymentConfig.get() } returns createDeploymentConfigForTesting(
            messenger = createMessengerVOForTesting(
                apps = Apps(
                    conversations = createConversationsVOForTesting(
                        autoStart = Conversations.AutoStart(enabled = true)
                    )
                )
            )
        )

        subject.connect()

        verifySequence {
            connectSequence()
            mockPlatformSocket.sendMessage(Request.autostart)
        }
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
                        autoStart = Conversations.AutoStart(enabled = true)
                    )
                )
            )
        )

        subject.connect()

        verify(exactly = 0) { mockPlatformSocket.sendMessage(Request.autostart) }
    }

    @Test
    fun `when old session and autostart disabled`() {
        every { mockPlatformSocket.sendMessage(Request.configureRequest()) } answers {
            slot.captured.onMessage(Response.configureSuccessWithNewSessionFalse)
        }

        subject.connect()

        verify(exactly = 0) { mockPlatformSocket.sendMessage(Request.autostart) }
    }

    @Test
    fun `when new session and autostart disabled`() {
        subject.connect()

        verify(exactly = 0) { mockPlatformSocket.sendMessage(Request.autostart) }
    }

    @Test
    fun `when new session and deploymentConfig not set`() {
        every { mockDeploymentConfig.get() } returns null

        subject.connect()

        verify(exactly = 0) { mockPlatformSocket.sendMessage(Request.autostart) }
    }

    @Test
    fun `when event Presence Join received`() {
        val givenPresenceJoinEvent = """{"eventType":"Presence","presence":{"type":"Join"}}"""
        val expectedEvent = Event.ConversationAutostart

        subject.connect()
        slot.captured.onMessage(Response.structuredMessageWithEvents(events = givenPresenceJoinEvent))

        verify {
            mockEventHandler.onEvent(eq(expectedEvent))
        }
    }
}