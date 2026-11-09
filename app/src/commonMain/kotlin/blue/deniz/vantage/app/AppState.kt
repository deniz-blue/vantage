package blue.deniz.vantage.app

import blue.deniz.vantage.app.ui.removal

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
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
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.utf16CodePoint
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import blue.deniz.vantage.app.screens.MY_EVENTS_SCREEN
import blue.deniz.vantage.core.CalendarMarks
import blue.deniz.vantage.app.screens.CalendarSelection
import blue.deniz.vantage.app.screens.MarkSheetTarget
import blue.deniz.vantage.app.screens.EventListFilters

import blue.deniz.vantage.app.ui.EventFormatting
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
    private var eventSelectionStarted by mutableStateOf(false)
    val isSelectingEvents get() = eventSelectionStarted || selectedEventIds.isNotEmpty()
    private var selectionAnchorId: String? = null
    private val selectAllActions = mutableListOf<() -> Unit>()

    fun startSelectingEvents() {
        eventSelectionStarted = true
    }

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
            isSelectingEvents || keys.ctrlOrMetaAtLastPress -> toggleEventSelected(id)
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
        eventSelectionStarted = false
        selectionAnchorId = null
    }

    fun removeEvent(id: String) = removeEvents(setOf(id))

    fun removeEvents(ids: Set<String>) {
        val removesWholeSelection = selectedEventIds.isNotEmpty() && ids.containsAll(selectedEventIds)
        selectedEventIds -= ids
        if (removesWholeSelection) eventSelectionStarted = false
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
                when (val added = addEvent(event)) {
                    is AddedEvent.Added -> addedIds += added.id
                    is AddedEvent.AlreadyStored -> {}
                    is AddedEvent.Failed -> problems[event.source] = added.problem
                }
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

    suspend fun addEvent(event: ResolvedEvent): AddedEvent {
        val read = if (event.data != null) event else previews.resolve(event)
        if (read.data == null) return AddedEvent.Failed(read.error?.message ?: "No event at this link")
        store.findStoredCopyId(read)?.let { return AddedEvent.AlreadyStored(it) }
        return runCatching { AddedEvent.Added(store.add(read)) }
            .getOrElse { AddedEvent.Failed(it.message ?: "Couldn't add this event") }
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
