package com.genesys.cloud.messenger.transport.util.extensions

import com.genesys.cloud.messenger.transport.Attachment
import com.genesys.cloud.messenger.transport.Message
import com.genesys.cloud.messenger.transport.Message.Direction
import com.genesys.cloud.messenger.transport.shyrka.receive.MessageEntityList
import com.genesys.cloud.messenger.transport.shyrka.receive.StructuredMessage

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
        timeStamp = this.channel?.time,
        attachments = this.content.toAttachments()
    )
}

internal fun Message.getUploadedAttachments(): Array<String> {
    if (this.attachments.isEmpty()) return emptyArray()
    return this.attachments.filter {
        it.value.state is Attachment.State.Uploaded
    }.map {
        it.key
    }.toTypedArray()
}

private fun List<StructuredMessage.Content>.toAttachments(): Map<String, Attachment> {
    return this.map {
        it.attachment.run {
            this.id to Attachment(
                id = this.id,
                fileName = this.filename,
                state = Attachment.State.Sent(this.url),
            )
        }
    }.toMap()
}
