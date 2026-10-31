package blue.deniz.vantage.core

import directory.evnt.OpenEvnt
import directory.evnt.convert.ConvertOptions
import directory.evnt.convert.ICalendar
import io.ktor.client.HttpClient
import io.ktor.client.statement.bodyAsText
import kotlinx.datetime.LocalDate
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.Instant

@Serializable
sealed interface SubscribedCalendar {
    @Serializable
    @SerialName("ical")
    data class ICalendarLink(val url: String) : SubscribedCalendar

    @Serializable
    @SerialName("device")
    data class OnDevice(val calendarId: Long) : SubscribedCalendar

    companion object {
        fun linkFromAddress(address: String): ICalendarLink? {
            val text = address.trim().replaceFirst(Regex("^webcals?://", RegexOption.IGNORE_CASE), "https://")
            return if (isHttpUrl(text)) ICalendarLink(text) else null
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

class ICalendarLinkReader(private val client: HttpClient) {
    suspend fun read(link: SubscribedCalendar.ICalendarLink, within: ClosedRange<LocalDate>, options: ConvertOptions): CalendarContents {
        val text = client.getOk(link.url).bodyAsText()
        if (!ICalendar.looksLikeICalendar(text)) failure(EventError.Kind.UnknownFormat, "${link.url} is not an iCalendar file")
        return CalendarContents(
            ICalendar.calendarName(text),
            ICalendar.readEntries(text, within, options).map { CalendarEntry(it.uid, it.event) },
        )
    }
}
