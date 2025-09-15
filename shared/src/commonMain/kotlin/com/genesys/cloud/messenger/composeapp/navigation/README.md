# Navigation System

This directory contains the navigation system for the Compose Multiplatform testbed application.

## Components

### Screen.kt (in model package)
Sealed class that defines all available screens in the application:
- `Screen.Home` - Main landing page
- `Screen.Interaction` - TestBed interface for messaging client operations
- `Screen.Settings` - Deployment configuration

### AppNavigation.kt
Main navigation composable that sets up the navigation graph using Compose Navigation.

**Key Features:**
- `AppNavigation` composable - Sets up NavHost with all screen destinations
- `NavigationState` class - Manages current screen state
- `rememberNavigationState()` - Remembers navigation state across recompositions
- Placeholder screen composables (will be replaced in task 11)

### NavigationExtensions.kt
Utility functions for type-safe navigation:
- `Screen.toRoute()` - Convert Screen to navigation route string
- `String.toScreen()` - Convert route string to Screen
- `NavController.navigateTo(screen)` - Navigate using Screen sealed class
- `NavController.navigateToSingleTop(screen)` - Navigate with single top behavior
- `NavController.navigateAndClearBackStack(screen)` - Navigate and clear back stack

## Usage

### Basic Navigation Setup
```kotlin
@Composable
fun MyApp() {
    AppNavigation(
        startDestination = "home"
    )
}
```

### Using Navigation State
```kotlin
@Composable
fun MyScreen() {
    val navigationState = rememberNavigationState()
    
    // Navigate to a screen
    Button(onClick = { navigationState.navigateTo(Screen.Interaction) }) {
        Text("Go to Interaction")
    }
    
    // Check current screen
    when (navigationState.currentScreen) {
        Screen.Home -> Text("Currently on Home")
        Screen.Interaction -> Text("Currently on Interaction")
        Screen.Settings -> Text("Currently on Settings")
    }
}
```

### Using NavController Extensions
```kotlin
@Composable
fun MyScreen(navController: NavController) {
    Button(onClick = { navController.navigateTo(Screen.Settings) }) {
        Text("Go to Settings")
    }
}
```

## Integration with AppState

The navigation system integrates with the `AppState` data class which includes a `currentScreen` property for state management across the application.

## Testing

Navigation functionality is tested in `NavigationTest.kt` which verifies:
- Screen to route conversion
- Route to screen conversion  
- Navigation state management
- Invalid route handling

## Implementation Status

The navigation system has been fully implemented with:
- InteractionScreen for TestBed messaging client operations
- SettingsScreen for deployment configuration
- HomeScreen as the main landing page