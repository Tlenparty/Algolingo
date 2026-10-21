package com.tlenparty.algolingo.ui.theme

import algolingo.shared.generated.resources.Res
import algolingo.shared.generated.resources.jetbrains_mono_bold
import algolingo.shared.generated.resources.jetbrains_mono_regular
import algolingo.shared.generated.resources.nunito_bold
import algolingo.shared.generated.resources.nunito_regular
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.Font

/** Colors M3 has no slot for (03-UI-SPEC). Tint = background, Text = text on tint. */
data class AppColors(
    val successTint: Color, val successText: Color,
    val acceptableTint: Color, val acceptableText: Color,
    val wrongTint: Color, val wrongText: Color,
    val focusTint: Color, val focusText: Color,
    val keyword: Color, val number: Color, val string: Color, val comment: Color,
    val greenLedge: Color, val redFill: Color, val redLedge: Color,
    val amber: Color, val amberLedge: Color, val focus: Color,
    val disabledFace: Color, val disabledLabel: Color,
    val secondaryText: Color, val xpText: Color,
)

private val LightApp = AppColors(
    Color(0xFFD7FFB8), Color(0xFF2F7A00), Color(0xFFFFF4CC), Color(0xFF8A5A00),
    Color(0xFFFFDFE0), Color(0xFFC62828), Color(0xFFDDF4FF), Color(0xFF0B6FA4),
    Color(0xFFAF00DB), Color(0xFF0B6FA4), Color(0xFFA0522D), Color(0xFF6B6B6B),
    Color(0xFF58A700), Color(0xFFFF4B4B), Color(0xFFEA2B2B),
    Color(0xFFFFC800), Color(0xFFE5A500), Color(0xFF1CB0F6),
    Color(0xFFE5E5E5), Color(0xFFAFAFAF), Color(0xFF6B6B6B), Color(0xFF2F7A00),
)
private val DarkApp = AppColors(
    Color(0xFF1F3A1A), Color(0xFF9BE15D), Color(0xFF3A3220), Color(0xFFFFD54A),
    Color(0xFF3A1F24), Color(0xFFFF8A8A), Color(0xFF1F3A4A), Color(0xFF4FC1FF),
    Color(0xFFC586C0), Color(0xFF4FC1FF), Color(0xFFCE9178), Color(0xFF9AA9B1),
    Color(0xFF58A700), Color(0xFFFF4B4B), Color(0xFFEA2B2B),
    Color(0xFFFFC800), Color(0xFFE5A500), Color(0xFF1CB0F6),
    Color(0xFF37464F), Color(0xFF52656D), Color(0xFF9AA9B1), Color(0xFF58CC02),
)

val LocalAppColors = staticCompositionLocalOf { LightApp }

val LocalCodeFont = staticCompositionLocalOf<FontFamily> { FontFamily.Monospace }

/** JetBrains Mono ligatures turn `<=`/`!=`/`->` into ≤/≠/→; learners must see the real Kotlin tokens. */
const val NO_LIGATURES = "liga 0, calt 0"

/** Exactly two weights app-wide: Normal and Bold (03-UI-SPEC Typography). */
class AppTextStyles(ui: FontFamily, code: FontFamily) {
    val body = TextStyle(fontFamily = ui, fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.Normal)
    val label = TextStyle(fontFamily = ui, fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Bold)
    val heading = TextStyle(fontFamily = ui, fontSize = 20.sp, lineHeight = 24.sp, fontWeight = FontWeight.Bold)
    val display = TextStyle(fontFamily = ui, fontSize = 28.sp, lineHeight = 34.sp, fontWeight = FontWeight.Bold)
    val button = TextStyle(fontFamily = ui, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    val codeRegular = TextStyle(fontFamily = code, fontFeatureSettings = NO_LIGATURES, fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Normal)
    val codeBold = TextStyle(fontFamily = code, fontFeatureSettings = NO_LIGATURES, fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Bold)
    val codeChip = TextStyle(fontFamily = code, fontFeatureSettings = NO_LIGATURES, fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.Bold)
}

val LocalAppText = staticCompositionLocalOf<AppTextStyles> { error("AppTheme not provided") }

private fun Typography.withFont(f: FontFamily) = copy(
    displayLarge = displayLarge.copy(fontFamily = f), displayMedium = displayMedium.copy(fontFamily = f),
    displaySmall = displaySmall.copy(fontFamily = f), headlineLarge = headlineLarge.copy(fontFamily = f),
    headlineMedium = headlineMedium.copy(fontFamily = f), headlineSmall = headlineSmall.copy(fontFamily = f),
    titleLarge = titleLarge.copy(fontFamily = f), titleMedium = titleMedium.copy(fontFamily = f),
    titleSmall = titleSmall.copy(fontFamily = f), bodyLarge = bodyLarge.copy(fontFamily = f),
    bodyMedium = bodyMedium.copy(fontFamily = f), bodySmall = bodySmall.copy(fontFamily = f),
    labelLarge = labelLarge.copy(fontFamily = f), labelMedium = labelMedium.copy(fontFamily = f),
    labelSmall = labelSmall.copy(fontFamily = f),
)

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
    val ui = FontFamily(
        Font(Res.font.nunito_regular, FontWeight.Normal),
        Font(Res.font.nunito_bold, FontWeight.Bold),
    )
    val code = FontFamily(
        Font(Res.font.jetbrains_mono_regular, FontWeight.Normal),
        Font(Res.font.jetbrains_mono_bold, FontWeight.Bold),
    )
    val text = remember(ui, code) { AppTextStyles(ui, code) }
    CompositionLocalProvider(
        LocalAppColors provides if (dark) DarkApp else LightApp,
        LocalCodeFont provides code,
        LocalAppText provides text,
    ) {
        MaterialTheme(colorScheme = scheme, typography = MaterialTheme.typography.withFont(ui), content = content)
    }
}
