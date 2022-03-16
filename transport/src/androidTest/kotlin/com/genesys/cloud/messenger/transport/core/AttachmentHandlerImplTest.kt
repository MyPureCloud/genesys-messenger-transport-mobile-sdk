package com.genesys.cloud.messenger.transport.core

import assertk.assertThat
import assertk.assertions.containsOnly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNull
import com.genesys.cloud.messenger.transport.core.Attachment.State
import com.genesys.cloud.messenger.transport.network.WebMessagingApi
import com.genesys.cloud.messenger.transport.shyrka.receive.PresignedUrlResponse
import com.genesys.cloud.messenger.transport.shyrka.receive.UploadSuccessEvent
import com.genesys.cloud.messenger.transport.shyrka.send.DeleteAttachmentRequest
import com.genesys.cloud.messenger.transport.shyrka.send.OnAttachmentRequest
import io.ktor.client.features.ClientRequestException
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.request
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.mockk.Called
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
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

internal class AttachmentHandlerImplTest {
    private val mockApi: WebMessagingApi = mockk {
        coEvery { uploadFile(any(), any(), captureLambda()) } coAnswers {
            thirdArg<(Float) -> Unit>().invoke(25f)
        }
    }
    private val attachmentSlot = slot<Attachment>()
    private val mockAttachmentListener: (Attachment) -> Unit = spyk()
    private val processedAttachments = mutableMapOf<String, ProcessedAttachment>()

    private val givenToken = "00000000-0000-0000-0000-000000000000"
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
    fun whenPrepareCalled() {
        val expectedAttachment = Attachment(givenAttachmentId, "image.png", State.Presigning)
        val expectedProcessedAttachment =
            ProcessedAttachment(expectedAttachment, ByteArray(1))
        val expectedOnAttachmentRequest = OnAttachmentRequest(
            token = "00000000-0000-0000-0000-000000000000",
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
    fun whenUploadProcessedAttachment() {
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
    fun whenUploadNotProcessedAttachment() {
        val mockUploadProgress: ((Float) -> Unit) = spyk()
        givenPrepareCalled(uploadProgress = mockUploadProgress)

        subject.upload(presignedUrlResponse(id = "not processed attachment id"))

        verify {
            listOf(mockAttachmentListener, mockApi, mockUploadProgress) wasNot Called
        }
    }

    @Test
    fun whenExceptionThrownDuringUpload() {
        val attachmentSlotList = mutableListOf<Attachment>()
        val mockHttpResponse: HttpResponse = mockk(relaxed = true) {
            every { status } returns HttpStatusCode(404, "page not found")
            every { request } returns mockk(relaxed = true) {
                every { url } returns Url("http://someurl.com")
            }
        }
        coEvery { mockApi.uploadFile(any(), any(), any()) } throws ClientRequestException(
            mockHttpResponse,
            "something went wrong"
        )
        val expectedAttachment = Attachment(
            id = givenAttachmentId,
            state = State.Error(
                ErrorCode.ClientResponseError(404),
                "Client request(http://someurl.com) invalid: 404 page not found. Text: \"something went wrong\""
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
    fun whenUploadSuccessForProcessedAttachment() {
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
    fun whenUploadSuccessForNotProcessedAttachment() {
        givenPrepareCalled()

        subject.onUploadSuccess(uploadSuccessEvent(id = "not processed attachment id"))

        verify {
            listOf(mockAttachmentListener) wasNot Called
        }
    }

    @Test
    fun whenDetachUploaded() {
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
    fun whenDetachNotUploaded() {
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
    fun whenDetachNotProcessedAttachment() {
        val result = subject.detach("not processed attachment id")

        verify {
            listOf(mockAttachmentListener) wasNot Called
        }
        assertThat(result).isNull()
    }

    @Test
    fun whenOnDetached() {
        val expectedAttachment = Attachment(givenAttachmentId, "image.png", State.Detached)
        givenPrepareCalled()
        givenUploadSuccessCalled()

        subject.onDetached(givenAttachmentId)

        verify { mockAttachmentListener.invoke(capture(attachmentSlot)) }
        assertThat(attachmentSlot.captured).isEqualTo(expectedAttachment)
        assertThat(processedAttachments.containsKey(givenAttachmentId)).isFalse()
    }

    @Test
    fun whenOnError() {
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
    fun whenOnMessageErrorWhileHasSendingAttachment() {
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
    fun whenOnMessageErrorHasNoSendingAttachment() {
        subject.onMessageError(ErrorCode.MessageTooLong, "Message too long")

        verify { listOf(mockAttachmentListener) wasNot Called }
    }

    @Test
    fun whenOnSendingHasUploadedAttachment() {
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
    fun whenOnSentProcessedAttachments() {
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
    fun whenOnSentNotProcessedAttachments() {
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
    fun whenClearAll() {
        givenPrepareCalled("attachment id 1")
        givenPrepareCalled("attachment id 2")

        subject.clearAll()

        assertThat(processedAttachments).isEmpty()
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
