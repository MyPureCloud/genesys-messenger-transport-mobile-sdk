package com.genesys.cloud.messenger.composeapp.model

/**
 * Represents a socket message in the TestBed interface.
 * Used for displaying messaging client communication and command results.
 * 
 * @param id Unique identifier for the message
 * @param timestamp When the message was created (milliseconds since epoch)
 * @param type The type of message (e.g., "Command", "Event", "Error", "System")
 * @param content Brief summary or main content of the message
 * @param rawMessage Complete raw message data for detailed view
 */
data class SocketMessage(
    val id: String,
    val timestamp: Long,
    val type: String,
    val content: String,
    val rawMessage: String
) {
    /**
     * Get the message type with proper formatting for display
     */
    val displayType: String
        get() = formatMessageType(type)
    
    /**
     * Get a summary of the message for collapsed view
     */
    val summary: String
        get() = generateMessageSummary(type, content)
    
    /**
     * Get formatted content for expanded view with JSON formatting if applicable
     */
    val formattedContent: String
        get() = formatMessageContent(rawMessage)
    
    /**
     * Get the priority level of this message type for UI styling
     */
    val priority: MessagePriority
        get() = getMessagePriority(type)
    
    companion object {
        /**
         * Format message type for display with proper capitalization and spacing
         */
        private fun formatMessageType(type: String): String {
            return when (type.lowercase()) {
                "command" -> "Command"
                "event" -> "Event"
                "error" -> "Error"
                "system" -> "System"
                "message" -> "Message"
                "state change", "statechange" -> "State Change"
                "warning" -> "Warning"
                "info" -> "Info"
                "debug" -> "Debug"
                else -> type.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            }
        }
        
        /**
         * Generate a concise summary for the message based on type and content
         */
        private fun generateMessageSummary(type: String, content: String): String {
            return when (type.lowercase()) {
                "command" -> {
                    if (content.startsWith("Executing:")) {
                        content.removePrefix("Executing:").trim()
                    } else {
                        truncateAtWordBoundary(content, 50)
                    }
                }
                "event" -> {
                    // Extract event name from content
                    val eventMatch = Regex("(\\w+)\\s*Event:?\\s*(.*)").find(content)
                    if (eventMatch != null) {
                        val (eventType, details) = eventMatch.destructured
                        val truncatedDetails = truncateAtWordBoundary(details, 30)
                        "$eventType: $truncatedDetails"
                    } else {
                        truncateAtWordBoundary(content, 50)
                    }
                }
                "state change", "statechange" -> {
                    // Extract state transition from content
                    val stateMatch = Regex("(.+?)\\s*→\\s*(.+)").find(content)
                    if (stateMatch != null) {
                        val (from, to) = stateMatch.destructured
                        "${from.trim()} → ${to.trim()}"
                    } else {
                        truncateAtWordBoundary(content, 50)
                    }
                }
                "error" -> {
                    // Show error type and brief message
                    truncateAtWordBoundary(content, 60)
                }
                "system" -> {
                    // Show system action
                    truncateAtWordBoundary(content, 40)
                }
                "message" -> {
                    // Show message type and brief content
                    val messageMatch = Regex("(\\w+)\\s*Message:?\\s*(.*)").find(content)
                    if (messageMatch != null) {
                        val (msgType, details) = messageMatch.destructured
                        val truncatedDetails = truncateAtWordBoundary(details, 30)
                        "$msgType: $truncatedDetails"
                    } else {
                        truncateAtWordBoundary(content, 50)
                    }
                }
                else -> truncateAtWordBoundary(content, 50)
            }
        }
        
        /**
         * Format message content with JSON formatting and structured data display
         */
        private fun formatMessageContent(rawMessage: String): String {
            return try {
                // Try to detect and format JSON content
                if (rawMessage.trim().startsWith("{") || rawMessage.trim().startsWith("[")) {
                    formatJsonContent(rawMessage)
                } else {
                    // Format structured text content
                    formatStructuredContent(rawMessage)
                }
            } catch (e: Exception) {
                // Fallback to raw message if formatting fails
                rawMessage
            }
        }
        
        /**
         * Format JSON content with proper indentation and structure
         */
        private fun formatJsonContent(jsonString: String): String {
            return try {
                // Simple JSON formatting - add proper indentation
                var formatted = jsonString
                var indentLevel = 0
                val result = StringBuilder()
                var inString = false
                var escapeNext = false
                
                for (char in formatted) {
                    when {
                        escapeNext -> {
                            result.append(char)
                            escapeNext = false
                        }
                        char == '\\' && inString -> {
                            result.append(char)
                            escapeNext = true
                        }
                        char == '"' -> {
                            result.append(char)
                            inString = !inString
                        }
                        !inString && (char == '{' || char == '[') -> {
                            result.append(char)
                            result.append('\n')
                            indentLevel++
                            result.append("  ".repeat(indentLevel))
                        }
                        !inString && (char == '}' || char == ']') -> {
                            result.append('\n')
                            indentLevel--
                            result.append("  ".repeat(indentLevel))
                            result.append(char)
                        }
                        !inString && char == ',' -> {
                            result.append(char)
                            result.append('\n')
                            result.append("  ".repeat(indentLevel))
                        }
                        !inString && char == ':' -> {
                            result.append(char)
                            result.append(' ')
                        }
                        char.isWhitespace() && !inString -> {
                            // Skip extra whitespace outside strings
                        }
                        else -> {
                            result.append(char)
                        }
                    }
                }
                
                result.toString()
            } catch (e: Exception) {
                jsonString
            }
        }
        
        /**
         * Format structured text content with better readability
         */
        private fun formatStructuredContent(content: String): String {
            return content
                .split('\n')
                .joinToString("\n") { line ->
                    when {
                        line.trim().isEmpty() -> line
                        line.contains(":") && !line.trim().startsWith("http") -> {
                            // Format key-value pairs
                            val parts = line.split(":", limit = 2)
                            if (parts.size == 2) {
                                "${parts[0].trim()}: ${parts[1].trim()}"
                            } else {
                                line
                            }
                        }
                        line.trim().startsWith("- ") -> {
                            // Format list items with proper indentation
                            "  ${line.trim()}"
                        }
                        else -> line
                    }
                }
        }
        
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
         * Get the priority level for message type to determine UI styling
         */
        private fun getMessagePriority(type: String): MessagePriority {
            return when (type.lowercase()) {
                "error" -> MessagePriority.HIGH
                "warning" -> MessagePriority.MEDIUM
                "command" -> MessagePriority.MEDIUM
                "state change", "statechange" -> MessagePriority.MEDIUM
                "event" -> MessagePriority.LOW
                "message" -> MessagePriority.LOW
                "system" -> MessagePriority.LOW
                "info" -> MessagePriority.LOW
                "debug" -> MessagePriority.LOW
                else -> MessagePriority.LOW
            }
        }
    }
}

/**
 * Priority levels for socket messages to determine UI styling and prominence
 */
enum class MessagePriority {
    HIGH,    // Errors, critical issues
    MEDIUM,  // Warnings, commands, state changes
    LOW      // Info, debug, regular events
}

/**
 * Represents a socket message with UI state for expandable display
 * 
 * @param message The underlying socket message
 * @param isExpanded Whether the message details are currently expanded
 */
data class SocketMessageItem(
    val message: SocketMessage,
    val isExpanded: Boolean = false
) {
    val id: String get() = message.id
    val timestamp: Long get() = message.timestamp
    val type: String get() = message.type
    val displayType: String get() = message.displayType
    val content: String get() = message.content
    val summary: String get() = message.summary
    val rawMessage: String get() = message.rawMessage
    val formattedContent: String get() = message.formattedContent
    val priority: MessagePriority get() = message.priority
    
    /**
     * Create a copy with toggled expansion state
     */
    fun toggleExpanded(): SocketMessageItem {
        return copy(isExpanded = !isExpanded)
    }
    
    /**
     * Create a copy with specific expansion state
     */
    fun withExpanded(expanded: Boolean): SocketMessageItem {
        return copy(isExpanded = expanded)
    }
}