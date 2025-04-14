package transport.core

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNull
import assertk.assertions.isTrue
import com.genesys.cloud.messenger.transport.core.Attachment
import com.genesys.cloud.messenger.transport.core.ButtonResponse
import com.genesys.cloud.messenger.transport.core.FileAttachmentProfile
import com.genesys.cloud.messenger.transport.core.Message
import com.genesys.cloud.messenger.transport.core.Message.Direction
import com.genesys.cloud.messenger.transport.core.Message.Participant
import com.genesys.cloud.messenger.transport.core.Message.State
import com.genesys.cloud.messenger.transport.core.Message.Type
import com.genesys.cloud.messenger.transport.core.events.Event
import com.genesys.cloud.messenger.transport.core.events.Event.ConnectionClosed.Reason
import com.genesys.cloud.messenger.transport.network.TestWebMessagingApiResponses
import com.genesys.cloud.messenger.transport.network.TestWebMessagingApiResponses.isoTestTimestamp
import com.genesys.cloud.messenger.transport.shyrka.WebMessagingJson
import com.genesys.cloud.messenger.transport.shyrka.receive.AllowedMedia
import com.genesys.cloud.messenger.transport.shyrka.receive.FileType
import com.genesys.cloud.messenger.transport.shyrka.receive.Inbound
import com.genesys.cloud.messenger.transport.shyrka.receive.MessageEntityList
import com.genesys.cloud.messenger.transport.shyrka.receive.PreIdentifiedWebMessagingMessage
import com.genesys.cloud.messenger.transport.shyrka.receive.PresenceEvent
import com.genesys.cloud.messenger.transport.shyrka.receive.PresignedUrlResponse
import com.genesys.cloud.messenger.transport.shyrka.receive.SessionResponse
import com.genesys.cloud.messenger.transport.shyrka.receive.StructuredMessage
import com.genesys.cloud.messenger.transport.shyrka.receive.StructuredMessageEvent
import com.genesys.cloud.messenger.transport.shyrka.receive.isInbound
import com.genesys.cloud.messenger.transport.shyrka.receive.isOutbound
import com.genesys.cloud.messenger.transport.shyrka.receive.toTransportConnectionClosedReason
import com.genesys.cloud.messenger.transport.shyrka.send.HealthCheckID
import com.genesys.cloud.messenger.transport.util.SIGNED_IN
import com.genesys.cloud.messenger.transport.util.extensions.fromIsoToEpochMilliseconds
import com.genesys.cloud.messenger.transport.util.extensions.getUploadedAttachments
import com.genesys.cloud.messenger.transport.util.extensions.isHealthCheckResponseId
import com.genesys.cloud.messenger.transport.util.extensions.isOutbound
import com.genesys.cloud.messenger.transport.util.extensions.isRefreshUrl
import com.genesys.cloud.messenger.transport.util.extensions.mapOriginatingEntity
import com.genesys.cloud.messenger.transport.util.extensions.sanitize
import com.genesys.cloud.messenger.transport.util.extensions.sanitizeCustomAttributes
import com.genesys.cloud.messenger.transport.util.extensions.sanitizeText
import com.genesys.cloud.messenger.transport.util.extensions.sanitizeToken
import com.genesys.cloud.messenger.transport.util.extensions.toFileAttachmentProfile
import com.genesys.cloud.messenger.transport.util.extensions.toMessage
import com.genesys.cloud.messenger.transport.util.extensions.toMessageList
import com.genesys.cloud.messenger.transport.utility.AttachmentValues
import com.genesys.cloud.messenger.transport.utility.MessageValues
import com.genesys.cloud.messenger.transport.utility.QuickReplyTestValues
import com.genesys.cloud.messenger.transport.utility.StructuredMessageValues
import com.genesys.cloud.messenger.transport.utility.TestValues
import kotlinx.serialization.encodeToString
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
                    firstName = MessageValues.PARTICIPANT_NAME,
                    lastName = MessageValues.PARTICIPANT_LAST_NAME,
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
                ),
                PresenceEvent(
                    eventType = StructuredMessageEvent.Type.Presence,
                    presence = PresenceEvent.Presence(PresenceEvent.Presence.Type.SignIn)
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
                events = listOf(Event.ConversationAutostart, Event.SignedIn(MessageValues.PARTICIPANT_NAME, MessageValues.PARTICIPANT_LAST_NAME)),
                from = Participant(
                    name = "Bob",
                    imageUrl = "http://image.png",
                    originatingEntity = Participant.OriginatingEntity.Human
                ),
            )

        givenStructuredMessage.toMessage().run {
            assertThat(this).isEqualTo(expectedMessage)
            assertThat(id).isEqualTo(expectedMessage.id)
            assertThat(direction).isEqualTo(expectedMessage.direction)
            assertThat(state).isEqualTo(expectedMessage.state)
            assertThat(type).isEqualTo(expectedMessage.type)
            assertThat(timeStamp).isEqualTo(expectedMessage.timeStamp)
            assertThat(events).containsExactly(*expectedMessage.events.toTypedArray())
            from.run {
                assertThat(name).isEqualTo(expectedMessage.from.name)
                assertThat(imageUrl).isEqualTo(expectedMessage.from.imageUrl)
                assertThat(originatingEntity).isEqualTo(expectedMessage.from.originatingEntity)
            }
        }
    }

    @Test
    fun `when getUploadedAttachments() with 1 uploaded and 1 deleted attachments`() {
        val givenMessage =
            Message(
                id = "test custom id",
                direction = Direction.Inbound,
                state = State.Sent,
                attachments = mapOf(
                    "first test attachment id" to Attachment(
                        id = "first test attachment id",
                        fileName = "test.png",
                        fileSizeInBytes = AttachmentValues.FILE_SIZE,
                        Attachment.State.Uploaded("http://test.com")
                    ),
                    "second test attachment id" to Attachment(
                        id = "second test attachment id",
                        fileName = "test2.png",
                        fileSizeInBytes = null,
                        Attachment.State.Detached,
                    )
                )
            )
        val expectedContent = Message.Content(
            contentType = Message.Content.Type.Attachment,
            attachment = Attachment(
                id = "first test attachment id",
                fileName = "test.png",
                fileSizeInBytes = AttachmentValues.FILE_SIZE,
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
                direction = Direction.Inbound,
                state = State.Sent,
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
        val expectedOriginatingEntity = Participant.OriginatingEntity.Human

        val result = originatingEntity.mapOriginatingEntity { givenIsInbound }

        assertThat(result).isEqualTo(expectedOriginatingEntity)
    }

    @Test
    fun `when mapOriginatingEntity() is Bot with inbound=false`() {
        val givenIsInbound = false
        val originatingEntity = "Bot"
        val expectedOriginatingEntity = Participant.OriginatingEntity.Bot

        val result = originatingEntity.mapOriginatingEntity { givenIsInbound }

        assertThat(result).isEqualTo(expectedOriginatingEntity)
    }

    @Test
    fun `when mapOriginatingEntity() is unknown with inbound=false`() {
        val givenIsInbound = false
        val originatingEntity = "any value"
        val expectedOriginatingEntity = Participant.OriginatingEntity.Unknown

        val result = originatingEntity.mapOriginatingEntity { givenIsInbound }

        assertThat(result).isEqualTo(expectedOriginatingEntity)
    }

    @Test
    fun `when mapOriginatingEntity() is null with inbound=false`() {
        val givenIsInbound = false
        val originatingEntity = null
        val expectedOriginatingEntity = Participant.OriginatingEntity.Unknown

        val result = originatingEntity.mapOriginatingEntity { givenIsInbound }

        assertThat(result).isEqualTo(expectedOriginatingEntity)
    }

    @Test
    fun `when mapOriginatingEntity() is Bot with inbound=true`() {
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
            content = listOf(QuickReplyTestValues.createQuickReplyContentForTesting())
        )
        val expectedButtonResponse = ButtonResponse(
            text = QuickReplyTestValues.TEXT_A,
            payload = QuickReplyTestValues.PAYLOAD_A,
            type = QuickReplyTestValues.QUICK_REPLY
        )
        val expectedMessage = Message(
            id = MessageValues.ID,
            state = State.Sent,
            type = Type.QuickReply.name,
            messageType = Type.QuickReply,
            quickReplies = listOf(expectedButtonResponse)
        )

        val result = givenStructuredMessage.toMessage()

        assertThat(result).isEqualTo(expectedMessage)
    }

    @Test
    fun `when StructureMessage toMessage() has Content with ButtonResponseContent`() {
        val givenStructuredMessage = StructuredMessageValues.createStructuredMessageForTesting(
            type = StructuredMessage.Type.Structured,
            content = listOf(QuickReplyTestValues.createButtonResponseContentForTesting())
        )
        val expectedButtonResponse = ButtonResponse(
            text = QuickReplyTestValues.TEXT_A,
            payload = QuickReplyTestValues.PAYLOAD_A,
            type = QuickReplyTestValues.QUICK_REPLY
        )
        val expectedMessage = Message(
            id = MessageValues.ID,
            state = State.Sent,
            type = Type.QuickReply.name,
            messageType = Type.QuickReply,
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
                QuickReplyTestValues.createQuickReplyContentForTesting(),
                QuickReplyTestValues.createButtonResponseContentForTesting(),
            )
        )
        val expectedButtonResponse = ButtonResponse(
            text = QuickReplyTestValues.TEXT_A,
            payload = QuickReplyTestValues.PAYLOAD_A,
            type = QuickReplyTestValues.QUICK_REPLY
        )
        val expectedMessage = Message(
            id = MessageValues.ID,
            state = State.Sent,
            type = Type.QuickReply.name,
            messageType = Type.QuickReply,
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
            id = MessageValues.ID,
            state = State.Sent,
            type = Type.Unknown.name,
            messageType = Type.Unknown,
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

    @Test
    fun `when MessageEntityList serialized`() {
        val givenStructuredMessage = StructuredMessage(
            id = "some_id",
            type = StructuredMessage.Type.Text,
            direction = "Inbound"
        )
        val givenMessageEntityList = MessageEntityList(
            entities = listOf(givenStructuredMessage),
            pageSize = MessageValues.PAGE_SIZE,
            pageNumber = MessageValues.PAGE_NUMBER,
            total = MessageValues.TOTAL,
            pageCount = MessageValues.PAGE_COUNT,
        )

        val expectedMessageEntityListAsJson =
            """{"entities":[{"id":"some_id","type":"Text","direction":"Inbound"}],"pageSize":25,"pageNumber":1,"total":25,"pageCount":1}"""

        val result = WebMessagingJson.json.encodeToString(givenMessageEntityList)

        assertThat(result).isEqualTo(expectedMessageEntityListAsJson)
    }

    @Test
    fun `when MessageEntityList deserialized`() {
        val givenMessageEntityListAsJson =
            """{"entities":[{"id":"some_id","type":"Text","direction":"Inbound"}],"pageSize":25,"pageNumber":1,"total":25,"pageCount":1}"""
        val expectedStructuredMessage = StructuredMessage(
            id = "some_id",
            type = StructuredMessage.Type.Text,
            direction = "Inbound"
        )
        val expectedMessageEntityList = MessageEntityList(
            entities = listOf(expectedStructuredMessage),
            pageSize = MessageValues.PAGE_SIZE,
            pageNumber = MessageValues.PAGE_NUMBER,
            total = MessageValues.TOTAL,
            pageCount = MessageValues.PAGE_COUNT,
        )

        val result =
            WebMessagingJson.json.decodeFromString<MessageEntityList>(givenMessageEntityListAsJson)

        result.run {
            assertThat(this).isEqualTo(expectedMessageEntityList)
            assertThat(entities).containsExactly(*expectedMessageEntityList.entities.toTypedArray())
            assertThat(pageSize).isEqualTo(expectedMessageEntityList.pageSize)
            assertThat(pageNumber).isEqualTo(expectedMessageEntityList.pageNumber)
            assertThat(total).isEqualTo(expectedMessageEntityList.total)
            assertThat(pageCount).isEqualTo(expectedMessageEntityList.pageCount)
        }
    }

    @Test
    fun `validate default constructor of MessageEntityList`() {
        val givenMessageEntityList = MessageEntityList(
            pageSize = MessageValues.PAGE_SIZE,
            pageNumber = MessageValues.PAGE_NUMBER,
            total = MessageValues.TOTAL,
            pageCount = MessageValues.PAGE_COUNT,
        )

        assertThat(givenMessageEntityList.entities).isEmpty()
    }

    @Test
    fun `when PreIdentifiedWebMessagingMessage serialized`() {
        val givenMPreIdentifiedWebMessagingMessage = PreIdentifiedWebMessagingMessage(
            type = MessageValues.PRE_IDENTIFIED_MESSAGE_TYPE,
            code = MessageValues.PRE_IDENTIFIED_MESSAGE_CODE,
            className = MessageValues.PRE_IDENTIFIED_MESSAGE_CLASS,
        )

        val expectedPreIdentifiedWebMessagingMessageAsJson =
            """{"type":"type","code":200,"class":"clazz"}"""

        val result = WebMessagingJson.json.encodeToString(givenMPreIdentifiedWebMessagingMessage)

        assertThat(result).isEqualTo(expectedPreIdentifiedWebMessagingMessageAsJson)
    }

    @Test
    fun `when PreIdentifiedWebMessagingMessage deserialized`() {
        val givenPreIdentifiedWebMessagingMessageAsJson =
            """{"type":"type","code":200,"class":"clazz"}"""

        val result = WebMessagingJson.json.decodeFromString<PreIdentifiedWebMessagingMessage>(
            givenPreIdentifiedWebMessagingMessageAsJson
        )

        result.run {
            assertThat(type).isEqualTo(MessageValues.PRE_IDENTIFIED_MESSAGE_TYPE)
            assertThat(code).isEqualTo(MessageValues.PRE_IDENTIFIED_MESSAGE_CODE)
            assertThat(className).isEqualTo(MessageValues.PRE_IDENTIFIED_MESSAGE_CLASS)
        }
    }

    @Test
    fun `when Channel serialized`() {
        val givenChannel = StructuredMessage.Channel(
            time = TestValues.TIME_STAMP,
            messageId = MessageValues.ID,
            type = MessageValues.TYPE,
            to = StructuredMessage.Participant(
                firstName = MessageValues.PARTICIPANT_NAME,
                lastName = MessageValues.PARTICIPANT_LAST_NAME,
                nickname = MessageValues.PARTICIPANT_NICKNAME,
                image = MessageValues.PARTICIPANT_IMAGE_URL,
            ),
            from = StructuredMessage.Participant(),
        )

        val expectedChannelAsJson =
            """{"time":"2022-08-22T19:24:26.704Z","messageId":"test_message_id","type":"Text","to":{"firstName":"participant_name","lastName":"participant_last_name","nickname":"participant_nickname","image":"http://participant.image"},"from":{}}"""

        val result = WebMessagingJson.json.encodeToString(givenChannel)

        assertThat(result).isEqualTo(expectedChannelAsJson)
    }

    @Test
    fun `when Channel deserialized`() {
        val givenChannelDefaultConstructor = StructuredMessage.Channel()
        val givenChannelAsJson =
            """{"time":"2022-08-22T19:24:26.704Z","messageId":"test_message_id","type":"Text","to":{"firstName":"participant_name","lastName":"participant_last_name","nickname":"participant_nickname","image":"http://participant.image"},"from":{}}"""
        val expectedChannel = StructuredMessage.Channel(
            time = TestValues.TIME_STAMP,
            messageId = MessageValues.ID,
            type = MessageValues.TYPE,
            to = StructuredMessage.Participant(
                firstName = MessageValues.PARTICIPANT_NAME,
                lastName = MessageValues.PARTICIPANT_LAST_NAME,
                nickname = MessageValues.PARTICIPANT_NICKNAME,
                image = MessageValues.PARTICIPANT_IMAGE_URL,
            ),
            from = StructuredMessage.Participant(),
        )

        val result =
            WebMessagingJson.json.decodeFromString<StructuredMessage.Channel>(givenChannelAsJson)

        result.run {
            assertThat(time).isEqualTo(expectedChannel.time)
            assertThat(messageId).isEqualTo(expectedChannel.messageId)
            assertThat(type).isEqualTo(expectedChannel.type)
            assertThat(to).isEqualTo(expectedChannel.to)
            to?.run {
                assertThat(firstName).isEqualTo(expectedChannel.to?.firstName)
                assertThat(lastName).isEqualTo(expectedChannel.to?.lastName)
                assertThat(nickname).isEqualTo(expectedChannel.to?.nickname)
                assertThat(image).isEqualTo(expectedChannel.to?.image)
            }
            assertThat(from).isEqualTo(expectedChannel.from)
            from?.run {
                assertThat(firstName).isNull()
                assertThat(lastName).isNull()
                assertThat(nickname).isNull()
                assertThat(image).isNull()
            }
            givenChannelDefaultConstructor.run {
                assertThat(time).isNull()
                assertThat(messageId).isNull()
                assertThat(type).isNull()
                assertThat(to).isNull()
                assertThat(from).isNull()
            }
        }
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
            enabled = true,
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
            enabled = true,
            allowedFileTypes = listOf("video/3gpp"),
            blockedFileTypes = listOf(".ade", ".adp"),
            maxFileSizeKB = 10240,
            hasWildCard = true,
        )

        val result = givenSessionResponse.toFileAttachmentProfile()

        assertThat(result).isEqualTo(expectedFileAttachmentProfile)
    }

    @Test
    fun `when PresignedUrlResponse isRefreshUrl() and headers are empty and fileSize is null`() {
        val givenPresignedUrlResponse = PresignedUrlResponse(
            attachmentId = "99999999-9999-9999-9999-999999999999",
            headers = emptyMap(),
            url = "https://downloadUrl.com",
            fileSize = null,
        )

        val result = givenPresignedUrlResponse.isRefreshUrl()

        assertThat(result).isFalse()
    }

    @Test
    fun `when PresignedUrlResponse isRefreshUrl() and headers are NOT empty and fileSize is null`() {
        val givenPresignedUrlResponse = PresignedUrlResponse(
            attachmentId = "99999999-9999-9999-9999-999999999999",
            headers = mapOf("A" to "B"),
            url = "https://downloadUrl.com",
            fileSize = null,
        )

        val result = givenPresignedUrlResponse.isRefreshUrl()

        assertThat(result).isFalse()
    }

    @Test
    fun `when PresignedUrlResponse isRefreshUrl() and headers are NOT empty and fileSize has value`() {
        val givenPresignedUrlResponse = PresignedUrlResponse(
            attachmentId = "99999999-9999-9999-9999-999999999999",
            headers = mapOf("A" to "B"),
            url = "https://downloadUrl.com",
            fileSize = 1,
        )

        val result = givenPresignedUrlResponse.isRefreshUrl()

        assertThat(result).isFalse()
    }

    @Test
    fun `when PresignedUrlResponse isRefreshUrl() and headers are empty and fileSize has value`() {
        val givenPresignedUrlResponse = PresignedUrlResponse(
            attachmentId = "99999999-9999-9999-9999-999999999999",
            headers = emptyMap(),
            url = "https://downloadUrl.com",
            fileSize = 1,
        )

        val result = givenPresignedUrlResponse.isRefreshUrl()

        assertThat(result).isTrue()
    }

    @Test
    fun `when toTransportConnectionClosedReason`() {
        assertThat(SIGNED_IN.toTransportConnectionClosedReason(false)).isEqualTo(Reason.UserSignedIn)
        assertThat(SIGNED_IN.toTransportConnectionClosedReason(true)).isEqualTo(Reason.UserSignedIn)
        assertThat(TestValues.DEFAULT_STRING.toTransportConnectionClosedReason(false)).isEqualTo(Reason.SessionLimitReached)
        assertThat(TestValues.DEFAULT_STRING.toTransportConnectionClosedReason(true)).isEqualTo(Reason.ConversationCleared)
        assertThat(null.toTransportConnectionClosedReason(true)).isEqualTo(Reason.ConversationCleared)
        assertThat(null.toTransportConnectionClosedReason(false)).isEqualTo(Reason.SessionLimitReached)
    }


    @Test
    fun `when sanitize not longer than 4 chars`() {
        val givenText = "aaaa"
        val expectedText = "aaaa"
        val resul = givenText.sanitize()
        assertThat(resul).isEqualTo(expectedText)
    }

    @Test
    fun `when sanitize longer than chars apply mask`() {
        val givenText = "aaaaa"
        val expectedText = "*aaaa"
        val resul = givenText.sanitize().sanitize()
        assertThat(resul).isEqualTo(expectedText)
    }

    @Test
    fun `when sanitize text field longer than 4 chars should apply mask`() {
           val givenText = """bla bla "text":"blaaa4aa" other"""
        val expectedText = """bla bla "text":"****a4aa" other"""
        val resul = givenText.sanitizeText()
        assertThat(resul).isEqualTo(expectedText)
    }

    @Test
    fun `when sanitize text field up to 4 chars doesn't apply mask`() {
           val givenText = """bla bla "text":"a4aa" other"""
        val expectedText = """bla bla "text":"a4aa" other"""
        val resul = givenText.sanitizeText()
        assertThat(resul).isEqualTo(expectedText)
    }

    @Test
    fun `when sanitize text field of toString longer than 4 chars should apply mask`() {
        val givenText = """bla bla text=blaaa aa4aa, other:etew"""
        val expectedText = """bla bla text=*******a4aa, other:etew"""
        val resul = givenText.sanitizeText()
        assertThat(resul).isEqualTo(expectedText)
    }

    //@Test //this would fail
    fun `when sanitize text fiel dwith comma and space of toString longer than 4 chars should apply mask`() {
        val givenText = """bla bla text=Yes I did, my secret is 12345, other:etew"""
        val expectedText = """bla bla text=*************************2345, other:etew"""
        val resul = givenText.sanitizeText()
        assertThat(resul).isEqualTo(expectedText)
    }

    @Test
    fun `when sanitize text field of toString up to 4 chars doesn't apply mask`() {
        val givenText = """bla bla text=a4aa, other=etew"""
        val expectedText = """bla bla text=a4aa, other=etew"""
        val resul = givenText.sanitizeText()
        assertThat(resul).isEqualTo(expectedText)
    }

    @Test
    fun `when sanitize token longer than 4 chars should apply mask`() {
        val givenText = """bla bla "token":"aaaaaaaaaabbbbbbbbbb1111111111222222" other"""
        val expectedText = """bla bla "token":"********************************2222" other"""
        val resul = givenText.sanitizeToken()
        assertThat(resul).isEqualTo(expectedText)
    }

    @Test
    fun `when sanitize token field not matching token format doesn't apply mask`() {
        val givenText = """bla bla "token":"a4aa" other"""
        val expectedText = """bla bla "token":"a4aa" other"""
        val resul = givenText.sanitizeToken()
        assertThat(resul).isEqualTo(expectedText)
    }

    @Test
    fun `when sanitize customAttributes field should apply mask`() {
        val givenText = """bla bla "customAttributes":{bla4aa:rrr} other"""
        val expectedText = """bla bla "customAttributes":{******:rrr} other"""
        val resul = givenText.sanitizeCustomAttributes()
        assertThat(resul).isEqualTo(expectedText)
    }

    //@Test // this would fail
    fun `when sanitize customAttributes field with apply mask`() {
        val givenText = """bla bla "customAttributes":{metadata:{key:value}} other"""
        val expectedText = """bla bla "customAttributes":{****************lue}} other"""
        val resul = givenText.sanitizeCustomAttributes()
        assertThat(resul).isEqualTo(expectedText)
    }
}
