package com.genesys.cloud.messenger.composeapp.util

import android.content.Context
import com.genesys.cloud.messenger.composeapp.platform.PlatformContextProvider

/**
 * Android-specific initialization utilities for the shared module
 */
object AndroidInitializer {
    
    /**
     * Initialize the shared module with Android Context.
     * This should be called from the Android Application class or MainActivity.
     */
    fun initialize(context: Context) {
        PlatformContextProvider.setAndroidContext(context)
    }
    
    /**
     * Check if the shared module is properly initialized for Android
     */
    fun isInitialized(): Boolean {
        return PlatformContextProvider.isPlatformContextAvailable()
    }
}