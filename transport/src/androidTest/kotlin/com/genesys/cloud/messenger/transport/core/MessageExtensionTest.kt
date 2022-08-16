package com.genesys.cloud.messenger.transport.core

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import com.genesys.cloud.messenger.transport.network.TestWebMessagingApiResponses
import com.genesys.cloud.messenger.transport.network.TestWebMessagingApiResponses.isoTestTimestamp
import com.genesys.cloud.messenger.transport.shyrka.receive.StructuredMessage
import com.genesys.cloud.messenger.transport.util.extensions.fromIsoToEpochMilliseconds
import com.genesys.cloud.messenger.transport.util.extensions.getUploadedAttachments
import com.genesys.cloud.messenger.transport.util.extensions.toMessage
import com.genesys.cloud.messenger.transport.util.extensions.toMessageList
import org.junit.Test

internal class MessageExtensionTest {

    @Test
    fun whenMessageEntityListToMessageList() {
        val expectedMessage1 = Message(
            id = "5befde6373a23f32f20b59b4e1cba0e6",
            direction = Message.Direction.Outbound,
            state = Message.State.Sent,
            type = "Text",
            text = "\uD83E\uDD2A",
            timeStamp = 1398892191411L
        )
        val expectedMessage2 = Message(
            id = "1234567890",
            direction = Message.Direction.Inbound,
            state = Message.State.Sent,
            type = "Text",
            text = "customer msg 7",
            timeStamp = null,
        )

        val result = TestWebMessagingApiResponses.testMessageEntityList.toMessageList()

        assertThat(result).containsExactly(expectedMessage1, expectedMessage2)
    }

    @Test
    fun whenStructuredMessageToMessage() {
        val givenStructuredMessage = StructuredMessage(
            id = "id",
            channel = StructuredMessage.Channel(time = isoTestTimestamp),
            type = StructuredMessage.Type.Text,
            text = "test text",
            content = listOf(
                StructuredMessage.Content(
                    contentType = "Attachment",
                    attachment = StructuredMessage.Attachment(
                        id = "test attachment id",
                        url = "http://test.com",
                        filename = "test.png",
                        mediaType = "image/png",
                    )
                )
            ),
            direction = "Inbound",
            metadata = mapOf("customMessageId" to "test custom id")
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
                )
            )

        assertThat(givenStructuredMessage.toMessage()).isEqualTo(expectedMessage)
    }

    @Test
    fun whenGetUploadedAttachmentsWithOneUploadedAndOneDeletedAttachments() {
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
    fun whenGetUploadedAttachmentsWithoutAttachments() {
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
    fun whenFromIsoToEpochMillisecondsOnValidISOString() {
        val expectedTimestamp = 1398892191411L

        val result = isoTestTimestamp.fromIsoToEpochMilliseconds()

        assertThat(result).isEqualTo(expectedTimestamp)
    }

    @Test
    fun whenFromIsoToEpochMillisecondsOnInvalidString() {
        val result = "invalid timestamp format".fromIsoToEpochMilliseconds()

        assertThat(result).isNull()
    }

    @Test
    fun whenFromIsoToEpochMillisecondsOnNullString() {
        val result = null.fromIsoToEpochMilliseconds()

        assertThat(result).isNull()
    }
}
