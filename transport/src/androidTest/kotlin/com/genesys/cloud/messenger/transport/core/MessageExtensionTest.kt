package com.genesys.cloud.messenger.transport.core

import com.genesys.cloud.messenger.transport.network.TestWebMessagingApiResponses
import com.genesys.cloud.messenger.transport.shyrka.receive.StructuredMessage
import com.genesys.cloud.messenger.transport.util.extensions.getUploadedAttachments
import com.genesys.cloud.messenger.transport.util.extensions.toMessage
import com.genesys.cloud.messenger.transport.util.extensions.toMessageList
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal class MessageExtensionTest {

    @Test
    fun whenMessageEntityListToMessageList() {
        val givenMessageEntityList = TestWebMessagingApiResponses.testMessageEntityList
        val expectedMessageList = listOf(
            Message(
                id = "5befde6373a23f32f20b59b4e1cba0e6",
                direction = Message.Direction.Outbound,
                state = Message.State.Sent,
                type = "Text",
                text = "\uD83E\uDD2A",
                timeStamp = "2021-03-26T21:11:01.464Z"
            ),
            Message(
                id = "1234567890",
                direction = Message.Direction.Inbound,
                state = Message.State.Sent,
                type = "Text",
                text = "customer msg 7",
                timeStamp = "2021-03-26T21:09:51.411Z",
            )
        )

        assertEquals(expectedMessageList, givenMessageEntityList.toMessageList())
    }

    @Test
    fun whenStructuredMessageToMessage() {
        val givenStructuredMessage = StructuredMessage(
            id = "id",
            channel = StructuredMessage.Channel(time = "2021-03-26T21:09:51.411Z"),
            type = "Text",
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
                timeStamp = "2021-03-26T21:09:51.411Z",
                attachments = mapOf(
                    "test attachment id" to Attachment(
                        id = "test attachment id",
                        fileName = "test.png",
                        state = Attachment.State.Sent("http://test.com")
                    )
                )
            )

        assertEquals(expectedMessage, givenStructuredMessage.toMessage())
    }

    @Test
    fun whenGetUploadedAttachmentsWithOneUploadedAndOneDeletedAttachments() {
        val givenMessage =
            Message(
                id = "test custom id",
                direction = Message.Direction.Inbound,
                state = Message.State.Sent,
                type = "Text",
                text = "test text",
                timeStamp = "2021-03-26T21:09:51.411Z",
                attachments = mapOf(
                    "first test attachment id" to Attachment(
                        id = "first test attachment id",
                        fileName = "test.png",
                        Attachment.State.Uploaded("http://test.com")
                    ),
                    "second test attachment id" to Attachment(
                        id = "second test attachment id",
                        fileName = "test2.png",
                        Attachment.State.Deleted,
                    )
                )
            )
        val expectedAttachmentIds = arrayOf("first test attachment id")

        assertTrue { expectedAttachmentIds.contentEquals(givenMessage.getUploadedAttachments()) }
    }

    @Test
    fun whenGetUploadedAttachmentsWithoutAttachments() {
        val givenMessage =
            Message(
                id = "test custom id",
                direction = Message.Direction.Inbound,
                state = Message.State.Sent,
                type = "Text",
                text = "test text",
                timeStamp = "2021-03-26T21:09:51.411Z",
                attachments = emptyMap()
            )

        assertTrue { givenMessage.getUploadedAttachments().isEmpty() }
    }
}
