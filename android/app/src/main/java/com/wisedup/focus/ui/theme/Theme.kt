package com.wisedup.focus.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Brand tokens — must match res/values/colors.xml.
val FocusOffGreen = Color(0xFF1B873F)
val FocusOffGreenContainer = Color(0xFFD2F5DC)
val FocusActiveRed = Color(0xFFC8262C)
val FocusActiveAmber = Color(0xFFE08A1A)
val FocusActiveOn = Color(0xFFFFFFFF)
val SurfaceLight = Color(0xFFFFFFFF)
val SurfaceDark = Color(0xFF121212)
val OnSurfaceLight = Color(0xFF1A1A1A)
val OnSurfaceDark = Color(0xFFEDEDED)

private val LightColors = lightColorScheme(
    primary = FocusOffGreen,
    onPrimary = FocusActiveOn,
    primaryContainer = FocusOffGreenContainer,
    onPrimaryContainer = OnSurfaceLight,
    secondary = FocusActiveAmber,
    onSecondary = FocusActiveOn,
    error = FocusActiveRed,
    onError = FocusActiveOn,
    background = SurfaceLight,
    onBackground = OnSurfaceLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
)

private val DarkColors = darkColorScheme(
    primary = FocusOffGreen,
    onPrimary = FocusActiveOn,
    primaryContainer = FocusOffGreen,
    onPrimaryContainer = FocusActiveOn,
    secondary = FocusActiveAmber,
    onSecondary = FocusActiveOn,
    error = FocusActiveRed,
    onError = FocusActiveOn,
    background = SurfaceDark,
    onBackground = OnSurfaceDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
)

@Composable
fun WisedUpTheme(
    useDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (useDarkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colors,
        content = content,
    )
}
