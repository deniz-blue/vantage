package blue.deniz.vantage.app.screens

import blue.deniz.vantage.app.ui.LoadingSpinner
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import blue.deniz.vantage.app.AppState
import blue.deniz.vantage.app.LocalStackDepth
import blue.deniz.vantage.app.OpenEvntSaver
import blue.deniz.vantage.app.countryName
import blue.deniz.vantage.app.Route
import blue.deniz.vantage.app.SystemBackHandler
import blue.deniz.vantage.app.actionsSheetEventKey
import blue.deniz.vantage.app.latestAcrossSession
import blue.deniz.vantage.app.rememberSendEditReply
import blue.deniz.vantage.core.Countries
import blue.deniz.vantage.core.EventsLink
import blue.deniz.vantage.core.isCancelled
import blue.deniz.vantage.core.ResolvedEvent
import androidx.compose.runtime.produceState
import androidx.compose.foundation.layout.ColumnScope
import blue.deniz.vantage.app.ui.FailureRow
import blue.deniz.vantage.app.ui.ListRowGap
import blue.deniz.vantage.app.ui.ListRowIconGap
import blue.deniz.vantage.app.ui.ListRowPadding
import blue.deniz.vantage.app.ui.ListRowHeight
import blue.deniz.vantage.app.ui.IconSize
import blue.deniz.vantage.app.ui.AppIcons
import blue.deniz.vantage.app.ui.BottomSheet
import blue.deniz.vantage.app.ui.BottomSheetFrame
import blue.deniz.vantage.app.ui.BottomSheetScope
import blue.deniz.vantage.app.ui.Button
import blue.deniz.vantage.app.ui.Card
import blue.deniz.vantage.app.ui.EventFormatting
import blue.deniz.vantage.app.ui.Icon
import blue.deniz.vantage.app.ui.IconButton
import blue.deniz.vantage.app.ui.MarkdownToolbar
import blue.deniz.vantage.app.ui.MarkdownView
import blue.deniz.vantage.app.ui.rememberMarkdownHighlighting
import blue.deniz.vantage.app.ui.HeaderButton
import blue.deniz.vantage.app.ui.ListRow
import blue.deniz.vantage.app.ui.dividerAboveInListSection
import blue.deniz.vantage.app.ui.ListSection
import blue.deniz.vantage.app.ui.Radius
import blue.deniz.vantage.app.ui.RemoveButton
import blue.deniz.vantage.app.ui.ScreenHeader
import blue.deniz.vantage.app.ui.Spacing
import blue.deniz.vantage.app.ui.Text
import blue.deniz.vantage.app.ui.SheetTitle
import blue.deniz.vantage.app.ui.TextInput
import blue.deniz.vantage.app.ui.linkText
import blue.deniz.vantage.core.isEditableHere
import blue.deniz.vantage.core.Markdown
import blue.deniz.vantage.core.backgroundSplashMedia
import blue.deniz.vantage.core.findEventLink
import blue.deniz.vantage.core.withBackgroundSplashMedia
import blue.deniz.vantage.core.withLinkMoved
import directory.evnt.Component
import directory.evnt.Occurrence
import directory.evnt.OpenEvnt
import directory.evnt.PartialDate
import directory.evnt.Translations
import directory.evnt.ValidationIssue
import directory.evnt.Venue
import directory.evnt.hasText
import directory.evnt.textToEdit
import directory.evnt.translate
import directory.evnt.withEditedText
import directory.evnt.withoutBlankTexts
import io.ktor.http.parseUrl
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
import sh.calvin.reorderable.ReorderableColumn
import blue.deniz.vantage.app.ui.ContentMaxWidth

private const val NEW_LINK = -1

private val STATUS_OPTIONS = listOf<Pair<OpenEvnt.Status?, String>>(
    null to "Planned",
    OpenEvnt.Status.Uncertain to "Uncertain",
    OpenEvnt.Status.Postponed to "Postponed",
    OpenEvnt.Status.Cancelled to "Cancelled",
)

private val OpenEvnt.Status?.icon get() = when (this) {
    null -> AppIcons.CalendarCheck
    OpenEvnt.Status.Uncertain -> AppIcons.QuestionCircle
    OpenEvnt.Status.Postponed -> AppIcons.ClockUpdate
    OpenEvnt.Status.Cancelled -> AppIcons.CalendarOff
}

internal fun Translations?.nullIfBlank() = this?.withoutBlankTexts()?.takeIf { it.isNotEmpty() }

private fun OpenEvnt.description() = components.orEmpty().filterIsInstance<Component.MarkdownText>().firstOrNull()

internal fun OpenEvnt.withDescription(preferredLanguages: List<String>, text: String): OpenEvnt {
    val current = components.orEmpty()
    val index = current.indexOfFirst { it is Component.MarkdownText }
    val existing = current.getOrNull(index) as Component.MarkdownText?
    val description = Component.MarkdownText(existing?.content.withEditedText(preferredLanguages, text), existing?.flavor ?: "gfm")
    return copy(components = if (index < 0) current + description else current.toMutableList().apply { set(index, description) })
}

private fun OpenEvnt.links() = components.orEmpty().withIndex().filter { it.value is Component.Link }

private fun OpenEvnt.withComponent(index: Int, component: Component?): OpenEvnt =
    copy(components = components.orEmpty().toMutableList().apply { if (component == null) removeAt(index) else set(index, component) })

private fun OpenEvnt.withOccurrence(index: Int, occurrence: Occurrence?): OpenEvnt =
    copy(occurrences = occurrences.orEmpty().toMutableList().apply { if (occurrence == null) removeAt(index) else set(index, occurrence) })

private fun PartialDate.nextDay(): PartialDate = when (this) {
    is PartialDate.Day -> PartialDate.Day(toLocalDate().plus(DatePeriod(days = 1)), timeZoneId)
    is PartialDate.Time -> PartialDate.Day(toLocalDate().plus(DatePeriod(days = 1)), timeZoneId).toTime(hour, minute)
    else -> this
}

private fun OpenEvnt.withNextOccurrence(today: PartialDate): OpenEvnt {
    val start = occurrences?.lastOrNull()?.start?.nextDay() ?: today
    return copy(occurrences = occurrences.orEmpty() + Occurrence(start = start))
}

private fun OpenEvnt.cleaned(): OpenEvnt = copy(
    name = name.withoutBlankTexts(),
    venue = venue?.let {
        Venue(
            name = it.name.nullIfBlank(),
            address = it.address?.takeIf(String::isNotBlank),
            countryCode = it.countryCode?.takeIf(String::isNotBlank),
            components = it.components,
        )
    }?.takeIf { it != Venue() },
    occurrences = occurrences?.filter { it.start != null }?.takeIf { it.isNotEmpty() },
    components = components?.mapNotNull {
        when (it) {
            is Component.MarkdownText -> it.content.nullIfBlank()?.let { content -> it.copy(content = content) }
            is Component.Link -> it.takeIf { link -> link.url.isNotBlank() }?.copy(name = it.name.nullIfBlank())
            else -> it
        }
    }?.takeIf { it.isNotEmpty() },
)

private val FIELD_LABELS = mapOf("url" to "URL", "countryCode" to "country", "content" to "text")

private fun fieldLabel(key: String) = FIELD_LABELS[key] ?: key

private fun OpenEvnt.componentLabel(index: Int?): String = when (val component = components?.getOrNull(index ?: -1)) {
    is Component.Link -> "Link ${links().indexOfFirst { it.index == index } + 1}"
    is Component.MarkdownText -> "Description"
    else -> component?.typeId ?: "Component"
}

private fun OpenEvnt.hasNoName() = name.values.all { it.isBlank() }

private fun OpenEvnt.describe(issue: ValidationIssue): String {
    val path = issue.path
    val index = path.getOrNull(1)?.toIntOrNull()
    val subject = when (path.firstOrNull()) {
        "name" -> "Event Name"
        "status" -> "Status"
        "venue" -> listOfNotNull("Location", path.getOrNull(1)?.let(::fieldLabel)).joinToString(" ")
        "occurrences" -> listOfNotNull("Date ${(index ?: 0) + 1}", path.getOrNull(2)?.let(::fieldLabel)).joinToString(" ")
        "components" -> listOfNotNull(componentLabel(index), path.getOrNull(2)?.let(::fieldLabel)).joinToString(" ")
        else -> path.joinToString(".")
    }
    return "$subject ${issue.message}"
}

@OptIn(ExperimentalUuidApi::class)
internal fun newBlankEvent(day: LocalDate?, timeZoneId: String) = OpenEvnt(
    id = "urn:uuid:${Uuid.random()}",
    name = emptyMap(),
    occurrences = day?.let { listOf(Occurrence(start = PartialDate.Day(it, timeZoneId))) },
)

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
private fun ColumnScope.FormSubtitle(text: String) {
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

@Composable
private fun EventForm(
    state: AppState,
    title: String,
    initial: OpenEvnt,
    unchanged: OpenEvnt,
    onDiscard: (OpenEvnt) -> Unit,
    offersImport: Boolean = false,
    sharedLink: String? = null,
    subtitle: String? = null,
    onCancel: (() -> Unit)? = null,
    destinationRow: (@Composable () -> Unit)? = null,
    save: suspend (OpenEvnt) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val preferredLanguages = state.preferredLanguages
    val formatting = state.formatting
    var event by rememberSaveable(initial, stateSaver = OpenEvntSaver) { mutableStateOf(initial) }
    var issues by remember { mutableStateOf(emptyList<String>()) }
    var showsNameProblem by remember { mutableStateOf(false) }
    val nameFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { if (event.hasNoName()) nameFocus.requestFocus() }
    var saveError by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }
    var editingDate by rememberSaveable { mutableStateOf<Int?>(null) }
    var editingLink by rememberSaveable { mutableStateOf<Int?>(null) }
    var editingCover by rememberSaveable { mutableStateOf(false) }
    var searchingPlace by rememberSaveable { mutableStateOf(false) }
    var editingPlace by rememberSaveable { mutableStateOf(false) }
    var dismissedLinks by rememberSaveable { mutableStateOf(emptyList<String>()) }
    val nameText = event.name.textToEdit(preferredLanguages)
    val descriptionText = event.description()?.content.textToEdit(preferredLanguages)
    var descriptionEdit by remember { mutableStateOf(TextFieldValue(descriptionText)) }
    val description = descriptionEdit.takeIf { it.text == descriptionText } ?: TextFieldValue(descriptionText, TextRange(descriptionText.length))
    var editingDescription by remember { mutableStateOf(false) }
    fun changeDescription(value: TextFieldValue) {
        descriptionEdit = value
        event = event.withDescription(preferredLanguages, value.text)
    }
    val linkInName = findEventLink(nameText)
    val offeredLink = (linkInName ?: sharedLink).takeIf { offersImport && it !in dismissedLinks }

    fun discardIfChanged() {
        if (event.cleaned() != unchanged.cleaned()) onDiscard(event)
    }

    fun close() {
        if (onCancel != null) return onCancel()
        state.navigator.goBack()
        discardIfChanged()
    }
    SystemBackHandler(enabled = true, onBack = ::close)
    val currentDiscardIfChanged by rememberUpdatedState(::discardIfChanged)
    val depth = LocalStackDepth.current
    DisposableEffect(state.navigator, depth) {
        state.navigator.setClearAction(depth) { currentDiscardIfChanged() }
        onDispose {}
    }

    fun submit() {
        if (saving) return
        val cleaned = event.cleaned()
        val nameMissing = cleaned.hasNoName()
        val found = OpenEvnt.issuesOf(cleaned.toJson())
        showsNameProblem = nameMissing
        issues = found.filterNot { nameMissing && it.path.firstOrNull() == "name" }.map(cleaned::describe)
        if (found.isNotEmpty()) {
            if (nameMissing) nameFocus.requestFocus()
            return
        }
        saving = true
        saveError = null
        scope.launch {
            try {
                save(cleaned)
            } catch (error: Exception) {
                saveError = error.message ?: "Couldn't save the event"
            } finally {
                saving = false
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().imePadding()) {
            ScreenHeader(title, onClose = ::close) {
                HeaderButton("Save", onClick = ::submit, busy = saving)
            }
            subtitle?.let { FormSubtitle(it) }
            val problems = issues + listOfNotNull(saveError)
            AnimatedVisibility(problems.isNotEmpty(), enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
                    FailureRow(problems.joinToString("\n"), onRetry = null, Modifier.widthIn(max = ContentMaxWidth).padding(horizontal = Spacing.md))
                }
            }
            Column(
                Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Column(Modifier.widthIn(max = ContentMaxWidth).fillMaxWidth()) {
                    NameField(
                        event,
                        preferredLanguages,
                        nameFocus,
                        isError = showsNameProblem && event.hasNoName(),
                        onChange = { event = it },
                    )
                    AnimatedContent(
                        offeredLink,
                        transitionSpec = { fadeIn(tween(220, delayMillis = 90)) togetherWith fadeOut(tween(90)) },
                    ) { link ->
                        if (link == null) return@AnimatedContent
                        LinkImportCard(
                            state,
                            link,
                            onAddAsLink = {
                                dismissedLinks += link
                                val name = if (link == linkInName) event.name.withEditedText(preferredLanguages, nameText.replace(link, "").trim()) else event.name
                                event = event.copy(name = name, components = event.components.orEmpty() + Component.Link(url = link))
                            },
                            onDismiss = { dismissedLinks += link },
                        )
                    }
                    destinationRow?.let { row -> ListSection(Modifier.padding(top = Spacing.md)) { row() } }
                    ListSection(Modifier.padding(top = Spacing.md), title = "Time & Place") {
                        StatusRow(event.status, onSelect = { event = event.copy(status = it) })
                        DateRows(
                            event.occurrences.orEmpty(),
                            formatting,
                            deviceTimeZoneId = state.timeZone.id,
                            onEdit = { editingDate = it },
                            onRemove = { event = event.withOccurrence(it, null) },
                            onAdd = {
                                editingDate = event.occurrences.orEmpty().size
                                event = event.withNextOccurrence(PartialDate.Day(state.today, state.timeZone.id))
                            },
                        )
                        PlaceRow(
                            event.venue,
                            state.preferredLanguages,
                            onEdit = { editingPlace = true },
                            onRemove = { event = event.copy(venue = null) },
                            onAdd = { searchingPlace = true },
                        )
                    }
                    ListSection(Modifier.padding(top = Spacing.md), title = "Details") {
                        DescriptionField(
                            description,
                            onChange = ::changeDescription,
                            onEditingChange = { editingDescription = it },
                            onLinkClick = state::openLinkActionsSheet,
                        )
                        CoverImageRow(
                            event.backgroundSplashMedia(),
                            state.preferredLanguages,
                            onEdit = { editingCover = true },
                            onRemove = { event = event.withBackgroundSplashMedia(null) },
                            onAdd = { editingCover = true },
                        )
                    }
                    ListSection(Modifier.padding(top = Spacing.md), title = "Links") {
                        LinkRows(
                            event.links(),
                            state.preferredLanguages,
                            onEdit = { editingLink = it },
                            onRemove = { event = event.withComponent(it, null) },
                            onMove = { from, to -> event = event.copy(components = event.components.orEmpty().withLinkMoved(from, to)) },
                            onAdd = { editingLink = NEW_LINK },
                            onLongPress = state::openLinkActionsSheet,
                        )
                    }
                    Spacer(Modifier.navigationBarsPadding().height(Spacing.xl))
                }
            }
            AnimatedVisibility(editingDescription, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                MarkdownToolbar(description, ::changeDescription)
            }
        }

        val editedOccurrence = editingDate?.let { index -> event.occurrences.orEmpty().getOrNull(index)?.let { IndexedValue(index, it) } }
        BottomSheetFrame(item = editedOccurrence, onDismiss = { editingDate = null }) { (index, occurrence) ->
            key(index) {
                OccurrenceEditor(
                    occurrence = occurrence,
                    formatting = formatting,
                    defaultTimeZoneId = state.timeZone.id,
                    now = state.now,
                    onChange = { event = event.withOccurrence(index, it) },
                    onDelete = {
                        editingDate = null
                        event = event.withOccurrence(index, null)
                    },
                    onDone = { editingDate = null },
                )
            }
        }

        BottomSheet(item = editingLink, onDismiss = { editingLink = null }) { index ->
            val link = event.components?.getOrNull(index) as? Component.Link
            key(index) {
                LinkForm(link, preferredLanguages) { saved ->
                    editingLink = null
                    event = if (link == null) event.copy(components = event.components.orEmpty() + saved) else event.withComponent(index, saved)
                }
            }
        }

        BottomSheet(visible = editingCover, onDismiss = { editingCover = false }) {
            CoverImageForm(event.backgroundSplashMedia(), preferredLanguages) { saved ->
                editingCover = false
                event = event.withBackgroundSplashMedia(saved)
            }
        }

        BottomSheetFrame(item = event.venue.takeIf { editingPlace }, onDismiss = { editingPlace = false }) { venue ->
            PlaceForm(
                venue,
                preferredLanguages,
                onChange = { event = event.copy(venue = it) },
                onSearch = {
                    editingPlace = false
                    searchingPlace = true
                },
                onDone = { editingPlace = false },
            )
        }

        PlaceSearchSheet(
            visible = searchingPlace,
            title = "Place",
            state = state,
            onDismiss = { searchingPlace = false },
            onPick = {
                searchingPlace = false
                event = event.copy(venue = it)
            },
            onNewPlace = { name ->
                searchingPlace = false
                event = event.copy(venue = Venue(name = null.withEditedText(preferredLanguages, name)))
                editingPlace = true
            },
        )
    }
}

private enum class NewEventDestination(val label: String) {
    Device("This Device"),
    Folio("Folio"),
}

private val NewEventDestination.icon get() = if (this == NewEventDestination.Device) AppIcons.Smartphone else AppIcons.WorldUpload

@Composable
private fun DestinationRow(destination: NewEventDestination, onSelect: (NewEventDestination) -> Unit) {
    var open by rememberSaveable { mutableStateOf(false) }
    ListRow("Save To", icon = destination.icon, supporting = destination.label, onClick = { open = true })
    BottomSheet(visible = open, onDismiss = { open = false }) {
        SheetTitle("Save To", Modifier.padding(horizontal = Spacing.md))
        Column {
            for (option in NewEventDestination.entries) {
                ListRow(
                    option.label,
                    icon = option.icon,
                    onClick = {
                        open = false
                        onSelect(option)
                    },
                    trailing = { if (option == destination) Icon(AppIcons.Check, tint = MaterialTheme.colorScheme.primary) },
                )
            }
        }
    }
}

@Composable
private fun StatusRow(status: OpenEvnt.Status?, onSelect: (OpenEvnt.Status?) -> Unit) {
    var open by rememberSaveable { mutableStateOf(false) }
    ListRow(
        "Status",
        icon = status.icon,
        supporting = STATUS_OPTIONS.first { it.first == status }.second,
        color = if (status == OpenEvnt.Status.Cancelled) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
        onClick = { open = true },
    )
    BottomSheet(visible = open, onDismiss = { open = false }) {
        SheetTitle("Status", Modifier.padding(horizontal = Spacing.md))
        Column {
            for ((option, label) in STATUS_OPTIONS) {
                ListRow(
                    label,
                    icon = option.icon,
                    onClick = {
                        open = false
                        onSelect(option)
                    },
                    trailing = { if (option == status) Icon(AppIcons.Check, tint = MaterialTheme.colorScheme.primary) },
                )
            }
        }
    }
}

@Composable
private fun NameField(
    event: OpenEvnt,
    preferredLanguages: List<String>,
    focus: FocusRequester,
    isError: Boolean,
    onChange: (OpenEvnt) -> Unit,
) {
    TextInput(
        value = event.name.textToEdit(preferredLanguages),
        onValueChange = { onChange(event.copy(name = event.name.withEditedText(preferredLanguages, it))) },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.md, vertical = ListRowGap / 2)
            .focusRequester(focus)
            .semantics { contentDescription = "Event Name" },
        placeholder = if (isError) "Give it a name" else event.name.translate(preferredLanguages).ifEmpty { "Event Name" },
        isError = isError,
        singleLine = false,
        allowsLineBreaks = false,
        maxLines = 3,
    )
}

@Composable
private fun DateRows(
    occurrences: List<Occurrence>,
    formatting: EventFormatting,
    deviceTimeZoneId: String,
    onEdit: (Int) -> Unit,
    onRemove: (Int) -> Unit,
    onAdd: () -> Unit,
) {
    occurrences.forEachIndexed { index, occurrence ->
        val text = formatting.occurrenceText(occurrence)
        val timeZoneId = occurrence.start?.timeZoneId?.takeIf { it != deviceTimeZoneId }
        ListRow(
            text.headline,
            icon = AppIcons.Clock.takeIf { index == 0 },
            alignWithIcons = true,
            supporting = listOfNotNull(
                text.supporting,
                timeZoneId,
                "Cancelled".takeIf { occurrence.isCancelled },
            ).joinToString(" · ").ifEmpty { null },
            onClick = { onEdit(index) },
            trailing = { RemoveButton("Remove Date") { onRemove(index) } },
        )
    }
    ListRow("Add Date", icon = if (occurrences.isEmpty()) AppIcons.Clock else AppIcons.Plus, onClick = onAdd)
}

@Composable
private fun PlaceRow(venue: Venue?, preferredLanguages: List<String>, onEdit: () -> Unit, onRemove: () -> Unit, onAdd: () -> Unit) {
    if (venue == null) {
        ListRow("Add Place", icon = AppIcons.AddPlace, onClick = onAdd)
        return
    }
    val name = venue.name.takeIf { it.hasText() }?.translate(preferredLanguages)
    ListRow(
        name ?: venue.address ?: "Unnamed Place",
        icon = AppIcons.MapPin,
        supporting = venue.address.takeIf { name != null },
        onClick = onEdit,
        trailing = { RemoveButton("Remove Place", onRemove) },
    )
}

@Composable
private fun LinkRows(
    links: List<IndexedValue<Component>>,
    preferredLanguages: List<String>,
    onEdit: (Int) -> Unit,
    onRemove: (Int) -> Unit,
    onMove: (fromLinkIndex: Int, toLinkIndex: Int) -> Unit,
    onAdd: () -> Unit,
    onLongPress: (url: String) -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    ReorderableColumn(
        links,
        onSettle = onMove,
        modifier = Modifier.zIndex(1f),
        onMove = { haptics.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick) },
    ) { _, (index, component), dragging ->
        key(index, component) {
            ReorderableItem {
                val link = component as Component.Link
                val text = link.linkText(preferredLanguages)
                val elevation by animateDpAsState(if (dragging) DRAGGED_LINK_ELEVATION else 0.dp)
                ListRow(
                    text.headline,
                    icon = AppIcons.Link,
                    supporting = text.supporting,
                    onClick = { onEdit(index) },
                    onLongClick = { onLongPress(link.url) },
                    elevation = elevation,
                    trailing = {
                        Row {
                            RemoveButton("Remove Link") { onRemove(index) }
                            if (links.size > 1) {
                                IconButton(
                                    AppIcons.DragHandle,
                                    "Reorder Link",
                                    onClick = {},
                                    modifier = Modifier.draggableHandle(
                                        onDragStarted = { haptics.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate) },
                                        onDragStopped = { haptics.performHapticFeedback(HapticFeedbackType.GestureEnd) },
                                    ),
                                )
                            }
                        }
                    },
                )
            }
        }
    }
    ListRow("Add Link", icon = AppIcons.AddLink, onClick = onAdd)
}

private val DRAGGED_LINK_ELEVATION = 8.dp

@Composable
private fun DescriptionField(
    value: TextFieldValue,
    onChange: (TextFieldValue) -> Unit,
    onEditingChange: (Boolean) -> Unit,
    onLinkClick: (url: String) -> Unit,
) {
    val text = value.text
    var previewing by rememberSaveable { mutableStateOf(false) }
    val showingPreview = previewing && text.isNotBlank()
    val style = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface)
    val interactions = remember { MutableInteractionSource() }
    Column(Modifier.fillMaxWidth().dividerAboveInListSection()) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = ListRowPadding, vertical = (ListRowHeight - IconSize.md) / 2),
            horizontalArrangement = Arrangement.spacedBy(ListRowIconGap),
        ) {
            Icon(AppIcons.Notes, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            AnimatedContent(
                showingPreview,
                Modifier.weight(1f),
                transitionSpec = { fadeIn(tween(220, delayMillis = 90)) togetherWith fadeOut(tween(90)) },
            ) { preview ->
                if (preview) {
                    val blocks = remember(text) { Markdown.parse(text) }
                    MarkdownView(blocks, onLinkClick, color = MaterialTheme.colorScheme.onSurface)
                } else {
                    DisposableEffect(Unit) { onDispose { onEditingChange(false) } }
                    BasicTextField(
                        value = value,
                        onValueChange = onChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged { onEditingChange(it.isFocused) }
                            .semantics { contentDescription = "Description" },
                        textStyle = style,
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        visualTransformation = rememberMarkdownHighlighting(),
                        interactionSource = interactions,
                        decorationBox = { inner ->
                            Box {
                                if (text.isEmpty()) Text("Add Description", color = MaterialTheme.colorScheme.onSurfaceVariant, style = style)
                                inner()
                            }
                        },
                    )
                }
            }
        }
        AnimatedVisibility(text.isNotBlank(), enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = Spacing.xs), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = { previewing = !showingPreview }) { Text(if (showingPreview) "Edit" else "Preview") }
            }
        }
    }
}

internal const val WEB_ADDRESS_PROBLEM = "Needs a web address, like example.com"

internal fun normalizedLinkUrl(text: String): String? {
    val trimmed = text.trim()
    if (trimmed.isEmpty() || trimmed.any(Char::isWhitespace)) return null
    val url = if ("://" in trimmed) trimmed else "https://$trimmed"
    return url.takeIf { parseUrl(it)?.host?.contains('.') == true }
}

@Composable
private fun LinkForm(link: Component.Link?, preferredLanguages: List<String>, onSave: (Component.Link) -> Unit) {
    var address by rememberSaveable { mutableStateOf(link?.url.orEmpty()) }
    var name by rememberSaveable { mutableStateOf(link?.name.textToEdit(preferredLanguages)) }
    val url = normalizedLinkUrl(address)
    val focus = remember { FocusRequester() }
    fun submit() {
        normalizedLinkUrl(address)?.let { onSave(Component.Link(url = it, name = link?.name.withEditedText(preferredLanguages, name).nullIfBlank(), disabled = link?.disabled)) }
    }
    Column(Modifier.padding(horizontal = Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
        SheetTitle(if (link == null) "Add Link" else "Edit Link")
        TextInput(
            value = address,
            onValueChange = { address = it },
            modifier = Modifier.fillMaxWidth().focusRequester(focus),
            label = "Address",
            problem = WEB_ADDRESS_PROBLEM.takeIf { address.isNotBlank() && url == null },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Next),
        )
        TextInput(
            value = name,
            onValueChange = { name = it },
            modifier = Modifier.fillMaxWidth(),
            label = "Name",
            placeholder = url?.let { parseUrl(it)?.host?.removePrefix("www.") } ?: "Optional",
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { submit() }),
        )
        if (url != null) Button(if (link == null) "Add" else "Save", onClick = ::submit, Modifier.align(Alignment.End), primary = true)
    }
    LaunchedEffect(Unit) { if (link == null) focus.requestFocus() }
}

@Composable
private fun CountryField(code: String?, language: String, onPick: () -> Unit, onClear: () -> Unit) {
    val shown = remember(code, language) { code?.let { "${Countries.flagOf(it)}  ${countryName(it, language)}" } }
    PickerBox("Country", shown ?: "Add Country", onClick = onPick, placeholder = shown == null, onClear = onClear.takeIf { code != null })
}

@Composable
private fun BottomSheetScope.PlaceForm(venue: Venue, preferredLanguages: List<String>, onChange: (Venue) -> Unit, onSearch: () -> Unit, onDone: () -> Unit) {
    var pickingCountry by rememberSaveable { mutableStateOf(false) }
    if (pickingCountry) {
        CountryPickerBody("Country", venue.countryCode, preferredLanguages.first(), onBack = { pickingCountry = false }) { picked ->
            pickingCountry = false
            onChange(venue.copy(countryCode = picked))
        }
        return
    }
    ScrollingBody { PlaceFields(venue, preferredLanguages, onChange, onSearch, onDone, onPickCountry = { pickingCountry = true }) }
}

@Composable
private fun PlaceFields(venue: Venue, preferredLanguages: List<String>, onChange: (Venue) -> Unit, onSearch: () -> Unit, onDone: () -> Unit, onPickCountry: () -> Unit) {
    Column(Modifier.padding(horizontal = Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
        SheetTitle("Place")
        TextInput(
            value = venue.name.textToEdit(preferredLanguages),
            onValueChange = { onChange(venue.copy(name = venue.name.withEditedText(preferredLanguages, it))) },
            modifier = Modifier.fillMaxWidth(),
            label = "Name",
        )
        TextInput(
            value = venue.address.orEmpty(),
            onValueChange = { onChange(venue.copy(address = it)) },
            modifier = Modifier.fillMaxWidth(),
            label = "Address",
            singleLine = false,
            minLines = 2,
        )
        CountryField(venue.countryCode, preferredLanguages.first(), onPick = onPickCountry, onClear = { onChange(venue.copy(countryCode = null)) })
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onSearch) { Text("Search Places") }
            Spacer(Modifier.weight(1f))
            Button("Done", onClick = onDone, primary = true)
        }
    }
}
