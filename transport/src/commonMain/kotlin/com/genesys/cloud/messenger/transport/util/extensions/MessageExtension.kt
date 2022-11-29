package com.genesys.cloud.messenger.transport.util.extensions

import com.genesys.cloud.messenger.transport.core.Attachment
import com.genesys.cloud.messenger.transport.core.Message
import com.genesys.cloud.messenger.transport.core.Message.Direction
import com.genesys.cloud.messenger.transport.core.events.toTransportEvent
import com.genesys.cloud.messenger.transport.shyrka.receive.MessageEntityList
import com.genesys.cloud.messenger.transport.shyrka.receive.StructuredMessage
import com.genesys.cloud.messenger.transport.shyrka.receive.isInbound
import com.soywiz.klock.DateTime

internal fun MessageEntityList.toMessageList(): List<Message> {
    return this.entities.map {
        it.toMessage()
    }
}

internal fun StructuredMessage.toMessage(): Message {
    return Message(
        id = metadata["customMessageId"] ?: id,
        direction = if (direction == "Inbound") Direction.Inbound else Direction.Outbound,
        state = Message.State.Sent,
        type = type.name,
        text = text,
        timeStamp = channel?.time.fromIsoToEpochMilliseconds(),
        attachments = content.toAttachments(),
        events = events.map { it.toTransportEvent() },
        from = Message.Participant(
            originatingEntity.mapOriginatingEntity {
                isInbound()
            }
        )
    )
}

internal fun Message.getUploadedAttachments(): List<Message.Content> {
    if (this.attachments.isEmpty()) return emptyList()
    return this.attachments.filter {
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

private fun List<StructuredMessage.Content>.toAttachments(): Map<String, Attachment> {
    return this.associate {
        it.attachment.run {
            this.id to Attachment(
                id = this.id,
                fileName = this.filename,
                state = Attachment.State.Sent(this.url),
            )
        }
    }
}
