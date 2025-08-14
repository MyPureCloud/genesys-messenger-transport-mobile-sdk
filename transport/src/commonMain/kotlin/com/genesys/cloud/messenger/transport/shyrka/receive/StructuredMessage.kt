package com.genesys.cloud.messenger.transport.shyrka.receive

import com.genesys.cloud.messenger.transport.core.Message.Direction
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

@Serializable
internal data class MessageEntityList(
    val entities: List<StructuredMessage> = emptyList(),
    val pageSize: Int,
    val pageNumber: Int,
    val total: Int,
    val pageCount: Int,
)

@Serializable
internal data class StructuredMessage(
    val id: String,
    val type: Type,
    val text: String? = null,
    val direction: String,
    val channel: Channel? = null,
    val content: List<Content> = emptyList(),
    val metadata: Map<String, String> = emptyMap(),
    val events: List<StructuredMessageEvent> = emptyList(),
    val originatingEntity: String? = null,
) {
    @Serializable
    data class Participant(
        val firstName: String? = null,
        val lastName: String? = null,
        val nickname: String? = null,
        val image: String? = null,
    )

    @Serializable
    data class Channel(
        val time: String? = null,
        val messageId: String? = null,
        val type: String? = null,
        val to: Participant? = null,
        val from: Participant? = null,
    )

    @Serializable
    enum class Type {
        @SerialName("Text")
        Text,
        @SerialName("Event")
        Event,
        @SerialName("Structured")
        Structured,
    }

    @Serializable(with = ContentSerializer::class)
    internal sealed class Content {
        @Serializable
        enum class Type {
            Attachment,
            QuickReply,
            ButtonResponse,
            Card,
            Carousel,
        }

        @Serializable
        data class AttachmentContent(
            val contentType: String,
            val attachment: Attachment,
        ) : Content() {
            @Serializable
            data class Attachment(
                val id: String,
                val url: String,
                val filename: String,
                val fileSize: Int? = null,
                val mediaType: String,
                val mime: String? = null,
                val sha256: String? = null,
                val text: String? = null,
            )
        }

        @Serializable
        data class QuickReplyContent(
            val contentType: String,
            val quickReply: QuickReply,
        ) : Content() {
            @Serializable
            data class QuickReply(
                val text: String,
                val payload: String,
                val action: String,
            )
        }

        @Serializable
        data class ButtonResponseContent(
            val contentType: String,
            val buttonResponse: ButtonResponse,
        ) : Content() {
            @Serializable
            data class ButtonResponse(
                val text: String,
                val payload: String,
                val type: String,
            )
        }

        @Serializable
        data class CardContent(
            val contentType: String,
            val card: Card,
        ) : Content() {
            @Serializable
            data class Card(
                val title: String,
                val description: String,
                val image: String? = null,
                val defaultAction: Action? = null,
                val actions: List<Action>
            )
        }

        @Serializable
        data class Action(
            val type: String,
            val text: String? = null,
            val url: String? = null,
            val payload: String? = null,
        )

        @Serializable
        data class CarouselContent(
            val contentType: String,
            val carousel: Carousel,
        ) : Content() {
            @Serializable
            data class Carousel(
                val cards: List<CardContent.Card>
            )
        }

        @Serializable
        internal data object UnknownContent : Content()
    }

    internal object ContentSerializer :
        JsonContentPolymorphicSerializer<Content>(Content::class) {
        override fun selectDeserializer(element: JsonElement): DeserializationStrategy<Content> {
            return when (element.jsonObject["contentType"]?.jsonPrimitive?.content) {
                Content.Type.Attachment.name -> Content.AttachmentContent.serializer()
                Content.Type.QuickReply.name -> Content.QuickReplyContent.serializer()
                Content.Type.ButtonResponse.name -> Content.ButtonResponseContent.serializer()
                Content.Type.Card.name -> Content.CardContent.serializer()
                Content.Type.Carousel.name -> Content.CarouselContent.serializer()
                else -> Content.UnknownContent.serializer()
            }
        }
    }
}

internal fun StructuredMessage.isOutbound(): Boolean = direction == Direction.Outbound.name

internal fun StructuredMessage.isInbound(): Boolean = direction == Direction.Inbound.name
