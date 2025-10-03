package com.genesys.cloud.messenger.transport.util

import android.content.res.Resources
import android.os.Build
import java.util.UUID

/**
 * Android platform-specific implementations of common utility functions.
 */
internal actual class Platform {

    /**
     * The name of the OS currently running on this device.
     */
    actual val os: String = "Android"

    /**
     * The name of the Android SDK version currently running on this device.
     */
    actual val platform: String = "$os ${Build.VERSION.SDK_INT}"

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

    /**
     * Gets the device's preferred language as a lowercase IETF BCP 47 language tag.
     *
     * @return A String like "en-us" or "fr-fr" representing the preferred language.
     */
    actual fun preferredLanguage(): String =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Resources
                .getSystem()
                .configuration.locales
                .get(0)
                .toLanguageTag()
                .lowercase()
        } else {
            Resources
                .getSystem()
                .configuration.locale
                .toLanguageTag()
                .lowercase()
        }
}
