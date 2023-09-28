package com.genesys.cloud.messenger.transport.core

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNull
import assertk.assertions.isTrue
import com.genesys.cloud.messenger.transport.core.events.Event
import com.genesys.cloud.messenger.transport.network.TestWebMessagingApiResponses
import com.genesys.cloud.messenger.transport.network.TestWebMessagingApiResponses.isoTestTimestamp
import com.genesys.cloud.messenger.transport.shyrka.receive.AllowedMedia
import com.genesys.cloud.messenger.transport.shyrka.receive.FileType
import com.genesys.cloud.messenger.transport.shyrka.receive.Inbound
import com.genesys.cloud.messenger.transport.shyrka.receive.PresenceEvent
import com.genesys.cloud.messenger.transport.shyrka.receive.SessionResponse
import com.genesys.cloud.messenger.transport.shyrka.receive.StructuredMessage
import com.genesys.cloud.messenger.transport.shyrka.receive.StructuredMessageEvent
import com.genesys.cloud.messenger.transport.shyrka.receive.isInbound
import com.genesys.cloud.messenger.transport.shyrka.receive.isOutbound
import com.genesys.cloud.messenger.transport.util.extensions.fromIsoToEpochMilliseconds
import com.genesys.cloud.messenger.transport.util.extensions.getUploadedAttachments
import com.genesys.cloud.messenger.transport.util.extensions.mapOriginatingEntity
import com.genesys.cloud.messenger.transport.util.extensions.toFileAttachmentProfile
import com.genesys.cloud.messenger.transport.util.extensions.toMessage
import com.genesys.cloud.messenger.transport.util.extensions.toMessageList
import org.junit.Test

internal class MessageExtensionTest {

    @Test
    fun `when MessageEntityList toMessageList()`() {
        val expectedMessage1 = Message(
            id = "5befde6373a23f32f20b59b4e1cba0e6",
            direction = Message.Direction.Outbound,
            state = Message.State.Sent,
            type = "Text",
            text = "\uD83E\uDD2A",
            timeStamp = 1398892191411L,
            from = Message.Participant(originatingEntity = Message.Participant.OriginatingEntity.Bot),
        )
        val expectedMessage2 = Message(
            id = "1234567890",
            direction = Message.Direction.Inbound,
            state = Message.State.Sent,
            type = "Text",
            text = "customer msg 7",
            timeStamp = null,
            events = listOf(Event.ConversationAutostart),
            from = Message.Participant(originatingEntity = Message.Participant.OriginatingEntity.Human),
        )

        val result = TestWebMessagingApiResponses.testMessageEntityList.entities.toMessageList()

        assertThat(result).containsExactly(expectedMessage1, expectedMessage2)
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
                direction = Message.Direction.Inbound,
                state = Message.State.Sent,
                type = "Text",
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
                from = Message.Participant(
                    name = "Bob",
                    imageUrl = "http://image.png",
                    originatingEntity = Message.Participant.OriginatingEntity.Human
                )
            )

        assertThat(givenStructuredMessage.toMessage()).isEqualTo(expectedMessage)
    }

    @Test
    fun `when getUploadedAttachments() with 1 uploaded and 1 deleted attachments`() {
        val givenMessage =
            Message(
                id = "test custom id",
                direction = Message.Direction.Inbound,
                state = Message.State.Sent,
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
    fun `when getUploadedAttachments() but there are no attachments`() {
        val givenMessage =
            Message(
                id = "test custom id",
                direction = Message.Direction.Inbound,
                state = Message.State.Sent,
                attachments = emptyMap()
            )

        assertThat(givenMessage.getUploadedAttachments()).isEmpty()
    }

    @Test
    fun `when outbound StructuredMessage toMessage() from participant with unknown info`() {
        val givenStructuredMessage = StructuredMessage(
            id = "id",
            type = StructuredMessage.Type.Text,
            direction = "Outbound",
        )
        val expectedMessage =
            Message(
                id = "id",
                direction = Message.Direction.Outbound,
                state = Message.State.Sent,
                type = "Text",
                from = Message.Participant(
                    originatingEntity = Message.Participant.OriginatingEntity.Unknown
                )
            )

        assertThat(givenStructuredMessage.toMessage()).isEqualTo(expectedMessage)
    }

    @Test
    fun `when fromIsoToEpochMilliseconds() on valid ISO timestamp String`() {
        val expectedTimestamp = 1398892191411L

        val result = isoTestTimestamp.fromIsoToEpochMilliseconds()

        assertThat(result).isEqualTo(expectedTimestamp)
    }

    @Test
    fun `when fromIsoToEpochMilliseconds() on invalid timestamp String`() {
        val result = "invalid timestamp format".fromIsoToEpochMilliseconds()

        assertThat(result).isNull()
    }

    @Test
    fun `when fromIsoToEpochMilliseconds() on a null String`() {
        val result = null.fromIsoToEpochMilliseconds()

        assertThat(result).isNull()
    }

    @Test
    fun `when outbound StructuredMessage checked for isOutbound()`() {
        val givenStructuredMessage = StructuredMessage(
            id = "some_id",
            type = StructuredMessage.Type.Text,
            direction = "Outbound"
        )

        assertThat(givenStructuredMessage.isOutbound()).isTrue()
    }

    @Test
    fun `when inbound StructuredMessage checked for isOutbound()`() {
        val givenStructuredMessage = StructuredMessage(
            id = "some_id",
            type = StructuredMessage.Type.Text,
            direction = "Inbound"
        )

        assertThat(givenStructuredMessage.isOutbound()).isFalse()
    }

    @Test
    fun `when inbound StructuredMessage checked for isInbound()`() {
        val givenStructuredMessage = StructuredMessage(
            id = "some_id",
            type = StructuredMessage.Type.Text,
            direction = "Inbound"
        )

        assertThat(givenStructuredMessage.isInbound()).isTrue()
    }

    @Test
    fun `when outbound StructuredMessage checked for isInbound()`() {
        val givenStructuredMessage = StructuredMessage(
            id = "some_id",
            type = StructuredMessage.Type.Text,
            direction = "Outbound"
        )

        assertThat(givenStructuredMessage.isInbound()).isFalse()
    }

    @Test
    fun `when mapOriginatingEntity() is Human with inbound=false`() {
        val givenIsInbound = false
        val originatingEntity = "Human"
        val expectedOriginatingEntity = Message.Participant.OriginatingEntity.Human

        val result = originatingEntity.mapOriginatingEntity { givenIsInbound }

        assertThat(result).isEqualTo(expectedOriginatingEntity)
    }

    @Test
    fun `when mapOriginatingEntity() is Bot with inbound=false`() {
        val givenIsInbound = false
        val originatingEntity = "Bot"
        val expectedOriginatingEntity = Message.Participant.OriginatingEntity.Bot

        val result = originatingEntity.mapOriginatingEntity { givenIsInbound }

        assertThat(result).isEqualTo(expectedOriginatingEntity)
    }

    @Test
    fun `when mapOriginatingEntity() is unknown with inbound=false`() {
        val givenIsInbound = false
        val originatingEntity = "any value"
        val expectedOriginatingEntity = Message.Participant.OriginatingEntity.Unknown

        val result = originatingEntity.mapOriginatingEntity { givenIsInbound }

        assertThat(result).isEqualTo(expectedOriginatingEntity)
    }

    @Test
    fun `when mapOriginatingEntity() is null with inbound=false`() {
        val givenIsInbound = false
        val originatingEntity = null
        val expectedOriginatingEntity = Message.Participant.OriginatingEntity.Unknown

        val result = originatingEntity.mapOriginatingEntity { givenIsInbound }

        assertThat(result).isEqualTo(expectedOriginatingEntity)
    }

    @Test
    fun `when mapOriginatingEntity() is Bot with inbound=true`() {
        val givenIsInbound = true
        val originatingEntity = "Bot"
        val expectedOriginatingEntity = Message.Participant.OriginatingEntity.Human

        val result = originatingEntity.mapOriginatingEntity { givenIsInbound }

        assertThat(result).isEqualTo(expectedOriginatingEntity)
    }

    @Test
    fun `when SessionResponse toFileAttachmentProfile() but it has no AllowedMedia and blockedExtensions entries`() {
        val givenSessionResponse = SessionResponse(connected = true)
        val expectedFileAttachmentProfile = FileAttachmentProfile()

        val result = givenSessionResponse.toFileAttachmentProfile()

        assertThat(result).isEqualTo(expectedFileAttachmentProfile)
    }

    @Test
    fun `when SessionResponse toFileAttachmentProfile() but AllowedMedia has no inbound and blockedExtensions entries`() {
        val givenSessionResponse = SessionResponse(connected = true, allowedMedia = AllowedMedia())
        val expectedFileAttachmentProfile = FileAttachmentProfile()

        val result = givenSessionResponse.toFileAttachmentProfile()

        assertThat(result).isEqualTo(expectedFileAttachmentProfile)
    }

    @Test
    fun `when SessionResponse toFileAttachmentProfile() but AllowedMedia has no filetypes,maxFileSizeKB and blockedExtensions entries`() {
        val givenSessionResponse =
            SessionResponse(connected = true, allowedMedia = AllowedMedia(Inbound()))
        val expectedFileAttachmentProfile = FileAttachmentProfile()

        val result = givenSessionResponse.toFileAttachmentProfile()

        assertThat(result).isEqualTo(expectedFileAttachmentProfile)
    }

    @Test
    fun `when SessionResponse toFileAttachmentProfile() and AllowedMedia has filetypes without wildcard but with maxFileSizeKB and blockedExtensions entries`() {
        val givenSessionResponse = SessionResponse(
            connected = true,
            allowedMedia = AllowedMedia(
                inbound = Inbound(
                    fileTypes = listOf(FileType("video/mpg"), FileType("video/3gpp")),
                    maxFileSizeKB = 10240,
                )
            ),
            blockedExtensions = listOf(".ade", ".adp")
        )
        val expectedFileAttachmentProfile = FileAttachmentProfile(
            allowedFileTypes = listOf("video/mpg", "video/3gpp"),
            blockedFileTypes = listOf(".ade", ".adp"),
            maxFileSizeKB = 10240,
            hasWildCard = false,
        )

        val result = givenSessionResponse.toFileAttachmentProfile()

        assertThat(result).isEqualTo(expectedFileAttachmentProfile)
    }

    @Test
    fun `when SessionResponse toFileAttachmentProfile() and AllowedMedia has filetypes with wildcard,maxFileSizeKB and blockedExtensions entries`() {
        val givenSessionResponse = SessionResponse(
            connected = true,
            allowedMedia = AllowedMedia(
                inbound = Inbound(
                    fileTypes = listOf(FileType("*/*"), FileType("video/3gpp")),
                    maxFileSizeKB = 10240,
                ),
            ),
            blockedExtensions = listOf(".ade", ".adp")
        )
        val expectedFileAttachmentProfile = FileAttachmentProfile(
            allowedFileTypes = listOf("video/3gpp"),
            blockedFileTypes = listOf(".ade", ".adp"),
            maxFileSizeKB = 10240,
            hasWildCard = true,
        )

        val result = givenSessionResponse.toFileAttachmentProfile()

        assertThat(result).isEqualTo(expectedFileAttachmentProfile)
    }
}
