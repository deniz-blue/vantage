package blue.deniz.vantage.app.screens

import kotlinx.datetime.daysUntil
import blue.deniz.vantage.app.EventsView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import blue.deniz.vantage.core.CalendarMarks
import blue.deniz.vantage.core.isCancelled
import blue.deniz.vantage.core.isCancelledOn
import blue.deniz.vantage.core.CalendarPeriod
import blue.deniz.vantage.app.AppState
import blue.deniz.vantage.app.CopyMenuItem
import blue.deniz.vantage.app.ui.AppIcons
import blue.deniz.vantage.app.ui.BottomSheet
import blue.deniz.vantage.app.ui.EventFormatting
import blue.deniz.vantage.app.ui.Icon
import blue.deniz.vantage.app.ui.ListRow
import blue.deniz.vantage.app.ui.ListSection
import blue.deniz.vantage.app.ui.SectionTitle
import blue.deniz.vantage.app.ui.SheetTitle
import blue.deniz.vantage.app.ui.Spacing
import blue.deniz.vantage.app.ui.MenuItem
import blue.deniz.vantage.app.ui.Text
import blue.deniz.vantage.app.ui.label
import directory.evnt.Activity
import directory.evnt.Occurrence
import directory.evnt.OpenEvnt
import directory.evnt.PartialDate
import directory.evnt.translate
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import blue.deniz.vantage.app.ui.Radius
import kotlin.time.Duration.Companion.days
import kotlinx.coroutines.delay
import kotlinx.datetime.LocalDate
import kotlinx.datetime.format.MonthNames
import kotlinx.datetime.number

private val DETAIL_TILE_SIZE = 48.dp

@Composable
internal fun OccurrenceRows(state: AppState, eventTitle: String, eventData: OpenEvnt?, occurrences: List<Occurrence>, marks: CalendarMarks) {
    var shownIndex by rememberSaveable { mutableStateOf<Int?>(null) }
    for ((index, occurrence) in occurrences.withIndex()) {
        val periods = remember(occurrence, marks) { occurrence.start?.let { marks.periodsOn(it, occurrence.end) }.orEmpty() }
        OccurrenceCard(occurrence, state.formatting, periods) { shownIndex = index }
    }
    val shown = shownIndex?.let { index -> occurrences.getOrNull(index)?.let { IndexedValue(index, it) } }
    BottomSheet(item = shown, onDismiss = { shownIndex = null }) { (index, occurrence) ->
        val position = if (occurrences.size > 1) "${index + 1} of ${occurrences.size}" else null
        val cancelled = eventData?.isCancelledOn(occurrence) ?: occurrence.isCancelled
        OccurrenceDetails(state, eventTitle, occurrence, position, cancelled, marks, onClose = { shownIndex = null })
    }
}

private fun activityCountLabel(count: Int) = if (count == 1) "1 activity" else "$count activities"

@Composable
private fun OccurrenceCard(occurrence: Occurrence, formatting: EventFormatting, periods: List<CalendarPeriod>, onOpen: () -> Unit) {
    val text = formatting.occurrenceText(occurrence)
    val cancelled = occurrence.isCancelled
    val activityCount = occurrence.activities.orEmpty().size
    val supporting = listOfNotNull(
        text.supporting,
        "Cancelled".takeIf { cancelled },
        activityCount.takeIf { it > 0 }?.let(::activityCountLabel),
        periods.takeIf { it.isNotEmpty() }?.joinToString(" and ", prefix = "During ") { it.label },
    ).joinToString(" · ").ifEmpty { null }
    ListRow(
        text.headline,
        leading = { DateTile(occurrence.start?.let(formatting::localDateOrNull)) },
        supporting = supporting,
        enabled = !cancelled || activityCount > 0,
        onClick = onOpen.takeIf { occurrence.start != null || activityCount > 0 },
        trailing = { if (activityCount > 0) Icon(AppIcons.ChevronRight) },
    )
}

@Composable
internal fun DetailTile(content: @Composable () -> Unit) {
    Box(
        Modifier.size(DETAIL_TILE_SIZE).clip(RoundedCornerShape(Radius.sm)).background(MaterialTheme.colorScheme.surfaceContainerHighest),
        contentAlignment = Alignment.Center,
    ) { content() }
}

@Composable
private fun DateTile(date: LocalDate?) {
    DetailTile {
        if (date == null) {
            Icon(AppIcons.Calendar, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            return@DetailTile
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(MonthNames.ENGLISH_ABBREVIATED.names[date.month.number - 1].uppercase(), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
            Text(date.day.toString(), style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun OccurrenceDetails(
    state: AppState,
    eventTitle: String,
    occurrence: Occurrence,
    position: String?,
    cancelled: Boolean,
    marks: CalendarMarks,
    onClose: () -> Unit,
) {
    val formatting = state.formatting
    val preferredLanguages = state.preferredLanguages
    Column(Modifier.fillMaxWidth().padding(horizontal = Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        SheetTitle(eventTitle)
        Text(listOfNotNull("Date", position).joinToString(" "), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
        if (cancelled) Text("Cancelled", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelLarge)
    }
    val start = occurrence.start
    if (start == null) {
        ActivityRows(occurrence.activities.orEmpty(), preferredLanguages)
        return
    }
    val end = occurrence.end
    val firstDay = formatting.localDateOrNull(start)
    val lastDay = end?.let(formatting::localDateOrNull)?.takeIf { firstDay != null && it > firstDay } ?: firstDay
    val endInstant = end?.endInstant ?: start.takeIf { it !is PartialDate.Time }?.endInstant
    val now by produceState(state.clock.now(), state.clock) {
        while (true) {
            delay(1_000 - value.toEpochMilliseconds() % 1_000)
            value = state.clock.now()
        }
    }
    ListSection {
        if (end != null && firstDay != null && lastDay != null && lastDay > firstDay) {
            ListRow(formatting.formatLongDate(start), icon = AppIcons.Calendar, supporting = "Starts")
            ListRow(formatting.formatLongDate(end), icon = AppIcons.Calendar, supporting = "Ends")
            val days = firstDay.daysUntil(lastDay) + 1
            ListRow("$days days", icon = AppIcons.DateRange, supporting = periodProgressText(state.today, firstDay, lastDay))
        } else {
            ListRow(formatting.formatLongDate(start), icon = AppIcons.Calendar, supporting = firstDay?.let { daysAwayText(state.today, it) })
        }
        val time = formatting.occurrenceTimeText(occurrence, showsStatedTime = false)
        if (time != null) {
            val length = (end as? PartialDate.Time)?.let { formatting.formatDuration(it.startInstant - start.startInstant) }
            ListRow(listOfNotNull(time, length).joinToString(" · "), icon = AppIcons.Clock, supporting = if (cancelled) null else formatting.formatTimeFromNow(now, start.startInstant, endInstant))
        }
        (start as? PartialDate.Time)?.let(formatting::statedTimeText)?.let { ListRow(it, icon = AppIcons.World, supporting = "Local Time There") }
    }
    PeriodsSection(state, remember(start, end, marks) { marks.periodsOn(start, end) }, firstDay, beforeOpening = onClose)
    ActivityRows(occurrence.activities.orEmpty(), preferredLanguages)
    Column {
        if (firstDay != null) {
            MenuItem("Show in Calendar", onClick = {
                onClose()
                state.calendarSelection.select(firstDay)
                state.openMyEvents(EventsView.Calendar)
            }, leadingIcon = AppIcons.Calendar)
        }
        if (start is PartialDate.Time) {
            val timestamp = discordTimestamp(start, end as? PartialDate.Time)
            val countdown = "<t:${start.startInstant.epochSeconds}:R>"
            CopyMenuItem("Copy Discord Timestamp", supportingText = timestamp) { timestamp }
            CopyMenuItem("Copy Discord Countdown", supportingText = countdown) { countdown }
        }
        val text = formatting.occurrenceText(occurrence)
        CopyMenuItem("Copy as Text") { listOfNotNull(eventTitle, text.headline, text.supporting).joinToString(" · ") }
    }
}

@Composable
private fun ActivityRows(activities: List<Activity>, preferredLanguages: List<String>) {
    if (activities.isEmpty()) return
    Column(Modifier.padding(horizontal = Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        SectionTitle("Activities", Modifier.padding(top = Spacing.md))
        for (activity in activities) {
            val cancelled = activity.status == Activity.Status.Cancelled
            ListRow(
                activity.name.translate(preferredLanguages),
                supporting = "Cancelled".takeIf { cancelled },
                color = if (cancelled) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

private fun discordTimestamp(start: PartialDate.Time, end: PartialDate.Time?): String {
    val from = "<t:${start.startInstant.epochSeconds}:F>"
    if (end == null) return from
    val endStyle = if (end.startInstant - start.startInstant < 1.days) "t" else "F"
    return "$from – <t:${end.startInstant.epochSeconds}:$endStyle>"
}
