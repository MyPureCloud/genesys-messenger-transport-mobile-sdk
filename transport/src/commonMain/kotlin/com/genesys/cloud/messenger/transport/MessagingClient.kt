package com.genesys.cloud.messenger.transport

import com.genesys.cloud.messenger.transport.shyrka.receive.DeploymentConfig
import com.genesys.cloud.messenger.transport.util.ErrorCode
import io.ktor.client.features.ResponseException
import kotlin.coroutines.cancellation.CancellationException

/**
 * WebMessagingClient provides bi-directional communication between a guest and Genesys Cloud via
 * Web Messaging service.
 */
interface MessagingClient {

    /**
     * MessagingClient state.
     */
    sealed class State {
        object Idle : State()
        object Connecting : State()
        object Connected : State()
        data class Configured(val connected: Boolean, val newSession: Boolean?) : State()
        data class Closing(val code: Int, val reason: String) : State()
        data class Closed(val code: Int, val reason: String) : State()
        data class Error(val code: ErrorCode, val message: String?) : State()
    }

    /**
     * The current state of the MessagingClient.
     */
    val currentState: State

    /**
     * Listener for client state changes.
     */
    var stateListener: ((State) -> Unit)?

    /**
     * Message object that is currently under construct.
     * [sendMessage] command dispatch message based on the values stored in pending message.
     */
    val pendingMessage: Message

    /**
     * Immutable Collection containing all Messages from current conversation.
     */
    val conversation: List<Message>

    /**
     * Open a secure WebSocket connection to the Web Messaging service with the url and
     * deploymentId configured on this MessagingClient instance.
     *
     * @throws IllegalStateException
     */
    @Throws(IllegalStateException::class)
    fun connect()

    /**
     * Configure a Web Messaging session.
     */
    @Throws(IllegalStateException::class)
    fun configureSession()

    /**
     * Send a message to the conversation as plain text.
     *
     * @param text the plain text to send.
     * @throws IllegalStateException
     */
    @Throws(IllegalStateException::class)
    fun sendMessage(text: String)

    /**
     * Perform a health check of the connection by sending an echo message.
     *
     * @throws IllegalStateException
     */
    @Throws(IllegalStateException::class)
    fun sendHealthCheck()

    /**
     * Attach file to the message. This file will be uploaded and cached locally
     * until user decides to send a message.
     * After message has been sent, attachment will be cleared from cache.
     *
     * @param byteArray data to upload.
     * @param fileName the name of the file to upload. Has to include file extension type
     * for instance: example.png
     * @param uploadProgress optional callback to track attachment upload progress.
     *
     * @return internally generated attachmentId. Can be used to track upload progress
     */
    @Throws(IllegalStateException::class)
    fun attach(
        byteArray: ByteArray,
        fileName: String,
        uploadProgress: ((Float) -> Unit)? = null
    ): String

    /**
     * Detach file from message. If file was already uploaded
     * or is uploading now - process will be stopped
     * and [deleteAttachment] will be called.
     *
     * @param attachmentId the ID of the attachment to remove
     */
    fun detach(attachmentId: String)

    /**
     * Request a new and valid URL to download an attachment that was previously uploaded. Download
     * URLs expire.
     *
     * @param attachmentId the ID of the attachment
     * @throws IllegalStateException
     */
    @Throws(IllegalStateException::class)
    fun generateDownloadUrl(attachmentId: String)

    /**
     * Attachments typically expire 72 hours after being uploaded. This method can be used to
     * immediately delete the file prior to that expiration.
     *
     * @param attachmentId the ID of the attachment
     * @throws IllegalStateException
     */
    @Throws(IllegalStateException::class)
    fun deleteAttachment(attachmentId: String)

    /**
     * Get message history for a conversation.
     *
     * @throws CancellationException
     */
    @Throws(Exception::class)
    suspend fun fetchNextPage()

    /**
     * Close the WebSocket connection to the Web Messaging service.
     *
     * @throws IllegalStateException
     */
    @Throws(IllegalStateException::class)
    fun disconnect()

    /**
     *  Fetch deployment configuration based on deployment id.
     */
    @Throws(Exception::class)
    suspend fun fetchDeploymentConfig(): DeploymentConfig
}
