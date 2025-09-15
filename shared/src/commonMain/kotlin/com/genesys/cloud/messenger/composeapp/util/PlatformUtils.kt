package com.genesys.cloud.messenger.composeapp.util

import com.genesys.cloud.messenger.composeapp.model.PlatformContext
import com.genesys.cloud.messenger.composeapp.platform.PlatformContextProvider

/**
 * Utility functions for platform-specific operations
 */
object PlatformUtils {
    
    /**
     * Get the current platform context for TestBedViewModel initialization.
     * Returns null if platform context is not available (e.g., Android context not set).
     */
    fun getCurrentPlatformContext(): PlatformContext? {
        return PlatformContextProvider.getCurrentPlatformContext()
    }
    
    /**
     * Check if platform context is available for TestBedViewModel initialization
     */
    fun isPlatformContextReady(): Boolean {
        return PlatformContextProvider.isPlatformContextAvailable()
    }
    
    /**
     * Get platform context or throw exception if not available
     */
    fun requirePlatformContext(): PlatformContext {
        return getCurrentPlatformContext() 
            ?: throw IllegalStateException("Platform context is not available. Ensure platform context is properly initialized.")
    }
}