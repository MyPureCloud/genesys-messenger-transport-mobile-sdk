package com.genesys.cloud.messenger.composeapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge

/**
 * MainActivity for the Android Compose Multiplatform template application.
 * 
 * This activity serves as the entry point for the Android app and integrates
 * the shared Compose UI with Android-specific lifecycle management.
 * 
 * Requirements addressed:
 * - 2.3: Android app uses shared UI components and ViewModels
 * - 3.4: Android app compiles and displays the same UI as iOS
 */
class MainActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Enable edge-to-edge display for modern Android UI
        enableEdgeToEdge()
        
        setContent {
            // Launch the shared App composable
            // The App composable will create its own ViewModels internally
            App()
        }
    }
}