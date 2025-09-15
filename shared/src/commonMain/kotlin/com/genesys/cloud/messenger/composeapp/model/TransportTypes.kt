package com.genesys.cloud.messenger.composeapp.model

/**
 * Placeholder types for Transport SDK components.
 * These will be replaced with actual transport module types when available.
 */

/**
 * Represents the state of the messaging client
 */
enum class MessagingClientState {
    Idle,
    Connecting,
    Connected,
    Disconnecting,
    Disconnected,
    Error
}

/**
 * Placeholder for MessengerTransportSDK
 * TODO: Replace with actual transport SDK when available
 */
class MessengerTransportSDK

/**
 * Placeholder for MessagingClient
 * TODO: Replace with actual transport SDK when available
 */
class MessagingClient

/**
 * Placeholder for PushService with device token management functionality
 * TODO: Replace with actual transport SDK when available
 */
class PushService {
    private var deviceToken: String? = null
    
    /**
     * Get the current device token for push notifications
     */
    fun getDeviceToken(): String? = deviceToken
    
    /**
     * Set the device token for push notifications
     */
    fun setDeviceToken(token: String) {
        deviceToken = token
    }
    
    /**
     * Clear the stored device token
     */
    fun clearDeviceToken() {
        deviceToken = null
    }
    
    /**
     * Synchronize device token with the server
     * TODO: Implement actual server synchronization when transport module is available
     */
    suspend fun synchronizeDeviceToken(token: String) {
        // Placeholder implementation
        setDeviceToken(token)
    }
    
    /**
     * Unregister device from push notifications
     * TODO: Implement actual server unregistration when transport module is available
     */
    suspend fun unregisterFromPush(token: String) {
        // Placeholder implementation
        clearDeviceToken()
    }
    
    /**
     * Check if push notifications are enabled/registered
     */
    fun isPushRegistered(): Boolean = deviceToken != null
}

/**
 * Platform-specific context for operations that require platform access.
 * Provides access to platform-specific functionality needed by the TestBedViewModel.
 */
expect class PlatformContext {
    /**
     * Get the underlying platform context (Android Context or iOS equivalent)
     */
    fun getContext(): Any
    
    /**
     * Get the storage directory for saved attachments
     */
    fun getStorageDirectory(): String
    
    /**
     * Load saved attachment files from platform storage
     */
    fun loadSavedAttachments(): List<SavedAttachment>
    
    /**
     * Set up vault context for encrypted or default vault
     */
    fun setupVaultContext(useEncryptedVault: Boolean)
}

/**
 * Placeholder for FileAttachmentProfile
 * TODO: Replace with actual transport SDK when available
 */
data class FileAttachmentProfile(
    val maxFileSizeBytes: Long = 25 * 1024 * 1024, // 25MB default
    val allowedFileTypes: List<String> = listOf("image/*", "application/pdf", "text/*"),
    val hasInlinePreviews: Boolean = true,
    val hasAttachmentPreviews: Boolean = true
)

/**
 * Represents an attachment in the messaging system
 */
data class Attachment(
    val id: String,
    val fileName: String,
    val url: String? = null,
    val mimeType: String,
    val sizeBytes: Long,
    val state: AttachmentState = AttachmentState.Uploading
)

/**
 * Represents the state of an attachment
 */
enum class AttachmentState {
    Uploading,
    Uploaded,
    Failed,
    Detached
}

/**
 * Result of command validation
 */
sealed class ValidationResult {
    object Valid : ValidationResult()
    data class Invalid(val message: String) : ValidationResult()
}

/**
 * Configuration for transport SDK initialization
 */
data class TransportConfiguration(
    val deploymentId: String,
    val domain: String,
    val logging: Boolean = true,
    val encryptedVault: Boolean = true
)

/**
 * Placeholder for MessageEvent
 * TODO: Replace with actual transport SDK when available
 */
data class MessageEvent(
    val type: String,
    val content: String = "",
    val timestamp: Long = 0L
) {
    override fun toString(): String = "MessageEvent(type='$type', content='$content', timestamp=$timestamp)"
}

/**
 * Placeholder for Event
 * TODO: Replace with actual transport SDK when available
 */
data class Event(
    val type: String,
    val data: String = ""
) {
    override fun toString(): String = "Event(type='$type', data='$data')"
}

/**
 * Represents a saved attachment file that can be used for testing
 */
data class SavedAttachment(
    val name: String,
    val path: String,
    val size: Long,
    val mimeType: String
)

/**
 * Get platform-specific context
 * TODO: Implement platform-specific versions when available
 */
expect fun getPlatformContext(): PlatformContext