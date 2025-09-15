package com.genesys.cloud.messenger.composeapp.util

import com.genesys.cloud.messenger.composeapp.platform.PlatformContextProvider

/**
 * iOS-specific initialization utilities for the shared module
 */
object IosInitializer {
    
    /**
     * Initialize the shared module for iOS.
     * On iOS, no additional setup is required as the platform context is always available.
     */
    fun initialize() {
        // No additional setup required for iOS
    }
    
    /**
     * Check if the shared module is properly initialized for iOS
     */
    fun isInitialized(): Boolean {
        return PlatformContextProvider.isPlatformContextAvailable()
    }
}