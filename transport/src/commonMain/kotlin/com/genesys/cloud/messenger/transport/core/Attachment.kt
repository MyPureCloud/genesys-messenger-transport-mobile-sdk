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
    @Transient val fileSize: Int? = null,
    @Transient val state: State = State.Presigning,
) {
    /**
     * Represent Attachment states.
     */
    @Serializable
    sealed class State {
        /**
         * Attachment was requested to be detached from the message,
         * but there were no confirmation of success or failure yet.
         */
        object Detaching : State()

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
         * Attachment url was successfully refreshed.
         *
         * @param downloadUrl is a refreshed url pointing to uploaded attachment.
         */
        data class Refreshed(val downloadUrl: String) : State()

        /**
         * Message that holds this attachment was sent, but there were no confirmation of delivery or failure yet.
         */
        object Sending : State()

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
