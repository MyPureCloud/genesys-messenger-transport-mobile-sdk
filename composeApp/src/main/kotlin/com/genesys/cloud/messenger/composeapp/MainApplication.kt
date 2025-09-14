package com.genesys.cloud.messenger.composeapp

import android.app.Application
import android.util.Log

/**
 * MainApplication class for the Android Compose Multiplatform template application.
 * 
 * This class handles application-level initialization and configuration.
 * In a production app, this would typically set up dependency injection,
 * crash reporting, analytics, and other global services.
 * 
 * Requirements addressed:
 * - 2.3: Android app uses shared UI components and ViewModels
 * - 3.4: Android app compiles successfully
 */
class MainApplication : Application() {
    
    companion object {
        private const val TAG = "MainApplication"
        
        /**
         * Global application instance for accessing application context
         * from shared code when needed.
         */
        lateinit var instance: MainApplication
            private set
    }
    
    override fun onCreate() {
        super.onCreate()
        
        // Store application instance for global access
        instance = this
        
        Log.d(TAG, "MainApplication initialized")
        
        // Initialize application-level components
        initializeApplication()
    }
    
    /**
     * Initialize application-level components and services.
     * This method can be extended to set up:
     * - Dependency injection (e.g., Hilt, Koin)
     * - Crash reporting (e.g., Crashlytics)
     * - Analytics (e.g., Firebase Analytics)
     * - Network configuration
     * - Database initialization
     * - Logging configuration
     */
    private fun initializeApplication() {
        try {
            // Initialize shared module components if needed
            initializeSharedComponents()
            
            // Initialize transport module if needed
            initializeTransportModule()
            
            Log.d(TAG, "Application components initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing application components", e)
        }
    }
    
    /**
     * Initialize shared module components.
     * This can include setting up platform-specific implementations
     * for expect/actual declarations.
     */
    private fun initializeSharedComponents() {
        // Platform-specific initialization for shared module
        // This could include setting up Android-specific implementations
        // for platform interfaces defined in the shared module
        Log.d(TAG, "Shared components initialized")
    }
    
    /**
     * Initialize transport module components.
     * This can include setting up messaging client configuration,
     * network settings, and other transport-related services.
     */
    private fun initializeTransportModule() {
        // Transport module initialization
        // This could include setting up messaging client,
        // configuring network settings, etc.
        Log.d(TAG, "Transport module initialized")
    }
    
    /**
     * Get application context from anywhere in the app.
     * This is useful for shared code that needs Android context.
     */
    fun getAppContext(): Application = this
    
    override fun onTerminate() {
        super.onTerminate()
        Log.d(TAG, "MainApplication terminated")
    }
    
    override fun onLowMemory() {
        super.onLowMemory()
        Log.w(TAG, "Low memory warning received")
    }
    
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        Log.w(TAG, "Memory trim requested with level: $level")
    }
}