package blue.deniz.vantage.app.ui

import blue.deniz.vantage.core.statedTimeZoneIfOffsetDiffers
import blue.deniz.vantage.core.timeRange
import blue.deniz.vantage.core.timeZoneShortLabel
import directory.evnt.DateGroup
import directory.evnt.Occurrence
import directory.evnt.OpenEvnt
import directory.evnt.PartialDate
import directory.evnt.TimeSlot
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.format
import kotlinx.datetime.format.DayOfWeekNames
import kotlinx.datetime.format.MonthNames
import kotlinx.datetime.format.Padding
import kotlinx.datetime.format.char
import kotlinx.datetime.toLocalDateTime

data class OccurrenceText(val headline: String, val supporting: String?)

data class DateBlockText(val above: String?, val main: String, val below: String?)

data class DayLabelText(val date: String, val weekday: String)

class EventFormatting(val timeZone: TimeZone, private val today: LocalDate) {
    private val dayInThisYear = LocalDate.Format {
        dayOfWeek(DayOfWeekNames.ENGLISH_ABBREVIATED)
        chars(", ")
        day(Padding.NONE)
        char(' ')
        monthName(MonthNames.ENGLISH_ABBREVIATED)
    }

    private val weekdayAndDay = LocalDate.Format {
        dayOfWeek(DayOfWeekNames.ENGLISH_ABBREVIATED)
        char(' ')
        day(Padding.NONE)
    }

    private val monthAndDay = LocalDate.Format {
        monthName(MonthNames.ENGLISH_ABBREVIATED)
        char(' ')
        day(Padding.NONE)
    }

    private val monthAndDayInOtherYear = LocalDate.Format {
        date(monthAndDay)
        chars(", ")
        year()
    }

    private val dayInOtherYear = LocalDate.Format {
        date(dayInThisYear)
        char(' ')
        year()
    }

    private val longDayInThisYear = LocalDate.Format {
        dayOfWeek(DayOfWeekNames.ENGLISH_FULL)
        char(' ')
        day(Padding.NONE)
        char(' ')
        monthName(MonthNames.ENGLISH_FULL)
    }

    private val longDayInOtherYear = LocalDate.Format {
        date(longDayInThisYear)
        char(' ')
        year()
    }

    fun localDateOf(date: PartialDate.Time): LocalDate = date.startInstant.toLocalDateTime(timeZone).date

    fun formatDay(date: LocalDate): String =
        date.format(if (date.year == today.year) dayInThisYear else dayInOtherYear)

    fun formatDate(date: PartialDate): String = when (date) {
        is PartialDate.Year -> date.year.toString()
        is PartialDate.Month -> "${MonthNames.ENGLISH_FULL.names[date.month - 1]} ${date.year}"
        is PartialDate.Day -> formatDay(date.toLocalDate())
        is PartialDate.Time -> formatDay(localDateOf(date))
    }

    fun formatLongDate(date: PartialDate): String = when (date) {
        is PartialDate.Day -> formatLongDay(date.toLocalDate())
        is PartialDate.Time -> formatLongDay(localDateOf(date))
        else -> formatDate(date)
    }

    private fun formatLongDay(date: LocalDate): String =
        date.format(if (date.year == today.year) longDayInThisYear else longDayInOtherYear)

    fun localDateOrNull(date: PartialDate): LocalDate? = when (date) {
        is PartialDate.Day -> date.toLocalDate()
        is PartialDate.Time -> localDateOf(date)
        else -> null
    }

    private fun nearDayName(date: LocalDate): String? = when (today.daysUntil(date)) {
        0 -> "Today"
        1 -> "Tomorrow"
        else -> null
    }

    fun formatDayRange(first: LocalDate, last: LocalDate): String {
        val lastText = "${last.day} ${MonthNames.ENGLISH_ABBREVIATED.names[last.month.ordinal]}" + if (last.year == today.year) "" else " ${last.year}"
        return when {
            last <= first -> formatDay(first)
            first.month == last.month && first.year == last.year -> "${first.day}–$lastText"
            else -> "${first.day} ${MonthNames.ENGLISH_ABBREVIATED.names[first.month.ordinal]} – $lastText"
        }
    }

    fun formatWeekdayAndDay(date: LocalDate): String = date.format(weekdayAndDay)

    fun formatDayLabel(date: LocalDate): DayLabelText {
        val nearDay = when (today.daysUntil(date)) {
            -1 -> "Yesterday, "
            0 -> "Today, "
            1 -> "Tomorrow, "
            else -> ""
        }
        val day = date.format(if (date.year == today.year) monthAndDay else monthAndDayInOtherYear)
        return DayLabelText(nearDay + day, DayOfWeekNames.ENGLISH_FULL.names[date.dayOfWeek.ordinal])
    }

    fun untilText(occurrence: Occurrence): String? {
        val start = occurrence.start ?: return null
        val end = occurrence.end ?: return null
        if (start is PartialDate.Time && end is PartialDate.Time && end.startInstant - start.startInstant < 1.days) return null
        val first = localDateOrNull(start) ?: return null
        val last = localDateOrNull(end) ?: return null
        return untilText(first, last)
    }

    fun untilText(first: LocalDate, last: LocalDate): String? {
        if (last <= first) return null
        val month = if (last.month == first.month && last.year == first.year) "" else " ${MonthNames.ENGLISH_ABBREVIATED.names[last.month.ordinal]}"
        return "Until ${formatWeekdayAndDay(last)}$month"
    }

    fun formatShortDay(date: PartialDate): String = localDateOrNull(date)?.let { day -> nearDayName(day) ?: formatDay(day) } ?: formatDate(date)

    fun dateBlockText(date: PartialDate): DateBlockText {
        if (date is PartialDate.Month) return DateBlockText(null, MonthNames.ENGLISH_ABBREVIATED.names[date.month - 1], date.year.toString())
        val day = localDateOrNull(date) ?: return DateBlockText(null, formatDate(date), null)
        val weekday = nearDayName(day) ?: DayOfWeekNames.ENGLISH_ABBREVIATED.names[day.dayOfWeek.ordinal]
        return DateBlockText(weekday, day.day.toString(), MonthNames.ENGLISH_ABBREVIATED.names[day.month.ordinal])
    }

    fun dateColumnText(occurrence: Occurrence): DateBlockText {
        val start = occurrence.start ?: return DateBlockText(null, "?", null)
        if (start is PartialDate.Month) return DateBlockText(null, MonthNames.ENGLISH_ABBREVIATED.names[start.month - 1], start.year.toString())
        val day = localDateOrNull(start) ?: return DateBlockText(null, formatDate(start), null)
        val lastDay = occurrence.end?.let(::localDateOrNull)?.takeIf { it > day }
        val until = lastDay?.let { if (it.month == day.month && it.year == day.year) "– ${it.day}" else "– ${it.day} ${MonthNames.ENGLISH_ABBREVIATED.names[it.month.ordinal]}" }
        val otherYear = day.year.takeIf { it != today.year }?.toString()
        return DateBlockText(MonthNames.ENGLISH_ABBREVIATED.names[day.month.ordinal], day.day.toString(), until ?: otherYear)
    }

    fun occurrenceTimeText(occurrence: Occurrence, showsStatedTime: Boolean = true): String? = when (val start = occurrence.start) {
        is PartialDate.Time -> formatTimeSlot(TimeSlot(start, occurrence.end as? PartialDate.Time)).let { if (showsStatedTime) withStatedTime(it, start) else it }
        is PartialDate.Day -> "All Day"
        else -> null
    }

    fun formatStartingSoon(occurrence: Occurrence, now: Instant, status: OpenEvnt.Status?): String? {
        if (occurrence.start !is PartialDate.Time) return null
        if (status == OpenEvnt.Status.Cancelled || status == OpenEvnt.Status.Postponed) return null
        val range = occurrence.timeRange() ?: return null
        return if (now in range) "Now" else formatCountdown(now, range.start)
    }

    fun relativeDay(date: LocalDate): String? = when (val days = today.daysUntil(date)) {
        -1 -> "Yesterday"
        0 -> "Today"
        1 -> "Tomorrow"
        in 2..6 -> "In $days days"
        else -> null
    }

    private fun formatCountdown(now: Instant, start: Instant): String? {
        val minutes = (start - now).inWholeMinutes
        return when {
            minutes < 0 -> null
            minutes < 1 -> "starting now"
            minutes < 60 -> "in $minutes min"
            minutes < 24 * 60 -> "in ${minutes / 60} h"
            else -> null
        }
    }

    fun formatDuration(duration: Duration): String {
        val seconds = duration.inWholeSeconds
        val parts = listOf(seconds / 86_400 to "d", seconds % 86_400 / 3_600 to "h", seconds % 3_600 / 60 to "min", seconds % 60 to "s")
        return parts.filter { (amount, _) -> amount != 0L }.ifEmpty { parts.takeLast(1) }.joinToString(" ") { (amount, unit) -> "$amount $unit" }
    }

    fun formatTimeFromNow(now: Instant, start: Instant, end: Instant?): String = when {
        now < start -> "Starts in ${formatDuration(start - now)}"
        end == null -> "Started ${formatDuration(now - start)} ago"
        now < end -> "Started ${formatDuration(now - start)} ago · ends in ${formatDuration(end - now)}"
        else -> "Ended ${formatDuration(now - end)} ago"
    }

    fun occurrenceText(occurrence: Occurrence): OccurrenceText {
        val start = occurrence.start ?: return OccurrenceText("Date Unknown", null)
        val end = occurrence.end
        val slot = withStatedTime(formatTimeSlot(TimeSlot(start as? PartialDate.Time, end as? PartialDate.Time)), start).ifEmpty { null }
        val startDay = localDateOrNull(start)
        val endDay = end?.let(::localDateOrNull)
        if (end != null && (startDay == null || endDay == null || startDay != endDay)) {
            return OccurrenceText("${formatDate(start)} – ${formatDate(end)}", slot)
        }
        return OccurrenceText(formatLongDate(start), slot ?: "All Day".takeIf { start is PartialDate.Day })
    }

    fun formatTime(time: PartialDate.Time): String = formatClock(time.startInstant.toLocalDateTime(timeZone).time)

    fun formatClock(time: LocalTime) = "${time.hour.toString().padStart(2, '0')}:${time.minute.toString().padStart(2, '0')}"

    fun statedTimeText(start: PartialDate.Time): String? {
        val zone = start.statedTimeZoneIfOffsetDiffers(timeZone) ?: return null
        val there = start.startInstant.toLocalDateTime(zone)
        val otherWeekday = DayOfWeekNames.ENGLISH_ABBREVIATED.names[there.date.dayOfWeek.ordinal].takeIf { there.date != localDateOf(start) }
        return listOfNotNull(otherWeekday, formatClock(there.time), timeZoneShortLabel(zone, start.startInstant)).joinToString(" ")
    }

    private fun withStatedTime(slot: String, start: PartialDate): String {
        val stated = (start as? PartialDate.Time)?.let(::statedTimeText) ?: return slot
        return listOf(slot, "($stated)").filter { it.isNotEmpty() }.joinToString(" ")
    }

    fun formatTimeSlot(slot: TimeSlot): String {
        val start = slot.start?.let(::formatTime)
        val end = slot.end?.let(::formatTime)
        return when {
            start != null && end != null -> "$start – $end"
            start != null -> "from $start"
            end != null -> "until $end"
            else -> ""
        }
    }

    fun formatDates(dates: DateGroup.Dates): List<String> = when (dates) {
        is DateGroup.Dates.Single -> listOf(formatDate(dates.date))
        is DateGroup.Dates.Range -> listOf("${formatDate(dates.from)} – ${formatDate(dates.to)}")
        is DateGroup.Dates.Days -> dates.days.map(::formatDate)
    }
}
