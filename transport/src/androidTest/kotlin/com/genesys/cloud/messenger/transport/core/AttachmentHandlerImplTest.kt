package com.genesys.cloud.messenger.transport.core

import assertk.assertThat
import assertk.assertions.containsOnly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNull
import assertk.assertions.isTrue
import com.genesys.cloud.messenger.transport.core.Attachment.State
import com.genesys.cloud.messenger.transport.network.WebMessagingApi
import com.genesys.cloud.messenger.transport.shyrka.receive.PresignedUrlResponse
import com.genesys.cloud.messenger.transport.shyrka.receive.UploadSuccessEvent
import com.genesys.cloud.messenger.transport.shyrka.send.DeleteAttachmentRequest
import com.genesys.cloud.messenger.transport.shyrka.send.OnAttachmentRequest
import com.genesys.cloud.messenger.transport.util.Request
import io.mockk.Called
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertFailsWith

internal class AttachmentHandlerImplTest {
    private val mockApi: WebMessagingApi = mockk {
        coEvery { uploadFile(any(), any(), captureLambda()) } coAnswers {
            thirdArg<(Float) -> Unit>().invoke(25f)
            Result.Success(Empty())
        }
    }
    private val attachmentSlot = slot<Attachment>()
    private val mockAttachmentListener: (Attachment) -> Unit = spyk()
    private val processedAttachments = mutableMapOf<String, ProcessedAttachment>()

    private val givenToken = Request.token
    private val givenAttachmentId = "99999999-9999-9999-9999-999999999999"
    private val givenUploadSuccessEvent = uploadSuccessEvent()
    private val givenPresignedUrlResponse = presignedUrlResponse()

    @ExperimentalCoroutinesApi
    private val threadSurrogate = newSingleThreadContext("main thread")

    private val subject = AttachmentHandlerImpl(
        mockApi,
        givenToken,
        mockk(relaxed = true),
        mockAttachmentListener,
        processedAttachments,
    )

    @ExperimentalCoroutinesApi
    @Before
    fun setup() {
        Dispatchers.setMain(threadSurrogate)
    }

    @ExperimentalCoroutinesApi
    @After
    fun tearDown() {
        Dispatchers.resetMain()
        threadSurrogate.close()
    }

    @Test
    fun `when prepare()`() {
        val expectedAttachment = Attachment(givenAttachmentId, "image.png", State.Presigning)
        val expectedProcessedAttachment = ProcessedAttachment(expectedAttachment, ByteArray(1))
        val expectedOnAttachmentRequest = OnAttachmentRequest(
            token = givenToken,
            attachmentId = "99999999-9999-9999-9999-999999999999",
            fileName = "image.png",
            fileType = "image/png",
            1,
            null,
            true,
        )

        val onAttachmentRequest = subject.prepare(givenAttachmentId, ByteArray(1), "image.png")

        assertThat(onAttachmentRequest).isEqualTo(expectedOnAttachmentRequest)
        assertThat(processedAttachments).containsOnly(givenAttachmentId to expectedProcessedAttachment)
        verify { mockAttachmentListener.invoke(capture(attachmentSlot)) }
        assertThat(attachmentSlot.captured).isEqualTo(expectedAttachment)
    }

    @Test
    fun `when upload() processed attachment`() {
        val expectedProgress = 25f
        val expectedAttachment = Attachment(givenAttachmentId, "image.png", State.Uploading)
        val mockUploadProgress: ((Float) -> Unit) = spyk()
        val progressSlot = slot<Float>()
        givenPrepareCalled(uploadProgress = mockUploadProgress)

        subject.upload(givenPresignedUrlResponse)

        coVerify {
            mockAttachmentListener.invoke(capture(attachmentSlot))
            mockApi.uploadFile(givenPresignedUrlResponse, ByteArray(1), mockUploadProgress)
            mockUploadProgress.invoke(capture(progressSlot))
        }
        assertThat(attachmentSlot.captured).isEqualTo(expectedAttachment)
        assertThat(progressSlot.captured).isEqualTo(expectedProgress)
    }

    @Test
    fun `when upload() not processed attachment`() {
        val mockUploadProgress: ((Float) -> Unit) = spyk()
        givenPrepareCalled(uploadProgress = mockUploadProgress)

        subject.upload(presignedUrlResponse(id = "not processed attachment id"))

        verify {
            listOf(mockAttachmentListener, mockApi, mockUploadProgress) wasNot Called
        }
    }

    @Test
    fun `when uploadFile() fails with ResponseError`() {
        val attachmentSlotList = mutableListOf<Attachment>()
        coEvery { mockApi.uploadFile(any(), any(), any()) } returns Result.Failure(
            ErrorCode.mapFrom(404),
            "something went wrong"
        )
        val expectedAttachment = Attachment(
            id = givenAttachmentId,
            state = State.Error(
                ErrorCode.ClientResponseError(404),
                "something went wrong"
            )
        )
        givenPrepareCalled()

        subject.upload(givenPresignedUrlResponse)

        coVerify {
            mockAttachmentListener.invoke(capture(attachmentSlotList))
            mockApi.uploadFile(any(), any(), any())
            mockAttachmentListener.invoke(capture(attachmentSlotList))
        }
        assertThat(attachmentSlotList[1]).isEqualTo(expectedAttachment)
        assertThat(processedAttachments.containsKey(givenAttachmentId)).isFalse()
    }

    @Test
    fun `when uploadFile() fails with CancellationError`() {
        val attachmentSlotList = mutableListOf<Attachment>()
        coEvery { mockApi.uploadFile(any(), any(), any()) } returns Result.Failure(
            ErrorCode.CancellationError,
            "upload was cancelled."
        )
        val expectedAttachment = Attachment(
            id = givenAttachmentId,
            fileName = "image.png",
            state = State.Uploading,
        )
        givenPrepareCalled()

        subject.upload(givenPresignedUrlResponse)

        coVerify {
            mockAttachmentListener.invoke(capture(attachmentSlotList))
            mockApi.uploadFile(any(), any(), any())
            mockAttachmentListener.invoke(capture(attachmentSlotList))
        }
        assertThat(attachmentSlotList[1]).isEqualTo(expectedAttachment)
        assertThat(processedAttachments.containsKey(givenAttachmentId)).isTrue()
    }

    @Test
    fun `when onUploadSuccess() on processed attachment`() {
        val expectedDownloadUrl = "http://somedownloadurl.com"
        val expectedAttachment =
            Attachment(givenAttachmentId, "image.png", State.Uploaded(expectedDownloadUrl))
        val expectedProcessedAttachment = ProcessedAttachment(expectedAttachment, ByteArray(1))
        givenPrepareCalled()

        subject.onUploadSuccess(givenUploadSuccessEvent)

        verify { mockAttachmentListener.invoke(capture(attachmentSlot)) }
        assertThat(attachmentSlot.captured).isEqualTo(expectedAttachment)
        assertThat(processedAttachments).containsOnly(givenAttachmentId to expectedProcessedAttachment)
    }

    @Test
    fun `when onUploadSuccess() on not processed attachment`() {
        givenPrepareCalled()

        subject.onUploadSuccess(uploadSuccessEvent(id = "not processed attachment id"))

        verify {
            listOf(mockAttachmentListener) wasNot Called
        }
    }

    @Test
    fun `when detach() on uploaded attachment`() {
        val expectedAttachment = Attachment(givenAttachmentId, "image.png", State.Detaching)
        val expectedProcessedAttachment = ProcessedAttachment(expectedAttachment, ByteArray(1))
        val expectedDeleteAttachmentRequest = DeleteAttachmentRequest(givenToken, givenAttachmentId)
        givenPrepareCalled()
        givenUploadSuccessCalled()

        val result = subject.detach(givenAttachmentId)

        verify {
            mockAttachmentListener.invoke(capture(attachmentSlot))
        }
        assertThat(result).isEqualTo(expectedDeleteAttachmentRequest)
        assertThat(attachmentSlot.captured).isEqualTo(expectedAttachment)
        assertThat(processedAttachments).containsOnly(givenAttachmentId to expectedProcessedAttachment)
    }

    @Test
    fun `when detach() on not uploaded attachment`() {
        val expectedAttachment = Attachment(givenAttachmentId, "image.png", State.Detached)
        givenPrepareCalled()

        val result = subject.detach(givenAttachmentId)

        verify {
            mockAttachmentListener.invoke(capture(attachmentSlot))
        }
        assertThat(result).isNull()
        assertThat(attachmentSlot.captured).isEqualTo(expectedAttachment)
        assertThat(processedAttachments.containsKey(givenAttachmentId)).isFalse()
    }

    @Test
    fun `when detach() on not processed attachment`() {
        val result = subject.detach("not processed attachment id")

        verify {
            listOf(mockAttachmentListener) wasNot Called
        }
        assertThat(result).isNull()
    }

    @Test
    fun `when OnDetached()`() {
        val expectedAttachment = Attachment(givenAttachmentId, "image.png", State.Detached)
        givenPrepareCalled()
        givenUploadSuccessCalled()

        subject.onDetached(givenAttachmentId)

        verify { mockAttachmentListener.invoke(capture(attachmentSlot)) }
        assertThat(attachmentSlot.captured).isEqualTo(expectedAttachment)
        assertThat(processedAttachments.containsKey(givenAttachmentId)).isFalse()
    }

    @Test
    fun `when OnError()`() {
        val expectedAttachment = Attachment(
            id = givenAttachmentId,
            state = State.Error(ErrorCode.FileTypeInvalid, "something went wrong")
        )
        givenPrepareCalled()

        subject.onError(givenAttachmentId, ErrorCode.mapFrom(4001), "something went wrong")

        verify { mockAttachmentListener.invoke(capture(attachmentSlot)) }
        assertThat(attachmentSlot.captured).isEqualTo(expectedAttachment)
        assertThat(processedAttachments.containsKey(givenAttachmentId)).isFalse()
    }

    @Test
    fun `when OnMessageError() while has sending attachment`() {
        val expectedAttachment = Attachment(
            id = givenAttachmentId,
            state = State.Error(ErrorCode.MessageTooLong, "Message too long")
        )
        givenPrepareCalled()
        givenUploadSuccessCalled()
        givenOnSendingCalled()

        subject.onMessageError(ErrorCode.MessageTooLong, "Message too long")

        verify { mockAttachmentListener.invoke(capture(attachmentSlot)) }
        assertThat(attachmentSlot.captured).isEqualTo(expectedAttachment)
        assertThat(processedAttachments.containsKey(givenAttachmentId)).isFalse()
    }

    @Test
    fun `when onMessageError() has no sending attachment`() {
        subject.onMessageError(ErrorCode.MessageTooLong, "Message too long")

        verify { listOf(mockAttachmentListener) wasNot Called }
    }

    @Test
    fun `when onSending() has uploaded attachment`() {
        val expectedAttachment = Attachment(
            id = givenAttachmentId,
            fileName = "image.png",
            state = State.Sending
        )
        val expectedProcessedAttachment =
            ProcessedAttachment(expectedAttachment, ByteArray(1))
        givenPrepareCalled()
        givenUploadSuccessCalled()

        subject.onSending()

        verify { mockAttachmentListener.invoke(capture(attachmentSlot)) }
        assertThat(attachmentSlot.captured).isEqualTo(expectedAttachment)
        assertThat(processedAttachments).containsOnly(givenAttachmentId to expectedProcessedAttachment)
    }

    @Test
    fun whenOnSendingHasNoUploadedAttachment() {
        givenPrepareCalled()

        subject.onSending()

        verify { listOf(mockAttachmentListener) wasNot Called }
    }

    @Test
    fun `when onSent() on processed attachment`() {
        val expectedAttachment = Attachment(
            id = givenAttachmentId,
            fileName = "image.png",
            state = State.Sent("http://somedownloadurl.com")
        )

        givenPrepareCalled()
        givenUploadSuccessCalled()
        givenOnSendingCalled()

        subject.onSent(
            mapOf(
                givenAttachmentId to Attachment(
                    givenAttachmentId,
                    "image.png",
                    State.Sent("http://somedownloadurl.com")
                )
            )
        )

        verify { mockAttachmentListener.invoke(capture(attachmentSlot)) }
        assertThat(attachmentSlot.captured).isEqualTo(expectedAttachment)
        assertThat(processedAttachments.containsKey(givenAttachmentId)).isFalse()
    }

    @Test
    fun `when onSent() on not processed Attachment`() {
        subject.onSent(
            mapOf(
                givenAttachmentId to Attachment(
                    givenAttachmentId,
                    "image.png",
                    State.Sent("http://somedownloadurl.com")
                )
            )
        )

        verify { listOf(mockAttachmentListener) wasNot Called }
    }

    @Test
    fun `when clearAll()`() {
        givenPrepareCalled("attachment id 1")
        givenPrepareCalled("attachment id 2")

        subject.clearAll()

        assertThat(processedAttachments).isEmpty()
    }

    @Test
    fun `when prepare() but fileAttachmentProfile is not set`() {
        val expectedOnAttachmentRequest = OnAttachmentRequest(
            token = givenToken,
            attachmentId = "99999999-9999-9999-9999-999999999999",
            fileName = "image.png",
            fileType = "image/png",
            2000,
            null,
            true,
        )

        val givenByteArray = ByteArray(2000)

        val onAttachmentRequest = subject.prepare(givenAttachmentId, givenByteArray, "image.png")

        assertThat(subject.fileAttachmentProfile).isNull()
        assertThat(onAttachmentRequest).isEqualTo(expectedOnAttachmentRequest)
    }

    @Test
    fun `when prepare() but maxFileSizeKB in fileAttachmentProfile is less then provided ByteArray size`() {
        val givenByteArray = ByteArray(2000)
        val expectedExceptionMessage = ErrorMessage.fileSizeIsTooBig(maxFileSize = 1)
        subject.fileAttachmentProfile = FileAttachmentProfile(enabled = true, maxFileSizeKB = 1)

        assertFailsWith<IllegalArgumentException>(expectedExceptionMessage) {
            subject.prepare(givenAttachmentId, givenByteArray, "image.png")
        }
    }

    @Test
    fun `when prepare() and maxFileSizeKB in fileAttachmentProfile is equal to then provided ByteArray size`() {
        val givenByteArray = ByteArray(2000,)
        subject.fileAttachmentProfile = FileAttachmentProfile(enabled = true, maxFileSizeKB = 2)
        val expectedFileSizeInKB = 2L
        val expectedOnAttachmentRequest = OnAttachmentRequest(
            token = givenToken,
            attachmentId = "99999999-9999-9999-9999-999999999999",
            fileName = "image.png",
            fileType = "image/png",
            2000,
            null,
            true,
        )

        val onAttachmentRequest = subject.prepare(givenAttachmentId, givenByteArray, "image.png")

        assertThat(subject.fileAttachmentProfile?.maxFileSizeKB).isEqualTo(expectedFileSizeInKB)
        assertThat(onAttachmentRequest).isEqualTo(expectedOnAttachmentRequest)
    }

    @Test
    fun `when prepare() and ByteArray size is 0`() {
        val givenByteArray = ByteArray(0)
        val expectedExceptionMessage = ErrorMessage.FileSizeIsToSmall
        subject.fileAttachmentProfile = FileAttachmentProfile(enabled = true, maxFileSizeKB = 2)

        assertFailsWith<IllegalArgumentException>(expectedExceptionMessage) {
            subject.prepare(givenAttachmentId, givenByteArray, "image.png")
        }
    }

    @Test
    fun `when prepare() and maxFileSizeKB in fileAttachmentProfile is greater then provided ByteArray size`() {
        val givenByteArray = ByteArray(2000)
        subject.fileAttachmentProfile = FileAttachmentProfile(enabled = true, maxFileSizeKB = 100)
        val expectedFileSizeInKB = 100L
        val expectedOnAttachmentRequest = OnAttachmentRequest(
            token = givenToken,
            attachmentId = "99999999-9999-9999-9999-999999999999",
            fileName = "image.png",
            fileType = "image/png",
            2000,
            null,
            true,
        )

        val onAttachmentRequest = subject.prepare(givenAttachmentId, givenByteArray, "image.png")

        assertThat(subject.fileAttachmentProfile?.maxFileSizeKB).isEqualTo(expectedFileSizeInKB)
        assertThat(onAttachmentRequest).isEqualTo(expectedOnAttachmentRequest)
    }

    @Test
    fun `when prepare() but file extension is included in blockedFileTypes list`() {
        val givenByteArray = ByteArray(1)
        val expectedExceptionMessage = ErrorMessage.fileTypeIsProhibited(fileName = "foo.exe")
        subject.fileAttachmentProfile =
            FileAttachmentProfile(enabled = true, maxFileSizeKB = 100, blockedFileTypes = listOf(".exe"))

        assertFailsWith<IllegalArgumentException>(expectedExceptionMessage) {
            subject.prepare(givenAttachmentId, givenByteArray, "foo.exe")
        }
    }

    @Test
    fun `when prepare() and file extension is NOT included in blockedFileTypes list`() {
        val givenByteArray = ByteArray(1)
        subject.fileAttachmentProfile =
            FileAttachmentProfile(enabled = true, maxFileSizeKB = 100, blockedFileTypes = listOf(".exe"))
        val expectedOnAttachmentRequest = OnAttachmentRequest(
            token = givenToken,
            attachmentId = "99999999-9999-9999-9999-999999999999",
            fileName = "image.png",
            fileType = "image/png",
            1,
            null,
            true,
        )

        val onAttachmentRequest = subject.prepare(givenAttachmentId, givenByteArray, "image.png")

        assertThat(onAttachmentRequest).isEqualTo(expectedOnAttachmentRequest)
    }

    @Test
    fun `when prepare() and file attachment is disabled in deployment config`() {
        val givenByteArray = ByteArray(1)
        subject.fileAttachmentProfile = FileAttachmentProfile()
        val expectedExceptionMessage = ErrorMessage.FileAttachmentIsDisabled

        assertFailsWith<IllegalArgumentException>(expectedExceptionMessage) {
            subject.prepare(givenAttachmentId, givenByteArray, "image.png")
        }
    }

    @Test
    fun `when onAttachmentRefreshed()`() {
        val givenPresignedUrlResponse = PresignedUrlResponse(
            attachmentId = givenAttachmentId,
            fileName = "image.png",
            url = "https://refreshedUrl.com",
            headers = emptyMap(),
        )
        val expectedAttachment = Attachment(
            id = givenAttachmentId,
            fileName = "image.png",
            state = State.Refreshed("https://refreshedUrl.com")
        )

        subject.onAttachmentRefreshed(givenPresignedUrlResponse)

        verify { mockAttachmentListener.invoke(capture(attachmentSlot)) }
        assertThat(attachmentSlot.captured).isEqualTo(expectedAttachment)
    }

    private fun presignedUrlResponse(id: String = givenAttachmentId): PresignedUrlResponse =
        PresignedUrlResponse(
            id, mapOf("header" to "given header"), "http://someuploadurl.com",
        )

    private fun uploadSuccessEvent(id: String = givenAttachmentId): UploadSuccessEvent =
        UploadSuccessEvent(
            id, "http://somedownloadurl.com", "2021-08-17T17:00:08.746Z",
        )

    /**
     * Helper function.
     * Executes the subject.prepare() call with given values and clearMocks related to this call.
     */
    private fun givenPrepareCalled(
        attachmentId: String = givenAttachmentId,
        byteArray: ByteArray = ByteArray(1),
        fileName: String = "image.png",
        uploadProgress: ((Float) -> Unit)? = null,
    ) {
        subject.prepare(
            attachmentId = attachmentId,
            byteArray = byteArray,
            fileName = fileName,
            uploadProgress = uploadProgress
        )
        clearMocks(mockAttachmentListener)
    }

    /**
     * Helper function.
     * Executes the subject.onUploadSuccess() call with given values and clearMocks related to this call.
     */
    private fun givenUploadSuccessCalled(uploadSuccessEvent: UploadSuccessEvent = givenUploadSuccessEvent) {
        subject.onUploadSuccess(uploadSuccessEvent)
        clearMocks(mockAttachmentListener)
    }

    /**
     * Helper function.
     * Executes the subject.onSending() call and clearMocks related to this call.
     */
    private fun givenOnSendingCalled() {
        subject.onSending()
        clearMocks(mockAttachmentListener)
    }
}
