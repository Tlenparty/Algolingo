package com.tlenparty.algolingo.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** Colors M3 has no slot for (03-UI-SPEC). Tint = background, Text = text on tint. */
data class AppColors(
    val successTint: Color, val successText: Color,
    val acceptableTint: Color, val acceptableText: Color,
    val wrongTint: Color, val wrongText: Color,
    val focusTint: Color, val focusText: Color,
    val keyword: Color, val number: Color, val string: Color, val comment: Color,
)

private val LightApp = AppColors(
    Color(0xFFD7FFB8), Color(0xFF2F7A00), Color(0xFFFFF4CC), Color(0xFF8A5A00),
    Color(0xFFFFDFE0), Color(0xFFC62828), Color(0xFFDDF4FF), Color(0xFF0B6FA4),
    Color(0xFFAF00DB), Color(0xFF0B6FA4), Color(0xFFA0522D), Color(0xFF6B6B6B),
)
private val DarkApp = AppColors(
    Color(0xFF1F3A1A), Color(0xFF9BE15D), Color(0xFF3A3220), Color(0xFFFFD54A),
    Color(0xFF3A1F24), Color(0xFFFF8A8A), Color(0xFF1F3A4A), Color(0xFF4FC1FF),
    Color(0xFFC586C0), Color(0xFF4FC1FF), Color(0xFFCE9178), Color(0xFF9AA9B1),
)

val LocalAppColors = staticCompositionLocalOf { LightApp }

// Fonts: FontFamily.Default / Monospace; Nunito and JetBrains Mono are deferred to Phase 3.
@Composable
fun AppTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val scheme = if (dark) {
        darkColorScheme(
            primary = Color(0xFF58CC02), onPrimary = Color(0xFF131F24), error = Color(0xFFFF4B4B),
            background = Color(0xFF131F24), surface = Color(0xFF131F24),
            surfaceVariant = Color(0xFF202F36), outline = Color(0xFF37464F),
            onBackground = Color(0xFFF1F7FB), onSurface = Color(0xFFF1F7FB), onSurfaceVariant = Color(0xFFF1F7FB),
        )
    } else {
        lightColorScheme(
            primary = Color(0xFF58CC02), onPrimary = Color(0xFF131F24), error = Color(0xFFFF4B4B),
            background = Color(0xFFFFFFFF), surface = Color(0xFFFFFFFF),
            surfaceVariant = Color(0xFFF7F7F7), outline = Color(0xFFE5E5E5),
            onBackground = Color(0xFF3C3C3C), onSurface = Color(0xFF3C3C3C), onSurfaceVariant = Color(0xFF3C3C3C),
        )
    }
    CompositionLocalProvider(LocalAppColors provides if (dark) DarkApp else LightApp) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}
