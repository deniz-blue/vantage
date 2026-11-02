package blue.deniz.vantage.app

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.asPaddingValues
import blue.deniz.vantage.app.ui.Backdrop
import blue.deniz.vantage.app.ui.removal

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import blue.deniz.vantage.app.ui.EventPageGrowingFromCard
import blue.deniz.vantage.app.ui.LocalScreenTransition
import blue.deniz.vantage.app.ui.ScreenTransition
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.ui.Alignment
import blue.deniz.vantage.app.ui.LocalTabBarHeight
import blue.deniz.vantage.app.ui.rememberBackdrop
import blue.deniz.vantage.app.ui.LocalBackdrop
import blue.deniz.vantage.app.ui.glassBar
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.drawBehind
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import androidx.compose.runtime.snapshotFlow
import blue.deniz.vantage.app.screens.LoadState
import blue.deniz.vantage.app.screens.loadCatching
import kotlin.time.Duration.Companion.seconds
import blue.deniz.vantage.core.SHORTEST_REFRESH_INTERVAL
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusTarget
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.utf16CodePoint
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import blue.deniz.vantage.app.screens.MY_EVENTS_SCREEN
import blue.deniz.vantage.app.screens.MyEventsScreen
import blue.deniz.vantage.app.screens.EventsViewControlsClearance
import blue.deniz.vantage.core.CalendarMarks
import blue.deniz.vantage.app.screens.CalendarSelection
import blue.deniz.vantage.app.screens.ChooseFeedSheet
import blue.deniz.vantage.app.screens.CreateEventScreen
import blue.deniz.vantage.app.screens.MarkKind
import blue.deniz.vantage.app.screens.MarkSheet
import blue.deniz.vantage.app.screens.NewEntrySheet
import blue.deniz.vantage.app.screens.MarkSheetTarget
import blue.deniz.vantage.app.screens.EditEventScreen
import blue.deniz.vantage.app.screens.EventDetailsScreen
import blue.deniz.vantage.app.screens.EditFromLinkScreen
import blue.deniz.vantage.app.screens.EventFromLinkScreen
import blue.deniz.vantage.app.screens.EventListFilters
import blue.deniz.vantage.app.screens.EventListFiltersSheet

import blue.deniz.vantage.app.screens.FeedScreen
import blue.deniz.vantage.app.screens.HomeScreen
import blue.deniz.vantage.app.screens.AboutScreen
import blue.deniz.vantage.app.screens.CalendarSettingsScreen
import blue.deniz.vantage.app.screens.HomeSettingsScreen
import blue.deniz.vantage.app.screens.AppearanceSettingsScreen
import blue.deniz.vantage.app.screens.NotificationSettingsScreen
import blue.deniz.vantage.app.screens.BackupSettingsScreen
import blue.deniz.vantage.app.screens.SetupScreen
import blue.deniz.vantage.app.screens.SettingsScreen
import blue.deniz.vantage.app.screens.TagSettingsScreen
import blue.deniz.vantage.app.ui.AppIcons
import blue.deniz.vantage.app.ui.BottomSheetHost
import blue.deniz.vantage.app.ui.LocalScreenOpacity
import blue.deniz.vantage.app.ui.EventFormatting
import blue.deniz.vantage.app.ui.Icon
import blue.deniz.vantage.app.ui.SnackbarHost
import blue.deniz.vantage.app.ui.rememberAnyBottomSheetOpen
import blue.deniz.vantage.app.ui.Text
import blue.deniz.vantage.app.ui.VantageTheme
import blue.deniz.vantage.core.AppSchemes
import blue.deniz.vantage.core.DeviceCalendars
import blue.deniz.vantage.core.EventFeedReader
import blue.deniz.vantage.core.ResolvedEvent
import blue.deniz.vantage.core.CalendarLinkReader
import blue.deniz.vantage.core.EventPreviews
import blue.deniz.vantage.core.EventSource
import blue.deniz.vantage.core.PlaceSearch
import blue.deniz.vantage.core.WeatherForecasts
import blue.deniz.vantage.storage.Backup
import blue.deniz.vantage.storage.EventStore
import io.ktor.http.parseUrl
import kotlin.time.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.todayIn
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.DisposableEffect
import blue.deniz.vantage.app.ui.HeldKeys

private val PHONE_CALENDAR_COPY_DELAY = 2.seconds

@Stable
class AppState(
    val store: EventStore,
    val feeds: EventFeedReader,
    val previews: EventPreviews,
    val places: PlaceSearch,
    val weather: WeatherForecasts,
    settingsStorage: KeyValueStorage,
    calendarLinks: CalendarLinkReader,
    deviceCalendars: DeviceCalendars?,
    val phoneCalendarCopy: PhoneCalendarCopy? = null,
    val backgroundChecks: BackgroundEventChecks? = null,
    val clock: Clock = Clock.System,
) {
    val navigator = Navigator()
    val myEventsFilters = EventListFilters()
    val settings = AppSettings(settingsStorage, TimeZone.currentSystemDefault())
    val calendarSelection = CalendarSelection(clock.todayIn(settings.timeZone))
    val calendars = CalendarSync(store, calendarLinks, deviceCalendars, settings, clock).also { it.syncAll() }
    val timeZone get() = settings.timeZone
    var now by mutableStateOf(clock.now())
        private set
    val today by derivedStateOf { now.toLocalDateTime(timeZone).date }
    var listsSplitAt by mutableStateOf(now)
        private set
    val formatting by derivedStateOf { EventFormatting(timeZone, today) }
    val preferredLanguages by derivedStateOf { listOf(settings.contentLanguage, DEFAULT_CONTENT_LANGUAGE).distinct() }
    var actionsSheetEventId by mutableStateOf<String?>(null)
        private set
    var actionsSheetEvent by mutableStateOf<ResolvedEvent?>(null)
        private set
    var actionsSheetOpen by mutableStateOf(false)
        private set

    val snackbars = SnackbarHostState()

    suspend fun exportBackup(): Backup = store.exportBackup().copy(settings = settings.portableValues())

    suspend fun importBackup(backup: Backup) {
        store.importBackup(backup)
        settings.replacePortableValues(backup.settings)
        calendars.syncAll()
    }
    private val appScope = MainScope()
    var phoneCalendarCopyFailure by mutableStateOf<String?>(null)
        private set
    private var phoneCalendarCopying: Job? = null

    init {
        store.testEventsShown = settings.testEventsShown
        if (settings.notifiesAboutChanges) backgroundChecks?.schedule()
        if (settings.copiesEventsToPhoneCalendar) phoneCalendarCopying = startCopyingToPhoneCalendar()
        appScope.launch {
            while (true) {
                loadCatching { store.refreshDueRemoteEvents() }
                delay(SHORTEST_REFRESH_INTERVAL)
            }
        }
        appScope.launch {
            while (true) {
                delay(60_000 - now.toEpochMilliseconds() % 60_000)
                readClock()
            }
        }
    }

    fun readClock() {
        val reading = clock.now()
        if (reading.toLocalDateTime(timeZone).date != today) listsSplitAt = reading
        now = reading
    }

    fun changeNotifiesAboutChanges(notifies: Boolean) {
        settings.changeNotifiesAboutChanges(notifies)
        if (notifies) backgroundChecks?.schedule() else backgroundChecks?.cancel()
    }

    fun changeCopiesEventsToPhoneCalendar(copies: Boolean) {
        val copy = phoneCalendarCopy ?: return
        settings.changeCopiesEventsToPhoneCalendar(copies)
        val previous = phoneCalendarCopying
        phoneCalendarCopyFailure = null
        phoneCalendarCopying = if (copies) {
            previous?.cancel()
            startCopyingToPhoneCalendar()
        } else {
            appScope.launch {
                previous?.cancelAndJoin()
                (loadCatching { copy.delete() } as? LoadState.Failed)?.let { phoneCalendarCopyFailure = it.message }
            }
            null
        }
    }

    private fun startCopyingToPhoneCalendar(): Job? {
        val copy = phoneCalendarCopy ?: return null
        return appScope.launch {
            combine(store.observeList(), store.observeMarkers(), store.observePeriods(), snapshotFlow { preferredLanguages }) { events, markers, periods, languages ->
                events.toPhoneCalendarEntries(languages) + CalendarMarks(markers, periods).toPhoneCalendarEntries()
            }
                .distinctUntilChanged()
                .collectLatest { entries ->
                    delay(PHONE_CALENDAR_COPY_DELAY)
                    if (!copy.hasPermission()) {
                        phoneCalendarCopyFailure = "Android has calendar access turned off for Vantage"
                        return@collectLatest
                    }
                    val copied = loadCatching { copy.replaceEntries(entries) }
                    phoneCalendarCopyFailure = (copied as? LoadState.Failed)?.message
                }
        }
    }

    fun changeTestEventsShown(shown: Boolean) {
        settings.changeTestEventsShown(shown)
        store.testEventsShown = shown
    }

    fun offerUndo(message: String, undo: suspend () -> Unit) {
        appScope.launch {
            snackbars.currentSnackbarData?.dismiss()
            val result = snackbars.showSnackbar(message, actionLabel = "Undo", withDismissAction = true, duration = SnackbarDuration.Long)
            if (result == SnackbarResult.ActionPerformed) undo()
        }
    }

    var selectedEventIds by mutableStateOf(emptySet<String>())
        private set
    private var selectionAnchorId: String? = null
    private val selectAllActions = mutableListOf<() -> Unit>()

    fun toggleEventSelected(id: String) {
        selectedEventIds = if (id in selectedEventIds) selectedEventIds - id else selectedEventIds + id
        selectionAnchorId = id
    }

    fun selectEventRange(shownIds: List<String>, id: String) {
        val from = shownIds.indexOf(selectionAnchorId)
        val to = shownIds.indexOf(id)
        if (from < 0 || to < 0) return toggleEventSelected(id)
        selectEvents(shownIds.subList(minOf(from, to), maxOf(from, to) + 1))
        selectionAnchorId = id
    }

    fun clickEventCard(id: String, shownIds: List<String>, keys: HeldKeys) {
        when {
            keys.shiftAtLastPress -> selectEventRange(shownIds, id)
            selectedEventIds.isNotEmpty() || keys.ctrlOrMetaAtLastPress -> toggleEventSelected(id)
            else -> openEvent(id, fromCard = true)
        }
    }

    fun selectAllShown() {
        selectAllActions.lastOrNull()?.invoke()
    }

    fun addSelectAllAction(action: () -> Unit) {
        selectAllActions += action
    }

    fun removeSelectAllAction(action: () -> Unit) {
        selectAllActions -= action
    }

    fun selectEvents(ids: Collection<String>) {
        selectedEventIds += ids
    }

    fun clearEventSelection() {
        selectedEventIds = emptySet()
        selectionAnchorId = null
    }

    fun removeEvent(id: String) = removeEvents(setOf(id))

    fun removeEvents(ids: Set<String>) {
        selectedEventIds -= ids
        appScope.launch {
            val removed = ids.mapNotNull { store.remove(it) }
            if (removed.isEmpty()) return@launch
            val done = removed.map { it.source.removal }.distinct().singleOrNull()?.done ?: "Removed"
            offerUndo(if (removed.size == 1) "Event $done" else "${removed.size} Events $done") { removed.forEach { store.restore(it) } }
        }
    }

    fun addEvents(
        events: Collection<ResolvedEvent>,
        onProgress: (position: Int) -> Unit = {},
        onFinished: (problems: Map<EventSource, String>) -> Unit = {},
    ) {
        appScope.launch {
            val problems = mutableMapOf<EventSource, String>()
            val addedIds = mutableListOf<String>()
            for ((index, event) in events.withIndex()) {
                onProgress(index + 1)
                val read = if (event.data != null) event else previews.resolve(event)
                if (read.data == null) {
                    problems[event.source] = read.error?.message ?: "No event at this link"
                    continue
                }
                if (store.findStoredCopyId(read) != null) continue
                runCatching { store.add(read) }
                    .onSuccess { addedIds += it }
                    .onFailure { problems[event.source] = it.message ?: "Couldn't add this event" }
            }
            onFinished(problems)
            val failedPart = if (problems.isEmpty()) "" else ", ${problems.size} Failed"
            if (addedIds.isNotEmpty()) {
                offerUndo((if (addedIds.size == 1) "Event Added" else "${addedIds.size} Events Added") + failedPart) { addedIds.forEach { store.remove(it) } }
                return@launch
            }
            snackbars.currentSnackbarData?.dismiss()
            snackbars.showSnackbar(
                when {
                    problems.isNotEmpty() -> if (problems.size == 1) "Couldn't Add the Event" else "Couldn't Add ${problems.size} Events"
                    events.size == 1 -> "Already in My Events"
                    else -> "All Already in My Events"
                },
                withDismissAction = true,
            )
        }
    }

    fun deleteTag(id: String) {
        appScope.launch {
            val removed = store.deleteTag(id) ?: return@launch
            offerUndo("Tag Deleted: ${removed.tag.name}") { store.restoreTag(removed) }
        }
    }

    fun showEventsTagged(tagId: String) {
        myEventsFilters.search = ""
        myEventsFilters.tagIds = setOf(tagId)
        openMyEvents(EventsView.List)
    }

    fun openActionsSheet(eventId: String) {
        actionsSheetEventId = eventId
        actionsSheetEvent = null
        actionsSheetOpen = true
    }

    fun openActionsSheet(event: ResolvedEvent) {
        actionsSheetEventId = event.id
        actionsSheetEvent = event
        actionsSheetOpen = true
        if (event.id != null) return
        appScope.launch {
            val storedId = store.findStoredCopyId(event) ?: return@launch
            if (actionsSheetEvent == event && actionsSheetEventId == null) actionsSheetEventId = storedId
        }
    }

    fun closeActionsSheet() {
        actionsSheetOpen = false
    }

    var markSheetTarget by mutableStateOf<MarkSheetTarget?>(null)
        private set
    var markSheetOpen by mutableStateOf(false)
        private set

    fun openMarkSheet(target: MarkSheetTarget) {
        markSheetTarget = target
        markSheetOpen = true
    }

    fun closeMarkSheet() {
        markSheetOpen = false
    }

    var newEntryDay by mutableStateOf<LocalDate?>(null)
        private set
    var chooseFeedSheetOpen by mutableStateOf(false)
        private set

    fun openChooseFeedSheet() {
        chooseFeedSheetOpen = true
    }

    fun closeChooseFeedSheet() {
        chooseFeedSheetOpen = false
    }

    var newEntrySheetOpen by mutableStateOf(false)
        private set

    fun openNewEntrySheet(day: LocalDate? = null) {
        newEntryDay = day
        newEntrySheetOpen = true
    }

    fun closeNewEntrySheet() {
        newEntrySheetOpen = false
    }

    var linkActionsSheetUrl by mutableStateOf<String?>(null)
        private set
    var linkActionsSheetOpen by mutableStateOf(false)
        private set

    fun openLinkActionsSheet(url: String) {
        linkActionsSheetUrl = url
        linkActionsSheetOpen = true
    }

    fun closeLinkActionsSheet() {
        linkActionsSheetOpen = false
    }

    var eventSearchFocusRequested by mutableStateOf(false)
        private set

    val myEventsView get() = settings.eventsViewOf(MY_EVENTS_SCREEN, EventsView.List)

    fun openMyEvents(view: EventsView) {
        settings.changeEventsView(MY_EVENTS_SCREEN, view)
        navigator.switchTab(Route.MyEvents)
    }

    fun showsCalendar(route: Route) = route == Route.MyEvents && myEventsView == EventsView.Calendar

    fun focusEventSearch() {
        if (navigator.current != Route.MyEvents || !myEventsView.searches) openMyEvents(EventsView.List)
        eventSearchFocusRequested = true
    }

    fun onEventSearchFocused() {
        eventSearchFocusRequested = false
    }

    fun handleShortcut(event: KeyEvent): Boolean {
        if (event.type != KeyEventType.KeyDown) return false
        if (event.key == Key.Escape) return handleEscapeKey()
        if (event.isCtrlPressed || event.isMetaPressed) {
            val selectAll = selectAllActions.lastOrNull()?.takeIf { event.key == Key.A } ?: return false
            selectAll()
            return true
        }
        if (event.isAltPressed) return false
        val onCalendar = showsCalendar(navigator.current)
        when {
            event.utf16CodePoint == '/'.code && navigator.entries.none { it is Route.Form } -> focusEventSearch()
            onCalendar && event.key == Key.DirectionLeft -> calendarSelection.showPreviousMonth()
            onCalendar && event.key == Key.DirectionRight -> calendarSelection.showNextMonth()
            else -> return false
        }
        return true
    }

    fun openEvent(id: String, fromCard: Boolean = false) {
        navigator.open(Route.EventDetails(id, openedFromCard = fromCard))
    }

    fun openSharedText(text: String) {
        navigator.open(Route.CreateEvent(sharedText = text))
    }

    fun openLink(url: String) {
        val route = parseUrl(url)?.let(AppSchemes::webAddressOf)?.let(::routeAtWebAddress) ?: return
        if (route is Route.Tab) navigator.switchTab(route) else navigator.open(route)
    }
}

private data class TabItem(val tab: Route.Tab, val label: String, val icon: ImageVector, val selectedIcon: ImageVector)

private val TAB_BAR_HEIGHT = 52.dp

private val tabs = listOf(
    TabItem(Route.Home, "Home", AppIcons.Home, AppIcons.HomeSelected),
    TabItem(Route.MyEvents, "My Events", AppIcons.Calendar, AppIcons.CalendarSelected),
    TabItem(Route.Settings, "Settings", AppIcons.Settings, AppIcons.SettingsSelected),
)

private data class StackEntry(val depth: Int, val screen: OpenedScreen) {
    val route get() = screen.route
    val screenStateKey get() = if (route is Route.Tab) "tabs" else "screen/${screen.openingId}"
}

private fun Modifier.blockingPointerInputWhile(blocked: () -> Boolean) = pointerInput(Unit) {
    awaitPointerEventScope {
        while (true) {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            if (blocked()) event.changes.forEach { it.consume() }
        }
    }
}

private val ScreenFadeIn = tween<Float>(210, delayMillis = 90)
private val ScreenFadeOut = tween<Float>(90)

private fun AnimatedContentTransitionScope<StackEntry>.screenTransition(): ContentTransform {
    val pushing = targetState.depth > initialState.depth
    val popping = targetState.depth < initialState.depth
    if (!pushing && !popping) return fadeIn(ScreenFadeIn) togetherWith fadeOut(ScreenFadeOut)
    val page = if (pushing) targetState.route else initialState.route
    if (page is Route.EventDetails && page.openedFromCard) {
        return (EnterTransition.None togetherWith ExitTransition.KeepUntilTransitionsFinished).apply { if (popping) targetContentZIndex = -1f }
    }
    val direction = if (pushing) 1 else -1
    return (slideInHorizontally(tween(300)) { direction * it / 4 } + fadeIn(ScreenFadeIn)) togetherWith
        (slideOutHorizontally(tween(300)) { -direction * it / 4 } + fadeOut(ScreenFadeOut))
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun VantageApp(state: AppState) {
    val navigator = state.navigator
    val screenStates = rememberSaveableStateHolder()
    val screenValues = remember { ScreenValueStore() }
    val sessionValues = remember { mutableMapOf<String, MutableState<*>>() }
    val stackedScreenKeys = navigator.screens.withIndex()
        .map { (depth, screen) -> StackEntry(depth, screen).screenStateKey }
        .filterNot { it == "tabs" }
        .toSet()
    var keptScreenKeys by remember { mutableStateOf(emptySet<String>()) }
    LaunchedEffect(stackedScreenKeys) {
        for (gone in keptScreenKeys - stackedScreenKeys) {
            screenStates.removeState(gone)
            screenValues.forget(gone)
        }
        keptScreenKeys = stackedScreenKeys
    }
    SystemBackHandler(enabled = navigator.canGoBack) { navigator.goBack() }
    LaunchedEffect(Unit) {
        if (!state.settings.setupFinished && navigator.current is Route.Tab) navigator.open(Route.Setup)
    }
    val tabBackdrop = rememberBackdrop()
    CompositionLocalProvider(LocalSessionValues provides sessionValues) {
        VantageTheme(state.settings.themeAccent, state.settings.fontSizes) {
            KeyboardShortcutHost(onShortcut = state::handleShortcut) {
                BottomSheetHost(aboveSheets = { SnackbarsOutsideTabBar(state, onTab = navigator.current is Route.Tab) }) {
                    Surface(Modifier.fillMaxSize()) {
                        Column(Modifier.fillMaxSize()) {
                            if (runsInAndroidBrowser) AndroidAppBanner()
                            Box(Modifier.fillMaxWidth().weight(1f)) {
                            SharedTransitionLayout(Modifier.fillMaxSize()) {
                                AnimatedContent(
                                    targetState = StackEntry(navigator.screens.lastIndex, navigator.screens.last()),
                                    transitionSpec = { screenTransition() },
                                    contentKey = { it.screenStateKey },
                                ) { entry ->
                                    val screenTransition = remember(this) { ScreenTransition(this@SharedTransitionLayout, this) }
                                    val opacity = transition.animateFloat(
                                        transitionSpec = { if (targetState == EnterExitState.Visible) ScreenFadeIn else ScreenFadeOut },
                                        label = "screen opacity",
                                    ) { if (it == EnterExitState.Visible) 1f else 0f }
                                    screenStates.SaveableStateProvider(entry.screenStateKey) {
                                        CompositionLocalProvider(
                                            LocalScreenValues provides screenValues.valuesFor(entry.screenStateKey),
                                            LocalStackDepth provides entry.depth,
                                            LocalScreenOpacity provides remember(opacity) { { opacity.value } },
                                            LocalScreenTransition provides screenTransition,
                                        ) {
                                            Box(Modifier.blockingPointerInputWhile { transition.targetState != EnterExitState.Visible }, propagateMinConstraints = true) {
                                                when (val route = entry.route) {
                                                    is Route.Tab -> TabShell(state, route, tabBackdrop, showsSnackbars = route == navigator.current)
                                                    is Route.EventDetails -> if (route.openedFromCard) {
                                                        EventPageGrowingFromCard(route.id) { EventDetailsScreen(state, route.id) }
                                                    } else {
                                                        EventDetailsScreen(state, route.id)
                                                    }
                                                    is Route.EventFromLink -> EventFromLinkScreen(state, route.intent)
                                                    is Route.Feed -> FeedScreen(state, route.feed)
                                                    is Route.CreateEvent -> CreateEventScreen(state, route.sharedText, route.day, route.draft)
                                                    is Route.EditEvent -> EditEventScreen(state, route.id, route.draft)
                                                    is Route.EditFromLink -> EditFromLinkScreen(state, route.request)
                                                    Route.AppearanceSettings -> AppearanceSettingsScreen(state)
                                                    Route.NotificationSettings -> NotificationSettingsScreen(state)
                                                    Route.BackupSettings -> BackupSettingsScreen(state)
                                                    Route.Setup -> SetupScreen(state)
                                                    Route.HomeSettings -> HomeSettingsScreen(state)
                                                    Route.TagSettings -> TagSettingsScreen(state)
                                                    is Route.CalendarSettings -> CalendarSettingsScreen(state, route.offeredLink)
                                                    Route.About -> AboutScreen(state)
                                                    Route.DeveloperTools -> DeveloperToolsScreen(state)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            SlidingTabBar(
                                visible = navigator.current is Route.Tab,
                                selected = navigator.currentTab,
                                backdrop = tabBackdrop,
                                onSelect = navigator::switchTab,
                                modifier = Modifier.align(Alignment.BottomCenter),
                            )
                            }
                        }
                        EventActionsSheet(state)
                        LinkActionsSheet(state)
                        MarkSheet(state)
                        NewEntrySheet(state)
                        ChooseFeedSheet(state)
                    }
                }
            }
        }
    }
}

@Composable
private fun KeyboardShortcutHost(onShortcut: (KeyEvent) -> Boolean, content: @Composable () -> Unit) {
    val focus = remember { FocusRequester() }
    var holdsFocus by remember { mutableStateOf(false) }
    LaunchedEffect(holdsFocus) { if (!holdsFocus) focus.requestFocus() }
    Box(
        Modifier
            .fillMaxSize()
            .onKeyEvent(onShortcut)
            .onFocusChanged { holdsFocus = it.hasFocus }
            .focusRequester(focus)
            .focusTarget(),
    ) { content() }
}

@Composable
private fun SlidingTabBar(visible: Boolean, selected: Route.Tab, backdrop: Backdrop, onSelect: (Route.Tab) -> Unit, modifier: Modifier = Modifier) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = slideInVertically(tween(300)) { it },
        exit = slideOutVertically(tween(300)) { it },
    ) {
        CompositionLocalProvider(LocalBackdrop provides backdrop) { TabBar(selected, onSelect) }
    }
}

@Composable
private fun TabBar(selected: Route.Tab, onSelect: (Route.Tab) -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(
        Modifier
            .fillMaxWidth()
            .glassBar(colors.surfaceContainer)
            .drawBehind { drawLine(colors.outlineVariant, Offset.Zero, Offset(size.width, 0f), 1f) }
            .navigationBarsPadding()
            .height(TAB_BAR_HEIGHT)
            .selectableGroup(),
    ) {
        for (item in tabs) {
            val isSelected = item.tab == selected
            val tint = if (isSelected) colors.primary else colors.onSurfaceVariant
            Column(
                Modifier.weight(1f).fillMaxHeight().selectable(isSelected, role = Role.Tab, onClick = { onSelect(item.tab) }),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(if (isSelected) item.selectedIcon else item.icon, tint = tint)
                Text(item.label, color = tint, maxLines = 1, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun SnackbarsOutsideTabBar(state: AppState, onTab: Boolean) {
    val anySheetOpen by rememberAnyBottomSheetOpen()
    if (onTab && !anySheetOpen) return
    Box(
        Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime)),
        contentAlignment = Alignment.BottomCenter,
    ) { SnackbarHost(state.snackbars) }
}

@Composable
private fun TabShell(state: AppState, tab: Route.Tab, backdrop: Backdrop, showsSnackbars: Boolean) {
    val anySheetOpen by rememberAnyBottomSheetOpen()
    var showingListFilters by rememberSaveable { mutableStateOf(false) }
    val tabStates = rememberSaveableStateHolder()
    SystemBackHandler(enabled = tab != Route.Home) { state.navigator.switchTab(Route.Home) }
    val tabBarHeight = TAB_BAR_HEIGHT + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    CompositionLocalProvider(LocalBackdrop provides backdrop) {
        Scaffold(
            snackbarHost = {
                if (showsSnackbars && !anySheetOpen) {
                    SnackbarHost(state.snackbars, Modifier.padding(bottom = tabBarHeight + if (tab == Route.MyEvents) EventsViewControlsClearance else 0.dp))
                }
            },
            contentWindowInsets = WindowInsets(0),
        ) {
            CompositionLocalProvider(LocalTabBarHeight provides tabBarHeight) {
                AnimatedContent(
                    targetState = tab,
                    transitionSpec = {
                        (fadeIn(tween(210, delayMillis = 90)) + scaleIn(tween(210, delayMillis = 90), initialScale = 0.92f)) togetherWith
                            fadeOut(tween(90))
                    },
                ) { shown ->
                    tabStates.SaveableStateProvider(shown.toString()) {
                        when (shown) {
                            Route.Home -> HomeScreen(state)
                            Route.MyEvents -> MyEventsScreen(state, onOpenFilters = { showingListFilters = true })
                            Route.Settings -> SettingsScreen(state)
                        }
                    }
                }
            }
        }
    }
    if (tab == Route.MyEvents) EventListFiltersSheet(state, visible = showingListFilters, onDismiss = { showingListFilters = false })
}

@Composable
fun SelectAllShortcut(state: AppState, onSelectAll: () -> Unit) {
    val currentOnSelectAll by rememberUpdatedState(onSelectAll)
    DisposableEffect(state) {
        val action = { currentOnSelectAll() }
        state.addSelectAllAction(action)
        onDispose { state.removeSelectAllAction(action) }
    }
}
