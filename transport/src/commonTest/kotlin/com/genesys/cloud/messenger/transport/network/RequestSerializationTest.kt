package com.genesys.cloud.messenger.transport.network

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.genesys.cloud.messenger.transport.shyrka.WebMessagingJson
import com.genesys.cloud.messenger.transport.shyrka.send.GetAttachmentRequest
import com.genesys.cloud.messenger.transport.shyrka.send.RequestAction
import com.genesys.cloud.messenger.transport.utility.AttachmentValues
import com.genesys.cloud.messenger.transport.utility.TestValues
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlin.test.Test

class RequestSerializationTest {

    @Test
    fun `validate GetAttachmentRequest serialization`() {
        val expectedGetAttachmentRequest = GetAttachmentRequest(
            token = TestValues.Token,
            attachmentId = AttachmentValues.Id
        )
        val expectedJson = """{"token":"<token>","attachmentId":"test_attachment_id","action":"getAttachment"}"""

        val encodedString = WebMessagingJson.json.encodeToString(expectedGetAttachmentRequest)
        val decoded = WebMessagingJson.json.decodeFromString<GetAttachmentRequest>(expectedJson)

        assertThat(encodedString, "encoded GetAttachmentRequest").isEqualTo(expectedJson)
        decoded.run {
            assertThat(action).isEqualTo(RequestAction.GET_ATTACHMENT.value)
            assertThat(token).isEqualTo(TestValues.Token)
            assertThat(attachmentId).isEqualTo(AttachmentValues.Id)
        }
    }
}
