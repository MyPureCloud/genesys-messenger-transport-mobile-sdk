package com.genesys.cloud.messenger.transport.shyrka.receive

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import com.genesys.cloud.messenger.transport.core.ErrorCode
import com.genesys.cloud.messenger.transport.shyrka.WebMessagingJson
import com.genesys.cloud.messenger.transport.utility.AttachmentValues
import com.genesys.cloud.messenger.transport.utility.AuthTest
import com.genesys.cloud.messenger.transport.utility.ErrorTest
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import org.junit.Test

class ResponsesTests {

    @Test
    fun `when AttachmentDeleteResponse serialized`() {
        val givenAttachmentDeleteResponse = AttachmentDeletedResponse(AttachmentValues.Id)
        val expectedAttachmentDeleteResponseAsJson = """{"attachmentId":"test_attachment_id"}"""

        val result = WebMessagingJson.json.encodeToString(givenAttachmentDeleteResponse)

        assertThat(result).isEqualTo(expectedAttachmentDeleteResponseAsJson)
    }

    @Test
    fun `when AttachmentDeleteResponse deserialized`() {
        val givenAttachmentDeleteResponseAsJson = """{"attachmentId":"test_attachment_id"}"""
        val expectedAttachmentDeleteResponse = AttachmentDeletedResponse(AttachmentValues.Id)

        val result = WebMessagingJson.json.decodeFromString<AttachmentDeletedResponse>(
            givenAttachmentDeleteResponseAsJson
        )

        assertThat(result).isEqualTo(expectedAttachmentDeleteResponse)
        assertThat(result.attachmentId).isEqualTo(AttachmentValues.Id)
    }

    @Test
    fun `when GenerateUrlError serialized`() {
        val givenGenerateUrlError = GenerateUrlError(
            attachmentId = AttachmentValues.Id,
            errorCode = ErrorCode.FileNameInvalid.code,
            errorMessage = ErrorTest.Message
        )
        val expectedGenerateUrlErrorAsJson = """{"attachmentId":"test_attachment_id","errorCode":4004,"errorMessage":"This is a generic error message for testing."}"""

        val result = WebMessagingJson.json.encodeToString(givenGenerateUrlError)

        assertThat(result).isEqualTo(expectedGenerateUrlErrorAsJson)
    }

    @Test
    fun `when GenerateUrlError deserialized`() {
        val givenGenerateUrlErrorAsJson = """{"attachmentId":"test_attachment_id","errorCode":4004,"errorMessage":"This is a generic error message for testing."}"""
        val expectedGenerateUrlError = GenerateUrlError(
            attachmentId = AttachmentValues.Id,
            errorCode = ErrorCode.FileNameInvalid.code,
            errorMessage = ErrorTest.Message
        )

        val result = WebMessagingJson.json.decodeFromString<GenerateUrlError>(
            givenGenerateUrlErrorAsJson
        )

        result.run {
            assertThat(this).isEqualTo(expectedGenerateUrlError)
            assertThat(attachmentId).isEqualTo(AttachmentValues.Id)
            assertThat(errorCode).isEqualTo(ErrorCode.FileNameInvalid.code)
            assertThat(errorMessage).isEqualTo(ErrorTest.Message)
        }
    }

    @Test
    fun `when JwtResponse serialized`() {
        val givenJwtResponse = JwtResponse(
            jwt = AuthTest.JwtToken,
            exp = AuthTest.JwtExpiry,
        )
        val expectedJwtResponseAsJson = """{"jwt":"jwt_Token","exp":100}"""

        val result = WebMessagingJson.json.encodeToString(givenJwtResponse)

        assertThat(result).isEqualTo(expectedJwtResponseAsJson)
    }

    @Test
    fun `when JwtResponse deserialized`() {
        val givenJwtResponseAsJson = """{"jwt":"jwt_Token","exp":100}"""
        val expectedJwtResponse = JwtResponse(
            jwt = AuthTest.JwtToken,
            exp = AuthTest.JwtExpiry,
        )

        val result = WebMessagingJson.json.decodeFromString<JwtResponse>(givenJwtResponseAsJson)

        result.run {
            assertThat(this).isEqualTo(expectedJwtResponse)
            assertThat(jwt).isEqualTo(AuthTest.JwtToken)
            assertThat(exp).isEqualTo(AuthTest.JwtExpiry)
        }
    }

    @Test
    fun `when PresignedUrlResponse serialized`() {
        val givenPresignedUrlResponse = PresignedUrlResponse(
            attachmentId = AttachmentValues.Id,
            fileName = AttachmentValues.FileName,
            headers = mapOf(AttachmentValues.PresignedHeaderKey to AttachmentValues.PresignedHeaderValue),
            url = AttachmentValues.DownloadUrl,
            fileSize = AttachmentValues.FileSize,
            fileType = AttachmentValues.FileType,
        )
        val expectedPresignedUrlResponse = """{"attachmentId":"test_attachment_id","headers":{"x-amz-tagging":"abc"},"url":"https://downloadurl.png","fileName":"fileName","fileSize":100,"fileType":"png"}"""

        val result = WebMessagingJson.json.encodeToString(givenPresignedUrlResponse)

        assertThat(result).isEqualTo(expectedPresignedUrlResponse)
    }

    @Test
    fun `when PresignedUrlResponse deserialized`() {
        val givenPresignedUrlResponseAsJson = """{"attachmentId":"test_attachment_id","headers":{"x-amz-tagging":"abc"},"url":"https://downloadurl.png","fileName":"fileName","fileSize":100,"fileType":"png"}"""
        val expectedPresignedUrlResponse = PresignedUrlResponse(
            attachmentId = AttachmentValues.Id,
            fileName = AttachmentValues.FileName,
            headers = mapOf(AttachmentValues.PresignedHeaderKey to AttachmentValues.PresignedHeaderValue),
            url = AttachmentValues.DownloadUrl,
            fileSize = AttachmentValues.FileSize,
            fileType = AttachmentValues.FileType,
        )

        val result = WebMessagingJson.json.decodeFromString<PresignedUrlResponse>(givenPresignedUrlResponseAsJson)

        result.run {
            assertThat(this).isEqualTo(expectedPresignedUrlResponse)
            assertThat(attachmentId).isEqualTo(expectedPresignedUrlResponse.attachmentId)
            assertThat(fileName).isEqualTo(expectedPresignedUrlResponse.fileName)
            assertThat(headers[AttachmentValues.PresignedHeaderKey]).isEqualTo(expectedPresignedUrlResponse.headers[AttachmentValues.PresignedHeaderKey])
            assertThat(url).isEqualTo(expectedPresignedUrlResponse.url)
            assertThat(fileSize).isEqualTo(expectedPresignedUrlResponse.fileSize)
            assertThat(fileName).isEqualTo(expectedPresignedUrlResponse.fileName)
        }
    }


    @Test
    fun `when SessionResponse serialized`() {
        val givenSessionResponse = SessionResponse(
            connected = true,
            newSession = true,
            readOnly = false,
        )
        val expectedSessionResponse = """{"connected":true,"newSession":true}"""

        val result = WebMessagingJson.json.encodeToString(givenSessionResponse)

        assertThat(result).isEqualTo(expectedSessionResponse)
    }

    @Test
    fun `when SessionResponse deserialized`() {
        val givenSessionResponseAsJson = """{"connected":true,"newSession":true,"readOnly":false}"""

        val result = WebMessagingJson.json.decodeFromString<SessionResponse>(givenSessionResponseAsJson)

        result.run {
            assertThat(connected).isTrue()
            assertThat(newSession).isTrue()
            assertThat(readOnly).isFalse()
        }
    }
}
