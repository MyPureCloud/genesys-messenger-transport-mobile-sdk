package com.genesys.cloud.messenger.transport.core

import com.genesys.cloud.messenger.transport.core.Attachment.State
import com.genesys.cloud.messenger.transport.network.WebMessagingApi
import com.genesys.cloud.messenger.transport.shyrka.receive.PresignedUrlResponse
import com.genesys.cloud.messenger.transport.shyrka.receive.UploadSuccessEvent
import io.ktor.client.features.ClientRequestException
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.request
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.spyk
import io.mockk.verify
import io.mockk.verifyOrder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

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
    fun whenPrepareAttachmentCalled() {
        val expectedFileName = "image.png"
        val expectedFileType = "image/png"
        val expectedByteArray = ByteArray(10)
        val expectedAttachment = Attachment(givenAttachmentId, "image.png", State.Presigning)

        val result = subject.prepare(givenAttachmentId, expectedByteArray, "image.png")

        assertEquals(expectedFileName, result.fileName)
        assertEquals(expectedFileType, result.fileType)
        assertEquals(expectedByteArray.size, result.fileSize)
        assertTrue(result.errorsAsJson)

        verify { mockAttachmentListener.invoke(capture(attachmentSlot)) }
        assertEquals(expectedAttachment, attachmentSlot.captured)
    }

    @Test
    fun whenUpload() {
        val expectedProgress = 25f
        val expectedAttachment =
            Attachment(givenAttachmentId, "filename.png", State.Uploading)
        val mockUploadProgress: ((Float) -> Unit) = spyk()
        val progressSlot = slot<Float>()
        subject.prepare(
            givenAttachmentId,
            ByteArray(1),
            "filename.png",
            mockUploadProgress
        )
        clearMocks(mockAttachmentListener)

        subject.upload(givenPresignedUrlResponse)

        coVerify {
            mockAttachmentListener.invoke(capture(attachmentSlot))
            mockApi.uploadFile(givenPresignedUrlResponse, ByteArray(1), mockUploadProgress)
            mockUploadProgress.invoke(capture(progressSlot))
        }
        assertNotNull(mockUploadProgress)
        assertEquals(expectedAttachment, attachmentSlot.captured)
        assertEquals(expectedProgress, progressSlot.captured)
    }

    @Test
    fun whenUploadSuccess() {
        val expectedDownloadUrl = "http://somedownloadurl.com"
        subject.prepare(givenAttachmentId, ByteArray(1), "image.png")
        val expectedAttachment =
            Attachment(givenAttachmentId, "image.png", State.Uploaded(expectedDownloadUrl))
        clearMocks(mockAttachmentListener)

        subject.onUploadSuccess(givenUploadSuccessEvent)

        verify { mockAttachmentListener.invoke(capture(attachmentSlot)) }
        assertEquals(expectedAttachment, attachmentSlot.captured)
    }

    @Test
    fun whenDetachUploaded() {
        val expectedAttachment = Attachment(givenAttachmentId, "image.png", State.Detached)
        val mockDeleteFun: () -> Unit = spyk()
        subject.prepare(givenAttachmentId, ByteArray(1), "image.png")
        subject.onUploadSuccess(givenUploadSuccessEvent)
        clearMocks(mockAttachmentListener)

        subject.detach(givenAttachmentId, mockDeleteFun)

        verifyOrder {
            mockDeleteFun.invoke()
            mockAttachmentListener.invoke(capture(attachmentSlot))
        }
        assertEquals(expectedAttachment, attachmentSlot.captured)
    }

    @Test
    fun whenDetachOnNonUploadedAttachment() {
        val expectedAttachment = Attachment(givenAttachmentId, "image.png", State.Detached)
        val mockDeleteFun: () -> Unit = spyk()
        subject.prepare(givenAttachmentId, ByteArray(1), "image.png")
        clearMocks(mockAttachmentListener)

        subject.detach(givenAttachmentId, mockDeleteFun)

        verify { mockAttachmentListener.invoke(capture(attachmentSlot)) }
        verify(exactly = 0) { mockDeleteFun.invoke() }
        assertEquals(expectedAttachment, attachmentSlot.captured)
    }

    @Test
    fun whenOnDeleted() {
        val expectedAttachment = Attachment(givenAttachmentId, "image.png", State.Deleted)
        subject.prepare(givenAttachmentId, ByteArray(1), "image.png")
        clearMocks(mockAttachmentListener)

        subject.onDeleted(givenAttachmentId)

        verify { mockAttachmentListener.invoke(capture(attachmentSlot)) }
        assertEquals(expectedAttachment, attachmentSlot.captured)
    }

    @Test
    fun whenAttachmentIdDoesNotExist() {
        subject.prepare(givenAttachmentId, ByteArray(1), "filename.png")
        clearMocks(mockAttachmentListener)

        subject.upload(presignedUrlResponse("some attachment id"))
        subject.onUploadSuccess(uploadSuccessEvent("some attachment id"))
        subject.detach("some attachment id", mockk())
        subject.onDeleted("some attachment id")

        verify(exactly = 0) { mockAttachmentListener.invoke(any()) }
    }

    @Test
    fun whenOnError() {
        val expectedAttachment =
            Attachment(
                givenAttachmentId,
                "filename.png",
                State.Error(ErrorCode.FileTypeInvalid, "something went wrong")
            )
        subject.prepare(givenAttachmentId, ByteArray(1), "filename.png")
        clearMocks(mockAttachmentListener)

        subject.onError(givenAttachmentId, ErrorCode.mapFrom(4001), "something went wrong")

        verify { mockAttachmentListener.invoke(capture(attachmentSlot)) }
        assertEquals(expectedAttachment, attachmentSlot.captured)
    }

    @Test
    fun whenOnUploadThrowsResponseException() {
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
        val expectedAttachment =
            Attachment(
                givenAttachmentId,
                "filename.png",
                State.Error(
                    ErrorCode.ClientResponseError(404),
                    "Client request(http://someurl.com) invalid: 404 page not found. Text: \"something went wrong\""
                )
            )
        subject.prepare(givenAttachmentId, ByteArray(1), "filename.png")
        clearMocks(mockAttachmentListener)

        subject.upload(givenPresignedUrlResponse)

        coVerify {
            mockAttachmentListener.invoke(capture(attachmentSlotList))
            mockApi.uploadFile(any(), any(), any())
            mockAttachmentListener.invoke(capture(attachmentSlotList))
        }
        assertEquals(expectedAttachment, attachmentSlotList[1])
    }

    private fun presignedUrlResponse(id: String = givenAttachmentId): PresignedUrlResponse =
        PresignedUrlResponse(
            id, mapOf("header" to "given header"), "http://someuploadurl.com",
        )

    private fun uploadSuccessEvent(id: String = givenAttachmentId): UploadSuccessEvent =
        UploadSuccessEvent(
            id, "http://somedownloadurl.com", "2021-08-17T17:00:08.746Z",
        )
}
