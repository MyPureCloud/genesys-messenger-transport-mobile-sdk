package com.genesys.cloud.messenger.composeapp

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.genesys.cloud.messenger.composeapp.navigation.AppNavigation
import com.genesys.cloud.messenger.composeapp.theme.AppTheme
import com.genesys.cloud.messenger.composeapp.viewmodel.TestBedViewModel
import com.genesys.cloud.messenger.composeapp.viewmodel.HomeViewModel
import com.genesys.cloud.messenger.composeapp.viewmodel.SettingsViewModel

/**
 * Main App composable that serves as the entry point for the Compose Multiplatform application.
 * 
 * This composable provides a complete messaging application template with:
 * - Cross-platform UI using Compose Multiplatform
 * - Material Design 3 theming with light/dark mode support
 * - Navigation between Home, Interaction, and Settings screens
 * - Shared ViewModels for consistent state management
 * - Error handling and validation
 * - Performance optimizations for both Android and iOS
 * 
 * Architecture:
 * - Uses MVVM pattern with ViewModels for business logic
 * - Implements unidirectional data flow
 * - Separates UI state from business logic
 * - Provides proper lifecycle management
 * 
 * Performance Optimizations:
 * - Stable keys for LazyColumn items
 * - Optimized recomposition with remember and derivedStateOf
 * - Efficient navigation state handling
 * - Memory-conscious ViewModel management
 * 
 * Platform Compatibility:
 * - Android: Full Compose support with Activity integration
 * - iOS: SwiftUI wrapper with native iOS lifecycle
 * - Shared business logic and UI components
 * 
 * Requirements addressed:
 * - 2.1: Shared UI components using Compose Multiplatform
 * - 2.3: Shared ViewModels for business logic
 * - 4.1: Basic navigation between screens
 * - 5.1: Best practices for Compose Multiplatform development
 * - 5.5: Performance optimization and documentation
 * 
 */
@Composable
fun App() {
    // Create ViewModels - in a real app, these would be provided by DI
    val homeViewModel = remember { HomeViewModel() }
    val testBedViewModel = remember { TestBedViewModel() }
    val settingsViewModel = remember { SettingsViewModel() }
    
    // Create navigation controller
    val navController = rememberNavController()
    
    // Observe navigation events from HomeViewModel
    val navigationEvent by homeViewModel.navigationEvent.collectAsState()
    
    // Handle navigation events
    LaunchedEffect(navigationEvent) {
        navigationEvent?.let { event ->
            when (event) {
                is com.genesys.cloud.messenger.composeapp.viewmodel.NavigationEvent.NavigateToScreen -> {
                    when (event.screen) {
                        is com.genesys.cloud.messenger.composeapp.model.Screen.Home -> {
                            navController.navigate("home") {
                                popUpTo("home") { inclusive = true }
                            }
                        }
                        is com.genesys.cloud.messenger.composeapp.model.Screen.Interaction -> {
                            navController.navigate("interaction")
                        }
                        is com.genesys.cloud.messenger.composeapp.model.Screen.Settings -> {
                            navController.navigate("settings")
                        }
                    }
                }
            }
            // Clear the navigation event after handling
            homeViewModel.clearNavigationEvent()
        }
    }
    
    // Apply theme and set up the main UI structure
    AppTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            AppNavigation(
                navController = navController,
                startDestination = "home",
                homeViewModel = homeViewModel,
                testBedViewModel = testBedViewModel,
                settingsViewModel = settingsViewModel
            )
        }
    }
}

/**
 * App composable with ViewModel providers for dependency injection.
 * This version allows external provision of ViewModels for better testability
 * and integration with platform-specific DI frameworks.
 */
@Composable
fun App(
    homeViewModel: HomeViewModel,
    testBedViewModel: TestBedViewModel,
    settingsViewModel: SettingsViewModel
) {
    val navController = rememberNavController()
    
    // Observe navigation events from HomeViewModel
    val navigationEvent by homeViewModel.navigationEvent.collectAsState()
    
    // Handle navigation events
    LaunchedEffect(navigationEvent) {
        navigationEvent?.let { event ->
            when (event) {
                is com.genesys.cloud.messenger.composeapp.viewmodel.NavigationEvent.NavigateToScreen -> {
                    when (event.screen) {
                        is com.genesys.cloud.messenger.composeapp.model.Screen.Home -> {
                            navController.navigate("home") {
                                popUpTo("home") { inclusive = true }
                            }
                        }
                        is com.genesys.cloud.messenger.composeapp.model.Screen.Interaction -> {
                            navController.navigate("interaction")
                        }
                        is com.genesys.cloud.messenger.composeapp.model.Screen.Settings -> {
                            navController.navigate("settings")
                        }
                    }
                }
            }
            homeViewModel.clearNavigationEvent()
        }
    }
    
    AppTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            AppNavigation(
                navController = navController,
                startDestination = "home",
                homeViewModel = homeViewModel,
                testBedViewModel = testBedViewModel,
                settingsViewModel = settingsViewModel
            )
        }
    }
}

/**
 * Lifecycle-aware App composable that handles ViewModel cleanup.
 * This version provides proper lifecycle management for ViewModels.
 */
@Composable
fun AppWithLifecycle(
    onViewModelCleared: () -> Unit = {}
) {
    val homeViewModel = remember { HomeViewModel() }
    val testBedViewModel = remember { TestBedViewModel() }
    val settingsViewModel = remember { SettingsViewModel() }
    
    // Handle lifecycle cleanup using DisposableEffect
    androidx.compose.runtime.DisposableEffect(Unit) {
        onDispose {
            homeViewModel.onCleared()
            testBedViewModel.onCleared()
            settingsViewModel.onCleared()
            onViewModelCleared()
        }
    }
    
    App(
        homeViewModel = homeViewModel,
        testBedViewModel = testBedViewModel,
        settingsViewModel = settingsViewModel
    )
}