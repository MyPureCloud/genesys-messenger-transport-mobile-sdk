package com.genesys.cloud.messenger.composeapp.util

import com.genesys.cloud.messenger.composeapp.model.Event
import com.genesys.cloud.messenger.composeapp.model.MessageEvent
import com.genesys.cloud.messenger.composeapp.model.MessagingClientState

/**
 * Utility functions for formatting different types of messages and events
 * for display in the TestBed interface.
 */
object MessageFormatting {
    
    /**
     * Truncate text at word boundary to avoid cutting words in the middle
     */
    private fun truncateAtWordBoundary(text: String, maxLength: Int): String {
        if (text.length <= maxLength) return text
        
        val truncated = text.take(maxLength)
        val lastSpaceIndex = truncated.lastIndexOf(' ')
        
        return if (lastSpaceIndex > maxLength * 0.7) { // Only use word boundary if it's not too short
            truncated.substring(0, lastSpaceIndex) + "..."
        } else {
            truncated + "..."
        }
    }
    

    
    /**
     * Format state change content for display
     */
    fun formatStateChangeContent(oldState: MessagingClientState, newState: MessagingClientState): String {
        return "$oldState → $newState"
    }
    
    /**
     * Format message event content for display
     */
    fun formatMessageContent(messageEvent: MessageEvent): String {
        return when (messageEvent.type.lowercase()) {
            "text", "message" -> {
                "Text: ${truncateAtWordBoundary(messageEvent.content, 50)}"
            }
            "typing" -> {
                "Typing indicator${if (messageEvent.content.isNotEmpty()) ": ${messageEvent.content}" else ""}"
            }
            "attachment" -> {
                "Attachment: ${extractAttachmentName(messageEvent.content) ?: "Unknown"}"
            }
            "quickreply" -> {
                val options = parseQuickReplyOptions(messageEvent.content)
                "Quick Reply (${options.size} options)"
            }
            "system" -> {
                "System: ${messageEvent.content.take(40)}${if (messageEvent.content.length > 40) "..." else ""}"
            }
            else -> {
                "${messageEvent.type}: ${messageEvent.content.take(40)}${if (messageEvent.content.length > 40) "..." else ""}"
            }
        }
    }
    
    /**
     * Format event content for display
     */
    fun formatEventContent(event: Event): String {
        return when (event.type.lowercase()) {
            "connectionestablished", "connected" -> {
                "Connected to messaging service"
            }
            "connectionlost", "disconnected" -> {
                "Disconnected${if (event.data.isNotEmpty()) ": ${event.data}" else ""}"
            }
            "error" -> {
                "Error: ${truncateAtWordBoundary(event.data, 50)}"
            }
            "authenticationrequired" -> {
                "Authentication required"
            }
            "conversationcleared" -> {
                "Conversation cleared"
            }
            "messagedelivered" -> {
                "Message delivered"
            }
            "messageread" -> {
                "Message read"
            }
            "presencechanged" -> {
                "Presence changed: ${truncateAtWordBoundary(event.data, 30)}"
            }
            "typingindicator" -> {
                "Typing: ${truncateAtWordBoundary(event.data, 30)}"
            }
            "attachmentuploaded" -> {
                "Attachment uploaded: ${extractAttachmentName(event.data) ?: "Unknown"}"
            }
            "attachmentfailed" -> {
                "Attachment failed: ${extractAttachmentName(event.data) ?: "Unknown"}"
            }
            else -> {
                "${event.type}: ${truncateAtWordBoundary(event.data, 40)}"
            }
        }
    }
    
    /**
     * Get disconnection reason based on previous state
     */
    fun getDisconnectionReason(previousState: MessagingClientState): String {
        return when (previousState) {
            MessagingClientState.Connecting -> "Connection failed"
            MessagingClientState.Connected -> "Normal disconnection"
            MessagingClientState.Error -> "Error state disconnection"
            else -> "Unknown reason"
        }
    }
    
    /**
     * Parse attachment information from message content
     */
    fun parseAttachmentInfo(content: String): Map<String, String> {
        val info = mutableMapOf<String, String>()
        
        // Try to extract common attachment properties
        val patterns = mapOf(
            "name" to Regex("name[\"']?\\s*[:=]\\s*[\"']?([^\"',\\s]+)"),
            "filename" to Regex("filename[\"']?\\s*[:=]\\s*[\"']?([^\"',\\s]+)"),
            "size" to Regex("size[\"']?\\s*[:=]\\s*[\"']?(\\d+)"),
            "type" to Regex("type[\"']?\\s*[:=]\\s*[\"']?([^\"',\\s]+)"),
            "mimetype" to Regex("mimetype[\"']?\\s*[:=]\\s*[\"']?([^\"',\\s]+)"),
            "url" to Regex("url[\"']?\\s*[:=]\\s*[\"']?([^\"',\\s]+)")
        )
        
        patterns.forEach { (key, pattern) ->
            val caseInsensitivePattern = Regex(pattern.pattern, RegexOption.IGNORE_CASE)
            caseInsensitivePattern.find(content)?.let { match ->
                info[key] = match.groupValues[1]
            }
        }
        
        return info
    }
    
    /**
     * Parse quick reply options from message content
     */
    fun parseQuickReplyOptions(content: String): List<String> {
        val options = mutableListOf<String>()
        
        // Try to extract options from various formats
        val patterns = listOf(
            Regex("options?[\"']?\\s*[:=]\\s*\\[([^\\]]+)\\]"),
            Regex("choices?[\"']?\\s*[:=]\\s*\\[([^\\]]+)\\]"),
            Regex("replies?[\"']?\\s*[:=]\\s*\\[([^\\]]+)\\]")
        )
        
        patterns.forEach { pattern ->
            val caseInsensitivePattern = Regex(pattern.pattern, RegexOption.IGNORE_CASE)
            caseInsensitivePattern.find(content)?.let { match ->
                val optionsText = match.groupValues[1]
                options.addAll(
                    optionsText.split(",")
                        .map { it.trim().removeSurrounding("\"").removeSurrounding("'") }
                        .filter { it.isNotEmpty() }
                )
            }
        }
        
        // Fallback: look for numbered or bulleted lists
        if (options.isEmpty()) {
            val lines = content.split('\n')
            lines.forEach { line ->
                val trimmed = line.trim()
                if (trimmed.matches(Regex("^\\d+\\.\\s+.+")) || 
                    trimmed.matches(Regex("^[-*•]\\s+.+"))) {
                    options.add(trimmed.replaceFirst(Regex("^\\d+\\.\\s+|^[-*•]\\s+"), ""))
                }
            }
        }
        
        return options
    }
    
    /**
     * Parse error information from event data
     */
    fun parseErrorInfo(errorData: String): Map<String, String> {
        val info = mutableMapOf<String, String>()
        
        val patterns = mapOf(
            "code" to Regex("code[\"']?\\s*[:=]\\s*[\"']?([^\"',\\s]+)"),
            "message" to Regex("message[\"']?\\s*[:=]\\s*[\"']?([^\"']+)"),
            "type" to Regex("type[\"']?\\s*[:=]\\s*[\"']?([^\"',\\s]+)"),
            "status" to Regex("status[\"']?\\s*[:=]\\s*[\"']?(\\d+)"),
            "reason" to Regex("reason[\"']?\\s*[:=]\\s*[\"']?([^\"']+)")
        )
        
        patterns.forEach { (key, pattern) ->
            val caseInsensitivePattern = Regex(pattern.pattern, RegexOption.IGNORE_CASE)
            caseInsensitivePattern.find(errorData)?.let { match ->
                info[key] = match.groupValues[1].trim()
            }
        }
        
        return info
    }
    
    /**
     * Extract attachment name from content
     */
    private fun extractAttachmentName(content: String): String? {
        val patterns = listOf(
            Regex("name[\"']?\\s*[:=]\\s*[\"']?([^\"',\\s]+)"),
            Regex("filename[\"']?\\s*[:=]\\s*[\"']?([^\"',\\s]+)"),
            Regex("file[\"']?\\s*[:=]\\s*[\"']?([^\"',\\s]+)")
        )
        
        patterns.forEach { pattern ->
            val caseInsensitivePattern = Regex(pattern.pattern, RegexOption.IGNORE_CASE)
            caseInsensitivePattern.find(content)?.let { match ->
                return match.groupValues[1]
            }
        }
        
        return null
    }
}