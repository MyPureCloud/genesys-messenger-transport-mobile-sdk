package com.genesys.cloud.messenger.composeapp.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Primary brand colors
val GenesysPrimary = Color(0xFF0066CC)
val GenesysPrimaryVariant = Color(0xFF004499)
val GenesysSecondary = Color(0xFF00A86B)
val GenesysSecondaryVariant = Color(0xFF007A4D)

// Surface and background colors
val LightSurface = Color(0xFFFFFBFE)
val LightBackground = Color(0xFFFFFBFE)
val LightSurfaceVariant = Color(0xFFE7E0EC)

val DarkSurface = Color(0xFF1C1B1F)
val DarkBackground = Color(0xFF1C1B1F)
val DarkSurfaceVariant = Color(0xFF49454F)

// Text colors
val LightOnSurface = Color(0xFF1C1B1F)
val LightOnBackground = Color(0xFF1C1B1F)
val LightOnPrimary = Color(0xFFFFFFFF)
val LightOnSecondary = Color(0xFFFFFFFF)

val DarkOnSurface = Color(0xFFE6E1E5)
val DarkOnBackground = Color(0xFFE6E1E5)
val DarkOnPrimary = Color(0xFFFFFFFF)
val DarkOnSecondary = Color(0xFFFFFFFF)

// Message bubble colors
val UserMessageBackground = GenesysPrimary
val BotMessageBackground = Color(0xFFF5F5F5)
val UserMessageBackgroundDark = GenesysPrimaryVariant
val BotMessageBackgroundDark = Color(0xFF2D2D2D)

// Status colors
val SuccessColor = Color(0xFF4CAF50)
val ErrorColor = Color(0xFFF44336)
val WarningColor = Color(0xFFFF9800)
val InfoColor = Color(0xFF2196F3)

val LightColorScheme = lightColorScheme(
    primary = GenesysPrimary,
    onPrimary = LightOnPrimary,
    primaryContainer = GenesysPrimaryVariant,
    onPrimaryContainer = Color(0xFFFFFFFF),
    secondary = GenesysSecondary,
    onSecondary = LightOnSecondary,
    secondaryContainer = GenesysSecondaryVariant,
    onSecondaryContainer = Color(0xFFFFFFFF),
    tertiary = InfoColor,
    onTertiary = Color(0xFFFFFFFF),
    error = ErrorColor,
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = Color(0xFF49454F),
    outline = Color(0xFF79747E),
    outlineVariant = Color(0xFFCAC4D0),
    scrim = Color(0xFF000000),
    inverseSurface = Color(0xFF313033),
    inverseOnSurface = Color(0xFFF4EFF4),
    inversePrimary = Color(0xFFB8C5FF)
)

val DarkColorScheme = darkColorScheme(
    primary = GenesysPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = GenesysPrimaryVariant,
    onPrimaryContainer = Color(0xFFB8C5FF),
    secondary = GenesysSecondary,
    onSecondary = DarkOnSecondary,
    secondaryContainer = GenesysSecondaryVariant,
    onSecondaryContainer = Color(0xFF9EFFC7),
    tertiary = InfoColor,
    onTertiary = Color(0xFFFFFFFF),
    error = ErrorColor,
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = Color(0xFFCAC4D0),
    outline = Color(0xFF938F99),
    outlineVariant = Color(0xFF49454F),
    scrim = Color(0xFF000000),
    inverseSurface = Color(0xFFE6E1E5),
    inverseOnSurface = Color(0xFF313033),
    inversePrimary = GenesysPrimary
)