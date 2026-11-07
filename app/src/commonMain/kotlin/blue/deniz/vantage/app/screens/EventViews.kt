package blue.deniz.vantage.app.screens

import blue.deniz.vantage.app.ui.growsIntoEventPage
import androidx.compose.ui.graphics.graphicsLayer
import blue.deniz.vantage.app.ui.backdropSource
import blue.deniz.vantage.app.ui.LocalBackdrop
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import blue.deniz.vantage.app.ui.AgendaPeriodLine
import blue.deniz.vantage.core.CalendarPeriod
import blue.deniz.vantage.core.CalendarMarks

import blue.deniz.vantage.app.Route

import blue.deniz.vantage.app.ui.AgendaMarkerLine

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import blue.deniz.vantage.app.AppState
import blue.deniz.vantage.app.EventCardStyle
import blue.deniz.vantage.app.EventsView
import blue.deniz.vantage.app.timeShown
import blue.deniz.vantage.app.ui.shownOccurrence
import blue.deniz.vantage.app.ui.lazyGridScrollStop
import blue.deniz.vantage.app.ui.rememberLazyGridScrollStop
import blue.deniz.vantage.app.SelectAllShortcut
import blue.deniz.vantage.app.ui.AgendaEventCard
import blue.deniz.vantage.app.ui.AgendaHeading
import blue.deniz.vantage.app.ui.AgendaGroupLabel
import blue.deniz.vantage.app.ui.AgendaLabelGap
import blue.deniz.vantage.app.ui.AgendaLabelHeight
import blue.deniz.vantage.app.ui.AgendaDayLabel
import blue.deniz.vantage.app.ui.eventCardColumns
import blue.deniz.vantage.app.ui.EventCard
import blue.deniz.vantage.app.ui.EventCardGrid
import blue.deniz.vantage.app.ui.EventCardHeight
import blue.deniz.vantage.app.ui.AppIcons
import blue.deniz.vantage.app.ui.EventMap
import blue.deniz.vantage.app.ui.MapCamera
import blue.deniz.vantage.app.ui.MY_LOCATION_ZOOM
import blue.deniz.vantage.app.ui.Spacing
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import blue.deniz.vantage.app.ui.EventPosterCard
import blue.deniz.vantage.app.ui.EventTicketCard
import blue.deniz.vantage.app.ui.FailureRow
import blue.deniz.vantage.app.ui.ListPlaceholder
import blue.deniz.vantage.app.ui.LoadingSpinner
import blue.deniz.vantage.app.ui.SegmentedControl
import blue.deniz.vantage.app.ui.rememberHeldKeys
import blue.deniz.vantage.core.EventCollection
import blue.deniz.vantage.core.EventListing
import blue.deniz.vantage.core.EventListingStatus
import blue.deniz.vantage.core.EventOrder
import blue.deniz.vantage.core.EventQuery
import blue.deniz.vantage.core.ResolvedEvent
import blue.deniz.vantage.core.TimeFilter
import blue.deniz.vantage.core.toQuery
import kotlinx.datetime.LocalDate

private val DAY_LINE_GUTTER = 12.dp
private const val DAY_DOT_ALPHA = 0.6f

@Composable
fun EventsViewContent(
    state: AppState,
    screen: String,
    view: EventsView,
    events: EventCollection,
    search: String,
    time: TimeFilter,
    order: EventOrder,
    actions: EventCardActions,
    topPadding: Dp,
    bottomPadding: Dp,
    emptyText: String = "No events found",
    listFooter: (LazyGridScope.(EventListing?) -> Unit)? = null,
    filtersButton: (@Composable () -> Unit)? = null,
    onNewOnDay: ((LocalDate) -> Unit)? = null,
    showsMarkersAndPeriods: Boolean = false,
    bottomEndClearance: Dp = 0.dp,
    calendarViewPicker: (@Composable () -> Unit)? = null,
) {
    val cards = rememberEventCardData(state)
    val marks = rememberCalendarMarks(state, showsMarkersAndPeriods)
    when (view) {
        EventsView.List -> {
            val query = remember(search, time, order, state.listsSplitAt) { view.timeShown(time).toQuery(state.listsSplitAt, search, order) }
            EventListView(state, screen, events, query, marks, actions, cards, topPadding, bottomPadding + bottomEndClearance, emptyText, listFooter)
        }
        EventsView.Calendar -> Box(Modifier.padding(bottom = bottomPadding)) {
            CalendarView(state, screen, events, marks, actions, cards, topPadding, filtersButton, onNewOnDay, calendarViewPicker)
        }
        EventsView.Map -> {
            val query = remember(time, state.listsSplitAt) { time.toQuery(state.listsSplitAt) }
            val currentActions by rememberUpdatedState(actions)
            EventMap(
                events,
                query,
                start = state.settings.myLocation?.let { MapCamera(it.latitude, it.longitude, MY_LOCATION_ZOOM) },
                onOpenEvent = { currentActions.open(it) },
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = topPadding, bottom = bottomPadding + bottomEndClearance),
            )
        }
    }
}

@Composable
private fun EventListView(
    state: AppState,
    screen: String,
    events: EventCollection,
    query: EventQuery,
    marks: CalendarMarks,
    actions: EventCardActions,
    cards: EventCardData,
    topPadding: Dp,
    bottomPadding: Dp,
    emptyText: String,
    footer: (LazyGridScope.(EventListing?) -> Unit)?,
) {
    val paged = rememberPagedListing("$screen list", events, query)
    val grid = rememberLazyGridState()
    val pastQuery = remember(query) { query.from?.takeIf { query.order == EventOrder.Start }?.let { EventQuery(query.search, endedBy = it, order = EventOrder.StartDescending) } }
    val past = if (pastQuery != null) rememberPagedListing("$screen past", events, pastQuery) else null
    val scrollStop = rememberLazyGridScrollStop(grid, resetKey = query, ready = paged.listing != null && (past == null || past.listing != null))
    val pastShown = remember(past?.events, paged.events, paged.listing == null) {
        val upcomingKeys = paged.events.mapTo(HashSet()) { it.listKey() }
        if (paged.listing == null) emptyList() else past?.events.orEmpty().asReversed().filter { it.listKey() !in upcomingKeys }
    }
    val shown = remember(pastShown, paged.events) { pastShown + paged.events }
    val cardListing = remember(paged.listing, past?.listing, shown) {
        val pastListing = past?.listing
        if (pastListing == null) paged.listing else paged.listing?.copy(events = shown, listedNames = pastListing.listedNames + paged.listing.listedNames)
    }
    SelectAllShortcut(state) { actions.selectAll(shown) }
    LoadMoreNearTheEnd(paged) { grid.layoutInfo.let { it.totalItemsCount - (it.visibleItemsInfo.lastOrNull()?.index ?: 0) } }
    past?.let { LoadMoreNearTheEnd(it) { grid.firstVisibleItemIndex } }
    val inDateOrder = query.order == EventOrder.Start
    val showsMarks = inDateOrder && query.search.isBlank()
    val now = cards.now
    val marksFrom = when {
        past?.listing == null -> query.from
        past.listing.status == EventListingStatus.Complete -> null
        else -> shown.firstOrNull()?.shownOccurrence(now)?.start?.startInstant ?: query.from
    }
    val markers = remember(marks, marksFrom, query, showsMarks) { if (showsMarks) marks.markersBetween(marksFrom, query.until) else emptyList() }
    val periods = remember(marks, marksFrom, query, showsMarks) { if (showsMarks) marks.periodsOverlapping(marksFrom, query.until) else emptyList() }
    val complete = paged.listing?.status == EventListingStatus.Complete
    val margin = Spacing.md + DAY_LINE_GUTTER
    val marginPx = with(LocalDensity.current) { margin.toPx() }
    val markerIcon = rememberVectorPainter(AppIcons.Label)
    val guideColors = AgendaGuideColors(
        marker = MaterialTheme.colorScheme.onSurface,
        pastMarker = MaterialTheme.colorScheme.onSurfaceVariant,
        line = MaterialTheme.colorScheme.outlineVariant,
        dot = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = DAY_DOT_ALPHA),
        today = MaterialTheme.colorScheme.primary,
        background = MaterialTheme.colorScheme.background,
    )
    val checkColors = CheckColors(MaterialTheme.colorScheme.onSurfaceVariant, MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.onPrimary)
    val eventsByKey = remember(shown) { shown.associateBy { it.listKey() } }
    val currentEventsByKey by rememberUpdatedState(eventsByKey)
    val currentShown by rememberUpdatedState(shown)
    val currentActions by rememberUpdatedState(actions)
    val currentTopPadding by rememberUpdatedState(topPadding)
    val haptics = LocalHapticFeedback.current
    val eventAt = { position: Offset -> eventAt(grid.layoutInfo, currentEventsByKey, position) }
    val backdropSource = Modifier.backdropSource(LocalBackdrop.current)
    BoxWithConstraints(Modifier.fillMaxSize().lazyGridScrollStop(scrollStop)) {
        val dayLabels = eventCardColumns(maxWidth, DAY_LINE_GUTTER) == 1
        val agenda = remember(shown, pastShown.size, past != null, cards, markers, periods, complete, dayLabels) {
            agendaOf(shown, cards, markers, periods, marksAfterEvents = complete, dayLabels = dayLabels, upcomingFrom = pastShown.size.takeIf { past != null })
        }
        val stopIndex = agenda.upcomingStartRow
        SideEffect { scrollStop.stopIndex = stopIndex }
        val labelGapPx = with(LocalDensity.current) { AgendaLabelGap.roundToPx() }
        val pinnedLabelRow = remember(grid, agenda, labelGapPx) {
            derivedStateOf {
                val top = grid.layoutInfo.visibleItemsInfo.lastOrNull { item ->
                    val row = agenda.rowIndexOf(item.key)?.takeIf { it >= scrollStop.firstReachableIndex } ?: return@lastOrNull false
                    item.offset.y + (if (agenda.rows[row] is AgendaRow.Label) labelGapPx else 0) < 0
                }
                val row = top?.let { agenda.rowIndexOf(it.key) } ?: return@derivedStateOf null
                (row downTo 0).firstOrNull { agenda.rows[it] is AgendaRow.Label }
            }
        }
        val headingPillHeight = with(LocalDensity.current) { MaterialTheme.typography.headlineSmall.lineHeight.toDp() } + Spacing.xs * 2
        val stuckPeriodsTop = if (dayLabels) AgendaLabelHeight + Spacing.xs else Spacing.xs + headingPillHeight + Spacing.xs
        val periodSlotPx = with(LocalDensity.current) { stuckPeriodsTop.roundToPx() }
        val stuckPeriods = remember(grid, agenda, periodSlotPx, pinnedLabelRow) {
            derivedStateOf {
                if (dayLabels && pinnedLabelRow.value == null) return@derivedStateOf emptyList()
                val under = grid.layoutInfo.visibleItemsInfo.lastOrNull { item ->
                    item.offset.y < periodSlotPx && agenda.rowIndexOf(item.key)?.takeIf { it >= scrollStop.firstReachableIndex } != null
                }
                under?.let { agenda.rowIndexOf(it.key) }?.let(agenda::periodsRunningAt).orEmpty()
            }
        }
        val hiddenWhilePinned = { row: Int -> Modifier.graphicsLayer { alpha = if (pinnedLabelRow.value == row) 0f else 1f } }
        val hiddenWhileStuck = { period: CalendarPeriod -> Modifier.graphicsLayer { alpha = if (period in stuckPeriods.value) 0f else 1f } }
        val decoration = { columns: Int ->
            backdropSource
                .drawBehind {
                    val layout = grid.layoutInfo
                    when {
                        actions.isSelecting && columns == 1 -> drawSelectionChecks(layout, eventsByKey, actions, margin, checkColors)
                        inDateOrder -> drawAgendaGuides(layout, agenda, cards.now, markerIcon, guideColors, pinnedLabelRow.value)
                    }
                }
                .selectsByDragging(grid, { currentTopPadding }, { if (columns == 1) marginPx else null }, eventAt, { currentShown }, { currentActions }) {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                }
        }
        EventCardGrid(grid, topPadding = topPadding, bottomPadding = bottomPadding, startGutter = DAY_LINE_GUTTER, decoration = decoration) { cardMinHeight ->
            if (inDateOrder) {
                for ((index, row) in agenda.rows.withIndex()) when (row) {
                    is AgendaRow.Heading -> item(key = row.key, span = { GridItemSpan(maxLineSpan) }) { AgendaHeading(row.title, row.isCurrent, Modifier.animateItem()) }
                    is AgendaRow.Day -> item(key = row.key, span = { GridItemSpan(maxLineSpan) }) {
                        AgendaDayLabel(row.date, row.isToday, cards.formatting, Modifier.animateItem().then(hiddenWhilePinned(index)))
                    }
                    is AgendaRow.Group -> item(key = row.key, span = { GridItemSpan(maxLineSpan) }) {
                        AgendaGroupLabel(row.title, Modifier.animateItem().then(hiddenWhilePinned(index)))
                    }
                    is AgendaRow.Event -> item(key = row.key) {
                        StyledEventCard(
                            row.event, EventCardStyle.Agenda, cardListing, shown, actions, cards, cardMinHeight, Modifier.animateItem(),
                            showsDate = !dayLabels, longPressSelects = false, selectionMarkInside = cardMinHeight > 0.dp,
                        )
                    }
                    is AgendaRow.Marker -> item(key = row.key, span = { GridItemSpan(maxLineSpan) }) {
                        AgendaMarkerLine(row.marker, cards.formatting, Modifier.animateItem(), showsDay = !dayLabels) {
                            state.openMarkSheet(MarkSheetTarget(MarkKind.Marker, row.marker.id))
                        }
                    }
                    is AgendaRow.PeriodStart -> item(key = row.key, span = { GridItemSpan(maxLineSpan) }) {
                        AgendaPeriodLine(row.period, cards.formatting, Modifier.animateItem().then(hiddenWhileStuck(row.period)), showsStartDay = !dayLabels) {
                            state.openMarkSheet(MarkSheetTarget(MarkKind.Period, row.period.id))
                        }
                    }
                }
            } else {
                items(shown, key = { it.listKey() }) { event ->
                    StyledEventCard(
                        event, EventCardStyle.Agenda, cardListing, shown, actions, cards, cardMinHeight, Modifier.animateItem(),
                        longPressSelects = false, selectionMarkInside = cardMinHeight > 0.dp,
                    )
                }
            }
            if (paged.isEmpty && (!inDateOrder || agenda.rows.drop(stopIndex).all { it is AgendaRow.Label || it is AgendaRow.Heading })) {
                item(key = "no events", span = { GridItemSpan(maxLineSpan) }) { ListPlaceholder(emptyText, Modifier.animateItem()) }
            }
            listingStatusItem(paged.listing, paged.retry)
            footer?.invoke(this, paged.listing)
            if (past != null) {
                item(key = "end filler", span = { GridItemSpan(maxLineSpan) }) {
                    Spacer(Modifier.height(with(LocalDensity.current) { scrollStop.endFillerHeight.toDp() }))
                }
            }
        }
        if (inDateOrder) {
            if (dayLabels) PinnedAgendaLabel(grid, agenda, pinnedLabelRow.value, topPadding, margin, cards.formatting, guideColors)
            else StickyAgendaHeading(grid, agenda, topPadding, margin) { scrollStop.firstReachableIndex }
            StuckAgendaPeriods(stuckPeriods.value, topPadding + stuckPeriodsTop, margin, cards.formatting) { period ->
                state.openMarkSheet(MarkSheetTarget(MarkKind.Period, period.id))
            }
        }
    }
}

private fun LazyGridScope.listingStatusItem(listing: EventListing?, retry: () -> Unit) {
    when (val status = listing?.status) {
        null -> item(key = "loading", span = { GridItemSpan(maxLineSpan) }) { LoadingSpinner(Modifier.animateItem()) }
        is EventListingStatus.Loading -> item(key = "loading", span = { GridItemSpan(maxLineSpan) }) { LoadingSpinner(Modifier.animateItem(), progress = status.progress) }
        is EventListingStatus.Failed -> item(key = "failure", span = { GridItemSpan(maxLineSpan) }) {
            FailureRow("Couldn't load events: ${status.message}", retry, Modifier.animateItem())
        }
        EventListingStatus.HasMore, EventListingStatus.Complete -> {}
    }
}

@Composable
fun StyledEventCard(
    event: ResolvedEvent,
    style: EventCardStyle,
    listing: EventListing?,
    shown: List<ResolvedEvent>,
    actions: EventCardActions,
    cards: EventCardData,
    minHeight: Dp,
    modifier: Modifier = Modifier,
    showsDate: Boolean = true,
    longPressSelects: Boolean = true,
    selectionMarkInside: Boolean = true,
) {
    val heldKeys = rememberHeldKeys()
    val cardModifier = modifier.growsIntoEventPage(event.id)
    val onClick = { actions.click(event, shown, heldKeys) }
    val onLongClick = if (longPressSelects) ({ actions.toggleSelected(event) }) else null
    val fallbackTitle = cards.fallbackTitleOf(event, listing)
    val selection = actions.selectionOf(event)
    when (style) {
        EventCardStyle.Rows -> EventCard(
            event = event,
            formatting = cards.formatting,
            preferredLanguages = cards.preferredLanguages,
            modifier = cardModifier.then(heldKeys.recorder),
            onClick = onClick,
            onOpenActions = actions.actionsFor(event),
            onLongClick = onLongClick,
            selection = selection,
            tags = cards.tagsOf(event),
            accentColor = cards.accentColorOf(event),
            changeLabel = cards.changeLabelOf(event),
            fallbackTitle = fallbackTitle,
            problem = actions.problemOf(event),
            minHeight = if (event.data == null && event.error == null) EventCardHeight else minHeight,
        )
        EventCardStyle.Agenda -> AgendaEventCard(
            event, cards.formatting, cards.preferredLanguages, cards.now, cardModifier.fillMaxWidth().then(heldKeys.recorder),
            onClick = onClick, onLongClick = onLongClick, selection = selection, tags = cards.tagsOf(event), accentColor = cards.accentColorOf(event),
            changeLabel = cards.changeLabelOf(event), fallbackTitle = fallbackTitle, problem = actions.problemOf(event),
            showsDate = showsDate, showsSelectionMark = selectionMarkInside,
        )
        EventCardStyle.Posters -> EventPosterCard(
            event, cards.formatting, cards.preferredLanguages, cards.now, cardModifier.fillMaxWidth().then(heldKeys.recorder),
            onClick = onClick, onLongClick = onLongClick, changeLabel = cards.changeLabelOf(event), selection = selection, fallbackTitle = fallbackTitle,
        )
        EventCardStyle.Tickets -> EventTicketCard(
            event, cards.formatting, cards.preferredLanguages, cards.now, cardModifier.fillMaxWidth().then(heldKeys.recorder),
            onClick = onClick, onLongClick = onLongClick, changeLabel = cards.changeLabelOf(event), selection = selection, fallbackTitle = fallbackTitle,
        )
    }
}
