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
import blue.deniz.vantage.ui.AppIcons
import blue.deniz.vantage.ui.BottomSheet
import blue.deniz.vantage.ui.IconButton
import blue.deniz.vantage.ui.ContentUnderOverlay
import blue.deniz.vantage.ui.FloatingControls
import blue.deniz.vantage.ui.FloatingText
import blue.deniz.vantage.ui.SearchField
import blue.deniz.vantage.ui.SegmentedControl
import blue.deniz.vantage.ui.TitledSection
import blue.deniz.vantage.app.ui.SourceChoicePills
import blue.deniz.vantage.app.ui.label
import blue.deniz.vantage.ui.Spacing
import blue.deniz.vantage.app.ui.TagChoicePills
import blue.deniz.vantage.app.ui.ChoicePill
import blue.deniz.vantage.app.ui.InclusionChoice
import blue.deniz.vantage.app.ui.InclusionFilter
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import blue.deniz.vantage.app.ui.tagColor
import blue.deniz.vantage.core.EventSourceType
import blue.deniz.vantage.core.TimeFilter
import blue.deniz.vantage.app.timeShown
import blue.deniz.vantage.storage.EventListQuery
import blue.deniz.vantage.storage.eventsMatching
import blue.deniz.vantage.storage.narrowedTo
import blue.deniz.vantage.app.EventsView
import blue.deniz.vantage.app.eventsViewsOn
import blue.deniz.vantage.ui.FloatingIconButton
import blue.deniz.vantage.ui.LocalTabBarHeight
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
    var tags by mutableStateOf(InclusionFilter<String>())
    var matchesAllTags by mutableStateOf(false)
    var sources by mutableStateOf(InclusionFilter<EventSourceType>())

    val storeFilter: EventListQuery
        get() = EventListQuery(
            allTagIds = tags.only.takeIf { matchesAllTags }.orEmpty(),
            anyTagIds = tags.only.takeUnless { matchesAllTags }.orEmpty(),
            excludedTagIds = tags.hidden,
            sourceTypes = sources.only,
            excludedSourceTypes = sources.hidden,
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
    val filteringTags = tags.filter { it.id in filters.tags.only }
    val hiddenTags = tags.filter { it.id in filters.tags.hidden }
    val scope = rememberCoroutineScope()
    val selecting = state.isSelectingEvents
    val filterValues = listOf(filters.search, filters.time, filters.order, storeFilter)
    val matchingIds by remember(filterValues, view, selecting, state.listsDayStart) { if (selecting) state.store.observeIds(filters.toListQuery(state.listsDayStart, view)) else flowOf(null) }
        .collectAsState(initial = null)
    if (view == EventsView.List) {
        SelectAllShortcut(state) { scope.launch { state.selectEvents(state.store.observeIds(filters.toListQuery(state.listsDayStart, view)).first()) } }
    }

    val searchFocus = remember { FocusRequester() }
    LaunchedEffect(state.eventSearchFocusRequested) {
        if (state.eventSearchFocusRequested) {
            if (view.searches) searchFocus.requestFocus()
            state.onEventSearchFocused()
        }
    }

    val filtering = filteringTags.isNotEmpty() || hiddenTags.isNotEmpty() || !filters.sources.isEmpty
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
                        selecting -> EventSelectionBar(state, matchingIds, filters.toListQuery(state.listsDayStart, view).order)
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
                                for (type in filters.sources.only + filters.sources.hidden) {
                                    FilterRowPill(type.label, filters.sources.choiceOf(type), MaterialTheme.colorScheme.primary, onRemove = { filters.sources = filters.sources.without(type) })
                                }
                                filteringTags.forEachIndexed { index, tag ->
                                    if (index > 0) FloatingText(if (filters.matchesAllTags) "and" else "or")
                                    FilterRowPill(tag.name, InclusionChoice.Only, tagColor(tag.color), onRemove = { filters.tags = filters.tags.without(tag.id) })
                                }
                                for (tag in hiddenTags) FilterRowPill(tag.name, InclusionChoice.Hidden, tagColor(tag.color), onRemove = { filters.tags = filters.tags.without(tag.id) })
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
                showsMarkersAndPeriods = filters.tags.only.isEmpty() && filters.sources.only.isEmpty(),
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
        if (sourceTypes.size > 1 || !filters.sources.isEmpty) {
            TitledSection("Sources") {
                SourceChoicePills((sourceTypes + filters.sources.only + filters.sources.hidden).distinct(), filters.sources, onTap = { type -> filters.sources = filters.sources.withNextChoice(type) })
            }
        }
        if (tags.isNotEmpty()) {
            TitledSection("Tags") {
                TagChoicePills(tags, filters.tags, onTap = { tag -> filters.tags = filters.tags.withNextChoice(tag.id) })
            }
            if (filters.tags.only.size > 1) {
                TitledSection("Match") {
                    SegmentedControl(
                        options = listOf(false to "Any", true to "All"),
                        selected = filters.matchesAllTags,
                        onSelect = { filters.matchesAllTags = it },
                    )
                }
            }
        }
    }
}

@Composable
private fun FilterRowPill(label: String, choice: InclusionChoice, accent: Color, onRemove: () -> Unit) {
    ChoicePill(label, choice, accent, onClick = onRemove, trailingIcon = AppIcons.X, floating = true)
}
