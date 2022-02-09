package com.genesys.cloud.messenger.transport.core

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

/**
 * Container for attachment related information.
 */
@Serializable
data class Attachment(
    val id: String,
    @Transient val fileName: String? = null,
    @Transient val state: State = State.Presigning,
) {
    /**
     * Represent Attachment states.
     */
    @Serializable
    sealed class State {
        /**
         * Attachment was deleted from the conversation history.
         */
        object Deleted : State()

        /**
         * Attachment was detached from the Message.
         */
        object Detached : State()

        /**
         * Obtaining the presigned url for the attachment.
         */
        object Presigning : State()

        /**
         * Uploading attachment to the server.
         */
        object Uploading : State()

        /**
         * Attachment was successfully uploaded to the server and is ready to be sent with the message.
         *
         * @param downloadUrl is a url pointing to uploaded attachment.
         */
        data class Uploaded(val downloadUrl: String) : State()

        /**
         * Attachment was sent.
         *
         * @param downloadUrl is a url pointing to uploaded attachment.
         */
        data class Sent(val downloadUrl: String) : State()

        /**
         * Attachment process failed.
         *
         * @param errorCode is an error code representing reason of the failure.
         * @param errorMessage is a detail error message.
         */
        data class Error(val errorCode: ErrorCode, val errorMessage: String) : State()
    }
}
