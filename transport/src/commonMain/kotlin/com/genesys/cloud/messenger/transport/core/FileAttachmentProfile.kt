package com.genesys.cloud.messenger.transport.core

/**
 * Represents a configuration for supported content profiles in the Admin Console.
 *
 * @property enabled indicates if file attachment feature is enabled in Admin Console.
 * @property allowedFileTypes the list of file types allowed for upload.
 * @property blockedFileTypes the list of file types that are NOT allowed for upload.
 * @property maxFileSizeKB the maximum allowed file size in kilobytes.
 * @property hasWildCard indicates if a wildcard is present in [allowedFileTypes]
 */
data class FileAttachmentProfile(
    val enabled: Boolean = false,
    val allowedFileTypes: List<String> = emptyList(),
    val blockedFileTypes: List<String> = emptyList(),
    val maxFileSizeKB: Long = 0,
    val hasWildCard: Boolean = false,
)
