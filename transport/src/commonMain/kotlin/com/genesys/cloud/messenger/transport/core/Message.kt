package com.genesys.cloud.messenger.transport.core

import com.genesys.cloud.messenger.transport.util.Platform
import kotlinx.serialization.Serializable

/**
 *  Container class with information about message.
 */
@Serializable
data class Message(
    val id: String = Platform().randomUUID(),
    val direction: Direction = Direction.Inbound,
    val state: State = State.Idle,
    val type: String = "Text",
    val text: String? = null,
    val timeStamp: String? = null,
    val attachments: Map<String, Attachment> = emptyMap(),
) {
    /**
     * Direction of the message.
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
         * Message was sent, but there were no confirmation of delivery or failure.
         */
        object Sending : State()

        /**
         * Message was successfully received by the other party.
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
}
