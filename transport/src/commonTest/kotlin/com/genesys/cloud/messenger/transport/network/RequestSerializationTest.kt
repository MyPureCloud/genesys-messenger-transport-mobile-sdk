package com.genesys.cloud.messenger.transport.network

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNull
import assertk.assertions.isTrue
import com.genesys.cloud.messenger.transport.push.DeviceTokenRequestBody
import com.genesys.cloud.messenger.transport.shyrka.WebMessagingJson
import com.genesys.cloud.messenger.transport.shyrka.receive.PresenceEvent
import com.genesys.cloud.messenger.transport.shyrka.receive.PresenceEvent.Presence
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
import com.genesys.cloud.messenger.transport.shyrka.send.JourneyAction
import com.genesys.cloud.messenger.transport.shyrka.send.JourneyActionMap
import com.genesys.cloud.messenger.transport.shyrka.send.JourneyContext
import com.genesys.cloud.messenger.transport.shyrka.send.JourneyCustomer
import com.genesys.cloud.messenger.transport.shyrka.send.JourneyCustomerSession
import com.genesys.cloud.messenger.transport.shyrka.send.JwtRequest
import com.genesys.cloud.messenger.transport.shyrka.send.OAuth
import com.genesys.cloud.messenger.transport.shyrka.send.OnAttachmentRequest
import com.genesys.cloud.messenger.transport.shyrka.send.RequestAction
import com.genesys.cloud.messenger.transport.shyrka.send.TextMessage
import com.genesys.cloud.messenger.transport.shyrka.send.UserTypingRequest
import com.genesys.cloud.messenger.transport.utility.AttachmentValues
import com.genesys.cloud.messenger.transport.utility.AuthTest
import com.genesys.cloud.messenger.transport.utility.Journey
import com.genesys.cloud.messenger.transport.utility.TestValues
import kotlinx.serialization.encodeToString
import kotlin.test.Test

class RequestSerializationTest {
    @Test
    fun `validate AutoStartRequest serialization`() {
        val expectedPresenceEvent =
            PresenceEvent(
                eventType = StructuredMessageEvent.Type.Presence,
                presence = Presence(type = Presence.Type.Join),
            )
        val expectedEvents = listOf(expectedPresenceEvent)
        val expectedMessage = EventMessage(expectedEvents)
        val expectedRequest = AutoStartRequest(TestValues.TOKEN, null)
        val expectedJson =
            """{"token":"token","action":"onMessage","message":{"events":[{"eventType":"Presence","presence":{"type":"Join"}}],"type":"Event"}}"""

        val encodedString = WebMessagingJson.json.encodeToString(expectedRequest)
        val decoded = WebMessagingJson.json.decodeFromString<AutoStartRequest>(expectedJson)

        assertThat(encodedString, "encoded AutoStartRequest").isEqualTo(expectedJson)
        decoded.run {
            assertThat(action).isEqualTo(RequestAction.ON_MESSAGE.value)
            assertThat(token).isEqualTo(TestValues.TOKEN)
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
        val expectedPresenceEvent =
            PresenceEvent(
                eventType = StructuredMessageEvent.Type.Presence,
                presence = Presence(type = Presence.Type.Clear),
            )
        val expectedEvents = listOf(expectedPresenceEvent)
        val expectedMessage = EventMessage(expectedEvents)
        val expectedRequest = ClearConversationRequest(TestValues.TOKEN)
        val expectedJson =
            """{"token":"token","action":"onMessage","message":{"events":[{"eventType":"Presence","presence":{"type":"Clear"}}],"type":"Event"}}"""
        val expectedPresenceJson = """{"type":"Clear"}"""

        val encodedString = WebMessagingJson.json.encodeToString(expectedRequest)
        val encodedPresenceString = WebMessagingJson.json.encodeToString(Presence(Presence.Type.Clear))
        val decoded = WebMessagingJson.json.decodeFromString<ClearConversationRequest>(expectedJson)
        val decodedPresence = WebMessagingJson.json.decodeFromString<Presence>(expectedPresenceJson)

        assertThat(encodedString, "encoded ClearConversationRequest").isEqualTo(expectedJson)
        assertThat(encodedPresenceString, "encoded Presence").isEqualTo(expectedPresenceJson)
        decoded.run {
            assertThat(action).isEqualTo(RequestAction.ON_MESSAGE.value)
            assertThat(token).isEqualTo(TestValues.TOKEN)
            assertThat(message).isEqualTo(expectedMessage)
            assertThat(message.events).containsExactly(*expectedEvents.toTypedArray())
        }
        assertThat(decodedPresence.type).isEqualTo(Presence.Type.Clear)
    }

    @Test
    fun `validate CloseSessionRequest serialization`() {
        val expectedRequest =
            CloseSessionRequest(
                token = TestValues.TOKEN,
                closeAllConnections = true,
            )
        val expectedJson =
            """{"token":"token","closeAllConnections":true,"action":"closeSession"}"""

        val encodedString = WebMessagingJson.json.encodeToString(expectedRequest)
        val decoded = WebMessagingJson.json.decodeFromString<CloseSessionRequest>(expectedJson)

        assertThat(encodedString, "encoded CloseSessionRequest").isEqualTo(expectedJson)
        decoded.run {
            assertThat(action).isEqualTo(RequestAction.CLOSE_SESSION.value)
            assertThat(token).isEqualTo(TestValues.TOKEN)
            assertThat(closeAllConnections).isTrue()
        }
    }

    @Test
    fun `validate ConfigureAuthenticatedSessionRequest serialization`() {
        val expectedData = ConfigureAuthenticatedSessionRequest.Data(AuthTest.JWT_TOKEN)
        val expectedRequest =
            ConfigureAuthenticatedSessionRequest(
                token = TestValues.TOKEN,
                deploymentId = TestValues.DEPLOYMENT_ID,
                startNew = false,
                data = expectedData
            )
        val expectedJson =
            """{"token":"token","deploymentId":"deploymentId","startNew":false,"data":{"code":"jwt_Token"},"action":"configureAuthenticatedSession"}"""
        val expectedDataJson = """{"code":"jwt_Token"}"""

        val encodedString = WebMessagingJson.json.encodeToString(expectedRequest)
        val encodedDataString = WebMessagingJson.json.encodeToString(expectedData)
        val decoded =
            WebMessagingJson.json.decodeFromString<ConfigureAuthenticatedSessionRequest>(
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
            assertThat(token).isEqualTo(TestValues.TOKEN)
            assertThat(deploymentId).isEqualTo(TestValues.DEPLOYMENT_ID)
            assertThat(startNew).isFalse()
            assertThat(journeyContext).isNull()
            assertThat(data).isEqualTo(expectedData)
        }
        assertThat(decodedData).isEqualTo(expectedData)
        assertThat(decodedData.code).isEqualTo(AuthTest.JWT_TOKEN)
    }

    @Test
    fun `validate ConfigureSessionRequest serialization`() {
        val expectedRequest =
            ConfigureSessionRequest(
                token = TestValues.TOKEN,
                deploymentId = TestValues.DEPLOYMENT_ID,
                startNew = true,
            )
        val expectedJson =
            """{"token":"token","deploymentId":"deploymentId","startNew":true,"action":"configureSession"}"""

        val encodedString = WebMessagingJson.json.encodeToString(expectedRequest)
        val decoded = WebMessagingJson.json.decodeFromString<ConfigureSessionRequest>(expectedJson)

        assertThat(encodedString, "encoded ConfigureSessionRequest").isEqualTo(expectedJson)
        decoded.run {
            assertThat(action).isEqualTo(RequestAction.CONFIGURE_SESSION.value)
            assertThat(token).isEqualTo(TestValues.TOKEN)
            assertThat(deploymentId).isEqualTo(TestValues.DEPLOYMENT_ID)
            assertThat(startNew).isTrue()
            assertThat(journeyContext).isNull()
        }
    }

    @Test
    fun `validate DeleteAttachmentRequest serialization`() {
        val expectedRequest =
            DeleteAttachmentRequest(
                token = TestValues.TOKEN,
                attachmentId = AttachmentValues.ID
            )
        val expectedJson =
            """{"token":"token","attachmentId":"test_attachment_id","action":"deleteAttachment"}"""

        val encodedString = WebMessagingJson.json.encodeToString(expectedRequest)
        val decoded = WebMessagingJson.json.decodeFromString<DeleteAttachmentRequest>(expectedJson)

        assertThat(encodedString, "encoded DeleteAttachmentRequest").isEqualTo(expectedJson)
        decoded.run {
            assertThat(action).isEqualTo(RequestAction.DELETE_ATTACHMENT.value)
            assertThat(token).isEqualTo(TestValues.TOKEN)
            assertThat(attachmentId).isEqualTo(AttachmentValues.ID)
        }
    }

    @Test
    fun `validate EchoRequest serialization`() {
        val expectedTextMessage = TextMessage("ping", mapOf("customMessageId" to HealthCheckID))
        val expectedRequest =
            EchoRequest(
                token = TestValues.TOKEN,
            )
        val expectedJson =
            """{"token":"token","action":"echo","message":{"text":"ping","metadata":{"customMessageId":"SGVhbHRoQ2hlY2tNZXNzYWdlSWQ="},"type":"Text"}}"""

        val encodedString = WebMessagingJson.json.encodeToString(expectedRequest)
        val decoded = WebMessagingJson.json.decodeFromString<EchoRequest>(expectedJson)

        assertThat(encodedString, "encoded EchoRequest").isEqualTo(expectedJson)
        decoded.run {
            assertThat(action).isEqualTo(RequestAction.ECHO_MESSAGE.value)
            assertThat(token).isEqualTo(TestValues.TOKEN)
            assertThat(message).isEqualTo(expectedTextMessage)
            message.run {
                assertThat(text).isEqualTo(expectedTextMessage.text)
                assertThat(metadata?.get("customMessageId")).isEqualTo(HealthCheckID)
            }
        }
    }

    @Test
    fun `validate EventMessage serialization`() {
        val expectedEvent =
            TypingEvent(
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
        val expectedRequest =
            GetAttachmentRequest(
                token = TestValues.TOKEN,
                attachmentId = AttachmentValues.ID
            )
        val expectedJson =
            """{"token":"token","attachmentId":"test_attachment_id","action":"getAttachment"}"""

        val encodedString = WebMessagingJson.json.encodeToString(expectedRequest)
        val decoded = WebMessagingJson.json.decodeFromString<GetAttachmentRequest>(expectedJson)

        assertThat(encodedString, "encoded GetAttachmentRequest").isEqualTo(expectedJson)
        decoded.run {
            assertThat(action).isEqualTo(RequestAction.GET_ATTACHMENT.value)
            assertThat(token).isEqualTo(TestValues.TOKEN)
            assertThat(attachmentId).isEqualTo(AttachmentValues.ID)
        }
    }

    @Test
    fun `validate JourneyContext serialization`() {
        val expectedJourneyCustomer =
            JourneyCustomer(id = Journey.CUSTOMER_ID, idType = Journey.CUSTOMER_ID_TYPE)
        val expectedCustomerSession =
            JourneyCustomerSession(
                id = Journey.CUSTOMER_SESSION_ID,
                type = Journey.CUSTOMER_SESSION_TYPE
            )
        val expectedJourneyActionMap =
            JourneyActionMap(id = Journey.ACTION_MAP_ID, version = Journey.ACTION_MAP_VERSION)
        val expectedTriggeringAction =
            JourneyAction(id = Journey.ACTION_ID, actionMap = expectedJourneyActionMap)
        val expectedJourneyContext =
            JourneyContext(
                customer = expectedJourneyCustomer,
                customerSession = expectedCustomerSession,
                triggeringAction = expectedTriggeringAction,
            )
        val expectedJourneyCustomerJson =
            """{"id":"customer_id","idType":"customer_id_type"}"""
        val expectedCustomerSessionJson =
            """{"id":"customer_session_id","type":"customer_session_type"}"""
        val expectedJourneyActionMapJson =
            """{"id":"action_map_id","version":1.0}"""
        val expectedTriggeringActionJson =
            """{"id":"action_id","actionMap":{"id":"action_map_id","version":1.0}}"""
        val expectedJourneyContextJson =
            """{"customer":{"id":"customer_id","idType":"customer_id_type"},"customerSession":{"id":"customer_session_id","type":"customer_session_type"},"triggeringAction":{"id":"action_id","actionMap":{"id":"action_map_id","version":1.0}}}"""

        val encodedJourneyCustomerString =
            WebMessagingJson.json.encodeToString(expectedJourneyCustomer)
        val encodedCustomerSessionString =
            WebMessagingJson.json.encodeToString(expectedCustomerSession)
        val encodedJourneyActionMapString =
            WebMessagingJson.json.encodeToString(expectedJourneyActionMap)
        val encodedJourneyActionString =
            WebMessagingJson.json.encodeToString(expectedTriggeringAction)
        val encodedJourneyContextString =
            WebMessagingJson.json.encodeToString(expectedJourneyContext)
        val decodedJourneyCustomer =
            WebMessagingJson.json.decodeFromString<JourneyCustomer>(expectedJourneyCustomerJson)
        val decodedJourneyCustomerSession =
            WebMessagingJson.json.decodeFromString<JourneyCustomerSession>(
                expectedCustomerSessionJson
            )
        val decodedJourneyActionMap =
            WebMessagingJson.json.decodeFromString<JourneyActionMap>(expectedJourneyActionMapJson)
        val decodedJourneyAction =
            WebMessagingJson.json.decodeFromString<JourneyAction>(expectedTriggeringActionJson)
        val decodedJourneyContext =
            WebMessagingJson.json.decodeFromString<JourneyContext>(expectedJourneyContextJson)

        assertThat(encodedJourneyCustomerString, "encoded JourneyCustomer").isEqualTo(
            expectedJourneyCustomerJson
        )
        assertThat(encodedCustomerSessionString, "encoded JourneyCustomerSession").isEqualTo(
            expectedCustomerSessionJson
        )
        assertThat(encodedJourneyActionMapString, "encoded JourneyActionMap").isEqualTo(
            expectedJourneyActionMapJson
        )
        assertThat(encodedJourneyActionString, "encoded JourneyAction").isEqualTo(
            expectedTriggeringActionJson
        )
        assertThat(encodedJourneyContextString, "encoded JourneyContext").isEqualTo(
            expectedJourneyContextJson
        )
        decodedJourneyCustomer.run {
            assertThat(id).isEqualTo(Journey.CUSTOMER_ID)
            assertThat(idType).isEqualTo(Journey.CUSTOMER_ID_TYPE)
        }
        decodedJourneyCustomerSession.run {
            assertThat(id).isEqualTo(Journey.CUSTOMER_SESSION_ID)
            assertThat(type).isEqualTo(Journey.CUSTOMER_SESSION_TYPE)
        }
        decodedJourneyActionMap.run {
            assertThat(id).isEqualTo(Journey.ACTION_MAP_ID)
            assertThat(version).isEqualTo(Journey.ACTION_MAP_VERSION)
        }
        decodedJourneyAction.run {
            assertThat(id).isEqualTo(Journey.ACTION_ID)
            assertThat(actionMap).isEqualTo(expectedJourneyActionMap)
        }
        decodedJourneyContext.run {
            assertThat(customer).isEqualTo(expectedJourneyCustomer)
            assertThat(customerSession).isEqualTo(expectedCustomerSession)
            assertThat(triggeringAction).isEqualTo(expectedTriggeringAction)
        }
    }

    @Test
    fun `validate JwtRequest serialization`() {
        val expectedRequest = JwtRequest(token = TestValues.TOKEN)
        val expectedJson = """{"token":"${TestValues.TOKEN}","action":"getJwt"}"""

        val encodedString = WebMessagingJson.json.encodeToString(expectedRequest)
        val decoded = WebMessagingJson.json.decodeFromString<JwtRequest>(expectedJson)

        assertThat(encodedString, "encoded JwtRequest").isEqualTo(expectedJson)
        decoded.run {
            assertThat(action).isEqualTo(RequestAction.GET_JWT.value)
            assertThat(token).isEqualTo(TestValues.TOKEN)
        }
    }

    @Test
    fun `validate OAuth serialization`() {
        val expectedRequest =
            OAuth(
                code = AuthTest.AUTH_CODE,
                redirectUri = AuthTest.REDIRECT_URI,
                codeVerifier = AuthTest.CODE_VERIFIER,
            )
        val expectedJson =
            """{"code":"${AuthTest.AUTH_CODE}","redirectUri":"${AuthTest.REDIRECT_URI}","codeVerifier":"${AuthTest.CODE_VERIFIER}"}"""

        val encodedString = WebMessagingJson.json.encodeToString(expectedRequest)
        val decoded = WebMessagingJson.json.decodeFromString<OAuth>(expectedJson)

        assertThat(encodedString, "encoded OAuth").isEqualTo(expectedJson)
        decoded.run {
            assertThat(code).isEqualTo(AuthTest.AUTH_CODE)
            assertThat(redirectUri).isEqualTo(AuthTest.REDIRECT_URI)
            assertThat(codeVerifier).isEqualTo(AuthTest.CODE_VERIFIER)
        }
    }

    @Test
    fun `validate OnAttachmentRequest serialization`() {
        val expectedRequest =
            OnAttachmentRequest(
                token = TestValues.TOKEN,
                attachmentId = AttachmentValues.ID,
                fileName = AttachmentValues.FILE_NAME,
                fileType = AttachmentValues.FILE_TYPE,
                fileSize = AttachmentValues.FILE_SIZE,
                fileMd5 = AttachmentValues.FILE_MD5,
                errorsAsJson = false,
            )
        val expectedJson =
            """{"token":"${TestValues.TOKEN}","attachmentId":"${AttachmentValues.ID}","fileName":"${AttachmentValues.FILE_NAME}","fileType":"${AttachmentValues.FILE_TYPE}","fileSize":${AttachmentValues.FILE_SIZE},"fileMd5":"${AttachmentValues.FILE_MD5}","errorsAsJson":false,"action":"onAttachment"}"""

        val encodedString = WebMessagingJson.json.encodeToString(expectedRequest)
        val decoded = WebMessagingJson.json.decodeFromString<OnAttachmentRequest>(expectedJson)

        assertThat(encodedString, "encoded OnAttachmentRequest").isEqualTo(expectedJson)
        decoded.run {
            assertThat(token).isEqualTo(TestValues.TOKEN)
            assertThat(action).isEqualTo(RequestAction.ON_ATTACHMENT.value)
            assertThat(fileName).isEqualTo(AttachmentValues.FILE_NAME)
            assertThat(fileType).isEqualTo(AttachmentValues.FILE_TYPE)
            assertThat(fileSize).isEqualTo(AttachmentValues.FILE_SIZE)
            assertThat(fileMd5).isEqualTo(AttachmentValues.FILE_MD5)
            assertThat(errorsAsJson).isFalse()
        }
    }

    @Test
    fun `validate UserTypingRequest serialization`() {
        val expectedEvent =
            TypingEvent(
                eventType = StructuredMessageEvent.Type.Typing,
                typing = TypingEvent.Typing(type = "On")
            )
        val expectedEventList = listOf(expectedEvent)
        val expectedMessage = EventMessage(expectedEventList)
        val expectedRequest = UserTypingRequest(token = TestValues.TOKEN)
        val expectedJson =
            """{"token":"token","action":"onMessage","message":{"events":[{"eventType":"Typing","typing":{"type":"On"}}],"type":"Event"}}"""

        val encodedString = WebMessagingJson.json.encodeToString(expectedRequest)
        val decoded = WebMessagingJson.json.decodeFromString<UserTypingRequest>(expectedJson)

        assertThat(encodedString, "encoded UserTypingRequest").isEqualTo(expectedJson)
        decoded.run {
            assertThat(token).isEqualTo(TestValues.TOKEN)
            assertThat(action).isEqualTo(RequestAction.ON_MESSAGE.value)
            assertThat(message).isEqualTo(expectedMessage)
            message.run {
                assertThat(events).containsExactly(*expectedEventList.toTypedArray())
            }
        }
    }

    @Test
    fun `validate DeviceTokenRequestBody serialization`() {
        val expectedRequest =
            DeviceTokenRequestBody(
                deviceToken = TestValues.DEVICE_TOKEN,
                notificationProvider = TestValues.PUSH_PROVIDER,
                language = TestValues.PREFERRED_LANGUAGE,
                deviceType = TestValues.DEVICE_TYPE
            )
        val expectedJson =
            """{"deviceToken":"${TestValues.DEVICE_TOKEN}","language":"${TestValues.PREFERRED_LANGUAGE}","deviceType":"${TestValues.DEVICE_TYPE}","notificationProvider":"${TestValues.PUSH_PROVIDER}"}"""

        val encodedString = WebMessagingJson.json.encodeToString(expectedRequest)
        val decoded = WebMessagingJson.json.decodeFromString<DeviceTokenRequestBody>(expectedJson)

        assertThat(encodedString, "encoded RegisterDeviceTokenRequestBody").isEqualTo(expectedJson)
        decoded.run {
            assertThat(deviceToken).isEqualTo(TestValues.DEVICE_TOKEN)
            assertThat(notificationProvider).isEqualTo(TestValues.PUSH_PROVIDER)
            assertThat(language).isEqualTo(TestValues.PREFERRED_LANGUAGE)
            assertThat(deviceType).isEqualTo(TestValues.DEVICE_TYPE)
        }
    }
}
