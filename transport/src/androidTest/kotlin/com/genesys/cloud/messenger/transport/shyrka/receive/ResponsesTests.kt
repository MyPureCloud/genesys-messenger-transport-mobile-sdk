package com.genesys.cloud.messenger.transport.shyrka.receive

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.genesys.cloud.messenger.transport.core.ErrorCode
import com.genesys.cloud.messenger.transport.shyrka.WebMessagingJson
import com.genesys.cloud.messenger.transport.utility.AttachmentValues
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
}
