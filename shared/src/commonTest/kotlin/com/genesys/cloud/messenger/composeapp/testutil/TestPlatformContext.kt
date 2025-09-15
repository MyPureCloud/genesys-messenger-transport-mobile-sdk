package com.genesys.cloud.messenger.composeapp.testutil

import com.genesys.cloud.messenger.composeapp.model.SavedAttachment

/**
 * Test utilities for creating mock data and platform contexts for unit testing.
 */

/**
 * Helper function to create test SavedAttachment instances
 */
fun createTestSavedAttachment(
    name: String = "test-image.jpg",
    path: String = "/test/path/test-image.jpg",
    size: Long = 1024,
    mimeType: String = "image/jpeg"
): SavedAttachment {
    return SavedAttachment(
        name = name,
        path = path,
        size = size,
        mimeType = mimeType
    )
}

/**
 * Helper function to create a list of test SavedAttachment instances
 */
fun createTestSavedAttachments(): List<SavedAttachment> {
    return listOf(
        createTestSavedAttachment(
            name = "test-image.jpg",
            path = "/test/path/test-image.jpg",
            size = 1024,
            mimeType = "image/jpeg"
        ),
        createTestSavedAttachment(
            name = "test-document.pdf",
            path = "/test/path/test-document.pdf",
            size = 2048,
            mimeType = "application/pdf"
        )
    )
}