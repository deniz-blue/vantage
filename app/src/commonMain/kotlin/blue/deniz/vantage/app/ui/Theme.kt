package blue.deniz.vantage.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.RippleConfiguration
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material.ripple.RippleAlpha
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import blue.deniz.vantage.app.resources.Res
import blue.deniz.vantage.app.resources.lexend_bold
import blue.deniz.vantage.app.resources.lexend_medium
import blue.deniz.vantage.app.resources.lexend_regular
import blue.deniz.vantage.app.resources.lexend_semibold
import org.jetbrains.compose.resources.Font
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import blue.deniz.vantage.app.ThemeAccent
import blue.deniz.vantage.app.wallpaperColorScheme

@Composable
fun VantageTheme(accent: ThemeAccent, fontSizes: Map<TypographyStyle, Int>, content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val colors = grayColorScheme(dark, accentColor(accent, dark))
    MaterialTheme(colorScheme = colors, typography = appTypography(lexend(), fontSizes)) {
        CompositionLocalProvider(
            LocalMinimumInteractiveComponentSize provides ControlHeight.md,
            LocalRippleConfiguration provides VisibleFocusRipple,
            content = content,
        )
    }
}

private data class AccentShades(val dark: Color, val light: Color)

private val ThemeAccent.shades: AccentShades
    get() = when (this) {
        ThemeAccent.Purple, ThemeAccent.Wallpaper -> AccentShades(Color(0xFFA97DFF), Color(0xFF6B3FD4))
        ThemeAccent.Pink -> AccentShades(Color(0xFFFF7AC6), Color(0xFFB8306F))
        ThemeAccent.Blue -> AccentShades(Color(0xFF7AAEFF), Color(0xFF2F62C4))
        ThemeAccent.Green -> AccentShades(Color(0xFF66D49E), Color(0xFF1C7F4E))
        ThemeAccent.Orange -> AccentShades(Color(0xFFFFA866), Color(0xFFAD5410))
    }

@Composable
fun accentColor(accent: ThemeAccent, dark: Boolean): Color {
    val wallpaper = if (accent == ThemeAccent.Wallpaper) wallpaperColorScheme(dark)?.primary else null
    return wallpaper ?: if (dark) accent.shades.dark else accent.shades.light
}

@Composable
fun isWallpaperAccentAvailable(): Boolean = wallpaperColorScheme(isSystemInDarkTheme()) != null

internal fun contentColorOn(background: Color) = if (background.luminance() > 0.4f) Color.Black else Color.White

private fun grayColorScheme(dark: Boolean, accent: Color): ColorScheme {
    val base = if (dark) {
        darkColorScheme(
            background = Color(0xFF0F0F11),
            onBackground = Color(0xFFE8E8EA),
            surface = Color(0xFF0F0F11),
            onSurface = Color(0xFFE8E8EA),
            surfaceVariant = Color(0xFF313135),
            onSurfaceVariant = Color(0xFFA9A9AF),
            surfaceTint = Color(0xFFA9A9AF),
            inverseSurface = Color(0xFFE8E8EA),
            inverseOnSurface = Color(0xFF1C1C1F),
            outline = Color(0xFF77777E),
            outlineVariant = Color(0xFF3C3C41),
            surfaceDim = Color(0xFF0F0F11),
            surfaceBright = Color(0xFF323236),
            surfaceContainerLowest = Color(0xFF0A0A0B),
            surfaceContainerLow = Color(0xFF161618),
            surfaceContainer = Color(0xFF1C1C1F),
            surfaceContainerHigh = Color(0xFF262629),
            surfaceContainerHighest = Color(0xFF313135),
        )
    } else {
        lightColorScheme(
            background = Color(0xFFF9F9FA),
            onBackground = Color(0xFF1B1B1E),
            surface = Color(0xFFF9F9FA),
            onSurface = Color(0xFF1B1B1E),
            surfaceVariant = Color(0xFFE1E1E5),
            onSurfaceVariant = Color(0xFF55555C),
            surfaceTint = Color(0xFF55555C),
            inverseSurface = Color(0xFF2F2F33),
            inverseOnSurface = Color(0xFFF1F1F3),
            outline = Color(0xFF85858C),
            outlineVariant = Color(0xFFC8C8CD),
            surfaceDim = Color(0xFFDCDCDF),
            surfaceBright = Color(0xFFF9F9FA),
            surfaceContainerLowest = Color(0xFFFFFFFF),
            surfaceContainerLow = Color(0xFFF3F3F5),
            surfaceContainer = Color(0xFFEDEDF0),
            surfaceContainerHigh = Color(0xFFE7E7EA),
            surfaceContainerHighest = Color(0xFFE1E1E5),
        )
    }
    val towardText = if (dark) Color.White else Color.Black
    val secondary = lerp(accent, towardText, 0.25f)
    val primaryContainer = accent.copy(alpha = 0.28f).compositeOver(base.surfaceContainerHigh)
    val secondaryContainer = accent.copy(alpha = 0.2f).compositeOver(base.surfaceContainerHigh)
    return base.copy(
        primary = accent,
        onPrimary = contentColorOn(accent),
        primaryContainer = primaryContainer,
        onPrimaryContainer = lerp(accent, towardText, 0.6f),
        inversePrimary = lerp(accent, if (dark) Color.Black else Color.White, 0.3f),
        secondary = secondary,
        onSecondary = contentColorOn(secondary),
        secondaryContainer = secondaryContainer,
        onSecondaryContainer = base.onSurface,
        tertiary = secondary,
        onTertiary = contentColorOn(secondary),
        tertiaryContainer = secondaryContainer,
        onTertiaryContainer = base.onSurface,
    )
}

@Composable
private fun lexend() = FontFamily(
    Font(Res.font.lexend_regular, FontWeight.Normal),
    Font(Res.font.lexend_medium, FontWeight.Medium),
    Font(Res.font.lexend_semibold, FontWeight.SemiBold),
    Font(Res.font.lexend_bold, FontWeight.Bold),
)

@Composable
private fun appTypography(family: FontFamily, fontSizes: Map<TypographyStyle, Int>) =
    remember(family, fontSizes) { AppTypography.withFontSizes(fontSizes).withFontFamily(family) }

enum class TypographyStyle(val label: String) {
    HeadlineLarge("Headline Large"),
    HeadlineMedium("Headline Medium"),
    HeadlineSmall("Headline Small"),
    TitleLarge("Title Large"),
    TitleMedium("Title Medium"),
    TitleSmall("Title Small"),
    BodyLarge("Body Large"),
    BodyMedium("Body Medium"),
    BodySmall("Body Small"),
    LabelLarge("Label Large"),
    LabelMedium("Label Medium"),
    LabelSmall("Label Small"),
}

val TypographyStyle.defaultFontSize: Int get() = AppTypography.styleOf(this).fontSize.value.toInt()

private fun Typography.styleOf(style: TypographyStyle): TextStyle = when (style) {
    TypographyStyle.HeadlineLarge -> headlineLarge
    TypographyStyle.HeadlineMedium -> headlineMedium
    TypographyStyle.HeadlineSmall -> headlineSmall
    TypographyStyle.TitleLarge -> titleLarge
    TypographyStyle.TitleMedium -> titleMedium
    TypographyStyle.TitleSmall -> titleSmall
    TypographyStyle.BodyLarge -> bodyLarge
    TypographyStyle.BodyMedium -> bodyMedium
    TypographyStyle.BodySmall -> bodySmall
    TypographyStyle.LabelLarge -> labelLarge
    TypographyStyle.LabelMedium -> labelMedium
    TypographyStyle.LabelSmall -> labelSmall
}

private fun Typography.withStyle(style: TypographyStyle, textStyle: TextStyle): Typography = when (style) {
    TypographyStyle.HeadlineLarge -> copy(headlineLarge = textStyle)
    TypographyStyle.HeadlineMedium -> copy(headlineMedium = textStyle)
    TypographyStyle.HeadlineSmall -> copy(headlineSmall = textStyle)
    TypographyStyle.TitleLarge -> copy(titleLarge = textStyle)
    TypographyStyle.TitleMedium -> copy(titleMedium = textStyle)
    TypographyStyle.TitleSmall -> copy(titleSmall = textStyle)
    TypographyStyle.BodyLarge -> copy(bodyLarge = textStyle)
    TypographyStyle.BodyMedium -> copy(bodyMedium = textStyle)
    TypographyStyle.BodySmall -> copy(bodySmall = textStyle)
    TypographyStyle.LabelLarge -> copy(labelLarge = textStyle)
    TypographyStyle.LabelMedium -> copy(labelMedium = textStyle)
    TypographyStyle.LabelSmall -> copy(labelSmall = textStyle)
}

private fun Typography.withFontSizes(fontSizes: Map<TypographyStyle, Int>): Typography {
    var typography = this
    for ((style, size) in fontSizes) {
        val textStyle = typography.styleOf(style)
        val lineHeight = textStyle.lineHeight.value + size - textStyle.fontSize.value
        typography = typography.withStyle(style, textStyle.copy(fontSize = size.sp, lineHeight = lineHeight.sp))
    }
    return typography
}

private fun Typography.withFontFamily(family: FontFamily) = Typography(
    displayLarge = displayLarge.copy(fontFamily = family),
    displayMedium = displayMedium.copy(fontFamily = family),
    displaySmall = displaySmall.copy(fontFamily = family),
    headlineLarge = headlineLarge.copy(fontFamily = family),
    headlineMedium = headlineMedium.copy(fontFamily = family),
    headlineSmall = headlineSmall.copy(fontFamily = family),
    titleLarge = titleLarge.copy(fontFamily = family),
    titleMedium = titleMedium.copy(fontFamily = family),
    titleSmall = titleSmall.copy(fontFamily = family),
    bodyLarge = bodyLarge.copy(fontFamily = family),
    bodyMedium = bodyMedium.copy(fontFamily = family),
    bodySmall = bodySmall.copy(fontFamily = family),
    labelLarge = labelLarge.copy(fontFamily = family),
    labelMedium = labelMedium.copy(fontFamily = family),
    labelSmall = labelSmall.copy(fontFamily = family),
)

private val AppTypography = Typography().run {
    copy(
        headlineLarge = headlineLarge.copy(fontSize = 30.sp, lineHeight = 36.sp),
        headlineMedium = headlineMedium.copy(fontSize = 26.sp, lineHeight = 32.sp),
        headlineSmall = headlineSmall.copy(fontSize = 20.sp, lineHeight = 26.sp),
        titleLarge = titleLarge.copy(fontSize = 20.sp, lineHeight = 26.sp),
        titleMedium = titleMedium.copy(fontSize = 17.sp, lineHeight = 24.sp),
        titleSmall = titleSmall.copy(fontSize = 13.sp, lineHeight = 18.sp),
        bodyLarge = bodyLarge.copy(fontSize = 15.sp, lineHeight = 22.sp),
        bodyMedium = bodyMedium.copy(fontSize = 15.sp, lineHeight = 20.sp),
        bodySmall = bodySmall.copy(fontSize = 13.sp, lineHeight = 18.sp),
        labelLarge = labelLarge.copy(fontSize = 13.sp, lineHeight = 18.sp),
        labelMedium = labelMedium.copy(fontSize = 13.sp, lineHeight = 18.sp),
    )
}

private val VisibleFocusRipple = RippleConfiguration(
    rippleAlpha = RippleAlpha(draggedAlpha = 0.16f, focusedAlpha = 0.32f, hoveredAlpha = 0.08f, pressedAlpha = 0.1f),
)

object Spacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 16.dp
    val lg = 24.dp
    val xl = 32.dp
}

object IconSize {
    val xs = 16.dp
    val sm = 18.dp
    val md = 22.dp
    val lg = 26.dp
}

object ControlHeight {
    val sm = 32.dp
    val md = 40.dp
    val lg = 44.dp
}

object Radius {
    val xs = 6.dp
    val sm = 10.dp
    val md = 12.dp
    val lg = 16.dp
}
