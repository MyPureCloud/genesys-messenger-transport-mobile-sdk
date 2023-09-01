package com.genesys.cloud.messenger.transport.core.messagingclient

import com.genesys.cloud.messenger.transport.core.CorrectiveAction
import com.genesys.cloud.messenger.transport.core.ErrorCode
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
import io.mockk.every
import io.mockk.verifySequence
import org.junit.Test

class MCCustomAttributesTests : BaseMessagingClientTest() {

    @Test
    fun `when sendMessage with customAttributes`() {
        val expectedMessage =
            """{"token":"${Request.token}","message":{"text":"Hello world","channel":{"metadata":{"customAttributes":{"A":"B"}}},"type":"Text"},"action":"onMessage"}"""
        val expectedText = "Hello world"
        val expectedCustomAttributes = mapOf("A" to "B")
        every { mockMessageStore.prepareMessage(any(), any()) } returns OnMessageRequest(
            token = Request.token,
            message = TextMessage(
                text = "Hello world",
                channel = Channel(Channel.Metadata(expectedCustomAttributes)),
            ),
        )
        subject.connect()

        subject.sendMessage(text = "Hello world", customAttributes = mapOf("A" to "B"))

        verifySequence {
            connectSequence()
            mockMessageStore.prepareMessage(expectedText, expectedCustomAttributes)
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
            mockMessageStore.clearInitialCustomAttributes()
            mockMessageStore.onMessageError(expectedErrorCode, expectedErrorMessage)
            mockAttachmentHandler.onMessageError(expectedErrorCode, expectedErrorMessage)
        }
    }

    @Test
    fun `when autostart request is sent with customAttributes`() {
        every { mockMessageStore.initialCustomAttributes } returns mutableMapOf("A" to "B")
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
            mockMessageStore.initialCustomAttributes
            mockPlatformSocket.sendMessage(Request.autostart("""{"customAttributes":{"A":"B"}}"""))
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
        every { mockMessageStore.initialCustomAttributes } returns mutableMapOf("A" to fakeLargeCustomAttribute)
        every { mockPlatformSocket.sendMessage(Request.autostart("""{"customAttributes":{"A":"$fakeLargeCustomAttribute"}}""")) } answers {
            slot.captured.onMessage(Response.customAttributeSizeTooLarge)
        }

        subject.connect()

        verifySequence {
            connectSequence()
            mockMessageStore.initialCustomAttributes
            mockPlatformSocket.sendMessage(Request.autostart("""{"customAttributes":{"A":"$fakeLargeCustomAttribute"}}"""))
            mockMessageStore.clearInitialCustomAttributes()
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
    }
}
