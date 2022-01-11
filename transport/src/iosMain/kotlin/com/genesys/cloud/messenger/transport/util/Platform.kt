package com.genesys.cloud.messenger.transport.util

import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import platform.Foundation.NSUUID
import platform.UIKit.UIDevice
import platform.posix.gettimeofday
import platform.posix.timeval

/**
 * Actual iOS implementation of common, platform specific utility functions.
 */
actual class Platform {
    /**
     * Get current platform name on iOS.
     */
    actual val platform: String =
        UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion

    /**
     * Generates a random UUID string.
     */
    actual fun randomUUID(): String = NSUUID().UUIDString()

    /**
     * @return the current time in milliseconds.
     */
    actual fun epochMillis(): Long = memScoped {
        val timeVal = alloc<timeval>()
        gettimeofday(timeVal.ptr, null)
        (timeVal.tv_sec * 1000) + (timeVal.tv_usec / 1000)
    }
}
