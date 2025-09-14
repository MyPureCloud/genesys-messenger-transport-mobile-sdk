package com.genesys.cloud.messenger.composeapp.platform

import platform.UIKit.UIDevice

/**
 * iOS implementation of Platform
 */
actual class Platform actual constructor() {
    actual val name: String = "iOS"
    actual val version: String = UIDevice.currentDevice.systemVersion
    actual val isDebug: Boolean = platform.Foundation.NSBundle.mainBundle.objectForInfoDictionaryKey("CFBundleExecutable") != null
}

/**
 * Get the iOS platform instance
 */
actual fun getPlatform(): Platform = Platform()