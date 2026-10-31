package blue.deniz.vantage.app.screens

import blue.deniz.vantage.core.statusOn
import blue.deniz.vantage.app.ui.label
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import blue.deniz.vantage.app.AppState
import blue.deniz.vantage.app.EventsView
import blue.deniz.vantage.app.Route
import blue.deniz.vantage.app.collectAcrossSession
import blue.deniz.vantage.app.ui.AppIcons
import blue.deniz.vantage.app.ui.ListRow
import blue.deniz.vantage.app.ui.UNTITLED_EVENT
import blue.deniz.vantage.app.ui.ListSection
import blue.deniz.vantage.core.CalendarMarker
import blue.deniz.vantage.core.CalendarPeriod
import blue.deniz.vantage.core.TimeRange
import blue.deniz.vantage.core.firstOccurrenceDuring
import blue.deniz.vantage.storage.EventListQuery
import directory.evnt.PartialDate
import directory.evnt.translate
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.daysUntil
import kotlinx.datetime.plus

private const val EVENTS_SHOWN_IN_SECTION = 5

fun daysAwayText(today: LocalDate, day: LocalDate): String = when (val days = today.daysUntil(day)) {
    -1 -> "Yesterday"
    0 -> "Today"
    1 -> "Tomorrow"
    else -> if (days > 0) "In $days days" else "${-days} days ago"
}

fun periodProgressText(day: LocalDate, firstDay: LocalDate, lastDay: LocalDate): String = when {
    day < firstDay -> "Starts ${daysAwayText(day, firstDay).lowercase()}"
    day > lastDay -> "Ended ${daysAwayText(day, lastDay).lowercase()}"
    else -> "Day ${firstDay.daysUntil(day) + 1} of ${firstDay.daysUntil(lastDay) + 1}"
}

@Composable
fun PeriodsSection(state: AppState, periods: List<CalendarPeriod>, day: LocalDate?, beforeOpening: () -> Unit) {
    if (periods.isEmpty()) return
    val formatting = state.formatting
    ListSection(title = "Periods") {
        for (period in periods) {
            val first = formatting.localDateOrNull(period.start)
            val last = formatting.localDateOrNull(period.end)
            val supporting = if (first == null || last == null) null else {
                listOfNotNull(formatting.formatDayRange(first, last), day?.let { periodProgressText(it, first, last) }).joinToString(" · ")
            }
            ListRow(period.label, icon = AppIcons.DateRange, supporting = supporting, onClick = {
                beforeOpening()
                state.openMarkSheet(MarkSheetTarget(MarkKind.Period, period.id))
            })
        }
    }
}

@Composable
fun MarkersSection(state: AppState, markers: List<CalendarMarker>, beforeOpening: () -> Unit) {
    if (markers.isEmpty()) return
    ListSection(title = "Markers") {
        for (marker in markers) {
            ListRow(
                marker.label,
                icon = AppIcons.Label,
                supporting = (marker.at as? PartialDate.Time)?.let(state.formatting::formatTime) ?: "All Day",
                onClick = {
                    beforeOpening()
                    state.openMarkSheet(MarkSheetTarget(MarkKind.Marker, marker.id))
                },
            )
        }
    }
}

@Composable
fun EventsSection(state: AppState, title: String, firstDay: LocalDate, lastDay: LocalDate, exceptEventId: String?, beforeOpening: () -> Unit) {
    val zone = state.timeZone
    val window = remember(firstDay, lastDay, zone) { TimeRange(firstDay.atStartOfDayIn(zone), lastDay.plus(1, DateTimeUnit.DAY).atStartOfDayIn(zone)) }
    val query = remember(window, exceptEventId) {
        EventListQuery(
            from = window.start,
            until = window.end,
            order = EventListQuery.Order.Start,
            limit = EVENTS_SHOWN_IN_SECTION + 1 + if (exceptEventId != null) 1 else 0,
        )
    }
    val found = collectAcrossSession("events $firstDay $lastDay", query) { state.store.observeList(query) } ?: return
    val events = found.filter { it.id == null || it.id != exceptEventId }
    if (events.isEmpty()) return
    ListSection(title = title) {
        for (event in events.take(EVENTS_SHOWN_IN_SECTION)) {
            val occurrence = event.data?.firstOccurrenceDuring(window)
            ListRow(
                event.data?.name.translate(state.preferredLanguages).ifEmpty { UNTITLED_EVENT },
                supporting = occurrence?.let {
                    listOfNotNull(event.data?.statusOn(it)?.label, it.start?.let(state.formatting::formatShortDay), state.formatting.occurrenceTimeText(it)).joinToString(" · ")
                },
                onClick = event.id?.let { eventId ->
                    {
                        beforeOpening()
                        state.navigator.open(Route.EventDetails(eventId))
                    }
                },
            )
        }
        if (events.size > EVENTS_SHOWN_IN_SECTION) {
            ListRow("Show in Calendar", icon = AppIcons.Calendar, onClick = {
                beforeOpening()
                state.calendarSelection.select(firstDay)
                state.openMyEvents(EventsView.Calendar)
            })
        }
    }
}
