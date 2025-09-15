package com.genesys.cloud.messenger.composeapp.model

/**
 * Represents a command that can be executed in the TestBed interface.
 * 
 * @param name The command name (e.g., "connect", "send")
 * @param description Human-readable description of what the command does
 * @param requiresInput Whether this command requires additional input parameters
 * @param inputPlaceholder Placeholder text for the input field when requiresInput is true
 */
data class Command(
    val name: String,
    val description: String,
    val requiresInput: Boolean = false,
    val inputPlaceholder: String? = null
)