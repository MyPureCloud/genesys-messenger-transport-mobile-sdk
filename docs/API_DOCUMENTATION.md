# Compose Multiplatform Template - API Documentation

This document provides comprehensive API documentation for the Compose Multiplatform template, including all public interfaces, components, and utilities.

## 📋 Table of Contents

- [Core Components](#core-components)
- [ViewModels](#viewmodels)
- [UI Components](#ui-components)
- [Data Models](#data-models)
- [Validation](#validation)
- [Error Handling](#error-handling)
- [Platform Interfaces](#platform-interfaces)
- [Navigation](#navigation)
- [Theming](#theming)

## 🏗️ Core Components

### App Composable

The main entry point for the application.

```kotlin
@Composable
fun App(themeMode: ThemeMode = ThemeMode.System)
```

**Parameters:**
- `themeMode`: The theme mode to apply (Light, Dark, or System)

**Features:**
- Cross-platform UI with Compose Multiplatform
- Material Design 3 theming
- Navigation between screens
- ViewModel integration
- Error handling

### App with ViewModels

Version that accepts external ViewModels for dependency injection.

```kotlin
@Composable
fun App(
    homeViewModel: HomeViewModel,
    chatViewModel: ChatViewModel,
    settingsViewModel: SettingsViewModel,
    themeMode: ThemeMode = ThemeMode.System
)
```

### App with Lifecycle

Version with proper ViewModel lifecycle management.

```kotlin
@Composable
fun AppWithLifecycle(
    themeMode: ThemeMode = ThemeMode.System,
    onViewModelCleared: () -> Unit = {}
)
```

## 🎯 ViewModels

### BaseViewModel

Abstract base class for all ViewModels providing common functionality.

```kotlin
abstract class BaseViewModel {
    val isLoading: StateFlow<Boolean>
    val error: StateFlow<AppError?>
    
    protected fun clearError()
    protected suspend fun <T> safeExecute(block: suspend () -> T): Result<T>
    protected suspend fun safeExecuteUnit(showLoading: Boolean = true, block: suspend () -> Unit)
    open fun onCleared()
}
```

**Features:**
- Loading state management
- Error handling
- Coroutine scope management
- Safe execution wrappers

### ChatViewModel

Manages chat functionality and message state.

```kotlin
class ChatViewModel : BaseViewModel() {
    val uiState: StateFlow<ChatUiState>
    val messageValidation: StateFlow<FieldValidationState>
    
    fun updateCurrentMessage(message: String)
    fun validateCurrentMessage(): Boolean
    fun sendMessage()
    fun retrySendMessage()
    fun clearCurrentMessage()
    fun reloadConversation()
    fun clearMessages()
}
```

**State:**
```kotlin
data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val isAgentTyping: Boolean = false
)
```

**Features:**
- Real-time message validation
- Message history management
- Typing indicators
- Error handling and retry
- Memory optimization (500 message limit)

### HomeViewModel

Handles navigation and home screen state.

```kotlin
class HomeViewModel : BaseViewModel() {
    val navigationEvent: StateFlow<NavigationEvent?>
    
    fun navigateToChat()
    fun navigateToSettings()
    fun clearNavigationEvent()
}
```

**Navigation Events:**
```kotlin
sealed class NavigationEvent {
    data class NavigateToScreen(val screen: Screen) : NavigationEvent()
}
```

### SettingsViewModel

Manages app preferences and configuration.

```kotlin
class SettingsViewModel : BaseViewModel() {
    val uiState: StateFlow<SettingsUiState>
    val settings: StateFlow<AppSettings>
    
    fun updateThemeMode(themeMode: ThemeMode)
    fun toggleNotifications()
    fun updateLanguage(language: String)
    fun resetToDefaults()
    fun reloadSettings()
    fun getAvailableThemeModes(): List<ThemeMode>
    fun getAvailableLanguages(): List<LanguageOption>
}
```

**State:**
```kotlin
data class SettingsUiState(
    val successMessage: String? = null
)

data class LanguageOption(
    val code: String,
    val displayName: String
)
```

## 🎨 UI Components

### Screen Components

#### HomeScreen
```kotlin
@Composable
fun HomeScreen(
    homeViewModel: HomeViewModel,
    onNavigateToChat: () -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
)
```

#### ChatScreen
```kotlin
@Composable
fun ChatScreen(
    chatViewModel: ChatViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
)
```

#### SettingsScreen
```kotlin
@Composable
fun SettingsScreen(
    settingsViewModel: SettingsViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
)
```

### Reusable Components

#### MessageBubble
```kotlin
@Composable
fun MessageBubble(
    message: ChatMessage,
    modifier: Modifier = Modifier
)
```

**Features:**
- User/agent message differentiation
- Timestamp display
- Material Design 3 styling
- Accessibility support

#### InputField
```kotlin
@Composable
fun InputField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    onSendMessage: () -> Unit,
    enabled: Boolean = true,
    placeholder: String = "",
    isError: Boolean = false,
    modifier: Modifier = Modifier
)
```

**Features:**
- Send button integration
- Error state styling
- Keyboard action handling
- Accessibility support

#### TopBar Components
```kotlin
@Composable
fun SimpleTopBar(
    title: String,
    showBackButton: Boolean = true,
    onBackClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
)

@Composable
fun ChatTopBar(
    title: String,
    subtitle: String? = null,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
)
```

### Error Components

#### ErrorDisplay
```kotlin
@Composable
fun ErrorDisplay(
    error: AppError,
    onDismiss: (() -> Unit)? = null,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    showIcon: Boolean = true,
    dismissible: Boolean = true
)
```

#### ErrorSnackbar
```kotlin
@Composable
fun ErrorSnackbar(
    error: AppError,
    onDismiss: () -> Unit,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    autoDismissDelay: Long = 5000L
)
```

#### InlineErrorDisplay
```kotlin
@Composable
fun InlineErrorDisplay(
    error: AppError?,
    modifier: Modifier = Modifier
)
```

#### FullScreenErrorDisplay
```kotlin
@Composable
fun FullScreenErrorDisplay(
    error: AppError,
    onRetry: (() -> Unit)? = null,
    onDismiss: (() -> Unit)? = null,
    modifier: Modifier = Modifier
)
```

## 📊 Data Models

### ChatMessage
```kotlin
data class ChatMessage(
    val id: String,
    val content: String,
    val timestamp: Long,
    val isFromUser: Boolean
)
```

### AppSettings
```kotlin
data class AppSettings(
    val theme: ThemeMode = ThemeMode.System,
    val notifications: Boolean = true,
    val language: String = "en"
)
```

### ThemeMode
```kotlin
enum class ThemeMode {
    Light, Dark, System
}
```

### Screen Navigation
```kotlin
sealed class Screen {
    object Home : Screen()
    object Chat : Screen()
    object Settings : Screen()
}
```

## ✅ Validation

### InputValidator

Comprehensive input validation utilities.

```kotlin
object InputValidator {
    fun validateChatMessage(message: String): Result<String>
    fun validateLanguageCode(languageCode: String, availableCodes: List<String>): Result<String>
    fun validateDisplayName(name: String): Result<String>
    fun validateEmail(email: String): Result<String>
    fun validatePhoneNumber(phoneNumber: String): Result<String>
    fun validateTextField(
        value: String,
        fieldName: String,
        minLength: Int = 0,
        maxLength: Int = Int.MAX_VALUE,
        allowEmpty: Boolean = false,
        customValidator: ((String) -> Boolean)? = null
    ): Result<String>
}
```

### FieldValidationState
```kotlin
data class FieldValidationState(
    val value: String = "",
    val error: AppError? = null,
    val isValid: Boolean = error == null
) {
    fun withValue(newValue: String): FieldValidationState
    fun withError(newError: AppError): FieldValidationState
    fun withValidation(validation: Result<String>): FieldValidationState
}
```

### Validation Extensions
```kotlin
fun validateFields(vararg validations: () -> Result<*>): List<AppError>
```

## ❌ Error Handling

### AppError Hierarchy

```kotlin
sealed class AppError(open val message: String, open val cause: Throwable? = null)
```

#### NetworkError
```kotlin
sealed class NetworkError : AppError {
    data class ConnectionError(...)
    data class TimeoutError(...)
    data class ServerError(val statusCode: Int, ...)
    data class UnauthorizedError(...)
}
```

#### ValidationError
```kotlin
sealed class ValidationError : AppError {
    data class EmptyFieldError(val fieldName: String, ...)
    data class InvalidFormatError(val fieldName: String, ...)
    data class TooLongError(val fieldName: String, val maxLength: Int, ...)
    data class TooShortError(val fieldName: String, val minLength: Int, ...)
    data class InvalidCharactersError(val fieldName: String, ...)
}
```

#### PlatformError
```kotlin
sealed class PlatformError : AppError {
    data class StorageError(...)
    data class PermissionError(val permission: String, ...)
    data class NotificationError(...)
}
```

#### BusinessError
```kotlin
sealed class BusinessError : AppError {
    data class MessageSendError(...)
    data class ConversationLoadError(...)
    data class SettingsSaveError(...)
    data class AuthenticationError(...)
}
```

### Result Wrapper
```kotlin
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val error: AppError) : Result<Nothing>()
    
    inline fun <R> map(transform: (T) -> R): Result<R>
    inline fun onSuccess(action: (T) -> Unit): Result<T>
    inline fun onError(action: (AppError) -> Unit): Result<T>
}
```

### Error Extensions
```kotlin
fun Throwable.toAppError(): AppError
```

## 🧭 Navigation

### AppNavigation
```kotlin
@Composable
fun AppNavigation(
    navController: NavHostController,
    startDestination: String,
    homeViewModel: HomeViewModel,
    chatViewModel: ChatViewModel,
    settingsViewModel: SettingsViewModel,
    modifier: Modifier = Modifier
)
```

**Routes:**
- `"home"` - Home screen
- `"chat"` - Chat screen
- `"settings"` - Settings screen

## 🎨 Theming

### AppTheme
```kotlin
@Composable
fun AppTheme(
    themeMode: ThemeMode = ThemeMode.System,
    colorScheme: ColorScheme? = null,
    content: @Composable () -> Unit
)
```

### Color System
```kotlin
object AppColors {
    val LightColorScheme: ColorScheme
    val DarkColorScheme: ColorScheme
}
```

### Typography
```kotlin
object AppTypography {
    val Typography: Typography
}
```

## 🔧 Platform Interfaces

### Platform
```kotlin
expect object Platform {
    val name: String
    val version: String
}
```

### PlatformContext
```kotlin
expect class PlatformContext

expect fun getPlatformContext(): PlatformContext
```

## 📱 Usage Examples

### Basic App Setup
```kotlin
@Composable
fun MyApp() {
    App(themeMode = ThemeMode.System)
}
```

### Custom ViewModel Integration
```kotlin
@Composable
fun MyAppWithDI() {
    val homeViewModel = koinInject<HomeViewModel>()
    val chatViewModel = koinInject<ChatViewModel>()
    val settingsViewModel = koinInject<SettingsViewModel>()
    
    App(
        homeViewModel = homeViewModel,
        chatViewModel = chatViewModel,
        settingsViewModel = settingsViewModel
    )
}
```

### Error Handling
```kotlin
@Composable
fun MyScreen(viewModel: MyViewModel) {
    val error by viewModel.error.collectAsState()
    
    Box {
        // Main content
        MainContent()
        
        // Error display
        error?.let { err ->
            ErrorSnackbar(
                error = err,
                onDismiss = { viewModel.clearError() },
                onRetry = { viewModel.retryLastOperation() }
            )
        }
    }
}
```

### Validation Usage
```kotlin
fun handleUserInput(input: String) {
    val result = InputValidator.validateChatMessage(input)
    when (result) {
        is Result.Success -> sendMessage(result.data)
        is Result.Error -> showError(result.error)
    }
}
```

## 🔍 Testing

### ViewModel Testing
```kotlin
@Test
fun `test chat message validation`() {
    val viewModel = ChatViewModel()
    viewModel.updateCurrentMessage("Hello")
    
    val isValid = viewModel.validateCurrentMessage()
    assertTrue(isValid)
}
```

### UI Testing
```kotlin
@Test
fun `test error display`() {
    composeTestRule.setContent {
        ErrorDisplay(
            error = AppError.NetworkError.ConnectionError(),
            onDismiss = {},
            onRetry = {}
        )
    }
    
    composeTestRule.onNodeWithText("Connection Error").assertIsDisplayed()
}
```

This API documentation provides a comprehensive reference for all public interfaces and components in the Compose Multiplatform template.