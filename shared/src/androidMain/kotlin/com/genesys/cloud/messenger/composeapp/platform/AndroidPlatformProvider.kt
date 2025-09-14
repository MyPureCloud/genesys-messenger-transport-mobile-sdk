package com.genesys.cloud.messenger.composeapp.platform

import android.content.Context

/**
 * Android-specific platform provider
 */
object AndroidPlatformProvider {
    
    /**
     * Create a PlatformManager instance for Android
     */
    fun createPlatformManager(context: Context): PlatformManager {
        return PlatformManager(AndroidPlatformContext(context))
    }
    
    /**
     * Get platform operations for Android
     */
    fun getPlatformOperations(context: Context): PlatformOperations {
        return PlatformOperations(AndroidPlatformContext(context))
    }
}