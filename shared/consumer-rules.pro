# Consumer ProGuard rules for Shared module
# These rules will be applied to any app that consumes this library

# Keep all public API classes and methods
-keep public class com.genesys.cloud.messenger.composeapp.** { 
    public *; 
}

# Keep ViewModels
-keep class com.genesys.cloud.messenger.composeapp.viewmodel.** { *; }

# Keep data models
-keep class com.genesys.cloud.messenger.composeapp.model.** { *; }

# Keep platform interfaces
-keep class com.genesys.cloud.messenger.composeapp.platform.** { *; }

# Keep Compose UI components
-keep class com.genesys.cloud.messenger.composeapp.ui.** { *; }

# Keep theme and styling
-keep class com.genesys.cloud.messenger.composeapp.theme.** { *; }

# Keep navigation components
-keep class com.genesys.cloud.messenger.composeapp.navigation.** { *; }

# Keep validation utilities
-keep class com.genesys.cloud.messenger.composeapp.validation.** { *; }