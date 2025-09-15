# Compose Multiplatform TestBed - Shared Module

This module contains the shared code for a Compose Multiplatform testbed application, providing a developer-focused interface for testing and monitoring messaging client operations across Android and iOS platforms.

## 🏗️ Architecture

### Overview
The shared module follows a clean architecture pattern with clear separation of concerns:

```
shared/
├── src/
│   ├── commonMain/kotlin/          # Shared code for all platforms
│   │   ├── ui/                     # UI components and screens
│   │   │   ├── components/         # Reusable UI components
│   │   │   ├── screens/            # Screen composables
│   │   │   └── theme/              # Theme and styling
│   │   ├── viewmodel/              # Business logic and state management
│   │   ├── model/                  # Data models and types
│   │   ├── navigation/             # Navigation setup
│   │   ├── platform/               # Platform abstractions
│   │   └── validation/             # Input validation utilities
│   ├── androidMain/kotlin/         # Android-specific implementations
│   ├── iosMain/kotlin/             # iOS-specific implementations
│   ├── commonTest/kotlin/          # Shared unit tests
│   ├── androidUnitTest/kotlin/     # Android-specific tests
│   └── iosTest/kotlin/             # iOS-specific tests
```

### Key Components

#### 1. UI Layer (`ui/`)
- **Screens**: Home, Interaction (TestBed), and Settings screens
- **Components**: Reusable components like InputField, TopBar, SocketMessageList
- **Theme**: Material Design 3 implementation optimized for developer tools

#### 2. ViewModel Layer (`viewmodel/`)
- **BaseViewModel**: Common functionality for all ViewModels
- **TestBedViewModel**: Manages messaging client operations, commands, and socket messages
- **HomeViewModel**: Handles navigation and home screen state
- **SettingsViewModel**: Manages deployment configuration (deploymentId, region)

#### 3. Model Layer (`model/`)
- **Data Models**: SocketMessage, Command, AuthState, TestBedError
- **Transport Types**: Placeholder implementations for messaging client integration
- **Error Types**: Comprehensive error handling for testbed operations

#### 4. Platform Layer (`platform/`)
- **Platform Interface**: Expect/actual declarations for platform-specific functionality
- **Context Abstraction**: Platform-agnostic context handling

## 🚀 Features

### Core Features
- ✅ Cross-platform TestBed UI with Compose Multiplatform
- ✅ Command execution interface for messaging client operations
- ✅ Socket message monitoring with expandable details
- ✅ Real-time message type display and filtering
- ✅ Deployment configuration management (deploymentId, region)
- ✅ Authentication flow testing (OAuth, OKTA)
- ✅ File attachment testing capabilities
- ✅ Push notification testing
- ✅ Error handling and command validation
- ✅ Developer-focused UI optimizations

### Platform Support
- **Android**: Full native Android support with TestBed functionality
- **iOS**: Native iOS support with shared TestBed operations
- **Shared Business Logic**: 100% code sharing for TestBed operations and messaging client integration

## 🎯 Performance Optimizations

### Memory Management
- **Message History Limiting**: Prevents memory issues with large conversations
- **Efficient State Updates**: Uses StateFlow for optimal recomposition
- **Stable Keys**: LazyColumn items use stable keys for better performance

### UI Performance
- **Optimized Animations**: Efficient typing indicators and transitions
- **Smart Scrolling**: Auto-scroll only when user is near bottom
- **Content Types**: LazyColumn content types for better recycling

### Build Optimizations
- **ProGuard Rules**: Optimized for release builds
- **Framework Configuration**: Dynamic frameworks for iOS to reduce size
- **Dependency Management**: Minimal dependencies with careful selection

## 🛠️ Usage

### Basic Integration

```kotlin
// In your platform-specific app
@Composable
fun MyApp() {
    App(themeMode = ThemeMode.System)
}
```

### With Custom ViewModels

```kotlin
// For dependency injection or custom ViewModel creation
@Composable
fun MyAppWithDI(
    homeViewModel: HomeViewModel,
    chatViewModel: ChatViewModel,
    settingsViewModel: SettingsViewModel
) {
    App(
        homeViewModel = homeViewModel,
        chatViewModel = chatViewModel,
        settingsViewModel = settingsViewModel,
        themeMode = ThemeMode.Dark
    )
}
```

### With Lifecycle Management

```kotlin
// For proper ViewModel cleanup
@Composable
fun MyAppWithLifecycle() {
    AppWithLifecycle(
        themeMode = ThemeMode.System,
        onViewModelCleared = {
            // Handle cleanup
        }
    )
}
```

## 🧪 Testing

### Unit Tests
The module includes comprehensive unit tests for:
- ViewModels and business logic
- Input validation
- Error handling
- State management

### Running Tests
```bash
# Run all tests
./gradlew shared:test

# Run Android-specific tests
./gradlew shared:testDebugUnitTest

# Run iOS tests
./gradlew shared:iosTest
```

## 📱 Platform-Specific Integration

### Android Integration
```kotlin
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            App()
        }
    }
}
```

### iOS Integration
```swift
struct ContentView: View {
    var body: some View {
        ComposeView()
    }
}

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        return AppKt.createComposeViewController()
    }
    
    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}
```

## 🎨 Theming

The module supports comprehensive theming with:
- **Light/Dark Mode**: Automatic system theme detection
- **Material Design 3**: Full MD3 color system and typography
- **Custom Colors**: Easy customization of brand colors
- **Platform Consistency**: Consistent appearance across platforms

### Theme Configuration
```kotlin
// Custom theme colors
val customColors = lightColorScheme(
    primary = Color(0xFF6750A4),
    secondary = Color(0xFF625B71),
    // ... other colors
)

AppTheme(
    colorScheme = customColors,
    themeMode = ThemeMode.Light
) {
    // Your app content
}
```

## 🔧 Configuration

### Build Configuration
The module is configured for optimal performance:
- **Kotlin Multiplatform**: Latest stable version
- **Compose Multiplatform**: Optimized for cross-platform UI
- **CocoaPods**: iOS integration with dynamic frameworks
- **ProGuard**: Release build optimizations

### Dependencies
Core dependencies include:
- Compose Multiplatform (UI framework)
- Kotlin Coroutines (Async operations)
- Navigation Compose (Screen navigation)
- Material Design 3 (UI components)

## 📚 Best Practices Demonstrated

### Architecture Patterns
- **MVVM**: Clear separation between UI and business logic
- **Unidirectional Data Flow**: Predictable state management
- **Repository Pattern**: Data access abstraction
- **Dependency Injection**: Testable and maintainable code

### Performance Patterns
- **State Hoisting**: Efficient state management
- **Stable Keys**: Optimized list performance
- **Memory Management**: Preventing memory leaks
- **Lazy Loading**: Efficient resource usage

### Error Handling
- **Typed Errors**: Type-safe error handling
- **User-Friendly Messages**: Clear error communication
- **Retry Mechanisms**: Graceful failure recovery
- **Validation**: Input validation with feedback

## 🤝 Contributing

When contributing to this template:
1. Follow the established architecture patterns
2. Add comprehensive tests for new features
3. Update documentation for API changes
4. Ensure cross-platform compatibility
5. Optimize for performance

## 📄 License

This template is part of the Genesys Cloud Messenger Mobile SDK and follows the same licensing terms.

## 🔗 Related Documentation

- [Integration Tests Documentation](../../docs/INTEGRATION_TESTS.md)
- [Transport Module Documentation](../../transport/README.md)
- [Android App Documentation](../../composeApp/README.md)
- [iOS App Documentation](../../iosComposeApp/README.md)