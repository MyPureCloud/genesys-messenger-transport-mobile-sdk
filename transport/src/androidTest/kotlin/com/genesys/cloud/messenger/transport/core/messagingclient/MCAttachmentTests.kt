package com.genesys.cloud.messenger.transport.core.messagingclient

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.genesys.cloud.messenger.transport.core.Attachment
import com.genesys.cloud.messenger.transport.core.ErrorMessage
import com.genesys.cloud.messenger.transport.core.FileAttachmentProfile
import com.genesys.cloud.messenger.transport.core.Message
import com.genesys.cloud.messenger.transport.shyrka.receive.PresignedUrlResponse
import com.genesys.cloud.messenger.transport.util.Request
import com.genesys.cloud.messenger.transport.util.Response
import io.mockk.Called
import io.mockk.CapturingSlot
import io.mockk.Runs
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.just
import io.mockk.slot
import io.mockk.verify
import io.mockk.verifySequence
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class MCAttachmentTests : BaseMessagingClientTest() {

    @Test
    fun `when attach()`() {
        val expectedAttachmentId = "88888888-8888-8888-8888-888888888888"
        val expectedMessage =
            """{"token":"${Request.token}","attachmentId":"88888888-8888-8888-8888-888888888888","fileName":"test_attachment.png","fileType":"image/png","errorsAsJson":true,"action":"onAttachment"}"""
        subject.connect()

        val result = subject.attach(ByteArray(1), "test.png")

        assertEquals(expectedAttachmentId, result)
        verifySequence {
            connectSequence()
            mockAttachmentHandler.validate(any())
            mockAttachmentHandler.prepare(any(), any(), any())
            mockPlatformSocket.sendMessage(expectedMessage)
        }
    }

    @Test
    fun `when detach()`() {
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
    fun `when detach() non existing attachmentId`() {
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
    fun `when attach() without connection`() {
        assertFailsWith<IllegalStateException> {
            subject.attach(ByteArray(1), "file.png")
        }
    }

    @Test
    fun `when detach() attachment without connection`() {
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
            mockAttachmentHandler.onSent(mapOf("attachment_id" to expectedAttachment))
        }
    }

    @Test
    fun `when SessionResponse has no AllowedMedia and blockedExtensions entries`() {
        val fileAttachmentProfileSlot = createFileAttachmentProfileSlot()
        every { mockPlatformSocket.sendMessage(Request.configureRequest()) } answers {
            slot.captured.onMessage(Response.configureSuccess())
        }
        val expectedFileAttachmentProfile = FileAttachmentProfile()
        subject.connect()
        // FileAttachmentProfileSlot captured value has to be returned after "connect" sequence, as it is initialized from SessionResponse.
        every { mockAttachmentHandler.fileAttachmentProfile } returns fileAttachmentProfileSlot.captured

        slot.captured.onMessage(Response.onMessageWithAttachment)

        assertThat(subject.fileAttachmentProfile).isEqualTo(expectedFileAttachmentProfile)
    }

    @Test
    fun `when AllowedMedia in SessionResponse has no inbound and blockedExtensions entries`() {
        val fileAttachmentProfileSlot = createFileAttachmentProfileSlot()
        every { mockPlatformSocket.sendMessage(Request.configureRequest()) } answers {
            slot.captured.onMessage(Response.configureSuccess(allowedMedia = Response.AllowedMedia.noInbound))
        }
        val expectedFileAttachmentProfile = FileAttachmentProfile()
        subject.connect()
        every { mockAttachmentHandler.fileAttachmentProfile } returns fileAttachmentProfileSlot.captured

        slot.captured.onMessage(Response.onMessageWithAttachment)

        assertThat(subject.fileAttachmentProfile).isEqualTo(expectedFileAttachmentProfile)
    }

    @Test
    fun `when AllowedMedia in SessionResponse has no filetypes,maxFileSizeKB and blockedExtensions entries`() {
        val fileAttachmentProfileSlot = createFileAttachmentProfileSlot()
        every { mockPlatformSocket.sendMessage(Request.configureRequest()) } answers {
            slot.captured.onMessage(Response.configureSuccess(allowedMedia = Response.AllowedMedia.emptyInbound))
        }
        val expectedFileAttachmentProfile = FileAttachmentProfile()
        subject.connect()
        every { mockAttachmentHandler.fileAttachmentProfile } returns fileAttachmentProfileSlot.captured

        slot.captured.onMessage(Response.onMessageWithAttachment)

        assertThat(subject.fileAttachmentProfile).isEqualTo(expectedFileAttachmentProfile)
    }

    @Test
    fun `when AllowedMedia in SessionResponse has filetypes without wildcard but with maxFileSizeKB and blockedExtensions entries`() {
        val fileAttachmentProfileSlot = createFileAttachmentProfileSlot()
        every { mockPlatformSocket.sendMessage(Request.configureRequest()) } answers {
            slot.captured.onMessage(
                Response.configureSuccess(
                    allowedMedia = Response.AllowedMedia.listOfFileTypesWithMaxSize,
                    blockedExtensions = Response.AllowedMedia.listOfBlockedExtensions,
                )
            )
        }
        val expectedFileAttachmentProfile = FileAttachmentProfile(
            allowedFileTypes = listOf("video/mpg", "video/3gpp"),
            blockedFileTypes = listOf(".ade", ".adp"),
            maxFileSizeKB = 10240,
            hasWildCard = false,
        )
        subject.connect()
        every { mockAttachmentHandler.fileAttachmentProfile } returns fileAttachmentProfileSlot.captured

        slot.captured.onMessage(Response.onMessageWithAttachment)

        assertThat(subject.fileAttachmentProfile).isEqualTo(expectedFileAttachmentProfile)
    }

    @Test
    fun `when AllowedMedia in SessionResponse has filetypes with wildcard,maxFileSizeKB and blockedExtensions entries`() {
        val fileAttachmentProfileSlot = createFileAttachmentProfileSlot()
        every { mockPlatformSocket.sendMessage(Request.configureRequest()) } answers {
            slot.captured.onMessage(
                Response.configureSuccess(
                    allowedMedia = Response.AllowedMedia.listOfFileTypesWithWildcardAndMaxSize,
                    blockedExtensions = Response.AllowedMedia.listOfBlockedExtensions,
                )
            )
        }
        val expectedFileAttachmentProfile = FileAttachmentProfile(
            allowedFileTypes = listOf("video/3gpp"),
            blockedFileTypes = listOf(".ade", ".adp"),
            maxFileSizeKB = 10240,
            hasWildCard = true,
        )
        subject.connect()
        every { mockAttachmentHandler.fileAttachmentProfile } returns fileAttachmentProfileSlot.captured

        slot.captured.onMessage(Response.onMessageWithAttachment)

        assertThat(subject.fileAttachmentProfile).isEqualTo(expectedFileAttachmentProfile)
    }

    @Test
    fun `when fileAttachmentProfile is called before session configure`() {
        val result = subject.fileAttachmentProfile

        assertNull(result)
        verify {
            mockAttachmentHandler.fileAttachmentProfile
        }
    }

    @Test
    fun `when attach() but validate attachment returns false`() {
        val givenByteArray = ByteArray(1)
        every { mockAttachmentHandler.validate(givenByteArray) } returns false
        subject.connect()

        assertFailsWith<IllegalArgumentException>(ErrorMessage.fileSizeIsTooBig(null)) {
            subject.attach(givenByteArray, "test.png")
        }
    }

    @Test
    fun `when refreshAttachmentUrl()`() {
        every { mockPlatformSocket.sendMessage(Request.refreshAttachmentUrl) } answers {
            slot.captured.onMessage(Response.presignedUrlResponse(headers = "", fileSize = 1))
        }
        val expectedPresignedUrlResponse = PresignedUrlResponse(
            attachmentId = "88888888-8888-8888-8888-888888888888",
            headers = emptyMap(),
            url = "https://downloadUrl.com",
            fileSize = 1,
            fileName = "test_asset.png",
            fileType = "image/jpeg"
        )
        subject.connect()

        subject.refreshAttachmentUrl("88888888-8888-8888-8888-888888888888")

        verify {
            connectSequence()
            mockPlatformSocket.sendMessage(Request.refreshAttachmentUrl)
            mockAttachmentHandler.onAttachmentRefreshed(expectedPresignedUrlResponse)
        }
        verify(exactly = 0) {
            mockAttachmentHandler.upload(any())
        }
    }

    @Test
    fun `when refreshAttachmentUrl() attachment without connection`() {
        assertFailsWith<IllegalStateException> {
            subject.refreshAttachmentUrl("attachmentId")
        }
    }

    private fun createFileAttachmentProfileSlot(): CapturingSlot<FileAttachmentProfile> {
        val fileAttachmentProfileSlot = slot<FileAttachmentProfile>()
        every {
            mockAttachmentHandler.fileAttachmentProfile = capture(fileAttachmentProfileSlot)
        } just Runs
        return fileAttachmentProfileSlot
    }
}
