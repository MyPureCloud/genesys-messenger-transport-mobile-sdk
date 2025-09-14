package com.genesys.cloud.messenger.composeapp.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.genesys.cloud.messenger.composeapp.model.Screen
import com.genesys.cloud.messenger.composeapp.ui.screens.ChatScreen
import com.genesys.cloud.messenger.composeapp.ui.screens.HomeScreen
import com.genesys.cloud.messenger.composeapp.ui.screens.SettingsScreen
import com.genesys.cloud.messenger.composeapp.viewmodel.ChatViewModel
import com.genesys.cloud.messenger.composeapp.viewmodel.HomeViewModel
import com.genesys.cloud.messenger.composeapp.viewmodel.SettingsViewModel

/**
 * Main navigation composable that sets up the navigation graph for the application.
 * Handles navigation between different screens using Compose Navigation.
 *
 * @param navController The navigation controller to handle navigation actions
 * @param startDestination The initial screen to display when the app starts
 * @param homeViewModel ViewModel for the home screen
 * @param chatViewModel ViewModel for the chat screen
 * @param settingsViewModel ViewModel for the settings screen
 */
@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController(),
    startDestination: String = "home",
    homeViewModel: HomeViewModel = remember { HomeViewModel() },
    chatViewModel: ChatViewModel = remember { ChatViewModel() },
    settingsViewModel: SettingsViewModel = remember { SettingsViewModel() }
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable("home") {
            HomeScreen(
                homeViewModel = homeViewModel,
                onNavigateToChat = {
                    navController.navigate("chat")
                },
                onNavigateToSettings = {
                    navController.navigate("settings")
                }
            )
        }
        
        composable("chat") {
            ChatScreen(
                chatViewModel = chatViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable("settings") {
            SettingsScreen(
                settingsViewModel = settingsViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}

/**
 * Navigation state holder that manages the current screen and navigation actions.
 * Provides a centralized way to handle navigation state across the application.
 */
class NavigationState {
    var currentScreen by mutableStateOf<Screen>(Screen.Home)
        private set
    
    /**
     * Navigate to a specific screen
     */
    fun navigateTo(screen: Screen) {
        currentScreen = screen
    }
    
    /**
     * Navigate back to the previous screen
     */
    fun navigateBack() {
        // For now, always navigate back to Home
        // In a more complex app, this would maintain a back stack
        currentScreen = Screen.Home
    }
}

/**
 * Remember navigation state across recompositions
 */
@Composable
fun rememberNavigationState(): NavigationState {
    return remember { NavigationState() }
}

