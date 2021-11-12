package com.genesys.cloud.messenger.transport

import com.genesys.cloud.messenger.transport.util.ErrorCode
import kotlinx.serialization.Serializable

@Serializable
data class Attachment(
    val id: String,
    val fileName: String,
    val state: State,
) {
    @Serializable
    sealed class State {
        object Deleted : State()
        object Detached : State()
        object Presigning : State()
        object Uploading : State()
        data class Uploaded(val downloadUrl: String) : State()
        data class Sent(val downloadUrl: String) : State()
        data class Error(val errorCode: ErrorCode, val errorMessage: String) : State()
    }
}
