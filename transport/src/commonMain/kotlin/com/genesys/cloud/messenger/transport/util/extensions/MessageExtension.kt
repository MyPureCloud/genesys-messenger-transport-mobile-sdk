package com.genesys.cloud.messenger.transport.util.extensions

import com.genesys.cloud.messenger.transport.core.Attachment
import com.genesys.cloud.messenger.transport.core.ButtonResponse
import com.genesys.cloud.messenger.transport.core.FileAttachmentProfile
import com.genesys.cloud.messenger.transport.core.Message
import com.genesys.cloud.messenger.transport.core.Message.Direction
import com.genesys.cloud.messenger.transport.core.events.toTransportEvent
import com.genesys.cloud.messenger.transport.shyrka.receive.FileUpload
import com.genesys.cloud.messenger.transport.shyrka.receive.PresignedUrlResponse
import com.genesys.cloud.messenger.transport.shyrka.receive.SessionResponse
import com.genesys.cloud.messenger.transport.shyrka.receive.StructuredMessage
import com.genesys.cloud.messenger.transport.shyrka.receive.StructuredMessage.Content.AttachmentContent
import com.genesys.cloud.messenger.transport.shyrka.receive.StructuredMessage.Content.ButtonResponseContent
import com.genesys.cloud.messenger.transport.shyrka.receive.StructuredMessage.Content.QuickReplyContent
import com.genesys.cloud.messenger.transport.shyrka.receive.isInbound
import com.genesys.cloud.messenger.transport.shyrka.send.HealthCheckID
import com.genesys.cloud.messenger.transport.util.WILD_CARD
import com.soywiz.klock.DateTime

internal fun List<StructuredMessage>.toMessageList(): List<Message> =
    map { it.toMessage() }
        .filter { it.messageType != Message.Type.Unknown }

internal fun StructuredMessage.toMessage(): Message {
    val quickReplies = content.toQuickReplies()
    val cards = content.toCards()
    val carousel = content.toCarousel()
    val cardsAndCarousel = cards + carousel
    return Message(
        id = metadata["customMessageId"] ?: id,
        direction = if (isInbound()) Direction.Inbound else Direction.Outbound,
        state = Message.State.Sent,
        messageType = type.toMessageType(quickReplies.isNotEmpty(), carousel.isNotEmpty()),
        text = text,
        timeStamp = channel?.time.fromIsoToEpochMilliseconds(),
        attachments = content.filterIsInstance<AttachmentContent>().toAttachments(),
        quickReplies = quickReplies,
        cards = cardsAndCarousel,
        events = events.mapNotNull { it.toTransportEvent(channel?.from) },
        from = Message.Participant(
            name = channel?.from?.nickname,
            imageUrl = channel?.from?.image,
            originatingEntity = originatingEntity.mapOriginatingEntity {
                isInbound()
            }
        ),
        authenticated = metadata["authenticated"].toBoolean()
    )
}

internal fun Message.getUploadedAttachments(): List<Message.Content> {
    if (attachments.isEmpty()) return emptyList()
    return attachments.filter {
        it.value.state is Attachment.State.Uploaded
    }.map {
        Message.Content(contentType = Message.Content.Type.Attachment, attachment = it.value)
    }.toList()
}

internal fun String?.fromIsoToEpochMilliseconds(): Long? {
    return try {
        this?.let {
            DateTime.fromString(it).local.unixMillisLong
        }
    } catch (t: Throwable) {
        null
    }
}

internal fun String?.mapOriginatingEntity(isInbound: () -> Boolean): Message.Participant.OriginatingEntity {
    return when {
        isInbound() -> Message.Participant.OriginatingEntity.Human
        this == "Human" -> Message.Participant.OriginatingEntity.Human
        this == "Bot" -> Message.Participant.OriginatingEntity.Bot
        else -> Message.Participant.OriginatingEntity.Unknown
    }
}

private fun List<AttachmentContent>.toAttachments(): Map<String, Attachment> {
    return this.associate {
        it.run {
            attachment.id to Attachment(
                id = attachment.id,
                fileName = attachment.filename,
                fileSizeInBytes = attachment.fileSize,
                state = Attachment.State.Sent(attachment.url),
            )
        }
    }
}

private fun List<StructuredMessage.Content>.toQuickReplies(): List<ButtonResponse> {
    val filteredQuickReply = this.filterIsInstance<QuickReplyContent>()
    val filteredButtonResponse = this.filterIsInstance<ButtonResponseContent>()
    return when {
        filteredQuickReply.isNotEmpty() -> filteredQuickReply.map {
            it.quickReply.run { ButtonResponse(text, payload, "QuickReply") }
        }

        filteredButtonResponse.isNotEmpty() -> filteredButtonResponse.map {
            it.buttonResponse.run { ButtonResponse(text, payload, type) }
        }

        else -> emptyList()
    }
}

private fun List<StructuredMessage.Content>.toCards(): List<Message.Card> {
    return this.filterIsInstance<StructuredMessage.Content.CardContent>().map {
        Message.Card(
            title = it.card.title.filterNot{it.isWhitespace()},
            description = it.card.description,
            imageUrl = it.card.image,
            actions = it.card.actions.map { action ->
                Message.Card.Action(
                    type = action.type,
                    title = action.title,
                    url = action.url,
                    payload = action.payload
                )
            }
        )
    }
}

private fun List<StructuredMessage.Content>.toCarousel(): List<Message.Card> {
    val carousel = this.filterIsInstance<StructuredMessage.Content.CarouselContent>().map {
        Message.Carousel(
            cards = it.carousel.cards.map { card ->
                Message.Card(
                    title = card.title.filterNot{it.isWhitespace()},
                    description = card.description,
                    imageUrl = card.image,
                    actions = card.actions.map { action ->
                        Message.Card.Action(
                            type = action.type,
                            title = action.title,
                            url = action.url,
                            payload = action.payload
                        )
                    }
                )
            }
        )
    }
    return carousel.flatMap { carousel ->
        carousel.cards
    }
}

private fun StructuredMessage.Type.toMessageType(hasQuickReplies: Boolean, hasCarousel: Boolean): Message.Type =
    when (this) {
        StructuredMessage.Type.Text -> Message.Type.Text
        StructuredMessage.Type.Event -> Message.Type.Event
        StructuredMessage.Type.Structured -> {
            if (hasQuickReplies) Message.Type.QuickReply
            else if (hasCarousel) Message.Type.Carousel
            else Message.Type.Unknown
        }
    }

internal fun String.isHealthCheckResponseId(): Boolean = this == HealthCheckID

internal fun Message.isOutbound(): Boolean = this.direction == Direction.Outbound

internal fun SessionResponse.toFileAttachmentProfile(): FileAttachmentProfile {
    val allowedFileTypes = allowedMedia?.inbound?.fileTypes?.map { it.type }?.toMutableList() ?: mutableListOf()
    val maxFileSize = allowedMedia?.inbound?.maxFileSizeKB ?: 0
    val enabled = allowedFileTypes.isNotEmpty() && maxFileSize > 0
    val hasWildcard = allowedFileTypes.remove(WILD_CARD)
    return FileAttachmentProfile(
        enabled = enabled,
        allowedFileTypes = allowedFileTypes,
        blockedFileTypes = blockedExtensions,
        maxFileSizeKB = maxFileSize,
        hasWildCard = hasWildcard
    )
}

internal fun FileUpload?.toFileAttachmentProfile(): FileAttachmentProfile {
    if (this == null) return FileAttachmentProfile()
    val allowedFileTypes = modes.flatMap { it.fileTypes }.toMutableList()
    val enabled = allowedFileTypes.isNotEmpty()
    val hasWildcard = allowedFileTypes.remove(WILD_CARD)
    return FileAttachmentProfile(
        enabled = enabled,
        allowedFileTypes = allowedFileTypes,
        blockedFileTypes = emptyList(),
        maxFileSizeKB = modes.firstOrNull()?.maxFileSizeKB ?: 0,
        hasWildCard = hasWildcard
    )
}

internal fun PresignedUrlResponse.isRefreshUrl(): Boolean {
    return headers.isEmpty() && fileSize != null
}

/**
 * Replaces characters with stars (*) except for the last 4 characters
 */
internal fun String.sanitize(): String {
    val lastChars = 4
    if (this.length <= lastChars) {
        return this // Nothing to sanitize if length is lastChars or fewer characters
    }

    return "*".repeat(this.length - lastChars) + this.takeLast(lastChars)
}

fun String.sanitizeSensitiveData(): String =
    this.sanitizeToken().sanitizeText().sanitizeCustomAttributes()

internal fun String.sanitizeCustomAttributes(): String {
    val regex = """("customAttributes":\{)(.*?)(\})""".toRegex()
    return this.replace(regex) {
        """${it.groupValues[1]}${it.groupValues[2].sanitize()}${it.groupValues[3]}"""
    }
}

internal fun String.sanitizeText(): String {
    var regex = """("text":")([^"]*)(")""".toRegex()
    var sanitizedInput = this.replace(regex) {
        """${it.groupValues[1]}${it.groupValues[2].sanitize()}${it.groupValues[3]}"""
    }
    regex = """(text=)(.*?)(?=(?:, \w+:)|$|[)])""".toRegex()
    sanitizedInput = sanitizedInput.replace(regex) {
        """${it.groupValues[1]}${it.groupValues[2].sanitize()}"""
    }
    return sanitizedInput
}

internal fun String.sanitizeToken(): String {
    val tokenRegex = """("token":")([a-fA-F0-9-]{36})(")""".toRegex()
    return this.replace(tokenRegex) {
        """${it.groupValues[1]}${it.groupValues[2].sanitize()}${it.groupValues[3]}"""
    }
}

internal fun Map<String, String>.sanitizeValues(): Map<String, String> {
    return this.mapValues { (_, value) ->
        value.sanitize()
    }
}
