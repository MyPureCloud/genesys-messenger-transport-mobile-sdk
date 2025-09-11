package com.genesys.cloud.messenger.transport.util

/**
 * Expect declaration of common utility functions.
 */
internal expect class Platform() {
    /**
     * The name of the OS currently running on this device.
     */
    val os: String

    /**
     * The name of the OS and its version currently running on this device.
     */
    val platform: String

    /**
     * Generate a random UUID.
     *
     * @return a random UUID string.
     */
    fun randomUUID(): String

    /**
     * Get the current time in milliseconds.
     *
     * @return the difference, in milliseconds, between current time and midnight January 1, 1970 UTC.
     */
    fun epochMillis(): Long

    fun preferredLanguage(): String
}
