package com.genesys.cloud.messenger.transport.network

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNull
import assertk.assertions.isTrue
import com.genesys.cloud.messenger.transport.shyrka.WebMessagingJson
import com.genesys.cloud.messenger.transport.shyrka.receive.PresenceEvent
import com.genesys.cloud.messenger.transport.shyrka.receive.StructuredMessageEvent
import com.genesys.cloud.messenger.transport.shyrka.receive.TypingEvent
import com.genesys.cloud.messenger.transport.shyrka.send.AutoStartRequest
import com.genesys.cloud.messenger.transport.shyrka.send.Channel
import com.genesys.cloud.messenger.transport.shyrka.send.ClearConversationRequest
import com.genesys.cloud.messenger.transport.shyrka.send.CloseSessionRequest
import com.genesys.cloud.messenger.transport.shyrka.send.ConfigureAuthenticatedSessionRequest
import com.genesys.cloud.messenger.transport.shyrka.send.ConfigureSessionRequest
import com.genesys.cloud.messenger.transport.shyrka.send.DeleteAttachmentRequest
import com.genesys.cloud.messenger.transport.shyrka.send.EchoRequest
import com.genesys.cloud.messenger.transport.shyrka.send.EventMessage
import com.genesys.cloud.messenger.transport.shyrka.send.GetAttachmentRequest
import com.genesys.cloud.messenger.transport.shyrka.send.HealthCheckID
import com.genesys.cloud.messenger.transport.shyrka.send.RequestAction
import com.genesys.cloud.messenger.transport.shyrka.send.TextMessage
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
        val expectedDataJson = """{"code":"jwt_Token"}"""

        val encodedString = WebMessagingJson.json.encodeToString(expectedRequest)
        val encodedDataString = WebMessagingJson.json.encodeToString(expectedData)
        val decoded = WebMessagingJson.json.decodeFromString<ConfigureAuthenticatedSessionRequest>(
            expectedJson
        )
        val decodedData =
            WebMessagingJson.json.decodeFromString<ConfigureAuthenticatedSessionRequest.Data>(
                expectedDataJson
            )

        assertThat(encodedString, "encoded ConfigureAuthenticatedSessionRequest").isEqualTo(
            expectedJson
        )
        assertThat(encodedDataString, "encoded Data").isEqualTo(expectedDataJson)
        decoded.run {
            assertThat(action).isEqualTo(RequestAction.CONFIGURE_AUTHENTICATED_SESSION.value)
            assertThat(token).isEqualTo(TestValues.Token)
            assertThat(deploymentId).isEqualTo(TestValues.DeploymentId)
            assertThat(startNew).isFalse()
            assertThat(journeyContext).isNull()
        }
        assertThat(decodedData).isEqualTo(expectedData)
        assertThat(decodedData.code).isEqualTo(AuthTest.JwtToken)
    }

    @Test
    fun `validate ConfigureSessionRequest serialization`() {
        val expectedRequest = ConfigureSessionRequest(
            token = TestValues.Token,
            deploymentId = TestValues.DeploymentId,
            startNew = true,
        )
        val expectedJson =
            """{"token":"<token>","deploymentId":"deploymentId","startNew":true,"action":"configureSession"}"""

        val encodedString = WebMessagingJson.json.encodeToString(expectedRequest)
        val decoded = WebMessagingJson.json.decodeFromString<ConfigureSessionRequest>(expectedJson)

        assertThat(encodedString, "encoded ConfigureSessionRequest").isEqualTo(expectedJson)
        decoded.run {
            assertThat(action).isEqualTo(RequestAction.CONFIGURE_SESSION.value)
            assertThat(token).isEqualTo(TestValues.Token)
            assertThat(deploymentId).isEqualTo(TestValues.DeploymentId)
            assertThat(startNew).isTrue()
            assertThat(journeyContext).isNull()
        }
    }

    @Test
    fun `validate DeleteAttachmentRequest serialization`() {
        val expectedRequest = DeleteAttachmentRequest(
            token = TestValues.Token,
            attachmentId = AttachmentValues.Id
        )
        val expectedJson =
            """{"token":"<token>","attachmentId":"test_attachment_id","action":"deleteAttachment"}"""

        val encodedString = WebMessagingJson.json.encodeToString(expectedRequest)
        val decoded = WebMessagingJson.json.decodeFromString<DeleteAttachmentRequest>(expectedJson)

        assertThat(encodedString, "encoded DeleteAttachmentRequest").isEqualTo(expectedJson)
        decoded.run {
            assertThat(action).isEqualTo(RequestAction.DELETE_ATTACHMENT.value)
            assertThat(token).isEqualTo(TestValues.Token)
            assertThat(attachmentId).isEqualTo(AttachmentValues.Id)
        }
    }

    @Test
    fun `validate EchoRequest serialization`() {
        val expectedTextMessage = TextMessage("ping", mapOf("customMessageId" to HealthCheckID))
        val expectedRequest = EchoRequest(
            token = TestValues.Token,
        )
        val expectedJson =
            """{"token":"<token>","action":"echo","message":{"text":"ping","metadata":{"customMessageId":"SGVhbHRoQ2hlY2tNZXNzYWdlSWQ="},"type":"Text"}}"""

        val encodedString = WebMessagingJson.json.encodeToString(expectedRequest)
        val decoded = WebMessagingJson.json.decodeFromString<EchoRequest>(expectedJson)

        assertThat(encodedString, "encoded EchoRequest").isEqualTo(expectedJson)
        decoded.run {
            assertThat(action).isEqualTo(RequestAction.ECHO_MESSAGE.value)
            assertThat(token).isEqualTo(TestValues.Token)
            assertThat(message).isEqualTo(expectedTextMessage)
            message.run {
                assertThat(text).isEqualTo(expectedTextMessage.text)
                assertThat(metadata?.get("customMessageId")).isEqualTo(HealthCheckID)
            }
        }
    }

    @Test
    fun `validate EventMessage serialization`() {
        val expectedEvent = TypingEvent(
            eventType = StructuredMessageEvent.Type.Typing,
            typing = TypingEvent.Typing(type = "typing", duration = 100)
        )
        val expectedEvents = listOf(expectedEvent)
        val expectedRequest = EventMessage(events = expectedEvents)
        val expectedJson =
            """{"events":[{"eventType":"Typing","typing":{"type":"typing","duration":100}}],"type":"Event"}"""

        val encodedString = WebMessagingJson.json.encodeToString(expectedRequest)
        val decoded = WebMessagingJson.json.decodeFromString<EventMessage>(expectedJson)

        assertThat(encodedString, "encoded EventMessage").isEqualTo(expectedJson)
        decoded.run {
            assertThat(this).isEqualTo(expectedRequest)
            assertThat(channel).isNull()
            assertThat(events).containsExactly(*expectedEvents.toTypedArray())
                assertThat((events[0] as TypingEvent).eventType).isEqualTo(StructuredMessageEvent.Type.Typing)
                assertThat((events[0] as TypingEvent).typing.type).isEqualTo("typing")
                assertThat((events[0] as TypingEvent).typing.duration).isEqualTo(100)
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
