package com.marianne.smartplanner.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

data class AppColors(
    val pink: Color,
    val pinkLight: Color,
    val pinkContainer: Color,
    val pageBg: Color,
    val cardBg: Color,
    val textMain: Color,
    val textSub: Color
)

private val LightAppColors = AppColors(
    pink          = Color(0xFFD81B60),
    pinkLight     = Color(0xFFFFCDD2),
    pinkContainer = Color(0xFFFCE4EC),
    pageBg        = Color(0xFFFFFDF5),
    cardBg        = Color.White,
    textMain      = Color(0xFF2C2C2C),
    textSub       = Color(0xFF6D6D6D)
)

private val DarkAppColors = AppColors(
    pink          = Color(0xFFFF80AB),
    pinkLight     = Color(0xFF5C2535),
    pinkContainer = Color(0xFF3E1729),
    pageBg        = Color(0xFF1C1B1F),
    cardBg        = Color(0xFF2D2B32),
    textMain      = Color(0xFFE6E1E5),
    textSub       = Color(0xFF9E99A3)
)

val LocalAppColors = staticCompositionLocalOf { LightAppColors }

private val LightColorScheme = lightColorScheme(
    primary            = LightAppColors.pink,
    onPrimary          = Color.White,
    primaryContainer   = LightAppColors.pinkContainer,
    onPrimaryContainer = Color(0xFF3E0021),
    secondary          = Color(0xFFE91E8C),
    background         = LightAppColors.pageBg,
    surface            = LightAppColors.pageBg,
    onBackground       = LightAppColors.textMain,
    onSurface          = LightAppColors.textMain,
)

private val DarkColorScheme = darkColorScheme(
    primary            = DarkAppColors.pink,
    onPrimary          = Color(0xFF3E0021),
    primaryContainer   = DarkAppColors.pinkContainer,
    onPrimaryContainer = Color(0xFFFFD9E3),
    secondary          = DarkAppColors.pink,
    background         = DarkAppColors.pageBg,
    surface            = DarkAppColors.cardBg,
    onBackground       = DarkAppColors.textMain,
    onSurface          = DarkAppColors.textMain,
)

@Composable
fun SmartPlannerTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val appColors = if (dark) DarkAppColors else LightAppColors
    val colorScheme = if (dark) DarkColorScheme else LightColorScheme
    CompositionLocalProvider(LocalAppColors provides appColors) {
        MaterialTheme(colorScheme = colorScheme, content = content)
    }
}
