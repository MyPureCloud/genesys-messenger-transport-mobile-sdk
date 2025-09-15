package com.genesys.cloud.messenger.composeapp.ui.components

import com.genesys.cloud.messenger.composeapp.model.*
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.assertNotNull

/**
 * Integration tests for InteractionScreen components focusing on component interactions and state management.
 * These tests verify the logic behind UI components without requiring actual Compose rendering.
 * 
 * Requirements addressed:
 * - 2.2: Display message type prominently
 * - 2.3: Expandable socket message details
 * - 5.1: Socket message display
 * - 5.2: Message type prominence in collapsed view
 * - 5.3: Chronological order display
 * - 5.4: Auto-scroll to latest messages
 * - 5.5: JSON formatting for readability
 */
class InteractionScreenComponentsTest {

    /**
     * Test SocketMessageItem creation and expansion state
     * Requirements: 2.3, 5.1, 5.2
     */
    @Test
    fun testSocketMessageItemCreationAndExpansion() = runTest {
        // Given
        val socketMessage = SocketMessage(
            id = "test_msg_1",
            timestamp = 1234567890L,
            type = "Error",
            content = "Test message content",
            rawMessage = """{"type":"MessageEvent","data":"test"}"""
        )
        
        // When - Create SocketMessageItem
        val messageItem = SocketMessageItem(
            message = socketMessage,
            isExpanded = false
        )
        
        // Then - Verify initial state
        assertEquals("test_msg_1", messageItem.id, "Message item should have correct ID")
        assertEquals(socketMessage, messageItem.message, "Message item should contain the socket message")
        assertFalse(messageItem.isExpanded, "Message item should initially be collapsed")
        
        // Verify message type prominence (requirement 2.2)
        assertEquals("Error", messageItem.message.displayType, "Display type should be prominent")
        assertTrue(messageItem.message.summary.isNotEmpty(), "Summary should be available for collapsed view")
        
        // When - Create expanded message item
        val expandedMessageItem = messageItem.copy(isExpanded = true)
        
        // Then - Verify expansion state (requirement 2.3)
        assertTrue(expandedMessageItem.isExpanded, "Message item should be expanded")
        assertEquals(messageItem.message, expandedMessageItem.message, "Message content should remain the same")
        assertTrue(expandedMessageItem.message.formattedContent.isNotEmpty(), "Formatted content should be available for expansion")
    }

    /**
     * Test socket message list ordering and chronological display
     * Requirements: 5.3, 5.4
     */
    @Test
    fun testSocketMessageListOrdering() = runTest {
        // Given
        val messages = listOf(
            SocketMessage(
                id = "msg_1",
                timestamp = 1000L,
                type = "FirstMessage",
                content = "First message",
                rawMessage = """{"order":1}"""
            ),
            SocketMessage(
                id = "msg_2", 
                timestamp = 2000L,
                type = "SecondMessage",
                content = "Second message",
                rawMessage = """{"order":2}"""
            ),
            SocketMessage(
                id = "msg_3",
                timestamp = 3000L,
                type = "ThirdMessage", 
                content = "Third message",
                rawMessage = """{"order":3}"""
            )
        )
        
        // When - Create message items with expansion state
        val expandedMessageIds = setOf("msg_2") // Only second message is expanded
        val messageItems = messages.map { message ->
            SocketMessageItem(
                message = message,
                isExpanded = expandedMessageIds.contains(message.id)
            )
        }
        
        // Then - Verify chronological order (requirement 5.3)
        assertEquals(3, messageItems.size, "Should have 3 message items")
        assertEquals(1000L, messageItems[0].message.timestamp, "First message should have earliest timestamp")
        assertEquals(2000L, messageItems[1].message.timestamp, "Second message should have middle timestamp")
        assertEquals(3000L, messageItems[2].message.timestamp, "Third message should have latest timestamp")
        
        // Verify expansion state is preserved
        assertFalse(messageItems[0].isExpanded, "First message should be collapsed")
        assertTrue(messageItems[1].isExpanded, "Second message should be expanded")
        assertFalse(messageItems[2].isExpanded, "Third message should be collapsed")
        
        // Verify latest message for auto-scroll (requirement 5.4)
        val latestMessage = messageItems.maxByOrNull { it.message.timestamp }
        assertNotNull(latestMessage, "Should have a latest message")
        assertEquals("msg_3", latestMessage.id, "Latest message should be the third one")
        assertEquals("ThirdMessage", latestMessage.message.displayType, "Latest message should have correct type")
    }

    /**
     * Test message priority handling and display
     * Requirements: 5.1, 5.2
     */
    @Test
    fun testMessagePriorityHandling() = runTest {
        // Given
        val highPriorityMessage = SocketMessage(
            id = "high_priority",
            timestamp = 1000L,
            type = "Error",
            content = "Critical error occurred",
            rawMessage = """{"level":"error","message":"critical"}"""
        )
        
        val mediumPriorityMessage = SocketMessage(
            id = "medium_priority",
            timestamp = 2000L,
            type = "Command",
            content = "Information message",
            rawMessage = """{"level":"info","message":"information"}"""
        )
        
        val lowPriorityMessage = SocketMessage(
            id = "low_priority",
            timestamp = 3000L,
            type = "Info",
            content = "Debug information",
            rawMessage = """{"level":"debug","message":"debug"}"""
        )
        
        // When - Create message items
        val messageItems = listOf(
            SocketMessageItem(message = highPriorityMessage, isExpanded = false),
            SocketMessageItem(message = mediumPriorityMessage, isExpanded = false),
            SocketMessageItem(message = lowPriorityMessage, isExpanded = false)
        )
        
        // Then - Verify priority is preserved and affects display
        assertEquals(MessagePriority.HIGH, messageItems[0].message.priority, "High priority message should maintain priority")
        assertEquals(MessagePriority.MEDIUM, messageItems[1].message.priority, "Medium priority message should maintain priority")
        assertEquals(MessagePriority.LOW, messageItems[2].message.priority, "Low priority message should maintain priority")
        
        // Verify message type prominence for different priorities (requirement 5.2)
        assertEquals("Error", messageItems[0].message.displayType, "High priority message type should be prominent")
        assertEquals("Command", messageItems[1].message.displayType, "Medium priority message type should be prominent")
        assertEquals("Info", messageItems[2].message.displayType, "Low priority message type should be prominent")
        
        // Verify content is available for all priorities
        assertTrue(messageItems[0].message.summary.isNotEmpty(), "High priority summary should be available")
        assertTrue(messageItems[1].message.summary.isNotEmpty(), "Medium priority summary should be available")
        assertTrue(messageItems[2].message.summary.isNotEmpty(), "Low priority summary should be available")
    }

    /**
     * Test JSON message formatting for readability
     * Requirements: 5.5
     */
    @Test
    fun testJSONMessageFormatting() = runTest {
        // Given
        val complexJsonMessage = SocketMessage(
            id = "json_complex",
            timestamp = 1000L,
            type = "ComplexEvent",
            content = "Complex JSON data received",
            rawMessage = """{"type":"event","data":{"user":{"id":123,"name":"John"},"actions":["login","view","logout"],"metadata":{"timestamp":"2023-01-01","version":"1.0"}}}"""
        )
        
        val simpleJsonMessage = SocketMessage(
            id = "json_simple",
            timestamp = 2000L,
            type = "SimpleEvent",
            content = "Simple JSON data",
            rawMessage = """{"status":"ok","code":200}"""
        )
        
        val nonJsonMessage = SocketMessage(
            id = "text_message",
            timestamp = 3000L,
            type = "TextEvent",
            content = "Plain text message",
            rawMessage = "This is plain text, not JSON"
        )
        
        // When - Create message items
        val messageItems = listOf(
            SocketMessageItem(message = complexJsonMessage, isExpanded = false),
            SocketMessageItem(message = simpleJsonMessage, isExpanded = false),
            SocketMessageItem(message = nonJsonMessage, isExpanded = false)
        )
        
        // Then - Verify JSON formatting is available (requirement 5.5)
        
        // Complex JSON message
        val complexItem = messageItems[0]
        assertTrue(complexItem.message.formattedContent.isNotEmpty(), "Complex JSON should have formatted content")
        assertTrue(complexItem.message.rawMessage.contains("{"), "Raw message should contain JSON")
        assertTrue(complexItem.message.formattedContent.length >= complexItem.message.rawMessage.length, "Formatted content should be at least as long as raw message")
        
        // Simple JSON message
        val simpleItem = messageItems[1]
        assertTrue(simpleItem.message.formattedContent.isNotEmpty(), "Simple JSON should have formatted content")
        assertTrue(simpleItem.message.rawMessage.contains("status"), "Raw message should contain JSON fields")
        
        // Non-JSON message
        val textItem = messageItems[2]
        assertTrue(textItem.message.formattedContent.isNotEmpty(), "Text message should have formatted content")
        assertEquals("This is plain text, not JSON", textItem.message.rawMessage, "Raw message should be preserved")
        assertTrue(textItem.message.formattedContent.length >= textItem.message.rawMessage.length, "Formatted content should be at least as long as raw message")
    }

    /**
     * Test message expansion state management
     * Requirements: 2.3
     */
    @Test
    fun testMessageExpansionStateManagement() = runTest {
        // Given
        val messages = listOf(
            SocketMessage(id = "msg_1", timestamp = 1000L, type = "Type1", content = "Content 1", rawMessage = "Raw 1"),
            SocketMessage(id = "msg_2", timestamp = 2000L, type = "Type2", content = "Content 2", rawMessage = "Raw 2"),
            SocketMessage(id = "msg_3", timestamp = 3000L, type = "Type3", content = "Content 3", rawMessage = "Raw 3")
        )
        
        // When - Start with no expanded messages
        var expandedMessageIds = setOf<String>()
        var messageItems = messages.map { message ->
            SocketMessageItem(
                message = message,
                isExpanded = expandedMessageIds.contains(message.id)
            )
        }
        
        // Then - Verify all messages are collapsed initially
        assertTrue(messageItems.all { !it.isExpanded }, "All messages should be collapsed initially")
        
        // When - Expand first message
        expandedMessageIds = expandedMessageIds + "msg_1"
        messageItems = messages.map { message ->
            SocketMessageItem(
                message = message,
                isExpanded = expandedMessageIds.contains(message.id)
            )
        }
        
        // Then - Verify only first message is expanded
        assertTrue(messageItems[0].isExpanded, "First message should be expanded")
        assertFalse(messageItems[1].isExpanded, "Second message should remain collapsed")
        assertFalse(messageItems[2].isExpanded, "Third message should remain collapsed")
        
        // When - Expand second message and collapse first
        expandedMessageIds = (expandedMessageIds - "msg_1") + "msg_2"
        messageItems = messages.map { message ->
            SocketMessageItem(
                message = message,
                isExpanded = expandedMessageIds.contains(message.id)
            )
        }
        
        // Then - Verify state changes
        assertFalse(messageItems[0].isExpanded, "First message should be collapsed")
        assertTrue(messageItems[1].isExpanded, "Second message should be expanded")
        assertFalse(messageItems[2].isExpanded, "Third message should remain collapsed")
        
        // When - Expand multiple messages
        expandedMessageIds = expandedMessageIds + "msg_1" + "msg_3"
        messageItems = messages.map { message ->
            SocketMessageItem(
                message = message,
                isExpanded = expandedMessageIds.contains(message.id)
            )
        }
        
        // Then - Verify multiple messages can be expanded simultaneously
        assertTrue(messageItems[0].isExpanded, "First message should be expanded")
        assertTrue(messageItems[1].isExpanded, "Second message should remain expanded")
        assertTrue(messageItems[2].isExpanded, "Third message should be expanded")
    }

    /**
     * Test empty message list handling
     * Requirements: 5.1
     */
    @Test
    fun testEmptyMessageListHandling() = runTest {
        // Given
        val emptyMessages = emptyList<SocketMessage>()
        val expandedMessageIds = setOf<String>()
        
        // When - Create message items from empty list
        val messageItems = emptyMessages.map { message ->
            SocketMessageItem(
                message = message,
                isExpanded = expandedMessageIds.contains(message.id)
            )
        }
        
        // Then - Verify empty state handling
        assertTrue(messageItems.isEmpty(), "Message items should be empty")
        assertEquals(0, messageItems.size, "Should have zero message items")
        
        // Verify empty state would trigger appropriate UI
        // (This would be tested in actual UI tests, but we verify the data state here)
        assertTrue(messageItems.none { it.isExpanded }, "No messages should be expanded in empty state")
    }

    /**
     * Test message filtering and search capabilities
     * Requirements: 5.1, 5.2
     */
    @Test
    fun testMessageFilteringCapabilities() = runTest {
        // Given
        val messages = listOf(
            SocketMessage(id = "error_1", timestamp = 1000L, type = "ErrorEvent", content = "Error occurred", rawMessage = "Error details"),
            SocketMessage(id = "info_1", timestamp = 2000L, type = "InfoEvent", content = "Information message", rawMessage = "Info details"),
            SocketMessage(id = "error_2", timestamp = 3000L, type = "ErrorEvent", content = "Another error", rawMessage = "More error details"),
            SocketMessage(id = "debug_1", timestamp = 4000L, type = "DebugEvent", content = "Debug information", rawMessage = "Debug details")
        )
        
        // When - Filter by message type
        val errorMessages = messages.filter { it.type == "ErrorEvent" }
        val infoMessages = messages.filter { it.type == "InfoEvent" }
        val debugMessages = messages.filter { it.type == "DebugEvent" }
        
        // Then - Verify filtering works correctly
        assertEquals(2, errorMessages.size, "Should have 2 error messages")
        assertEquals(1, infoMessages.size, "Should have 1 info message")
        assertEquals(1, debugMessages.size, "Should have 1 debug message")
        
        // Verify filtered messages maintain their properties
        assertTrue(errorMessages.all { it.type == "ErrorEvent" }, "All filtered messages should be error events")
        assertTrue(errorMessages.all { it.content.contains("error", ignoreCase = true) }, "Error messages should contain error content")
        
        // When - Filter by content
        val messagesWithError = messages.filter { it.content.contains("error", ignoreCase = true) }
        val messagesWithInfo = messages.filter { it.content.contains("info", ignoreCase = true) }
        
        // Then - Verify content filtering
        assertEquals(2, messagesWithError.size, "Should find 2 messages with 'error' in content")
        assertEquals(2, messagesWithInfo.size, "Should find 2 messages with 'info' in content (info + information)")
        
        // Verify message type prominence is preserved in filtered results (requirement 5.2)
        messagesWithError.forEach { message ->
            assertTrue(message.displayType.isNotEmpty(), "Display type should be prominent in filtered results")
            assertTrue(message.summary.isNotEmpty(), "Summary should be available in filtered results")
        }
    }
}