# Compose Multiplatform Performance Guide

This guide outlines the performance optimizations implemented in the Compose Multiplatform template and provides best practices for maintaining optimal performance across Android and iOS platforms.

## 🎯 Performance Optimizations Overview

### Memory Management
- **Message History Limiting**: Chat messages are limited to 500 items to prevent memory issues
- **Efficient State Updates**: StateFlow usage minimizes unnecessary recompositions
- **ViewModel Lifecycle**: Proper cleanup prevents memory leaks
- **Resource Management**: Optimized image and asset loading

### UI Performance
- **Stable Keys**: LazyColumn items use stable, unique keys for optimal recycling
- **Content Types**: Different content types improve LazyColumn performance
- **Animation Optimization**: Shared animation states reduce memory usage
- **Smart Scrolling**: Auto-scroll only when user is at bottom of conversation

### Build Performance
- **ProGuard Optimization**: Comprehensive rules for release builds
- **Framework Configuration**: Dynamic iOS frameworks reduce app size
- **Dependency Management**: Minimal, carefully selected dependencies
- **Incremental Compilation**: Enabled for faster build times

## 📱 Platform-Specific Optimizations

### Android Optimizations

#### Build Configuration
```kotlin
android {
    buildFeatures {
        buildConfig = false // Disable BuildConfig generation
        resValues = false   // Disable resource value generation
    }
    
    compileOptions {
        isCoreLibraryDesugaringEnabled = true // Enable incremental compilation
    }
    
    packagingOptions {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/versions/9/previous-compilation-data.bin"
        }
    }
}
```

#### ProGuard Rules
- Remove debug logging in release builds
- Optimize string concatenation
- Keep essential classes while removing unused code
- Preserve crash reporting information

#### Memory Optimizations
- Vector drawable support library usage
- Efficient bitmap handling
- Proper Activity lifecycle management

### iOS Optimizations

#### Framework Configuration
```kotlin
cocoapods {
    framework {
        baseName = "Shared"
        isStatic = false // Dynamic framework reduces duplicate symbols
        transitiveExport = false // Reduce framework size
    }
}
```

#### Memory Management
- Automatic Reference Counting (ARC) compatibility
- Proper Swift/Kotlin interop
- Efficient CocoaPods integration

## 🔧 Code-Level Optimizations

### Compose Performance

#### Efficient State Management
```kotlin
// ✅ Good: Use remember for expensive calculations
@Composable
fun ExpensiveComponent(data: List<Item>) {
    val processedData = remember(data) {
        data.map { processItem(it) }
    }
    // ... rest of composable
}

// ✅ Good: Use derivedStateOf for computed values
@Composable
fun FilteredList(items: List<Item>, query: String) {
    val filteredItems by remember {
        derivedStateOf {
            items.filter { it.name.contains(query, ignoreCase = true) }
        }
    }
    // ... rest of composable
}
```

#### LazyColumn Optimizations
```kotlin
LazyColumn {
    items(
        items = messages,
        key = { message -> message.id }, // Stable keys
        contentType = { "message" }      // Content types for recycling
    ) { message ->
        MessageBubble(
            message = message,
            modifier = Modifier.animateItemPlacement() // Smooth animations
        )
    }
}
```

#### Animation Performance
```kotlin
// ✅ Good: Use rememberInfiniteTransition for shared animations
@Composable
fun TypingIndicator() {
    val infiniteTransition = rememberInfiniteTransition(label = "typing")
    
    repeat(3) { index ->
        val alpha by infiniteTransition.animateFloat(
            initialValue = 0.3f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(600, delayMillis = index * 200),
                repeatMode = RepeatMode.Reverse
            ),
            label = "dot_$index"
        )
        // ... use alpha
    }
}
```

### ViewModel Performance

#### Efficient State Updates
```kotlin
class ChatViewModel : BaseViewModel() {
    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()
    
    // ✅ Good: Limit message history for memory optimization
    private fun addMessage(message: ChatMessage) {
        val currentMessages = _uiState.value.messages.toMutableList()
        currentMessages.add(message)
        
        val optimizedMessages = if (currentMessages.size > MAX_MESSAGE_HISTORY) {
            currentMessages.takeLast(MAX_MESSAGE_HISTORY)
        } else {
            currentMessages
        }
        
        _uiState.value = _uiState.value.copy(messages = optimizedMessages)
    }
    
    companion object {
        private const val MAX_MESSAGE_HISTORY = 500
    }
}
```

#### Coroutine Optimization
```kotlin
// ✅ Good: Use appropriate dispatchers
scope.launch(Dispatchers.IO) {
    // Network or file operations
    val result = networkCall()
    
    withContext(Dispatchers.Main) {
        // UI updates
        updateUI(result)
    }
}
```

## 📊 Performance Monitoring

### Key Metrics to Monitor

#### Android
- **Frame Rate**: Target 60 FPS, monitor with GPU Profiler
- **Memory Usage**: Monitor heap usage and GC frequency
- **App Startup Time**: Cold start should be under 2 seconds
- **APK Size**: Monitor size increases with each release

#### iOS
- **Frame Rate**: Target 60 FPS, monitor with Instruments
- **Memory Usage**: Monitor memory footprint and leaks
- **App Launch Time**: Cold launch should be under 2 seconds
- **Framework Size**: Monitor shared framework size

### Profiling Tools

#### Android
```bash
# Memory profiling
adb shell dumpsys meminfo com.your.package

# CPU profiling
adb shell top -p $(adb shell pidof com.your.package)

# GPU profiling
adb shell setprop debug.hwui.profile visual_bars
```

#### iOS
- Use Xcode Instruments for memory and CPU profiling
- Monitor framework loading times
- Check for memory leaks in shared code

## 🚀 Performance Best Practices

### Do's ✅

1. **Use Stable Keys**: Always provide stable, unique keys for LazyColumn items
2. **Limit State**: Keep state minimal and focused
3. **Remember Expensive Operations**: Use `remember` for costly calculations
4. **Optimize Images**: Use appropriate image formats and sizes
5. **Profile Regularly**: Monitor performance metrics continuously
6. **Test on Real Devices**: Emulators don't reflect real performance

### Don'ts ❌

1. **Don't Create Objects in Composables**: Avoid object creation during composition
2. **Don't Use Unstable Keys**: Avoid using indices or unstable values as keys
3. **Don't Ignore Memory Leaks**: Always clean up resources properly
4. **Don't Over-Animate**: Excessive animations can hurt performance
5. **Don't Skip Testing**: Performance issues often appear only under load

### Code Examples

#### ❌ Bad: Unstable keys and object creation
```kotlin
LazyColumn {
    items(messages.size) { index ->
        MessageBubble(
            message = messages[index],
            timestamp = Date() // Creates new object every recomposition
        )
    }
}
```

#### ✅ Good: Stable keys and remembered objects
```kotlin
LazyColumn {
    items(
        items = messages,
        key = { it.id }
    ) { message ->
        val formattedTime = remember(message.timestamp) {
            formatTimestamp(message.timestamp)
        }
        MessageBubble(
            message = message,
            formattedTime = formattedTime
        )
    }
}
```

## 🔍 Debugging Performance Issues

### Common Issues and Solutions

#### Excessive Recomposition
- **Problem**: UI updates too frequently
- **Solution**: Use `remember` and `derivedStateOf` appropriately
- **Debug**: Enable composition tracing in debug builds

#### Memory Leaks
- **Problem**: ViewModels or resources not cleaned up
- **Solution**: Implement proper lifecycle management
- **Debug**: Use memory profilers to identify leaks

#### Slow List Scrolling
- **Problem**: LazyColumn performance issues
- **Solution**: Use stable keys and content types
- **Debug**: Monitor frame rate during scrolling

#### Large App Size
- **Problem**: APK/IPA size too large
- **Solution**: Optimize ProGuard rules and dependencies
- **Debug**: Analyze APK/IPA contents

## 📈 Performance Benchmarks

### Target Performance Metrics

| Platform | Metric | Target | Measurement |
|----------|--------|--------|-------------|
| Android | Cold Start | < 2s | Time to first frame |
| Android | Frame Rate | 60 FPS | During scrolling |
| Android | Memory | < 100MB | Steady state |
| Android | APK Size | < 50MB | Release build |
| iOS | Cold Launch | < 2s | Time to first frame |
| iOS | Frame Rate | 60 FPS | During scrolling |
| iOS | Memory | < 80MB | Steady state |
| iOS | Framework | < 20MB | Shared framework |

### Continuous Monitoring

Set up automated performance testing:
- Monitor key metrics in CI/CD pipeline
- Alert on performance regressions
- Track performance trends over time
- Test on various device configurations

## 🛠️ Tools and Resources

### Development Tools
- **Android Studio Profiler**: Memory, CPU, and network profiling
- **Xcode Instruments**: iOS performance analysis
- **Compose Layout Inspector**: UI hierarchy analysis
- **LeakCanary**: Android memory leak detection

### Monitoring Services
- **Firebase Performance**: Real-time performance monitoring
- **Crashlytics**: Crash reporting and analysis
- **Custom Analytics**: Track app-specific metrics

### Documentation
- [Compose Performance](https://developer.android.com/jetpack/compose/performance)
- [iOS Performance Best Practices](https://developer.apple.com/documentation/xcode/improving_your_app_s_performance)
- [Kotlin Multiplatform Performance](https://kotlinlang.org/docs/multiplatform-mobile-performance.html)

This performance guide should be regularly updated as new optimizations are discovered and implemented.