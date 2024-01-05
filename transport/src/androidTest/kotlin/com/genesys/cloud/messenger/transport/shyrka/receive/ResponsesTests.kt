package com.genesys.cloud.messenger.transport.shyrka.receive

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.genesys.cloud.messenger.transport.shyrka.WebMessagingJson
import com.genesys.cloud.messenger.transport.utility.AttachmentValues
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

        val result = WebMessagingJson.json.decodeFromString<AttachmentDeletedResponse>(givenAttachmentDeleteResponseAsJson)

        assertThat(result).isEqualTo(expectedAttachmentDeleteResponse)
        assertThat(result.attachmentId).isEqualTo(AttachmentValues.Id)
    }
}
