package com.genesys.cloud.messenger.transport.util

/**
 * Expect declaration of common utility functions.
 */
expect class Platform() {
    val platform: String

    fun randomUUID(): String

    fun epochMillis(): Long
}
