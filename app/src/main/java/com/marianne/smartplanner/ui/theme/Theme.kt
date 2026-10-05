package com.marianne.smartplanner.ui.theme

import android.content.Context
import android.content.res.Configuration
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb

// NOTE: the `pink*` fields hold the user's chosen accent (pink by default).
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

// ── Theme color presets ───────────────────────────────────────────────────────

data class ThemePreset(val id: String, val label: String, val light: Color, val dark: Color)

object ThemePresets {
    const val DEFAULT_ID = "pink"
    private const val PREFS = "app_settings"
    private const val KEY   = "theme_color"

    val all = listOf(
        ThemePreset("pink",   "Pink",   Color(0xFFD81B60), Color(0xFFFF80AB)),
        ThemePreset("red",    "Red",    Color(0xFFD32F2F), Color(0xFFFF8A80)),
        ThemePreset("orange", "Orange", Color(0xFFE64A19), Color(0xFFFFAB91)),
        ThemePreset("green",  "Green",  Color(0xFF2E7D32), Color(0xFFA5D6A7)),
        ThemePreset("teal",   "Teal",   Color(0xFF00796B), Color(0xFF80CBC4)),
        ThemePreset("blue",   "Blue",   Color(0xFF1976D2), Color(0xFF90CAF9)),
        ThemePreset("indigo", "Indigo", Color(0xFF3949AB), Color(0xFF9FA8DA)),
        ThemePreset("purple", "Purple", Color(0xFF7B1FA2), Color(0xFFCE93D8)),
    )

    // A custom color is encoded in the id itself: "custom:RRGGBB".
    private const val CUSTOM = "custom:"

    fun customId(color: Color) = "$CUSTOM%06X".format(color.toArgb() and 0xFFFFFF)
    fun isCustom(id: String) = id.startsWith(CUSTOM)

    /** The color exactly as chosen/typed. */
    fun customColor(id: String): Color =
        id.removePrefix(CUSTOM).toLongOrNull(16)?.let { Color(0xFF000000L or (it and 0xFFFFFFL)) } ?: Color(0xFFD81B60)

    // The accent carries white text (buttons, selected chips), so a very light pick is
    // darkened until white text has a contrast ratio of about 3:1.
    private fun readableOnWhiteText(c: Color): Color {
        var x = c
        var n = 0
        while (x.luminance() > 0.3f && n++ < 20) x = lerp(x, Color.Black, 0.1f)
        return x
    }

    fun find(id: String): ThemePreset {
        if (isCustom(id)) {
            val light = readableOnWhiteText(customColor(id))
            return ThemePreset(id, "Custom", light, lerp(light, Color.White, 0.5f))
        }
        return all.firstOrNull { it.id == id } ?: all.first()
    }

    fun get(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, DEFAULT_ID) ?: DEFAULT_ID

    fun set(context: Context, id: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, id).apply()
    }

    fun colorsFor(id: String, dark: Boolean): AppColors {
        val base = if (dark) DarkAppColors else LightAppColors
        val p = find(id)
        if (p.id == DEFAULT_ID) return base
        val accent = if (dark) p.dark else p.light
        return if (dark) base.copy(
            pink          = accent,
            pinkLight     = lerp(accent, Color.Black, 0.65f),
            pinkContainer = lerp(accent, Color.Black, 0.78f)
        ) else base.copy(
            pink          = accent,
            pinkLight     = lerp(accent, Color.White, 0.75f),
            pinkContainer = lerp(accent, Color.White, 0.90f)
        )
    }

    private fun schemeFor(id: String, dark: Boolean, c: AppColors): ColorScheme {
        val base = if (dark) DarkColorScheme else LightColorScheme
        if (find(id).id == DEFAULT_ID) return base
        return if (dark) base.copy(
            primary            = c.pink,
            onPrimary          = lerp(c.pink, Color.Black, 0.8f),
            primaryContainer   = c.pinkContainer,
            onPrimaryContainer = lerp(c.pink, Color.White, 0.8f),
            secondary          = c.pink
        ) else base.copy(
            primary            = c.pink,
            primaryContainer   = c.pinkContainer,
            onPrimaryContainer = lerp(c.pink, Color.Black, 0.75f),
            secondary          = c.pink
        )
    }

    internal fun scheme(id: String, dark: Boolean, c: AppColors) = schemeFor(id, dark, c)
}

/** Accent colors for non-Compose callers (widgets). */
object ThemeColors {
    private fun isDark(context: Context) =
        (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES

    fun accent(context: Context): Color {
        val dark = isDark(context)
        return ThemePresets.colorsFor(ThemePresets.get(context), dark).pink
    }

    fun accentContainer(context: Context): Color {
        val dark = isDark(context)
        return ThemePresets.colorsFor(ThemePresets.get(context), dark).pinkContainer
    }
}

@Composable
fun SmartPlannerTheme(themeId: String = ThemePresets.DEFAULT_ID, content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val appColors   = remember(themeId, dark) { ThemePresets.colorsFor(themeId, dark) }
    val colorScheme = remember(themeId, dark) { ThemePresets.scheme(themeId, dark, appColors) }
    CompositionLocalProvider(LocalAppColors provides appColors) {
        MaterialTheme(colorScheme = colorScheme, content = content)
    }
}
