package com.genesys.cloud.messenger.transport.util

import java.util.UUID

/**
 * Android platform-specific implementations of common utility functions.
 */
actual class Platform {
    /**
     * Get current platform name on Android.
     */
    actual val platform: String = "Android ${android.os.Build.VERSION.SDK_INT}"

    /**
     * Generate a random UUID string.
     *
     * @return a random UUID string.
     */
    actual fun randomUUID() = UUID.randomUUID().toString()

    /**
     * Get the current time in milliseconds.
     *
     * @return the difference, in milliseconds, between current time and midnight January 1, 1970 UTC.
     */
    actual fun epochMillis(): Long = System.currentTimeMillis()
}
