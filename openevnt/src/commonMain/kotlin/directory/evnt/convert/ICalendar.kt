package directory.evnt.convert

import directory.evnt.Component
import directory.evnt.Occurrence
import directory.evnt.OpenEvnt
import directory.evnt.PartialDate
import directory.evnt.Venue
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.plus
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime

private class ContentLine(val name: String, val parameters: Map<String, String>, val value: String)

private class CalendarComponent(val name: String) {
    val properties = mutableListOf<ContentLine>()
    val children = mutableListOf<CalendarComponent>()

    fun first(name: String) = properties.firstOrNull { it.name == name }

    fun all(name: String) = properties.filter { it.name == name }
}

private val FOLDED_LINE_BREAK = Regex("""\r?\n[ \t]""")
private val DATE_VALUE = Regex("""(\d{4})(\d{2})(\d{2})""")
private val DATE_TIME_VALUE = Regex("""(\d{4})(\d{2})(\d{2})T(\d{2})(\d{2})(\d{2})?(Z?)""", RegexOption.IGNORE_CASE)

private fun parseContentLine(line: String): ContentLine? {
    var index = 0
    var quoted = false
    val parts = mutableListOf<String>()
    var start = 0
    while (index < line.length) {
        val char = line[index]
        if (char == '"') quoted = !quoted
        if (!quoted && (char == ';' || char == ':')) {
            parts += line.substring(start, index)
            start = index + 1
            if (char == ':') break
        }
        index++
    }
    if (index >= line.length) return null
    val parameters = parts.drop(1).associate { parameter ->
        parameter.substringBefore('=').uppercase() to parameter.substringAfter('=', "").removeSurrounding("\"")
    }
    return ContentLine(parts.first().uppercase(), parameters, line.substring(start))
}

private fun parseCalendar(text: String): List<CalendarComponent> {
    val roots = mutableListOf<CalendarComponent>()
    val open = ArrayDeque<CalendarComponent>()
    for (line in text.replace(FOLDED_LINE_BREAK, "").lines()) {
        val content = parseContentLine(line) ?: continue
        when (content.name) {
            "BEGIN" -> {
                val component = CalendarComponent(content.value.trim().uppercase())
                (open.lastOrNull()?.children ?: roots) += component
                open.addLast(component)
            }
            "END" -> open.removeLastOrNull()
            else -> open.lastOrNull()?.properties?.add(content)
        }
    }
    return roots
}

private fun unescapeText(value: String) = buildString {
    var index = 0
    while (index < value.length) {
        val char = value[index]
        if (char == '\\' && index + 1 < value.length) {
            val next = value[index + 1]
            append(if (next == 'n' || next == 'N') '\n' else next)
            index += 2
        } else {
            append(char)
            index++
        }
    }
}

private fun ContentLine.text() = unescapeText(value).takeIf { it.isNotBlank() }

private fun ContentLine.dateOrNull(fallbackTimeZoneId: String, exclusiveEnd: Boolean = false): PartialDate? {
    val value = value.trim()
    val zoneOf = { candidate: String? ->
        candidate?.takeIf(PartialDate::isTimeZoneId) ?: fallbackTimeZoneId
    }
    DATE_TIME_VALUE.matchEntire(value)?.destructured?.let { (year, month, day, hour, minute, _, utc) ->
        val zone = if (utc.isNotEmpty()) "UTC" else zoneOf(parameters["TZID"])
        return runCatching { PartialDate.Time(year.toInt(), month.toInt(), day.toInt(), hour.toInt(), minute.toInt(), zone) }.getOrNull()
    }
    DATE_VALUE.matchEntire(value)?.destructured?.let { (year, month, day) ->
        val date = runCatching { LocalDate(year.toInt(), month.toInt(), day.toInt()) }.getOrNull() ?: return null
        val lastDay = if (exclusiveEnd) date.minus(1, DateTimeUnit.DAY) else date
        return PartialDate.Day(lastDay, zoneOf(parameters["TZID"]))
    }
    return null
}

private fun CalendarComponent.status(): OpenEvnt.Status? = when (first("STATUS")?.value?.trim()?.uppercase()) {
    "TENTATIVE" -> OpenEvnt.Status.Uncertain
    "CANCELLED" -> OpenEvnt.Status.Cancelled
    else -> null
}

data class ICalendarEntry(val uid: String, val event: OpenEvnt)

private fun CalendarComponent.recurrenceDateOrNull(fallbackTimeZoneId: String) =
    first("RECURRENCE-ID")?.dateOrNull(fallbackTimeZoneId)?.let(::localDateOf)

private fun localDateOf(date: PartialDate): LocalDate? = when (date) {
    is PartialDate.Day -> date.toLocalDate()
    is PartialDate.Time -> date.toLocalDate()
    else -> null
}

private fun lastRepeatDate(until: PartialDate, start: PartialDate?): LocalDate? {
    if (until !is PartialDate.Time || start !is PartialDate.Time) return localDateOf(until)
    val local = until.startInstant.toLocalDateTime(start.timeZone)
    return if (local.time < LocalTime(start.hour, start.minute)) local.date.minus(1, DateTimeUnit.DAY) else local.date
}

private fun Occurrence.lastDay(): LocalDate? = (end ?: start)?.let(::localDateOf)

private fun PartialDate.shiftedByDays(days: Int): PartialDate = when (this) {
    is PartialDate.Day -> PartialDate.Day(toLocalDate().plus(days, DateTimeUnit.DAY), timeZoneId)
    is PartialDate.Time -> PartialDate.Time(LocalDateTime(toLocalDate().plus(days, DateTimeUnit.DAY), LocalTime(hour, minute)), timeZoneId)
    else -> this
}

private fun CalendarComponent.occurrence(fallbackTimeZoneId: String, eventStatus: OpenEvnt.Status?): Occurrence? {
    val start = first("DTSTART")?.dateOrNull(fallbackTimeZoneId) ?: return null
    val end = first("DTEND")?.dateOrNull(fallbackTimeZoneId, exclusiveEnd = true)
    val cancelledAlone = status() == OpenEvnt.Status.Cancelled && eventStatus != OpenEvnt.Status.Cancelled
    return Occurrence(start = start, end = end, status = if (cancelledAlone) Occurrence.Status.Cancelled else null)
}

private fun CalendarComponent.excludedDates(fallbackTimeZoneId: String): Set<LocalDate> =
    all("EXDATE").flatMap { line ->
        line.value.split(',').mapNotNull { value -> ContentLine(line.name, line.parameters, value).dateOrNull(fallbackTimeZoneId)?.let(::localDateOf) }
    }.toSet()

private fun CalendarComponent.toOpenEvnt(id: String, occurrences: List<Occurrence>, options: ConvertOptions): OpenEvnt {
    val language = options.language
    val components = buildList<Component> {
        first("DESCRIPTION")?.text()?.let { add(plainText(it, language)) }
        for (url in all("URL")) url.value.trim().takeIf { it.isNotEmpty() }?.let { add(linkTo(it, null, language)) }
    }
    return OpenEvnt(
        id = id,
        name = textIn(language, first("SUMMARY")?.text()) ?: mapOf(language to "Untitled Event"),
        status = status(),
        venue = first("LOCATION")?.text()?.let { Venue(name = mapOf(language to it)) },
        occurrences = occurrences.orNullIfEmpty(),
        components = components.orNullIfEmpty(),
    )
}

object ICalendar {
    fun looksLikeICalendar(text: String) = "BEGIN:VCALENDAR" in text && "END:VCALENDAR" in text

    fun calendarName(text: String): String? =
        parseCalendar(text).firstOrNull { it.name == "VCALENDAR" }?.first("X-WR-CALNAME")?.text()

    fun readEntries(text: String, within: ClosedRange<LocalDate>, options: ConvertOptions = ConvertOptions()): List<ICalendarEntry> {
        val zone = options.timeZoneId
        val events = parseCalendar(text)
            .flatMap { if (it.name == "VCALENDAR") it.children else listOf(it) }
            .filter { it.name == "VEVENT" }
        return events.withIndex()
            .groupBy({ (index, event) -> event.first("UID")?.value?.trim()?.takeIf { it.isNotEmpty() } ?: "event-$index" }, { it.value })
            .mapNotNull { (uid, versions) ->
                val main = versions.firstOrNull { it.first("RECURRENCE-ID") == null } ?: versions.first()
                val status = main.status()
                val first = main.occurrence(zone, status) ?: return@mapNotNull null
                val overrides = versions.filter { it !== main }.mapNotNull { override -> override.recurrenceDateOrNull(zone)?.let { it to override } }.toMap()
                val rule = main.first("RRULE")?.value?.let { value ->
                    RecurrenceRule.parse(value) { until -> ContentLine("UNTIL", emptyMap(), until).dateOrNull(zone)?.let { lastRepeatDate(it, first.start) } }
                }
                val firstDate = first.start?.let(::localDateOf)
                val occurrences = if (rule == null || firstDate == null) {
                    sequenceOf(first)
                } else {
                    val excluded = main.excludedDates(zone)
                    rule.datesFrom(firstDate)
                        .takeWhile { it <= within.endInclusive }
                        .filter { it !in excluded }
                        .map { date ->
                            overrides[date]?.occurrence(zone, status) ?: run {
                                val days = (date.toEpochDays() - firstDate.toEpochDays()).toInt()
                                first.copy(start = first.start?.shiftedByDays(days), end = first.end?.shiftedByDays(days))
                            }
                        }
                }.filter { occurrence -> occurrence.lastDay()?.let { it >= within.start } ?: false }.toList()
                if (occurrences.isEmpty()) return@mapNotNull null
                ICalendarEntry(uid, main.toOpenEvnt(uid, occurrences, options))
            }
    }

    fun read(text: String, options: ConvertOptions = ConvertOptions()): OpenEvnt {
        val language = options.language
        val events = parseCalendar(text)
            .flatMap { if (it.name == "VCALENDAR") it.children else listOf(it) }
            .filter { it.name == "VEVENT" }
        val first = events.firstOrNull() ?: throw IllegalArgumentException("No VEVENT found in calendar input")
        val status = first.status()

        val location = events.firstNotNullOfOrNull { event -> event.all("LOCATION").firstNotNullOfOrNull { it.text() } }
        val occurrences = events.mapNotNull { event ->
            val start = event.first("DTSTART")?.dateOrNull(options.timeZoneId) ?: return@mapNotNull null
            val end = event.first("DTEND")?.dateOrNull(options.timeZoneId, exclusiveEnd = true)
            val cancelledAlone = event.status() == OpenEvnt.Status.Cancelled && status != OpenEvnt.Status.Cancelled
            Occurrence(start = start, end = end, status = if (cancelledAlone) Occurrence.Status.Cancelled else null)
        }

        val components = buildList<Component> {
            first.first("DESCRIPTION")?.text()?.let { add(plainText(it, language)) }
            for (url in first.all("URL")) url.value.trim().takeIf { it.isNotEmpty() }?.let { add(linkTo(it, null, language)) }
        }

        return OpenEvnt(
            id = options.documentId(),
            name = textIn(language, first.first("SUMMARY")?.text()) ?: mapOf(language to "Untitled Event"),
            status = status,
            venue = location?.let { Venue(name = mapOf(language to it)) },
            occurrences = occurrences.orNullIfEmpty(),
            components = components.orNullIfEmpty(),
        )
    }
}
