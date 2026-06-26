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
import com.genesys.cloud.messenger.transport.shyrka.receive.StructuredMessage.Content.TimeSlotPickerContent.TimeSlotContent
import com.genesys.cloud.messenger.transport.shyrka.receive.isInbound
import com.genesys.cloud.messenger.transport.shyrka.send.HealthCheckID
import com.genesys.cloud.messenger.transport.util.WILD_CARD
import kotlinx.datetime.Instant

internal fun List<StructuredMessage>.toMessageList(): List<Message> =
    map { it.toMessage() }
        .filter { it.messageType != Message.Type.Unknown }

internal fun List<StructuredMessage.Content>.toButtonResponseList(): List<ButtonResponse> =
    this.filterIsInstance<ButtonResponseContent>()
        .filter { !it.buttonResponse.type.isQuickReplyType() && !it.buttonResponse.type.isCardButtonType() }
        .map { it.buttonResponse }
        .map { buttonResponse ->
            ButtonResponse(
                text = buttonResponse.text,
                payload = buttonResponse.payload,
                type = buttonResponse.type,
                originatingMessageId = buttonResponse.originatingMessageId
            )
        }

internal fun StructuredMessage.Content.TimeSlotPickerContent.toMessage() = Message.TimeSlotPicker(
    title = title,
    subtitle = subtitle,
    imageUrl = imageUrl,
    availableTimes = availableTimes.map { it.toMessage() }
)

internal fun TimeSlotContent.toMessage(): Message.TimeSlot =
    Message.TimeSlot(
        timeEpochMillis = dateTime.fromIsoToEpochMilliseconds(),
        duration = duration,
        payload = dateTime
    )

internal fun StructuredMessage.Content.ListPickerContent.ListPicker.toMessage(): Message.ListPicker =
    Message.ListPicker(
        sections = sections.map { it.toMessage() },
        receivedMessage = receivedMessage?.let {
            Message.ListPicker.ReceivedMessage(it.title, it.subtitle, it.imageUrl)
        },
        replyMessage = replyMessage?.let {
            Message.ListPicker.ReplyMessage(it.title, it.subtitle, it.imageUrl)
        },
    )

private fun StructuredMessage.Content.ListPickerContent.ListPicker.Section.toMessage(): Message.ListPicker.Section =
    Message.ListPicker.Section(
        title = title,
        multipleSelection = multipleSelection,
        items = items.map {
            Message.ListPicker.ListItem(
                id = it.id,
                title = it.title,
                subtitle = it.subtitle,
                imageUrl = it.imageUrl,
            )
        },
    )

internal fun StructuredMessage.toMessage(tracingId: String? = null): Message {
    val quickReplies = content.toQuickReplies().map { it.copy(originatingMessageId = this.id) }
    val cards = content.toCards()
    val hasCardSelection = content.hasCardSelection()
    val timePicker: Message.TimeSlotPicker? =
        content.filterIsInstance<StructuredMessage.Content.DatePickerContent>()
            .firstOrNull()?.datePicker?.toMessage()
    val buttonResponses = content.toButtonResponseList()
    val listPicker: Message.ListPicker? =
        content.filterIsInstance<StructuredMessage.Content.ListPickerContent>()
            .firstOrNull()?.listPicker?.toMessage()

    return Message(
        id = tracingId ?: id,
        direction = if (isInbound()) Direction.Inbound else Direction.Outbound,
        state = Message.State.Sent,
        messageType = type.toMessageType(
            quickReplies.isNotEmpty(),
            cards.isNotEmpty(),
            hasCardSelection,
            timePicker != null,
            buttonResponses.isNotEmpty(),
            listPicker != null,
        ),
        text = text,
        timePicker = timePicker,
        timeStamp = channel?.time.fromIsoToEpochMilliseconds(),
        attachments = content.filterIsInstance<AttachmentContent>().toAttachments(),
        quickReplies = quickReplies,
        cards = cards,
        events = events.mapNotNull { it.toTransportEvent(channel?.from) },
        from =
            Message.Participant(
                name = channel?.from?.nickname,
                imageUrl = channel?.from?.image,
                originatingEntity =
                    originatingEntity.mapOriginatingEntity {
                        isInbound()
                    }
            ),
        authenticated = metadata["authenticated"]?.toBoolean() ?: false,
        metadata = metadata,
        originatingMessageId = originatingMessageId,
        buttonResponses = buttonResponses,
        listPicker = listPicker,
    )
}

internal fun Message.getUploadedAttachments(): List<Message.Content> {
    if (attachments.isEmpty()) return emptyList()
    return attachments
        .filter { it.value.state is Attachment.State.Uploaded }
        .map { Message.Content(contentType = Message.Content.Type.Attachment, attachment = it.value) }
        .toList()
}

internal fun String?.fromIsoToEpochMilliseconds(): Long? {
    return try {
        this?.let {
            Instant.parse(it).toEpochMilliseconds()
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
            attachment.id to
                Attachment(
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

    if (filteredQuickReply.isNotEmpty()) {
        return filteredQuickReply.map {
            it.quickReply.run { ButtonResponse(text, payload, "QuickReply") }
        }
    }

    if (filteredButtonResponse.isNotEmpty()) {
        return filteredButtonResponse
            .mapNotNull { buttonResponseContent ->
                buttonResponseContent.buttonResponse.takeIf { it.type.isQuickReplyType() }
            }.map { br -> ButtonResponse(br.text, br.payload, "QuickReply") }
    }

    return emptyList()
}

private fun StructuredMessage.Content.Action.toMessageCardAction(): ButtonResponse? =
    when {
        type.equals("Link", ignoreCase = true) ->
            ButtonResponse(
                type = "Link",
                text = text,
                payload = url ?: ""
            )
        type.equals("Postback", ignoreCase = true) ||
            type.equals("Button", ignoreCase = true) ->
            ButtonResponse(
                type = "Button",
                text = text,
                payload = payload ?: ""
            )
        else -> null
    }

private fun StructuredMessage.Content.Action.mapDefaultActionIfLink(): ButtonResponse? =
    if (type.equals("Link", ignoreCase = true) && !url.isNullOrBlank()) {
        ButtonResponse(type = "Link", text = text, payload = url)
    } else {
        null
    }

private fun StructuredMessage.Content.CardContent.Card.toMessageCard(): Message.Card =
    Message.Card(
        title = title,
        description = description,
        imageUrl = image,
        actions = actions.mapNotNull { it.toMessageCardAction() },
        defaultAction = defaultAction?.mapDefaultActionIfLink()
    )

private fun List<StructuredMessage.Content>.toCards(): List<Message.Card> =
    flatMap {
        when (it) {
            is StructuredMessage.Content.CardContent -> listOf(it.card.toMessageCard())
            is StructuredMessage.Content.CarouselContent -> it.carousel.cards.map { card -> card.toMessageCard() }
            else -> emptyList()
        }
    }

private fun StructuredMessage.Type.toMessageType(
    hasQuickReplies: Boolean,
    hasCards: Boolean,
    hasCardSelection: Boolean,
    hasTimePicker: Boolean,
    hasButtonResponse: Boolean = false,
    hasListPicker: Boolean = false,
): Message.Type =
    when (this) {
        StructuredMessage.Type.Text -> Message.Type.Text
        StructuredMessage.Type.Event -> Message.Type.Event
        StructuredMessage.Type.Structured -> {
            when {
                hasTimePicker -> Message.Type.DatePicker
                hasListPicker -> Message.Type.ListPicker
                hasQuickReplies -> Message.Type.QuickReply
                hasCards || hasCardSelection -> Message.Type.Cards
                hasButtonResponse -> Message.Type.ButtonResponse
                else -> Message.Type.Unknown
            }
        }
    }

private fun String.isQuickReplyType(): Boolean = this.equals("QuickReply", true)

private fun String.isCardButtonType() = this.equals("Button", true)

private fun List<StructuredMessage.Content>.hasCardSelection(): Boolean =
    this
        .filterIsInstance<ButtonResponseContent>()
        .any { it.buttonResponse.type.isCardButtonType() }

internal fun String.isHealthCheckResponseId(): Boolean = this == HealthCheckID

internal fun Message.isOutbound(): Boolean = this.direction == Direction.Outbound

internal fun SessionResponse.toFileAttachmentProfile(): FileAttachmentProfile {
    val allowedFileTypes =
        allowedMedia
            ?.inbound
            ?.fileTypes
            ?.map { it.type }
            ?.toMutableList() ?: mutableListOf()
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
