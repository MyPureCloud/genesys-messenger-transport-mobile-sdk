package com.genesys.cloud.messenger.transport.core

import com.genesys.cloud.messenger.transport.core.events.Event

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
        data object Idle : State()

        /**
         * Trying to establish secure connection via WebSocket.
         */
        data object Connecting : State()

        /**
         * Secure connection with WebSocket was opened.
         */
        data object Connected : State()

        /**
         * Trying to reconnect after WebSocket failure.
         */
        data object Reconnecting : State()

        /**
         * Session was successfully configured.
         *
         * @property connected true if session has been configured and connection is established.
         * @property newSession indicates if configured session is new. When configuring an existing session, [newSession] will be false.
         */
        data class Configured(
            val connected: Boolean,
            val newSession: Boolean,
        ) : State()

        /**
         * MessagingClient will transition to this state, once [Event.ConversationDisconnect] is received AND
         * messenger is configured to display conversation status and disconnect session OR when SessionResponse indicates that session is `readOnly=true`.
         * For more information on this state, please visit official [documentation](https://developer.genesys.cloud/commdigital/digital/webmessaging/messenger-transport-mobile-sdk/)
         * While in this state, it is only allowed to perform read actions. For example, [fetchNextPage].
         *
         * An IllegalStateException will be thrown on any attempt to send an action.
         */
        data object ReadOnly : State()

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
         * @property code the [ErrorCode] representing the reason for the failure.
         * @property message an optional message describing the error.
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
    var stateChangedListener: ((StateChange) -> Unit)?

    /**
     * Listener for Message events.
     */
    var messageListener: ((MessageEvent) -> Unit)?

    /**
     * Listener for Transport events.
     */
    var eventListener: ((Event) -> Unit)?

    /**
     * Message that is currently in progress of being sent.
     */
    val pendingMessage: Message

    /**
     * Immutable Collection containing all Messages from the current conversation.
     */
    val conversation: List<Message>

    /**
     * This object provides convenience methods for adding, getting,
     * and managing custom attributes. It tracks the state of custom attributes and can be used to
     * send them when necessary.
     *
     * Note: once added or updated - custom attributes will be dispatched along with the next message or autostart event.
     */
    val customAttributesStore: CustomAttributesStore

    /**
     * Represents the configuration for supported content profiles in the Admin Console.
     *
     * This property contains information about the allowed and blocked file extensions, maximum file size,
     * and whether a wildcard is present in the allowed file types list.
     *
     * If an attempt is made to upload a file with an extension that is not included in the `allowedFileTypes` list,
     * it will result in an `IllegalArgumentException` when using the [attach] function.
     *
     * NOTE: `fileAttachmentProfile` should be accessible (not null) once `MessagingClient` has transitioned to `State.Configured`.
     */
    val fileAttachmentProfile: FileAttachmentProfile?

    /**
     * This property helps to indicate if previously connected session was guest or authenticated.
     */
    val wasAuthenticated: Boolean

    /**
     * Open and Configure a secure WebSocket connection to the Web Messaging service with the url and
     * deploymentId configured on this MessagingClient instance.
     *
     * @throws IllegalStateException If the current state of the MessagingClient is not compatible with the requested action.
     */
    @Throws(IllegalStateException::class)
    fun connect()

    /**
     * Open and configure an authenticated and secure WebSocket connection to the Web Messaging service using the url and deploymentId
     * configured on this MessagingClient instance. Note, once Authenticated session is configured it can not be downgraded to Anonymous session.
     * When called on a session that was previously configured as anonymous/guest, it will perform a Step-Up.
     *
     * @throws IllegalStateException If the current state of the MessagingClient is not compatible with the requested action.
     */
    @Throws(IllegalStateException::class)
    fun connectAuthenticatedSession()

    /**
     * Performs a step-up of from existing anonymous/guest session to an authenticated session.
     * To perform this action, the MessagingClient must be in State.Configured and authorized(see [authorize] function)
     *
     * @throws IllegalStateException If the current state of the MessagingClient is not compatible with the requested action.
     */
    @Throws(IllegalStateException::class)
    fun stepUpToAuthenticatedSession()

    /**
     * Configure a new chat once the previous one is in State.ReadOnly.
     * It is important to note that once new chat started,
     * all information regarding previous conversations will be lost.
     *
     * @throws IllegalStateException if MessagingClient not in ReadOnly state.
     */
    @Throws(IllegalStateException::class)
    fun startNewChat()

    /**
     * Send a message to the conversation as plain text.
     *
     * @param text the plain text to send.
     * @param customAttributes optional dictionary of attributes to send with the message. Empty by default.
     * @throws IllegalStateException If the current state of the MessagingClient is not compatible with the requested action.
     */
    @Throws(IllegalStateException::class)
    fun sendMessage(text: String, customAttributes: Map<String, String> = emptyMap())

    /**
     * Send a quick reply to the Agent/Bot.
     *
     * @param buttonResponse the quick reply to send.
     * @throws IllegalStateException If the current state of the MessagingClient is not compatible with the requested action.
     */
    @Throws(IllegalStateException::class)
    fun sendQuickReply(buttonResponse: ButtonResponse)

    /**
     * Perform a health check of the connection by sending an echo message.
     * This command sends a single echo request and should be called a maximum of once every 30 seconds.
     * If called more frequently, this command will be rate limited in order to optimize network traffic.
     *
     * @throws IllegalStateException If the current state of the MessagingClient is not compatible with the requested action.
     */
    @Throws(IllegalStateException::class)
    fun sendHealthCheck()

    /**
     * Attach a file to the message. This file will be uploaded and cached locally
     * until customer decides to send a message.
     * After the message has been sent, attachment will be cleared from cache.
     *
     * @param byteArray data to upload.
     * @param fileName the name of the file to upload. Has to include file extension type
     * for instance: example.png
     * @param uploadProgress optional callback to track attachment upload progress.
     *
     * @throws IllegalStateException If the current state of the MessagingClient is not compatible with the requested action.
     * @throws IllegalArgumentException If provided file size exceeds [FileAttachmentProfile.maxFileSizeKB].
     * @return internally generated attachmentId. Can be used to track upload progress
     */
    @Throws(IllegalStateException::class, IllegalArgumentException::class)
    fun attach(
        byteArray: ByteArray,
        fileName: String,
        uploadProgress: ((Float) -> Unit)? = null,
    ): String

    /**
     * Detach file from message. If file was already uploaded it will be deleted.
     * If file is uploading now, the process will be stopped.
     *
     * @param attachmentId the ID of the attachment to remove.
     *
     * @throws IllegalStateException If the current state of the MessagingClient is not compatible with the requested action.
     * @throws IllegalArgumentException if the attachment ID is invalid.
     */
    @Throws(IllegalStateException::class, IllegalArgumentException::class)
    fun detach(attachmentId: String)

    /**
     * Refreshes the downloadUrl for a given attachment.
     * The `downloadUrl` of an attachment typically expires after 10 minutes.
     * If this URL is consumed after the expiration period, it will result in an error.
     * To mitigate this, the `refreshAttachmentUrl` function can be used to obtain a new valid `downloadUrl` for the specified attachment.
     *
     * @param attachmentId the ID of the attachment to refresh.
     *
     * @throws IllegalStateException If the current state of the MessagingClient is not compatible with the requested action.
     */
    @Throws(IllegalStateException::class)
    fun refreshAttachmentUrl(attachmentId: String)

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
     * @throws IllegalStateException If the current state of the MessagingClient is not compatible with the requested action.
     */
    @Throws(IllegalStateException::class)
    fun disconnect()

    /**
     * Reset current [conversation] and history request pagination index.
     * Note! After calling this function, the next call to [fetchNextPage] will request the
     * latest available history.
     */
    fun invalidateConversationCache()

    /**
     * Notify the agent that the customer is typing a message.
     * This command sends a single typing indicator event and should be called a maximum of once every 5 seconds.
     * If called more frequently, this command will be rate limited in order to optimize network traffic.
     *
     * @throws IllegalStateException If the current state of the MessagingClient is not compatible with the requested action.
     */
    @Throws(IllegalStateException::class)
    fun indicateTyping()

    /**
     * Exchange authCode for accessToken(JWT) using the provided authCode, redirect URI, and code verifier.
     * In case of failure Event.Error with [ErrorCode.AuthFailed] will be sent.
     *
     * @param authCode The authentication code to use for fetching the Auth JWT.
     * @param redirectUri The redirect URI to use for fetching the Auth JWT.
     * @param codeVerifier The code verifier to use for fetching the Auth JWT (optional).
     */
    fun authorize(authCode: String, redirectUri: String, codeVerifier: String?)

    /**
     * Exchange authCode for accessToken(JWT) using the provided id token using implicit grant flow.
     * In case of failure Event.Error with [ErrorCode.AuthFailed] will be sent.
     *
     * @param idToken The authentication code to use for fetching the Auth JWT.
     */
    fun authorizeImplicit(idToken: String)

    /**
     * Logs out user from authenticated session on all devices that shares the same auth session.
     * In case of failure Event.Error with [ErrorCode.AuthLogoutFailed] will be sent.
     *
     * @throws IllegalStateException If the current state of the MessagingClient is not compatible with the requested action.
     */
    @Throws(IllegalStateException::class)
    fun logoutFromAuthenticatedSession()

    /**
     * Check if the user needs to be authorized.
     *
     * @param callback A function that receives true if authorization is needed, or false otherwise.
     */
    fun shouldAuthorize(callback: (Boolean) -> Unit)

    /**
     * Permanently clears the existing conversation history with an agent from all devices that share the same session.
     * This action is allowed in both [State.Configured] and [State.ReadOnly].
     *
     * After successful clearance, [Event.ConversationCleared] will be dispatched to allow
     * the application to update the UI and clear necessary cache.
     *
     * In case of failure, [Event.Error] will be dispatched with a description of the failure cause.
     *
     * Note: Calling this API will result in the WebSocket connection being closed and new session will be created upon [connect].
     *
     * @throws IllegalStateException if the current state of the MessagingClient is not compatible with the requested action.
     */
    @Throws(IllegalStateException::class)
    fun clearConversation()
}
