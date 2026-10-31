package blue.deniz.vantage.app

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import blue.deniz.vantage.app.ui.MapCamera
import blue.deniz.vantage.app.ui.TypographyStyle
import blue.deniz.vantage.core.SubscribedCalendar
import kotlinx.datetime.IllegalTimeZoneException
import kotlinx.datetime.TimeZone
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

const val DEFAULT_CONTENT_LANGUAGE = "en"
private const val CONTENT_LANGUAGE_KEY = "contentLanguage"
private const val TIME_ZONE_KEY = "timeZone"
private const val HOME_TILES_KEY = "homeTiles"
private const val SUBSCRIBED_CALENDARS_KEY = "subscribedCalendars"
private const val TEST_EVENTS_SHOWN_KEY = "testEventsShown"
private const val NOTIFIES_ABOUT_CHANGES_KEY = "notifiesAboutChanges"
private const val COPIES_EVENTS_TO_PHONE_CALENDAR_KEY = "copiesEventsToPhoneCalendar"
private const val THEME_ACCENT_KEY = "themeAccent"
private const val MAP_START_KEY = "mapStart"
private const val HOME_COUNTRY_KEY = "homeCountry"
private const val EVENTS_VIEW_KEY_PREFIX = "eventsView."
private const val FONT_SIZE_KEY_PREFIX = "fontSize."
private val subscribedCalendarsJson = ListSerializer(SubscribedCalendar.serializer())

enum class ThemeAccent(val storedName: String) {
    Purple("purple"),
    Pink("pink"),
    Blue("blue"),
    Green("green"),
    Orange("orange"),
    Wallpaper("wallpaper"),
}

interface KeyValueStorage {
    fun read(key: String): String?
    fun write(key: String, value: String?)
}

@Stable
class AppSettings(private val storage: KeyValueStorage, val detectedTimeZone: TimeZone) {
    var contentLanguage by mutableStateOf(storage.read(CONTENT_LANGUAGE_KEY) ?: DEFAULT_CONTENT_LANGUAGE)
        private set
    var chosenTimeZone by mutableStateOf(storage.read(TIME_ZONE_KEY)?.let(::timeZoneOrNull))
        private set
    val timeZone get() = chosenTimeZone ?: detectedTimeZone
    var homeTiles by mutableStateOf(storage.read(HOME_TILES_KEY)?.let(::homeTilesOfJson) ?: DEFAULT_HOME_TILES)
        private set
    var subscribedCalendars by mutableStateOf(storage.read(SUBSCRIBED_CALENDARS_KEY)?.let(::subscribedCalendarsOfJson).orEmpty())
        private set
    var testEventsShown by mutableStateOf(storage.read(TEST_EVENTS_SHOWN_KEY) == "true")
        private set
    var notifiesAboutChanges by mutableStateOf(storage.read(NOTIFIES_ABOUT_CHANGES_KEY) == "true")
        private set
    var copiesEventsToPhoneCalendar by mutableStateOf(storage.read(COPIES_EVENTS_TO_PHONE_CALENDAR_KEY) == "true")
        private set
    var themeAccent by mutableStateOf(ThemeAccent.entries.find { it.storedName == storage.read(THEME_ACCENT_KEY) } ?: ThemeAccent.Purple)
        private set
    var mapStart by mutableStateOf(storage.read(MAP_START_KEY)?.let(MapCamera::fromStoredText))
        private set
    var homeCountry by mutableStateOf(storage.read(HOME_COUNTRY_KEY))
        private set
    var fontSizes by mutableStateOf(
        TypographyStyle.entries.mapNotNull { style -> storage.read(FONT_SIZE_KEY_PREFIX + style.name)?.toIntOrNull()?.let { style to it } }.toMap(),
    )
        private set
    private val eventsViews = mutableStateMapOf<String, EventsView>()

    fun eventsViewOf(screen: String, default: EventsView): EventsView =
        eventsViews[screen] ?: EventsView.entries.find { it.storedName == storage.read(EVENTS_VIEW_KEY_PREFIX + screen) } ?: default

    fun changeEventsView(screen: String, view: EventsView) {
        eventsViews[screen] = view
        storage.write(EVENTS_VIEW_KEY_PREFIX + screen, view.storedName)
    }

    fun changeFontSize(style: TypographyStyle, size: Int?) {
        fontSizes = if (size == null) fontSizes - style else fontSizes + (style to size)
        storage.write(FONT_SIZE_KEY_PREFIX + style.name, size?.toString())
    }

    fun changeMapStart(camera: MapCamera?) {
        mapStart = camera
        storage.write(MAP_START_KEY, camera?.storedText)
    }

    fun changeHomeCountry(code: String?) {
        homeCountry = code
        storage.write(HOME_COUNTRY_KEY, code)
    }

    fun changeThemeAccent(accent: ThemeAccent) {
        themeAccent = accent
        storage.write(THEME_ACCENT_KEY, accent.storedName.takeIf { accent != ThemeAccent.Purple })
    }

    fun changeContentLanguage(code: String) {
        contentLanguage = code
        storage.write(CONTENT_LANGUAGE_KEY, code.takeIf { it != DEFAULT_CONTENT_LANGUAGE })
    }

    fun changeTimeZone(zone: TimeZone) {
        chosenTimeZone = zone.takeIf { it != detectedTimeZone }
        storage.write(TIME_ZONE_KEY, chosenTimeZone?.id)
    }

    fun changeHomeTiles(tiles: List<HomeTile>) {
        homeTiles = tiles
        storage.write(HOME_TILES_KEY, homeTilesToJson(tiles))
    }

    fun changeSubscribedCalendars(calendars: List<SubscribedCalendar>) {
        subscribedCalendars = calendars
        storage.write(SUBSCRIBED_CALENDARS_KEY, Json.encodeToString(subscribedCalendarsJson, calendars).takeIf { calendars.isNotEmpty() })
    }

    fun changeTestEventsShown(shown: Boolean) {
        testEventsShown = shown
        storage.write(TEST_EVENTS_SHOWN_KEY, "true".takeIf { shown })
    }

    fun changeNotifiesAboutChanges(notifies: Boolean) {
        notifiesAboutChanges = notifies
        storage.write(NOTIFIES_ABOUT_CHANGES_KEY, "true".takeIf { notifies })
    }

    fun changeCopiesEventsToPhoneCalendar(copies: Boolean) {
        copiesEventsToPhoneCalendar = copies
        storage.write(COPIES_EVENTS_TO_PHONE_CALENDAR_KEY, "true".takeIf { copies })
    }
}

private fun timeZoneOrNull(id: String) = try {
    TimeZone.of(id)
} catch (_: IllegalTimeZoneException) {
    null
}

private fun subscribedCalendarsOfJson(text: String) = try {
    Json.decodeFromString(subscribedCalendarsJson, text)
} catch (_: SerializationException) {
    null
} catch (_: IllegalArgumentException) {
    null
}
