package com.genesys.cloud.messenger.composeapp.model

import android.content.Context
import android.webkit.MimeTypeMap
import java.io.File

/**
 * Android implementation of PlatformContext
 */
actual class PlatformContext(private val context: Context) {
    
    /**
     * Get the underlying Android Context
     */
    actual fun getContext(): Any = context
    
    /**
     * Get the Android app storage directory for saved attachments
     */
    actual fun getStorageDirectory(): String {
        // Use app-specific external files directory if available, otherwise internal files
        return context.getExternalFilesDir("attachments")?.absolutePath 
            ?: File(context.filesDir, "attachments").absolutePath
    }
    
    /**
     * Load saved attachment files from Android storage
     */
    actual fun loadSavedAttachments(): List<SavedAttachment> {
        val attachmentsDir = File(getStorageDirectory())
        
        if (!attachmentsDir.exists()) {
            attachmentsDir.mkdirs()
            return emptyList()
        }
        
        return attachmentsDir.listFiles()?.mapNotNull { file ->
            if (file.isFile) {
                val mimeType = getMimeType(file.name) ?: "application/octet-stream"
                SavedAttachment(
                    name = file.name,
                    path = file.absolutePath,
                    size = file.length(),
                    mimeType = mimeType
                )
            } else null
        } ?: emptyList()
    }
    
    /**
     * Set up vault context for encrypted or default vault on Android
     */
    actual fun setupVaultContext(useEncryptedVault: Boolean) {
        try {
            if (useEncryptedVault) {
                // Set up encrypted vault context
                com.genesys.cloud.messenger.transport.util.EncryptedVault.context = context
            } else {
                // Set up default vault context
                com.genesys.cloud.messenger.transport.util.DefaultVault.context = context
            }
        } catch (e: Exception) {
            // Log error but don't fail initialization
            println("Warning: Failed to set up vault context: ${e.message}")
        }
    }
    
    /**
     * Get MIME type for a file based on its extension
     */
    private fun getMimeType(fileName: String): String? {
        val extension = fileName.substringAfterLast('.', "")
        return if (extension.isNotEmpty()) {
            MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.lowercase())
        } else null
    }
}

/**
 * Get Android platform context
 */
actual fun getPlatformContext(): PlatformContext {
    throw IllegalStateException(
        "getPlatformContext() must be called with Android Context. " +
        "Use getPlatformContext(context: Context) instead."
    )
}

/**
 * Get Android platform context with Context parameter
 */
fun getPlatformContext(context: Context): PlatformContext {
    return PlatformContext(context)
}