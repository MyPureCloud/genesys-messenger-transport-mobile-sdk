package com.genesys.cloud.messenger.transport.network

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.genesys.cloud.messenger.transport.shyrka.WebMessagingJson
import com.genesys.cloud.messenger.transport.shyrka.send.GetAttachmentRequest
import com.genesys.cloud.messenger.transport.utility.AttachmentValues
import com.genesys.cloud.messenger.transport.utility.TestValues
import kotlin.test.Test
import kotlinx.serialization.encodeToString

class RequestSerializationTest {

    @Test
    fun `validate GetAttachmentRequest serialization`() {
        val getAttachmentRequest = GetAttachmentRequest(
            token = TestValues.Token,
            attachmentId = AttachmentValues.Id
        )

        val encodedString = WebMessagingJson.json.encodeToString(getAttachmentRequest)

        assertThat(encodedString, "encoded GetAttachmentRequest")
            .isEqualTo("""{"token":"<token>","attachmentId":"test_attachment_id","action":"getAttachment"}""")
    }
}
