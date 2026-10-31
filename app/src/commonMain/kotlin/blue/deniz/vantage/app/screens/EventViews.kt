package blue.deniz.vantage.app.screens

import blue.deniz.vantage.app.ui.growsIntoEventPage
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.fadeIn
import blue.deniz.vantage.app.ui.SELECTED_TINT_ALPHA
import blue.deniz.vantage.app.ui.backdropSource
import blue.deniz.vantage.app.ui.LocalBackdrop
import blue.deniz.vantage.app.ui.floatingSurface
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.ceil
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.LazyGridLayoutInfo
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import blue.deniz.vantage.app.ui.AgendaPeriodLine
import blue.deniz.vantage.app.ui.FloatingAgendaPeriodChip
import blue.deniz.vantage.core.CalendarPeriod
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.key
import androidx.compose.animation.AnimatedVisibility
import blue.deniz.vantage.core.CalendarMarks

import blue.deniz.vantage.app.Route

import blue.deniz.vantage.app.ui.AgendaMarkerLine

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import blue.deniz.vantage.app.AppState
import blue.deniz.vantage.app.EventCardStyle
import blue.deniz.vantage.app.EventsView
import blue.deniz.vantage.app.SelectAllShortcut
import blue.deniz.vantage.app.collectAcrossSession
import blue.deniz.vantage.app.ui.AgendaEventCard
import blue.deniz.vantage.app.ui.AgendaHeading
import blue.deniz.vantage.app.ui.AgendaHeadingText
import blue.deniz.vantage.app.ui.AgendaDayLabelText
import blue.deniz.vantage.app.ui.AgendaGroupLabel
import blue.deniz.vantage.app.ui.AgendaGroupLabelText
import blue.deniz.vantage.app.ui.AgendaLabelGap
import blue.deniz.vantage.app.ui.AgendaLabelHeight
import blue.deniz.vantage.app.ui.AgendaDayLabel
import blue.deniz.vantage.app.ui.eventCardColumns
import blue.deniz.vantage.app.ui.EventCard
import blue.deniz.vantage.app.ui.EventCardGrid
import blue.deniz.vantage.app.ui.EventCardHeight
import blue.deniz.vantage.app.ui.EventCardSelection
import blue.deniz.vantage.app.ui.EventFormatting
import blue.deniz.vantage.app.ui.AppIcons
import blue.deniz.vantage.app.ui.EventMap
import blue.deniz.vantage.app.ui.FloatingShadowElevation
import blue.deniz.vantage.app.ui.Icon
import blue.deniz.vantage.app.ui.IconSize
import blue.deniz.vantage.app.ui.MapCamera
import blue.deniz.vantage.app.ui.Spacing
import blue.deniz.vantage.app.ui.floatingBorder
import blue.deniz.vantage.app.ui.ControlHeight
import blue.deniz.vantage.app.ui.FloatingIconButton
import blue.deniz.vantage.app.ui.AgendaIconSize
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import blue.deniz.vantage.app.ui.floatingContainer
import blue.deniz.vantage.app.ui.EventPosterCard
import blue.deniz.vantage.app.ui.EventTicketCard
import blue.deniz.vantage.app.ui.FailureRow
import blue.deniz.vantage.app.ui.HeldKeys
import blue.deniz.vantage.app.ui.ListPlaceholder
import blue.deniz.vantage.app.ui.LoadingSpinner
import blue.deniz.vantage.app.ui.SegmentedControl
import blue.deniz.vantage.app.ui.rememberHeldKeys
import blue.deniz.vantage.app.ui.shortLabel
import blue.deniz.vantage.core.EventCollection
import blue.deniz.vantage.core.EventListing
import blue.deniz.vantage.core.EventListingStatus
import blue.deniz.vantage.core.EventOrder
import blue.deniz.vantage.core.EventQuery
import blue.deniz.vantage.core.EventSource
import blue.deniz.vantage.core.ResolvedEvent
import blue.deniz.vantage.core.TimeFilter
import blue.deniz.vantage.core.toQuery
import blue.deniz.vantage.storage.EventListQuery
import blue.deniz.vantage.storage.TagEntity
import directory.evnt.translate
import kotlinx.datetime.LocalDate
import kotlin.time.Instant

private const val PAGE_SIZE = 50
private val DAY_LINE_GUTTER = 12.dp
private val DAY_LINE_X = 18.dp
private val DAY_LINE_WIDTH = 2.dp
private val DAY_LINE_DASH = 4.dp
private val DAY_DOT_RADIUS = 4.dp
private val TODAY_DOT_RADIUS = 5.dp
private const val DAY_DOT_ALPHA = 0.6f
private val MARKER_LINE_GAP_RADIUS = 9.dp
private val CHECK_SIZE = 20.dp
private val CHECK_CORNER = 4.dp
private val CHECK_STROKE = 2.dp
private val AUTOSCROLL_ZONE = 64.dp
private val AUTOSCROLL_STEP = 16.dp
private const val LOAD_MORE_DISTANCE = 10
private val MAP_START_ROW_HEIGHT = 48.dp

@Stable
interface EventCardActions {
    val isSelecting: Boolean

    fun selectionOf(event: ResolvedEvent): EventCardSelection

    fun open(event: ResolvedEvent)

    fun click(event: ResolvedEvent, shown: List<ResolvedEvent>, keys: HeldKeys)

    fun toggleSelected(event: ResolvedEvent)

    fun selectAll(shown: List<ResolvedEvent>)

    fun actionsFor(event: ResolvedEvent): (() -> Unit)?

    fun problemOf(event: ResolvedEvent): String? = null

    @Composable
    fun SelectionBar(shown: List<ResolvedEvent>)
}

class StoredEventCardActions(private val state: AppState) : EventCardActions {
    override val isSelecting get() = state.selectedEventIds.isNotEmpty()

    override fun selectionOf(event: ResolvedEvent) = when {
        !isSelecting -> EventCardSelection.NotSelecting
        event.id in state.selectedEventIds -> EventCardSelection.Selected
        else -> EventCardSelection.Unselected
    }

    override fun open(event: ResolvedEvent) {
        event.id?.let(state::openEvent)
    }

    override fun click(event: ResolvedEvent, shown: List<ResolvedEvent>, keys: HeldKeys) {
        event.id?.let { id -> state.clickEventCard(id, shown.mapNotNull { it.id }, keys) }
    }

    override fun toggleSelected(event: ResolvedEvent) {
        event.id?.let(state::toggleEventSelected)
    }

    override fun selectAll(shown: List<ResolvedEvent>) {
        state.selectEvents(shown.mapNotNull { it.id })
    }

    override fun actionsFor(event: ResolvedEvent): (() -> Unit)? = if (event.id == null) null else ({ state.openActionsSheet(event) })

    @Composable
    override fun SelectionBar(shown: List<ResolvedEvent>) {
        EventSelectionBar(state, shown.mapNotNull { it.id }, EventListQuery.Order.Start)
    }
}

class EventCardData(
    val formatting: EventFormatting,
    val now: Instant,
    val preferredLanguages: List<String>,
    private val tagsByEvent: Map<String, List<TagEntity>>,
    private val changeLabels: Map<String, String>,
    private val accentColorOf: (EventSource) -> Color?,
) {
    fun tagsOf(event: ResolvedEvent) = event.id?.let(tagsByEvent::get).orEmpty()

    fun changeLabelOf(event: ResolvedEvent) = event.id?.let(changeLabels::get)

    fun accentColorOf(event: ResolvedEvent) = accentColorOf(event.source)

    fun fallbackTitleOf(event: ResolvedEvent, listing: EventListing?): String? =
        listing?.listedNames?.get(event.source)?.translate(preferredLanguages)?.takeIf { it.isNotBlank() }
            ?: (event.source as? EventSource.Remote)?.location?.takeIf { event.data == null }
}

@Composable
fun rememberEventCardData(state: AppState): EventCardData {
    val tagsByEvent = collectAcrossSession("tags by event", null) { state.store.observeTagsByEvent() }.orEmpty()
    val unseenDifferences = collectAcrossSession("unseen differences", null) { state.store.observeUnseenDifferences() }.orEmpty()
    val changeLabels = remember(unseenDifferences) { unseenDifferences.mapValues { it.value.shortLabel() } }
    val formatting = state.formatting
    val now = state.now
    return remember(formatting, now, state.preferredLanguages, tagsByEvent, changeLabels, state.calendars) {
        EventCardData(formatting, now, state.preferredLanguages, tagsByEvent, changeLabels) { source -> state.calendars.colorOf(source)?.let(::Color) }
    }
}

fun ResolvedEvent.listKey(): String = id ?: source.toString()

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
    listHeader: (LazyGridScope.(EventListing?) -> Unit)? = null,
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
            val query = remember(search, time, order, state.listsSplitAt) { time.toQuery(state.listsSplitAt, search, order) }
            EventListView(state, screen, events, query, marks, actions, cards, topPadding, bottomPadding + bottomEndClearance, emptyText, listHeader)
        }
        EventsView.Calendar -> Box(Modifier.padding(bottom = bottomPadding)) {
            CalendarView(state, screen, events, marks, actions, cards, topPadding, filtersButton, onNewOnDay, calendarViewPicker)
        }
        EventsView.Map -> {
            val query = remember(time, state.listsSplitAt) { time.toQuery(state.listsSplitAt) }
            val currentActions by rememberUpdatedState(actions)
            var camera by remember { mutableStateOf<MapCamera?>(null) }
            Box(Modifier.fillMaxSize()) {
                EventMap(
                    events,
                    query,
                    start = state.settings.mapStart,
                    onCameraSettled = { camera = it },
                    onOpenEvent = { currentActions.open(it) },
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = topPadding, bottom = bottomPadding + maxOf(MAP_START_ROW_HEIGHT, bottomEndClearance)),
                )
                AnimatedVisibility(
                    camera != null,
                    Modifier.align(Alignment.BottomStart).padding(start = Spacing.md, bottom = bottomPadding + Spacing.sm),
                    enter = fadeIn(),
                ) {
                    camera?.let { MapStartChip(state, it) }
                }
            }
        }
    }
}

@Composable
private fun MapStartChip(state: AppState, camera: MapCamera, modifier: Modifier = Modifier) {
    val startsHere = state.settings.mapStart?.isAt(camera) == true
    FilterChip(
        selected = startsHere,
        onClick = { state.settings.changeMapStart(camera.takeUnless { startsHere }) },
        label = { Text(if (startsHere) "Map Starts Here" else "Start Map Here", maxLines = 1) },
        modifier = modifier,
        leadingIcon = { Icon(if (startsHere) AppIcons.Check else AppIcons.MapPin, size = IconSize.xs) },
        colors = FilterChipDefaults.filterChipColors(containerColor = MaterialTheme.colorScheme.floatingContainer),
        elevation = FilterChipDefaults.filterChipElevation(elevation = FloatingShadowElevation),
        border = floatingBorder(),
    )
}

@Composable
fun EventsViewControls(selected: EventsView, onSelect: (EventsView) -> Unit, onNew: (() -> Unit)?, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(Spacing.sm), verticalAlignment = Alignment.CenterVertically) {
        EventsViewPicker(selected, onSelect)
        if (onNew != null) FloatingIconButton(AppIcons.Plus, "New", onNew, size = EventsViewControlsHeight)
    }
}

@Composable
private fun EventsViewPicker(selected: EventsView, onSelect: (EventsView) -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.floatingSurface(CircleShape).selectableGroup(),
        shape = CircleShape,
        color = Color.Transparent,
        border = floatingBorder(),
    ) {
        Row(Modifier.padding(Spacing.xs)) {
            for (view in EventsView.entries) EventsViewButton(view, view == selected, onClick = { onSelect(view) })
        }
    }
}

private val EventsViewControlsHeight = ControlHeight.lg + Spacing.xs * 2

val EventsViewControlsClearance = EventsViewControlsHeight + Spacing.sm * 2

@Composable
private fun EventsViewButton(view: EventsView, selected: Boolean, onClick: () -> Unit) {
    val accent = MaterialTheme.colorScheme.primary
    Box(
        Modifier
            .size(ControlHeight.lg)
            .clip(CircleShape)
            .background(if (selected) accent.copy(alpha = SELECTED_TINT_ALPHA) else Color.Transparent)
            .selectable(selected, role = Role.Tab, onClick = onClick)
            .semantics { contentDescription = view.label },
        contentAlignment = Alignment.Center,
    ) {
        Icon(view.icon(selected), tint = if (selected) accent else MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun EventsView.icon(selected: Boolean) = when (this) {
    EventsView.List -> if (selected) AppIcons.ListSelected else AppIcons.List
    EventsView.Calendar -> if (selected) AppIcons.CalendarSelected else AppIcons.Calendar
    EventsView.Map -> if (selected) AppIcons.MapSelected else AppIcons.Map
}

@Composable
fun rememberPagedListing(sessionKey: String, events: EventCollection, query: EventQuery): PagedListing {
    var limit by rememberSaveable(events, query) { mutableIntStateOf(PAGE_SIZE) }
    var attempt by remember { mutableIntStateOf(0) }
    val listing = collectAcrossSession(sessionKey, listOf(events, query, limit, attempt)) { events.list(query, limit) }
    return PagedListing(listing, limit, loadMore = { limit += PAGE_SIZE }, retry = { attempt++ })
}

class PagedListing(val listing: EventListing?, val limit: Int, val loadMore: () -> Unit, val retry: () -> Unit) {
    val events get() = listing?.events.orEmpty()
    val wantsMore get() = listing?.status == EventListingStatus.HasMore && events.size >= limit
    val isEmpty get() = listing != null && events.isEmpty() && (listing.status == EventListingStatus.Complete || listing.status == EventListingStatus.HasMore)
}

@Composable
fun LoadMoreNearTheEnd(paged: PagedListing, itemsAfterLastVisible: () -> Int) {
    val current by rememberUpdatedState(paged)
    LaunchedEffect(Unit) {
        snapshotFlow { current.wantsMore && itemsAfterLastVisible() <= LOAD_MORE_DISTANCE }.collect { nearTheEnd ->
            if (nearTheEnd) current.loadMore()
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
    header: (LazyGridScope.(EventListing?) -> Unit)?,
) {
    val paged = rememberPagedListing("$screen list", events, query)
    val shown = paged.events
    val grid = rememberLazyGridState()
    SelectAllShortcut(state) { actions.selectAll(shown) }
    LoadMoreNearTheEnd(paged) { grid.layoutInfo.let { it.totalItemsCount - (it.visibleItemsInfo.lastOrNull()?.index ?: 0) } }
    val inDateOrder = query.order == EventOrder.Start
    val showsMarks = inDateOrder && query.search.isBlank()
    val markers = remember(marks, query, showsMarks) { if (showsMarks) marks.markersBetween(query.from, query.until) else emptyList() }
    val periods = remember(marks, query, showsMarks) { if (showsMarks) marks.periodsOverlapping(query.from, query.until) else emptyList() }
    val complete = paged.listing?.status == EventListingStatus.Complete
    val margin = Spacing.md + DAY_LINE_GUTTER
    val markerIcon = rememberVectorPainter(AppIcons.Label)
    val guideColors = AgendaGuideColors(
        marker = MaterialTheme.colorScheme.onSurfaceVariant,
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
    val haptics = LocalHapticFeedback.current
    val eventAt = { position: Offset -> eventAt(grid.layoutInfo, currentEventsByKey, position) }
    val backdropSource = Modifier.backdropSource(LocalBackdrop.current)
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val dayLabels = eventCardColumns(maxWidth, DAY_LINE_GUTTER) == 1
        val agenda = remember(shown, cards, markers, periods, complete, dayLabels) {
            agendaOf(shown, cards, markers, periods, marksAfterEvents = complete, dayLabels = dayLabels)
        }
        val labelGapPx = with(LocalDensity.current) { AgendaLabelGap.roundToPx() }
        val pinnedLabelRow = remember(grid, agenda, labelGapPx) {
            derivedStateOf {
                val top = grid.layoutInfo.visibleItemsInfo.lastOrNull { item ->
                    val row = agenda.rowIndexOf(item.key) ?: return@lastOrNull false
                    item.offset.y + (if (agenda.rows[row] is AgendaRow.Label) labelGapPx else 0) < 0
                }
                val row = top?.let { agenda.rowIndexOf(it.key) } ?: return@derivedStateOf null
                (row downTo 0).firstOrNull { agenda.rows[it] is AgendaRow.Label }
            }
        }
        val headingPillHeight = with(LocalDensity.current) { MaterialTheme.typography.headlineSmall.lineHeight.toDp() } + Spacing.xs * 2
        val stuckPeriodsTop = if (dayLabels) AgendaLabelHeight + Spacing.xs else Spacing.xs + headingPillHeight + Spacing.xs
        val periodSlotPx = with(LocalDensity.current) { stuckPeriodsTop.roundToPx() }
        val stuckPeriods = remember(grid, agenda, periodSlotPx) {
            derivedStateOf {
                val under = grid.layoutInfo.visibleItemsInfo.lastOrNull { item -> item.offset.y < periodSlotPx && agenda.rowIndexOf(item.key) != null }
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
                        inDateOrder -> drawAgendaGuides(layout, agenda, markerIcon, guideColors, pinnedLabelRow.value)
                    }
                }
                .selectsByDragging(grid, topPadding, eventAt, { currentShown }, { currentActions }) { haptics.performHapticFeedback(HapticFeedbackType.LongPress) }
                .pointerInput(grid, columns) {
                    detectTapGestures { position ->
                        if (columns == 1 && currentActions.isSelecting && position.x < margin.toPx()) eventAt(position)?.let(currentActions::toggleSelected)
                    }
                }
        }
        EventCardGrid(grid, topPadding = topPadding, bottomPadding = bottomPadding, startGutter = DAY_LINE_GUTTER, decoration = decoration) { cardMinHeight ->
            header?.invoke(this, paged.listing)
            if (paged.isEmpty && markers.isEmpty() && periods.isEmpty()) {
                item(key = "no events", span = { GridItemSpan(maxLineSpan) }) { ListPlaceholder(emptyText, Modifier.animateItem()) }
            }
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
                            row.event, EventCardStyle.Agenda, paged.listing, shown, actions, cards, cardMinHeight, Modifier.animateItem(),
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
                        event, EventCardStyle.Agenda, paged.listing, shown, actions, cards, cardMinHeight, Modifier.animateItem(),
                        longPressSelects = false, selectionMarkInside = cardMinHeight > 0.dp,
                    )
                }
            }
            listingStatusItem(paged.listing, paged.retry)
        }
        if (inDateOrder) {
            if (dayLabels) PinnedAgendaLabel(grid, agenda, pinnedLabelRow.value, topPadding, margin, cards.formatting, guideColors)
            else StickyAgendaHeading(grid, agenda, topPadding, margin)
            StuckAgendaPeriods(stuckPeriods.value, topPadding + stuckPeriodsTop, margin, cards.formatting) { period ->
                state.openMarkSheet(MarkSheetTarget(MarkKind.Period, period.id))
            }
        }
    }
}

private class AgendaGuideColors(val marker: Color, val line: Color, val dot: Color, val today: Color, val background: Color)

private fun DrawScope.drawAgendaGuides(layout: LazyGridLayoutInfo, agenda: Agenda, markerIcon: Painter, colors: AgendaGuideColors, pinnedLabelRow: Int?) {
    val spans = HashMap<Int, ClosedFloatingPointRange<Float>>()
    for (item in layout.visibleItemsInfo) {
        val row = agenda.rowIndexOf(item.key) ?: continue
        val top = (item.offset.y - layout.viewportStartOffset).toFloat()
        val span = top..top + item.size.height
        spans[row] = spans[row]?.let { minOf(it.start, span.start)..maxOf(it.endInclusive, span.endInclusive) } ?: span
    }
    if (spans.isEmpty()) return
    drawDayLine(agenda, spans, colors, pinnedLabelRow)
    drawMarkerIcons(agenda, spans, markerIcon, colors)
}

private fun DrawScope.drawMarkerIcons(agenda: Agenda, spans: Map<Int, ClosedFloatingPointRange<Float>>, icon: Painter, colors: AgendaGuideColors) {
    val size = AgendaIconSize.toPx()
    val tint = ColorFilter.tint(colors.marker)
    for ((row, span) in spans) {
        if (agenda.rows[row] !is AgendaRow.Marker) continue
        val center = Offset(DAY_LINE_X.toPx(), (span.start + span.endInclusive) / 2)
        drawCircle(colors.background, MARKER_LINE_GAP_RADIUS.toPx(), center)
        translate(center.x - size / 2, center.y - size / 2) {
            with(icon) { draw(Size(size, size), colorFilter = tint) }
        }
    }
}

private fun DrawScope.drawDayLine(agenda: Agenda, spans: Map<Int, ClosedFloatingPointRange<Float>>, colors: AgendaGuideColors, pinnedLabelRow: Int?) {
    val x = DAY_LINE_X.toPx()
    val width = DAY_LINE_WIDTH.toPx()
    val dash = DAY_LINE_DASH.toPx()
    val dashes = PathEffect.dashPathEffect(floatArrayOf(dash, dash))
    val shown = spans.keys.sorted()
    for ((position, row) in shown.withIndex()) {
        val time = agenda.rowTimes[row]
        val span = spans.getValue(row)
        val top = if (position == 0 && row > 0 && span.start > 0) span.start - ceil(span.start / (2 * dash)) * 2 * dash else span.start
        val bottom = shown.getOrNull(position + 1)?.let { spans.getValue(it).start }
            ?: if (row == agenda.rows.lastIndex) span.endInclusive else size.height
        val color = if (time == AgendaTime.Today) colors.today else colors.line
        drawLine(color, Offset(x, top), Offset(x, bottom), width, pathEffect = if (time == AgendaTime.Future || time == AgendaTime.Undated) dashes else null)
    }
    for (row in shown) {
        if (agenda.rows[row] !is AgendaRow.Label || row == pinnedLabelRow) continue
        val span = spans.getValue(row)
        drawDayDot(agenda.rowTimes[row], Offset(x, (span.start + AgendaLabelGap.toPx() + span.endInclusive) / 2), colors)
    }
}

private fun DrawScope.drawDayDot(time: AgendaTime, center: Offset, colors: AgendaGuideColors) {
    when (time) {
        AgendaTime.Today -> drawCircle(colors.today, TODAY_DOT_RADIUS.toPx(), center)
        AgendaTime.Past -> drawCircle(colors.dot, DAY_DOT_RADIUS.toPx(), center)
        AgendaTime.Future, AgendaTime.Undated -> {
            val width = DAY_LINE_WIDTH.toPx()
            drawCircle(colors.background, DAY_DOT_RADIUS.toPx(), center)
            drawCircle(colors.dot, DAY_DOT_RADIUS.toPx() - width / 2, center, style = Stroke(width))
        }
    }
}

private class CheckColors(val box: Color, val checked: Color, val mark: Color)

private fun eventAt(layout: LazyGridLayoutInfo, eventsByKey: Map<String, ResolvedEvent>, position: Offset): ResolvedEvent? {
    val line = layout.visibleItemsInfo.filter { item ->
        val top = item.offset.y - layout.viewportStartOffset
        position.y >= top && position.y < top + item.size.height && (item.key as? String)?.let(eventsByKey::get) != null
    }
    val item = line.firstOrNull { position.x >= it.offset.x && position.x < it.offset.x + it.size.width }
        ?: line.minByOrNull { abs(it.offset.x + it.size.width / 2f - position.x) }
    return (item?.key as? String)?.let(eventsByKey::get)
}

private fun DrawScope.drawSelectionChecks(layout: LazyGridLayoutInfo, eventsByKey: Map<String, ResolvedEvent>, actions: EventCardActions, margin: Dp, colors: CheckColors) {
    val box = CHECK_SIZE.toPx()
    val left = (margin.toPx() - box) / 2
    val corner = CornerRadius(CHECK_CORNER.toPx())
    val stroke = CHECK_STROKE.toPx()
    for (item in layout.visibleItemsInfo) {
        val event = (item.key as? String)?.let(eventsByKey::get) ?: continue
        val top = item.offset.y - layout.viewportStartOffset + (item.size.height - box) / 2
        if (actions.selectionOf(event) == EventCardSelection.Selected) {
            drawRoundRect(colors.checked, Offset(left, top), Size(box, box), corner)
            val mark = Path().apply {
                moveTo(left + box * 0.24f, top + box * 0.52f)
                lineTo(left + box * 0.42f, top + box * 0.7f)
                lineTo(left + box * 0.76f, top + box * 0.32f)
            }
            drawPath(mark, colors.mark, style = Stroke(stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
        } else {
            drawRoundRect(colors.box, Offset(left + stroke / 2, top + stroke / 2), Size(box - stroke, box - stroke), corner, style = Stroke(stroke))
        }
    }
}

private fun Modifier.selectsByDragging(
    grid: LazyGridState,
    topPadding: Dp,
    eventAt: (Offset) -> ResolvedEvent?,
    shown: () -> List<ResolvedEvent>,
    actions: () -> EventCardActions,
    onHeld: () -> Unit,
): Modifier = pointerInput(grid) {
    coroutineScope {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            val start = eventAt(down.position) ?: return@awaitEachGesture
            var released = false
            var moved = false
            val held = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
                while (!released && !moved) {
                    val change = awaitPointerEvent(PointerEventPass.Initial).changes.firstOrNull { it.id == down.id }
                    released = change == null || !change.pressed
                    moved = change != null && (change.position - down.position).getDistance() > viewConfiguration.touchSlop
                }
            } == null
            if (!held) return@awaitEachGesture
            onHeld()
            actions().toggleSelected(start)
            val draggedIn = mutableSetOf<String>()
            var current = start.listKey()
            var pointer = down.position
            val extendTo = { event: ResolvedEvent ->
                if (event.listKey() != current) {
                    current = event.listKey()
                    val list = shown()
                    val from = list.indexOfFirst { it.listKey() == start.listKey() }
                    val to = list.indexOfFirst { it.listKey() == current }
                    if (from >= 0 && to >= 0) {
                        val range = list.subList(minOf(from, to), maxOf(from, to) + 1)
                        val rangeKeys = range.mapTo(mutableSetOf()) { it.listKey() }
                        val acting = actions()
                        for (inRange in range) {
                            if (acting.selectionOf(inRange) != EventCardSelection.Selected) {
                                acting.toggleSelected(inRange)
                                draggedIn += inRange.listKey()
                            }
                        }
                        for (outside in list) {
                            val key = outside.listKey()
                            if (key in draggedIn && key !in rangeKeys) {
                                acting.toggleSelected(outside)
                                draggedIn -= key
                            }
                        }
                    }
                }
            }
            val top = topPadding.toPx()
            val zone = AUTOSCROLL_ZONE.toPx()
            val fastest = AUTOSCROLL_STEP.toPx()
            val autoscroll = launch {
                while (isActive) {
                    withFrameNanos {}
                    val speed = when {
                        pointer.y < top + zone -> -fastest * ((top + zone - pointer.y) / zone).coerceIn(0f, 1f)
                        pointer.y > size.height - zone -> fastest * ((pointer.y - (size.height - zone)) / zone).coerceIn(0f, 1f)
                        else -> 0f
                    }
                    if (speed != 0f) {
                        grid.scrollBy(speed)
                        eventAt(pointer)?.let(extendTo)
                    }
                }
            }
            try {
                while (true) {
                    val change = awaitPointerEvent(PointerEventPass.Initial).changes.firstOrNull { it.id == down.id } ?: break
                    change.consume()
                    if (!change.pressed) break
                    pointer = change.position
                    eventAt(pointer)?.let(extendTo)
                }
            } finally {
                autoscroll.cancel()
            }
        }
    }
}

@Composable
private fun StickyAgendaHeading(grid: LazyGridState, agenda: Agenda, topPadding: Dp, cardsStart: Dp) {
    val stuckRow by remember(grid, agenda) {
        derivedStateOf {
            val above = grid.layoutInfo.visibleItemsInfo.lastOrNull { it.offset.y < 0 && agenda.rowIndexOf(it.key) != null }
            above?.let { agenda.rowIndexOf(it.key) }?.let(agenda::headingRowOf)
        }
    }
    val row = stuckRow ?: return
    val heading = agenda.rows[row] as? AgendaRow.Heading ?: return
    val pillAlpha = remember(row) { Animatable(0f) }
    LaunchedEffect(row) { pillAlpha.animateTo(1f) }
    Layout(
        content = {
            Box(Modifier.clearAndSetSemantics {}) {
                Box(Modifier.matchParentSize().graphicsLayer { alpha = pillAlpha.value }.floatingSurface(CircleShape).border(floatingBorder(), CircleShape))
                Row(Modifier.padding(horizontal = Spacing.md, vertical = Spacing.xs), verticalAlignment = Alignment.CenterVertically) {
                    AgendaHeadingText(heading.title, heading.isCurrent)
                }
            }
        },
        modifier = Modifier.fillMaxSize(),
    ) { measurables, constraints ->
        val pillStart = (cardsStart + Spacing.xs - Spacing.md).roundToPx()
        val pill = measurables.single().measure(Constraints(maxWidth = (constraints.maxWidth - pillStart * 2).coerceAtLeast(0)))
        layout(constraints.maxWidth, constraints.maxHeight) {
            val next = grid.layoutInfo.visibleItemsInfo.firstOrNull { item ->
                val index = agenda.rowIndexOf(item.key)
                index != null && index > row && agenda.rows[index] is AgendaRow.Heading
            }
            val pushedUp = next?.let { minOf(0, it.offset.y - pill.height) } ?: 0
            pill.place(pillStart, (topPadding + Spacing.xs).roundToPx() + pushedUp)
        }
    }
}

@Composable
private fun StuckAgendaPeriods(periods: List<CalendarPeriod>, top: Dp, cardsStart: Dp, formatting: EventFormatting, onOpen: (CalendarPeriod) -> Unit) {
    if (periods.isEmpty()) return
    Column(
        Modifier.padding(start = cardsStart, top = top, end = Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        for (period in periods) key(period.id) {
            FloatingAgendaPeriodChip(period, formatting) { onOpen(period) }
        }
    }
}

@Composable
private fun PinnedAgendaLabel(
    grid: LazyGridState,
    agenda: Agenda,
    pinnedRow: Int?,
    topPadding: Dp,
    cardsStart: Dp,
    formatting: EventFormatting,
    colors: AgendaGuideColors,
) {
    val row = pinnedRow ?: return
    val label = agenda.rows[row] as? AgendaRow.Label ?: return
    val time = agenda.rowTimes[row]
    val pillAlpha = remember(row) { Animatable(0f) }
    LaunchedEffect(row) { pillAlpha.animateTo(1f) }
    val pillStart = DAY_LINE_X - AgendaLabelHeight / 2
    val dotToText = cardsStart + Spacing.xs - DAY_LINE_X - DAY_DOT_RADIUS
    Layout(
        content = {
            Box(Modifier.clearAndSetSemantics {}) {
                Box(Modifier.matchParentSize().graphicsLayer { alpha = pillAlpha.value }.floatingSurface(CircleShape).border(floatingBorder(), CircleShape))
                Row(Modifier.heightIn(min = AgendaLabelHeight).padding(end = dotToText), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(width = cardsStart + Spacing.xs - pillStart, height = AgendaLabelHeight).drawBehind {
                            drawDayDot(time, Offset((DAY_LINE_X - pillStart).toPx(), size.height / 2), colors)
                        },
                    )
                    when (label) {
                        is AgendaRow.Day -> AgendaDayLabelText(label.date, label.isToday, formatting)
                        is AgendaRow.Group -> AgendaGroupLabelText(label.title)
                    }
                }
            }
        },
        modifier = Modifier.fillMaxSize(),
    ) { measurables, constraints ->
        val pill = measurables.single().measure(Constraints(maxWidth = constraints.maxWidth))
        layout(constraints.maxWidth, constraints.maxHeight) {
            val next = grid.layoutInfo.visibleItemsInfo.firstOrNull { item ->
                val index = agenda.rowIndexOf(item.key)
                index != null && index > row && agenda.rows[index] is AgendaRow.Label
            }
            val pushedUp = next?.let { minOf(0, it.offset.y + AgendaLabelGap.roundToPx() - pill.height) } ?: 0
            pill.place(pillStart.roundToPx(), topPadding.roundToPx() + pushedUp)
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
