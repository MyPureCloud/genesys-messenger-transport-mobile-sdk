package com.genesys.cloud.messenger.transport.util

import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import platform.Foundation.NSUUID
import platform.UIKit.UIDevice
import platform.posix.gettimeofday
import platform.posix.timeval

actual class Platform {
    actual val platform: String =
        UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion

    actual fun randomUUID(): String = NSUUID().UUIDString()

    actual fun epochMillis(): Long = memScoped {
        val timeVal = alloc<timeval>()
        gettimeofday(timeVal.ptr, null)
        (timeVal.tv_sec * 1000) + (timeVal.tv_usec / 1000)
    }
}
