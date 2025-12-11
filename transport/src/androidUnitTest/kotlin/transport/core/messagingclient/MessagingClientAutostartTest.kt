package transport.core.messagingclient

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.genesys.cloud.messenger.transport.core.ErrorCode
import com.genesys.cloud.messenger.transport.core.ErrorMessage
import com.genesys.cloud.messenger.transport.core.Message
import com.genesys.cloud.messenger.transport.core.TransportSDKException
import com.genesys.cloud.messenger.transport.core.events.Event
import com.genesys.cloud.messenger.transport.shyrka.receive.Apps
import com.genesys.cloud.messenger.transport.shyrka.receive.Conversations
import com.genesys.cloud.messenger.transport.shyrka.receive.createConversationsVOForTesting
import com.genesys.cloud.messenger.transport.shyrka.receive.createDeploymentConfigForTesting
import com.genesys.cloud.messenger.transport.shyrka.receive.createMessengerVOForTesting
import com.genesys.cloud.messenger.transport.util.logs.LogMessages
import io.mockk.every
import io.mockk.verify
import io.mockk.verifySequence
import org.junit.Test
import transport.util.Request
import transport.util.Response
import kotlin.test.assertFailsWith

class MessagingClientAutostartTest : BaseMessagingClientTest() {
    @Test
    fun `when new session and autostart enabled`() {
        every { mockCustomAttributesStore.getCustomAttributesToSend() } returns emptyMap()
        every { mockDeploymentConfig.get() } returns
            createDeploymentConfigForTesting(
                messenger =
                    createMessengerVOForTesting(
                        apps =
                            Apps(
                                conversations =
                                    createConversationsVOForTesting(
                                        autoStart =
                                            Conversations.AutoStart(
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
            mockPlatformSocket.sendMessage(match { Request.isAutostartRequest(it) })
        }
        assertThat(logSlot[0].invoke()).isEqualTo(LogMessages.CONNECT)
        assertThat(logSlot[1].invoke()).isEqualTo(LogMessages.configureSession(Request.token))
        assertThat(logSlot[2].invoke()).isEqualTo(LogMessages.SEND_AUTO_START)
        assertThat(logSlot[3].invoke()).isEqualTo(LogMessages.WILL_SEND_MESSAGE)
    }

    @Test
    fun `when old session and autostart enabled`() {
        every { mockPlatformSocket.sendMessage(match { Request.isConfigureRequest(it) }) } answers {
            slot.captured.onMessage(Response.configureSuccessWithNewSessionFalse)
        }
        every { mockDeploymentConfig.get() } returns
            createDeploymentConfigForTesting(
                messenger =
                    createMessengerVOForTesting(
                        apps =
                            Apps(
                                conversations =
                                    createConversationsVOForTesting(
                                        autoStart =
                                            Conversations.AutoStart(
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
            mockPlatformSocket.sendMessage(match { Request.isAutostartRequest(it) })
        }
    }

    @Test
    fun `when old session and autostart disabled`() {
        every { mockPlatformSocket.sendMessage(match { Request.isConfigureRequest(it) }) } answers {
            slot.captured.onMessage(Response.configureSuccessWithNewSessionFalse)
        }

        subject.connect()

        verify(exactly = 0) { mockPlatformSocket.sendMessage(match { Request.isAutostartRequest(it) }) }
    }

    @Test
    fun `when new session and autostart disabled`() {
        subject.connect()

        verify(exactly = 0) {
            mockCustomAttributesStore.getCustomAttributesToSend()
            mockCustomAttributesStore.onSending()
            mockPlatformSocket.sendMessage(match { Request.isAutostartRequest(it) })
        }
    }

    @Test
    fun `when new session and deploymentConfig not set`() {
        every { mockDeploymentConfig.get() } returns null

        val exception =
            assertFailsWith<TransportSDKException> {
                subject.connect()
            }

        assertThat(exception.errorCode).isEqualTo(ErrorCode.MissingDeploymentConfig)
        assertThat(exception.message).isEqualTo(ErrorMessage.MissingDeploymentConfig)

        verify(exactly = 0) {
            mockCustomAttributesStore.getCustomAttributesToSend()
            mockCustomAttributesStore.onSending()
            mockPlatformSocket.sendMessage(match { Request.isAutostartRequest(it) })
        }
    }

    @Test
    fun `when event Presence Join received`() {
        val expectedEvent = Event.ConversationAutostart

        subject.connect()
        slot.captured.onMessage(
            Response.structuredMessageWithEvents(
                direction = Message.Direction.Inbound,
                events = Response.StructuredEvent.presenceJoin
            )
        )

        verify {
            mockCustomAttributesStore.onSent()
            mockEventHandler.onEvent(eq(expectedEvent))
        }
    }
}
