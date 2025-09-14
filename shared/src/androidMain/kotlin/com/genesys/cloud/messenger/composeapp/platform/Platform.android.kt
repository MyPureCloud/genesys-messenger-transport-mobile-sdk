package com.genesys.cloud.messenger.composeapp.platform

import android.os.Build

/**
 * Android implementation of Platform
 */
actual class Platform actual constructor() {
    actual val name: String = "Android"
    actual val version: String = "${Build.VERSION.SDK_INT}"
    actual val isDebug: Boolean = android.util.Log.isLoggable("DEBUG", android.util.Log.DEBUG)
}

/**
 * Get the Android platform instance
 */
actual fun getPlatform(): Platform = Platform()