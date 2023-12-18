package com.genesys.cloud.messenger.transport.core.messagingclient

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
import com.genesys.cloud.messenger.transport.util.Request
import com.genesys.cloud.messenger.transport.util.Response
import com.genesys.cloud.messenger.transport.utility.QuickReplyTestValues
import io.mockk.every
import io.mockk.verify
import io.mockk.verifySequence
import org.junit.Test

class MCCustomAttributesTests : BaseMessagingClientTest() {

    @Test
    fun `when sendMessage with customAttributes`() {
        val expectedMessage =
            """{"token":"${Request.token}","message":{"text":"Hello world","channel":{"metadata":{"customAttributes":{"A":"B"}}},"type":"Text"},"action":"onMessage"}"""
        val expectedText = "Hello world"
        val expectedCustomAttributes = mapOf("A" to "B")
        val expectedChannel = Channel(Channel.Metadata(expectedCustomAttributes))
        every { mockMessageStore.prepareMessage(any(), any()) } returns OnMessageRequest(
            token = Request.token,
            message = TextMessage(
                text = "Hello world",
                channel = expectedChannel,
            ),
        )
        subject.connect()

        subject.sendMessage(text = "Hello world", customAttributes = mapOf("A" to "B"))

        verifySequence {
            connectSequence()
            mockCustomAttributesStore.add(expectedCustomAttributes)
            mockCustomAttributesStore.getCustomAttributesToSend()
            mockCustomAttributesStore.onSending()
            mockMessageStore.prepareMessage(expectedText, expectedChannel)
            mockAttachmentHandler.onSending()
            mockPlatformSocket.sendMessage(expectedMessage)
        }
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

        subject.connect()

        verifySequence {
            connectSequence()
            mockCustomAttributesStore.getCustomAttributesToSend()
            mockCustomAttributesStore.onSending()
            mockPlatformSocket.sendMessage(Request.autostart())
        }
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
            mockCustomAttributesStore.getCustomAttributesToSend()
            mockCustomAttributesStore.onSending()
            mockPlatformSocket.sendMessage(Request.autostart(""""channel":{"metadata":{"customAttributes":{"A":"$fakeLargeCustomAttribute"}}},"""))
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
    }

    @Test
    fun `when sendQuickReply() with customAttributes`() {
        val expectedButtonResponse = QuickReplyTestValues.buttonResponse_a
        val expectedCustomAttributes = mapOf("A" to "B")
        val expectedChannel = Channel(Channel.Metadata(expectedCustomAttributes))
        every { mockMessageStore.prepareMessageWith(any(), expectedChannel) } returns OnMessageRequest(
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
        subject.connect()

        subject.sendQuickReply(QuickReplyTestValues.buttonResponse_a)

        verifySequence {
            connectSequence()
            mockCustomAttributesStore.getCustomAttributesToSend()
            mockCustomAttributesStore.onSending()
            mockMessageStore.prepareMessageWith(expectedButtonResponse, expectedChannel)
            mockPlatformSocket.sendMessage(Request.quickReplyWith(channel = """"channel":{"metadata":{"customAttributes":{"A":"B"}}},"""))
        }
    }
}
