package directory.evnt

import directory.evnt.PartialDate.Precision
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

data class DateGroup(val dates: Dates, val times: List<TimeSlot>) {
    sealed interface Dates {
        data class Single(val date: PartialDate) : Dates

        data class Range(val from: PartialDate, val to: PartialDate) : Dates

        data class Days(val days: List<PartialDate>) : Dates
    }
}

data class TimeSlot(val start: PartialDate.Time?, val end: PartialDate.Time?)

private data class TimeOfDayPair(val start: LocalTime?, val end: LocalTime?)

private val Occurrence.timesOfDay
    get() = TimeOfDayPair(
        (start as? PartialDate.Time)?.let { LocalTime(it.hour, it.minute) },
        (end as? PartialDate.Time)?.let { LocalTime(it.hour, it.minute) },
    )

private fun PartialDate.epochDay(): Long? = when (this) {
    is PartialDate.Day -> LocalDate(year, month, day).toEpochDays()
    is PartialDate.Time -> LocalDate(year, month, day).toEpochDays()
    else -> null
}

private fun isSameDay(one: PartialDate?, other: PartialDate?) =
    one?.epochDay() != null && one.epochDay() == other?.epochDay()

private fun isNextDay(one: PartialDate, other: PartialDate): Boolean {
    val first = one.epochDay() ?: return false
    val second = other.epochDay() ?: return false
    return second - first == 1L
}

private fun PartialDate.roundedToDay() = if (has(Precision.Day)) withPrecision(Precision.Day) else this

private fun datesOf(occurrences: List<Occurrence>): DateGroup.Dates {
    val only = occurrences.singleOrNull()
    if (only != null) {
        val start = only.start
        val end = only.end
        return if (start != null && end != null && !isSameDay(start, end)) DateGroup.Dates.Range(start, end)
        else DateGroup.Dates.Single(start ?: end!!)
    }
    val days = occurrences.mapNotNull { occurrence -> occurrence.start?.takeIf { it.has(Precision.Day) }?.roundedToDay() }
    val consecutive = days.zipWithNext().all { (one, other) -> isNextDay(one, other) }
    return if (days.size >= 2 && consecutive) DateGroup.Dates.Range(days.first(), days.last()) else DateGroup.Dates.Days(days)
}

private fun timeSlotsOf(occurrences: List<Occurrence>) = occurrences
    .distinctBy { it.timesOfDay }
    .map { TimeSlot(it.start as? PartialDate.Time, it.end as? PartialDate.Time) }
    .filter { it.start != null || it.end != null }

fun List<Occurrence>.sortedByStart(): List<Occurrence> = sortedWith(compareBy(nullsFirst(Comparator(PartialDate::compare))) { it.start })

fun groupDates(occurrences: List<Occurrence>, groupConsecutiveDates: Boolean = true): List<DateGroup> {
    val dated = occurrences
        .filter { it.start != null || it.end != null }
        .sortedByStart()
    val groups = if (!groupConsecutiveDates) dated.map(::listOf) else buildList<MutableList<Occurrence>> {
        for (occurrence in dated) {
            val last = lastOrNull()
            if (last != null && last.first().timesOfDay == occurrence.timesOfDay) last += occurrence
            else add(mutableListOf(occurrence))
        }
    }
    return groups.map { DateGroup(datesOf(it), timeSlotsOf(it)) }
}
