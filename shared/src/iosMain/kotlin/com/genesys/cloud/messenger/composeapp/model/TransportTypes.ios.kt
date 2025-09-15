package com.genesys.cloud.messenger.composeapp.model

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.*
import platform.UniformTypeIdentifiers.*

/**
 * iOS implementation of PlatformContext
 */
@OptIn(ExperimentalForeignApi::class)
actual class PlatformContext(private val context: Any = Any()) {
    
    /**
     * Get the underlying iOS context
     */
    actual fun getContext(): Any = context
    
    /**
     * Get the iOS app storage directory for saved attachments
     */
    actual fun getStorageDirectory(): String {
        // Use Documents directory for saved attachments
        val documentsPath = NSSearchPathForDirectoriesInDomains(
            NSDocumentDirectory,
            NSUserDomainMask,
            true
        ).firstOrNull() as? String
        
        return if (documentsPath != null) {
            "$documentsPath/attachments"
        } else {
            // Fallback to temporary directory
            "${NSTemporaryDirectory()}attachments"
        }
    }
    
    /**
     * Load saved attachment files from iOS storage
     */
    actual fun loadSavedAttachments(): List<SavedAttachment> {
        val attachmentsDir = getStorageDirectory()
        val fileManager = NSFileManager.defaultManager
        
        // Create directory if it doesn't exist
        if (!fileManager.fileExistsAtPath(attachmentsDir)) {
            fileManager.createDirectoryAtPath(
                attachmentsDir,
                withIntermediateDirectories = true,
                attributes = null,
                error = null
            )
            return emptyList()
        }
        
        val contents = fileManager.contentsOfDirectoryAtPath(attachmentsDir, error = null)
        
        return contents?.mapNotNull { fileName ->
            val filePath = "$attachmentsDir/$fileName"
            val attributes = fileManager.attributesOfItemAtPath(filePath, error = null)
            
            if (attributes != null) {
                val fileSize = (attributes[NSFileSize] as? NSNumber)?.longValue ?: 0L
                val mimeType = getMimeType(fileName as String)
                
                SavedAttachment(
                    name = fileName,
                    path = filePath,
                    size = fileSize,
                    mimeType = mimeType
                )
            } else null
        } ?: emptyList()
    }
    
    /**
     * Set up vault context for encrypted or default vault on iOS
     */
    actual fun setupVaultContext(useEncryptedVault: Boolean) {
        // iOS vault implementations don't require explicit context setup
        // They work directly with the iOS Keychain
        // No action needed - vault context is ready by default on iOS
    }
    
    /**
     * Get MIME type for a file based on its extension
     */
    private fun getMimeType(fileName: String): String {
        val extension = fileName.substringAfterLast('.', "")
        
        return if (extension.isNotEmpty()) {
            // Try to get UTType for the extension
            val utType = UTType.typeWithFilenameExtension(extension)
            utType?.preferredMIMEType ?: "application/octet-stream"
        } else {
            "application/octet-stream"
        }
    }
}

/**
 * Get iOS platform context
 */
actual fun getPlatformContext(): PlatformContext {
    return PlatformContext()
}