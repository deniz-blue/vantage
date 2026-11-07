package blue.deniz.vantage.app.screens

import blue.deniz.vantage.app.ui.LoadingSpinner
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import blue.deniz.vantage.app.AppState
import blue.deniz.vantage.app.Route
import blue.deniz.vantage.app.SystemBackHandler
import blue.deniz.vantage.app.actionsSheetEventKey
import blue.deniz.vantage.app.latestAcrossSession
import blue.deniz.vantage.app.rememberSendEditReply
import blue.deniz.vantage.core.EventsLink
import blue.deniz.vantage.core.ResolvedEvent
import androidx.compose.runtime.produceState
import androidx.compose.foundation.layout.ColumnScope
import blue.deniz.vantage.app.ui.Card
import blue.deniz.vantage.app.ui.Radius
import blue.deniz.vantage.app.ui.ScreenHeader
import blue.deniz.vantage.app.ui.Spacing
import blue.deniz.vantage.app.ui.Text
import blue.deniz.vantage.core.isEditableHere
import blue.deniz.vantage.core.findEventLink
import directory.evnt.OpenEvnt
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.first
import kotlinx.datetime.LocalDate
import blue.deniz.vantage.app.ui.ContentMaxWidth

@Composable
fun CreateEventScreen(state: AppState, sharedText: String?, day: LocalDate?, draft: OpenEvnt?) {
    val sharedLink = remember(sharedText) { sharedText?.let(::findEventLink) }
    val blank = remember {
        val blank = newBlankEvent(day, state.timeZone.id)
        val text = sharedText?.trim()?.takeUnless { sharedLink != null && it.replace(sharedLink, "").isBlank() }
        if (text.isNullOrEmpty()) blank else blank.filledFromText(text, state.today, state.timeZone.id, state.preferredLanguages)
    }
    var destination by rememberSaveable { mutableStateOf(NewEventDestination.Device) }
    EventForm(
        state,
        "New Event",
        initial = draft ?: blank,
        unchanged = blank,
        onDiscard = { discarded -> state.offerUndo("Draft Discarded") { state.navigator.openDraft(Route.CreateEvent(draft = discarded)) } },
        offersImport = true,
        sharedLink = sharedLink,
        destinationRow = { DestinationRow(destination, onSelect = { destination = it }) },
    ) { event ->
        val id = when (destination) {
            NewEventDestination.Device -> state.store.addLocal(event)
            NewEventDestination.Folio -> state.store.addToFolio(event)
        }
        state.navigator.replace(Route.EventDetails(id))
    }
}

@OptIn(ExperimentalUuidApi::class)
@Composable
fun EditFromLinkScreen(state: AppState, request: EventsLink.EditRequest) {
    val sendReply = rememberSendEditReply()
    val linked = remember(request) { ResolvedEvent.fromEventsLinkIntent(request.event) }
    val title = if (linked == null) "New Event" else "Edit Event"
    val loaded by produceState<LoadState<OpenEvnt>>(LoadState.Loading, linked) {
        if (linked == null) {
            value = LoadState.Ready(OpenEvnt(id = "urn:uuid:${Uuid.random()}", name = emptyMap()))
            return@produceState
        }
        val resolved = state.previews.resolve(linked)
        value = resolved.data?.let { LoadState.Ready(it) } ?: LoadState.Failed(resolved.error?.message ?: "No event at this link")
    }

    fun reply(reply: EventsLink.EditReply) {
        sendReply(reply)
        state.navigator.goBack()
    }
    val cancel = { reply(EventsLink.EditReply.Cancelled(request)) }
    val subtitle = "Save sends the event back to ${request.returnHost}"
    when (val load = loaded) {
        is LoadState.Ready -> EventForm(
            state,
            title,
            initial = load.value,
            unchanged = load.value,
            onDiscard = {},
            subtitle = subtitle,
            onCancel = cancel,
        ) { edited ->
            reply(EventsLink.EditReply.Saved(request, edited.toJson().toString()))
        }
        else -> {
            SystemBackHandler(enabled = true, onBack = cancel)
            Column(Modifier.fillMaxSize()) {
                ScreenHeader(title, onClose = cancel)
                FormSubtitle(subtitle)
                if (load is LoadState.Failed) {
                    Text(load.message, Modifier.padding(Spacing.md), color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    LoadingSpinner()
                }
            }
        }
    }
}

@Composable
internal fun ColumnScope.FormSubtitle(text: String) {
    Text(
        text,
        Modifier.align(Alignment.CenterHorizontally).widthIn(max = ContentMaxWidth).fillMaxWidth().padding(start = Spacing.md, end = Spacing.md, bottom = Spacing.sm),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodyMedium,
    )
}

@Composable
fun EditEventScreen(state: AppState, id: String, draft: OpenEvnt?) {
    val known = latestAcrossSession<ResolvedEvent>(eventPageKey(id)) ?: latestAcrossSession<ResolvedEvent>(actionsSheetEventKey(id))
    var stored by remember(id) { mutableStateOf(known) }
    var reading by remember(id) { mutableStateOf(known == null) }
    LaunchedEffect(id) {
        if (reading) {
            stored = state.store.observe(id).first()
            reading = false
        }
    }
    val current = stored
    val problem = when {
        reading -> null
        current == null -> "This event is gone"
        !current.source.isEditableHere -> "Only events made on this device can be edited"
        current.data == null -> "This event has no data to edit"
        else -> null
    }
    val event = current?.data?.takeIf { problem == null }
    if (event == null) {
        Column(Modifier.fillMaxSize()) {
            ScreenHeader("Edit Event", onBack = { state.navigator.goBack() })
            problem?.let { Text(it, Modifier.padding(Spacing.md), color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        return
    }
    EventForm(
        state,
        "Edit Event",
        initial = draft ?: event,
        unchanged = event,
        onDiscard = { discarded -> state.offerUndo("Changes Discarded") { state.navigator.openDraft(Route.EditEvent(id, discarded)) } },
    ) { edited ->
        state.store.saveEdited(id, edited)
        state.navigator.goBack()
    }
}

internal enum class NewEventDestination(val label: String) {
    Device("This Device"),
    Folio("Folio"),
}
