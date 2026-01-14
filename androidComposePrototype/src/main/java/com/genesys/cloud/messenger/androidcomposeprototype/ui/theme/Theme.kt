package com.genesys.cloud.messenger.androidcomposeprototype.ui.theme

import androidx.compose.material.MaterialTheme
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable

private val LightColorPalette =
    lightColors(
        primary = GenesysDarkBlue,
        onPrimary = GenesysOffWhite,
        primaryVariant = GenesysBlue,
        secondary = GenesysGrey1,
        secondaryVariant = GenesysGrey2,
        onSecondary = GenesysOffWhite,
        background = GenesysOffWhite,
        onBackground = GenesysGrey1
    )

@Composable
fun WebMessagingTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colors = LightColorPalette,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}
