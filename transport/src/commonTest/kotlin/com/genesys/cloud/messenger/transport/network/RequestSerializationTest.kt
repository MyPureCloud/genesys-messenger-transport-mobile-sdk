package com.genesys.cloud.messenger.transport.network

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import com.genesys.cloud.messenger.transport.shyrka.WebMessagingJson
import com.genesys.cloud.messenger.transport.shyrka.receive.PresenceEvent
import com.genesys.cloud.messenger.transport.shyrka.receive.StructuredMessageEvent
import com.genesys.cloud.messenger.transport.shyrka.send.AutoStartRequest
import com.genesys.cloud.messenger.transport.shyrka.send.Channel
import com.genesys.cloud.messenger.transport.shyrka.send.ClearConversationRequest
import com.genesys.cloud.messenger.transport.shyrka.send.CloseSessionRequest
import com.genesys.cloud.messenger.transport.shyrka.send.ConfigureAuthenticatedSessionRequest
import com.genesys.cloud.messenger.transport.shyrka.send.EventMessage
import com.genesys.cloud.messenger.transport.shyrka.send.GetAttachmentRequest
import com.genesys.cloud.messenger.transport.shyrka.send.RequestAction
import com.genesys.cloud.messenger.transport.utility.AttachmentValues
import com.genesys.cloud.messenger.transport.utility.AuthTest
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
    fun `validate ClearConversationRequest serialization`() {
        val expectedPresenceEvent = PresenceEvent(
            eventType = StructuredMessageEvent.Type.Presence,
            presence = PresenceEvent.Presence(type = PresenceEvent.Presence.Type.Clear),
        )
        val expectedEvents = listOf(expectedPresenceEvent)
        val expectedMessage = EventMessage(expectedEvents)
        val expectedRequest = ClearConversationRequest(TestValues.Token)
        val expectedJson =
            """{"token":"<token>","action":"onMessage","message":{"events":[{"eventType":"Presence","presence":{"type":"Clear"}}],"type":"Event"}}"""

        val encodedString = WebMessagingJson.json.encodeToString(expectedRequest)
        val decoded = WebMessagingJson.json.decodeFromString<ClearConversationRequest>(expectedJson)

        assertThat(encodedString, "encoded ClearConversationRequest").isEqualTo(expectedJson)
        decoded.run {
            assertThat(action).isEqualTo(RequestAction.ON_MESSAGE.value)
            assertThat(token).isEqualTo(TestValues.Token)
            assertThat(message).isEqualTo(expectedMessage)
            assertThat(message.events).containsExactly(*expectedEvents.toTypedArray())
        }
    }

    @Test
    fun `validate CloseSessionRequest serialization`() {
        val expectedRequest = CloseSessionRequest(
            token = TestValues.Token,
            closeAllConnections = true,
        )
        val expectedJson =
            """{"token":"<token>","closeAllConnections":true,"action":"closeSession"}"""

        val encodedString = WebMessagingJson.json.encodeToString(expectedRequest)
        val decoded = WebMessagingJson.json.decodeFromString<CloseSessionRequest>(expectedJson)

        assertThat(encodedString, "encoded CloseSessionRequest").isEqualTo(expectedJson)
        decoded.run {
            assertThat(action).isEqualTo(RequestAction.CLOSE_SESSION.value)
            assertThat(token).isEqualTo(TestValues.Token)
            assertThat(closeAllConnections).isTrue()
        }
    }

    @Test
    fun `validate ConfigureAuthenticatedSessionRequest serialization`() {
        val expectedData = ConfigureAuthenticatedSessionRequest.Data(AuthTest.JwtToken)
        val expectedRequest = ConfigureAuthenticatedSessionRequest(
            token = TestValues.Token,
            deploymentId = TestValues.DeploymentId,
            startNew = false,
            data = expectedData
        )
        val expectedJson =
            """{"token":"<token>","deploymentId":"deploymentId","startNew":false,"data":{"code":"jwt_Token"},"action":"configureAuthenticatedSession"}"""

        val encodedString = WebMessagingJson.json.encodeToString(expectedRequest)
        val decoded = WebMessagingJson.json.decodeFromString<ConfigureAuthenticatedSessionRequest>(expectedJson)

        assertThat(encodedString, "encoded ConfigureAuthenticatedSessionRequest").isEqualTo(expectedJson)
        decoded.run {
            assertThat(action).isEqualTo(RequestAction.CONFIGURE_AUTHENTICATED_SESSION.value)
            assertThat(token).isEqualTo(TestValues.Token)
            assertThat(deploymentId).isEqualTo(TestValues.DeploymentId)
            assertThat(startNew).isFalse()
            assertThat(data).isEqualTo(expectedData)
            assertThat(data.code).isEqualTo(AuthTest.JwtToken)
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
