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
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import java.io.File
import java.io.IOException
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import blue.deniz.vantage.core.EventFeedReader
import blue.deniz.vantage.core.EventFetcher
import blue.deniz.vantage.core.EventPreviews
import blue.deniz.vantage.core.StoredFeedSnapshots
import blue.deniz.vantage.core.EventsLink
import blue.deniz.vantage.core.EventResolver
import blue.deniz.vantage.core.FolioClient
import blue.deniz.vantage.core.CalendarLinkReader
import blue.deniz.vantage.core.MapLinks
import blue.deniz.vantage.core.MapPlace
import blue.deniz.vantage.core.PlaceSearch
import blue.deniz.vantage.core.WeatherForecasts
import blue.deniz.vantage.storage.EventStore
import blue.deniz.vantage.storage.VantageDatabase
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import okhttp3.Cache
import java.util.IllformedLocaleException
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import androidx.compose.material3.CalendarLocale

@Composable
actual fun SystemBackHandler(enabled: Boolean, handlesEscape: Boolean, onBack: () -> Unit) = key(enabled) { BackHandler(enabled, onBack) }

actual fun handleEscapeKey(): Boolean = false

@Composable
actual fun rememberSystemBack(): () -> Boolean {
    val dispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    return remember(dispatcher) {
        {
            if (dispatcher == null || !dispatcher.hasEnabledCallbacks()) {
                false
            } else {
                dispatcher.onBackPressed()
                true
            }
        }
    }
}

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

@Composable
actual fun rememberSaveTextFile(onSaved: (saved: Boolean) -> Unit): (fileName: String, text: String) -> Unit {
    val context = LocalContext.current
    val currentOnSaved by rememberUpdatedState(onSaved)
    val pendingText = remember { mutableStateOf<String?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        val text = pendingText.value
        pendingText.value = null
        if (uri == null || text == null) return@rememberLauncherForActivityResult
        val saved = try {
            context.contentResolver.openOutputStream(uri)?.use { it.write(text.encodeToByteArray()) } != null
        } catch (_: IOException) {
            false
        }
        currentOnSaved(saved)
    }
    return remember(launcher) {
        { fileName, text ->
            pendingText.value = text
            launcher.launch(fileName)
        }
    }
}

@Composable
actual fun rememberOpenTextFile(onOpened: (text: String) -> Unit): () -> Unit {
    val context = LocalContext.current
    val currentOnOpened by rememberUpdatedState(onOpened)
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val text = try {
            context.contentResolver.openInputStream(uri)?.use { it.readBytes().decodeToString() }
        } catch (_: IOException) {
            null
        }
        currentOnOpened(text.orEmpty())
    }
    return remember(launcher) { { launcher.launch(arrayOf("*/*")) } }
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

    override fun keys(): Set<String> = preferences.all.keys

    override fun write(key: String, value: String?) {
        preferences.edit().apply { if (value == null) remove(key) else putString(key, value) }.apply()
    }
}

private const val HTTP_CACHE_BYTES = 20L * 1024 * 1024

fun createAppState(context: Context, backgroundChecks: BackgroundEventChecks? = null): AppState {
    val database = Room.databaseBuilder<VantageDatabase>(context, context.getDatabasePath("vantage.db").absolutePath)
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()
    val settingsStorage = SharedPreferencesStorage(context.getSharedPreferences("vantage-settings", Context.MODE_PRIVATE))
    val client = HttpClient(OkHttp) {
        engine { config { cache(Cache(File(context.cacheDir, "http"), HTTP_CACHE_BYTES)) } }
    }
    val resolver = EventResolver(EventFetcher(client))
    val previews = EventPreviews(resolver)
    return AppState(
        EventStore(database, resolver, FolioClient(client)),
        EventFeedReader(client, resolver, previews = previews, snapshots = StoredFeedSnapshots(settingsStorage::read, settingsStorage::write, resolver)),
        previews,
        PlaceSearch(client),
        WeatherForecasts(client),
        settingsStorage,
        CalendarLinkReader(client),
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

actual val runsInBrowser = false

actual val runsInAndroidBrowser = false

actual val mondayFirstCalendarLocale: CalendarLocale get() = java.util.Locale.UK
