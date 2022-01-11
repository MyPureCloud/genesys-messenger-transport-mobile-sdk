package com.genesys.cloud.messenger.transport.util

import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import platform.Foundation.NSUUID
import platform.UIKit.UIDevice
import platform.posix.gettimeofday
import platform.posix.timeval

/**
 * iOS platform-specific implementations of common utility functions.
 */
actual class Platform {
    /**
     * The device name and system version currently running on this device.
     */
    actual val platform: String =
        UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion

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
}
