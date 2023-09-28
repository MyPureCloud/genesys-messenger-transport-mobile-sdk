package com.genesys.cloud.messenger.transport.core.messagingclient

import com.genesys.cloud.messenger.transport.core.ErrorCode
import com.genesys.cloud.messenger.transport.shyrka.send.Channel
import com.genesys.cloud.messenger.transport.shyrka.send.OnMessageRequest
import com.genesys.cloud.messenger.transport.shyrka.send.TextMessage
import com.genesys.cloud.messenger.transport.util.Request
import com.genesys.cloud.messenger.transport.util.Response
import io.mockk.every
import io.mockk.verifySequence
import org.junit.Test

class MCustomAttributesTests : BaseMessagingClientTest() {

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
            mockMessageStore.onMessageError(expectedErrorCode, expectedErrorMessage)
            mockAttachmentHandler.onMessageError(expectedErrorCode, expectedErrorMessage)
        }
    }
}
