package blue.deniz.vantage.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import blue.deniz.vantage.app.ThemeAccent
import blue.deniz.vantage.app.wallpaperColorScheme

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
