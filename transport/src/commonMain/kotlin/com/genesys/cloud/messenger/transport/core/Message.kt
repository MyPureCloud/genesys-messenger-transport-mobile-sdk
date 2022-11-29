package com.genesys.cloud.messenger.transport.core

import com.genesys.cloud.messenger.transport.util.Platform
import kotlinx.serialization.Serializable

/**
 *  Container class with information about message.
 *
 *  @property id a unique message identifier.
 *  @property direction the direction in which the message was sent.
 *  @property state the current message state.
 *  @property type the message type. The default type is "Text".
 *  @property text the text payload of the message.
 *  @property timeStamp the time when the message occurred represented in Unix epoch time, the number of milliseconds since January 1, 1970 UTC.
 *  @property attachments a map of [Attachment] files to the message. Empty by default.
 *  @property from the [Participant] that sends a message.
 */
@Serializable
data class Message(
    val id: String = Platform().randomUUID(),
    val direction: Direction = Direction.Inbound,
    val state: State = State.Idle,
    val type: String = "Text",
    val text: String? = null,
    val timeStamp: Long? = null,
    val attachments: Map<String, Attachment> = emptyMap(),
    val from: Participant = Participant(
        originatingEntity = Participant.OriginatingEntity.Human
    ),
) {
    /**
     * Direction of the message.
     *
     * @property Inbound when message sent by User.
     * @property Outbound when message was sent by Agent.
     */
    @Serializable
    enum class Direction {
        Inbound,
        Outbound
    }

    /**
     * Represents Message state.
     */
    @Serializable
    sealed class State {
        /**
         * Message was constructed but not sent.
         */
        object Idle : State()

        /**
         * Message was sent, but there were no confirmation of delivery or failure yet.
         */
        object Sending : State()

        /**
         * Message was successfully sent.
         */
        object Sent : State()

        /**
         * Message failed to deliver.
         *
         * @property code is Genesys error code representation of the failure.
         * @property message optional message describing reason of failure.
         */
        data class Error(val code: ErrorCode, val message: String?) : State()
    }

    /**
     * Content of the Message.
     *
     * @property contentType is a type of the Content attached to the Message.
     * @property attachment attachment itself.
     */
    @Serializable
    data class Content(
        val contentType: Type,
        val attachment: Attachment,
    ) {
        @Serializable
        enum class Type {
            Attachment
        }
    }

    /**
     * Box that contains information about conversation participant.
     */
    @Serializable
    data class Participant(
        val originatingEntity: OriginatingEntity,
    ) {
        /**
         * Participant type.
         *
         * @property Bot if message was originated from bot.
         * @property Human if message was originated from real person.
         * @property Unknown if originating entity can not be identified.
         */
        @Serializable
        enum class OriginatingEntity {
            Bot,
            Human,
            Unknown,
        }
    }
}
