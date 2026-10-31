package blue.deniz.vantage.core

import directory.evnt.Occurrence
import directory.evnt.PartialDate
import directory.evnt.Venue
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.plus

data class EventTextGuess(
    val name: String?,
    val occurrences: List<Occurrence>,
    val venue: Venue?,
    val links: List<String>,
    val description: String?,
)

private const val MAX_NAME_LENGTH = 120
private const val MIN_NAME_LETTERS = 3

private const val ENGLISH_MONTH =
    """(jan(?:uary)?|feb(?:ruary)?|mar(?:ch)?|apr(?:il)?|may|june?|july?|aug(?:ust)?|sept?(?:ember)?|oct(?:ober)?|nov(?:ember)?|dec(?:ember)?)"""
private const val LITHUANIAN_MONTH =
    """(sausio|vasario|kovo|baland[žz]io|gegu[žz][ėe]s|bir[žz]elio|liepos|rugpj[ūu][čc]io|rugs[ėe]jo|spalio|lapkri[čc]io|gruod[žz]io)"""
private const val ORDINAL = """(?:st|nd|rd|th)?"""
private const val RANGE_DASH = """\s*(?:-|–|—)\s*"""

private val YEAR_FIRST_DATE = Regex("""(?<![\w./-])(\d{4})[-./](\d{1,2})[-./](\d{1,2})(?![\w./-])""")
private val LITHUANIAN_DATE = Regex("""(?:(\d{4})\s*m\.?\s*)?$LITHUANIAN_MONTH\s+(\d{1,2})(?:$RANGE_DASH(\d{1,2}))?\s*d\b\.?""", RegexOption.IGNORE_CASE)
private val DAY_MONTH_DATE = Regex(
    """\b(\d{1,2})$ORDINAL(?:$RANGE_DASH(\d{1,2})$ORDINAL)?\s+(?:of\s+)?$ENGLISH_MONTH\b\.?(?:,?\s+(\d{4})\b)?""",
    RegexOption.IGNORE_CASE,
)
private val MONTH_DAY_DATE = Regex(
    """\b$ENGLISH_MONTH\b\.?\s+(\d{1,2})$ORDINAL\b(?:$RANGE_DASH(\d{1,2})$ORDINAL\b)?(?:,?\s+(\d{4})\b)?""",
    RegexOption.IGNORE_CASE,
)
private val NUMERIC_DATE = Regex("""(?<![\w./:-])(\d{1,2})[./](\d{1,2})(?:[./](\d{4}|\d{2}))?(?![\w/:]|\.\d)""")
private val NAMED_DAY = Regex("""\b(today|tonight|tomorrow|monday|tuesday|wednesday|thursday|friday|saturday|sunday)\b""", RegexOption.IGNORE_CASE)
private val LITHUANIAN_NAMED_DAY = Regex(
    """(?<!\p{L})(?:[ŠšSs]iandien|[ŠšSs][įi]vakar|rytoj|poryt|(?:pirm|antr|tre[čc]i|ketvirt|penkt|[ŠšSs]e[šs]t|sekm)adien\p{L}*)(?!\p{L})""",
    RegexOption.IGNORE_CASE,
)
private val LITHUANIAN_WEEKDAY_STEMS = listOf("pirm", "antr", "treci", "ketvirt", "penkt", "sest", "sekm")

private val TWELVE_HOUR_RANGE = Regex(
    """\b(\d{1,2})(?::(\d{2}))?\s*(?:([ap])\.?m\.?)?\s*(?:-|–|—|to|until|till)\s*(\d{1,2})(?::(\d{2}))?\s*([ap])\.?m\b\.?""",
    RegexOption.IGNORE_CASE,
)
private val CLOCK_TIME = Regex("""(?<![\d.:])([01]?\d|2[0-3])[.:]([0-5]\d)(?![\d.:])(?:\s*([ap])\.?m\b\.?)?""", RegexOption.IGNORE_CASE)
private val TWELVE_HOUR_TIME = Regex("""\b(1[0-2]|0?[1-9])\s*([ap])\.?m\b\.?""", RegexOption.IGNORE_CASE)
private val HOUR_ONLY_TIME = Regex("""\b([01]?\d|2[0-3])(?:h\b|\s*val\b\.?)""", RegexOption.IGNORE_CASE)
private val RANGE_JOIN = Regex("""^\s*(?:-|–|—|to|until|till|iki)\s*$""", RegexOption.IGNORE_CASE)

private val PLACE_LINE = Regex("""^\s*(?:(?:📍|📌)\s*|(location|place|venue|where|address|vieta|adresas)\s*[：:]\s*)(.+)$""", RegexOption.IGNORE_CASE)
private val LITHUANIAN_STREET = Regex("""\p{L}[\p{L}.-]*\s+(?:g|pr|al|pl|skg)\.\s*\d+""")
private val PLACE_AFTER_AT = Regex("""(?:^|\s)(?:@|at)\s+(\p{Lu}[^;!?\n]*)$""")
private val URL = Regex("""https?://[^\s<>"]+""")
private val URL_TRAILING_PUNCTUATION = ".,;:!?)]}'\""
private val NAME_EDGE_PUNCTUATION = " \t-–—|,:;·•@"
private val DANGLING_NAME_WORDS = setOf("at", "on", "from", "nuo", "@")
private val WEEKDAY_WORD = Regex("""\b(?:mon|tue|tues|wed|thu|thur|thurs|fri|sat|sun)(?:day|nesday|rsday|urday)?\b\.?""", RegexOption.IGNORE_CASE)

private data class DateMention(val start: LocalDate, val end: LocalDate?, val range: IntRange)

private data class TimeOfDay(val hour: Int, val minute: Int) : Comparable<TimeOfDay> {
    override fun compareTo(other: TimeOfDay) = compareValuesBy(this, other, { it.hour }, { it.minute })
}

private data class TimeMention(val start: TimeOfDay, val end: TimeOfDay?, val range: IntRange)

fun guessEventFromText(text: String, today: LocalDate, timeZoneId: String, language: String): EventTextGuess {
    val lines = text.lines()
    val links = URL.findAll(text).map { it.value.trimEnd { c -> c in URL_TRAILING_PUNCTUATION } }.distinct().toList()
    val occurrences = mutableListOf<Occurrence>()
    var datesWaitingForTime = emptyList<DateMention>()
    var placeLine: Pair<String?, String>? = null
    var nameLineIndex: Int? = null
    var name: String? = null

    fun addDays(dates: List<DateMention>) = dates.forEach { occurrences += occurrenceOf(it, null, timeZoneId) }

    lines.forEachIndexed { index, line ->
        val withoutLinks = line.replace(URL) { " ".repeat(it.value.length) }
        PLACE_LINE.find(withoutLinks)?.let { match ->
            if (placeLine == null) placeLine = match.groups[1]?.value to match.groupValues[2].trim()
            return@forEachIndexed
        }
        val dates = datesIn(withoutLinks, today)
        val times = timesIn(withoutLinks, excluded = dates.map { it.range })
        if (dates.isEmpty() && times.isEmpty() && LITHUANIAN_STREET.containsMatchIn(withoutLinks)) {
            if (placeLine == null) placeLine = null to withoutLinks.trim()
            return@forEachIndexed
        }
        val atPlace = if (dates.isNotEmpty() || times.isNotEmpty()) PLACE_AFTER_AT.find(withoutLinks) else null
        if (placeLine == null && atPlace != null) placeLine = null to atPlace.groupValues[1].trim().trimEnd('.', ',')
        when {
            dates.isNotEmpty() && times.isNotEmpty() -> {
                addDays(datesWaitingForTime)
                datesWaitingForTime = emptyList()
                val paired = if (dates.size == times.size) dates.zip(times) else dates.map { it to times.first() }
                paired.forEach { (date, time) -> occurrences += occurrenceOf(date, time, timeZoneId) }
            }
            dates.isNotEmpty() -> {
                addDays(datesWaitingForTime)
                datesWaitingForTime = dates
            }
            times.isNotEmpty() && datesWaitingForTime.isNotEmpty() -> {
                datesWaitingForTime.forEach { occurrences += occurrenceOf(it, times.first(), timeZoneId) }
                datesWaitingForTime = emptyList()
            }
        }
        if (name == null) {
            val cut = (dates.map { it.range } + times.map { it.range } + listOfNotNull(atPlace?.range)).sortedByDescending { it.first }
            val withoutWhen = cut.fold(withoutLinks) { remaining, range -> remaining.removeRange(range.first, minOf(range.last + 1, remaining.length)) }
            val remaining = cleanedName(if (dates.isEmpty()) withoutWhen else withoutWhen.replace(WEEKDAY_WORD, " "))
            if (remaining.count(Char::isLetter) >= MIN_NAME_LETTERS && remaining.length <= MAX_NAME_LENGTH) {
                name = remaining
                nameLineIndex = index
            }
        }
    }
    addDays(datesWaitingForTime)

    val description = lines.filterIndexed { index, _ -> index != nameLineIndex }.joinToString("\n").trim().ifEmpty { null }
    return EventTextGuess(
        name = name,
        occurrences = occurrences.distinct(),
        venue = placeLine?.let { (label, value) -> venueOf(label, value, language) },
        links = links,
        description = description,
    )
}

private fun cleanedName(text: String): String {
    var name = text.replace(Regex("""\s{2,}"""), " ").trim { it in NAME_EDGE_PUNCTUATION }
    while (true) {
        val last = name.substringAfterLast(' ', "")
        if (last.lowercase() !in DANGLING_NAME_WORDS) return name
        name = name.dropLast(last.length).trim { it in NAME_EDGE_PUNCTUATION }
    }
}

private fun venueOf(label: String?, text: String, language: String): Venue {
    val isAddress = label != null && label.lowercase() in setOf("address", "adresas")
    val name = text.substringBefore(',').trim()
    if (isAddress || LITHUANIAN_STREET.containsMatchIn(name)) return Venue(address = text)
    val address = text.substringAfter(',', "").trim().ifEmpty { null }
    return Venue(name = mapOf(language to name), address = address)
}

private fun occurrenceOf(date: DateMention, time: TimeMention?, timeZoneId: String): Occurrence {
    val startDay = PartialDate.Day(date.start, timeZoneId)
    if (time == null) return Occurrence(start = startDay, end = date.end?.let { PartialDate.Day(it, timeZoneId) })
    val start = startDay.toTime(time.start.hour, time.start.minute)
    val endTime = time.end ?: return Occurrence(start = start)
    val end = date.end?.let { PartialDate.Day(it, timeZoneId).toTime(endTime.hour, endTime.minute) } ?: start.endAtTimeOfDay(endTime.hour, endTime.minute)
    return Occurrence(start = start, end = end)
}

private fun datesIn(line: String, today: LocalDate): List<DateMention> {
    val found = mutableListOf<DateMention>()
    fun overlapsFound(range: IntRange) = found.any { it.range.first <= range.last && range.first <= it.range.last }
    fun add(range: IntRange, start: LocalDate?, end: LocalDate? = null) {
        if (start != null && !overlapsFound(range)) found += DateMention(start, end?.takeIf { it > start }, range)
    }

    YEAR_FIRST_DATE.findAll(line).forEach { m ->
        add(m.range, dateOrNull(m.groupValues[1].toInt(), m.groupValues[2].toInt(), m.groupValues[3].toInt()))
    }
    LITHUANIAN_DATE.findAll(line).forEach { m ->
        val month = lithuanianMonthNumber(m.groupValues[2])
        val start = resolvedDate(m.groupValues[1].toIntOrNull(), month, m.groupValues[3].toInt(), today)
        val end = m.groupValues[4].toIntOrNull()?.let { day -> start?.let { dateOrNull(it.year, month, day) } }
        add(m.range, start, end)
    }
    DAY_MONTH_DATE.findAll(line).forEach { m ->
        val month = englishMonthNumber(m.groupValues[3])
        val year = m.groupValues[4].toIntOrNull()
        val start = resolvedDate(year, month, m.groupValues[1].toInt(), today)
        val end = m.groupValues[2].toIntOrNull()?.let { day -> start?.let { dateOrNull(it.year, month, day) } }
        add(m.range, start, end)
    }
    MONTH_DAY_DATE.findAll(line).forEach { m ->
        val month = englishMonthNumber(m.groupValues[1])
        val year = m.groupValues[4].toIntOrNull()
        val start = resolvedDate(year, month, m.groupValues[2].toInt(), today)
        val end = m.groupValues[3].toIntOrNull()?.let { day -> start?.let { dateOrNull(it.year, month, day) } }
        add(m.range, start, end)
    }
    NUMERIC_DATE.findAll(line).forEach { m ->
        val year = m.groupValues[3].toIntOrNull()?.let { if (it < 100) 2000 + it else it }
        add(m.range, resolvedDate(year, m.groupValues[2].toInt(), m.groupValues[1].toInt(), today))
    }
    if (found.isEmpty()) {
        NAMED_DAY.find(line)?.let { m -> add(m.range, namedDay(m.groupValues[1].lowercase(), today)) }
            ?: LITHUANIAN_NAMED_DAY.find(line)?.let { m -> add(m.range, lithuanianNamedDay(m.value, today)) }
    }
    return joinedDateRanges(line, found.sortedBy { it.range.first })
}

private fun joinedDateRanges(line: String, dates: List<DateMention>): List<DateMention> {
    if (dates.size != 2) return dates
    val (first, second) = dates
    val between = line.substring(first.range.last + 1, second.range.first)
    if (!RANGE_JOIN.matches(between) || second.start <= first.start) return dates
    return listOf(DateMention(first.start, second.end ?: second.start, first.range.first..second.range.last))
}

private fun timesIn(line: String, excluded: List<IntRange>): List<TimeMention> {
    val taken = excluded.toMutableList()
    fun isFree(range: IntRange) = taken.none { it.first <= range.last && range.first <= it.last }
    val ranges = mutableListOf<TimeMention>()
    TWELVE_HOUR_RANGE.findAll(line).filter { isFree(it.range) }.forEach { m ->
        val endIsPm = m.groupValues[6].equals("p", ignoreCase = true)
        val startSuffix = m.groupValues[3].ifEmpty { m.groupValues[6] }
        val end = twelveHourTime(m.groupValues[4].toInt(), m.groupValues[5].toIntOrNull() ?: 0, endIsPm) ?: return@forEach
        var start = twelveHourTime(m.groupValues[1].toInt(), m.groupValues[2].toIntOrNull() ?: 0, startSuffix.equals("p", ignoreCase = true)) ?: return@forEach
        if (m.groupValues[3].isEmpty() && start > end) start = TimeOfDay(start.hour - 12, start.minute)
        ranges += TimeMention(start, end, m.range)
        taken += m.range
    }
    val single = mutableListOf<Pair<TimeOfDay, IntRange>>()
    fun collect(regex: Regex, timeOf: (MatchResult) -> TimeOfDay?) {
        regex.findAll(line).filter { isFree(it.range) }.forEach { m ->
            val time = timeOf(m) ?: return@forEach
            single += time to m.range
            taken += m.range
        }
    }
    collect(CLOCK_TIME) { m ->
        val hour = m.groupValues[1].toInt()
        val minute = m.groupValues[2].toInt()
        when (m.groupValues[3].lowercase()) {
            "" -> TimeOfDay(hour, minute)
            else -> if (hour in 1..12) twelveHourTime(hour, minute, m.groupValues[3].equals("p", ignoreCase = true)) else null
        }
    }
    collect(TWELVE_HOUR_TIME) { m -> twelveHourTime(m.groupValues[1].toInt(), 0, m.groupValues[2].equals("p", ignoreCase = true)) }
    collect(HOUR_ONLY_TIME) { m -> TimeOfDay(m.groupValues[1].toInt(), 0) }
    single.sortBy { it.second.first }
    var index = 0
    while (index < single.size) {
        val (time, range) = single[index]
        val next = single.getOrNull(index + 1)
        if (next != null && RANGE_JOIN.matches(line.substring(range.last + 1, next.second.first))) {
            ranges += TimeMention(time, next.first, range.first..next.second.last)
            index += 2
        } else {
            ranges += TimeMention(time, null, range)
            index++
        }
    }
    return ranges.sortedBy { it.range.first }
}

private fun twelveHourTime(hour: Int, minute: Int, pm: Boolean): TimeOfDay? {
    if (hour !in 1..12 || minute !in 0..59) return null
    val base = hour % 12
    return TimeOfDay(if (pm) base + 12 else base, minute)
}

private fun dateOrNull(year: Int, month: Int, day: Int): LocalDate? =
    try {
        LocalDate(year, month, day)
    } catch (_: IllegalArgumentException) {
        null
    }

private fun resolvedDate(year: Int?, month: Int, day: Int, today: LocalDate): LocalDate? {
    if (year != null) return dateOrNull(year, month, day)
    val thisYear = dateOrNull(today.year, month, day) ?: return dateOrNull(today.year + 1, month, day)
    return if (thisYear < today) dateOrNull(today.year + 1, month, day) else thisYear
}

private fun namedDay(word: String, today: LocalDate): LocalDate? = when (word) {
    "today", "tonight" -> today
    "tomorrow" -> today.plus(DatePeriod(days = 1))
    else -> DayOfWeek.entries.find { it.name.equals(word, ignoreCase = true) }?.let(today::comingOrSame)
}

private fun lithuanianNamedDay(word: String, today: LocalDate): LocalDate? = when (val plain = plainLithuanian(word)) {
    "siandien", "sivakar" -> today
    "rytoj" -> today.plus(DatePeriod(days = 1))
    "poryt" -> today.plus(DatePeriod(days = 2))
    else -> LITHUANIAN_WEEKDAY_STEMS.indexOfFirst(plain::startsWith).takeIf { it >= 0 }?.let { today.comingOrSame(DayOfWeek.entries[it]) }
}

private fun LocalDate.comingOrSame(weekday: DayOfWeek): LocalDate =
    plus(DatePeriod(days = (weekday.isoDayNumber - dayOfWeek.isoDayNumber + 7) % 7))

private fun plainLithuanian(word: String) = word.lowercase()
    .replace('ž', 'z').replace('ė', 'e').replace('ū', 'u').replace('č', 'c').replace('š', 's').replace('į', 'i').replace('ą', 'a').replace('ę', 'e').replace('ų', 'u')

private fun englishMonthNumber(word: String) =
    listOf("jan", "feb", "mar", "apr", "may", "jun", "jul", "aug", "sep", "oct", "nov", "dec").indexOf(word.take(3).lowercase()) + 1

private fun lithuanianMonthNumber(word: String): Int {
    val plain = plainLithuanian(word)
    return listOf("sausio", "vasario", "kovo", "balandzio", "geguzes", "birzelio", "liepos", "rugpjucio", "rugsejo", "spalio", "lapkricio", "gruodzio")
        .indexOf(plain) + 1
}
