package transport.core.messagingclient

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.genesys.cloud.messenger.transport.core.CorrectiveAction
import com.genesys.cloud.messenger.transport.core.ErrorCode
import com.genesys.cloud.messenger.transport.core.Message
import com.genesys.cloud.messenger.transport.core.events.Event
import com.genesys.cloud.messenger.transport.shyrka.receive.Apps
import com.genesys.cloud.messenger.transport.shyrka.receive.Conversations
import com.genesys.cloud.messenger.transport.shyrka.receive.createConversationsVOForTesting
import com.genesys.cloud.messenger.transport.shyrka.receive.createDeploymentConfigForTesting
import com.genesys.cloud.messenger.transport.shyrka.receive.createMessengerVOForTesting
import com.genesys.cloud.messenger.transport.shyrka.send.Channel
import com.genesys.cloud.messenger.transport.shyrka.send.OnMessageRequest
import com.genesys.cloud.messenger.transport.shyrka.send.TextMessage
import com.genesys.cloud.messenger.transport.util.logs.LogMessages
import com.genesys.cloud.messenger.transport.utility.QuickReplyTestValues
import io.mockk.MockKVerificationScope
import io.mockk.every
import io.mockk.verify
import io.mockk.verifySequence
import org.junit.Test
import transport.util.Request
import transport.util.Response

class MCCustomAttributesTests : BaseMessagingClientTest() {

    @Test
    fun `when getCustomAttributesStore()`() {
        val result = subject.customAttributesStore

        assertThat(result).isEqualTo(mockCustomAttributesStore)
    }

    @Test
    fun `when sendMessage with customAttributes`() {
        val expectedMessage =
            """{"token":"${Request.token}","message":{"text":"Hello world","channel":{"metadata":{"customAttributes":{"A":"B"}}},"type":"Text"},"action":"onMessage"}"""
        val expectedText = "Hello world"
        val expectedCustomAttributes = mapOf("A" to "B")
        val expectedChannel = Channel(Channel.Metadata(expectedCustomAttributes))
        every { mockMessageStore.prepareMessage(any(), any(), any()) } returns OnMessageRequest(
            token = Request.token,
            message = TextMessage(
                text = "Hello world",
                channel = expectedChannel,
            ),
        )
        every { mockCustomAttributesStore.getCustomAttributesToSend() } returns mapOf("A" to "B")
        subject.connect()

        subject.sendMessage(text = "Hello world", customAttributes = mapOf("A" to "B"))

        verifySequence {
            connectSequence()
            mockLogger.i(capture(logSlot))
            mockCustomAttributesStore.add(expectedCustomAttributes)
            mockCustomAttributesStore.getCustomAttributesToSend()
            mockCustomAttributesStore.onSending()
            mockMessageStore.prepareMessage(Request.token, expectedText, expectedChannel)
            mockAttachmentHandler.onSending()
            mockLogger.i(capture(logSlot))
            mockPlatformSocket.sendMessage(expectedMessage)
        }
        assertThat(logSlot[0].invoke()).isEqualTo(LogMessages.CONNECT)
        assertThat(logSlot[1].invoke()).isEqualTo(LogMessages.configureSession(Request.token, false))
        assertThat(logSlot[2].invoke()).isEqualTo(LogMessages.sendMessage(expectedText, expectedCustomAttributes))
        assertThat(logSlot[3].invoke()).isEqualTo(LogMessages.WILL_SEND_MESSAGE)
    }

    @Test
    fun `when message with customAttributes is too large`() {
        val expectedErrorCode = ErrorCode.CustomAttributeSizeTooLarge
        val expectedErrorMessage = "Custom Attributes in channel metadata is larger than 2048 bytes"
        subject.connect()

        slot.captured.onMessage(Response.customAttributeSizeTooLarge)

        verifySequence {
            connectSequence()
            mockCustomAttributesStore.onError()
            mockMessageStore.onMessageError(expectedErrorCode, expectedErrorMessage)
            mockAttachmentHandler.onMessageError(expectedErrorCode, expectedErrorMessage)
        }

        verify(exactly = 0) {
            mockCustomAttributesStore.onMessageError()
        }
    }

    @Test
    fun `when autostart request is sent with customAttributes`() {
        every { mockDeploymentConfig.get() } returns createDeploymentConfigForTesting(
            messenger = createMessengerVOForTesting(
                apps = Apps(
                    conversations = createConversationsVOForTesting(
                        autoStart = Conversations.AutoStart(enabled = true)
                    )
                )
            )
        )
        every { mockCustomAttributesStore.getCustomAttributesToSend() } returns mapOf("A" to "B")

        subject.connect()

        verifySequence {
            connectSequence()
            sendingCustomAttributesSequence(Request.autostart())
        }
        assertThat(logSlot[0].invoke()).isEqualTo(LogMessages.CONNECT)
        assertThat(logSlot[1].invoke()).isEqualTo(LogMessages.configureSession(Request.token, false))
        assertThat(logSlot[2].invoke()).isEqualTo(LogMessages.SEND_AUTO_START)
        assertThat(logSlot[3].invoke()).isEqualTo(LogMessages.WILL_SEND_MESSAGE)
    }

    @Test
    fun `when autostart request is sent with customAttributes size that is too large`() {
        val expectedCustomAttributesErrorMessage = "Custom Attributes in channel metadata is larger than 2048 bytes"
        val fakeLargeCustomAttribute = "This Custom Attribute is too large and will be rejected."
        every { mockDeploymentConfig.get() } returns createDeploymentConfigForTesting(
            messenger = createMessengerVOForTesting(
                apps = Apps(
                    conversations = createConversationsVOForTesting(
                        autoStart = Conversations.AutoStart(enabled = true)
                    )
                )
            )
        )
        every { mockCustomAttributesStore.getCustomAttributesToSend() } returns mutableMapOf("A" to fakeLargeCustomAttribute)
        every { mockPlatformSocket.sendMessage(Request.autostart(""""channel":{"metadata":{"customAttributes":{"A":"$fakeLargeCustomAttribute"}}},""")) } answers {
            slot.captured.onMessage(Response.customAttributeSizeTooLarge)
        }

        subject.connect()

        verifySequence {
            connectSequence()
            sendingCustomAttributesSequence(Request.autostart(""""channel":{"metadata":{"customAttributes":{"A":"$fakeLargeCustomAttribute"}}},"""))
            mockCustomAttributesStore.onError()
            mockEventHandler.onEvent(
                Event.Error(
                    ErrorCode.CustomAttributeSizeTooLarge,
                    expectedCustomAttributesErrorMessage,
                    CorrectiveAction.CustomAttributeSizeTooLarge
                )
            )
            mockMessageStore.onMessageError(ErrorCode.CustomAttributeSizeTooLarge, expectedCustomAttributesErrorMessage)
            mockAttachmentHandler.onMessageError(ErrorCode.CustomAttributeSizeTooLarge, expectedCustomAttributesErrorMessage)
        }
        verify(exactly = 0) {
            mockCustomAttributesStore.onMessageError()
        }
        assertThat(logSlot[0].invoke()).isEqualTo(LogMessages.CONNECT)
        assertThat(logSlot[1].invoke()).isEqualTo(LogMessages.configureSession(Request.token, false))
        assertThat(logSlot[2].invoke()).isEqualTo(LogMessages.SEND_AUTO_START)
        assertThat(logSlot[3].invoke()).isEqualTo(LogMessages.WILL_SEND_MESSAGE)
    }

    @Test
    fun `when sendQuickReply() with customAttributes`() {
        val expectedButtonResponse = QuickReplyTestValues.buttonResponse_a
        val expectedCustomAttributes = mapOf("A" to "B")
        val expectedChannel = Channel(Channel.Metadata(expectedCustomAttributes))
        every { mockMessageStore.prepareMessageWith(Request.token, any(), expectedChannel) } returns OnMessageRequest(
            token = Request.token,
            message = TextMessage(
                text = "",
                channel = expectedChannel,
                content = listOf(
                    Message.Content(
                        contentType = Message.Content.Type.ButtonResponse,
                        buttonResponse = QuickReplyTestValues.buttonResponse_a,
                    )
                ),
            ),
        )
        every { mockCustomAttributesStore.getCustomAttributesToSend() } returns mapOf("A" to "B")
        subject.connect()

        subject.sendQuickReply(QuickReplyTestValues.buttonResponse_a)

        verifySequence {
            connectSequence()
            mockLogger.i(capture(logSlot))
            mockCustomAttributesStore.getCustomAttributesToSend()
            mockCustomAttributesStore.onSending()
            mockMessageStore.prepareMessageWith(Request.token, expectedButtonResponse, expectedChannel)
            mockLogger.i(capture(logSlot))
            mockPlatformSocket.sendMessage(Request.quickReplyWith(channel = """"channel":{"metadata":{"customAttributes":{"A":"B"}}},"""))
        }
    }

    private fun MockKVerificationScope.sendingCustomAttributesSequence(message: String) {
        mockCustomAttributesStore.getCustomAttributesToSend()
        mockCustomAttributesStore.onSending()
        mockLogger.i(capture(logSlot))
        mockLogger.i(capture(logSlot))
        mockPlatformSocket.sendMessage(message)
    }
}
