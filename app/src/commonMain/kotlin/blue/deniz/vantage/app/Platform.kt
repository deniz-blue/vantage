package blue.deniz.vantage.app

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap
import blue.deniz.vantage.core.EventsLink
import blue.deniz.vantage.core.MapPlace
import androidx.compose.material3.CalendarLocale

@Composable
expect fun rememberShareText(): (String) -> Unit

@Composable
expect fun rememberSendEditReply(): (EventsLink.EditReply) -> Unit

@Composable
expect fun rememberCopyText(): (String) -> Unit

@Composable
expect fun rememberSaveTextFile(onSaved: (saved: Boolean) -> Unit): (fileName: String, text: String) -> Unit

@Composable
expect fun rememberOpenTextFile(onOpened: (text: String) -> Unit): () -> Unit

expect fun languageAutonym(code: String): String?

expect fun countryName(code: String, displayLanguage: String): String

@Composable
expect fun wallpaperColorScheme(dark: Boolean): ColorScheme?

class MapApp(val name: String, val isGoogleMaps: Boolean, val open: (MapPlace) -> Unit)

@Composable
expect fun rememberMapApps(): List<MapApp>

expect fun imageBitmapFromArgb(pixels: IntArray, width: Int, height: Int): ImageBitmap

@Composable
expect fun rememberCalendarPermissionRequest(writes: Boolean = false, onResult: (granted: Boolean) -> Unit): () -> Unit

@Composable
expect fun rememberNotificationPermissionRequest(onResult: (granted: Boolean) -> Unit): () -> Unit

expect val runsInBrowser: Boolean

expect val runsInAndroidBrowser: Boolean

expect val mondayFirstCalendarLocale: CalendarLocale

@Composable
expect fun rememberAppLabel(): String
