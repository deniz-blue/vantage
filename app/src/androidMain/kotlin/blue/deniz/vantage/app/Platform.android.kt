package blue.deniz.vantage.app

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.SharedPreferences
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import blue.deniz.vantage.core.EventFeedReader
import blue.deniz.vantage.core.EventFetcher
import blue.deniz.vantage.core.EventPreviews
import blue.deniz.vantage.core.EventsLink
import blue.deniz.vantage.core.EventResolver
import blue.deniz.vantage.core.FolioClient
import blue.deniz.vantage.core.ICalendarLinkReader
import blue.deniz.vantage.core.MapLinks
import blue.deniz.vantage.core.MapPlace
import blue.deniz.vantage.core.PlaceSearch
import blue.deniz.vantage.core.WeatherForecasts
import blue.deniz.vantage.storage.EventStore
import blue.deniz.vantage.storage.VantageDatabase
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import java.util.IllformedLocaleException
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import androidx.compose.material3.CalendarLocale

@Composable
actual fun SystemBackHandler(enabled: Boolean, handlesEscape: Boolean, onBack: () -> Unit) = key(enabled) { BackHandler(enabled, onBack) }

actual fun handleEscapeKey(): Boolean = false

@Composable
actual fun rememberShareText(): (String) -> Unit {
    val context = LocalContext.current
    return remember(context) {
        { text ->
            val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
            context.startActivity(Intent.createChooser(send, null))
        }
    }
}

@Composable
actual fun rememberSendEditReply(): (EventsLink.EditReply) -> Unit {
    val uriHandler = LocalUriHandler.current
    return remember(uriHandler) { { reply -> uriHandler.openUri(EventsLink.returnLinkFor(reply)) } }
}

@Composable
actual fun rememberCopyText(): (String) -> Unit {
    val context = LocalContext.current
    return remember(context) {
        { text -> context.getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText(null, text)) }
    }
}

actual fun mapsAppLink(place: MapPlace): String = MapLinks.geo(place)

actual fun imageBitmapFromArgb(pixels: IntArray, width: Int, height: Int): ImageBitmap =
    Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888).asImageBitmap()

actual fun languageAutonym(code: String): String? {
    val locale = Locale.forLanguageTag(code)
    return locale.getDisplayLanguage(locale).takeUnless { it.isEmpty() || it == code }
}

actual fun countryName(code: String, displayLanguage: String): String = try {
    Locale.Builder().setRegion(code).build().getDisplayCountry(Locale.forLanguageTag(displayLanguage)).ifEmpty { code }
} catch (_: IllformedLocaleException) {
    code
}

private class SharedPreferencesStorage(private val preferences: SharedPreferences) : KeyValueStorage {
    override fun read(key: String): String? = preferences.getString(key, null)

    override fun write(key: String, value: String?) {
        preferences.edit().apply { if (value == null) remove(key) else putString(key, value) }.apply()
    }
}


fun createAppState(context: Context, backgroundChecks: BackgroundEventChecks? = null): AppState {
    val database = Room.databaseBuilder<VantageDatabase>(context, context.getDatabasePath("vantage.db").absolutePath)
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()
    val settingsStorage = SharedPreferencesStorage(context.getSharedPreferences("vantage-settings", Context.MODE_PRIVATE))
    val client = HttpClient(OkHttp)
    val resolver = EventResolver(EventFetcher(client))
    val previews = EventPreviews(resolver)
    return AppState(
        EventStore(database, resolver, FolioClient(client)),
        EventFeedReader(client, resolver, previews = previews),
        previews,
        PlaceSearch(client),
        WeatherForecasts(client),
        settingsStorage,
        ICalendarLinkReader(client),
        AndroidDeviceCalendars(context),
        AndroidPhoneCalendarCopy(context),
        backgroundChecks,
    )
}

@Composable
actual fun wallpaperColorScheme(dark: Boolean): ColorScheme? {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return null
    val context = LocalContext.current
    return if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
}

@Composable
actual fun rememberCalendarPermissionRequest(writes: Boolean, onResult: (granted: Boolean) -> Unit): () -> Unit {
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
        onResult(results.isNotEmpty() && results.values.all { it })
    }
    val permissions = if (writes) arrayOf(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR) else arrayOf(Manifest.permission.READ_CALENDAR)
    return remember(launcher, writes) { { launcher.launch(permissions) } }
}

@Composable
actual fun rememberNotificationPermissionRequest(onResult: (granted: Boolean) -> Unit): () -> Unit {
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission(), onResult)
    return remember(launcher) {
        { if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) launcher.launch(Manifest.permission.POST_NOTIFICATIONS) else onResult(true) }
    }
}

actual val mondayFirstCalendarLocale: CalendarLocale get() = java.util.Locale.UK
