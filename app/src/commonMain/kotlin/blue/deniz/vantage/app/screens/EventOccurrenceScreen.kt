package blue.deniz.vantage.app.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import blue.deniz.vantage.app.AppState
import blue.deniz.vantage.app.CopyOption
import blue.deniz.vantage.app.CopyOptionsPage
import blue.deniz.vantage.app.CopyOptionsRow
import blue.deniz.vantage.app.EventsView
import blue.deniz.vantage.ui.AppIcons
import blue.deniz.vantage.ui.BottomSheet
import blue.deniz.vantage.ui.ContentMaxWidth
import blue.deniz.vantage.ui.ContentUnderOverlay
import blue.deniz.vantage.app.ui.EventFormatting
import blue.deniz.vantage.ui.IconButton
import blue.deniz.vantage.ui.ListRow
import blue.deniz.vantage.ui.LocalBackdrop
import blue.deniz.vantage.ui.MenuItem
import blue.deniz.vantage.ui.ScreenHeader
import blue.deniz.vantage.ui.SectionTitle
import blue.deniz.vantage.ui.Spacing
import blue.deniz.vantage.ui.Text
import blue.deniz.vantage.app.ui.UNTITLED_EVENT
import blue.deniz.vantage.ui.backdropSource
import blue.deniz.vantage.app.ui.translatedName
import blue.deniz.vantage.core.CalendarMarks
import blue.deniz.vantage.core.isCancelledOn
import directory.evnt.Activity
import directory.evnt.Occurrence
import directory.evnt.OpenEvnt
import directory.evnt.PartialDate
import directory.evnt.sortedByStart
import directory.evnt.translate
import kotlin.time.Duration.Companion.days

@Composable
fun EventOccurrenceScreen(state: AppState, event: OpenEvnt, occurrence: Occurrence) {
    val title = event.translatedName(state.preferredLanguages) ?: UNTITLED_EVENT
    val occurrences = remember(event) { event.occurrences.orEmpty().sortedByStart() }
    val index = occurrences.indexOf(occurrence)
    val position = if (occurrences.size > 1 && index >= 0) "${index + 1} of ${occurrences.size}" else null
    val cancelled = event.isCancelledOn(occurrence)
    val marks = rememberCalendarMarks(state, shown = true)
    var showingMenu by rememberSaveable { mutableStateOf(false) }
    ContentUnderOverlay(
        overlay = { ScreenHeader("", onBack = { state.navigator.goBack() }) { IconButton(AppIcons.DotsVertical, "More", onClick = { showingMenu = true }) } },
        modifier = Modifier.fillMaxSize(),
    ) { headerHeight ->
        Column(
            Modifier.fillMaxSize().backdropSource(LocalBackdrop.current).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                Modifier.widthIn(max = ContentMaxWidth).fillMaxWidth().navigationBarsPadding().padding(top = headerHeight, bottom = Spacing.md),
                verticalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                OccurrenceSummary(state, title, occurrence, position, cancelled)
                OccurrencePeriods(state, occurrence, marks, beforeOpening = {})
                ActivityRows(occurrence.activities.orEmpty(), state.preferredLanguages)
            }
        }
    }
    BottomSheet(visible = showingMenu, onDismiss = { showingMenu = false }) {
        OccurrenceMenu(state, title, occurrence, onClose = { showingMenu = false })
    }
}

@Composable
private fun OccurrenceMenu(state: AppState, eventTitle: String, occurrence: Occurrence, onClose: () -> Unit) {
    var copying by rememberSaveable { mutableStateOf(false) }
    val copyOptions = occurrenceCopyOptions(state.formatting, eventTitle, occurrence)
    if (copying) return CopyOptionsPage(copyOptions, onBack = { copying = false })
    Column {
        ShowInCalendarItem(state, occurrence, beforeOpening = onClose)
        CopyOptionsRow(copyOptions, onOpenPage = { copying = true })
    }
}

@Composable
internal fun OccurrencePeriods(state: AppState, occurrence: Occurrence, marks: CalendarMarks, beforeOpening: () -> Unit) {
    val start = occurrence.start ?: return
    val periods = remember(start, occurrence.end, marks) { marks.periodsOn(start, occurrence.end) }
    PeriodsSection(state, periods, state.formatting.localDateOrNull(start), beforeOpening = beforeOpening)
}

@Composable
internal fun ShowInCalendarItem(state: AppState, occurrence: Occurrence, beforeOpening: () -> Unit) {
    val firstDay = occurrence.start?.let(state.formatting::localDateOrNull) ?: return
    MenuItem("Show in Calendar", onClick = {
        beforeOpening()
        state.calendarSelection.select(firstDay)
        state.openMyEvents(EventsView.Calendar)
    }, leadingIcon = AppIcons.Calendar)
}

internal fun occurrenceCopyOptions(formatting: EventFormatting, eventTitle: String, occurrence: Occurrence): List<CopyOption> {
    val start = occurrence.start
    val text = formatting.occurrenceText(occurrence)
    val asText = CopyOption("Text") { listOfNotNull(eventTitle, text.headline, text.supporting).joinToString(" · ") }
    if (start !is PartialDate.Time) return listOf(asText)
    val timestamp = discordTimestamp(start, occurrence.end as? PartialDate.Time)
    val countdown = "<t:${start.startInstant.epochSeconds}:R>"
    return listOf(
        CopyOption("Discord Timestamp", timestamp) { timestamp },
        CopyOption("Discord Countdown", countdown) { countdown },
        asText,
    )
}

@Composable
internal fun ActivityRows(activities: List<Activity>, preferredLanguages: List<String>) {
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
