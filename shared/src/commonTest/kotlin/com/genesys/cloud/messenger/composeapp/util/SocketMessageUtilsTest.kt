package com.genesys.cloud.messenger.composeapp.util

import com.genesys.cloud.messenger.composeapp.model.MessagePriority
import com.genesys.cloud.messenger.composeapp.model.SocketMessage
import com.genesys.cloud.messenger.composeapp.model.SocketMessageItem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SocketMessageUtilsTest {
    
    private fun createTestMessages(): List<SocketMessage> {
        return listOf(
            SocketMessage("1", 1000L, "command", "Connect command", "raw1"),
            SocketMessage("2", 2000L, "error", "Connection failed", "raw2"),
            SocketMessage("3", 3000L, "event", "Message received", "raw3"),
            SocketMessage("4", 4000L, "system", "System initialized", "raw4")
        )
    }
    
    @Test
    fun testToSocketMessageItems() {
        val messages = createTestMessages()
        val items = SocketMessageUtils.toSocketMessageItems(messages)
        
        assertEquals(4, items.size)
        items.forEach { item ->
            assertFalse(item.isExpanded)
        }
        assertEquals("1", items[0].id)
        assertEquals("command", items[0].type)
    }
    
    @Test
    fun testToggleExpansion() {
        val messages = createTestMessages()
        val items = SocketMessageUtils.toSocketMessageItems(messages)
        
        val updatedItems = SocketMessageUtils.toggleExpansion(items, "2")
        
        assertFalse(updatedItems[0].isExpanded) // ID "1"
        assertTrue(updatedItems[1].isExpanded)  // ID "2" - toggled
        assertFalse(updatedItems[2].isExpanded) // ID "3"
        assertFalse(updatedItems[3].isExpanded) // ID "4"
    }
    
    @Test
    fun testExpandMessage() {
        val messages = createTestMessages()
        val items = SocketMessageUtils.toSocketMessageItems(messages)
        
        val updatedItems = SocketMessageUtils.expandMessage(items, "3")
        
        assertFalse(updatedItems[0].isExpanded) // ID "1"
        assertFalse(updatedItems[1].isExpanded) // ID "2"
        assertTrue(updatedItems[2].isExpanded)  // ID "3" - expanded
        assertFalse(updatedItems[3].isExpanded) // ID "4"
    }
    
    @Test
    fun testCollapseMessage() {
        val messages = createTestMessages()
        val items = SocketMessageUtils.toSocketMessageItems(messages)
            .map { it.withExpanded(true) } // Start with all expanded
        
        val updatedItems = SocketMessageUtils.collapseMessage(items, "2")
        
        assertTrue(updatedItems[0].isExpanded)  // ID "1"
        assertFalse(updatedItems[1].isExpanded) // ID "2" - collapsed
        assertTrue(updatedItems[2].isExpanded)  // ID "3"
        assertTrue(updatedItems[3].isExpanded)  // ID "4"
    }
    
    @Test
    fun testCollapseAll() {
        val messages = createTestMessages()
        val items = SocketMessageUtils.toSocketMessageItems(messages)
            .map { it.withExpanded(true) } // Start with all expanded
        
        val updatedItems = SocketMessageUtils.collapseAll(items)
        
        updatedItems.forEach { item ->
            assertFalse(item.isExpanded)
        }
    }
    
    @Test
    fun testExpandAll() {
        val messages = createTestMessages()
        val items = SocketMessageUtils.toSocketMessageItems(messages)
        
        val updatedItems = SocketMessageUtils.expandAll(items)
        
        updatedItems.forEach { item ->
            assertTrue(item.isExpanded)
        }
    }
    
    @Test
    fun testFilterByType() {
        val messages = createTestMessages()
        val items = SocketMessageUtils.toSocketMessageItems(messages)
        
        val filteredItems = SocketMessageUtils.filterByType(items, setOf("command", "error"))
        
        assertEquals(2, filteredItems.size)
        assertEquals("command", filteredItems[0].type)
        assertEquals("error", filteredItems[1].type)
    }
    
    @Test
    fun testFilterByPriority() {
        val messages = createTestMessages()
        val items = SocketMessageUtils.toSocketMessageItems(messages)
        
        val highPriorityItems = SocketMessageUtils.filterByPriority(items, setOf(MessagePriority.HIGH))
        
        assertEquals(1, highPriorityItems.size)
        assertEquals("error", highPriorityItems[0].type) // Error messages have HIGH priority
    }
    
    @Test
    fun testSortByTimestamp() {
        val messages = createTestMessages()
        val items = SocketMessageUtils.toSocketMessageItems(messages)
        
        // Test descending (default)
        val descendingItems = SocketMessageUtils.sortByTimestamp(items)
        assertEquals("4", descendingItems[0].id) // Newest first (4000L)
        assertEquals("1", descendingItems[3].id) // Oldest last (1000L)
        
        // Test ascending
        val ascendingItems = SocketMessageUtils.sortByTimestamp(items, ascending = true)
        assertEquals("1", ascendingItems[0].id) // Oldest first (1000L)
        assertEquals("4", ascendingItems[3].id) // Newest last (4000L)
    }
    
    @Test
    fun testFilterByTimeRange() {
        val messages = createTestMessages()
        val items = SocketMessageUtils.toSocketMessageItems(messages)
        
        val filteredItems = SocketMessageUtils.filterByTimeRange(items, 1500L, 3500L)
        
        assertEquals(2, filteredItems.size)
        assertEquals("2", filteredItems[0].id) // 2000L
        assertEquals("3", filteredItems[1].id) // 3000L
    }
    
    @Test
    fun testSearchMessages() {
        val messages = createTestMessages()
        val items = SocketMessageUtils.toSocketMessageItems(messages)
        
        val searchResults = SocketMessageUtils.searchMessages(items, "connect")
        
        assertEquals(2, searchResults.size)
        assertTrue(searchResults.any { it.content.lowercase().contains("connect") })
    }
    
    @Test
    fun testGetUniqueTypes() {
        val messages = createTestMessages()
        val items = SocketMessageUtils.toSocketMessageItems(messages)
        
        val uniqueTypes = SocketMessageUtils.getUniqueTypes(items)
        
        assertEquals(4, uniqueTypes.size)
        assertTrue(uniqueTypes.contains("command"))
        assertTrue(uniqueTypes.contains("error"))
        assertTrue(uniqueTypes.contains("event"))
        assertTrue(uniqueTypes.contains("system"))
    }
    
    @Test
    fun testGetMessageStats() {
        val messages = createTestMessages()
        val items = SocketMessageUtils.toSocketMessageItems(messages)
            .mapIndexed { index, item -> 
                if (index < 2) item.withExpanded(true) else item 
            }
        
        val stats = SocketMessageUtils.getMessageStats(items)
        
        assertEquals(4, stats.totalMessages)
        assertEquals(2, stats.expandedMessages)
        assertEquals(2, stats.collapsedMessages)
        assertEquals(1000L, stats.oldestTimestamp)
        assertEquals(4000L, stats.newestTimestamp)
        
        // Check type counts
        assertEquals(1, stats.messagesByType["command"])
        assertEquals(1, stats.messagesByType["error"])
        assertEquals(1, stats.messagesByType["event"])
        assertEquals(1, stats.messagesByType["system"])
        
        // Check priority counts
        assertEquals(1, stats.messagesByPriority[MessagePriority.HIGH]) // error
        assertEquals(1, stats.messagesByPriority[MessagePriority.MEDIUM]) // command
        assertEquals(2, stats.messagesByPriority[MessagePriority.LOW]) // event, system
    }
}