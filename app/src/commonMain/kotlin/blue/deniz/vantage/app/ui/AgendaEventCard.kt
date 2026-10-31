package blue.deniz.vantage.app.ui

import blue.deniz.vantage.core.statusOn
import directory.evnt.Occurrence

import blue.deniz.vantage.core.CalendarMarker
import blue.deniz.vantage.core.CalendarPeriod


import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text as MaterialText
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import blue.deniz.vantage.core.EventComplexity
import blue.deniz.vantage.core.ResolvedEvent
import blue.deniz.vantage.core.backgroundSplashMedia
import blue.deniz.vantage.core.complexity
import blue.deniz.vantage.storage.TagEntity
import directory.evnt.Media
import directory.evnt.PartialDate
import kotlinx.datetime.LocalDate
import kotlin.math.hypot
import kotlin.time.Instant

val AgendaCardHeight = 128.dp
val AgendaLineHeight = 40.dp
val AgendaLabelHeight = 28.dp
val AgendaLabelGap = Spacing.md
private val CURRENT_MONTH_DOT_SIZE = 8.dp
val AgendaIconSize = 14.dp
private val AGENDA_CHIP_HEIGHT = 24.dp
private const val AGENDA_CHIP_ALPHA = 0.18f
private const val AGENDA_DETAIL_ALPHA = 0.7f
private const val TABULAR_DIGITS = "tnum"
private const val WIDEST_CLOCK_TIME = "00:00"
private const val AGENDA_TITLE_MAX_LINES = 2
private val DATE_COLUMN_WIDTH = 44.dp
private const val DAY_NUMBER_LENGTH = 2
private const val TITLE_PATTERN_ALPHA = 0.06f
private const val TITLE_PATTERN_ANGLE = -45f
private val TITLE_PATTERN_STYLE = TextStyle(fontSize = 11.sp, lineHeight = 16.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.04.em)
private val TITLE_PATTERN_ROW_SHIFTS = listOf(40.dp, 110.dp)

@Composable
fun AgendaEventCard(
    event: ResolvedEvent,
    formatting: EventFormatting,
    preferredLanguages: List<String>,
    now: Instant,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    selection: EventCardSelection = EventCardSelection.NotSelecting,
    tags: List<TagEntity> = emptyList(),
    accentColor: Color? = null,
    changeLabel: String? = null,
    fallbackTitle: String? = null,
    problem: String? = null,
    showsDate: Boolean = true,
    showsSelectionMark: Boolean = true,
) {
    val occurrence = remember(event.data, now) { event.shownOccurrence(now) }
    val splash = remember(event.data) { event.data?.backgroundSplashMedia() }
    val title = event.data?.translatedName(preferredLanguages) ?: fallbackTitle
    val details = remember(occurrence, event.data, preferredLanguages, formatting, showsDate) {
        listOfNotNull(
            occurrence?.takeIf { it.start is PartialDate.Time }?.let(formatting::occurrenceTimeText),
            occurrence?.takeUnless { showsDate }?.let(formatting::untilText),
            event.data?.venueLine(preferredLanguages, formatting.homeCountry),
        ).joinToString(" · ")
    }
    val status = remember(event.data, occurrence) { event.data?.statusOn(occurrence) }
    val complexity = remember(event.data) { event.data?.complexity() ?: EventComplexity.Plain }
    val dateText = occurrence?.let { formatting.dateColumnText(it) }
    val textStart = if (showsDate) Spacing.sm else 0.dp
    val big = complexity == EventComplexity.Full
    Card(
        modifier,
        background = if (big) MaterialTheme.colorScheme.surfaceContainer else MaterialTheme.colorScheme.glassTint,
        onClick = onClick,
        onLongClick = onLongClick,
        border = selection.border() ?: if (big) null else floatingBorder(),
        contentPadding = PaddingValues(),
    ) {
        if (complexity == EventComplexity.Line) {
            Row(
                Modifier.fillMaxWidth().accentStripe(accentColor).heightIn(min = AgendaLineHeight).padding(horizontal = Spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (showsDate) InlineDate(dateText)
                Column(Modifier.weight(1f).padding(start = textStart)) {
                    AgendaTitleLine(event, preferredLanguages, changeLabel, fallbackTitle)
                    if (problem != null) IconLine(AppIcons.AlertTriangle, problem, color = MaterialTheme.colorScheme.error)
                    if (tags.isNotEmpty()) EventCardTags(tags)
                }
                if (showsSelectionMark && selection != EventCardSelection.NotSelecting) SelectionMark(selection == EventCardSelection.Selected)
            }
        } else {
            AgendaCardBox(big, splash, title, preferredLanguages, accentColor) {
                if (showsDate) DateColumn(dateText)
                Column(Modifier.weight(1f).padding(start = textStart)) {
                    AgendaTitleLine(event, preferredLanguages, changeLabel, fallbackTitle)
                    if (details.isNotEmpty() || status != null) TextAfterStatus(status, details, MaterialTheme.colorScheme.onSurfaceVariant, MaterialTheme.typography.bodyMedium)
                    if (problem != null) IconLine(AppIcons.AlertTriangle, problem, color = MaterialTheme.colorScheme.error)
                    if (tags.isNotEmpty()) EventCardTags(tags)
                }
                if (showsSelectionMark && selection != EventCardSelection.NotSelecting) SelectionMark(selection == EventCardSelection.Selected)
            }
        }
    }
}

@Composable
fun AgendaMarkerLine(marker: CalendarMarker, formatting: EventFormatting, modifier: Modifier = Modifier, showsDay: Boolean = true, onClick: () -> Unit) {
    val time = remember(marker, formatting) { (marker.at as? PartialDate.Time)?.let(formatting::formatTime) }
    val day = remember(marker, formatting, showsDay) { if (showsDay) formatting.formatShortDay(marker.at) else null }
    val style = MaterialTheme.typography.labelMedium
    val timeStyle = style.copy(fontWeight = FontWeight.Normal, fontFeatureSettings = TABULAR_DIGITS)
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val timeColumnWidth = remember(timeStyle, density) { with(density) { measurer.measure(WIDEST_CLOCK_TIME, timeStyle).size.width.toDp() } }
    val detailColor = MaterialTheme.colorScheme.onSurface.copy(alpha = AGENDA_DETAIL_ALPHA)
    Row(modifier.fillMaxWidth()) {
        Row(
            Modifier
                .clip(RoundedCornerShape(Radius.xs))
                .clickable(onClick = onClick)
                .heightIn(min = AGENDA_CHIP_HEIGHT)
                .padding(end = Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            if (time != null) Text(time, Modifier.widthIn(min = timeColumnWidth), color = detailColor, maxLines = 1, textAlign = TextAlign.End, style = timeStyle)
            Text(marker.label, Modifier.weight(1f, fill = false), color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold, maxLines = 1, style = style)
            if (day != null) Text(day, color = detailColor, maxLines = 1, style = style)
        }
    }
}

@Composable
private fun AgendaCardBox(
    full: Boolean,
    splash: Media?,
    title: String?,
    preferredLanguages: List<String>,
    accentColor: Color?,
    content: @Composable RowScope.() -> Unit,
) {
    Box(
        Modifier.fillMaxWidth().accentStripe(accentColor).then(if (full) Modifier.heightIn(min = AgendaCardHeight) else Modifier),
        propagateMinConstraints = full,
    ) {
        if (full) when {
            splash != null -> EventImageBackdrop(splash, preferredLanguages) { if (title != null) TitlePatternBackdrop(title) }
            title != null -> TitlePatternBackdrop(title)
        }
        Row(Modifier.fillMaxWidth().padding(Spacing.sm), verticalAlignment = if (full) Alignment.Bottom else Alignment.CenterVertically, content = content)
    }
}

@Composable
private fun AgendaTitleLine(event: ResolvedEvent, preferredLanguages: List<String>, changeLabel: String?, fallbackTitle: String?) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        EventTitle(event, preferredLanguages, Modifier.weight(1f, fill = false), style = MaterialTheme.typography.titleMedium, maxLines = AGENDA_TITLE_MAX_LINES, fallbackTitle = fallbackTitle)
        if (event.error != null) {
            Icon(AppIcons.AlertTriangle, Modifier.semantics { contentDescription = "Has an Error" }, size = IconSize.xs, tint = MaterialTheme.colorScheme.error)
        }
        if (changeLabel != null) {
            Text(changeLabel, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun DateColumn(text: DateBlockText?) {
    Column(Modifier.width(DATE_COLUMN_WIDTH), horizontalAlignment = Alignment.CenterHorizontally) {
        val dim = MaterialTheme.colorScheme.onSurfaceVariant
        text?.above?.let { Text(it.uppercase(), color = dim, maxLines = 1, style = MaterialTheme.typography.labelSmall) }
        val main = text?.main ?: "?"
        val mainStyle = if (main.length <= DAY_NUMBER_LENGTH) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleSmall
        Text(main, color = if (text == null) dim else MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, maxLines = 1, style = mainStyle)
        text?.below?.let { Text(it, color = dim, maxLines = 1, style = MaterialTheme.typography.labelSmall) }
    }
}

@Composable
private fun InlineDate(text: DateBlockText?) {
    Row(
        Modifier.widthIn(min = DATE_COLUMN_WIDTH),
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val dim = MaterialTheme.colorScheme.onSurfaceVariant
        text?.above?.let { Text(it.uppercase(), color = dim, maxLines = 1, style = MaterialTheme.typography.labelSmall) }
        Text(text?.main ?: "?", color = if (text == null) dim else MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, maxLines = 1, style = MaterialTheme.typography.titleSmall)
    }
}

@Composable
private fun BoxScope.TitlePatternBackdrop(title: String) {
    val measurer = rememberTextMeasurer()
    val style = TITLE_PATTERN_STYLE.copy(color = MaterialTheme.colorScheme.onSurface.copy(alpha = TITLE_PATTERN_ALPHA), fontFamily = MaterialTheme.typography.labelSmall.fontFamily)
    Box(
        Modifier
            .matchParentSize()
            .clipToBounds()
            .clearAndSetSemantics {}
            .drawWithCache {
                val unit = measurer.measure("${title.uppercase()} · ", style, softWrap = false, maxLines = 1)
                val rowHeight = TITLE_PATTERN_STYLE.lineHeight.toPx()
                val shifts = TITLE_PATTERN_ROW_SHIFTS.map { it.toPx() }
                val reach = hypot(size.width, size.height) / 2
                onDrawBehind {
                    rotate(TITLE_PATTERN_ANGLE, pivot = center) {
                        var y = center.y - reach
                        var row = 0
                        while (y < center.y + reach) {
                            var x = center.x - reach - shifts[row % shifts.size]
                            while (x < center.x + reach) {
                                drawText(unit, topLeft = Offset(x, y))
                                x += unit.size.width
                            }
                            y += rowHeight
                            row++
                        }
                    }
                }
            },
    )
}

@Composable
fun AgendaHeading(title: String, isCurrent: Boolean, modifier: Modifier = Modifier) {
    Row(modifier.padding(start = Spacing.xs, top = Spacing.md, bottom = Spacing.xs), verticalAlignment = Alignment.CenterVertically) {
        AgendaHeadingText(title, isCurrent)
    }
}

@Composable
fun RowScope.AgendaHeadingText(title: String, isCurrent: Boolean) {
    Text(title, maxLines = 1, style = MaterialTheme.typography.headlineSmall)
    if (isCurrent) {
        Box(
            Modifier
                .padding(start = Spacing.sm)
                .size(CURRENT_MONTH_DOT_SIZE)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
                .semantics { contentDescription = "This Month" },
        )
    }
}

@Composable
fun AgendaDayLabel(date: LocalDate, isToday: Boolean, formatting: EventFormatting, modifier: Modifier = Modifier) {
    AgendaLabelBox(modifier) { AgendaDayLabelText(date, isToday, formatting) }
}

@Composable
fun AgendaGroupLabel(title: String, modifier: Modifier = Modifier) {
    AgendaLabelBox(modifier) { AgendaGroupLabelText(title) }
}

@Composable
private fun AgendaLabelBox(modifier: Modifier, content: @Composable () -> Unit) {
    Box(modifier.fillMaxWidth().padding(top = AgendaLabelGap).heightIn(min = AgendaLabelHeight).padding(start = Spacing.xs), contentAlignment = Alignment.CenterStart) {
        content()
    }
}

@Composable
fun AgendaDayLabelText(date: LocalDate, isToday: Boolean, formatting: EventFormatting) {
    val weekdayColor = MaterialTheme.colorScheme.onSurfaceVariant
    val text = remember(date, formatting, weekdayColor) {
        val label = formatting.formatDayLabel(date)
        buildAnnotatedString {
            append(label.date)
            append(' ')
            withStyle(SpanStyle(color = weekdayColor, fontWeight = FontWeight.Normal)) { append(label.weekday) }
        }
    }
    AgendaLabelText(text, if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
}

@Composable
fun AgendaGroupLabelText(title: String) {
    AgendaLabelText(AnnotatedString(title), MaterialTheme.colorScheme.onSurface)
}

@Composable
private fun AgendaLabelText(text: AnnotatedString, color: Color) {
    MaterialText(text, color = color, fontWeight = FontWeight.Bold, maxLines = 1, style = MaterialTheme.typography.titleLarge)
}

@Composable
fun AgendaPeriodLine(period: CalendarPeriod, formatting: EventFormatting, modifier: Modifier = Modifier, showsStartDay: Boolean = true, onClick: () -> Unit) {
    val dates = remember(period, formatting, showsStartDay) {
        val first = formatting.localDateOrNull(period.start)
        val last = formatting.localDateOrNull(period.end)
        when {
            first == null || last == null -> null
            showsStartDay -> formatting.formatDayRange(first, last)
            else -> formatting.untilText(first, last)
        }
    }
    Row(modifier.fillMaxWidth()) {
        AgendaPeriodChip(period, dates, onClick = onClick)
    }
}

@Composable
fun FloatingAgendaPeriodChip(period: CalendarPeriod, formatting: EventFormatting, onClick: () -> Unit) {
    val dates = remember(period, formatting) {
        val first = formatting.localDateOrNull(period.start)
        val last = formatting.localDateOrNull(period.end)
        if (first == null || last == null) null else formatting.untilText(first, last)
    }
    val shape = RoundedCornerShape(Radius.xs)
    AgendaPeriodChip(period, dates, Modifier.floatingSurface(shape).border(floatingBorder(), shape), onClick)
}

@Composable
private fun AgendaPeriodChip(period: CalendarPeriod, dates: String?, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val color = MaterialTheme.colorScheme.tertiary
    Row(
        modifier
            .clip(RoundedCornerShape(Radius.xs))
            .clickable(onClick = onClick)
            .background(color.copy(alpha = AGENDA_CHIP_ALPHA))
            .heightIn(min = AGENDA_CHIP_HEIGHT)
            .padding(horizontal = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(AppIcons.DateRange, size = AgendaIconSize, tint = color)
        Text(period.label, Modifier.weight(1f, fill = false).padding(start = Spacing.xs), color = color, fontWeight = FontWeight.Bold, maxLines = 1, style = MaterialTheme.typography.labelMedium)
        if (dates != null) {
            Text(dates, Modifier.padding(start = Spacing.sm), color = MaterialTheme.colorScheme.onSurface.copy(alpha = AGENDA_DETAIL_ALPHA), maxLines = 1, style = MaterialTheme.typography.labelMedium)
        }
    }
}
