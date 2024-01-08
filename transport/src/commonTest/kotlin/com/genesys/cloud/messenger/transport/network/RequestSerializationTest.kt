package com.genesys.cloud.messenger.transport.network

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import com.genesys.cloud.messenger.transport.shyrka.WebMessagingJson
import com.genesys.cloud.messenger.transport.shyrka.receive.PresenceEvent
import com.genesys.cloud.messenger.transport.shyrka.receive.StructuredMessageEvent
import com.genesys.cloud.messenger.transport.shyrka.send.AutoStartRequest
import com.genesys.cloud.messenger.transport.shyrka.send.Channel
import com.genesys.cloud.messenger.transport.shyrka.send.EventMessage
import com.genesys.cloud.messenger.transport.shyrka.send.GetAttachmentRequest
import com.genesys.cloud.messenger.transport.shyrka.send.RequestAction
import com.genesys.cloud.messenger.transport.utility.AttachmentValues
import com.genesys.cloud.messenger.transport.utility.TestValues
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlin.test.Test

class RequestSerializationTest {

    @Test
    fun `validate AutoStartRequest serialization`() {
        val expectedPresenceEvent = PresenceEvent(
            eventType = StructuredMessageEvent.Type.Presence,
            presence = PresenceEvent.Presence(type = PresenceEvent.Presence.Type.Join),
        )
        val expectedEvents = listOf(expectedPresenceEvent)
        val expectedMessage = EventMessage(expectedEvents)
        val expectedRequest = AutoStartRequest(TestValues.Token, null)
        val expectedJson =
            """{"token":"<token>","action":"onMessage","message":{"events":[{"eventType":"Presence","presence":{"type":"Join"}}],"type":"Event"}}"""

        val encodedString = WebMessagingJson.json.encodeToString(expectedRequest)
        val decoded = WebMessagingJson.json.decodeFromString<AutoStartRequest>(expectedJson)

        assertThat(encodedString, "encoded AutoStartRequest").isEqualTo(expectedJson)
        decoded.run {
            assertThat(action).isEqualTo(RequestAction.ON_MESSAGE.value)
            assertThat(token).isEqualTo(TestValues.Token)
            assertThat(message).isEqualTo(expectedMessage)
            assertThat(message.events).containsExactly(*expectedEvents.toTypedArray())
        }
    }

    @Test
    fun `validate Channel serialization`() {
        val expectedMetadata = Channel.Metadata(mapOf("A" to "B"))
        val expectedRequest = Channel(expectedMetadata)
        val expectedJson = """{"metadata":{"customAttributes":{"A":"B"}}}"""

        val encodedString = WebMessagingJson.json.encodeToString(expectedRequest)
        val decoded = WebMessagingJson.json.decodeFromString<Channel>(expectedJson)

        assertThat(encodedString, "encoded Channel").isEqualTo(expectedJson)
        decoded.run {
            assertThat(this).isEqualTo(expectedRequest)
            assertThat(metadata).isEqualTo(expectedMetadata)
            assertThat(metadata.customAttributes["A"]).isEqualTo("B")
        }
    }

    @Test
    fun `validate GetAttachmentRequest serialization`() {
        val expectedRequest = GetAttachmentRequest(
            token = TestValues.Token,
            attachmentId = AttachmentValues.Id
        )
        val expectedJson =
            """{"token":"<token>","attachmentId":"test_attachment_id","action":"getAttachment"}"""

        val encodedString = WebMessagingJson.json.encodeToString(expectedRequest)
        val decoded = WebMessagingJson.json.decodeFromString<GetAttachmentRequest>(expectedJson)

        assertThat(encodedString, "encoded GetAttachmentRequest").isEqualTo(expectedJson)
        decoded.run {
            assertThat(action).isEqualTo(RequestAction.GET_ATTACHMENT.value)
            assertThat(token).isEqualTo(TestValues.Token)
            assertThat(attachmentId).isEqualTo(AttachmentValues.Id)
        }
    }
}
