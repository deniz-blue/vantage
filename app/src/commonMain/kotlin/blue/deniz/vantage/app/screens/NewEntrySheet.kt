package blue.deniz.vantage.app.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import blue.deniz.vantage.app.AppState
import blue.deniz.vantage.app.Route
import blue.deniz.vantage.app.ui.AppIcons
import blue.deniz.vantage.app.ui.BottomSheet
import blue.deniz.vantage.app.ui.Button
import blue.deniz.vantage.app.ui.MenuItem
import blue.deniz.vantage.app.ui.Spacing
import blue.deniz.vantage.app.SystemBackHandler
import blue.deniz.vantage.app.ui.Text
import blue.deniz.vantage.app.ui.SheetTitle
import blue.deniz.vantage.app.ui.TextInput
import blue.deniz.vantage.core.EventTextGuess
import blue.deniz.vantage.core.guessEventFromText
import directory.evnt.Component
import directory.evnt.OpenEvnt
import directory.evnt.hasText
import directory.evnt.withEditedText
import kotlinx.datetime.LocalDate

@Composable
fun NewEntrySheet(state: AppState) {
    var inferringFromText by remember { mutableStateOf(false) }
    LaunchedEffect(state.newEntrySheetOpen) { if (state.newEntrySheetOpen) inferringFromText = false }
    val day = state.newEntryDay

    fun openMarkSheet(kind: MarkKind) {
        state.closeNewEntrySheet()
        state.openMarkSheet(MarkSheetTarget(kind, id = null, day = day))
    }

    BottomSheet(visible = state.newEntrySheetOpen, onDismiss = state::closeNewEntrySheet) {
        if (inferringFromText) {
            SystemBackHandler(enabled = state.newEntrySheetOpen) { inferringFromText = false }
            InferFromTextForm { text ->
                val draft = newBlankEvent(day, state.timeZone.id).filledFromText(text, state.today, state.timeZone.id, state.preferredLanguages)
                state.closeNewEntrySheet()
                state.navigator.open(Route.CreateEvent(draft = draft))
            }
            return@BottomSheet
        }
        SheetTitle("New", Modifier.padding(horizontal = Spacing.md))
        Column {
            MenuItem("Event", onClick = {
                state.closeNewEntrySheet()
                state.navigator.open(Route.CreateEvent(day = day))
            }, leadingIcon = AppIcons.Calendar)
            MenuItem("Event From Text", onClick = { inferringFromText = true }, leadingIcon = AppIcons.Paste)
            MenuItem("Marker", onClick = { openMarkSheet(MarkKind.Marker) }, leadingIcon = AppIcons.Label)
            MenuItem("Period", onClick = { openMarkSheet(MarkKind.Period) }, leadingIcon = AppIcons.DateRange)
        }
    }
}

fun OpenEvnt.filledFromText(text: String, today: LocalDate, timeZoneId: String, preferredLanguages: List<String>): OpenEvnt =
    filledFrom(guessEventFromText(text, today, timeZoneId, preferredLanguages.first()), preferredLanguages)

private fun OpenEvnt.filledFrom(guess: EventTextGuess, preferredLanguages: List<String>): OpenEvnt {
    val named = guess.name?.takeUnless { name.hasText() }?.let { copy(name = name.withEditedText(preferredLanguages, it)) } ?: this
    val dated = if (guess.occurrences.isEmpty()) named else named.copy(occurrences = guess.occurrences)
    val placed = guess.venue?.let { dated.copy(venue = it) } ?: dated
    val linked = if (guess.links.isEmpty()) placed else placed.copy(components = placed.components.orEmpty() + guess.links.map { Component.Link(url = it) })
    return guess.description?.let { linked.withDescription(preferredLanguages, it) } ?: linked
}

@Composable
private fun InferFromTextForm(onFill: (text: String) -> Unit) {
    var text by rememberSaveable { mutableStateOf("") }
    val focus = remember { FocusRequester() }
    Column(Modifier.padding(horizontal = Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
        SheetTitle("Event From Text")
        TextInput(
            value = text,
            onValueChange = { text = it },
            modifier = Modifier.fillMaxWidth().focusRequester(focus),
            label = "Text",
            singleLine = false,
            minLines = 6,
            maxLines = 14,
        )
        if (text.isNotBlank()) Button("Fill In", onClick = { onFill(text) }, Modifier.align(Alignment.End), primary = true)
    }
    LaunchedEffect(Unit) { focus.requestFocus() }
}
