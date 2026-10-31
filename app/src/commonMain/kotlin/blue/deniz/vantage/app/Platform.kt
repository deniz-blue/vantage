package blue.deniz.vantage.app

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap
import blue.deniz.vantage.core.EventsLink
import blue.deniz.vantage.core.MapPlace
import androidx.compose.material3.CalendarLocale

@Composable
expect fun SystemBackHandler(enabled: Boolean, handlesEscape: Boolean = false, onBack: () -> Unit)

expect fun handleEscapeKey(): Boolean

@Composable
expect fun rememberShareText(): (String) -> Unit

@Composable
expect fun rememberSendEditReply(): (EventsLink.EditReply) -> Unit

@Composable
expect fun rememberCopyText(): (String) -> Unit

expect fun languageAutonym(code: String): String?

expect fun countryName(code: String, displayLanguage: String): String

@Composable
expect fun wallpaperColorScheme(dark: Boolean): ColorScheme?

expect fun mapsAppLink(place: MapPlace): String

expect fun imageBitmapFromArgb(pixels: IntArray, width: Int, height: Int): ImageBitmap

@Composable
expect fun rememberCalendarPermissionRequest(writes: Boolean = false, onResult: (granted: Boolean) -> Unit): () -> Unit

@Composable
expect fun rememberNotificationPermissionRequest(onResult: (granted: Boolean) -> Unit): () -> Unit

expect val mondayFirstCalendarLocale: CalendarLocale
