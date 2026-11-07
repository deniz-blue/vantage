package blue.deniz.vantage.app.screens

import blue.deniz.vantage.core.CalendarMarks
import blue.deniz.vantage.core.shownOccurrences
import blue.deniz.vantage.core.isCancelledOn


import blue.deniz.vantage.app.ui.Card

import blue.deniz.vantage.app.ui.AgendaMarkerLine

import blue.deniz.vantage.app.Route

import blue.deniz.vantage.core.CalendarPeriod

import blue.deniz.vantage.core.CalendarMarker

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text as MaterialText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import blue.deniz.vantage.app.collectAcrossSession
import blue.deniz.vantage.app.latestAcrossSession
import blue.deniz.vantage.app.AppState
import blue.deniz.vantage.app.ui.ListPlaceholder
import blue.deniz.vantage.app.ui.SheetTitle
import blue.deniz.vantage.app.ui.AppIcons
import blue.deniz.vantage.app.ui.Button
import blue.deniz.vantage.app.ui.BottomSheet
import blue.deniz.vantage.app.collectAcrossNavigation
import blue.deniz.vantage.app.ui.EventFormatting
import blue.deniz.vantage.app.ui.IconButton
import blue.deniz.vantage.app.ui.Spacing
import blue.deniz.vantage.app.ui.Text
import blue.deniz.vantage.app.ui.contentColorOn
import blue.deniz.vantage.app.ui.translatedName
import blue.deniz.vantage.core.CalendarGrid
import blue.deniz.vantage.core.DaySpan
import blue.deniz.vantage.core.ResolvedEvent
import blue.deniz.vantage.app.EventCardStyle
import blue.deniz.vantage.app.ui.FailureRow
import blue.deniz.vantage.app.ui.LoadingSpinner
import blue.deniz.vantage.core.EventCollection
import blue.deniz.vantage.core.EventListing
import blue.deniz.vantage.core.EventListingStatus
import blue.deniz.vantage.core.EventOrder
import blue.deniz.vantage.core.EventQuery
import directory.evnt.PartialDate
import kotlinx.coroutines.launch
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.format.MonthNames
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import blue.deniz.vantage.app.SelectAllShortcut

private const val MAX_DAYS_PER_OCCURRENCE = 62
private const val MAX_EVENTS_PER_MONTH = 500
private const val OUTSIDE_MONTH_ALPHA = 0.5f
private val WEEKDAY_LABELS = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
private const val FIRST_CALENDAR_YEAR = 1900
private const val CALENDAR_MONTH_COUNT = 300 * 12
private val DAY_NUMBER_SIZE = 22.dp
private val DAY_NUMBER_AREA = 28.dp
private val BAR_HEIGHT = 15.dp
private val BAR_GAP = 2.dp
private val BAR_INSET = 2.dp
private val BAR_RADIUS = 4.dp
private val BAR_TEXT_SIZE = 11.sp
private val PERIOD_BAND_HEIGHT = 15.dp
private const val MAX_PERIOD_ROWS = 2
private const val PERIOD_FILL_ALPHA = 0.08f
private const val PERIOD_BAND_ALPHA = 0.22f

private sealed interface CalendarBarItem {
    data class Event(val event: ResolvedEvent, val cancelled: Boolean) : CalendarBarItem
    data class Marker(val marker: CalendarMarker) : CalendarBarItem
}

private fun EventFormatting.localDaysOf(start: PartialDate, end: PartialDate?): List<LocalDate> {
    fun localDate(date: PartialDate): LocalDate? = when (date) {
        is PartialDate.Time -> localDateOf(date)
        is PartialDate.Day -> date.toLocalDate()
        else -> null
    }
    val first = localDate(start) ?: return emptyList()
    val last = end?.let { localDate(it) }?.takeIf { it >= first } ?: first
    return generateSequence(first) { it.plus(DatePeriod(days = 1)) }
        .takeWhile { it <= last }
        .take(MAX_DAYS_PER_OCCURRENCE)
        .toList()
}

private fun EventFormatting.occurrenceSpans(events: List<ResolvedEvent>): List<DaySpan<CalendarBarItem>> =
    events.flatMap { event ->
        event.data?.occurrences.orEmpty().mapNotNull { occurrence ->
            val days = occurrence.start?.let { localDaysOf(it, occurrence.end) }.orEmpty()
            val cancelled = event.data?.isCancelledOn(occurrence) == true
            if (days.isEmpty()) null else DaySpan<CalendarBarItem>(CalendarBarItem.Event(event, cancelled), days.first(), days.last())
        }.distinct()
    }

private fun EventFormatting.markerSpans(markers: List<CalendarMarker>): List<DaySpan<CalendarBarItem>> =
    markers.mapNotNull { marker -> localDateOrNull(marker.at)?.let { DaySpan(CalendarBarItem.Marker(marker), it, it) } }

internal fun EventFormatting.periodSpans(periods: List<CalendarPeriod>): List<DaySpan<CalendarPeriod>> =
    periods.mapNotNull { period ->
        val first = localDateOrNull(period.start) ?: return@mapNotNull null
        val last = localDateOrNull(period.end) ?: return@mapNotNull null
        DaySpan(period, first, maxOf(first, last))
    }

private fun EventFormatting.eventsByDay(events: List<ResolvedEvent>): Map<LocalDate, List<ResolvedEvent>> {
    val days = mutableMapOf<LocalDate, MutableList<ResolvedEvent>>()
    for (event in events) {
        val eventDays = event.data?.occurrences.orEmpty()
            .flatMap { occurrence -> occurrence.start?.let { localDaysOf(it, occurrence.end) }.orEmpty() }
            .toSet()
        for (day in eventDays) days.getOrPut(day) { mutableListOf() } += event
    }
    return days
}

@Stable
class CalendarSelection(today: LocalDate) {
    var month by mutableStateOf(today.firstOfMonth())
        private set
    var day by mutableStateOf(today)
        private set
    var openedDay by mutableStateOf<LocalDate?>(null)
        private set

    fun showMonth(firstDay: LocalDate) {
        month = firstDay.firstOfMonth()
    }

    fun showPreviousMonth() = showMonth(month.minus(DatePeriod(months = 1)))

    fun showNextMonth() = showMonth(month.plus(DatePeriod(months = 1)))

    fun select(selected: LocalDate) {
        day = selected
        month = selected.firstOfMonth()
    }

    fun openDay(opened: LocalDate) {
        select(opened)
        openedDay = opened
    }

    fun closeOpenedDay() {
        openedDay = null
    }
}

private fun LocalDate.firstOfMonth() = LocalDate(year, month, 1)

private fun calendarPageOf(month: LocalDate): Int =
    ((month.year - FIRST_CALENDAR_YEAR) * 12 + month.month.ordinal).coerceIn(0, CALENDAR_MONTH_COUNT - 1)

private fun monthOfCalendarPage(page: Int): LocalDate =
    LocalDate(FIRST_CALENDAR_YEAR + page / 12, Month(page % 12 + 1), 1)

@Composable
private fun rememberMonthPager(selection: CalendarSelection): PagerState {
    val pager = rememberPagerState(initialPage = calendarPageOf(selection.month)) { CALENDAR_MONTH_COUNT }
    LaunchedEffect(pager) {
        var swiped = false
        launch { pager.interactionSource.interactions.collect { if (it is DragInteraction.Start) swiped = true } }
        snapshotFlow { pager.isScrollInProgress }.collect { scrolling ->
            if (!scrolling && swiped) {
                swiped = false
                selection.showMonth(monthOfCalendarPage(pager.currentPage))
            }
        }
    }
    val requestedPage = calendarPageOf(selection.month)
    LaunchedEffect(requestedPage) {
        if (pager.currentPage != requestedPage || pager.isScrollInProgress) pager.animateScrollToPage(requestedPage)
    }
    return pager
}

@Composable
fun CalendarView(
    state: AppState,
    screen: String,
    events: EventCollection,
    marks: CalendarMarks,
    actions: EventCardActions,
    cards: EventCardData,
    topPadding: Dp,
    filtersButton: (@Composable () -> Unit)?,
    onNewOnDay: ((LocalDate) -> Unit)?,
    viewPicker: (@Composable () -> Unit)? = null,
) {
    val today = state.today
    val selection = state.calendarSelection
    val pager = rememberMonthPager(selection)
    val shownMonth = monthOfCalendarPage(pager.currentPage)

    Column(Modifier.fillMaxSize().padding(top = topPadding)) {
        Row(Modifier.fillMaxWidth().padding(start = Spacing.md, end = Spacing.xs), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "${MonthNames.ENGLISH_FULL.names[shownMonth.month.ordinal]} ${shownMonth.year}",
                Modifier.weight(1f),
                maxLines = 1,
                style = MaterialTheme.typography.titleLarge,
            )
            filtersButton?.invoke()
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = Spacing.xs)) {
            for (label in WEEKDAY_LABELS) {
                Box(Modifier.weight(1f).padding(bottom = Spacing.xs), contentAlignment = Alignment.Center) {
                    Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
        HorizontalPager(
            state = pager,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            beyondViewportPageCount = 1,
            verticalAlignment = Alignment.Top,
        ) { page ->
            MonthPage(
                state = state,
                screen = screen,
                events = events,
                marks = marks,
                month = monthOfCalendarPage(page),
                today = today,
                selectedDay = selection.day,
                cards = cards,
                onOpenDay = selection::openDay,
                modifier = Modifier.fillMaxSize().padding(horizontal = Spacing.xs),
            )
        }
        Row(Modifier.fillMaxWidth().padding(start = Spacing.xs, end = Spacing.md, top = Spacing.xs, bottom = Spacing.sm), verticalAlignment = Alignment.CenterVertically) {
            IconButton(AppIcons.ChevronLeft, "Previous Month", onClick = selection::showPreviousMonth)
            IconButton(AppIcons.ChevronRight, "Next Month", onClick = selection::showNextMonth)
            AnimatedVisibility(shownMonth != today.firstOfMonth(), enter = fadeIn(), exit = fadeOut()) {
                TextButton(onClick = { selection.select(today) }) { Text("Today") }
            }
            Spacer(Modifier.weight(1f))
            viewPicker?.invoke()
        }
    }
    DaySheet(state, screen, events, marks, actions, cards, onNewOnDay)
}

@Composable
private fun DaySheet(
    state: AppState,
    screen: String,
    events: EventCollection,
    marks: CalendarMarks,
    actions: EventCardActions,
    cards: EventCardData,
    onNewOnDay: ((LocalDate) -> Unit)?,
) {
    val selection = state.calendarSelection
    BottomSheet(item = selection.openedDay, onDismiss = selection::closeOpenedDay) { day ->
        val listing = latestAcrossSession<EventListing>(monthListingKey(screen, day.firstOfMonth()))
            ?.takeIf { it.status != EventListingStatus.HasMore }
            ?: run {
                val dayQuery = remember(day, state.timeZone) { daysQuery(day, day, state.timeZone) }
                collectAcrossNavigation("$screen day events", listOf(events, dayQuery)) { events.list(dayQuery, MAX_EVENTS_PER_MONTH) }
            }
        val dayEvents = remember(listing, cards.formatting, day) { cards.formatting.eventsByDay(listing?.events.orEmpty())[day].orEmpty() }
        val dayPeriods = remember(marks.periods, cards.formatting, day) { cards.formatting.periodSpans(marks.periods).filter { day in it } }
        val dayMarkers = remember(marks.markers, cards.formatting, day) { marks.markers.filter { cards.formatting.localDateOrNull(it.at) == day } }
        SelectAllShortcut(state) { actions.selectAll(dayEvents) }

        if (actions.isSelecting) {
            Box(Modifier.padding(horizontal = Spacing.sm)) { actions.SelectionBar(dayEvents) }
        } else {
            Row(Modifier.fillMaxWidth().padding(horizontal = Spacing.md), verticalAlignment = Alignment.CenterVertically) {
                SheetTitle(cards.formatting.formatDay(day), Modifier.weight(1f))
                if (onNewOnDay != null) Button("New", onClick = { onNewOnDay(day) }, leadingIcon = AppIcons.Plus)
            }
        }
        Column(Modifier.fillMaxWidth().padding(horizontal = Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            when (val status = listing?.status) {
                null, is EventListingStatus.Loading -> LoadingSpinner()
                is EventListingStatus.Failed -> FailureRow("Couldn't load events: ${status.message}", onRetry = null)
                EventListingStatus.HasMore, EventListingStatus.Complete -> if (dayEvents.isEmpty() && dayPeriods.isEmpty() && dayMarkers.isEmpty()) ListPlaceholder("No events")
            }
            for (span in dayPeriods) {
                PeriodDayRow(span.item.label, span.dayNumberOf(day), span.dayCount) {
                    state.openMarkSheet(MarkSheetTarget(MarkKind.Period, span.item.id))
                }
            }
            for (marker in dayMarkers) {
                AgendaMarkerLine(marker, cards.formatting) { state.openMarkSheet(MarkSheetTarget(MarkKind.Marker, marker.id)) }
            }
            for (event in dayEvents) {
                StyledEventCard(event, EventCardStyle.Rows, listing, dayEvents, actions, cards, minHeight = 0.dp, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

private fun monthListingKey(screen: String, month: LocalDate) = "$screen month $month"

@Composable
private fun MonthPage(
    state: AppState,
    screen: String,
    events: EventCollection,
    marks: CalendarMarks,
    month: LocalDate,
    today: LocalDate,
    selectedDay: LocalDate,
    cards: EventCardData,
    onOpenDay: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val formatting = cards.formatting
    val weeks = remember(month) { CalendarGrid.weeksOf(month) }
    val query = remember(weeks, state.timeZone) { daysQuery(weeks.first().first(), weeks.last().last(), state.timeZone) }
    val listing = collectAcrossSession(monthListingKey(screen, month), listOf(events, query)) { events.list(query, MAX_EVENTS_PER_MONTH) }
    val spans = remember(listing, marks.markers, formatting) { formatting.occurrenceSpans(listing?.events.orEmpty()) + formatting.markerSpans(marks.markers) }
    val periodSpans = remember(marks.periods, formatting) { formatting.periodSpans(marks.periods) }
    val eventsOpacity by animateFloatAsState(if (listing != null && listing.status !is EventListingStatus.Loading) 1f else 0f, label = "month events opacity")
    Column(modifier) {
        for (week in weeks) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            WeekRow(
                state = state,
                week = week,
                month = month,
                today = today,
                selectedDay = selectedDay,
                spans = spans,
                periodSpans = periodSpans,
                eventsOpacity = { eventsOpacity },
                cards = cards,
                onOpenDay = onOpenDay,
                modifier = Modifier.weight(1f).fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun WeekRow(
    state: AppState,
    week: List<LocalDate>,
    month: LocalDate,
    today: LocalDate,
    selectedDay: LocalDate,
    spans: List<DaySpan<CalendarBarItem>>,
    periodSpans: List<DaySpan<CalendarPeriod>>,
    eventsOpacity: () -> Float,
    cards: EventCardData,
    onOpenDay: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier.pointerInput(week) {
            detectTapGestures { tap ->
                val column = (tap.x / (size.width.toFloat() / CalendarGrid.DAYS_IN_WEEK)).toInt()
                onOpenDay(week[column.coerceIn(0, CalendarGrid.DAYS_IN_WEEK - 1)])
            }
        },
    ) {
        val columnWidth = maxWidth / CalendarGrid.DAYS_IN_WEEK
        val rowHeight = maxHeight
        val periods = remember(week, periodSpans) { CalendarGrid.layOutWeek(week, periodSpans, MAX_PERIOD_ROWS) }
        val periodRows = periods.bars.maxOfOrNull { it.row + 1 } ?: 0
        val rows = ((maxHeight - DAY_NUMBER_AREA - PERIOD_BAND_HEIGHT * periodRows) / (BAR_HEIGHT + BAR_GAP)).toInt().coerceAtLeast(1)
        val layout = remember(week, spans, rows) { CalendarGrid.layOutWeek(week, spans, rows) }
        val periodColor = MaterialTheme.colorScheme.tertiary
        for (band in periods.bars) {
            Box(
                Modifier
                    .offset(x = columnWidth * band.firstColumn)
                    .width(columnWidth * (band.lastColumn - band.firstColumn + 1))
                    .fillMaxHeight()
                    .background(periodColor.copy(alpha = PERIOD_FILL_ALPHA)),
            )
        }
        Row(Modifier.fillMaxSize()) {
            for ((column, day) in week.withIndex()) {
                val label = cards.formatting.formatDay(day)
                val hidden = layout.hiddenCounts[column]
                DayNumber(
                    day = day,
                    inMonth = day.month == month.month,
                    isToday = day == today,
                    isSelected = day == selectedDay,
                    modifier = Modifier.weight(1f).fillMaxHeight().clearAndSetSemantics {
                        contentDescription = if (layout.bars.any { column in it.firstColumn..it.lastColumn } || hidden > 0) "$label, Has Events" else label
                        selected = day == selectedDay
                        role = Role.Button
                        onClick { onOpenDay(day); true }
                    },
                )
            }
        }
        Box(Modifier.fillMaxSize().graphicsLayer { alpha = eventsOpacity() }) {
            for (bar in layout.bars) {
                val start = columnWidth * bar.firstColumn + if (bar.continuesBefore) 0.dp else BAR_INSET
                val end = columnWidth * (bar.lastColumn + 1) - if (bar.continuesAfter) 0.dp else BAR_INSET
                val placement = Modifier
                    .offset(x = start, y = barTop(bar.row))
                    .width(end - start)
                    .height(BAR_HEIGHT)
                when (val item = bar.item) {
                    is CalendarBarItem.Event -> EventBar(
                        title = item.event.data?.translatedName(state.preferredLanguages).orEmpty(),
                        color = cards.accentColorOf(item.event) ?: MaterialTheme.colorScheme.primary,
                        continuesBefore = bar.continuesBefore,
                        continuesAfter = bar.continuesAfter,
                        cancelled = item.cancelled,
                        modifier = placement,
                    )
                    is CalendarBarItem.Marker -> MarkerBar(item.marker.label, placement)
                }
            }
            for (band in periods.bars) {
                val start = columnWidth * band.firstColumn + if (band.continuesBefore) 0.dp else BAR_INSET
                val end = columnWidth * (band.lastColumn + 1) - if (band.continuesAfter) 0.dp else BAR_INSET
                PeriodLabel(
                    label = band.item.label,
                    color = periodColor,
                    modifier = Modifier
                        .offset(x = start, y = rowHeight - PERIOD_BAND_HEIGHT * (band.row + 1))
                        .width(end - start)
                        .height(PERIOD_BAND_HEIGHT - BAR_GAP),
                )
            }
            for ((column, hidden) in layout.hiddenCounts.withIndex()) {
                if (hidden == 0) continue
                Box(
                    Modifier
                        .offset(x = columnWidth * column + BAR_INSET * 2, y = barTop(rows - 1))
                        .height(BAR_HEIGHT),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    MaterialText(
                        "+$hidden",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = BAR_TEXT_SIZE,
                        lineHeight = BAR_TEXT_SIZE,
                        maxLines = 1,
                        softWrap = false,
                    )
                }
            }
        }
    }
}

private fun barTop(row: Int): Dp = DAY_NUMBER_AREA + (BAR_HEIGHT + BAR_GAP) * row

@Composable
private fun DayNumber(day: LocalDate, inMonth: Boolean, isToday: Boolean, isSelected: Boolean, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val textColor = when {
        isToday -> colors.onPrimary
        inMonth -> colors.onSurface
        else -> colors.onSurfaceVariant.copy(alpha = OUTSIDE_MONTH_ALPHA)
    }
    Box(modifier.padding(top = Spacing.xs), contentAlignment = Alignment.TopCenter) {
        Box(
            Modifier
                .size(DAY_NUMBER_SIZE)
                .background(if (isToday) colors.primary else Color.Transparent, CircleShape)
                .then(if (isSelected && !isToday) Modifier.border(1.dp, colors.primary, CircleShape) else Modifier),
            contentAlignment = Alignment.Center,
        ) {
            Text(day.day.toString(), color = textColor, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun EventBar(
    title: String,
    color: Color,
    continuesBefore: Boolean,
    continuesAfter: Boolean,
    cancelled: Boolean,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(
        topStart = if (continuesBefore) 0.dp else BAR_RADIUS,
        bottomStart = if (continuesBefore) 0.dp else BAR_RADIUS,
        topEnd = if (continuesAfter) 0.dp else BAR_RADIUS,
        bottomEnd = if (continuesAfter) 0.dp else BAR_RADIUS,
    )
    Box(modifier.clip(shape).background(if (cancelled) color.copy(alpha = 0.3f) else color).padding(horizontal = Spacing.xs), contentAlignment = Alignment.CenterStart) {
        MaterialText(
            title,
            color = if (cancelled) MaterialTheme.colorScheme.onSurfaceVariant else contentColorOn(color),
            fontSize = BAR_TEXT_SIZE,
            lineHeight = BAR_TEXT_SIZE,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Clip,
            textDecoration = if (cancelled) androidx.compose.ui.text.style.TextDecoration.LineThrough else null,
        )
    }
}

@Composable
private fun MarkerBar(label: String, modifier: Modifier = Modifier) {
    Box(modifier.padding(horizontal = Spacing.xs), contentAlignment = Alignment.CenterStart) {
        MaterialText(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = BAR_TEXT_SIZE, lineHeight = BAR_TEXT_SIZE, maxLines = 1, softWrap = false, overflow = TextOverflow.Clip)
        Box(Modifier.align(Alignment.BottomStart).fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outline))
    }
}

@Composable
private fun PeriodLabel(label: String, color: Color, modifier: Modifier = Modifier) {
    Box(modifier.padding(horizontal = Spacing.xs), contentAlignment = Alignment.CenterStart) {
        MaterialText(label, color = color, fontSize = BAR_TEXT_SIZE, lineHeight = BAR_TEXT_SIZE, maxLines = 1, softWrap = false, overflow = TextOverflow.Clip)
    }
}

@Composable
internal fun PeriodDayRow(label: String, day: Int, days: Int, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth(), background = MaterialTheme.colorScheme.tertiary.copy(alpha = PERIOD_BAND_ALPHA), onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Text(label, Modifier.weight(1f), maxLines = 1, style = MaterialTheme.typography.titleMedium)
            Text("Day $day of $days", color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, style = MaterialTheme.typography.labelLarge)
        }
    }
}

private fun daysQuery(first: LocalDate, last: LocalDate, timeZone: TimeZone) = EventQuery(
    from = first.atStartOfDayIn(timeZone),
    until = last.plus(DatePeriod(days = 1)).atStartOfDayIn(timeZone),
    order = EventOrder.Start,
)
