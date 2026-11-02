package blue.deniz.vantage.app

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import blue.deniz.vantage.core.EventsLink
import blue.deniz.vantage.core.MapLinks
import blue.deniz.vantage.core.MapPlace
import kotlinx.serialization.json.Json
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.Image
import org.jetbrains.skia.ImageInfo
import androidx.compose.material3.CalendarLocale

private fun shareOrCopy(text: String) {
    js("navigator.share ? navigator.share({ url: text }) : navigator.clipboard.writeText(text)")
}

@Composable
actual fun rememberShareText(): (String) -> Unit = ::shareOrCopy

private fun copyToClipboard(text: String) {
    js("navigator.clipboard.writeText(text)")
}

@Composable
actual fun rememberCopyText(): (String) -> Unit = ::copyToClipboard

private fun downloadText(fileName: String, text: String) {
    js("(() => { const link = document.createElement('a'); link.href = URL.createObjectURL(new Blob([text], { type: 'application/json' })); link.download = fileName; link.click(); setTimeout(() => URL.revokeObjectURL(link.href), 1000) })()")
}

private fun pickTextFile(onOpened: (String) -> Unit) {
    js("(() => { const input = document.createElement('input'); input.type = 'file'; input.accept = '.json,application/json'; input.onchange = () => { const file = input.files[0]; if (file) file.text().then(text => onOpened(text)) }; input.click() })()")
}

@Composable
actual fun rememberSaveTextFile(onSaved: (saved: Boolean) -> Unit): (fileName: String, text: String) -> Unit {
    val currentOnSaved by rememberUpdatedState(onSaved)
    return remember {
        { fileName, text ->
            downloadText(fileName, text)
            currentOnSaved(true)
        }
    }
}

@Composable
actual fun rememberOpenTextFile(onOpened: (text: String) -> Unit): () -> Unit {
    val currentOnOpened by rememberUpdatedState(onOpened)
    return remember { { pickTextFile { currentOnOpened(it) } } }
}

private fun replyToOpener(message: String, returnUrl: String): Boolean =
    js("(() => { if (!window.opener) return false; window.opener.postMessage(JSON.parse(message), new URL(returnUrl).origin); window.close(); return true })()")

private fun goToAddress(url: String) {
    js("window.location.assign(url)")
}

private fun sendEditReply(reply: EventsLink.EditReply) {
    if (!replyToOpener(EventsLink.replyMessageFor(reply), reply.request.returnUrl)) goToAddress(EventsLink.returnLinkFor(reply))
}

@Composable
actual fun rememberSendEditReply(): (EventsLink.EditReply) -> Unit = ::sendEditReply

private fun displayAutonym(code: String): String? =
    js("(() => { try { if (Intl.DisplayNames.supportedLocalesOf([code]).length === 0) return null; const name = new Intl.DisplayNames([code], { type: 'language' }).of(code); return name && name !== code ? name : null } catch (e) { return null } })()")

actual fun languageAutonym(code: String): String? = displayAutonym(code)

private fun displayRegionName(code: String, displayLanguage: String): String =
    js("(() => { try { return new Intl.DisplayNames([displayLanguage], { type: 'region' }).of(code) || code } catch (e) { return code } })()")

actual fun countryName(code: String, displayLanguage: String): String = displayRegionName(code, displayLanguage)

private fun readLocalStorage(key: String): String? = js("localStorage.getItem(key)")

private fun writeLocalStorage(key: String, value: String?) {
    js("value == null ? localStorage.removeItem(key) : localStorage.setItem(key, value)")
}

private fun localStorageKeysJson(): String = js("JSON.stringify(Object.keys(localStorage))")

object BrowserLocalStorage : KeyValueStorage {
    override fun read(key: String) = readLocalStorage(key)
    override fun write(key: String, value: String?) = writeLocalStorage(key, value)
    override fun keys(): Set<String> = Json.decodeFromString<List<String>>(localStorageKeysJson()).toSet()
}

@Composable
actual fun wallpaperColorScheme(dark: Boolean): ColorScheme? = null

actual fun mapsAppLink(place: MapPlace): String = MapLinks.googleMaps(place)

actual fun imageBitmapFromArgb(pixels: IntArray, width: Int, height: Int): ImageBitmap {
    val bytes = ByteArray(pixels.size * 4)
    pixels.forEachIndexed { index, argb ->
        bytes[index * 4] = argb.toByte()
        bytes[index * 4 + 1] = (argb shr 8).toByte()
        bytes[index * 4 + 2] = (argb shr 16).toByte()
        bytes[index * 4 + 3] = (argb shr 24).toByte()
    }
    val info = ImageInfo(width, height, ColorType.BGRA_8888, ColorAlphaType.OPAQUE)
    return Image.makeRaster(info, bytes, width * 4).toComposeImageBitmap()
}

@Composable
actual fun rememberCalendarPermissionRequest(writes: Boolean, onResult: (granted: Boolean) -> Unit): () -> Unit = { onResult(false) }

@Composable
actual fun rememberNotificationPermissionRequest(onResult: (granted: Boolean) -> Unit): () -> Unit = { onResult(false) }

actual val runsInBrowser = true

actual val runsInAndroidBrowser = "Android" in kotlinx.browser.window.navigator.userAgent

actual val mondayFirstCalendarLocale: CalendarLocale get() = androidx.compose.ui.text.intl.Locale("en-GB")
