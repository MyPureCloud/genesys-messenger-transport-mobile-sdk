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
     * Generates a random UUID string.
     */
    actual fun randomUUID() = UUID.randomUUID().toString()

    /**
     * @return the current time in milliseconds.
     */
    actual fun epochMillis(): Long = System.currentTimeMillis()
}
