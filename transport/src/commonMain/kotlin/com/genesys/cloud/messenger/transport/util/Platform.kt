package com.genesys.cloud.messenger.transport.util

/**
 * Expect declaration of common utility functions.
 */
expect class Platform() {
    /**
     * Get current platform name.
     */
    val platform: String

    /**
     * Generates a random UUID string.
     */
    fun randomUUID(): String

    /**
     * @return the current time in milliseconds.
     */
    fun epochMillis(): Long
}
