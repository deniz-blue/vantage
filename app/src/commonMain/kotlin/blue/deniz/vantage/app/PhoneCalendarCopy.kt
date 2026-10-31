package blue.deniz.vantage.app

import blue.deniz.vantage.core.CalendarMarks
import blue.deniz.vantage.core.isCancelledOn
import blue.deniz.vantage.app.ui.UNTITLED_EVENT
import blue.deniz.vantage.app.ui.displayName
import blue.deniz.vantage.app.ui.translatedName
import blue.deniz.vantage.core.EventSource
import blue.deniz.vantage.core.EventsLink
import blue.deniz.vantage.core.ResolvedEvent
import blue.deniz.vantage.core.SubscribedCalendar
import directory.evnt.PartialDate
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

data class PhoneCalendarEntry(
    val key: String,
    val title: String,
    val start: Instant,
    val end: Instant,
    val allDay: Boolean,
    val timeZoneId: String,
    val location: String?,
    val description: String?,
    val cancelled: Boolean,
)

interface PhoneCalendarCopy {
    fun hasPermission(): Boolean

    suspend fun replaceEntries(entries: List<PhoneCalendarEntry>)

    suspend fun delete()
}

fun List<ResolvedEvent>.toPhoneCalendarEntries(preferredLanguages: List<String>): List<PhoneCalendarEntry> =
    filterNot { it.cameFromPhoneCalendar() }
        .flatMap { it.toPhoneCalendarEntries(preferredLanguages) }
        .distinctBy { it.key }

private fun ResolvedEvent.cameFromPhoneCalendar() = (source as? EventSource.Calendar)?.calendar is SubscribedCalendar.OnDevice

private fun ResolvedEvent.toPhoneCalendarEntries(preferredLanguages: List<String>): List<PhoneCalendarEntry> {
    val id = id ?: return emptyList()
    val event = data ?: return emptyList()
    val title = event.translatedName(preferredLanguages) ?: UNTITLED_EVENT
    val location = event.venue?.let { venue -> listOfNotNull(venue.displayName(preferredLanguages), venue.address).distinct().joinToString(", ") }?.ifBlank { null }
    val link = EventsLink.shareLinkTo(source)
    return event.occurrences.orEmpty().mapNotNull { occurrence ->
        val start = occurrence.start ?: return@mapNotNull null
        phoneCalendarEntry("$id $start", title, start, occurrence.end, location, link, cancelled = event.isCancelledOn(occurrence))
    }
}

fun CalendarMarks.toPhoneCalendarEntries(): List<PhoneCalendarEntry> =
    markers.mapNotNull { phoneCalendarEntry("marker ${it.id}", it.label, it.at, end = null) } +
        periods.mapNotNull { phoneCalendarEntry("period ${it.id}", it.label, it.start, it.end) }

private fun phoneCalendarEntry(
    key: String,
    title: String,
    start: PartialDate,
    end: PartialDate?,
    location: String? = null,
    description: String? = null,
    cancelled: Boolean = false,
): PhoneCalendarEntry? = when (start) {
    is PartialDate.Day -> {
        val lastDay = end?.lastDay()?.takeIf { it > start.toLocalDate() } ?: start.toLocalDate()
        PhoneCalendarEntry(
            key = key,
            title = title,
            start = start.toLocalDate().atStartOfDayIn(TimeZone.UTC),
            end = lastDay.plus(1, DateTimeUnit.DAY).atStartOfDayIn(TimeZone.UTC),
            allDay = true,
            timeZoneId = TimeZone.UTC.id,
            location = location,
            description = description,
            cancelled = cancelled,
        )
    }
    is PartialDate.Time -> PhoneCalendarEntry(
        key = key,
        title = title,
        start = start.startInstant,
        end = end?.lastInstant()?.coerceAtLeast(start.startInstant) ?: start.startInstant,
        allDay = false,
        timeZoneId = start.timeZoneId,
        location = location,
        description = description,
        cancelled = cancelled,
    )
    else -> null
}

private fun PartialDate.lastDay() = when (this) {
    is PartialDate.Day -> toLocalDate()
    is PartialDate.Time -> startInstant.toLocalDateTime(timeZone).date
    else -> endInstant.toLocalDateTime(timeZone).date.minus(1, DateTimeUnit.DAY)
}

private fun PartialDate.lastInstant() = when (this) {
    is PartialDate.Time -> startInstant
    else -> endInstant
}
