package com.genesys.cloud.messenger.transport.core.messagingclient

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.genesys.cloud.messenger.transport.core.Attachment
import com.genesys.cloud.messenger.transport.core.ErrorMessage
import com.genesys.cloud.messenger.transport.core.FileAttachmentProfile
import com.genesys.cloud.messenger.transport.core.ErrorCode
import com.genesys.cloud.messenger.transport.core.Message
import com.genesys.cloud.messenger.transport.shyrka.receive.PresignedUrlResponse
import com.genesys.cloud.messenger.transport.shyrka.receive.createDeploymentConfigForTesting
import com.genesys.cloud.messenger.transport.shyrka.receive.createFileUploadVOForTesting
import com.genesys.cloud.messenger.transport.shyrka.receive.createMessengerVOForTesting
import com.genesys.cloud.messenger.transport.core.Message.Direction
import com.genesys.cloud.messenger.transport.core.Message.State
import com.genesys.cloud.messenger.transport.core.Message.Type
import com.genesys.cloud.messenger.transport.shyrka.receive.PresignedUrlResponse
import com.genesys.cloud.messenger.transport.shyrka.receive.UploadSuccessEvent
import com.genesys.cloud.messenger.transport.util.Request
import com.genesys.cloud.messenger.transport.util.Response
import com.genesys.cloud.messenger.transport.util.logs.LogMessages
import com.genesys.cloud.messenger.transport.utility.AttachmentValues
import com.genesys.cloud.messenger.transport.utility.ErrorTest
import com.genesys.cloud.messenger.transport.utility.TestValues
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
            mockLogger.i(capture(logSlot))
            mockAttachmentHandler.prepare(any(), any(), any())
            mockLogger.i(capture(logSlot))
            mockPlatformSocket.sendMessage(expectedMessage)
        }
        assertThat(logSlot[0].invoke()).isEqualTo(LogMessages.CONNECT)
        assertThat(logSlot[1].invoke()).isEqualTo(LogMessages.configureSession(Request.token))
        assertThat(logSlot[2].invoke()).isEqualTo(LogMessages.attach("test.png"))
        assertThat(logSlot[3].invoke()).isEqualTo(LogMessages.WILL_SEND_MESSAGE)
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
            mockLogger.i(capture(logSlot))
            mockAttachmentHandler.detach(capture(attachmentIdSlot))
            mockPlatformSocket.sendMessage(expectedMessage)
        }
        assertThat(attachmentIdSlot.captured).isEqualTo(expectedAttachmentId)
        assertThat(logSlot[0].invoke()).isEqualTo(LogMessages.CONNECT)
        assertThat(logSlot[1].invoke()).isEqualTo(LogMessages.configureSession(Request.token))
        assertThat(logSlot[2].invoke()).isEqualTo(LogMessages.detach(expectedAttachmentId))
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
    fun `when SocketListener invoke onMessage with Inbound StructuredMessage that contains attachment`() {
        val expectedAttachment = Attachment(
            "attachment_id",
            "image.png",
            Attachment.State.Sent("https://downloadurl.com")
        )
        val expectedMessage = Message(
            "msg_id",
            Direction.Inbound,
            State.Sent,
            Type.Text,
            "Text",
            "Hi",
            null,
            mapOf("attachment_id" to expectedAttachment)
        )

        subject.connect()

        slot.captured.onMessage(Response.onMessageWithAttachment(Direction.Inbound))

        verifySequence {
            connectSequence()
            mockMessageStore.update(expectedMessage)
            mockCustomAttributesStore.onSent()
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
            enabled = true,
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
            enabled = true,
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
    fun `when enableAttachments is null and FileUpload Mode is empty`() {
        every { mockDeploymentConfig.get() } returns createDeploymentConfigForTesting(
            createMessengerVOForTesting(
                fileUpload = createFileUploadVOForTesting(
                    enableAttachments = null,
                    modes = emptyList(),
                )
            )
        )
        val fileAttachmentProfileSlot = createFileAttachmentProfileSlot()
        every { mockPlatformSocket.sendMessage(Request.configureRequest()) } answers {
            slot.captured.onMessage(
                Response.configureSuccess(
                    allowedMedia = Response.AllowedMedia.listOfFileTypesWithWildcardAndMaxSize,
                    blockedExtensions = Response.AllowedMedia.listOfBlockedExtensions,
                )
            )
        }
        val expectedFileAttachmentProfile = FileAttachmentProfile()
        subject.connect()
        every { mockAttachmentHandler.fileAttachmentProfile } returns fileAttachmentProfileSlot.captured

        slot.captured.onMessage(Response.onMessageWithAttachment)

        assertThat(subject.fileAttachmentProfile).isEqualTo(expectedFileAttachmentProfile)
    }

    @Test
    fun `when enableAttachments is null and FileUpload Mode has entries`() {
        every { mockDeploymentConfig.get() } returns createDeploymentConfigForTesting(
            createMessengerVOForTesting(
                fileUpload = createFileUploadVOForTesting(
                    enableAttachments = null,
                )
            )
        )
        val fileAttachmentProfileSlot = createFileAttachmentProfileSlot()
        val expectedFileAttachmentProfile = FileAttachmentProfile(
            enabled = true,
            allowedFileTypes = listOf("png"),
            maxFileSizeKB = 100
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
    fun `when attach() but attachmentHandler_prepare() throws exception`() {
        val givenByteArray = ByteArray(1)
        every {
            mockAttachmentHandler.prepare(
                any(),
                any(),
                any()
            )
        } throws IllegalArgumentException(ErrorMessage.fileSizeIsTooBig(null))
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

    @Test
    fun `when SocketListener invoke onMessage with Outbound StructuredMessage that contains attachment`() {
        val expectedAttachment = Attachment(
            "attachment_id",
            "image.png",
            Attachment.State.Sent("https://downloadurl.com")
        )
        val expectedMessage = Message(
            "msg_id",
            Direction.Outbound,
            State.Sent,
            Type.Text,
            "Text",
            "Hi",
            null,
            mapOf("attachment_id" to expectedAttachment)
        )

        subject.connect()

        slot.captured.onMessage(Response.onMessageWithAttachment(Direction.Outbound))

        verifySequence {
            connectSequence()
            mockMessageStore.update(expectedMessage)
        }

        verify(exactly = 0) {
            mockCustomAttributesStore.onSent()
            mockAttachmentHandler.onSent(any())
        }
    }

    @Test
    fun `when SocketListener invoke OnMessage with UploadSuccessEvent response`() {
        val expectedEvent = UploadSuccessEvent(
            attachmentId = AttachmentValues.Id,
            downloadUrl = AttachmentValues.DownloadUrl,
            timestamp = TestValues.Timestamp,
        )

        subject.connect()

        slot.captured.onMessage(Response.uploadSuccessEvent)

        verifySequence {
            connectSequence()
            mockAttachmentHandler.onUploadSuccess(expectedEvent)
        }
    }

    @Test
    fun `when SocketListener invoke OnMessage with PresignedUrlResponse response`() {
        val expectedEvent = PresignedUrlResponse(
            attachmentId = AttachmentValues.Id,
            headers = mapOf(AttachmentValues.PresignedHeaderKey to AttachmentValues.PresignedHeaderValue),
            url = AttachmentValues.DownloadUrl,
        )

        subject.connect()

        slot.captured.onMessage(Response.presignedUrlResponse)

        verifySequence {
            connectSequence()
            mockAttachmentHandler.upload(expectedEvent)
        }
    }

    @Test
    fun `when SocketListener invoke OnMessage with GenerateUrlError response`() {
        subject.connect()

        slot.captured.onMessage(Response.generateUrlError)

        verifySequence {
            connectSequence()
            mockAttachmentHandler.onError(AttachmentValues.Id, ErrorCode.FileTypeInvalid, ErrorTest.Message)
        }
    }

    @Test
    fun `when SocketListener invoke OnMessage with UploadFailureEvent response`() {
        subject.connect()

        slot.captured.onMessage(Response.uploadFailureEvent)

        verifySequence {
            connectSequence()
            mockAttachmentHandler.onError(AttachmentValues.Id, ErrorCode.FileTypeInvalid, ErrorTest.Message)
        }
    }
}
