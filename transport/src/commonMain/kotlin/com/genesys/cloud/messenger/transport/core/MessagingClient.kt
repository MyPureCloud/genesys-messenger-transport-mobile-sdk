package com.genesys.cloud.messenger.transport.core

/**
 * The main SDK interface providing bi-directional communication between a guest and Genesys Cloud
 * via Web Messaging service.
 */
interface MessagingClient {

    /**
     * Container that holds all possible MessagingClient states.
     */
    sealed class State {
        /**
         * MessagingClient has been instantiated and has not attempted to connect.
         */
        object Idle : State()

        /**
         * Trying to establish secure connection via WebSocket.
         */
        object Connecting : State()

        /**
         * Secure connection with WebSocket was opened.
         */
        object Connected : State()

        /**
         * Session was successfully configured.
         *
         * @property connected true if session has been configured and connection is established.
         * @property newSession indicates if configured session is new. When configuring an existing session, [newSession] will be false.
         */
        data class Configured(val connected: Boolean, val newSession: Boolean?) : State()

        /**
         * Remote peer has indicated that no more incoming messages will be transmitted.
         */
        data class Closing(val code: Int, val reason: String) : State()

        /**
         * Both peers have indicated that no more messages will be transmitted and the connection has been successfully released.
         */
        data class Closed(val code: Int, val reason: String) : State()

        /**
         * In case of fatal, unrecoverable errors MessagingClient will transition to this state.
         *
         * @property code the [ErrorCode.WebsocketError] for websocket errors.
         * @property message is an optional message.
         */
        data class Error(val code: ErrorCode, val message: String?) : State()
    }

    /**
     * The current state of the MessagingClient.
     */
    val currentState: State

    /**
     * Listener for MessagingClient state changes.
     */
    var stateListener: ((State) -> Unit)?

    /**
     * Listener for Message events.
     */
    var messageListener: ((MessageEvent) -> Unit)?

    /**
     * Message that is currently in progress of being sent.
     */
    val pendingMessage: Message

    /**
     * Immutable Collection containing all Messages from the current conversation.
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
     * Attach a file to the message. This file will be uploaded and cached locally
     * until user decides to send a message.
     * After the message has been sent, attachment will be cleared from cache.
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
     * Detach file from message. If file was already uploaded it will be deleted.
     * If file is uploading now, the process will be stopped.
     *
     * @param attachmentId the ID of the attachment to remove
     * @throws IllegalStateException if called before session was connected.
     */
    @Throws(IllegalStateException::class)
    fun detach(attachmentId: String)

    /**
     * Get message history for a conversation.
     *
     * @throws Exception
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
     * Reset current [conversation] and history request pagination index.
     * Note! After calling this function, the next call to [fetchNextPage] will request the
     * latest available history.
     */
    fun invalidateConversationCache()
}
