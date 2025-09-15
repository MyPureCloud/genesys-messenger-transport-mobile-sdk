package com.genesys.cloud.messenger.composeapp.platform

import android.content.Context
import com.genesys.cloud.messenger.composeapp.model.PlatformContext
import com.genesys.cloud.messenger.composeapp.model.getPlatformContext

/**
 * Android implementation of PlatformContextProvider
 */
actual object PlatformContextProvider {
    
    private var androidContext: Context? = null
    
    /**
     * Set the Android Context for platform operations.
     * This should be called from the Android app initialization.
     */
    fun setAndroidContext(context: Context) {
        androidContext = context.applicationContext
    }
    
    /**
     * Get the current Android platform context
     */
    actual fun getCurrentPlatformContext(): PlatformContext? {
        return androidContext?.let { getPlatformContext(it) }
    }
    
    /**
     * Check if Android context is available
     */
    actual fun isPlatformContextAvailable(): Boolean {
        return androidContext != null
    }
    
    /**
     * Clear the stored Android context (useful for testing)
     */
    fun clearAndroidContext() {
        androidContext = null
    }
}