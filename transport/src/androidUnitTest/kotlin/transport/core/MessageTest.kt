package transport.core

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNull
import assertk.assertions.isTrue
import com.genesys.cloud.messenger.transport.core.Attachment
import com.genesys.cloud.messenger.transport.core.Message
import com.genesys.cloud.messenger.transport.core.Message.Direction
import com.genesys.cloud.messenger.transport.core.Message.Participant
import com.genesys.cloud.messenger.transport.core.Message.State
import com.genesys.cloud.messenger.transport.core.events.Event
import com.genesys.cloud.messenger.transport.shyrka.WebMessagingJson
import com.genesys.cloud.messenger.transport.utility.AttachmentValues
import com.genesys.cloud.messenger.transport.utility.CardTestValues
import com.genesys.cloud.messenger.transport.utility.MessageValues
import com.genesys.cloud.messenger.transport.utility.QuickReplyTestValues
import org.junit.Test

class MessageTest {

    @Test
    fun `validate default constructor`() {
        val expectedDirection = Direction.Inbound
        val expectedState = State.Idle
        val expectedType = MessageValues.TYPE
        val expectedMessageType = Message.Type.Text
        val expectedParticipant =
            Participant(originatingEntity = Participant.OriginatingEntity.Human)

        val subject = Message()

        subject.run {
            assertThat(direction).isEqualTo(expectedDirection)
            assertThat(state).isEqualTo(expectedState)
            assertThat(type).isEqualTo(expectedType)
            assertThat(messageType).isEqualTo(expectedMessageType)
            assertThat(text).isNull()
            assertThat(timeStamp).isNull()
            assertThat(attachments).isEmpty()
            assertThat(events).isEmpty()
            assertThat(quickReplies).isEmpty()
            assertThat(cards).isEmpty()
            from.run {
                assertThat(this).isEqualTo(expectedParticipant)
                assertThat(name).isNull()
                assertThat(imageUrl).isNull()
                assertThat(originatingEntity).isEqualTo(Participant.OriginatingEntity.Human)
            }
            assertThat(from).isEqualTo(expectedParticipant)
            assertThat(authenticated).isFalse()
        }
    }

    @Test
    fun `validate custom constructor`() {
        val expectedId = MessageValues.ID
        val expectedDirection = Direction.Outbound
        val expectedState = State.Sending
        val expectedType = "QuickReply"
        val expectedMessageType = Message.Type.QuickReply
        val expectedText = MessageValues.TEXT
        val expectedTimestamp = MessageValues.TIME_STAMP
        val expectedAttachments = mapOf(AttachmentValues.ID to Attachment(AttachmentValues.ID))
        val expectedEvents = listOf(Event.ConversationAutostart)
        val expectedQuickReplies = listOf(QuickReplyTestValues.buttonResponse_a)
        val expectedCards = listOf(CardTestValues.card)
        val expectedParticipant = Participant(
            name = MessageValues.PARTICIPANT_NAME,
            imageUrl = MessageValues.PARTICIPANT_IMAGE_URL,
            originatingEntity = Participant.OriginatingEntity.Bot
        )

        val subject = Message(
            id = MessageValues.ID,
            direction = Direction.Outbound,
            state = State.Sending,
            type = "QuickReply",
            messageType = Message.Type.QuickReply,
            text = MessageValues.TEXT,
            timeStamp = MessageValues.TIME_STAMP,
            attachments = mapOf(AttachmentValues.ID to Attachment(AttachmentValues.ID)),
            events = listOf(Event.ConversationAutostart),
            quickReplies = listOf(QuickReplyTestValues.buttonResponse_a),
            cards = listOf(CardTestValues.card),
            from = Participant(
                name = MessageValues.PARTICIPANT_NAME,
                imageUrl = MessageValues.PARTICIPANT_IMAGE_URL,
                originatingEntity = Participant.OriginatingEntity.Bot,
            ),
            authenticated = true
        )

        subject.run {
            assertThat(id).isEqualTo(expectedId)
            assertThat(direction).isEqualTo(expectedDirection)
            assertThat(state).isEqualTo(expectedState)
            assertThat(type).isEqualTo(expectedType)
            assertThat(messageType).isEqualTo(expectedMessageType)
            assertThat(text).isEqualTo(expectedText)
            assertThat(timeStamp).isEqualTo(expectedTimestamp)
            assertThat(attachments).isEqualTo(expectedAttachments)
            assertThat(events).containsExactly(*expectedEvents.toTypedArray())
            assertThat(quickReplies).containsExactly(*expectedQuickReplies.toTypedArray())
            assertThat(cards).containsExactly(*expectedCards.toTypedArray())
            assertThat(from).isEqualTo(expectedParticipant)
            assertThat(authenticated).isTrue()
        }
    }

    @Test
    fun `validate Content with Attachment serialization`() {
        val expectedAttachment = Attachment(
            id = AttachmentValues.ID,
            fileName = null,
            state = Attachment.State.Presigning
        )
        val expectedRequest = Message.Content(
            contentType = Message.Content.Type.Attachment,
            attachment = Attachment(AttachmentValues.ID)
        )
        val expectedJson =
            """{"contentType":"Attachment","attachment":{"id":"test_attachment_id"}}"""

        val encodedString = WebMessagingJson.json.encodeToString(expectedRequest)
        val decoded = WebMessagingJson.json.decodeFromString<Message.Content>(expectedJson)

        assertThat(encodedString, "encoded Content").isEqualTo(expectedJson)
        decoded.run {
            assertThat(this).isEqualTo(expectedRequest)
            assertThat(attachment).isEqualTo(expectedAttachment)
            assertThat(contentType).isEqualTo(Message.Content.Type.Attachment)
        }
    }

    @Test
    fun `validate Content with ButtonResponse serialization`() {
        val expectedButtonResponse = QuickReplyTestValues.buttonResponse_a
        val expectedRequest = Message.Content(
            contentType = Message.Content.Type.ButtonResponse,
            buttonResponse = QuickReplyTestValues.buttonResponse_a
        )
        val expectedJson =
            """{"contentType":"ButtonResponse","buttonResponse":{"text":"text_a","payload":"payload_a","type":"QuickReply"}}"""

        val encodedString = WebMessagingJson.json.encodeToString(expectedRequest)
        val decoded = WebMessagingJson.json.decodeFromString<Message.Content>(expectedJson)

        assertThat(encodedString, "encoded Content with ButtonResponse").isEqualTo(expectedJson)
        decoded.run {
            assertThat(this).isEqualTo(expectedRequest)
            assertThat(buttonResponse).isEqualTo(expectedButtonResponse)
            assertThat(contentType).isEqualTo(Message.Content.Type.ButtonResponse)
        }
    }

    @Test
    fun `validate Direction serialization`() {
        val expectedRequest = Direction.Inbound
        val expectedJson = """"Inbound""""

        val encodedString = WebMessagingJson.json.encodeToString(expectedRequest)
        val decoded = WebMessagingJson.json.decodeFromString<Direction>(expectedJson)

        assertThat(encodedString, "encoded Direction").isEqualTo(expectedJson)
        assertThat(decoded).isEqualTo(expectedRequest)
    }

    @Test
    fun `validate MessageType serialization`() {
        val expectedRequest = Message.Type.Text
        val expectedJson = """"Text""""

        val encodedString = WebMessagingJson.json.encodeToString(expectedRequest)
        val decoded = WebMessagingJson.json.decodeFromString<Message.Type>(expectedJson)

        assertThat(encodedString, "encoded Message.Type").isEqualTo(expectedJson)
        assertThat(decoded).isEqualTo(expectedRequest)
    }

    @Test
    fun `validate Participant serialization`() {
        val expectedRequest = Participant(
            name = MessageValues.PARTICIPANT_NAME,
            imageUrl = MessageValues.PARTICIPANT_IMAGE_URL,
            originatingEntity = Participant.OriginatingEntity.Human
        )
        val expectedJson =
            """{"name":"participant_name","imageUrl":"http://participant.image","originatingEntity":"Human"}"""

        val encodedString = WebMessagingJson.json.encodeToString(expectedRequest)
        val decoded = WebMessagingJson.json.decodeFromString<Participant>(expectedJson)

        assertThat(encodedString, "encoded Participant").isEqualTo(expectedJson)
        decoded.run {
            assertThat(this).isEqualTo(expectedRequest)
            assertThat(name).isEqualTo(expectedRequest.name)
            assertThat(imageUrl).isEqualTo(expectedRequest.imageUrl)
            assertThat(originatingEntity).isEqualTo(expectedRequest.originatingEntity)
        }
    }
}
