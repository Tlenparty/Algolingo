package com.tlenparty.algolingo.design

import algolingo.design.generated.resources.Res
import algolingo.design.generated.resources.jetbrains_mono_bold
import algolingo.design.generated.resources.jetbrains_mono_regular
import algolingo.design.generated.resources.nunito_bold
import algolingo.design.generated.resources.nunito_regular
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.Font

/** Colors M3 has no slot for (03-UI-SPEC). Tint = background, Text = text on tint. M3 slots stay in MaterialTheme.colorScheme. */
data class AlgoColors(
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

private val LightApp = AlgoColors(
    successTint = Color(0xFFD7FFB8), successText = Color(0xFF2F7A00),
    acceptableTint = Color(0xFFFFF4CC), acceptableText = Color(0xFF8A5A00),
    wrongTint = Color(0xFFFFDFE0), wrongText = Color(0xFFC62828),
    focusTint = Color(0xFFDDF4FF), focusText = Color(0xFF0B6FA4),
    keyword = Color(0xFFAF00DB), number = Color(0xFF0B6FA4), string = Color(0xFFA0522D), comment = Color(0xFF6B6B6B),
    greenLedge = Color(0xFF58A700), redFill = Color(0xFFFF4B4B), redLedge = Color(0xFFEA2B2B),
    amber = Color(0xFFFFC800), amberLedge = Color(0xFFE5A500), focus = Color(0xFF1CB0F6),
    disabledFace = Color(0xFFE5E5E5), disabledLabel = Color(0xFFAFAFAF),
    secondaryText = Color(0xFF6B6B6B), xpText = Color(0xFF2F7A00),
)
private val DarkApp = AlgoColors(
    successTint = Color(0xFF1F3A1A), successText = Color(0xFF9BE15D),
    acceptableTint = Color(0xFF3A3220), acceptableText = Color(0xFFFFD54A),
    wrongTint = Color(0xFF3A1F24), wrongText = Color(0xFFFF8A8A),
    focusTint = Color(0xFF1F3A4A), focusText = Color(0xFF4FC1FF),
    keyword = Color(0xFFC586C0), number = Color(0xFF4FC1FF), string = Color(0xFFCE9178), comment = Color(0xFF9AA9B1),
    greenLedge = Color(0xFF58A700), redFill = Color(0xFFFF4B4B), redLedge = Color(0xFFEA2B2B),
    amber = Color(0xFFFFC800), amberLedge = Color(0xFFE5A500), focus = Color(0xFF1CB0F6),
    disabledFace = Color(0xFF37464F), disabledLabel = Color(0xFF52656D),
    secondaryText = Color(0xFF9AA9B1), xpText = Color(0xFF58CC02),
)

// Every M3 slot a stock component can read is set explicitly, so nothing falls back to the purple
// baseline (NavigationBar container/indicator, AlertDialog container, text fields, tonal elevation).
private fun algoScheme(
    dark: Boolean, bg: Color, variant: Color, outline: Color, ink: Color, app: AlgoColors,
) = (if (dark) darkColorScheme() else lightColorScheme()).copy(
    primary = Green, onPrimary = OnAccent, primaryContainer = app.successTint, onPrimaryContainer = app.successText,
    inversePrimary = Green,
    secondary = Green, onSecondary = OnAccent, secondaryContainer = app.successTint, onSecondaryContainer = app.successText,
    tertiary = Green, onTertiary = OnAccent, tertiaryContainer = app.successTint, onTertiaryContainer = app.successText,
    error = app.redFill, onError = OnAccent, errorContainer = app.wrongTint, onErrorContainer = app.wrongText,
    background = bg, onBackground = ink, surface = bg, onSurface = ink,
    surfaceVariant = variant, onSurfaceVariant = ink, surfaceTint = bg,
    inverseSurface = ink, inverseOnSurface = bg,
    outline = outline, outlineVariant = outline,
    surfaceBright = bg, surfaceDim = variant,
    surfaceContainerLowest = bg, surfaceContainerLow = bg, surfaceContainer = bg,
    surfaceContainerHigh = bg, surfaceContainerHighest = variant,
)

private val Green = Color(0xFF58CC02)
private val OnAccent = Color(0xFF131F24)
private val LightScheme = algoScheme(
    dark = false, bg = Color(0xFFFFFFFF), variant = Color(0xFFF7F7F7), outline = Color(0xFFE5E5E5),
    ink = Color(0xFF3C3C3C), app = LightApp,
)
private val DarkScheme = algoScheme(
    dark = true, bg = Color(0xFF131F24), variant = Color(0xFF202F36), outline = Color(0xFF37464F),
    ink = Color(0xFFF1F7FB), app = DarkApp,
)

/** JetBrains Mono ligatures turn `<=`/`!=`/`->` into ≤/≠/→; learners must see the real Kotlin tokens. */
const val NO_LIGATURES = "liga 0, calt 0"

/** Exactly two weights app-wide: Normal and Bold (03-UI-SPEC Typography). */
class AlgoTypography(ui: FontFamily, val code: FontFamily) {
    val body = TextStyle(fontFamily = ui, fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.Normal)
    val label = TextStyle(fontFamily = ui, fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Bold)
    val heading = TextStyle(fontFamily = ui, fontSize = 20.sp, lineHeight = 24.sp, fontWeight = FontWeight.Bold)
    val display = TextStyle(fontFamily = ui, fontSize = 28.sp, lineHeight = 34.sp, fontWeight = FontWeight.Bold)
    val button = TextStyle(fontFamily = ui, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    val codeRegular = TextStyle(fontFamily = code, fontFeatureSettings = NO_LIGATURES, fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Normal)
    val codeBold = TextStyle(fontFamily = code, fontFeatureSettings = NO_LIGATURES, fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Bold)
    val codeChip = TextStyle(fontFamily = code, fontFeatureSettings = NO_LIGATURES, fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.Bold)

    /** Emoji glyph sizes: top bar ✕/💡, summary hero. */
    val iconEmoji: TextUnit = 24.sp
    val heroEmoji: TextUnit = 64.sp
}

/** 03-UI-SPEC spacing scale; pillX = horizontal padding inside the Home «Начать» pill. */
data class AlgoSpacing(
    val xs: Dp = 4.dp,
    val sm: Dp = 8.dp,
    val smd: Dp = 12.dp,
    val md: Dp = 16.dp,
    val lg: Dp = 24.dp,
    val xl: Dp = 32.dp,
    val pillX: Dp = 20.dp,
)

data class AlgoSizes(
    val touchTarget: Dp = 48.dp,
    val topBar: Dp = 56.dp,
    val progressBar: Dp = 16.dp,
    val buttonFace: Dp = 52.dp,
    val chipFace: Dp = 44.dp,
    val pillFace: Dp = 40.dp,
    val ledge: Dp = 4.dp,
    val border: Dp = 2.dp,
    val holeLine: Dp = 40.dp,
    val holeFace: Dp = 32.dp,
    val holeMinWidth: Dp = 48.dp,
    val codeLine: Dp = 24.dp,
    val node: Dp = 72.dp,
    /** Home node ▶, drawn as a path (U+25B6 falls back to the colour emoji on iOS). */
    val nodeGlyph: Dp = 24.dp,
    val nodeCaption: Dp = 160.dp,
    val contentMaxWidth: Dp = 600.dp,
    val dialogButton: Dp = 140.dp,
    val quoteBar: Dp = 3.dp,
    val listMarker: Dp = 24.dp,
)

data class AlgoShapes(
    /** Hole, kb code block. */
    val small: Shape = RoundedCornerShape(8.dp),
    /** Chip, hint card, callout. */
    val medium: Shape = RoundedCornerShape(12.dp),
    /** Button, code block, template, theory card. */
    val large: Shape = RoundedCornerShape(16.dp),
    /** Home «Начать». */
    val pill: Shape = RoundedCornerShape(20.dp),
    /** Home path node. */
    val node: Shape = RoundedCornerShape(36.dp),
)

private val LocalAlgoColors = staticCompositionLocalOf { LightApp }
private val LocalAlgoTypography = staticCompositionLocalOf<AlgoTypography> { error("AlgoTheme not provided") }
private val LocalAlgoSpacing = staticCompositionLocalOf { AlgoSpacing() }
private val LocalAlgoSizes = staticCompositionLocalOf { AlgoSizes() }
private val LocalAlgoShapes = staticCompositionLocalOf { AlgoShapes() }

object AlgoTheme {
    val colors: AlgoColors @Composable @ReadOnlyComposable get() = LocalAlgoColors.current
    val typography: AlgoTypography @Composable @ReadOnlyComposable get() = LocalAlgoTypography.current
    val spacing: AlgoSpacing @Composable @ReadOnlyComposable get() = LocalAlgoSpacing.current
    val sizes: AlgoSizes @Composable @ReadOnlyComposable get() = LocalAlgoSizes.current
    val shapes: AlgoShapes @Composable @ReadOnlyComposable get() = LocalAlgoShapes.current
}

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
fun AlgoTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val ui = FontFamily(
        Font(Res.font.nunito_regular, FontWeight.Normal),
        Font(Res.font.nunito_bold, FontWeight.Bold),
    )
    val code = FontFamily(
        Font(Res.font.jetbrains_mono_regular, FontWeight.Normal),
        Font(Res.font.jetbrains_mono_bold, FontWeight.Bold),
    )
    val typography = remember(ui, code) { AlgoTypography(ui, code) }
    CompositionLocalProvider(
        LocalAlgoColors provides if (dark) DarkApp else LightApp,
        LocalAlgoTypography provides typography,
    ) {
        MaterialTheme(
            colorScheme = if (dark) DarkScheme else LightScheme,
            typography = MaterialTheme.typography.withFont(ui),
            content = content,
        )
    }
}
