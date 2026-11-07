package blue.deniz.vantage.app.screens

import blue.deniz.vantage.app.ui.EventFormatting
import blue.deniz.vantage.app.ui.shownOccurrence
import blue.deniz.vantage.core.CalendarMarker
import blue.deniz.vantage.core.CalendarPeriod
import blue.deniz.vantage.core.ResolvedEvent
import directory.evnt.PartialDate
import kotlinx.datetime.LocalDate
import kotlinx.datetime.format.MonthNames
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

sealed interface AgendaRow {
    val key: String

    sealed interface Label : AgendaRow

    data class Heading(override val key: String, val title: String, val isCurrent: Boolean = false) : AgendaRow
    data class Day(override val key: String, val date: LocalDate, val isToday: Boolean) : Label
    data class Group(override val key: String, val title: String) : Label
    data class Event(val event: ResolvedEvent) : AgendaRow {
        override val key get() = event.listKey()
    }
    data class Marker(val marker: CalendarMarker) : AgendaRow {
        override val key get() = "marker ${marker.id}"
    }
    data class PeriodStart(val period: CalendarPeriod) : AgendaRow {
        override val key get() = "period ${period.id}"
    }
}

enum class AgendaTime { Past, Today, Future, Undated }

class Agenda(
    val rows: List<AgendaRow>,
    val rowTimes: List<AgendaTime>,
    private val periodsRunning: List<List<CalendarPeriod>>,
    val upcomingStartRow: Int = 0,
) {
    private val rowIndexByKey: Map<String, Int> = rows.withIndex().associate { (index, row) -> row.key to index }
    private val headingRowOfRow: List<Int?> = run {
        var heading: Int? = null
        rows.mapIndexed { index, row -> if (row is AgendaRow.Heading) index.also { heading = it } else heading }
    }

    fun rowIndexOf(key: Any): Int? = (key as? String)?.let(rowIndexByKey::get)

    fun headingRowOf(row: Int): Int? = headingRowOfRow.getOrNull(row)

    fun periodsRunningAt(row: Int): List<CalendarPeriod> = periodsRunning.getOrNull(row).orEmpty()
}

private const val UNDATED_HEADING = "No Date"

fun agendaOf(
    events: List<ResolvedEvent>,
    cards: EventCardData,
    markers: List<CalendarMarker> = emptyList(),
    periods: List<CalendarPeriod> = emptyList(),
    marksAfterEvents: Boolean = false,
    dayLabels: Boolean = true,
    upcomingFrom: Int? = null,
): Agenda = agendaOf(events, cards.formatting, cards.now, markers, periods, marksAfterEvents, dayLabels, upcomingFrom) { event -> event.shownOccurrence(cards.now)?.start }

fun agendaOf(
    events: List<ResolvedEvent>,
    formatting: EventFormatting,
    now: Instant,
    markers: List<CalendarMarker> = emptyList(),
    periods: List<CalendarPeriod> = emptyList(),
    marksAfterEvents: Boolean = false,
    dayLabels: Boolean = true,
    upcomingFrom: Int? = null,
    startOf: (ResolvedEvent) -> PartialDate?,
): Agenda {
    val today = now.toLocalDateTime(formatting.timeZone).date
    val rows = mutableListOf<AgendaRow>()
    val rowDays = mutableListOf<LocalDate?>()
    var headingKey: String? = null
    var labelledDay: LocalDate? = null
    val usedKeys = mutableMapOf<String, Int>()
    var todayPlaced = false
    val hasPastPart = upcomingFrom != null
    val (endedPeriods, openPeriods) = periods.partition { hasPastPart && it.endsAt <= now }
    val (endedMarkers, openMarkers) = markers.partition { hasPastPart && it.endsAt <= now }
    var marks = marksOf(endedPeriods, endedMarkers)
    var placedMarks = 0
    var upcomingStartRow = -1

    fun add(row: AgendaRow, day: LocalDate? = null) {
        rows += row
        rowDays += day
    }

    fun uniqueKey(key: String): String {
        val repeat = usedKeys.getOrElse(key) { 0 }
        usedKeys[key] = repeat + 1
        return if (repeat == 0) key else "$key #$repeat"
    }

    fun addHeading(heading: AgendaRow.Heading) {
        if (heading.key == headingKey) return
        add(if (dayLabels) AgendaRow.Group(uniqueKey(heading.key), heading.title) else heading.copy(key = uniqueKey(heading.key)))
        headingKey = heading.key
    }

    fun addDayLabel(day: LocalDate) {
        if (day == labelledDay) return
        add(AgendaRow.Day(uniqueKey("day $day"), day, isToday = day == today), day = day)
        labelledDay = day
    }

    fun placeToday() {
        if (!dayLabels) addHeading(monthHeadingOf(today, today))
        addDayLabel(today)
        todayPlaced = true
    }

    fun place(start: PartialDate?, row: AgendaRow) {
        val firstDay = start?.let(formatting::firstDayOf)
        if (!todayPlaced && (firstDay == null || firstDay >= today)) {
            val labelledAsToday = dayLabels && firstDay == today && (start is PartialDate.Day || start is PartialDate.Time)
            if (labelledAsToday) todayPlaced = true else placeToday()
        }
        val exactDay = firstDay?.takeIf { start is PartialDate.Day || start is PartialDate.Time }
        if (dayLabels && exactDay != null) headingKey = null else addHeading(headingOf(start, firstDay, today))
        when {
            !dayLabels -> Unit
            exactDay != null -> addDayLabel(exactDay)
            else -> labelledDay = null
        }
        add(row, firstDay)
    }

    fun placeMarksBefore(instant: Instant?) {
        while (placedMarks < marks.size && (instant == null || marks[placedMarks].comesBefore(instant))) {
            val (at, row) = marks[placedMarks++]
            place(at, row)
        }
    }

    fun startUpcoming() {
        placeMarksBefore(null)
        marks = marksOf(openPeriods, openMarkers)
        placedMarks = 0
        upcomingStartRow = rows.size
        headingKey = null
        labelledDay = null
        todayPlaced = false
    }

    for ((index, event) in events.withIndex()) {
        if (index == (upcomingFrom ?: 0)) startUpcoming()
        val start = startOf(event)
        placeMarksBefore(start?.startInstant)
        place(start, AgendaRow.Event(event))
    }
    if (upcomingStartRow < 0) startUpcoming()
    if (marksAfterEvents) placeMarksBefore(null)
    if (!todayPlaced) placeToday()
    return Agenda(rows, timesOf(rowDays, today), periodsRunningOf(rows, rowDays, formatting), upcomingStartRow)
}

private fun marksOf(periods: List<CalendarPeriod>, markers: List<CalendarMarker>): List<Pair<PartialDate, AgendaRow>> =
    (periods.map { it.start to AgendaRow.PeriodStart(it) } + markers.map { it.at to AgendaRow.Marker(it) })
        .sortedBy { (at, _) -> at.startInstant }

private fun Pair<PartialDate, AgendaRow>.comesBefore(instant: Instant): Boolean {
    val (at, row) = this
    return at.startInstant < instant || (row is AgendaRow.PeriodStart && at is PartialDate.Day && at.startInstant == instant)
}

private fun periodsRunningOf(rows: List<AgendaRow>, rowDays: List<LocalDate?>, formatting: EventFormatting): List<List<CalendarPeriod>> {
    var nextDay: LocalDate? = null
    val days = rowDays.asReversed().map { day -> (day ?: nextDay).also { nextDay = it } }.asReversed()
    val started = mutableListOf<Pair<CalendarPeriod, LocalDate>>()
    return rows.mapIndexed { index, row ->
        if (row is AgendaRow.PeriodStart) formatting.localDateOrNull(row.period.end)?.let { started += row.period to it }
        val day = days[index] ?: return@mapIndexed emptyList()
        started.filter { (_, lastDay) -> lastDay >= day }.map { (period, _) -> period }
    }
}

private fun timesOf(rowDays: List<LocalDate?>, today: LocalDate): List<AgendaTime> {
    var next = AgendaTime.Undated
    return rowDays.asReversed().map { day ->
        if (day != null) next = when {
            day < today -> AgendaTime.Past
            day == today -> AgendaTime.Today
            else -> AgendaTime.Future
        }
        next
    }.asReversed()
}

private fun EventFormatting.firstDayOf(start: PartialDate): LocalDate = when (start) {
    is PartialDate.Year -> LocalDate(start.year, 1, 1)
    is PartialDate.Month -> LocalDate(start.year, start.month, 1)
    is PartialDate.Day -> start.toLocalDate()
    is PartialDate.Time -> localDateOf(start)
}

private fun headingOf(start: PartialDate?, firstDay: LocalDate?, today: LocalDate): AgendaRow.Heading = when {
    start == null || firstDay == null -> AgendaRow.Heading("undated", UNDATED_HEADING)
    start is PartialDate.Year -> AgendaRow.Heading("year ${start.year}", start.year.toString())
    else -> monthHeadingOf(firstDay, today)
}

private fun monthHeadingOf(day: LocalDate, today: LocalDate): AgendaRow.Heading {
    val month = MonthNames.ENGLISH_FULL.names[day.month.ordinal]
    return AgendaRow.Heading(
        "month ${day.year}-${day.month.ordinal}",
        if (day.year == today.year) month else "$month ${day.year}",
        isCurrent = day.year == today.year && day.month == today.month,
    )
}
