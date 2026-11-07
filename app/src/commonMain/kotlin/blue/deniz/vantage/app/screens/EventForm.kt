package blue.deniz.vantage.app.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import blue.deniz.vantage.app.AppState
import blue.deniz.vantage.app.LocalStackDepth
import blue.deniz.vantage.app.OpenEvntSaver
import blue.deniz.vantage.app.SystemBackHandler
import blue.deniz.vantage.app.ui.FailureRow
import blue.deniz.vantage.app.ui.BottomSheet
import blue.deniz.vantage.app.ui.BottomSheetFrame
import blue.deniz.vantage.app.ui.MarkdownToolbar
import blue.deniz.vantage.app.ui.HeaderButton
import blue.deniz.vantage.app.ui.ListSection
import blue.deniz.vantage.app.ui.ScreenHeader
import blue.deniz.vantage.app.ui.Spacing
import blue.deniz.vantage.core.backgroundSplashMedia
import blue.deniz.vantage.core.findEventLink
import blue.deniz.vantage.core.withBackgroundSplashMedia
import blue.deniz.vantage.core.withLinkMoved
import directory.evnt.Component
import directory.evnt.Occurrence
import directory.evnt.OpenEvnt
import directory.evnt.PartialDate
import directory.evnt.Venue
import directory.evnt.textToEdit
import directory.evnt.withEditedText
import kotlinx.coroutines.launch
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.plus
import blue.deniz.vantage.app.ui.ContentMaxWidth

private const val NEW_LINK = -1

private fun OpenEvnt.description() = components.orEmpty().filterIsInstance<Component.MarkdownText>().firstOrNull()

private fun OpenEvnt.withComponent(index: Int, component: Component?): OpenEvnt =
    copy(components = components.orEmpty().toMutableList().apply { if (component == null) removeAt(index) else set(index, component) })

private fun PartialDate.nextDay(): PartialDate = when (this) {
    is PartialDate.Day -> PartialDate.Day(toLocalDate().plus(DatePeriod(days = 1)), timeZoneId)
    is PartialDate.Time -> PartialDate.Day(toLocalDate().plus(DatePeriod(days = 1)), timeZoneId).toTime(hour, minute)
    else -> this
}

private fun OpenEvnt.withNextOccurrence(today: PartialDate): OpenEvnt {
    val start = occurrences?.lastOrNull()?.start?.nextDay() ?: today
    return copy(occurrences = occurrences.orEmpty() + Occurrence(start = start))
}

private fun OpenEvnt.hasNoName() = name.values.all { it.isBlank() }

@Composable
internal fun EventForm(
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
