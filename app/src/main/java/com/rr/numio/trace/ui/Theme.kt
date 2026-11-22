package com.rr.numio.trace.ui

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/** Trace colours, from the official Numio palette. */
data class Palette(
    val bg: Color,
    val surface: Color,
    val line: Color,
    val text: Color,
    val muted: Color,
    val accent: Color,
    val accentText: Color,
    val onAccent: Color,
    val dark: Boolean,
)

val DarkPalette = Palette(
    bg = Color(0xFF0F0F0E), surface = Color(0xFF1A1916), line = Color(0xFF2A2925),
    text = Color(0xFFF1EEE6), muted = Color(0xFFA8A49A),
    accent = Color(0xFFFFC21A), accentText = Color(0xFFFFC21A), onAccent = Color(0xFF1A1400),
    dark = true,
)

val LightPalette = Palette(
    bg = Color(0xFFF6F4EE), surface = Color(0xFFFFFFFF), line = Color(0xFFE4E0D5),
    text = Color(0xFF191815), muted = Color(0xFF5F5B52),
    accent = Color(0xFFE9A800), accentText = Color(0xFF7A5600), onAccent = Color(0xFF1A1400),
    dark = false,
)

val LocalPalette = staticCompositionLocalOf { DarkPalette }

@Composable
fun TraceTheme(dark: Boolean, content: @Composable () -> Unit) {
    val p = if (dark) DarkPalette else LightPalette
    val scheme = if (dark) {
        darkColorScheme(
            primary = p.accent, onPrimary = p.onAccent,
            background = p.bg, onBackground = p.text,
            surface = p.surface, onSurface = p.text, onSurfaceVariant = p.muted,
            surfaceContainer = p.surface, surfaceContainerLow = p.surface, surfaceContainerHigh = p.surface,
            surfaceContainerHighest = p.surface, outline = p.line, outlineVariant = p.line,
        )
    } else {
        lightColorScheme(
            primary = p.accent, onPrimary = p.onAccent,
            background = p.bg, onBackground = p.text,
            surface = p.surface, onSurface = p.text, onSurfaceVariant = p.muted,
            surfaceContainer = p.surface, surfaceContainerLow = p.surface, surfaceContainerHigh = p.surface,
            surfaceContainerHighest = p.surface, outline = p.line, outlineVariant = p.line,
        )
    }
    SystemBarIcons(lightIcons = dark)
    CompositionLocalProvider(LocalPalette provides p) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}

/** Dark status-bar icons on light screens, light icons on dark ones. */
@Composable
private fun SystemBarIcons(lightIcons: Boolean) {
    val view = LocalView.current
    DisposableEffect(lightIcons) {
        val window = (view.context as? Activity)?.window
        if (window != null) {
            val c = WindowCompat.getInsetsController(window, view)
            c.isAppearanceLightStatusBars = !lightIcons
            c.isAppearanceLightNavigationBars = !lightIcons
        }
        onDispose { }
    }
}
