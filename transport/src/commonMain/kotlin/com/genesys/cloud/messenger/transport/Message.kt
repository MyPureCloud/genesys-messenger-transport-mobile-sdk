package com.genesys.cloud.messenger.transport

import com.genesys.cloud.messenger.transport.util.ErrorCode
import com.genesys.cloud.messenger.transport.util.Platform
import kotlinx.serialization.Serializable

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
    enum class Direction {
        Inbound,
        Outbound
    }

    @Serializable
    sealed class State {
        object Idle : State()
        object Sending : State()
        object Sent : State()
        data class Error(val code: ErrorCode, val message: String?) : State()
    }
}
