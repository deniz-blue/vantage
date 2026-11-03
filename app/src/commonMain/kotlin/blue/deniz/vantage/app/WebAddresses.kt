package blue.deniz.vantage.app

import blue.deniz.vantage.core.EventFeed
import blue.deniz.vantage.core.EventsLink
import directory.evnt.OpenEvnt
import io.ktor.http.Url
import io.ktor.http.encodeURLPathPart
import io.ktor.http.formUrlEncode
import kotlinx.datetime.LocalDate

private val SETTINGS_PAGES = mapOf(
    "appearance" to Route.AppearanceSettings,
    "notifications" to Route.NotificationSettings,
    "backup" to Route.BackupSettings,
    "home" to Route.HomeSettings,
    "tags" to Route.TagSettings,
    "calendars" to Route.SystemCalendarSettings,
    "following" to Route.FollowingSettings,
    "about" to Route.About,
    "developer-tools" to Route.DeveloperTools,
)

val Route.webAddress: String?
    get() = when (this) {
        Route.Home -> "/"
        Route.MyEvents -> "/events"
        Route.Settings -> "/settings"
        is Route.EventDetails -> "/event/${id.encodeURLPathPart()}"
        is Route.EditEvent -> "/event/${id.encodeURLPathPart()}/edit"
        is Route.EditFromLink -> with(request) {
            withQuery("/edit", "url" to event.url, "at" to event.at, "data" to event.data, "return" to returnUrl, "state" to state)
        }
        is Route.EventFromLink -> withQuery("/event", "url" to intent.url, "at" to intent.at, "data" to intent.data)
        is Route.CreateEvent -> withQuery("/new", "text" to sharedText, "day" to day?.toString())
        Route.Setup -> "/setup"
        Route.AppearanceSettings, Route.NotificationSettings, Route.BackupSettings,
        Route.HomeSettings, Route.TagSettings, Route.SystemCalendarSettings, Route.FollowingSettings, Route.About, Route.DeveloperTools -> "/settings/" + SETTINGS_PAGES.entries.first { it.value == this }.key
        is Route.Feed -> when (val feed = feed) {
            is EventFeed.OpenEvntIndex -> withQuery("/feed", "type" to "index", "url" to feed.url)
            is EventFeed.AtRepository -> withQuery("/feed", "type" to "at", "repo" to feed.repo, "collection" to feed.collection)
        }
    }

private fun withQuery(path: String, vararg parameters: Pair<String, String?>): String {
    val given = parameters.filter { it.second != null }
    return if (given.isEmpty()) path else path + "?" + given.formUrlEncode()
}

fun routeAtWebAddress(url: Url): Route? {
    EventsLink.parseEditRequest(url)?.let { return Route.EditFromLink(it) }
    EventsLink.parseIntent(url)?.let { return Route.EventFromLink(it) }
    EventsLink.parseIndexLink(url)?.let { return Route.Feed(it) }
    val segments = url.segments
    return when (segments.size) {
        0 -> Route.Home
        1 -> when (segments[0]) {
            "events", "list", "calendar" -> Route.MyEvents
            "settings" -> Route.Settings
            "setup" -> Route.Setup
            "new" -> Route.CreateEvent(url.parameters["text"], url.parameters["day"]?.let(::localDateOrNull))
            "feed" -> feedAtWebAddress(url)?.let(Route::Feed)
            else -> null
        }
        2 -> when (segments[0]) {
            "event" -> Route.EventDetails(segments[1])
            "settings" -> SETTINGS_PAGES[segments[1]]
            else -> null
        }
        3 -> Route.EditEvent(segments[1]).takeIf { segments[0] == "event" && segments[2] == "edit" }
        else -> null
    }
}

private fun localDateOrNull(text: String) = try {
    LocalDate.parse(text)
} catch (_: IllegalArgumentException) {
    null
}

private fun feedAtWebAddress(url: Url): EventFeed? {
    val parameters = url.parameters
    return when (parameters["type"]) {
        "at" -> parameters["repo"]?.let { EventFeed.AtRepository(it, parameters["collection"] ?: OpenEvnt.AT_RECORD_TYPE) }
        else -> null
    }
}
