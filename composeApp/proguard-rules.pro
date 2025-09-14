# Compose Multiplatform Template - Optimized ProGuard Rules
# These rules are optimized for performance and compatibility across platforms

# Preserve line number information for debugging stack traces
-keepattributes SourceFile,LineNumberTable

# Keep annotation attributes for runtime reflection
-keepattributes RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations

# Compose Multiplatform optimizations
-keep class androidx.compose.** { *; }
-keep class androidx.compose.runtime.** { *; }
-keep class androidx.compose.ui.** { *; }
-keep class androidx.compose.foundation.** { *; }
-keep class androidx.compose.material3.** { *; }
-keep class androidx.compose.animation.** { *; }

# Kotlin and Coroutines optimizations
-keep class kotlin.** { *; }
-keep class kotlinx.coroutines.** { *; }
-keep class kotlinx.serialization.** { *; }

# Navigation component
-keep class androidx.navigation.** { *; }

# Keep ViewModels and their state
-keep class com.genesys.cloud.messenger.composeapp.viewmodel.** { *; }

# Keep data models for serialization
-keep class com.genesys.cloud.messenger.composeapp.model.** { *; }

# Keep platform-specific implementations
-keep class com.genesys.cloud.messenger.composeapp.platform.** { *; }

# Optimize enums
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Keep Parcelable implementations
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# Remove logging in release builds for better performance
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int i(...);
    public static int w(...);
    public static int d(...);
    public static int e(...);
}

# Optimize string concatenation
-optimizations !code/simplification/string

# Keep crash reporting information
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes Exceptions

# Performance optimizations
-optimizationpasses 5
-allowaccessmodification
-dontpreverify

# Remove unused code
-dontwarn javax.annotation.**
-dontwarn org.jetbrains.annotations.**

# Transport module compatibility
-keep class com.genesys.cloud.messenger.transport.** { *; }