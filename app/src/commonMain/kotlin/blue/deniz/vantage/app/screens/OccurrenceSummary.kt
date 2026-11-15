package blue.deniz.vantage.app.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import blue.deniz.vantage.app.AppState
import blue.deniz.vantage.ui.Spacing
import blue.deniz.vantage.ui.Text
import directory.evnt.Occurrence
import directory.evnt.PartialDate
import kotlinx.coroutines.delay
import kotlinx.datetime.daysUntil

@Composable
internal fun OccurrenceSummary(state: AppState, eventTitle: String, occurrence: Occurrence, position: String?, cancelled: Boolean) {
    val formatting = state.formatting
    val start = occurrence.start
    val end = occurrence.end
    val firstDay = start?.let(formatting::localDateOrNull)
    val lastDay = end?.let(formatting::localDateOrNull)?.takeIf { firstDay != null && it > firstDay }
    val now by produceState(state.clock.now(), state.clock) {
        while (true) {
            delay(1_000 - value.toEpochMilliseconds() % 1_000)
            value = state.clock.now()
        }
    }
    val date = when {
        start == null -> "Date Unknown"
        firstDay != null && lastDay != null -> formatting.formatLongDayRange(firstDay, lastDay)
        else -> formatting.formatLongDate(start)
    }
    val timedStart = start as? PartialDate.Time
    val timedEnd = (end as? PartialDate.Time)?.takeIf { lastDay != null }
    val details = when {
        timedStart != null && timedEnd != null -> listOf(
            "${formatting.formatWeekdayAndTime(timedStart)} – ${formatting.formatWeekdayAndTime(timedEnd)}",
            formatting.formatHours(timedEnd.startInstant - timedStart.startInstant),
        ).joinToString(" · ")
        firstDay != null && lastDay != null -> "${firstDay.daysUntil(lastDay) + 1} days"
        start is PartialDate.Time -> {
            val length = (end as? PartialDate.Time)?.let { formatting.formatDuration(it.startInstant - start.startInstant) }
            listOfNotNull(formatting.occurrenceTimeText(occurrence, showsStatedTime = false), length).joinToString(" · ")
        }
        else -> null
    }
    val timeThere = (start as? PartialDate.Time)?.let(formatting::statedTimeText)?.let { "$it there" }
    val fromToday = when {
        start == null || firstDay == null -> null
        timedStart != null && timedEnd != null && firstDay <= state.today && now < timedEnd.startInstant -> when {
            now < timedStart.startInstant -> "Starts in ${formatting.formatHours(timedStart.startInstant - now)}"
            else -> "Ends in ${formatting.formatHours(timedEnd.startInstant - now)}"
        }
        lastDay != null -> periodProgressText(state.today, firstDay, lastDay)
        start is PartialDate.Time && firstDay == state.today -> formatting.formatTimeFromNow(now, start.startInstant, end?.endInstant)
        else -> daysAwayText(state.today, firstDay)
    }
    Column(Modifier.fillMaxWidth().padding(horizontal = Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Text(listOfNotNull(eventTitle, position).joinToString(" · "), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
        Text(date, style = MaterialTheme.typography.headlineMedium)
        details?.let { Text(it, style = MaterialTheme.typography.titleMedium) }
        timeThere?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium) }
        when {
            cancelled -> Text("Cancelled", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            fromToday != null -> Text(fromToday, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
