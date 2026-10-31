package blue.deniz.vantage.core

import directory.evnt.Occurrence
import directory.evnt.PartialDate
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.daysUntil
import kotlinx.datetime.plus

private const val LAST_HOUR = 23
private const val LAST_MINUTE = 59

fun PartialDate.withTimeZone(timeZoneId: String): PartialDate = when (this) {
    is PartialDate.Year -> copy(timeZoneId = timeZoneId)
    is PartialDate.Month -> copy(timeZoneId = timeZoneId)
    is PartialDate.Day -> copy(timeZoneId = timeZoneId)
    is PartialDate.Time -> copy(timeZoneId = timeZoneId)
}

fun PartialDate.localDateOrNull(): LocalDate? = when (this) {
    is PartialDate.Day -> toLocalDate()
    is PartialDate.Time -> toLocalDate()
    else -> null
}

fun PartialDate.withLocalDate(date: LocalDate): PartialDate = when (this) {
    is PartialDate.Time -> PartialDate.Day(date, timeZoneId).toTime(hour, minute)
    else -> PartialDate.Day(date, timeZoneId)
}

fun PartialDate.withTimeOfDay(hour: Int, minute: Int): PartialDate.Time =
    (withPrecision(PartialDate.Precision.Day) as PartialDate.Day).toTime(hour, minute)

fun PartialDate.withoutTimeOfDay(): PartialDate = if (this is PartialDate.Time) toDay() else this

fun PartialDate.Time.endAtTimeOfDay(hour: Int, minute: Int): PartialDate.Time {
    val date = if (LocalTime(hour, minute) > LocalTime(this.hour, this.minute)) toLocalDate() else toLocalDate().plus(DatePeriod(days = 1))
    return PartialDate.Day(date, timeZoneId).toTime(hour, minute)
}

fun PartialDate.movedWithStart(oldStart: PartialDate?, newStart: PartialDate): PartialDate {
    val oldDate = oldStart?.localDateOrNull() ?: return this
    val newDate = newStart.localDateOrNull() ?: return this
    val date = localDateOrNull() ?: return this
    return withLocalDate(date.plus(DatePeriod(days = oldDate.daysUntil(newDate))))
}

fun Occurrence.withStart(newStart: PartialDate): Occurrence {
    val movedEnd = end?.movedWithStart(start, newStart) ?: return copy(start = newStart)
    return copy(start = newStart, end = movedEnd.takeUnless { it.endInstant <= newStart.startInstant })
}

fun Occurrence.withEndTimeOfDay(hour: Int, minute: Int): Occurrence {
    val end = end ?: return this
    val start = start as? PartialDate.Time ?: return copy(end = end.withTimeOfDay(hour, minute))
    val endFollowsStart = end.localDateOrNull() == start.toLocalDate() || (end is PartialDate.Time && start.endAtTimeOfDay(end.hour, end.minute) == end)
    return copy(end = if (endFollowsStart) start.endAtTimeOfDay(hour, minute) else end.withTimeOfDay(hour, minute))
}

fun Occurrence.withDatePrecision(precision: PartialDate.Precision): Occurrence {
    fun PartialDate.converted(fill: PartialDate.Fill) =
        if (precision == PartialDate.Precision.Day && this is PartialDate.Time) this else withPrecision(precision, fill)
    return copy(start = start?.converted(PartialDate.Fill.First), end = end?.converted(PartialDate.Fill.Last))
}

fun Occurrence.withDefaultEnd(): Occurrence {
    val start = start ?: return this
    val end = when {
        start !is PartialDate.Time -> start
        start.hour < LAST_HOUR -> start.withTimeOfDay(start.hour + 1, start.minute)
        else -> start.withTimeOfDay(LAST_HOUR, LAST_MINUTE)
    }
    return copy(end = end)
}
