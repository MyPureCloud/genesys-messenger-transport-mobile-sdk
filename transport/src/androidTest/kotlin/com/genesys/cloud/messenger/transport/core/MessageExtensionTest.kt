package com.genesys.cloud.messenger.transport.core

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNull
import assertk.assertions.isTrue
import com.genesys.cloud.messenger.transport.core.Message.Direction
import com.genesys.cloud.messenger.transport.core.Message.Participant
import com.genesys.cloud.messenger.transport.core.Message.State
import com.genesys.cloud.messenger.transport.core.Message.Type
import com.genesys.cloud.messenger.transport.core.events.Event
import com.genesys.cloud.messenger.transport.network.TestWebMessagingApiResponses
import com.genesys.cloud.messenger.transport.network.TestWebMessagingApiResponses.isoTestTimestamp
import com.genesys.cloud.messenger.transport.shyrka.receive.PresenceEvent
import com.genesys.cloud.messenger.transport.shyrka.receive.StructuredMessage
import com.genesys.cloud.messenger.transport.shyrka.receive.StructuredMessageEvent
import com.genesys.cloud.messenger.transport.shyrka.receive.isInbound
import com.genesys.cloud.messenger.transport.shyrka.receive.isOutbound
import com.genesys.cloud.messenger.transport.shyrka.send.HealthCheckID
import com.genesys.cloud.messenger.transport.util.extensions.fromIsoToEpochMilliseconds
import com.genesys.cloud.messenger.transport.util.extensions.getUploadedAttachments
import com.genesys.cloud.messenger.transport.util.extensions.isHealthCheckResponseId
import com.genesys.cloud.messenger.transport.util.extensions.isOutbound
import com.genesys.cloud.messenger.transport.util.extensions.mapOriginatingEntity
import com.genesys.cloud.messenger.transport.util.extensions.toMessage
import com.genesys.cloud.messenger.transport.util.extensions.toMessageList
import com.genesys.cloud.messenger.transport.utility.MessageValues
import com.genesys.cloud.messenger.transport.utility.QuickReplyTestValues
import com.genesys.cloud.messenger.transport.utility.StructuredMessageValues
import net.bytebuddy.utility.RandomString
import org.junit.Test

internal class MessageExtensionTest {

    @Test
    fun `when MessageEntityList toMessageList()`() {
        val expectedMessage1 = Message(
            id = "5befde6373a23f32f20b59b4e1cba0e6",
            direction = Direction.Outbound,
            state = State.Sent,
            messageType = Type.Text,
            text = "\uD83E\uDD2A",
            timeStamp = 1398892191411L,
            from = Participant(originatingEntity = Participant.OriginatingEntity.Bot),
        )
        val expectedMessage2 = Message(
            id = "1234567890",
            direction = Direction.Inbound,
            state = State.Sent,
            messageType = Type.Event,
            text = "customer msg 7",
            timeStamp = null,
            events = listOf(Event.ConversationAutostart),
            from = Participant(originatingEntity = Participant.OriginatingEntity.Human),
        )
        val expectedMessage3 = Message(
            id = "1234567890",
            direction = Direction.Outbound,
            state = State.Sent,
            messageType = Type.QuickReply,
            text = "quick reply text",
            timeStamp = null,
            quickReplies = listOf(
                QuickReplyTestValues.buttonResponse_a,
                QuickReplyTestValues.buttonResponse_b,
            ),
            from = Participant(originatingEntity = Participant.OriginatingEntity.Bot),
        )

        val result = TestWebMessagingApiResponses.testMessageEntityList.entities.toMessageList()

        assertThat(result).containsExactly(expectedMessage1, expectedMessage2, expectedMessage3)
    }

    @Test
    fun `when inbound StructuredMessage toMessage()`() {
        val givenStructuredMessage = StructuredMessage(
            id = "id",
            channel = StructuredMessage.Channel(
                time = isoTestTimestamp,
                from = StructuredMessage.Participant(
                    nickname = "Bob",
                    image = "http://image.png",
                )
            ),
            type = StructuredMessage.Type.Text,
            text = "test text",
            content = listOf(
                StructuredMessage.Content.AttachmentContent(
                    contentType = "Attachment",
                    attachment = StructuredMessage.Content.AttachmentContent.Attachment(
                        id = "test attachment id",
                        url = "http://test.com",
                        filename = "test.png",
                        mediaType = "image/png",
                    )
                )
            ),
            direction = "Inbound",
            metadata = mapOf("customMessageId" to "test custom id"),
            events = listOf(
                PresenceEvent(
                    eventType = StructuredMessageEvent.Type.Presence,
                    presence = PresenceEvent.Presence(PresenceEvent.Presence.Type.Join)
                )
            )
        )
        val expectedMessage =
            Message(
                id = "test custom id",
                direction = Direction.Inbound,
                state = State.Sent,
                messageType = Type.Text,
                text = "test text",
                timeStamp = 1398892191411L,
                attachments = mapOf(
                    "test attachment id" to Attachment(
                        id = "test attachment id",
                        fileName = "test.png",
                        state = Attachment.State.Sent("http://test.com")
                    )
                ),
                events = listOf<Event>(Event.ConversationAutostart),
                from = Participant(
                    name = "Bob",
                    imageUrl = "http://image.png",
                    originatingEntity = Participant.OriginatingEntity.Human
                ),
            )

        assertThat(givenStructuredMessage.toMessage()).isEqualTo(expectedMessage)
    }

    @Test
    fun `when getUploadedAttachments() with 1 uploaded and 1 deleted attachment`() {
        val givenMessage =
            Message(
                id = "test custom id",
                direction = Direction.Inbound,
                state = State.Sent,
                attachments = mapOf(
                    "first test attachment id" to Attachment(
                        id = "first test attachment id",
                        fileName = "test.png",
                        Attachment.State.Uploaded("http://test.com")
                    ),
                    "second test attachment id" to Attachment(
                        id = "second test attachment id",
                        fileName = "test2.png",
                        Attachment.State.Detached,
                    )
                )
            )
        val expectedContent = Message.Content(
            contentType = Message.Content.Type.Attachment,
            attachment = Attachment(
                id = "first test attachment id",
                fileName = "test.png",
                state = Attachment.State.Uploaded("http://test.com")
            )
        )

        assertThat(givenMessage.getUploadedAttachments()).containsExactly(expectedContent)
    }

    @Test
    fun `when getUploadedAttachments() without attachments`() {
        val givenMessage =
            Message(
                id = "test custom id",
                direction = Direction.Inbound,
                state = State.Sent,
                attachments = emptyMap()
            )

        assertThat(givenMessage.getUploadedAttachments()).isEmpty()
    }

    @Test
    fun `when outbound StructuredMessage toMessage() from Participant with unknown info`() {
        val givenStructuredMessage = StructuredMessage(
            id = "id",
            type = StructuredMessage.Type.Text,
            direction = "Outbound",
        )
        val expectedMessage =
            Message(
                id = "id",
                direction = Direction.Outbound,
                state = State.Sent,
                messageType = Type.Text,
                from = Participant(
                    originatingEntity = Participant.OriginatingEntity.Unknown
                ),
            )

        assertThat(givenStructuredMessage.toMessage()).isEqualTo(expectedMessage)
    }

    @Test
    fun `when fromIsoToEpochMilliseconds() on valid ISO string`() {
        val expectedTimestamp = 1398892191411L

        val result = isoTestTimestamp.fromIsoToEpochMilliseconds()

        assertThat(result).isEqualTo(expectedTimestamp)
    }

    @Test
    fun `when fromIsoToEpochMilliseconds() on invalid string`() {
        val result = "invalid timestamp format".fromIsoToEpochMilliseconds()

        assertThat(result).isNull()
    }

    @Test
    fun `when fromIsoToEpochMilliseconds() on null string`() {
        val result = null.fromIsoToEpochMilliseconds()

        assertThat(result).isNull()
    }

    @Test
    fun `when outbound StructuredMessage is checked for isOutbound()`() {
        val givenStructuredMessage = StructuredMessage(
            id = "some_id",
            type = StructuredMessage.Type.Text,
            direction = "Outbound"
        )

        assertThat(givenStructuredMessage.isOutbound()).isTrue()
    }

    @Test
    fun `when inbound StructuredMessage is checked for isOutbound()`() {
        val givenStructuredMessage = StructuredMessage(
            id = "some_id",
            type = StructuredMessage.Type.Text,
            direction = "Inbound"
        )

        assertThat(givenStructuredMessage.isOutbound()).isFalse()
    }

    @Test
    fun `when inbound StructuredMessage is checked for isInbound()`() {
        val givenStructuredMessage = StructuredMessage(
            id = "some_id",
            type = StructuredMessage.Type.Text,
            direction = "Inbound"
        )

        assertThat(givenStructuredMessage.isInbound()).isTrue()
    }

    @Test
    fun `when outbound StructuredMessage is checked for isInbound()`() {
        val givenStructuredMessage = StructuredMessage(
            id = "some_id",
            type = StructuredMessage.Type.Text,
            direction = "Outbound"
        )

        assertThat(givenStructuredMessage.isInbound()).isFalse()
    }

    @Test
    fun `when mapOriginatingEntity() with originatingEntity=Human and isInbound=false`() {
        val givenIsInbound = false
        val originatingEntity = "Human"
        val expectedOriginatingEntity = Participant.OriginatingEntity.Human

        val result = originatingEntity.mapOriginatingEntity { givenIsInbound }

        assertThat(result).isEqualTo(expectedOriginatingEntity)
    }

    @Test
    fun `when mapOriginatingEntity() with originatingEntity=Bot and isInbound=false`() {
        val givenIsInbound = false
        val originatingEntity = "Bot"
        val expectedOriginatingEntity = Participant.OriginatingEntity.Bot

        val result = originatingEntity.mapOriginatingEntity { givenIsInbound }

        assertThat(result).isEqualTo(expectedOriginatingEntity)
    }

    @Test
    fun `when mapOriginatingEntity() with originatingEntity=Unknown and isInbound=false`() {
        val givenIsInbound = false
        val originatingEntity = "any value"
        val expectedOriginatingEntity = Participant.OriginatingEntity.Unknown

        val result = originatingEntity.mapOriginatingEntity { givenIsInbound }

        assertThat(result).isEqualTo(expectedOriginatingEntity)
    }

    @Test
    fun `when mapOriginatingEntity() with originatingEntity=null and isInbound=false`() {
        val givenIsInbound = false
        val originatingEntity = null
        val expectedOriginatingEntity = Participant.OriginatingEntity.Unknown

        val result = originatingEntity.mapOriginatingEntity { givenIsInbound }

        assertThat(result).isEqualTo(expectedOriginatingEntity)
    }

    @Test
    fun `when mapOriginatingEntity() with originatingEntity=Bot and isInbound=true`() {
        val givenIsInbound = true
        val originatingEntity = "Bot"
        val expectedOriginatingEntity = Participant.OriginatingEntity.Human

        val result = originatingEntity.mapOriginatingEntity { givenIsInbound }

        assertThat(result).isEqualTo(expectedOriginatingEntity)
    }

    @Test
    fun `when isHealthCheckResponseId match HealthCheckId`() {
        assertThat(HealthCheckID.isHealthCheckResponseId()).isTrue()
    }

    @Test
    fun `when isHealthCheckResponseId not equals HealthCheckId`() {
        var randomString: String
        do {
            randomString = RandomString().nextString()
        } while (randomString == HealthCheckID)

        assertThat(randomString.isHealthCheckResponseId()).isFalse()
    }

    @Test
    fun `when outbound Message is checked for isOutbound()`() {
        val givenMessage = Message(direction = Direction.Outbound)

        assertThat(givenMessage.isOutbound()).isTrue()
    }

    @Test
    fun `when inbound Message is checked for isOutbound()`() {
        val givenMessage = Message(direction = Direction.Inbound)

        assertThat(givenMessage.isOutbound()).isFalse()
    }

    @Test
    fun `when StructureMessage toMessage() has Content with QuickReplyContent`() {
        val givenStructuredMessage = StructuredMessageValues.createStructuredMessageForTesting(
            type = StructuredMessage.Type.Structured,
            content = listOf(StructuredMessageValues.createQuickReplyContentForTesting())
        )
        val expectedButtonResponse = ButtonResponse(
            text = MessageValues.Text,
            payload = StructuredMessageValues.Payload,
            type = StructuredMessageValues.QuickReply
        )
        val expectedMessage = Message(
            id = MessageValues.Id,
            state = State.Sent,
            type = Message.Type.QuickReply.name,
            messageType = Message.Type.QuickReply,
            quickReplies = listOf(expectedButtonResponse)
        )

        val result = givenStructuredMessage.toMessage()

        assertThat(result).isEqualTo(expectedMessage)
    }

    @Test
    fun `when StructureMessage toMessage() has Content with ButtonResponseContent`() {
        val givenStructuredMessage = StructuredMessageValues.createStructuredMessageForTesting(
            type = StructuredMessage.Type.Structured,
            content = listOf(StructuredMessageValues.createButtonResponseContentForTesting())
        )
        val expectedButtonResponse = ButtonResponse(
            text = MessageValues.Text,
            payload = StructuredMessageValues.Payload,
            type = StructuredMessageValues.QuickReply
        )
        val expectedMessage = Message(
            id = MessageValues.Id,
            state = State.Sent,
            type = Message.Type.QuickReply.name,
            messageType = Message.Type.QuickReply,
            quickReplies = listOf(expectedButtonResponse)
        )

        val result = givenStructuredMessage.toMessage()

        assertThat(result).isEqualTo(expectedMessage)
    }

    @Test
    fun `when StructureMessage toMessage() has Content with QuickReplyContent and ButtonResponseContent`() {
        val givenStructuredMessage = StructuredMessageValues.createStructuredMessageForTesting(
            type = StructuredMessage.Type.Structured,
            content = listOf(
                StructuredMessageValues.createQuickReplyContentForTesting(),
                StructuredMessageValues.createButtonResponseContentForTesting(),
            )
        )
        val expectedButtonResponse = ButtonResponse(
            text = MessageValues.Text,
            payload = StructuredMessageValues.Payload,
            type = StructuredMessageValues.QuickReply
        )
        val expectedMessage = Message(
            id = MessageValues.Id,
            state = State.Sent,
            type = Message.Type.QuickReply.name,
            messageType = Message.Type.QuickReply,
            quickReplies = listOf(expectedButtonResponse)
        )

        val result = givenStructuredMessage.toMessage()

        assertThat(result).isEqualTo(expectedMessage)
    }

    @Test
    fun `when StructureMessage toMessage() has Content without QuickReplyContent or ButtonResponseContent`() {
        val givenStructuredMessage = StructuredMessageValues.createStructuredMessageForTesting(
            type = StructuredMessage.Type.Structured
        )

        val expectedMessage = Message(
            id = MessageValues.Id,
            state = State.Sent,
            type = Message.Type.Unknown.name,
            messageType = Message.Type.Unknown,
        )

        val result = givenStructuredMessage.toMessage()

        assertThat(result).isEqualTo(expectedMessage)
    }

    @Test
    fun `when MessageEntityList toMessageList() has message with type Unknown`() {
        val givenStructuredMessageList = listOf(
            StructuredMessageValues.createStructuredMessageForTesting(
                type = StructuredMessage.Type.Structured
            )
        )

        val result = givenStructuredMessageList.toMessageList()

        assertThat(result).isEmpty()
    }
}
