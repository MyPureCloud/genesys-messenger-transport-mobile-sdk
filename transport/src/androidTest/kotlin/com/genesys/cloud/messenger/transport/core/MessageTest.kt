package com.genesys.cloud.messenger.transport.core

import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import com.genesys.cloud.messenger.transport.core.Message.Direction
import com.genesys.cloud.messenger.transport.core.Message.Participant
import com.genesys.cloud.messenger.transport.core.Message.State
import com.genesys.cloud.messenger.transport.core.events.Event
import com.genesys.cloud.messenger.transport.shyrka.WebMessagingJson
import com.genesys.cloud.messenger.transport.utility.AttachmentValues
import com.genesys.cloud.messenger.transport.utility.MessageValues
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import org.junit.Test

class MessageTest {

    @Test
    fun `validate default constructor`() {
        val expectedDirection = Direction.Inbound
        val expectedState = State.Idle
        val expectedType = MessageValues.Type
        val expectedParticipant =
            Participant(originatingEntity = Participant.OriginatingEntity.Human)

        val message = Message()

        message.run {
            assertThat(direction).isEqualTo(expectedDirection)
            assertThat(state).isEqualTo(expectedState)
            assertThat(type).isEqualTo(expectedType)
            assertThat(text).isNull()
            assertThat(timeStamp).isNull()
            assertThat(attachments).isEmpty()
            assertThat(events).isEmpty()
            from.run {
                assertThat(this).isEqualTo(expectedParticipant)
                assertThat(name).isNull()
                assertThat(imageUrl).isNull()
                assertThat(originatingEntity).isEqualTo(Participant.OriginatingEntity.Human)
            }
            assertThat(from).isEqualTo(expectedParticipant)
        }
    }

    @Test
    fun `validate custom constructor`() {
        val expectedId = MessageValues.Id
        val expectedDirection = Direction.Outbound
        val expectedState = State.Sending
        val expectedType = "custom_type"
        val expectedText = MessageValues.Text
        val expectedTimestamp = MessageValues.TimeStamp
        val expectedAttachments = mapOf(AttachmentValues.Id to Attachment(AttachmentValues.Id))
        val expectedEvents = listOf(Event.ConversationAutostart)
        val expectedParticipant = Participant(
            name = MessageValues.ParticipantName,
            imageUrl = MessageValues.ParticipantImageUrl,
            originatingEntity = Participant.OriginatingEntity.Bot
        )

        val message = Message(
            id = MessageValues.Id,
            direction = Direction.Outbound,
            state = State.Sending,
            type = "custom_type",
            text = MessageValues.Text,
            timeStamp = MessageValues.TimeStamp,
            attachments = mapOf(AttachmentValues.Id to Attachment(AttachmentValues.Id)),
            events = listOf(Event.ConversationAutostart),
            from = Participant(
                name = MessageValues.ParticipantName,
                imageUrl = MessageValues.ParticipantImageUrl,
                originatingEntity = Participant.OriginatingEntity.Bot,
            )
        )

        message.run {
            assertThat(id).isEqualTo(expectedId)
            assertThat(direction).isEqualTo(expectedDirection)
            assertThat(state).isEqualTo(expectedState)
            assertThat(type).isEqualTo(expectedType)
            assertThat(text).isEqualTo(expectedText)
            assertThat(timeStamp).isEqualTo(expectedTimestamp)
            assertThat(attachments).isEqualTo(expectedAttachments)
            assertThat(events).isEqualTo(expectedEvents)
            assertThat(from).isEqualTo(expectedParticipant)
        }
    }

    @Test
    fun `validate Content serialization`() {
        val expectedAttachment = Attachment(
            id = AttachmentValues.Id,
            fileName = null,
            state = Attachment.State.Presigning
        )
        val expectedRequest = Message.Content(
            contentType = Message.Content.Type.Attachment,
            attachment = Attachment(AttachmentValues.Id)
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
    fun `validate Direction serialization`() {
        val expectedRequest = Direction.Inbound
        val expectedJson = """"Inbound""""

        val encodedString = WebMessagingJson.json.encodeToString(expectedRequest)
        val decoded = WebMessagingJson.json.decodeFromString<Direction>(expectedJson)

        assertThat(encodedString, "encoded Direction").isEqualTo(expectedJson)
        assertThat(decoded).isEqualTo(expectedRequest)
    }

    @Test
    fun `validate Participant serialization`() {
        val expectedRequest = Participant(
            name = MessageValues.ParticipantName,
            imageUrl = MessageValues.ParticipantImageUrl,
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
