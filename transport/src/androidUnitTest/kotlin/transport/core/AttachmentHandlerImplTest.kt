package transport.core

import assertk.assertThat
import assertk.assertions.containsOnly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNull
import assertk.assertions.isTrue
import com.genesys.cloud.messenger.transport.core.Attachment
import com.genesys.cloud.messenger.transport.core.Attachment.State
import com.genesys.cloud.messenger.transport.core.AttachmentHandlerImpl
import com.genesys.cloud.messenger.transport.core.Empty
import com.genesys.cloud.messenger.transport.core.ErrorCode
import com.genesys.cloud.messenger.transport.core.ErrorMessage
import com.genesys.cloud.messenger.transport.core.FileAttachmentProfile
import com.genesys.cloud.messenger.transport.core.ProcessedAttachment
import com.genesys.cloud.messenger.transport.core.Result
import com.genesys.cloud.messenger.transport.network.WebMessagingApi
import com.genesys.cloud.messenger.transport.shyrka.WebMessagingJson
import com.genesys.cloud.messenger.transport.shyrka.receive.PresignedUrlResponse
import com.genesys.cloud.messenger.transport.shyrka.receive.UploadSuccessEvent
import com.genesys.cloud.messenger.transport.shyrka.send.DeleteAttachmentRequest
import com.genesys.cloud.messenger.transport.shyrka.send.OnAttachmentRequest
import com.genesys.cloud.messenger.transport.util.logs.Log
import com.genesys.cloud.messenger.transport.util.logs.LogMessages
import com.genesys.cloud.messenger.transport.utility.AttachmentValues
import com.genesys.cloud.messenger.transport.utility.TestValues
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
import kotlinx.serialization.encodeToString
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
    private val mockLogger: Log = mockk(relaxed = true)
    private val logSlot = mutableListOf<() -> String>()
    private val attachmentSlot = slot<Attachment>()
    private val mockAttachmentListener: (Attachment) -> Unit = spyk()
    private val processedAttachments = mutableMapOf<String, ProcessedAttachment>()

    private val givenPresignedUrlResponse = presignedUrlResponse()
    private val givenUploadSuccessEvent = uploadSuccessEvent()

    @ExperimentalCoroutinesApi
    private val threadSurrogate = newSingleThreadContext("main thread")

    private val subject = AttachmentHandlerImpl(
        mockApi,
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
    fun `when prepare()`() {
        val expectedAttachment = Attachment(
            AttachmentValues.Id,
            AttachmentValues.FileName,
            null,
            State.Presigning
        )
        val expectedProcessedAttachment = ProcessedAttachment(expectedAttachment, ByteArray(AttachmentValues.FileSize))
        val expectedOnAttachmentRequest = OnAttachmentRequest(
            token = TestValues.Token,
            attachmentId = AttachmentValues.Id,
            fileName = AttachmentValues.FileName,
            fileType = "image/png",
            fileSize = AttachmentValues.FileSize,
            null,
            true,
        )

        val onAttachmentRequest =
            subject.prepare(TestValues.Token, AttachmentValues.Id, ByteArray(AttachmentValues.FileSize), AttachmentValues.FileName)

        verify {
            mockLogger.i(capture(logSlot))
            mockAttachmentListener.invoke(capture(attachmentSlot))
        }
        assertThat(onAttachmentRequest).isEqualTo(expectedOnAttachmentRequest)
        assertThat(processedAttachments).containsOnly(AttachmentValues.Id to expectedProcessedAttachment)
        assertThat(attachmentSlot.captured).isEqualTo(expectedAttachment)
        assertThat(logSlot[0].invoke()).isEqualTo(
            LogMessages.presigningAttachment(
                expectedAttachment
            )
        )
    }

    @Test
    fun `when prepare() txt file`() {
        val expectedAttachment = Attachment(
            AttachmentValues.Id,
            AttachmentValues.TXT_FILE_NAME,
            null,
            State.Presigning
        )
        val expectedProcessedAttachment = ProcessedAttachment(expectedAttachment, ByteArray(AttachmentValues.FileSize))
        val expectedOnAttachmentRequest = OnAttachmentRequest(
            token = TestValues.Token,
            attachmentId = AttachmentValues.Id,
            fileName = AttachmentValues.TXT_FILE_NAME,
            fileType = "text/plain",
            fileSize = AttachmentValues.FileSize,
            null,
            true,
        )

        val onAttachmentRequest =
            subject.prepare(TestValues.Token, AttachmentValues.Id, ByteArray(AttachmentValues.FileSize), AttachmentValues.TXT_FILE_NAME)

        verify {
            mockLogger.i(capture(logSlot))
            mockAttachmentListener.invoke(capture(attachmentSlot))
        }
        assertThat(onAttachmentRequest).isEqualTo(expectedOnAttachmentRequest)
        assertThat(processedAttachments).containsOnly(AttachmentValues.Id to expectedProcessedAttachment)
        assertThat(attachmentSlot.captured).isEqualTo(expectedAttachment)
        assertThat(logSlot[0].invoke()).isEqualTo(
            LogMessages.presigningAttachment(
                expectedAttachment
            )
        )
    }

    @Test
    fun `when upload() processed attachment`() {
        val expectedProgress = 25f
        val expectedAttachment =
            Attachment(AttachmentValues.Id, AttachmentValues.FileName, null, State.Uploading)
        val mockUploadProgress: ((Float) -> Unit) = spyk()
        val progressSlot = slot<Float>()
        givenPrepareCalled(uploadProgress = mockUploadProgress)
        val expectedPresignedUrlResponse = givenPresignedUrlResponse.copy(fileName = AttachmentValues.FileName)

        subject.upload(givenPresignedUrlResponse)

        coVerify {
            mockLogger.i(capture(logSlot))
            mockAttachmentListener.invoke(capture(attachmentSlot))
            mockApi.uploadFile(expectedPresignedUrlResponse, ByteArray(1), mockUploadProgress)
            mockUploadProgress.invoke(capture(progressSlot))
        }
        assertThat(attachmentSlot.captured).isEqualTo(expectedAttachment)
        assertThat(progressSlot.captured).isEqualTo(expectedProgress)
        assertThat(logSlot[1].invoke()).isEqualTo(LogMessages.uploadingAttachment(expectedAttachment))
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
        val expectedState = State.Error(
            ErrorCode.ClientResponseError(404),
            "something went wrong"
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
        assertThat(logSlot[0].invoke()).isEqualTo(
            LogMessages.attachmentError(
                expectedAttachment.id,
                expectedState.errorCode,
                expectedState.errorMessage
            )
        )
    }

    @Test
    fun `when uploadFile() fails with CancellationError`() {
        val attachmentSlotList = mutableListOf<Attachment>()
        coEvery { mockApi.uploadFile(any(), any(), any()) } returns Result.Failure(
            ErrorCode.CancellationError,
            "upload was cancelled."
        )
        val expectedAttachment = Attachment(
            id = AttachmentValues.Id,
            fileName = AttachmentValues.FileName,
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
        assertThat(processedAttachments.containsKey(AttachmentValues.Id)).isTrue()
    }

    @Test
    fun `when onUploadSuccess() on processed attachment`() {
        val expectedDownloadUrl = "http://somedownloadurl.com"
        val expectedState = State.Uploaded(expectedDownloadUrl)
        val expectedAttachment =
            Attachment(AttachmentValues.Id, AttachmentValues.FileName, null, expectedState)
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
    fun `when onUploadSuccess() on not processed attachment`() {
        givenPrepareCalled()

        subject.onUploadSuccess(uploadSuccessEvent(id = "not processed attachment id"))

        verify {
            listOf(mockAttachmentListener) wasNot Called
        }
    }

    @Test
    fun `when detach() on uploaded attachment`() {
        val expectedAttachment =
            Attachment(AttachmentValues.Id, AttachmentValues.FileName, null, State.Detaching)
        val expectedProcessedAttachment = ProcessedAttachment(expectedAttachment, ByteArray(1))
        val expectedDeleteAttachmentRequest =
            DeleteAttachmentRequest(TestValues.Token, AttachmentValues.Id)
        givenPrepareCalled()
        givenUploadSuccessCalled()

        val result = subject.detach(TestValues.Token, AttachmentValues.Id)

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
    fun `when detach() on not uploaded attachment`() {
        val expectedAttachment =
            Attachment(AttachmentValues.Id, AttachmentValues.FileName, null, State.Detached)

        givenPrepareCalled()

        val result = subject.detach(TestValues.Token, AttachmentValues.Id)

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
    fun `when detach() on not processed attachment`() {
        val givenAttachmentId = TestValues.DEFAULT_STRING

        val exception = assertFailsWith<IllegalArgumentException> {
            subject.detach(TestValues.Token, givenAttachmentId)
        }
        assertThat(exception.message).isEqualTo(ErrorMessage.detachFailed(givenAttachmentId))

        verify { listOf(mockAttachmentListener) wasNot Called }
    }

    @Test
    fun `when OnDetached()`() {
        val expectedAttachment =
            Attachment(AttachmentValues.Id, AttachmentValues.FileName, null, State.Detached)
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
    fun `when OnError()`() {
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
        assertThat(logSlot[0].invoke()).isEqualTo(
            LogMessages.attachmentError(
                expectedAttachment.id,
                expectedState.errorCode,
                expectedState.errorMessage
            )
        )
    }

    @Test
    fun `when OnMessageError() while has sending attachment`() {
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
        assertThat(logSlot[0].invoke()).isEqualTo(
            LogMessages.attachmentError(
                expectedAttachment.id,
                expectedState.errorCode,
                expectedState.errorMessage
            )
        )
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
        assertThat(logSlot[0].invoke()).isEqualTo(
            LogMessages.attachmentError(
                expectedAttachment.id,
                expectedState.errorCode,
                ""
            )
        )
    }

    @Test
    fun `when onMessageError() has no sending attachment`() {
        subject.onMessageError(ErrorCode.MessageTooLong, "Message too long")

        verify { listOf(mockAttachmentListener) wasNot Called }
    }

    @Test
    fun `when onSending() has uploaded attachment`() {
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
    fun `when onSent() on processed attachment`() {
        val expectedDownloadUrl = "http://somedownloadurl.com"
        val expectedState = State.Sent(expectedDownloadUrl)
        val expectedAttachment =
            Attachment(AttachmentValues.Id, AttachmentValues.FileName, AttachmentValues.FileSize, expectedState)
        givenPrepareCalled()
        givenUploadSuccessCalled()
        givenOnSendingCalled()

        subject.onSent(
            mapOf(
                AttachmentValues.Id to Attachment(
                    AttachmentValues.Id,
                    AttachmentValues.FileName,
                    AttachmentValues.FileSize,
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
        assertThat(logSlot[3].invoke()).isEqualTo(
            LogMessages.attachmentSent(
                mapOf(
                    expectedAttachment.id to expectedAttachment
                )
            )
        )
    }

    @Test
    fun `when onSent() on not processed Attachment`() {
        subject.onSent(
            mapOf(
                AttachmentValues.Id to Attachment(
                    AttachmentValues.Id,
                    AttachmentValues.FileName,
                    AttachmentValues.FileSize,
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
            token = TestValues.Token,
            attachmentId = AttachmentValues.Id,
            fileName = AttachmentValues.FileName,
            fileType = "image/png",
            2000,
            null,
            true,
        )

        val givenByteArray = ByteArray(2000)

        val onAttachmentRequest =
            subject.prepare(TestValues.Token, AttachmentValues.Id, givenByteArray, AttachmentValues.FileName)

        assertThat(subject.fileAttachmentProfile).isNull()
        assertThat(onAttachmentRequest).isEqualTo(expectedOnAttachmentRequest)
    }

    @Test
    fun `when prepare() but maxFileSizeKB in fileAttachmentProfile is less then provided ByteArray size`() {
        val givenByteArray = ByteArray(2000)
        val expectedExceptionMessage = ErrorMessage.fileSizeIsTooBig(maxFileSize = 1)
        subject.fileAttachmentProfile = FileAttachmentProfile(enabled = true, maxFileSizeKB = 1)

        assertFailsWith<IllegalArgumentException>(expectedExceptionMessage) {
            subject.prepare(TestValues.Token, AttachmentValues.Id, givenByteArray, AttachmentValues.FileName)
        }
    }

    @Test
    fun `when prepare() and maxFileSizeKB in fileAttachmentProfile is equal to then provided ByteArray size`() {
        val givenByteArray = ByteArray(2000)
        subject.fileAttachmentProfile = FileAttachmentProfile(enabled = true, maxFileSizeKB = 2)
        val expectedFileSizeInKB = 2L
        val expectedOnAttachmentRequest = OnAttachmentRequest(
            token = TestValues.Token,
            attachmentId = AttachmentValues.Id,
            fileName = AttachmentValues.FileName,
            fileType = "image/png",
            2000,
            null,
            true,
        )

        val onAttachmentRequest =
            subject.prepare(TestValues.Token, AttachmentValues.Id, givenByteArray, AttachmentValues.FileName)

        assertThat(subject.fileAttachmentProfile?.maxFileSizeKB).isEqualTo(expectedFileSizeInKB)
        assertThat(onAttachmentRequest).isEqualTo(expectedOnAttachmentRequest)
    }

    @Test
    fun `when prepare() and ByteArray size is 0`() {
        val givenByteArray = ByteArray(0)
        val expectedExceptionMessage = ErrorMessage.FileSizeIsToSmall
        subject.fileAttachmentProfile = FileAttachmentProfile(enabled = true, maxFileSizeKB = 2)

        assertFailsWith<IllegalArgumentException>(expectedExceptionMessage) {
            subject.prepare(TestValues.Token, AttachmentValues.Id, givenByteArray, AttachmentValues.FileName)
        }
    }

    @Test
    fun `when prepare() and maxFileSizeKB in fileAttachmentProfile is greater then provided ByteArray size`() {
        val givenByteArray = ByteArray(2000)
        subject.fileAttachmentProfile = FileAttachmentProfile(enabled = true, maxFileSizeKB = 100)
        val expectedFileSizeInKB = 100L
        val expectedOnAttachmentRequest = OnAttachmentRequest(
            token = TestValues.Token,
            attachmentId = AttachmentValues.Id,
            fileName = AttachmentValues.FileName,
            fileType = "image/png",
            2000,
            null,
            true,
        )

        val onAttachmentRequest =
            subject.prepare(TestValues.Token, AttachmentValues.Id, givenByteArray, AttachmentValues.FileName)

        assertThat(subject.fileAttachmentProfile?.maxFileSizeKB).isEqualTo(expectedFileSizeInKB)
        assertThat(onAttachmentRequest).isEqualTo(expectedOnAttachmentRequest)
    }

    @Test
    fun `when prepare() but file extension is included in blockedFileTypes list`() {
        val givenByteArray = ByteArray(1)
        val expectedExceptionMessage = ErrorMessage.fileTypeIsProhibited(fileName = "foo.exe")
        subject.fileAttachmentProfile =
            FileAttachmentProfile(
                enabled = true,
                maxFileSizeKB = 100,
                blockedFileTypes = listOf(".exe")
            )

        assertFailsWith<IllegalArgumentException>(expectedExceptionMessage) {
            subject.prepare(TestValues.Token, AttachmentValues.Id, givenByteArray, "foo.exe")
        }
    }

    @Test
    fun `when prepare() and file extension is NOT included in blockedFileTypes list`() {
        val givenByteArray = ByteArray(1)
        subject.fileAttachmentProfile =
            FileAttachmentProfile(
                enabled = true,
                maxFileSizeKB = 100,
                blockedFileTypes = listOf(".exe")
            )
        val expectedOnAttachmentRequest = OnAttachmentRequest(
            token = TestValues.Token,
            attachmentId = AttachmentValues.Id,
            fileName = AttachmentValues.FileName,
            fileType = "image/png",
            1,
            null,
            true,
        )

        val onAttachmentRequest =
            subject.prepare(TestValues.Token, AttachmentValues.Id, givenByteArray, AttachmentValues.FileName)

        assertThat(onAttachmentRequest).isEqualTo(expectedOnAttachmentRequest)
    }

    @Test
    fun `when prepare() and file attachment is disabled in deployment config`() {
        val givenByteArray = ByteArray(1)
        subject.fileAttachmentProfile = FileAttachmentProfile()
        val expectedExceptionMessage = ErrorMessage.FileAttachmentIsDisabled

        assertFailsWith<IllegalArgumentException>(expectedExceptionMessage) {
            subject.prepare(TestValues.Token, AttachmentValues.Id, givenByteArray, AttachmentValues.FileName)
        }
    }

    @Test
    fun `when onAttachmentRefreshed()`() {
        val givenPresignedUrlResponse = PresignedUrlResponse(
            attachmentId = AttachmentValues.Id,
            fileName = AttachmentValues.FileName,
            url = "https://refreshedUrl.com",
            headers = emptyMap(),
        )
        val expectedAttachment = Attachment(
            id = AttachmentValues.Id,
            fileName = AttachmentValues.FileName,
            state = State.Refreshed("https://refreshedUrl.com")
        )

        subject.onAttachmentRefreshed(givenPresignedUrlResponse)

        verify { mockAttachmentListener.invoke(capture(attachmentSlot)) }
        assertThat(attachmentSlot.captured).isEqualTo(expectedAttachment)
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
            token = TestValues.Token,
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
