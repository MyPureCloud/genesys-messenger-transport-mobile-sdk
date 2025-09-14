package com.genesys.cloud.messenger.composeapp.model

/**
 * Represents a chat message in the conversation.
 *
 * @param id Unique identifier for the message
 * @param content The text content of the message
 * @param timestamp Unix timestamp when the message was created
 * @param isFromUser True if the message is from the user, false if from the agent/bot
 */
data class ChatMessage(
    val id: String,
    val content: String,
    val timestamp: Long,
    val isFromUser: Boolean
)