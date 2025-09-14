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
import com.genesys.cloud.messenger.composeapp.model.ThemeMode
import com.genesys.cloud.messenger.composeapp.viewmodel.ChatViewModel
import com.genesys.cloud.messenger.composeapp.viewmodel.HomeViewModel
import com.genesys.cloud.messenger.composeapp.viewmodel.SettingsViewModel

/**
 * Main App composable that serves as the entry point for the Compose Multiplatform application.
 * 
 * This composable integrates:
 * - Theme system with Material Design 3
 * - Navigation using Compose Navigation
 * - ViewModels for state management
 * - Proper lifecycle handling
 * 
 * Requirements addressed:
 * - 2.1: Shared UI components using Compose Multiplatform
 * - 2.3: Shared ViewModels for business logic
 * - 4.1: Basic navigation between screens
 */
@Composable
fun App(
    themeMode: ThemeMode = ThemeMode.System
) {
    // Create ViewModels - in a real app, these would be provided by DI
    val homeViewModel = remember { HomeViewModel() }
    val chatViewModel = remember { ChatViewModel() }
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
                        is com.genesys.cloud.messenger.composeapp.model.Screen.Chat -> {
                            navController.navigate("chat")
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
    AppTheme(themeMode = themeMode) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            AppNavigation(
                navController = navController,
                startDestination = "home",
                homeViewModel = homeViewModel,
                chatViewModel = chatViewModel,
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
    chatViewModel: ChatViewModel,
    settingsViewModel: SettingsViewModel,
    themeMode: ThemeMode = ThemeMode.System
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
                        is com.genesys.cloud.messenger.composeapp.model.Screen.Chat -> {
                            navController.navigate("chat")
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
    
    AppTheme(themeMode = themeMode) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            AppNavigation(
                navController = navController,
                startDestination = "home",
                homeViewModel = homeViewModel,
                chatViewModel = chatViewModel,
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
    themeMode: ThemeMode = ThemeMode.System,
    onViewModelCleared: () -> Unit = {}
) {
    val homeViewModel = remember { HomeViewModel() }
    val chatViewModel = remember { ChatViewModel() }
    val settingsViewModel = remember { SettingsViewModel() }
    
    // Handle lifecycle cleanup using DisposableEffect
    androidx.compose.runtime.DisposableEffect(Unit) {
        onDispose {
            homeViewModel.onCleared()
            chatViewModel.onCleared()
            settingsViewModel.onCleared()
            onViewModelCleared()
        }
    }
    
    App(
        homeViewModel = homeViewModel,
        chatViewModel = chatViewModel,
        settingsViewModel = settingsViewModel,
        themeMode = themeMode
    )
}