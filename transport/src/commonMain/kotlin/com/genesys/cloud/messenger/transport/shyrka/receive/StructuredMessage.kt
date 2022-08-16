package com.genesys.cloud.messenger.transport.shyrka.receive

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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
    val metadata: Map<String, String> = emptyMap()
) {
    @Serializable
    data class Participant(
        val firstName: String? = null,
        val lastName: String? = null,
        val nickname: String? = null,
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

    @Serializable
    data class Content(
        val contentType: String,
        val attachment: Attachment,
    )

    @Serializable
    enum class Type {
        @SerialName("Text")
        Text,
        @SerialName("Event")
        Event,
    }
}
