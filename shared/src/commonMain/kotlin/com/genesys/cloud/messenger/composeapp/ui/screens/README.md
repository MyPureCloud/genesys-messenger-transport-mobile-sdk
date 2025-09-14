# Screen Composables

This directory contains the main screen composables for the Compose Multiplatform application.

## Implemented Screens

### HomeScreen.kt
- **Purpose**: Main landing page of the application
- **Features**:
  - Welcome section with app description
  - Navigation buttons to Chat and Settings screens
  - App features showcase
  - Error handling with retry functionality
  - Loading states
- **Requirements**: 2.1, 4.1, 4.3

### ChatScreen.kt
- **Purpose**: Messaging interface for user-agent conversations
- **Features**:
  - Message list with auto-scroll to bottom
  - Message input field with send functionality
  - Typing indicators for agent responses
  - Loading and empty states
  - Error handling with retry functionality
  - Real-time message updates
- **Requirements**: 2.1, 4.1, 4.3

### SettingsScreen.kt
- **Purpose**: App preferences and configuration management
- **Features**:
  - Theme selection (Light, Dark, System)
  - Notification preferences toggle
  - Language selection dropdown
  - Reset to defaults functionality
  - Success and error message handling
  - Loading states during operations
- **Requirements**: 2.1, 4.1, 4.3

## Architecture

All screens follow the same architectural pattern:

1. **ViewModel Integration**: Each screen receives its corresponding ViewModel as a parameter
2. **State Management**: Uses `collectAsState()` to observe ViewModel state changes
3. **Navigation**: Receives navigation callbacks as parameters for loose coupling
4. **Error Handling**: Consistent error display and retry mechanisms
5. **Loading States**: Visual feedback during asynchronous operations
6. **Accessibility**: Proper content descriptions and semantic roles

## Usage

The screens are integrated into the navigation system via `AppNavigation.kt` and are automatically instantiated with their corresponding ViewModels.

```kotlin
// Example usage in navigation
composable("home") {
    HomeScreen(
        homeViewModel = homeViewModel,
        onNavigateToChat = { navController.navigate("chat") },
        onNavigateToSettings = { navController.navigate("settings") }
    )
}
```

## Dependencies

- **UI Components**: Uses shared components from `ui/components/`
- **ViewModels**: Integrates with ViewModels from `viewmodel/`
- **Models**: Uses data models from `model/`
- **Theme**: Applies theming from `theme/`