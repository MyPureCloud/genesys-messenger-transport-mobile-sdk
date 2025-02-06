package com.genesys.cloud.messenger.transport.shyrka.receive

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNull
import assertk.assertions.isTrue
import com.genesys.cloud.messenger.transport.core.ButtonResponse
import com.genesys.cloud.messenger.transport.core.ErrorCode
import com.genesys.cloud.messenger.transport.core.MAX_CUSTOM_DATA_BYTES_UNSET
import com.genesys.cloud.messenger.transport.shyrka.WebMessagingJson
import com.genesys.cloud.messenger.transport.utility.AttachmentValues
import com.genesys.cloud.messenger.transport.utility.AuthTest
import com.genesys.cloud.messenger.transport.utility.ErrorTest
import com.genesys.cloud.messenger.transport.utility.QuickReplyTestValues
import com.genesys.cloud.messenger.transport.utility.QuickReplyTestValues.createButtonResponseContentForTesting
import com.genesys.cloud.messenger.transport.utility.QuickReplyTestValues.createQuickReplyContentForTesting
import com.genesys.cloud.messenger.transport.utility.TestValues
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import org.junit.Test

class ResponsesTests {

    @Test
    fun `when AttachmentDeleteResponse serialized`() {
        val expectedRequest = AttachmentDeletedResponse(AttachmentValues.Id)
        val expectedJson = """{"attachmentId":"${AttachmentValues.Id}"}"""

        val encodedString = WebMessagingJson.json.encodeToString(expectedRequest)
        val decoded = WebMessagingJson.json.decodeFromString<AttachmentDeletedResponse>(expectedJson)

        assertThat(encodedString, "encoded AttachmentDeletedResponse").isEqualTo(expectedJson)
        assertThat(decoded.attachmentId).isEqualTo(AttachmentValues.Id)
    }

    @Test
    fun `when GenerateUrlError serialized`() {
        val expectedRequest = GenerateUrlError(
            attachmentId = AttachmentValues.Id,
            errorCode = ErrorCode.FileNameInvalid.code,
            errorMessage = ErrorTest.Message
        )
        val expectedJson = """{"attachmentId":"${AttachmentValues.Id}","errorCode":4004,"errorMessage":"${ErrorTest.Message}"}"""

        val encodedString = WebMessagingJson.json.encodeToString(expectedRequest)
        val decoded = WebMessagingJson.json.decodeFromString<GenerateUrlError>(expectedJson)

        assertThat(encodedString, "encoded GenerateUrlError").isEqualTo(expectedJson)
        decoded.run {
            assertThat(this).isEqualTo(expectedRequest)
            assertThat(attachmentId).isEqualTo(AttachmentValues.Id)
            assertThat(errorCode).isEqualTo(ErrorCode.FileNameInvalid.code)
            assertThat(errorMessage).isEqualTo(ErrorTest.Message)
        }
    }

    @Test
    fun `when JwtResponse serialized`() {
        val expectedRequest = JwtResponse(AuthTest.JwtToken, AuthTest.JwtExpiry)
        val expectedJson = """{"jwt":"jwt_Token","exp":100}"""

        val encodedString = WebMessagingJson.json.encodeToString(expectedRequest)
        val decoded = WebMessagingJson.json.decodeFromString<JwtResponse>(expectedJson)

        assertThat(encodedString, "encoded JwtResponse").isEqualTo(expectedJson)
        decoded.run {
            assertThat(this).isEqualTo(expectedRequest)
            assertThat(jwt).isEqualTo(AuthTest.JwtToken)
            assertThat(exp).isEqualTo(AuthTest.JwtExpiry)
        }
    }

    @Test
    fun `when PresignedUrlResponse serialized`() {
        val expectedRequest = PresignedUrlResponse(
            attachmentId = AttachmentValues.Id,
            fileName = AttachmentValues.FileName,
            headers = mapOf(AttachmentValues.PresignedHeaderKey to AttachmentValues.PresignedHeaderValue),
            url = AttachmentValues.DownloadUrl,
            fileSize = AttachmentValues.FileSize,
            fileType = AttachmentValues.FileType,
        )
        val expectedJson = """{"attachmentId":"test_attachment_id","headers":{"x-amz-tagging":"abc"},"url":"https://downloadurl.png","fileName":"fileName.png","fileSize":100,"fileType":"png"}"""

        val encodedString = WebMessagingJson.json.encodeToString(expectedRequest)
        val decoded = WebMessagingJson.json.decodeFromString<PresignedUrlResponse>(expectedJson)

        assertThat(encodedString, "encoded PresignedUrlResponse").isEqualTo(expectedJson)
        decoded.run {
            assertThat(this).isEqualTo(expectedRequest)
            assertThat(attachmentId).isEqualTo(expectedRequest.attachmentId)
            assertThat(fileName).isEqualTo(expectedRequest.fileName)
            assertThat(headers[AttachmentValues.PresignedHeaderKey]).isEqualTo(
                expectedRequest.headers[AttachmentValues.PresignedHeaderKey]
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
            contentType = AttachmentValues.AttachmentContentType,
            attachment = StructuredMessage.Content.AttachmentContent.Attachment(
                id = AttachmentValues.Id,
                url = AttachmentValues.DownloadUrl,
                filename = AttachmentValues.FileName,
                mediaType = AttachmentValues.MediaType,
            )
        )
        val expectedJson =
            """{"contentType":"Attachment","attachment":{"id":"test_attachment_id","url":"https://downloadurl.png","filename":"fileName.png","mediaType":"png"}}"""

        val encodedString = WebMessagingJson.json.encodeToString(expectedRequest)
        val decoded = WebMessagingJson.json.decodeFromString<StructuredMessage.Content.AttachmentContent>(expectedJson)

        assertThat(encodedString, "encoded StructuredMessage.Content.AttachmentContent").isEqualTo(expectedJson)
        decoded.run {
            assertThat(contentType).isEqualTo(AttachmentValues.AttachmentContentType)
            attachment.run {
                assertThat(id).isEqualTo(AttachmentValues.Id)
                assertThat(url).isEqualTo(AttachmentValues.DownloadUrl)
                assertThat(filename).isEqualTo(AttachmentValues.FileName)
                assertThat(mediaType).isEqualTo(AttachmentValues.MediaType)
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
            retryAfter = ErrorTest.RetryAfter,
            errorCode = ErrorCode.UnexpectedError.code,
            errorMessage = ErrorTest.Message
        )
        val expectedJson =
            """{"retryAfter":1,"errorCode":5000,"errorMessage":"This is a generic error message for testing."}"""

        val encodedString = WebMessagingJson.json.encodeToString(expectedRequest)
        val decoded = WebMessagingJson.json.decodeFromString<TooManyRequestsErrorMessage>(expectedJson)

        assertThat(encodedString, "encoded TooManyRequestsErrorMessage").isEqualTo(expectedJson)
        decoded.run {
            assertThat(retryAfter).isEqualTo(ErrorTest.RetryAfter)
            assertThat(errorCode).isEqualTo(ErrorCode.UnexpectedError.code)
            assertThat(errorMessage).isEqualTo(ErrorTest.Message)
        }
    }

    @Test
    fun `validate UploadFailureEvent serialization`() {
        val expectedRequest = UploadFailureEvent(
            attachmentId = AttachmentValues.Id,
            errorCode = ErrorCode.UnexpectedError.code,
            errorMessage = ErrorTest.Message,
            timestamp = TestValues.Timestamp
        )
        val expectedJson =
            """{"attachmentId":"test_attachment_id","errorCode":5000,"errorMessage":"This is a generic error message for testing.","timestamp":"2022-08-22T19:24:26.704Z"}"""

        val encodedString = WebMessagingJson.json.encodeToString(expectedRequest)
        val decoded = WebMessagingJson.json.decodeFromString<UploadFailureEvent>(expectedJson)

        assertThat(encodedString, "encoded UploadFailureEvent").isEqualTo(expectedJson)
        decoded.run {
            assertThat(attachmentId).isEqualTo(AttachmentValues.Id)
            assertThat(errorCode).isEqualTo(ErrorCode.UnexpectedError.code)
            assertThat(errorMessage).isEqualTo(ErrorTest.Message)
            assertThat(timestamp).isEqualTo(TestValues.Timestamp)
        }
    }

    @Test
    fun `validate UploadSuccessEvent serialization`() {
        val expectedRequest = UploadSuccessEvent(
            attachmentId = AttachmentValues.Id,
            downloadUrl = AttachmentValues.DownloadUrl,
            timestamp = TestValues.Timestamp
        )
        val expectedJson =
            """{"attachmentId":"test_attachment_id","downloadUrl":"https://downloadurl.png","timestamp":"2022-08-22T19:24:26.704Z"}"""

        val encodedString = WebMessagingJson.json.encodeToString(expectedRequest)
        val decoded = WebMessagingJson.json.decodeFromString<UploadSuccessEvent>(expectedJson)

        assertThat(encodedString, "encoded UploadSuccessEvent").isEqualTo(expectedJson)
        decoded.run {
            assertThat(attachmentId).isEqualTo(AttachmentValues.Id)
            assertThat(downloadUrl).isEqualTo(AttachmentValues.DownloadUrl)
            assertThat(timestamp).isEqualTo(TestValues.Timestamp)
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
            assertThat(payload).isEqualTo(QuickReplyTestValues.Payload_A)
            assertThat(type).isEqualTo(QuickReplyTestValues.QuickReply)
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
            assertThat(contentType).isEqualTo(QuickReplyTestValues.ButtonResponse)
            buttonResponse.run {
                assertThat(text).isEqualTo(QuickReplyTestValues.Text_A)
                assertThat(payload).isEqualTo(QuickReplyTestValues.Payload_A)
                assertThat(type).isEqualTo(QuickReplyTestValues.QuickReply)
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
            assertThat(contentType).isEqualTo(QuickReplyTestValues.QuickReply)
            quickReply.run {
                assertThat(text).isEqualTo(QuickReplyTestValues.Text_A)
                assertThat(payload).isEqualTo(QuickReplyTestValues.Payload_A)
                assertThat(action).isEqualTo("action")
            }
        }
    }
}
