# Platform-Specific Context Handling

This module provides platform-specific context handling for the TestBedViewModel, enabling access to platform-specific functionality like vault setup and asset loading.

## Overview

The platform context system consists of:

1. **PlatformContext** - Common interface for platform-specific operations
2. **PlatformContextProvider** - Platform-specific context provider
3. **PlatformUtils** - Utility functions for accessing platform context
4. **Platform-specific implementations** - Android and iOS implementations

## Usage

### Android Setup

In your Android Application class or MainActivity:

```kotlin
import com.genesys.cloud.messenger.composeapp.util.AndroidInitializer

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Initialize the shared module with Android context
        AndroidInitializer.initialize(this)
    }
}
```

### iOS Setup

In your iOS app, no additional setup is required:

```kotlin
import com.genesys.cloud.messenger.composeapp.util.IosInitializer

// Optional: Call initialize for consistency
IosInitializer.initialize()
```

### Using Platform Context in TestBedViewModel

```kotlin
import com.genesys.cloud.messenger.composeapp.util.PlatformUtils

// Check if platform context is ready
if (PlatformUtils.isPlatformContextReady()) {
    val platformContext = PlatformUtils.requirePlatformContext()
    
    // Initialize TestBedViewModel
    testBedViewModel.init(
        platformContext = platformContext,
        selectFile = { fileProfile -> /* handle file selection */ },
        onOktaSignIn = { url -> /* handle OAuth sign-in */ }
    )
}
```

## Platform-Specific Features

### Vault Context Setup

The platform context handles vault setup for the transport SDK:

```kotlin
// Set up encrypted vault
platformContext.setupVaultContext(useEncryptedVault = true)

// Set up default vault
platformContext.setupVaultContext(useEncryptedVault = false)
```

### Asset Loading

Load saved attachment files from platform storage:

```kotlin
val savedAttachments = platformContext.loadSavedAttachments()
savedAttachments.forEach { attachment ->
    println("Found: ${attachment.name} (${attachment.size} bytes)")
}
```

### Storage Directory

Get the platform-specific storage directory:

```kotlin
val storageDir = platformContext.getStorageDirectory()
// Android: /data/data/com.example.app/files/attachments
// iOS: /Documents/attachments
```

## Implementation Details

### Android Implementation

- Uses Android Context for platform operations
- Storage directory: `context.getExternalFilesDir("attachments")` or `context.filesDir/attachments`
- MIME type detection using `MimeTypeMap`
- Requires Android Context to be set via `AndroidInitializer`

### iOS Implementation

- Uses iOS Foundation APIs for platform operations
- Storage directory: `NSDocumentDirectory/attachments`
- MIME type detection using `UTType`
- No additional setup required

## Error Handling

The platform context system includes proper error handling:

- Android: Throws `IllegalStateException` if context is not set
- iOS: Always available, no special error handling needed
- Common: `PlatformUtils.isPlatformContextReady()` checks availability

## Testing

Basic tests are provided to verify platform context functionality:

- `PlatformContextTest` - Common tests
- `AndroidPlatformContextTest` - Android-specific tests

Run tests with:
```bash
./gradlew shared:testDebugUnitTest
```