package com.genesys.cloud.messenger.transport.util.extensions

import com.genesys.cloud.messenger.transport.core.Attachment
import com.genesys.cloud.messenger.transport.core.Message
import com.genesys.cloud.messenger.transport.core.Message.Direction
import com.genesys.cloud.messenger.transport.shyrka.receive.MessageEntityList
import com.genesys.cloud.messenger.transport.shyrka.receive.StructuredMessage
import com.soywiz.klock.DateTime

internal fun MessageEntityList.toMessageList(): List<Message> {
    return this.entities.map {
        it.toMessage()
    }
}

internal fun StructuredMessage.toMessage(): Message {
    return Message(
        id = this.metadata["customMessageId"] ?: this.id,
        direction = if (this.direction == "Inbound") Direction.Inbound else Direction.Outbound,
        state = Message.State.Sent,
        type = this.type,
        text = this.text,
        timeStamp = this.channel?.time.fromIsoToEpochMilliseconds(),
        attachments = this.content.toAttachments()
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
