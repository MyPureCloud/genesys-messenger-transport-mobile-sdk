package com.genesys.cloud.messenger.transport.core

import assertk.assertThat
import assertk.assertions.containsOnly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNull
import com.genesys.cloud.messenger.transport.core.Attachment.State
import com.genesys.cloud.messenger.transport.network.WebMessagingApi
import com.genesys.cloud.messenger.transport.shyrka.WebMessagingJson
import com.genesys.cloud.messenger.transport.shyrka.receive.PresignedUrlResponse
import com.genesys.cloud.messenger.transport.shyrka.receive.UploadSuccessEvent
import com.genesys.cloud.messenger.transport.shyrka.send.DeleteAttachmentRequest
import com.genesys.cloud.messenger.transport.shyrka.send.OnAttachmentRequest
import com.genesys.cloud.messenger.transport.util.logs.Log
import com.genesys.cloud.messenger.transport.util.logs.LogMessages
import com.genesys.cloud.messenger.transport.utility.AttachmentValues
import com.genesys.cloud.messenger.transport.utility.ErrorTest
import com.genesys.cloud.messenger.transport.utility.TestValues
import io.ktor.client.plugins.ClientRequestException
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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.encodeToString
import org.junit.After
import org.junit.Before
import org.junit.Test

internal class AttachmentHandlerImplTest {
    private val mockApi: WebMessagingApi = mockk {
        coEvery { uploadFile(any(), any(), captureLambda()) } coAnswers {
            thirdArg<(Float) -> Unit>().invoke(25f)
        }
    }
    private val mockLogger: Log = mockk(relaxed = true)
    private val logSlot = mutableListOf<() -> String>()
    private val attachmentSlot = slot<Attachment>()
    private val mockAttachmentListener: (Attachment) -> Unit = spyk()
    private val processedAttachments = mutableMapOf<String, ProcessedAttachment>()

    private val givenUploadSuccessEvent = uploadSuccessEvent()
    private val givenPresignedUrlResponse = presignedUrlResponse()

    @ExperimentalCoroutinesApi
    private val threadSurrogate = newSingleThreadContext("main thread")

    private val subject = AttachmentHandlerImpl(
        mockApi,
        TestValues.Token,
        mockLogger,
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
        val expectedAttachment = Attachment(AttachmentValues.Id, AttachmentValues.FileName, State.Presigning)
        val expectedProcessedAttachment = ProcessedAttachment(expectedAttachment, ByteArray(1))
        val expectedOnAttachmentRequest = OnAttachmentRequest(
            token = TestValues.Token,
            attachmentId = AttachmentValues.Id,
            fileName = AttachmentValues.FileName,
            fileType = "image/png",
            1,
            null,
            true,
        )

        val onAttachmentRequest = subject.prepare(AttachmentValues.Id, ByteArray(1), AttachmentValues.FileName)

        verify {
            mockLogger.i(capture(logSlot))
            mockAttachmentListener.invoke(capture(attachmentSlot))
        }
        assertThat(onAttachmentRequest).isEqualTo(expectedOnAttachmentRequest)
        assertThat(processedAttachments).containsOnly(AttachmentValues.Id to expectedProcessedAttachment)
        assertThat(attachmentSlot.captured).isEqualTo(expectedAttachment)
        assertThat(logSlot[0].invoke()).isEqualTo(LogMessages.presigningAttachment(expectedAttachment))
    }

    @Test
    fun whenUploadProcessedAttachment() {
        val expectedProgress = 25f
        val expectedAttachment = Attachment(AttachmentValues.Id, AttachmentValues.FileName, State.Uploading)
        val mockUploadProgress: ((Float) -> Unit) = spyk()
        val progressSlot = slot<Float>()
        givenPrepareCalled(uploadProgress = mockUploadProgress)

        subject.upload(givenPresignedUrlResponse)

        coVerify {
            mockLogger.i(capture(logSlot))
            mockAttachmentListener.invoke(capture(attachmentSlot))
            mockApi.uploadFile(givenPresignedUrlResponse, ByteArray(1), mockUploadProgress)
            mockUploadProgress.invoke(capture(progressSlot))
        }
        assertThat(attachmentSlot.captured).isEqualTo(expectedAttachment)
        assertThat(progressSlot.captured).isEqualTo(expectedProgress)
        assertThat(logSlot[1].invoke()).isEqualTo(LogMessages.uploadingAttachment(expectedAttachment))
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
        val expectedState = State.Error(
            ErrorCode.ClientResponseError(404),
            "Client request( http://someurl.com) invalid: 404 page not found. Text: \"something went wrong\""
        )
        val expectedAttachment = Attachment(
            id = AttachmentValues.Id,
            state = expectedState,
        )
        givenPrepareCalled()

        subject.upload(givenPresignedUrlResponse)

        coVerify {
            mockAttachmentListener.invoke(capture(attachmentSlotList))
            mockApi.uploadFile(any(), any(), any())
            mockLogger.e(capture(logSlot))
            mockAttachmentListener.invoke(capture(attachmentSlotList))
        }
        assertThat(attachmentSlotList[1]).isEqualTo(expectedAttachment)
        assertThat(processedAttachments.containsKey(AttachmentValues.Id)).isFalse()
        assertThat(logSlot[0].invoke()).isEqualTo(LogMessages.attachmentError(expectedAttachment.id, expectedState.errorCode, expectedState.errorMessage))
    }

    @Test
    fun whenCancellationExceptionThrownDuringUpload() {
        coEvery { mockApi.uploadFile(any(), any(), any()) } throws CancellationException(ErrorTest.Message)
        val expectedAttachment = Attachment(
            id = AttachmentValues.Id,
            fileName = AttachmentValues.FileName,
            state = State.Uploading
        )
        givenPrepareCalled()

        subject.upload(givenPresignedUrlResponse)

        coVerify {
            mockApi.uploadFile(any(), any(), any())
            mockLogger.w(capture(logSlot))
        }
        assertThat(logSlot[0].invoke()).isEqualTo(LogMessages.cancellationExceptionAttachmentUpload(expectedAttachment))
    }

    @Test
    fun whenUploadSuccessForProcessedAttachment() {
        val expectedDownloadUrl = "http://somedownloadurl.com"
        val expectedState = State.Uploaded(expectedDownloadUrl)
        val expectedAttachment = Attachment(AttachmentValues.Id, AttachmentValues.FileName, expectedState)
        val expectedProcessedAttachment = ProcessedAttachment(expectedAttachment, ByteArray(1))
        givenPrepareCalled()

        subject.onUploadSuccess(givenUploadSuccessEvent)

        verify {
            mockLogger.i(capture(logSlot))
            mockAttachmentListener.invoke(capture(attachmentSlot))
        }
        attachmentSlot.captured.run {
            assertThat(this).isEqualTo(expectedAttachment)
            assertThat(state).isEqualTo(expectedState)
            assertThat((state as State.Uploaded).downloadUrl).isEqualTo(expectedDownloadUrl)
        }
        assertThat(processedAttachments).containsOnly(AttachmentValues.Id to expectedProcessedAttachment)
        assertThat(logSlot[1].invoke()).isEqualTo(LogMessages.attachmentUploaded(expectedAttachment))
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
        val expectedAttachment = Attachment(AttachmentValues.Id, AttachmentValues.FileName, State.Detaching)
        val expectedProcessedAttachment = ProcessedAttachment(expectedAttachment, ByteArray(1))
        val expectedDeleteAttachmentRequest = DeleteAttachmentRequest(TestValues.Token, AttachmentValues.Id)
        givenPrepareCalled()
        givenUploadSuccessCalled()

        val result = subject.detach(AttachmentValues.Id)

        verify {
            mockLogger.i(capture(logSlot))
            mockAttachmentListener.invoke(capture(attachmentSlot))
        }
        assertThat(result).isEqualTo(expectedDeleteAttachmentRequest)
        assertThat(attachmentSlot.captured).isEqualTo(expectedAttachment)
        assertThat(processedAttachments).containsOnly(AttachmentValues.Id to expectedProcessedAttachment)
        assertThat(logSlot[2].invoke()).isEqualTo(LogMessages.detachingAttachment(expectedAttachment.id))
    }

    @Test
    fun whenDetachNotUploaded() {
        val expectedAttachment = Attachment(AttachmentValues.Id, AttachmentValues.FileName, State.Detached)
        givenPrepareCalled()

        val result = subject.detach(AttachmentValues.Id)

        verify {
            mockLogger.i(capture(logSlot))
            mockAttachmentListener.invoke(capture(attachmentSlot))
        }
        assertThat(result).isNull()
        assertThat(attachmentSlot.captured).isEqualTo(expectedAttachment)
        assertThat(processedAttachments.containsKey(AttachmentValues.Id)).isFalse()
        assertThat(logSlot[1].invoke()).isEqualTo(LogMessages.detachingAttachment(expectedAttachment.id))
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
        val expectedAttachment = Attachment(AttachmentValues.Id, AttachmentValues.FileName, State.Detached)
        givenPrepareCalled()
        givenUploadSuccessCalled()

        subject.onDetached(AttachmentValues.Id)

        verify {
            mockLogger.i(capture(logSlot))
            mockAttachmentListener.invoke(capture(attachmentSlot))
        }
        assertThat(attachmentSlot.captured).isEqualTo(expectedAttachment)
        assertThat(processedAttachments.containsKey(AttachmentValues.Id)).isFalse()
        assertThat(logSlot[2].invoke()).isEqualTo(LogMessages.attachmentDetached(expectedAttachment.id))
    }

    @Test
    fun whenOnError() {
        val expectedState = State.Error(ErrorCode.FileTypeInvalid, "something went wrong")
        val expectedAttachment = Attachment(
            id = AttachmentValues.Id,
            state = expectedState,
        )
        givenPrepareCalled()

        subject.onError(AttachmentValues.Id, ErrorCode.mapFrom(4001), "something went wrong")

        verify {
            mockLogger.e(capture(logSlot))
            mockAttachmentListener.invoke(capture(attachmentSlot))
        }
        attachmentSlot.captured.run {
            assertThat(this).isEqualTo(expectedAttachment)
            assertThat(id).isEqualTo(expectedAttachment.id)
            assertThat(fileName).isNull()
            assertThat((state as State.Error).errorCode).isEqualTo(expectedState.errorCode)
            assertThat((state as State.Error).errorMessage).isEqualTo(expectedState.errorMessage)
        }
        assertThat(processedAttachments.containsKey(AttachmentValues.Id)).isFalse()
        assertThat(logSlot[0].invoke()).isEqualTo(LogMessages.attachmentError(expectedAttachment.id, expectedState.errorCode, expectedState.errorMessage))
    }

    @Test
    fun whenOnMessageErrorWhileHasSendingAttachment() {
        val expectedState = State.Error(ErrorCode.MessageTooLong, "Message too long")
        val expectedAttachment = Attachment(
            id = AttachmentValues.Id,
            state = expectedState,
        )
        givenPrepareCalled()
        givenUploadSuccessCalled()
        givenOnSendingCalled()

        subject.onMessageError(ErrorCode.MessageTooLong, "Message too long")

        verify {
            mockLogger.e(capture(logSlot))
            mockAttachmentListener.invoke(capture(attachmentSlot))
        }
        attachmentSlot.captured.run {
            assertThat(this).isEqualTo(expectedAttachment)
            assertThat(id).isEqualTo(expectedAttachment.id)
            assertThat(fileName).isNull()
            assertThat((state as State.Error).errorCode).isEqualTo(expectedState.errorCode)
            assertThat((state as State.Error).errorMessage).isEqualTo(expectedState.errorMessage)
        }
        assertThat(processedAttachments.containsKey(AttachmentValues.Id)).isFalse()
        assertThat(logSlot[0].invoke()).isEqualTo(LogMessages.attachmentError(expectedAttachment.id, expectedState.errorCode, expectedState.errorMessage))
    }

    @Test
    fun whenOnMessageErrorWithNullErrorMessage() {
        val expectedState = State.Error(ErrorCode.MessageTooLong, "")
        val expectedAttachment = Attachment(
            id = AttachmentValues.Id,
            state = expectedState,
        )
        givenPrepareCalled()
        givenUploadSuccessCalled()
        givenOnSendingCalled()

        subject.onMessageError(ErrorCode.MessageTooLong, null)

        verify {
            mockLogger.e(capture(logSlot))
            mockAttachmentListener.invoke(capture(attachmentSlot))
        }
        attachmentSlot.captured.run {
            assertThat(this).isEqualTo(expectedAttachment)
            assertThat(id).isEqualTo(expectedAttachment.id)
            assertThat(fileName).isNull()
            assertThat((state as State.Error).errorCode).isEqualTo(expectedState.errorCode)
            assertThat((state as State.Error).errorMessage).isEqualTo(expectedState.errorMessage)
        }
        assertThat(processedAttachments.containsKey(AttachmentValues.Id)).isFalse()
        assertThat(logSlot[0].invoke()).isEqualTo(LogMessages.attachmentError(expectedAttachment.id, expectedState.errorCode, ""))
    }

    @Test
    fun whenOnMessageErrorHasNoSendingAttachment() {
        subject.onMessageError(ErrorCode.MessageTooLong, "Message too long")

        verify { listOf(mockAttachmentListener) wasNot Called }
    }

    @Test
    fun whenOnSendingHasUploadedAttachment() {
        val expectedAttachment = Attachment(
            id = AttachmentValues.Id,
            fileName = AttachmentValues.FileName,
            state = State.Sending
        )
        val expectedProcessedAttachment =
            ProcessedAttachment(expectedAttachment, ByteArray(1))
        givenPrepareCalled()
        givenUploadSuccessCalled()

        subject.onSending()

        verify {
            mockLogger.i(capture(logSlot))
            mockAttachmentListener.invoke(capture(attachmentSlot))
        }
        assertThat(attachmentSlot.captured).isEqualTo(expectedAttachment)
        assertThat(processedAttachments).containsOnly(AttachmentValues.Id to expectedProcessedAttachment)
        assertThat(logSlot[2].invoke()).isEqualTo(LogMessages.sendingAttachment(expectedAttachment.id))
    }

    @Test
    fun whenOnSendingHasNoUploadedAttachment() {
        givenPrepareCalled()

        subject.onSending()

        verify { listOf(mockAttachmentListener) wasNot Called }
    }

    @Test
    fun whenOnSentProcessedAttachments() {
        val expectedDownloadUrl = "http://somedownloadurl.com"
        val expectedState = State.Sent(expectedDownloadUrl)
        val expectedAttachment = Attachment(AttachmentValues.Id, AttachmentValues.FileName, expectedState)
        givenPrepareCalled()
        givenUploadSuccessCalled()
        givenOnSendingCalled()

        subject.onSent(
            mapOf(
                AttachmentValues.Id to Attachment(
                    AttachmentValues.Id,
                    AttachmentValues.FileName,
                    State.Sent("http://somedownloadurl.com")
                )
            )
        )

        verify {
            mockLogger.i(capture(logSlot))
            mockAttachmentListener.invoke(capture(attachmentSlot))
        }
        attachmentSlot.captured.run {
            assertThat(this).isEqualTo(expectedAttachment)
            assertThat(state).isEqualTo(expectedState)
            assertThat((state as State.Sent).downloadUrl).isEqualTo(expectedDownloadUrl)
        }
        assertThat(processedAttachments.containsKey(AttachmentValues.Id)).isFalse()
        assertThat(logSlot[3].invoke()).isEqualTo(LogMessages.attachmentSent(mapOf(expectedAttachment.id to expectedAttachment)))
    }

    @Test
    fun whenOnSentNotProcessedAttachments() {
        subject.onSent(
            mapOf(
                AttachmentValues.Id to Attachment(
                    AttachmentValues.Id,
                    AttachmentValues.FileName,
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

    @Test
    fun whenSerializeAttachment() {
        val expectedAttachmentJson = """{"id":"${AttachmentValues.Id}"}"""
        val givenAttachment = Attachment(AttachmentValues.Id, AttachmentValues.FileName)

        val attachmentJson = WebMessagingJson.json.encodeToString(givenAttachment)

        assertThat(attachmentJson).isEqualTo(expectedAttachmentJson)
    }

    private fun presignedUrlResponse(id: String = AttachmentValues.Id): PresignedUrlResponse =
        PresignedUrlResponse(
            id, mapOf("header" to "given header"), "http://someuploadurl.com",
        )

    private fun uploadSuccessEvent(id: String = AttachmentValues.Id): UploadSuccessEvent =
        UploadSuccessEvent(
            id, "http://somedownloadurl.com", "2021-08-17T17:00:08.746Z",
        )

    /**
     * Helper function.
     * Executes the subject.prepare() call with given values and clearMocks related to this call.
     */
    private fun givenPrepareCalled(
        attachmentId: String = AttachmentValues.Id,
        byteArray: ByteArray = ByteArray(1),
        fileName: String = AttachmentValues.FileName,
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
