package blue.deniz.vantage.app.screens

import blue.deniz.vantage.app.ui.tagColorOrNull

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import blue.deniz.vantage.app.AppState
import blue.deniz.vantage.app.collectAcrossSession
import blue.deniz.vantage.app.ui.EventCardSelection
import blue.deniz.vantage.app.ui.EventFormatting
import blue.deniz.vantage.app.ui.HeldKeys
import blue.deniz.vantage.app.ui.shortLabel
import blue.deniz.vantage.core.EventListing
import blue.deniz.vantage.core.EventSource
import blue.deniz.vantage.core.ResolvedEvent
import blue.deniz.vantage.storage.EventListQuery
import blue.deniz.vantage.storage.TagEntity
import directory.evnt.translate
import kotlin.time.Instant

@Stable
interface EventCardActions {
    val isSelecting: Boolean

    fun selectionOf(event: ResolvedEvent): EventCardSelection

    fun open(event: ResolvedEvent)

    fun click(event: ResolvedEvent, shown: List<ResolvedEvent>, keys: HeldKeys)

    fun toggleSelected(event: ResolvedEvent)

    fun startSelecting()

    fun selectAll(shown: List<ResolvedEvent>)

    fun actionsFor(event: ResolvedEvent): (() -> Unit)?

    fun problemOf(event: ResolvedEvent): String? = null

    @Composable
    fun SelectionBar(shown: List<ResolvedEvent>)
}

class StoredEventCardActions(private val state: AppState) : EventCardActions {
    override val isSelecting get() = state.isSelectingEvents

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

    override fun startSelecting() {
        state.startSelectingEvents()
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
    private val calendarColorOf: (EventSource) -> Color?,
) {
    fun tagsOf(event: ResolvedEvent) = event.id?.let(tagsByEvent::get).orEmpty()

    fun changeLabelOf(event: ResolvedEvent) = event.id?.let(changeLabels::get)

    fun accentColorOf(event: ResolvedEvent) = calendarColorOf(event.source) ?: tagsOf(event).firstOrNull()?.color?.let(::tagColorOrNull)

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
