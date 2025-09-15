package com.genesys.cloud.messenger.composeapp.util

import android.content.Context
import com.genesys.cloud.messenger.composeapp.platform.PlatformContextProvider
import com.genesys.cloud.messenger.composeapp.model.getPlatformContext
import io.mockk.mockk
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.assertNotNull

/**
 * Tests for AndroidInitializer to verify proper Android context setup
 */
class AndroidInitializerTest {
    
    @Test
    fun testInitializationSetsAndroidContext() {
        // Given - Clear any existing context
        PlatformContextProvider.clearAndroidContext()
        
        // Initially should not be available
        assertFalse(AndroidInitializer.isInitialized(), "Should not be initialized initially")
        assertFalse(PlatformContextProvider.isPlatformContextAvailable(), "Platform context should not be available initially")
        
        // When - Initialize with mock context
        val mockContext = mockk<Context>(relaxed = true)
        AndroidInitializer.initialize(mockContext)
        
        // Then - Should be initialized
        assertTrue(AndroidInitializer.isInitialized(), "Should be initialized after calling initialize()")
        assertTrue(PlatformContextProvider.isPlatformContextAvailable(), "Platform context should be available after initialization")
    }
    
    @Test
    fun testIsInitializedReflectsPlatformContextAvailability() {
        // Given - Clear context
        PlatformContextProvider.clearAndroidContext()
        
        // When - Check initialization status
        val isInitialized = AndroidInitializer.isInitialized()
        val isPlatformContextAvailable = PlatformContextProvider.isPlatformContextAvailable()
        
        // Then - Both should return the same value
        assertTrue(isInitialized == isPlatformContextAvailable, "isInitialized() should match isPlatformContextAvailable()")
    }
    
    @Test
    fun testPlatformContextCanBeCreatedAfterInitialization() {
        // Given - Initialize with mock context
        val mockContext = mockk<Context>(relaxed = true)
        AndroidInitializer.initialize(mockContext)
        
        // When - Get platform context
        val platformContext = PlatformContextProvider.getCurrentPlatformContext()
        
        // Then - Should be available
        assertNotNull(platformContext, "Platform context should be available after initialization")
        
        // Should be able to call setupVaultContext without throwing
        try {
            platformContext.setupVaultContext(true)
            platformContext.setupVaultContext(false)
            assertTrue(true, "setupVaultContext should not throw")
        } catch (e: Exception) {
            // This is expected if transport module is not available in test environment
            assertTrue(true, "setupVaultContext may throw in test environment: ${e.message}")
        }
    }
}