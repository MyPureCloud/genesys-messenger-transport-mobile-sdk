package com.genesys.cloud.messenger.composeapp.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import com.genesys.cloud.messenger.composeapp.model.ThemeMode

/**
 * Extended color palette for custom colors not covered by Material Design 3
 */
data class ExtendedColors(
    val userMessageBackground: androidx.compose.ui.graphics.Color,
    val botMessageBackground: androidx.compose.ui.graphics.Color,
    val successColor: androidx.compose.ui.graphics.Color,
    val errorColor: androidx.compose.ui.graphics.Color,
    val warningColor: androidx.compose.ui.graphics.Color,
    val infoColor: androidx.compose.ui.graphics.Color
)

/**
 * CompositionLocal for accessing extended colors
 */
val LocalExtendedColors = staticCompositionLocalOf {
    ExtendedColors(
        userMessageBackground = UserMessageBackground,
        botMessageBackground = BotMessageBackground,
        successColor = SuccessColor,
        errorColor = ErrorColor,
        warningColor = WarningColor,
        infoColor = InfoColor
    )
}

/**
 * Main theme composable that provides Material Design 3 theming
 * with custom color extensions and typography
 */
@Composable
fun AppTheme(
    themeMode: ThemeMode = ThemeMode.System,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
        ThemeMode.System -> isSystemInDarkTheme()
    }

    val colorScheme = if (darkTheme) {
        DarkColorScheme
    } else {
        LightColorScheme
    }

    val extendedColors = if (darkTheme) {
        ExtendedColors(
            userMessageBackground = UserMessageBackgroundDark,
            botMessageBackground = BotMessageBackgroundDark,
            successColor = SuccessColor,
            errorColor = ErrorColor,
            warningColor = WarningColor,
            infoColor = InfoColor
        )
    } else {
        ExtendedColors(
            userMessageBackground = UserMessageBackground,
            botMessageBackground = BotMessageBackground,
            successColor = SuccessColor,
            errorColor = ErrorColor,
            warningColor = WarningColor,
            infoColor = InfoColor
        )
    }

    CompositionLocalProvider(
        LocalExtendedColors provides extendedColors
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            content = content
        )
    }
}

/**
 * Extension property to access extended colors from MaterialTheme
 */
val MaterialTheme.extendedColors: ExtendedColors
    @Composable
    get() = LocalExtendedColors.current

/**
 * Utility composable for previewing light theme
 */
@Composable
fun AppThemeLight(content: @Composable () -> Unit) {
    AppTheme(themeMode = ThemeMode.Light, content = content)
}

/**
 * Utility composable for previewing dark theme
 */
@Composable
fun AppThemeDark(content: @Composable () -> Unit) {
    AppTheme(themeMode = ThemeMode.Dark, content = content)
}