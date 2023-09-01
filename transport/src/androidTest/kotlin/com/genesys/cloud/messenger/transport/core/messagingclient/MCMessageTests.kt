package com.genesys.cloud.messenger.transport.core.messagingclient

import com.genesys.cloud.messenger.transport.core.ErrorCode
import com.genesys.cloud.messenger.transport.core.MessageEvent
import com.genesys.cloud.messenger.transport.util.Request
import com.genesys.cloud.messenger.transport.util.Response
import io.mockk.verify
import io.mockk.verifySequence
import org.junit.Test
import kotlin.test.assertFailsWith

class MCMessageTests : BaseMessagingClientTest() {

    @Test
    fun `when messageListener is set`() {
        val givenMessageListener: (MessageEvent) -> Unit = {}

        subject.messageListener = givenMessageListener

        verify {
            mockMessageStore.messageListener = givenMessageListener
        }
    }

    @Test
    fun `when connect and then sendMessage`() {
        val expectedMessage =
            """{"token":"${Request.token}","message":{"text":"Hello world","type":"Text"},"action":"onMessage"}"""
        val expectedText = "Hello world"
        subject.connect()

        subject.sendMessage("Hello world")

        verifySequence {
            connectSequence()
            mockMessageStore.prepareMessage(expectedText)
            mockAttachmentHandler.onSending()
            mockPlatformSocket.sendMessage(expectedMessage)
        }
    }

    @Test
    fun `when invalidateConversationCache`() {
        subject.invalidateConversationCache()

        verify {
            mockMessageStore.invalidateConversationCache()
        }
    }

    @Test
    fun `when sendMessage without connection`() {
        assertFailsWith<IllegalStateException> {
            subject.sendMessage("foo")
        }
    }

    @Test
    fun `when SocketListener invoke onMessage with MessageTooLong error message`() {
        subject.connect()

        slot.captured.onMessage(Response.messageTooLong)

        verifySequence {
            connectSequence()
            mockMessageStore.onMessageError(ErrorCode.MessageTooLong, "message too long")
        }
        verify(exactly = 0) {
            mockMessageStore.clearInitialCustomAttributes()
        }
    }

    @Test
    fun `when SocketListener invoke onMessage with TooManyRequests error message`() {
        subject.connect()

        slot.captured.onMessage(Response.tooManyRequests)

        verifySequence {
            connectSequence()
            mockMessageStore.onMessageError(
                ErrorCode.RequestRateTooHigh,
                "Message rate too high for this session. Retry after 3 seconds."
            )
            mockAttachmentHandler.onMessageError(
                ErrorCode.RequestRateTooHigh,
                "Message rate too high for this session. Retry after 3 seconds."
            )
        }
        verify(exactly = 0) {
            mockMessageStore.clearInitialCustomAttributes()
        }
    }

    @Test
    fun `when SocketListener invoke onMessage with CustomAttributesTooLarge error message`() {
        val expectedErrorMessage = "Custom Attributes in channel metadata is larger than 2048 bytes"
        subject.connect()

        slot.captured.onMessage(Response.customAttributeSizeTooLarge)

        verifySequence {
            connectSequence()
            mockMessageStore.clearInitialCustomAttributes()
            mockMessageStore.onMessageError(
                ErrorCode.CustomAttributeSizeTooLarge,
                expectedErrorMessage
            )
            mockAttachmentHandler.onMessageError(
                ErrorCode.CustomAttributeSizeTooLarge,
                expectedErrorMessage
            )
        }
        verify(exactly = 0) {
            mockEventHandler.onEvent(any())
        }
    }
}
