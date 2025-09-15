package com.genesys.cloud.messenger.composeapp.util

import com.genesys.cloud.messenger.composeapp.model.SocketMessage
import com.genesys.cloud.messenger.composeapp.model.SocketMessageItem

/**
 * Utility functions for working with socket messages and socket message items
 */
object SocketMessageUtils {
    
    /**
     * Convert a list of SocketMessage to SocketMessageItem with expansion state
     */
    fun toSocketMessageItems(messages: List<SocketMessage>): List<SocketMessageItem> {
        return messages.map { message ->
            SocketMessageItem(message = message, isExpanded = false)
        }
    }
    
    /**
     * Toggle expansion state for a specific message by ID
     */
    fun toggleExpansion(
        items: List<SocketMessageItem>,
        messageId: String
    ): List<SocketMessageItem> {
        return items.map { item ->
            if (item.id == messageId) {
                item.toggleExpanded()
            } else {
                item
            }
        }
    }
    
    /**
     * Expand a specific message by ID
     */
    fun expandMessage(
        items: List<SocketMessageItem>,
        messageId: String
    ): List<SocketMessageItem> {
        return items.map { item ->
            if (item.id == messageId) {
                item.withExpanded(true)
            } else {
                item
            }
        }
    }
    
    /**
     * Collapse a specific message by ID
     */
    fun collapseMessage(
        items: List<SocketMessageItem>,
        messageId: String
    ): List<SocketMessageItem> {
        return items.map { item ->
            if (item.id == messageId) {
                item.withExpanded(false)
            } else {
                item
            }
        }
    }
    
    /**
     * Collapse all messages
     */
    fun collapseAll(items: List<SocketMessageItem>): List<SocketMessageItem> {
        return items.map { item ->
            item.withExpanded(false)
        }
    }
    
    /**
     * Expand all messages
     */
    fun expandAll(items: List<SocketMessageItem>): List<SocketMessageItem> {
        return items.map { item ->
            item.withExpanded(true)
        }
    }
    
    /**
     * Filter messages by type
     */
    fun filterByType(
        items: List<SocketMessageItem>,
        types: Set<String>
    ): List<SocketMessageItem> {
        return items.filter { item ->
            item.type.lowercase() in types.map { it.lowercase() }
        }
    }
    
    /**
     * Filter messages by priority
     */
    fun filterByPriority(
        items: List<SocketMessageItem>,
        priorities: Set<com.genesys.cloud.messenger.composeapp.model.MessagePriority>
    ): List<SocketMessageItem> {
        return items.filter { item ->
            item.priority in priorities
        }
    }
    
    /**
     * Sort messages by timestamp (newest first by default)
     */
    fun sortByTimestamp(
        items: List<SocketMessageItem>,
        ascending: Boolean = false
    ): List<SocketMessageItem> {
        return if (ascending) {
            items.sortedBy { it.timestamp }
        } else {
            items.sortedByDescending { it.timestamp }
        }
    }
    
    /**
     * Get messages within a time range
     */
    fun filterByTimeRange(
        items: List<SocketMessageItem>,
        startTime: Long,
        endTime: Long
    ): List<SocketMessageItem> {
        return items.filter { item ->
            item.timestamp in startTime..endTime
        }
    }
    
    /**
     * Search messages by content (case-insensitive)
     */
    fun searchMessages(
        items: List<SocketMessageItem>,
        query: String
    ): List<SocketMessageItem> {
        if (query.isBlank()) return items
        
        val lowerQuery = query.lowercase()
        return items.filter { item ->
            item.content.lowercase().contains(lowerQuery) ||
            item.rawMessage.lowercase().contains(lowerQuery) ||
            item.type.lowercase().contains(lowerQuery)
        }
    }
    
    /**
     * Get unique message types from a list of messages
     */
    fun getUniqueTypes(items: List<SocketMessageItem>): Set<String> {
        return items.map { it.type }.toSet()
    }
    
    /**
     * Get message statistics
     */
    fun getMessageStats(items: List<SocketMessageItem>): MessageStats {
        val typeCount = items.groupingBy { it.type }.eachCount()
        val priorityCount = items.groupingBy { it.priority }.eachCount()
        val expandedCount = items.count { it.isExpanded }
        
        return MessageStats(
            totalMessages = items.size,
            expandedMessages = expandedCount,
            collapsedMessages = items.size - expandedCount,
            messagesByType = typeCount,
            messagesByPriority = priorityCount,
            oldestTimestamp = items.minOfOrNull { it.timestamp },
            newestTimestamp = items.maxOfOrNull { it.timestamp }
        )
    }
}

/**
 * Statistics about a collection of socket messages
 */
data class MessageStats(
    val totalMessages: Int,
    val expandedMessages: Int,
    val collapsedMessages: Int,
    val messagesByType: Map<String, Int>,
    val messagesByPriority: Map<com.genesys.cloud.messenger.composeapp.model.MessagePriority, Int>,
    val oldestTimestamp: Long?,
    val newestTimestamp: Long?
)