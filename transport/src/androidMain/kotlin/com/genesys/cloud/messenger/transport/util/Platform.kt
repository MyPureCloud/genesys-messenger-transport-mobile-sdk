package com.genesys.cloud.messenger.transport.util

import java.util.UUID

/**
 * Android platform-specific implementations of common utility functions.
 */
internal actual class Platform {
    /**
     * The name of the Android SDK version currently running on this device.
     */
    actual val platform: String = "Android ${android.os.Build.VERSION.SDK_INT}"

    /**
     * Generate a random UUID.
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
