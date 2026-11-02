package blue.deniz.vantage.app.ui

import directory.evnt.OpenEvnt
import blue.deniz.vantage.core.statusOn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import blue.deniz.vantage.core.ResolvedEvent
import blue.deniz.vantage.core.backgroundSplashMedia
import blue.deniz.vantage.core.nextOrLastOccurrenceAt
import directory.evnt.Occurrence
import kotlin.time.Instant

private val POSTER_WIDTH = 176.dp
private const val POSTER_MEDIA_ASPECT_RATIO = 2f
private val TICKET_WIDTH = 296.dp
private val DATE_BLOCK_WIDTH = 64.dp

internal fun ResolvedEvent.shownOccurrence(now: Instant): Occurrence? =
    data?.nextOrLastOccurrenceAt(now)

private const val UNKNOWN_TIME = "Time Unknown"
private const val UNKNOWN_PLACE = "Place Unknown"

private fun EventFormatting.whenLine(occurrence: Occurrence?, now: Instant, status: OpenEvnt.Status?): String? =
    listOfNotNull(occurrence?.let { occurrenceTimeText(it, showsStatedTime = false) }, occurrence?.let { formatStartingSoon(it, now, status) }).joinToString(" · ").ifEmpty { null }

@Composable
private fun CompactEventLines(
    event: ResolvedEvent,
    preferredLanguages: List<String>,
    dayLine: String?,
    whenLine: String?,
    venue: String?,
    changeLabel: String?,
    fallbackTitle: String?,
    titleMaxLines: Int,
    titleMinLines: Int = 1,
    status: OpenEvnt.Status? = null,
) {
    EventTitleRow(event, preferredLanguages, maxLines = titleMaxLines, minLines = titleMinLines, changeLabel = changeLabel, fallbackTitle = fallbackTitle)
    if (dayLine != null) IconLine(AppIcons.Calendar, dayLine)
    if (whenLine != null || status != null) IconLine(AppIcons.Clock, whenLine.orEmpty(), status = status)
    else IconLine(AppIcons.Clock, UNKNOWN_TIME, color = MaterialTheme.colorScheme.onSurfaceVariant, fontStyle = FontStyle.Italic)
    if (venue != null) IconLine(AppIcons.MapPin, venue)
    else IconLine(AppIcons.MapPin, UNKNOWN_PLACE, color = MaterialTheme.colorScheme.onSurfaceVariant, fontStyle = FontStyle.Italic)
}

@Composable
private fun DateBlock(text: DateBlockText?, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        if (text == null) {
            Text("?", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.titleLarge)
            return@Column
        }
        text.above?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, style = MaterialTheme.typography.labelSmall) }
        Text(text.main, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold, maxLines = 1, style = MaterialTheme.typography.titleLarge)
        text.below?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, style = MaterialTheme.typography.labelSmall) }
    }
}

@Composable
fun EventPosterCard(
    event: ResolvedEvent,
    formatting: EventFormatting,
    preferredLanguages: List<String>,
    now: Instant,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    changeLabel: String? = null,
    selection: EventCardSelection = EventCardSelection.NotSelecting,
    fallbackTitle: String? = null,
) {
    val occurrence = remember(event.data, now) { event.shownOccurrence(now) }
    val splash = remember(event.data) { event.data?.backgroundSplashMedia() }
    val venue = remember(event.data, preferredLanguages, formatting) { event.data?.venueLine(preferredLanguages) }
    Card(modifier.width(POSTER_WIDTH), onClick = onClick, onLongClick = onLongClick, border = selection.border(), contentPadding = PaddingValues()) {
        Column {
            Box(Modifier.fillMaxWidth().aspectRatio(POSTER_MEDIA_ASPECT_RATIO).background(MaterialTheme.colorScheme.surfaceContainerHighest)) {
                if (splash == null) DateBlock(occurrence?.start?.let(formatting::dateBlockText), Modifier.align(Alignment.Center))
                else OpenEvntImage(splash, preferredLanguages, Modifier.matchParentSize())
                if (selection != EventCardSelection.NotSelecting) SelectionMark(selection == EventCardSelection.Selected, Modifier.align(Alignment.TopEnd))
            }
            val dayLine = occurrence?.start?.let(formatting::formatShortDay)
            val status = event.data?.statusOn(occurrence)
            val whenLine = formatting.whenLine(occurrence, now, status)
            Box(Modifier.padding(Spacing.sm)) {
                AtLeastAsTallAs(reference = { Column { CompactEventLines(event, preferredLanguages, dayLine, whenLine, venue, changeLabel, fallbackTitle, titleMaxLines = 2, titleMinLines = 2, status = status) } }) {
                    Column { CompactEventLines(event, preferredLanguages, dayLine, whenLine, venue, changeLabel, fallbackTitle, titleMaxLines = 2, status = status) }
                }
            }
        }
    }
}

@Composable
fun EventTicketCard(
    event: ResolvedEvent,
    formatting: EventFormatting,
    preferredLanguages: List<String>,
    now: Instant,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    changeLabel: String? = null,
    selection: EventCardSelection = EventCardSelection.NotSelecting,
    fallbackTitle: String? = null,
) {
    val occurrence = remember(event.data, now) { event.shownOccurrence(now) }
    val venue = remember(event.data, preferredLanguages, formatting) { event.data?.venueLine(preferredLanguages) }
    Card(modifier.width(TICKET_WIDTH), onClick = onClick, onLongClick = onLongClick, border = selection.border(), contentPadding = PaddingValues()) {
        Row(Modifier.height(IntrinsicSize.Min)) {
            DateBlock(
                occurrence?.start?.let(formatting::dateBlockText),
                Modifier
                    .width(DATE_BLOCK_WIDTH)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                    .padding(vertical = Spacing.sm),
            )
            Column(Modifier.weight(1f).padding(Spacing.sm)) {
                CompactEventLines(event, preferredLanguages, dayLine = null, formatting.whenLine(occurrence, now, event.data?.statusOn(occurrence)), venue, changeLabel, fallbackTitle, titleMaxLines = 1, status = event.data?.statusOn(occurrence))
            }
            if (selection != EventCardSelection.NotSelecting) SelectionMark(selection == EventCardSelection.Selected, Modifier.align(Alignment.CenterVertically))
        }
    }
}
