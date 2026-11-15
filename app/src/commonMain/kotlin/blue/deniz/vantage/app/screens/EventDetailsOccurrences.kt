package blue.deniz.vantage.app.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
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
import blue.deniz.vantage.app.CopyOptionsPage
import blue.deniz.vantage.app.CopyOptionsRow
import blue.deniz.vantage.app.Route
import blue.deniz.vantage.ui.AppIcons
import blue.deniz.vantage.ui.BottomSheet
import blue.deniz.vantage.app.ui.EventFormatting
import blue.deniz.vantage.ui.Icon
import blue.deniz.vantage.ui.ListRow
import blue.deniz.vantage.ui.Text
import blue.deniz.vantage.app.ui.label
import directory.evnt.Occurrence
import directory.evnt.OpenEvnt
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import blue.deniz.vantage.ui.Radius
import kotlinx.datetime.LocalDate
import kotlinx.datetime.format.MonthNames
import kotlinx.datetime.number

private val DETAIL_TILE_SIZE = 48.dp

@Composable
internal fun OccurrenceRows(state: AppState, eventTitle: String, eventData: OpenEvnt?, occurrences: List<Occurrence>, marks: CalendarMarks) {
    var shownIndex by rememberSaveable { mutableStateOf<Int?>(null) }
    for ((index, occurrence) in occurrences.withIndex()) {
        val periods = remember(occurrence, marks) { occurrence.start?.let { marks.periodsOn(it, occurrence.end) }.orEmpty() }
        OccurrenceCard(occurrence, state.formatting, periods) {
            if (eventData != null && occurrence.activities.orEmpty().isNotEmpty()) state.navigator.open(Route.EventOccurrence(eventData, occurrence))
            else shownIndex = index
        }
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
    var copying by rememberSaveable { mutableStateOf(false) }
    val copyOptions = occurrenceCopyOptions(state.formatting, eventTitle, occurrence)
    if (copying) return CopyOptionsPage(copyOptions, onBack = { copying = false })
    OccurrenceSummary(state, eventTitle, occurrence, position, cancelled)
    OccurrencePeriods(state, occurrence, marks, beforeOpening = onClose)
    Column {
        ShowInCalendarItem(state, occurrence, beforeOpening = onClose)
        CopyOptionsRow(copyOptions, onOpenPage = { copying = true })
    }
}
