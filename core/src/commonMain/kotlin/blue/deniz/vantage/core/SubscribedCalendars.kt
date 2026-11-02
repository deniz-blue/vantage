package blue.deniz.vantage.core

import directory.evnt.OpenEvnt
import directory.evnt.convert.ConvertOptions
import directory.evnt.convert.ICalendar
import io.ktor.client.HttpClient
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.datetime.LocalDate
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.Instant

@Serializable
sealed interface SubscribedCalendar {
    @Serializable
    @SerialName("ical")
    data class Link(val url: String) : SubscribedCalendar

    @Serializable
    @SerialName("device")
    data class OnDevice(val calendarId: Long) : SubscribedCalendar

    companion object {
        fun linkFromAddress(address: String): Link? {
            val text = address.trim().replaceFirst(Regex("^webcals?://", RegexOption.IGNORE_CASE), "https://")
            return if (isHttpUrl(text)) Link(text) else null
        }
    }
}

data class CalendarEntry(val uid: String, val event: OpenEvnt)

data class CalendarContents(val name: String?, val entries: List<CalendarEntry>)

data class DeviceCalendar(val id: Long, val name: String, val account: String?, val color: Int?)

interface DeviceCalendars {
    fun hasPermission(): Boolean

    suspend fun list(): List<DeviceCalendar>

    suspend fun read(calendarId: Long, from: Instant, until: Instant, options: ConvertOptions): List<CalendarEntry>
}

class CalendarLinkReader(private val client: HttpClient) {
    suspend fun read(link: SubscribedCalendar.Link, within: ClosedRange<LocalDate>, options: ConvertOptions): CalendarContents {
        val text = client.getOk(link.url).bodyAsText()
        return when {
            ICalendar.looksLikeICalendar(text) -> CalendarContents(
                ICalendar.calendarName(text),
                ICalendar.readEntries(text, within, options).map { CalendarEntry(it.uid, it.event) },
            )
            text.trimStart().startsWith("{") -> CalendarContents(null, readOpenEvntIndex(parseOpenEvntIndexPage(link.url, text)))
            else -> failure(EventError.Kind.UnknownFormat, "${link.url} is not an iCalendar file or an Open Evnt index")
        }
    }

    private suspend fun readOpenEvntIndex(first: OpenEvntIndexPage): List<CalendarEntry> = coroutineScope {
        val items = first.items.toMutableList()
        val visited = mutableSetOf<String>()
        var next = first.next
        while (next != null && visited.add(next) && visited.size < INDEX_PAGES_TO_READ && items.size < INDEX_ITEMS_TO_RESOLVE) {
            val page = parseOpenEvntIndexPage(next, client.getOk(next).bodyAsText())
            items += page.items
            next = page.next
        }
        val fetching = Semaphore(INDEX_EVENTS_RESOLVED_AT_ONCE)
        items.distinctBy { it.url }.take(INDEX_ITEMS_TO_RESOLVE).map { item ->
            async {
                val event = fetching.withPermit { OpenEvnt.parse(client.getOk(item.url).bodyAsText()) }
                CalendarEntry(event.id, event)
            }
        }.awaitAll()
    }
}
