package com.genesys.cloud.messenger.transport.util

import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import platform.Foundation.NSLocale
import platform.Foundation.NSUUID
import platform.Foundation.currentLocale
import platform.Foundation.languageCode
import platform.Foundation.preferredLanguages
import platform.UIKit.UIDevice
import platform.posix.gettimeofday
import platform.posix.timeval

/**
 * iOS platform-specific implementations of common utility functions.
 */
@OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
internal actual class Platform {
    /**
     * The name of the OS currently running on this device.
     */
    actual val os: String = UIDevice.currentDevice.systemName()

    /**
     * The device name and system version currently running on this device.
     */
    actual val platform: String =
        os + " " + UIDevice.currentDevice.systemVersion

    /**
     * Generate a random UUID.
     *
     * @return a random UUID string.
     */
    actual fun randomUUID(): String = NSUUID().UUIDString()

    /**
     * Get the current time in milliseconds.
     *
     * @return the difference, in milliseconds, between current time and midnight January 1, 1970 UTC.
     */
    actual fun epochMillis(): Long = memScoped {
        val timeVal = alloc<timeval>()
        gettimeofday(timeVal.ptr, null)
        (timeVal.tv_sec * 1000) + (timeVal.tv_usec / 1000)
    }

    /**
     * Gets the device's preferred language as a lowercase string.
     *
     * @return A lowercase String representing the preferred language. This may be a full
     * IETF BCP 47 language tag (e.g., "en-us", "fr-fr") or just a language code
     * (e.g., "en", "fr"), depending on the system's locale settings.
     */
    actual fun preferredLanguage(): String = NSLocale.preferredLanguages.run {
        if (this.isNotEmpty()) {
            (first() as String).lowercase()
        } else {
            NSLocale.currentLocale.languageCode.lowercase()
        }
    }
}
