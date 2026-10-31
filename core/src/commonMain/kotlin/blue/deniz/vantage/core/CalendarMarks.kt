package blue.deniz.vantage.core

import directory.evnt.PartialDate
import kotlinx.serialization.Serializable
import kotlin.time.Instant

@Serializable
data class CalendarMarker(val id: String, val label: String, val at: PartialDate) {
    init {
        require(at.isDayOrTime()) { "A marker is on a day or at a time, not $at" }
    }

    val instant: Instant get() = at.startInstant
}

@Serializable
data class CalendarPeriod(val id: String, val label: String, val start: PartialDate, val end: PartialDate) {
    init {
        require(start.isDayOrTime() && end.isDayOrTime()) { "A period starts and ends on a day or at a time" }
        require(end.endInstant > start.startInstant) { "A period ends after it starts" }
    }

    val startsAt: Instant get() = start.startInstant

    val endsAt: Instant get() = end.endInstant

    operator fun contains(instant: Instant) = instant >= startsAt && instant < endsAt
}

fun PartialDate.isDayOrTime() = this is PartialDate.Day || this is PartialDate.Time

data class CalendarMarks(val markers: List<CalendarMarker>, val periods: List<CalendarPeriod>) {
    fun markersBetween(from: Instant?, until: Instant?): List<CalendarMarker> =
        markers.filter { (from == null || it.instant >= from) && (until == null || it.instant < until) }

    fun periodsAt(instant: Instant): List<CalendarPeriod> = periods.filter { instant in it }

    fun periodsOn(start: PartialDate, end: PartialDate? = null): List<CalendarPeriod> = when {
        end != null -> periodsOverlapping(start.startInstant, end.endInstant)
        start is PartialDate.Time -> periodsAt(start.startInstant).sortedBy { it.startsAt }
        else -> periodsOverlapping(start.startInstant, start.endInstant)
    }

    fun periodsOverlapping(from: Instant?, until: Instant?): List<CalendarPeriod> =
        periods.filter { (from == null || it.endsAt > from) && (until == null || it.startsAt < until) }.sortedBy { it.startsAt }

    companion object {
        val None = CalendarMarks(emptyList(), emptyList())
    }
}
