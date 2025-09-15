# Screen Composables

This directory contains the main screen composables for the Compose Multiplatform testbed application.

## Implemented Screens

### HomeScreen.kt
- **Purpose**: Main landing page of the testbed application
- **Features**:
  - Welcome section with testbed description
  - Navigation buttons to Interaction and Settings screens
  - TestBed features showcase
  - Error handling with retry functionality
  - Loading states
- **Requirements**: 2.1, 4.1, 4.3

### InteractionScreen.kt
- **Purpose**: TestBed interface for messaging client operations and socket message monitoring
- **Features**:
  - Command dropdown with all available messaging client commands
  - Socket message display with expandable details
  - Command execution with loading states
  - Client state indicators
  - Real-time socket message updates
  - Command input with parameter support
- **Requirements**: 2.1, 2.2, 2.3, 2.4, 2.5, 2.6, 4.1, 4.2, 4.3, 4.4, 5.1, 5.2, 5.3, 5.4, 5.5

### SettingsScreen.kt
- **Purpose**: Simplified deployment configuration management
- **Features**:
  - Deployment ID input field
  - Region selection dropdown
  - Default value loading from BuildConfig
  - Validation and error handling
  - Save/load functionality
- **Requirements**: 1.1, 1.2, 1.3, 1.4, 6.1, 6.2, 6.3

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
        onNavigateToInteraction = { navController.navigate("interaction") },
        onNavigateToSettings = { navController.navigate("settings") }
    )
}
```

## Dependencies

- **UI Components**: Uses shared components from `ui/components/`
- **ViewModels**: Integrates with ViewModels from `viewmodel/`
- **Models**: Uses data models from `model/`
- **Theme**: Applies theming from `theme/`