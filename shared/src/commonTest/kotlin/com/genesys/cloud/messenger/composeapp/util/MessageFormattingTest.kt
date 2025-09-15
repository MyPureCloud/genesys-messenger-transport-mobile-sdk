package com.genesys.cloud.messenger.composeapp.util

import com.genesys.cloud.messenger.composeapp.model.Event
import com.genesys.cloud.messenger.composeapp.model.MessageEvent
import com.genesys.cloud.messenger.composeapp.model.MessagingClientState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MessageFormattingTest {
    
    @Test
    fun testFormatStateChangeContent() {
        val oldState = MessagingClientState.Idle
        val newState = MessagingClientState.Connected
        val formatted = MessageFormatting.formatStateChangeContent(oldState, newState)
        assertEquals("Idle → Connected", formatted)
    }
    
    @Test
    fun testFormatMessageContentForTextMessage() {
        val messageEvent = MessageEvent(
            type = "text",
            content = "Hello, this is a test message that is longer than fifty characters",
            timestamp = 0L
        )
        val formatted = MessageFormatting.formatMessageContent(messageEvent)
        assertEquals("Text: Hello, this is a test message that is longer than...", formatted)
    }
    
    @Test
    fun testFormatMessageContentForTypingIndicator() {
        val messageEvent = MessageEvent(
            type = "typing",
            content = "User is typing a message",
            timestamp = 0L
        )
        val formatted = MessageFormatting.formatMessageContent(messageEvent)
        assertEquals("Typing indicator: User is typing a message", formatted)
    }
    
    @Test
    fun testFormatMessageContentForAttachment() {
        val messageEvent = MessageEvent(
            type = "attachment",
            content = "name: document.pdf, size: 1024",
            timestamp = 0L
        )
        val formatted = MessageFormatting.formatMessageContent(messageEvent)
        assertEquals("Attachment: document.pdf", formatted)
    }
    
    @Test
    fun testFormatEventContentForConnection() {
        val event = Event(
            type = "connected",
            data = "Successfully connected to server"
        )
        val formatted = MessageFormatting.formatEventContent(event)
        assertEquals("Connected to messaging service", formatted)
    }
    
    @Test
    fun testFormatEventContentForError() {
        val event = Event(
            type = "error",
            data = "Connection failed due to network timeout error"
        )
        val formatted = MessageFormatting.formatEventContent(event)
        assertEquals("Error: Connection failed due to network timeout error", formatted)
    }
    
    @Test
    fun testGetDisconnectionReason() {
        assertEquals("Connection failed", MessageFormatting.getDisconnectionReason(MessagingClientState.Connecting))
        assertEquals("Normal disconnection", MessageFormatting.getDisconnectionReason(MessagingClientState.Connected))
        assertEquals("Error state disconnection", MessageFormatting.getDisconnectionReason(MessagingClientState.Error))
        assertEquals("Unknown reason", MessageFormatting.getDisconnectionReason(MessagingClientState.Idle))
    }
    
    @Test
    fun testParseAttachmentInfo() {
        val content = """name: "document.pdf", size: 1024, type: "application/pdf""""
        val info = MessageFormatting.parseAttachmentInfo(content)
        
        assertEquals("document.pdf", info["name"])
        assertEquals("1024", info["size"])
        assertEquals("application/pdf", info["type"])
    }
    
    @Test
    fun testParseQuickReplyOptions() {
        val content = """options: ["Yes", "No", "Maybe"]"""
        val options = MessageFormatting.parseQuickReplyOptions(content)
        
        assertEquals(3, options.size)
        assertTrue(options.contains("Yes"))
        assertTrue(options.contains("No"))
        assertTrue(options.contains("Maybe"))
    }
    
    @Test
    fun testParseQuickReplyOptionsFromNumberedList() {
        val content = """
            Available options:
            1. Accept the invitation
            2. Decline the invitation
            3. Ask for more information
        """.trimIndent()
        
        val options = MessageFormatting.parseQuickReplyOptions(content)
        
        assertEquals(3, options.size)
        assertTrue(options.contains("Accept the invitation"))
        assertTrue(options.contains("Decline the invitation"))
        assertTrue(options.contains("Ask for more information"))
    }
    
    @Test
    fun testParseErrorInfo() {
        val errorData = """code: 500, message: "Internal server error", type: "ServerError""""
        val info = MessageFormatting.parseErrorInfo(errorData)
        
        assertEquals("500", info["code"])
        assertEquals("Internal server error", info["message"])
        assertEquals("ServerError", info["type"])
    }
}