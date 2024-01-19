package com.genesys.cloud.messenger.transport.util.extensions

import com.genesys.cloud.messenger.transport.core.Attachment
import com.genesys.cloud.messenger.transport.core.ButtonResponse
import com.genesys.cloud.messenger.transport.core.Message
import com.genesys.cloud.messenger.transport.core.Message.Direction
import com.genesys.cloud.messenger.transport.core.events.toTransportEvent
import com.genesys.cloud.messenger.transport.shyrka.receive.StructuredMessage
import com.genesys.cloud.messenger.transport.shyrka.receive.StructuredMessage.Content.AttachmentContent
import com.genesys.cloud.messenger.transport.shyrka.receive.StructuredMessage.Content.ButtonResponseContent
import com.genesys.cloud.messenger.transport.shyrka.receive.StructuredMessage.Content.QuickReplyContent
import com.genesys.cloud.messenger.transport.shyrka.receive.isInbound
import com.genesys.cloud.messenger.transport.shyrka.send.HealthCheckID
import com.soywiz.klock.DateTime

internal fun List<StructuredMessage>.toMessageList(): List<Message> = map { it.toMessage() }

internal fun StructuredMessage.toMessage(): Message {
    val quickReplies = content.toQuickReplies()
    return Message(
        id = metadata["customMessageId"] ?: id,
        direction = if (isInbound()) Direction.Inbound else Direction.Outbound,
        state = Message.State.Sent,
        messageType = type.toMessageType(quickReplies.isNotEmpty()),
        text = text,
        timeStamp = channel?.time.fromIsoToEpochMilliseconds(),
        attachments = content.filterIsInstance<AttachmentContent>().toAttachments(),
        quickReplies = quickReplies,
        events = events.mapNotNull { it.toTransportEvent() },
        from = Message.Participant(
            name = channel?.from?.nickname,
            imageUrl = channel?.from?.image,
            originatingEntity = originatingEntity.mapOriginatingEntity {
                isInbound()
            }
        )
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

private fun StructuredMessage.Type.toMessageType(hasQuickReplies: Boolean): Message.Type =
    when (this) {
        StructuredMessage.Type.Text -> Message.Type.Text
        StructuredMessage.Type.Event -> Message.Type.Event
        StructuredMessage.Type.Structured -> if (hasQuickReplies) Message.Type.QuickReply else Message.Type.Unknown
    }

internal fun String.isHealthCheckResponseId(): Boolean = this == HealthCheckID

internal fun Message.isOutbound(): Boolean = this.direction == Direction.Outbound
