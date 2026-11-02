package blue.deniz.vantage.app.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import blue.deniz.vantage.app.AppState
import blue.deniz.vantage.app.EventsView
import blue.deniz.vantage.app.HomeTile
import blue.deniz.vantage.app.Route
import blue.deniz.vantage.app.SystemBackHandler
import blue.deniz.vantage.app.newHomeTileId
import blue.deniz.vantage.app.rememberAcrossNavigation
import blue.deniz.vantage.app.rememberShareText
import blue.deniz.vantage.app.ui.AppIcons
import blue.deniz.vantage.app.ui.BottomSheet
import blue.deniz.vantage.app.ui.Button
import blue.deniz.vantage.app.ui.SheetTitle
import blue.deniz.vantage.app.ui.focusesWhenShown
import blue.deniz.vantage.app.ui.ContentUnderOverlay
import blue.deniz.vantage.app.ui.EventCardSelection
import blue.deniz.vantage.app.ui.FloatingControls
import blue.deniz.vantage.app.ui.FloatingHeader
import blue.deniz.vantage.app.ui.FloatingHeaderRow
import blue.deniz.vantage.app.ui.HeldKeys
import blue.deniz.vantage.app.ui.IconButton
import blue.deniz.vantage.app.ui.MenuItem
import blue.deniz.vantage.app.ui.SearchField
import blue.deniz.vantage.app.ui.SegmentedControl
import blue.deniz.vantage.app.ui.Spacing
import blue.deniz.vantage.app.ui.Text
import blue.deniz.vantage.app.ui.TextInput
import blue.deniz.vantage.app.ui.TitledSection
import blue.deniz.vantage.app.ui.markdownSummary
import blue.deniz.vantage.core.EventCount
import blue.deniz.vantage.core.EventFeed
import blue.deniz.vantage.core.EventListing
import blue.deniz.vantage.core.EventOrder
import blue.deniz.vantage.core.EventSource
import blue.deniz.vantage.core.EventsLink
import blue.deniz.vantage.core.ResolvedEvent
import blue.deniz.vantage.core.SubscribedCalendar
import blue.deniz.vantage.core.TimeFilter
import blue.deniz.vantage.core.address
import io.ktor.http.parseUrl
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val INDEX_SEARCH_DELAY_MILLIS = 400L

internal const val FEED_ADDRESS_PROBLEM = "Feed addresses start with https:// or are a handle like alice.bsky.social"

@Composable
fun ChooseFeedSheet(state: AppState) {
    var address by rememberSaveable { mutableStateOf("") }
    var showsAddressProblem by rememberSaveable { mutableStateOf(false) }
    val feed = EventFeed.fromAddress(address)
    val addressProblem = FEED_ADDRESS_PROBLEM.takeIf { address.isNotBlank() && feed == null }
    fun open() {
        val opened = EventFeed.fromAddress(address)
        if (opened == null) {
            showsAddressProblem = true
            return
        }
        state.closeChooseFeedSheet()
        address = ""
        showsAddressProblem = false
        state.navigator.open(Route.Feed(opened))
    }

    BottomSheet(visible = state.chooseFeedSheetOpen, onDismiss = state::closeChooseFeedSheet) {
        SheetTitle("Browse a Feed", Modifier.padding(horizontal = Spacing.md))
        Column(Modifier.padding(horizontal = Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            TextInput(
                value = address,
                onValueChange = { address = it },
                modifier = Modifier.fillMaxWidth().focusesWhenShown(),
                label = "Feed Address",
                placeholder = "https://… or alice.bsky.social",
                problem = addressProblem,
                showsProblem = showsAddressProblem,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions(onGo = { open() }),
            )
            feed?.let { Button("View Feed", onClick = ::open, primary = true, modifier = Modifier.fillMaxWidth()) }
        }
    }
}

@Stable
private class FeedFilters {
    var searching by mutableStateOf(false)
    var search by mutableStateOf("")
    var time by mutableStateOf(TimeFilter.Future)
    var order by mutableStateOf(EventOrder.Start)

    fun closeSearch() {
        searching = false
        search = ""
    }
}

private fun feedScreenKey(feed: EventFeed) = "feed ${feed.address}"

@Composable
fun FeedScreen(state: AppState, feed: EventFeed) {
    val screen = feedScreenKey(feed)
    val view = state.settings.eventsViewOf(screen, EventsView.List)
    val filters = rememberAcrossNavigation("filters $feed") { FeedFilters() }
    var choosingFilters by remember { mutableStateOf(false) }
    var showingMenu by remember { mutableStateOf(false) }
    val title = feedTitle(state, feed)
    val searches = view.searches && filters.searching
    SystemBackHandler(enabled = searches, handlesEscape = true) { filters.closeSearch() }
    var search by remember { mutableStateOf(filters.search) }
    LaunchedEffect(filters.search) {
        if (filters.search.isNotBlank()) delay(INDEX_SEARCH_DELAY_MILLIS)
        search = filters.search
    }
    val events = remember(feed, state.preferredLanguages) { state.feeds.eventsOf(feed, state.preferredLanguages) }
    val selection = rememberAcrossNavigation("selection $feed") { RemoteEventSelection(state) }
    val changeView = { chosen: EventsView -> state.settings.changeEventsView(screen, chosen) }

    Box(Modifier.fillMaxSize()) {
        ContentUnderOverlay(
            overlay = {
                if (selection.isSelecting) {
                    FloatingControls(backdrop = false) { RemoteEventSelectionBar(state, selection) }
                } else {
                    FloatingControls {
                        val filtersButton = @Composable {
                            if (view.filtersByTime || view.sorts) IconButton(AppIcons.Filter, "Filters", { choosingFilters = true })
                        }
                        if (searches) {
                            FloatingHeaderRow {
                                IconButton(AppIcons.ArrowLeft, "Close Search", filters::closeSearch)
                                SearchField(
                                    value = filters.search,
                                    onValueChange = { filters.search = it },
                                    placeholder = "Search $title",
                                    modifier = Modifier.weight(1f),
                                    trailing = if (filters.search.isEmpty()) null else ({ IconButton(AppIcons.X, "Clear", { filters.search = "" }) }),
                                    floating = false,
                                    focusesWhenShown = true,
                                )
                                filtersButton()
                            }
                        } else {
                            FloatingHeader(title, onBack = { state.navigator.goBack() }) {
                                if (view.searches) IconButton(AppIcons.Search, "Search", { filters.searching = true })
                                filtersButton()
                                IconButton(AppIcons.DotsVertical, "More", { showingMenu = true })
                            }
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxSize(),
        ) { overlayHeight ->
            EventsViewContent(
                state = state,
                screen = screen,
                view = view,
                events = events,
                search = search,
                time = filters.time,
                order = filters.order,
                actions = selection,
                topPadding = overlayHeight,
                bottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + Spacing.md,
                bottomEndClearance = EventsViewControlsClearance,
                calendarViewPicker = { EventsViewControls(view, changeView, onNew = null) },
                listFooter = { listing ->
                    listing?.countLabel()?.let { count ->
                        item(key = "count", span = { GridItemSpan(maxLineSpan) }) {
                            Text(
                                count,
                                Modifier.animateItem().fillMaxWidth().padding(vertical = Spacing.md),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                },
            )
        }
        if (view != EventsView.Calendar) {
            EventsViewControls(
                view,
                changeView,
                onNew = null,
                Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(end = Spacing.md, bottom = Spacing.sm),
            )
        }
    }
    FeedFiltersSheet(state, feed, filters, visible = choosingFilters, onDismiss = { choosingFilters = false })
    FeedMenuSheet(state, feed, title, visible = showingMenu, onDismiss = { showingMenu = false })
}

@Composable
private fun feedTitle(state: AppState, feed: EventFeed): String = when (feed) {
    is EventFeed.OpenEvntIndex -> remember(feed.url) { parseUrl(feed.url)?.host?.removePrefix("www.")?.takeIf { it.isNotEmpty() } ?: "Index" }
    is EventFeed.AtRepository -> {
        val actor by produceState(state.feeds.lastResolvedActor(feed.repo), feed.repo) {
            value = (loadCatching { state.feeds.resolveActor(feed.repo) } as? LoadState.Ready)?.value ?: value
        }
        actor?.let { it.handle ?: it.did } ?: feed.repo
    }
}

private fun EventListing.countLabel(): String? = when (val count = count) {
    is EventCount.Total -> eventCountLabel(count.events)
    is EventCount.Matching -> when {
        !count.readWholeSource -> "${count.matching} of the first ${eventCountLabel(count.read)}"
        count.matching == count.read -> eventCountLabel(count.read)
        else -> "${count.matching} of ${eventCountLabel(count.read)}"
    }
    null -> null
}

@Composable
private fun FeedFiltersSheet(state: AppState, feed: EventFeed, filters: FeedFilters, visible: Boolean, onDismiss: () -> Unit) {
    val screen = feedScreenKey(feed)
    BottomSheet(visible, onDismiss) {
        val view = state.settings.eventsViewOf(screen, EventsView.List)
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
    }
}

@Composable
private fun FeedMenuSheet(state: AppState, feed: EventFeed, title: String, visible: Boolean, onDismiss: () -> Unit) {
    val tiles = state.settings.homeTiles
    val onHome = tiles.any { it is HomeTile.Feed && it.feed == feed }
    val calendarLink = (feed as? EventFeed.OpenEvntIndex)?.let { SubscribedCalendar.Link(it.url) }
    val subscribed = calendarLink != null && calendarLink in state.settings.subscribedCalendars
    val scope = rememberCoroutineScope()
    var checking by remember { mutableStateOf(false) }
    var failure by remember { mutableStateOf<String?>(null) }
    fun addToHome() = state.settings.changeHomeTiles(state.settings.homeTiles + HomeTile.Feed(newHomeTileId(), feed))
    BottomSheet(visible, onDismiss) {
        Column(Modifier.padding(horizontal = Spacing.md).padding(bottom = Spacing.sm)) {
            Text(title, style = MaterialTheme.typography.titleLarge, maxLines = 1)
            SelectionContainer {
                Text(feed.address, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (onHome) {
            MenuItem("Remove from Home", leadingIcon = AppIcons.Home, onClick = {
                state.settings.changeHomeTiles(tiles.filterNot { it is HomeTile.Feed && it.feed == feed })
                onDismiss()
                state.offerUndo("Removed from Home") { state.settings.changeHomeTiles(tiles) }
            })
        } else {
            MenuItem("Add to Home", leadingIcon = AppIcons.Home, busy = checking, enabled = !checking, onClick = {
                checking = true
                scope.launch {
                    val read = loadCatching { state.feeds.readEvents(feed) }
                    checking = false
                    onDismiss()
                    if (read is LoadState.Failed) failure = read.message else addToHome()
                }
            })
        }
        if (calendarLink != null) {
            if (subscribed) {
                MenuItem("Unsubscribe", leadingIcon = AppIcons.Rss, onClick = {
                    onDismiss()
                    state.calendars.unsubscribe(calendarLink) { removed -> state.offerUndo("Unsubscribed") { state.calendars.restore(removed) } }
                })
            } else {
                MenuItem("Subscribe", leadingIcon = AppIcons.Rss, onClick = {
                    onDismiss()
                    state.calendars.subscribe(calendarLink)
                })
            }
        }
    }
    BottomSheet(item = failure, onDismiss = { failure = null }) { message ->
        Column(Modifier.padding(horizontal = Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Text("Couldn't Read the Feed", style = MaterialTheme.typography.titleLarge)
            Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { failure = null }) { Text("Cancel") }
                Button(
                    "Add Anyway",
                    onClick = {
                        failure = null
                        addToHome()
                    },
                    primary = true,
                )
            }
        }
    }
}

internal sealed interface LoadState<out T> {
    data object Loading : LoadState<Nothing>
    data class Ready<T>(val value: T) : LoadState<T>
    data class Failed(val message: String) : LoadState<Nothing>
}

@Composable
internal fun <T : Any> produceLoadState(vararg keys: Any?, last: () -> T?, load: suspend () -> T): State<LoadState<T>> =
    produceState<LoadState<T>>(last()?.let { LoadState.Ready(it) } ?: LoadState.Loading, *keys) {
        val shown = last()
        value = shown?.let { LoadState.Ready(it) } ?: LoadState.Loading
        val loaded = loadCatching(load)
        value = if (loaded is LoadState.Failed && shown != null) LoadState.Ready(shown) else loaded
    }

internal suspend fun <T> loadCatching(block: suspend () -> T): LoadState<T> = try {
    LoadState.Ready(block())
} catch (cancelled: CancellationException) {
    throw cancelled
} catch (failed: Exception) {
    LoadState.Failed(failed.message ?: "Unknown error")
}

@Stable
private data class AddProgress(val position: Int, val total: Int)

private class RemoteEventSelection(private val state: AppState) : EventCardActions {
    var events by mutableStateOf(emptyMap<EventSource, ResolvedEvent>())
        private set
    var problems by mutableStateOf(emptyMap<EventSource, String>())
        private set
    var adding by mutableStateOf<AddProgress?>(null)
        private set
    private var anchor: EventSource? = null
    override val isSelecting get() = events.isNotEmpty()

    fun isSelected(event: ResolvedEvent) = event.source in events

    override fun problemOf(event: ResolvedEvent) = problems[event.source]

    fun addAll() {
        val batch = events.values.toList()
        val batchSources = batch.mapTo(HashSet()) { it.source }
        adding = AddProgress(0, batch.size)
        state.addEvents(batch, onProgress = { adding = AddProgress(it, batch.size) }) { failed ->
            events = events.filterKeys { it in failed || it !in batchSources }
            problems = failed
            adding = null
        }
    }

    override fun toggleSelected(event: ResolvedEvent) {
        events = if (isSelected(event)) events - event.source else events + (event.source to event)
        anchor = event.source
    }

    private fun selectRange(shown: List<ResolvedEvent>, event: ResolvedEvent) {
        val from = shown.indexOfFirst { it.source == anchor }
        val to = shown.indexOfFirst { it.source == event.source }
        if (from < 0 || to < 0) return toggleSelected(event)
        selectAll(shown.subList(minOf(from, to), maxOf(from, to) + 1))
        anchor = event.source
    }

    override fun open(event: ResolvedEvent) {
        EventsLink.intentFor(event.source)?.let { state.navigator.open(Route.EventFromLink(it)) }
    }

    override fun click(event: ResolvedEvent, shown: List<ResolvedEvent>, keys: HeldKeys) {
        when {
            keys.shiftAtLastPress -> selectRange(shown, event)
            isSelecting || keys.ctrlOrMetaAtLastPress -> toggleSelected(event)
            else -> open(event)
        }
    }

    override fun selectAll(shown: List<ResolvedEvent>) {
        events += shown.associateBy { it.source }
    }

    override fun actionsFor(event: ResolvedEvent): (() -> Unit)? = if (event.data == null) null else ({ state.openActionsSheet(event) })

    fun clear() {
        events = emptyMap()
        problems = emptyMap()
        anchor = null
    }

    override fun selectionOf(event: ResolvedEvent) = when {
        !isSelecting -> EventCardSelection.NotSelecting
        isSelected(event) -> EventCardSelection.Selected
        else -> EventCardSelection.Unselected
    }

    @Composable
    override fun SelectionBar(shown: List<ResolvedEvent>) {
        RemoteEventSelectionBar(state, this, shown)
    }
}

@Composable
private fun RemoteEventSelectionBar(state: AppState, selection: RemoteEventSelection, shownEvents: List<ResolvedEvent>? = null) {
    val formatting = state.formatting
    val share = rememberShareText()
    val selected = selection.events.values.toList()
    val shareLinks = selected.mapNotNull { EventsLink.shareLinkTo(it.source) }
    val summaries = selected.mapNotNull { it.data?.markdownSummary(formatting, state.preferredLanguages) }
    val unselected = shownEvents?.filterNot(selection::isSelected)

    SelectionBar(
        selectedCount = selected.size,
        onClear = selection::clear,
        onSelectAll = when {
            unselected == null -> state::selectAllShown
            unselected.isNotEmpty() -> ({ selection.selectAll(unselected) })
            else -> null
        },
    ) {
        val adding = selection.adding
        IconButton(
            AppIcons.Plus,
            "Add to My Events",
            onClick = selection::addAll,
            busy = adding != null,
            busyProgress = adding?.let { it.position.toFloat() / it.total },
        )
        if (shareLinks.isNotEmpty()) IconButton(AppIcons.Share, "Share", onClick = { share(shareLinks.joinToString("\n")) })
        if (summaries.isNotEmpty()) CopySummariesButton(summaries)
        selected.singleOrNull()?.let { event -> IconButton(AppIcons.DotsVertical, "More", onClick = { state.openActionsSheet(event) }) }
    }
}
