package com.genesys.cloud.messenger.composeapp.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for SocketMessage and SocketMessageItem functionality
 * Verifies expandable socket message display requirements
 */
class SocketMessageTest {
    
    @Test
    fun testSocketMessage_displayType_formatsCorrectly() {
        // Given
        val message = SocketMessage(
            id = "test1",
            timestamp = 1234567890L,
            type = "command",
            content = "Test command",
            rawMessage = "Raw command data"
        )
        
        // Then
        assertEquals("Command", message.displayType)
    }
    
    @Test
    fun testSocketMessage_summary_generatesCorrectly() {
        // Given
        val commandMessage = SocketMessage(
            id = "test1",
            timestamp = 1234567890L,
            type = "command",
            content = "Executing: connect",
            rawMessage = "Raw command data"
        )
        
        // Then
        assertEquals("connect", commandMessage.summary)
    }
    
    @Test
    fun testSocketMessage_formattedContent_handlesJson() {
        // Given
        val jsonMessage = SocketMessage(
            id = "test1",
            timestamp = 1234567890L,
            type = "event",
            content = "Event received",
            rawMessage = """{"type":"message","content":"hello","timestamp":123}"""
        )
        
        // Then
        val formatted = jsonMessage.formattedContent
        assertTrue(formatted.contains("\"type\": \"message\""))
        assertTrue(formatted.contains("\"content\": \"hello\""))
        assertTrue(formatted.contains("\"timestamp\": 123"))
    }
    
    @Test
    fun testSocketMessage_priority_assignsCorrectly() {
        // Given
        val errorMessage = SocketMessage(
            id = "test1",
            timestamp = 1234567890L,
            type = "error",
            content = "Error occurred",
            rawMessage = "Error details"
        )
        
        val commandMessage = SocketMessage(
            id = "test2",
            timestamp = 1234567890L,
            type = "command",
            content = "Command executed",
            rawMessage = "Command details"
        )
        
        val infoMessage = SocketMessage(
            id = "test3",
            timestamp = 1234567890L,
            type = "info",
            content = "Info message",
            rawMessage = "Info details"
        )
        
        // Then
        assertEquals(MessagePriority.HIGH, errorMessage.priority)
        assertEquals(MessagePriority.MEDIUM, commandMessage.priority)
        assertEquals(MessagePriority.LOW, infoMessage.priority)
    }
    
    @Test
    fun testSocketMessageItem_initialState() {
        // Given
        val message = SocketMessage(
            id = "test1",
            timestamp = 1234567890L,
            type = "command",
            content = "Test command",
            rawMessage = "Raw command data"
        )
        val messageItem = SocketMessageItem(message = message)
        
        // Then
        assertFalse(messageItem.isExpanded)
        assertEquals("test1", messageItem.id)
        assertEquals("Command", messageItem.displayType)
    }
    
    @Test
    fun testSocketMessageItem_toggleExpanded() {
        // Given
        val message = SocketMessage(
            id = "test1",
            timestamp = 1234567890L,
            type = "command",
            content = "Test command",
            rawMessage = "Raw command data"
        )
        val messageItem = SocketMessageItem(message = message, isExpanded = false)
        
        // When
        val expandedItem = messageItem.toggleExpanded()
        
        // Then
        assertTrue(expandedItem.isExpanded)
        assertFalse(messageItem.isExpanded) // Original should be unchanged
    }
    
    @Test
    fun testSocketMessageItem_withExpanded() {
        // Given
        val message = SocketMessage(
            id = "test1",
            timestamp = 1234567890L,
            type = "command",
            content = "Test command",
            rawMessage = "Raw command data"
        )
        val messageItem = SocketMessageItem(message = message, isExpanded = false)
        
        // When
        val expandedItem = messageItem.withExpanded(true)
        val collapsedItem = messageItem.withExpanded(false)
        
        // Then
        assertTrue(expandedItem.isExpanded)
        assertFalse(collapsedItem.isExpanded)
        assertFalse(messageItem.isExpanded) // Original should be unchanged
    }
    
    @Test
    fun testSocketMessage_jsonFormatting_handlesComplexJson() {
        // Given
        val complexJson = """{"user":{"name":"John","age":30},"messages":[{"id":1,"text":"hello"},{"id":2,"text":"world"}],"active":true}"""
        val message = SocketMessage(
            id = "test1",
            timestamp = 1234567890L,
            type = "event",
            content = "Complex event",
            rawMessage = complexJson
        )
        
        // Then
        val formatted = message.formattedContent
        assertTrue(formatted.contains("\"user\": {"))
        assertTrue(formatted.contains("\"name\": \"John\""))
        assertTrue(formatted.contains("\"messages\": ["))
        assertTrue(formatted.contains("\"active\": true"))
        // Should have proper indentation
        assertTrue(formatted.contains("  "))
    }
    
    @Test
    fun testSocketMessage_textFormatting_handlesStructuredText() {
        // Given
        val structuredText = """
            Event Type: MessageReceived
            Sender: user123
            Content: Hello world
            Timestamp: 1234567890
        """.trimIndent()
        
        val message = SocketMessage(
            id = "test1",
            timestamp = 1234567890L,
            type = "event",
            content = "Message event",
            rawMessage = structuredText
        )
        
        // Then
        val formatted = message.formattedContent
        assertTrue(formatted.contains("Event Type: MessageReceived"))
        assertTrue(formatted.contains("Sender: user123"))
        assertTrue(formatted.contains("Content: Hello world"))
    }
}