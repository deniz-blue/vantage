package blue.deniz.vantage.app.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import blue.deniz.vantage.app.collectAcrossSession
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import blue.deniz.vantage.app.AppState
import blue.deniz.vantage.app.Route
import blue.deniz.vantage.app.ui.AppIcons
import blue.deniz.vantage.app.ui.BottomSheet
import blue.deniz.vantage.app.ui.IconButton
import blue.deniz.vantage.app.ui.ContentUnderOverlay
import blue.deniz.vantage.app.ui.FloatingControls
import blue.deniz.vantage.app.ui.FloatingText
import blue.deniz.vantage.app.ui.SearchField
import blue.deniz.vantage.app.ui.SegmentedControl
import blue.deniz.vantage.app.ui.TitledSection
import blue.deniz.vantage.app.ui.SourceTypeChip
import blue.deniz.vantage.app.ui.SourceTypeFilterChips
import blue.deniz.vantage.app.ui.Spacing
import blue.deniz.vantage.app.ui.TagChip
import blue.deniz.vantage.app.ui.TagFilterChips
import blue.deniz.vantage.core.EventSourceType
import blue.deniz.vantage.core.TimeFilter
import blue.deniz.vantage.app.timeShown
import blue.deniz.vantage.storage.EventListQuery
import blue.deniz.vantage.storage.eventsMatching
import blue.deniz.vantage.storage.narrowedTo
import blue.deniz.vantage.app.EventsView
import blue.deniz.vantage.app.eventsViewsOn
import blue.deniz.vantage.app.ui.FloatingIconButton
import blue.deniz.vantage.app.ui.LocalTabBarHeight
import blue.deniz.vantage.core.EventOrder
import blue.deniz.vantage.core.toQuery
import kotlinx.coroutines.flow.flowOf
import kotlin.time.Instant
import blue.deniz.vantage.app.SelectAllShortcut
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope

const val MY_EVENTS_SCREEN = "myEvents"

@Stable
class EventListFilters {
    var search by mutableStateOf("")
    var time by mutableStateOf(TimeFilter.Future)
    var order by mutableStateOf(EventOrder.Start)
    var tagIds by mutableStateOf(emptySet<String>())
    var matchesAllTags by mutableStateOf(false)
    var excludedTagIds by mutableStateOf(emptySet<String>())
    var sourceTypes by mutableStateOf(emptySet<EventSourceType>())

    val storeFilter: EventListQuery
        get() = EventListQuery(
            allTagIds = tagIds.takeIf { matchesAllTags }.orEmpty(),
            anyTagIds = tagIds.takeUnless { matchesAllTags }.orEmpty(),
            excludedTagIds = excludedTagIds,
            sourceTypes = sourceTypes,
        )

    fun toListQuery(now: Instant, view: EventsView) = storeFilter.narrowedTo(view.timeShown(time).toQuery(now, search, order), limit = null)
}

@Composable
fun MyEventsScreen(state: AppState, onOpenFilters: () -> Unit) {
    val filters = state.myEventsFilters
    val view = state.myEventsView
    val storeFilter = filters.storeFilter
    val events = remember(storeFilter) { state.store.eventsMatching(storeFilter) }
    val actions = remember(state) { StoredEventCardActions(state) }
    val tags = collectAcrossSession("tags", null) { state.store.observeTags() }.orEmpty()
    val filteringTags = tags.filter { it.id in filters.tagIds }
    val hiddenTags = tags.filter { it.id in filters.excludedTagIds }
    val scope = rememberCoroutineScope()
    val selecting = state.isSelectingEvents
    val filterValues = listOf(filters.search, filters.time, filters.order, storeFilter)
    val matchingIds by remember(filterValues, view, selecting, state.listsSplitAt) { if (selecting) state.store.observeIds(filters.toListQuery(state.listsSplitAt, view)) else flowOf(null) }
        .collectAsState(initial = null)
    if (view == EventsView.List) {
        SelectAllShortcut(state) { scope.launch { state.selectEvents(state.store.observeIds(filters.toListQuery(state.listsSplitAt, view)).first()) } }
    }

    val searchFocus = remember { FocusRequester() }
    LaunchedEffect(state.eventSearchFocusRequested) {
        if (state.eventSearchFocusRequested) {
            if (view.searches) searchFocus.requestFocus()
            state.onEventSearchFocused()
        }
    }

    val filtering = filteringTags.isNotEmpty() || hiddenTags.isNotEmpty() || filters.sourceTypes.isNotEmpty()
    val floatingFiltersButton = !selecting && !view.searches && view != EventsView.Calendar
    val changeView = { chosen: EventsView -> state.settings.changeEventsView(MY_EVENTS_SCREEN, chosen) }
    val openNewEntry = { state.openNewEntrySheet() }
    val openNewEntryOnSelectedDay = { state.openNewEntrySheet(state.calendarSelection.day) }

    Box(Modifier.fillMaxSize()) {
        ContentUnderOverlay(
            overlay = {
                if (!selecting && !view.searches && !filtering && !floatingFiltersButton) {
                    Spacer(Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
                } else FloatingControls(backdrop = false) {
                    when {
                        selecting -> EventSelectionBar(state, matchingIds, filters.toListQuery(state.listsSplitAt, view).order)
                        view.searches -> SearchField(
                            value = filters.search,
                            onValueChange = { filters.search = it },
                            placeholder = "Search Events",
                            modifier = Modifier.fillMaxWidth().focusRequester(searchFocus),
                            trailing = { IconButton(AppIcons.Filter, "Filters", onOpenFilters) },
                        )
                    }
                    if (filtering || floatingFiltersButton) {
                        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm), verticalAlignment = Alignment.Top) {
                            FlowRow(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(Spacing.sm), itemVerticalAlignment = Alignment.CenterVertically) {
                                for (type in filters.sourceTypes) SourceTypeChip(type, onClick = { filters.sourceTypes -= type })
                                filteringTags.forEachIndexed { index, tag ->
                                    if (index > 0) FloatingText(if (filters.matchesAllTags) "and" else "or")
                                    TagChip(tag, onClick = { filters.tagIds -= tag.id }, trailingIcon = AppIcons.X, floating = true)
                                }
                                for (tag in hiddenTags) TagChip(tag, onClick = { filters.excludedTagIds -= tag.id }, trailingIcon = AppIcons.X, hidden = true, floating = true)
                            }
                            if (floatingFiltersButton) FloatingIconButton(AppIcons.Filter, "Filters", onOpenFilters)
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxSize(),
        ) { overlayHeight ->
            EventsViewContent(
                state = state,
                screen = MY_EVENTS_SCREEN,
                view = view,
                events = events,
                search = filters.search,
                time = filters.time,
                order = filters.order,
                actions = actions,
                topPadding = overlayHeight,
                bottomPadding = LocalTabBarHeight.current,
                filtersButton = { IconButton(AppIcons.Filter, "Filters", onOpenFilters) },
                onNewOnDay = state::openNewEntrySheet,
                showsMarkersAndPeriods = filters.tagIds.isEmpty() && filters.sourceTypes.isEmpty(),
                bottomEndClearance = EventsViewControlsClearance,
                calendarViewPicker = { EventsViewControls(eventsViewsOn(MY_EVENTS_SCREEN), view, changeView, onNew = openNewEntryOnSelectedDay) },
            )
        }
        if (view != EventsView.Calendar) {
            EventsViewControls(eventsViewsOn(MY_EVENTS_SCREEN), view, changeView, onNew = openNewEntry, Modifier.align(Alignment.BottomEnd).padding(end = Spacing.md, bottom = LocalTabBarHeight.current + Spacing.sm))
        }
    }
}

@Composable
fun EventListFiltersSheet(state: AppState, visible: Boolean, onDismiss: () -> Unit) {
    val filters = state.myEventsFilters
    val tags = collectAcrossSession("tags", null) { state.store.observeTags() }.orEmpty()
    val sourceTypes = collectAcrossSession("source types", null) { state.store.observeSourceTypes() }.orEmpty()
    BottomSheet(visible, onDismiss) {
        val view = state.myEventsView
        if (view == EventsView.List) SelectEventsRow(state::startSelectingEvents, onDismiss)
        if (view.filtersByTime) {
            TitledSection("Filter by") {
                SegmentedControl(
                    options = listOf(TimeFilter.Past to "Past", TimeFilter.All to "All", TimeFilter.Future to "Future"),
                    selected = filters.time,
                    onSelect = { filters.time = it },
                )
            }
        }
        if (view.sorts) {
            TitledSection("Sort by") {
                SegmentedControl(
                    options = listOf(EventOrder.Start to "Date", EventOrder.Name to "Name"),
                    selected = filters.order,
                    onSelect = { filters.order = it },
                )
            }
        }
        if (sourceTypes.size > 1 || filters.sourceTypes.isNotEmpty()) {
            TitledSection("Sources") {
                SourceTypeFilterChips((sourceTypes + filters.sourceTypes).distinct(), filters.sourceTypes, onToggle = { type -> filters.sourceTypes = filters.sourceTypes.toggled(type) })
            }
        }
        if (tags.isNotEmpty()) {
            TitledSection("Tags") {
                TagFilterChips(tags, filters.tagIds, onToggle = { tag -> filters.tagIds = filters.tagIds.toggled(tag.id) })
            }
            if (filters.tagIds.size > 1) {
                TitledSection("Match") {
                    SegmentedControl(
                        options = listOf(false to "Any", true to "All"),
                        selected = filters.matchesAllTags,
                        onSelect = { filters.matchesAllTags = it },
                    )
                }
            }
            TitledSection("Hide Tags") {
                TagFilterChips(tags, filters.excludedTagIds, onToggle = { tag -> filters.excludedTagIds = filters.excludedTagIds.toggled(tag.id) }, chosenIcon = AppIcons.Hidden)
            }
        }
    }
}

private fun <T> Set<T>.toggled(item: T) = if (item in this) this - item else this + item
