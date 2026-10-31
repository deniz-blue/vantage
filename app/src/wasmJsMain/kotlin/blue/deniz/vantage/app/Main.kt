package blue.deniz.vantage.app

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import androidx.room3.Room
import androidx.sqlite.driver.web.WebWorkerSQLiteDriver
import blue.deniz.vantage.core.EventFeedReader
import blue.deniz.vantage.core.EventFetcher
import blue.deniz.vantage.core.EventPreviews
import blue.deniz.vantage.core.EventResolver
import blue.deniz.vantage.core.FolioClient
import blue.deniz.vantage.core.ICalendarLinkReader
import blue.deniz.vantage.core.PlaceSearch
import blue.deniz.vantage.core.WeatherForecasts
import blue.deniz.vantage.storage.EventStore
import blue.deniz.vantage.storage.VantageDatabase
import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.Worker

private fun createSqliteWorker(): Worker =
    js("""new Worker(new URL("./sqlite-worker.js", import.meta.url), { type: "module" })""")

// The page starts with <body> focused, and Compose removes its text-input element while it has
// focus, which hands focus back to <body>. Compose only listens for keys on its own canvas.
private fun keepKeyboardFocusOnCompose() {
    js("""
        const focusCanvas = () => {
            if (document.activeElement !== document.body) return;
            for (const host of document.body.querySelectorAll("*")) {
                const canvas = host.shadowRoot && host.shadowRoot.querySelector("canvas");
                if (canvas) return canvas.focus({ preventScroll: true });
            }
        };
        document.addEventListener("focusout", () => setTimeout(focusCanvas));
        setTimeout(focusCanvas);
    """)
}

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    val database = Room.databaseBuilder<VantageDatabase>("vantage.db")
        .setDriver(WebWorkerSQLiteDriver(createSqliteWorker()))
        .build()
    val client = browserHttpClient()
    val resolver = EventResolver(EventFetcher(client))
    val previews = EventPreviews(resolver)
    val state = AppState(
        EventStore(database, resolver, FolioClient(client)),
        EventFeedReader(client, resolver, previews = previews),
        previews,
        PlaceSearch(client),
        WeatherForecasts(client),
        BrowserLocalStorage,
        ICalendarLinkReader(client),
        deviceCalendars = null,
    )
    state.openLink(window.location.href)
    keepKeyboardFocusOnCompose()
    document.addEventListener("visibilitychange", { state.readClock() })
    ComposeViewport(document.body!!) {
        LaunchedEffect(state.navigator) {
            snapshotFlow { state.navigator.entries.first().webAddress to state.navigator.current.webAddress }
                .collect { (base, top) -> BrowserBackDispatcher.showAddresses(base, top) }
        }
        VantageApp(state)
    }
}
