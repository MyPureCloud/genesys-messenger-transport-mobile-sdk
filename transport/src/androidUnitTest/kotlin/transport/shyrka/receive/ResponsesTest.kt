package transport.shyrka.receive

import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNull
import assertk.assertions.isTrue
import com.genesys.cloud.messenger.transport.core.ButtonResponse
import com.genesys.cloud.messenger.transport.core.ErrorCode
import com.genesys.cloud.messenger.transport.core.MAX_CUSTOM_DATA_BYTES_UNSET
import com.genesys.cloud.messenger.transport.shyrka.WebMessagingJson
import com.genesys.cloud.messenger.transport.shyrka.receive.AttachmentDeletedResponse
import com.genesys.cloud.messenger.transport.shyrka.receive.GenerateUrlError
import com.genesys.cloud.messenger.transport.shyrka.receive.JwtResponse
import com.genesys.cloud.messenger.transport.shyrka.receive.PresignedUrlResponse
import com.genesys.cloud.messenger.transport.shyrka.receive.PushErrorResponse
import com.genesys.cloud.messenger.transport.shyrka.receive.SessionResponse
import com.genesys.cloud.messenger.transport.shyrka.receive.StructuredMessage
import com.genesys.cloud.messenger.transport.shyrka.receive.TooManyRequestsErrorMessage
import com.genesys.cloud.messenger.transport.shyrka.receive.UploadFailureEvent
import com.genesys.cloud.messenger.transport.shyrka.receive.UploadSuccessEvent
import com.genesys.cloud.messenger.transport.utility.AttachmentValues
import com.genesys.cloud.messenger.transport.utility.AuthTest
import com.genesys.cloud.messenger.transport.utility.ErrorTest
import com.genesys.cloud.messenger.transport.utility.PushTestValues
import com.genesys.cloud.messenger.transport.utility.QuickReplyTestValues
import com.genesys.cloud.messenger.transport.utility.QuickReplyTestValues.createButtonResponseContentForTesting
import com.genesys.cloud.messenger.transport.utility.QuickReplyTestValues.createQuickReplyContentForTesting
import com.genesys.cloud.messenger.transport.utility.TestValues
import kotlinx.serialization.encodeToString
import org.junit.Test

class ResponsesTest {

    @Test
    fun `when AttachmentDeleteResponse serialized`() {
        val expectedRequest = AttachmentDeletedResponse(AttachmentValues.ID)
        val expectedJson = """{"attachmentId":"${AttachmentValues.ID}"}"""

        val encodedString = WebMessagingJson.json.encodeToString(expectedRequest)
        val decoded = WebMessagingJson.json.decodeFromString<AttachmentDeletedResponse>(expectedJson)

        assertThat(encodedString, "encoded AttachmentDeletedResponse").isEqualTo(expectedJson)
        assertThat(decoded.attachmentId).isEqualTo(AttachmentValues.ID)
    }

    @Test
    fun `when GenerateUrlError serialized`() {
        val expectedRequest = GenerateUrlError(
            attachmentId = AttachmentValues.ID,
            errorCode = ErrorCode.FileNameInvalid.code,
            errorMessage = ErrorTest.MESSAGE
        )
        val expectedJson = """{"attachmentId":"${AttachmentValues.ID}","errorCode":4004,"errorMessage":"${ErrorTest.MESSAGE}"}"""

        val encodedString = WebMessagingJson.json.encodeToString(expectedRequest)
        val decoded = WebMessagingJson.json.decodeFromString<GenerateUrlError>(expectedJson)

        assertThat(encodedString, "encoded GenerateUrlError").isEqualTo(expectedJson)
        decoded.run {
            assertThat(this).isEqualTo(expectedRequest)
            assertThat(attachmentId).isEqualTo(AttachmentValues.ID)
            assertThat(errorCode).isEqualTo(ErrorCode.FileNameInvalid.code)
            assertThat(errorMessage).isEqualTo(ErrorTest.MESSAGE)
        }
    }

    @Test
    fun `when JwtResponse serialized`() {
        val expectedRequest = JwtResponse(AuthTest.JWT_TOKEN, AuthTest.JWT_EXPIRY)
        val expectedJson = """{"jwt":"jwt_Token","exp":100}"""

        val encodedString = WebMessagingJson.json.encodeToString(expectedRequest)
        val decoded = WebMessagingJson.json.decodeFromString<JwtResponse>(expectedJson)

        assertThat(encodedString, "encoded JwtResponse").isEqualTo(expectedJson)
        decoded.run {
            assertThat(this).isEqualTo(expectedRequest)
            assertThat(jwt).isEqualTo(AuthTest.JWT_TOKEN)
            assertThat(exp).isEqualTo(AuthTest.JWT_EXPIRY)
        }
    }

    @Test
    fun `when PresignedUrlResponse serialized`() {
        val expectedRequest = PresignedUrlResponse(
            attachmentId = AttachmentValues.ID,
            fileName = AttachmentValues.FILE_NAME,
            headers = mapOf(AttachmentValues.PRESIGNED_HEADER_KEY to AttachmentValues.PRESIGNED_HEADER_VALUE),
            url = AttachmentValues.DOWNLOAD_URL,
            fileSize = AttachmentValues.FILE_SIZE,
            fileType = AttachmentValues.FILE_TYPE,
        )
        val expectedJson = """{"attachmentId":"test_attachment_id","headers":{"x-amz-tagging":"abc"},"url":"https://downloadurl.png","fileName":"fileName.png","fileSize":100,"fileType":"png"}"""

        val encodedString = WebMessagingJson.json.encodeToString(expectedRequest)
        val decoded = WebMessagingJson.json.decodeFromString<PresignedUrlResponse>(expectedJson)

        assertThat(encodedString, "encoded PresignedUrlResponse").isEqualTo(expectedJson)
        decoded.run {
            assertThat(this).isEqualTo(expectedRequest)
            assertThat(attachmentId).isEqualTo(expectedRequest.attachmentId)
            assertThat(fileName).isEqualTo(expectedRequest.fileName)
            assertThat(headers[AttachmentValues.PRESIGNED_HEADER_KEY]).isEqualTo(
                expectedRequest.headers[AttachmentValues.PRESIGNED_HEADER_KEY]
            )
            assertThat(url).isEqualTo(expectedRequest.url)
            assertThat(fileSize).isEqualTo(expectedRequest.fileSize)
            assertThat(fileType).isEqualTo(expectedRequest.fileType)
        }
    }

    @Test
    fun `when SessionResponse serialized`() {
        val givenSessionResponse = SessionResponse(
            connected = true,
            newSession = true,
            readOnly = false,
            clearedExistingSession = false
        )
        val expectedSessionResponse = """{"connected":true,"newSession":true}"""

        val result = WebMessagingJson.json.encodeToString(givenSessionResponse)

        assertThat(result).isEqualTo(expectedSessionResponse)
    }

    @Test
    fun `when SessionResponse deserialized`() {
        val givenDefaultSessionResponseConstructor = SessionResponse(connected = true)
        val givenSessionResponseAsJson = """{"connected":true,"newSession":true,"readOnly":false,"clearedExistingSession":true}"""

        val result =
            WebMessagingJson.json.decodeFromString<SessionResponse>(givenSessionResponseAsJson)

        result.run {
            assertThat(connected).isTrue()
            assertThat(newSession).isTrue()
            assertThat(readOnly).isFalse()
            assertThat(clearedExistingSession).isTrue()
            assertThat(maxCustomDataBytes).isEqualTo(MAX_CUSTOM_DATA_BYTES_UNSET)
        }
        givenDefaultSessionResponseConstructor.run {
            assertThat(connected).isTrue()
            assertThat(newSession).isFalse()
            assertThat(readOnly).isFalse()
            assertThat(clearedExistingSession).isFalse()
        }
    }

    @Test
    fun `validate StructuredMessage_Attachment serialization`() {
        val expectedRequest = StructuredMessage.Content.AttachmentContent(
            contentType = AttachmentValues.ATTACHMENT_CONTENT_TYPE,
            attachment = StructuredMessage.Content.AttachmentContent.Attachment(
                id = AttachmentValues.ID,
                url = AttachmentValues.DOWNLOAD_URL,
                filename = AttachmentValues.FILE_NAME,
                mediaType = AttachmentValues.MEDIA_TYPE,
            )
        )
        val expectedJson =
            """{"contentType":"Attachment","attachment":{"id":"test_attachment_id","url":"https://downloadurl.png","filename":"fileName.png","mediaType":"png"}}"""

        val encodedString = WebMessagingJson.json.encodeToString(expectedRequest)
        val decoded = WebMessagingJson.json.decodeFromString<StructuredMessage.Content.AttachmentContent>(expectedJson)

        assertThat(encodedString, "encoded StructuredMessage.Content.AttachmentContent").isEqualTo(expectedJson)
        decoded.run {
            assertThat(contentType).isEqualTo(AttachmentValues.ATTACHMENT_CONTENT_TYPE)
            attachment.run {
                assertThat(id).isEqualTo(AttachmentValues.ID)
                assertThat(url).isEqualTo(AttachmentValues.DOWNLOAD_URL)
                assertThat(filename).isEqualTo(AttachmentValues.FILE_NAME)
                assertThat(mediaType).isEqualTo(AttachmentValues.MEDIA_TYPE)
                assertThat(fileSize).isNull()
                assertThat(mime).isNull()
                assertThat(sha256).isNull()
                assertThat(text).isNull()
            }
        }
    }

    @Test
    fun `validate TooManyRequestsErrorMessage serialization`() {
        val expectedRequest = TooManyRequestsErrorMessage(
            retryAfter = ErrorTest.RETRY_AFTER,
            errorCode = ErrorCode.UnexpectedError.code,
            errorMessage = ErrorTest.MESSAGE
        )
        val expectedJson =
            """{"retryAfter":1,"errorCode":5000,"errorMessage":"This is a generic error message for testing."}"""

        val encodedString = WebMessagingJson.json.encodeToString(expectedRequest)
        val decoded = WebMessagingJson.json.decodeFromString<TooManyRequestsErrorMessage>(expectedJson)

        assertThat(encodedString, "encoded TooManyRequestsErrorMessage").isEqualTo(expectedJson)
        decoded.run {
            assertThat(retryAfter).isEqualTo(ErrorTest.RETRY_AFTER)
            assertThat(errorCode).isEqualTo(ErrorCode.UnexpectedError.code)
            assertThat(errorMessage).isEqualTo(ErrorTest.MESSAGE)
        }
    }

    @Test
    fun `validate UploadFailureEvent serialization`() {
        val expectedRequest = UploadFailureEvent(
            attachmentId = AttachmentValues.ID,
            errorCode = ErrorCode.UnexpectedError.code,
            errorMessage = ErrorTest.MESSAGE,
            timestamp = TestValues.TIME_STAMP
        )
        val expectedJson =
            """{"attachmentId":"test_attachment_id","errorCode":5000,"errorMessage":"This is a generic error message for testing.","timestamp":"2022-08-22T19:24:26.704Z"}"""

        val encodedString = WebMessagingJson.json.encodeToString(expectedRequest)
        val decoded = WebMessagingJson.json.decodeFromString<UploadFailureEvent>(expectedJson)

        assertThat(encodedString, "encoded UploadFailureEvent").isEqualTo(expectedJson)
        decoded.run {
            assertThat(attachmentId).isEqualTo(AttachmentValues.ID)
            assertThat(errorCode).isEqualTo(ErrorCode.UnexpectedError.code)
            assertThat(errorMessage).isEqualTo(ErrorTest.MESSAGE)
            assertThat(timestamp).isEqualTo(TestValues.TIME_STAMP)
        }
    }

    @Test
    fun `validate UploadSuccessEvent serialization`() {
        val expectedRequest = UploadSuccessEvent(
            attachmentId = AttachmentValues.ID,
            downloadUrl = AttachmentValues.DOWNLOAD_URL,
            timestamp = TestValues.TIME_STAMP
        )
        val expectedJson =
            """{"attachmentId":"test_attachment_id","downloadUrl":"https://downloadurl.png","timestamp":"2022-08-22T19:24:26.704Z"}"""

        val encodedString = WebMessagingJson.json.encodeToString(expectedRequest)
        val decoded = WebMessagingJson.json.decodeFromString<UploadSuccessEvent>(expectedJson)

        assertThat(encodedString, "encoded UploadSuccessEvent").isEqualTo(expectedJson)
        decoded.run {
            assertThat(attachmentId).isEqualTo(AttachmentValues.ID)
            assertThat(downloadUrl).isEqualTo(AttachmentValues.DOWNLOAD_URL)
            assertThat(timestamp).isEqualTo(TestValues.TIME_STAMP)
        }
    }

    @Test
    fun `when ButtonResponse serialized`() {
        val expectedRequest = QuickReplyTestValues.buttonResponse_a
        val expectedJson = """{"text":"text_a","payload":"payload_a","type":"QuickReply"}"""

        val encodedString = WebMessagingJson.json.encodeToString(expectedRequest)
        val decoded = WebMessagingJson.json.decodeFromString<ButtonResponse>(expectedJson)

        assertThat(encodedString, "encoded ButtonResponse").isEqualTo(expectedJson)
        decoded.run {
            assertThat(this).isEqualTo(expectedRequest)
            assertThat(text).isEqualTo("text_a")
            assertThat(payload).isEqualTo(QuickReplyTestValues.PAYLOAD_A)
            assertThat(type).isEqualTo(QuickReplyTestValues.QUICK_REPLY)
        }
    }

    @Test
    fun `validate StructuredMessage_ButtonResponse serialization`() {
        val expectedRequest = createButtonResponseContentForTesting()
        val expectedJson =
            """{"contentType":"ButtonResponse","buttonResponse":{"text":"text_a","payload":"payload_a","type":"QuickReply"}}"""

        val encodedString = WebMessagingJson.json.encodeToString(expectedRequest)
        val decoded = WebMessagingJson.json.decodeFromString<StructuredMessage.Content.ButtonResponseContent>(expectedJson)

        assertThat(encodedString, "encoded StructuredMessage.Content.ButtonResponse").isEqualTo(expectedJson)
        decoded.run {
            assertThat(contentType).isEqualTo(QuickReplyTestValues.BUTTON_RESPONSE)
            buttonResponse.run {
                assertThat(text).isEqualTo(QuickReplyTestValues.TEXT_A)
                assertThat(payload).isEqualTo(QuickReplyTestValues.PAYLOAD_A)
                assertThat(type).isEqualTo(QuickReplyTestValues.QUICK_REPLY)
            }
        }
    }

    @Test
    fun `validate StructuredMessage_QuickReply serialization`() {
        val expectedRequest = createQuickReplyContentForTesting()
        val expectedJson =
            """{"contentType":"QuickReply","quickReply":{"text":"text_a","payload":"payload_a","action":"action"}}"""

        val encodedString = WebMessagingJson.json.encodeToString(expectedRequest)
        val decoded = WebMessagingJson.json.decodeFromString<StructuredMessage.Content.QuickReplyContent>(expectedJson)

        assertThat(encodedString, "encoded StructuredMessage.Content.QuickReply").isEqualTo(expectedJson)
        decoded.run {
            assertThat(contentType).isEqualTo(QuickReplyTestValues.QUICK_REPLY)
            quickReply.run {
                assertThat(text).isEqualTo(QuickReplyTestValues.TEXT_A)
                assertThat(payload).isEqualTo(QuickReplyTestValues.PAYLOAD_A)
                assertThat(action).isEqualTo("action")
            }
        }
    }

    @Test
    fun `validate PushErrorResponse serialization`() {
        val expectedRequest = PushTestValues.pushErrorResponseWith(PushTestValues.PUSH_CODE_DEPLOYMENT_NOT_FOUND)
        val expectedJson =
            """{"message":"This is a generic error message for testing.","code":"deployment.not.found","status":404,"contextId":"any string"}"""

        val encodedString = WebMessagingJson.json.encodeToString(expectedRequest)
        val decoded = WebMessagingJson.json.decodeFromString<PushErrorResponse>(expectedJson)

        assertThat(encodedString, "encoded PushErrorResponse").isEqualTo(expectedJson)
        decoded.run {
            assertThat(message).isEqualTo(ErrorTest.MESSAGE)
            assertThat(code).isEqualTo(PushTestValues.PUSH_CODE_DEPLOYMENT_NOT_FOUND)
            assertThat(status).isEqualTo(ErrorTest.CODE_404.toInt())
            assertThat(contextId).isEqualTo(TestValues.DEFAULT_STRING)
            assertThat(details).isEmpty()
        }
    }
}
