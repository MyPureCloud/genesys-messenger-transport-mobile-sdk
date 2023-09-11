package com.genesys.cloud.messenger.transport.core.messagingclient

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.genesys.cloud.messenger.transport.core.Attachment
import com.genesys.cloud.messenger.transport.core.Message
import com.genesys.cloud.messenger.transport.util.Request
import com.genesys.cloud.messenger.transport.util.Response
import io.mockk.Called
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.slot
import io.mockk.verify
import io.mockk.verifySequence
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class MCAttachmentTests : BaseMessagingClientTest() {

    @Test
    fun `when attach`() {
        val expectedAttachmentId = "88888888-8888-8888-8888-888888888888"
        val expectedMessage =
            """{"token":"${Request.token}","attachmentId":"88888888-8888-8888-8888-888888888888","fileName":"test_attachment.png","fileType":"image/png","errorsAsJson":true,"action":"onAttachment"}"""
        subject.connect()

        val result = subject.attach(ByteArray(1), "test.png")

        assertEquals(expectedAttachmentId, result)
        verifySequence {
            connectSequence()
            mockAttachmentHandler.prepare(any(), any(), any())
            mockPlatformSocket.sendMessage(expectedMessage)
        }
    }

    @Test
    fun `when detach`() {
        val expectedAttachmentId = "88888888-8888-8888-8888-888888888888"
        val expectedMessage =
            """{"token":"${Request.token}","attachmentId":"88888888-8888-8888-8888-888888888888","action":"deleteAttachment"}"""
        val attachmentIdSlot = slot<String>()
        subject.connect()

        subject.detach("88888888-8888-8888-8888-888888888888")

        verify {
            mockAttachmentHandler.detach(capture(attachmentIdSlot))
            mockPlatformSocket.sendMessage(expectedMessage)
        }
        assertThat(attachmentIdSlot.captured).isEqualTo(expectedAttachmentId)
    }

    @Test
    fun `when detach non existing attachmentId`() {
        subject.connect()
        clearMocks(mockPlatformSocket)
        every { mockAttachmentHandler.detach(any()) } returns null

        subject.detach("88888888-8888-8888-8888-888888888888")

        verify {
            mockAttachmentHandler.detach("88888888-8888-8888-8888-888888888888")
            mockPlatformSocket wasNot Called
        }
    }

    @Test
    fun `when SocketListener invoke OnMessage with AttachmentDeleted response`() {
        val expectedAttachmentId = "attachment_id"

        subject.connect()

        slot.captured.onMessage(Response.attachmentDeleted)

        verifySequence {
            connectSequence()
            mockAttachmentHandler.onDetached(expectedAttachmentId)
        }
    }

    @Test
    fun `when attach without connection`() {
        assertFailsWith<IllegalStateException> {
            subject.attach(ByteArray(1), "file.png")
        }
    }

    @Test
    fun `when detach attachment without connection`() {
        assertFailsWith<IllegalStateException> {
            subject.detach("attachmentId")
        }
    }

    @Test
    fun `when SocketListener invoke onMessage with StructuredMessage that contains attachment`() {
        val expectedAttachment = Attachment(
            "attachment_id",
            "image.png",
            Attachment.State.Sent("https://downloadurl.com")
        )
        val expectedMessage = Message(
            "msg_id",
            Message.Direction.Outbound,
            Message.State.Sent,
            "Text",
            "Hi",
            null,
            mapOf("attachment_id" to expectedAttachment)
        )

        subject.connect()

        slot.captured.onMessage(Response.onMessageWithAttachment)

        verifySequence {
            connectSequence()
            mockMessageStore.update(expectedMessage)
            mockCustomAttributesStore.onSent()
            mockAttachmentHandler.onSent(mapOf("attachment_id" to expectedAttachment))
        }
    }
}
