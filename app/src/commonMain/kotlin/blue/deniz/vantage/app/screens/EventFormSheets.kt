package blue.deniz.vantage.app.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import blue.deniz.vantage.app.countryName
import blue.deniz.vantage.core.Countries
import blue.deniz.vantage.app.ui.BottomSheetScope
import blue.deniz.vantage.app.ui.Button
import blue.deniz.vantage.app.ui.Spacing
import blue.deniz.vantage.app.ui.Text
import blue.deniz.vantage.app.ui.SheetTitle
import blue.deniz.vantage.app.ui.TextInput
import directory.evnt.Component
import directory.evnt.Venue
import directory.evnt.textToEdit
import directory.evnt.withEditedText
import io.ktor.http.parseUrl

internal const val WEB_ADDRESS_PROBLEM = "Needs a web address, like example.com"

internal fun normalizedLinkUrl(text: String): String? {
    val trimmed = text.trim()
    if (trimmed.isEmpty() || trimmed.any(Char::isWhitespace)) return null
    val url = if ("://" in trimmed) trimmed else "https://$trimmed"
    return url.takeIf { parseUrl(it)?.host?.contains('.') == true }
}

@Composable
internal fun LinkForm(link: Component.Link?, preferredLanguages: List<String>, onSave: (Component.Link) -> Unit) {
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
        Button(if (link == null) "Add" else "Save", onClick = ::submit, Modifier.align(Alignment.End), primary = true, enabled = url != null)
    }
    LaunchedEffect(Unit) { if (link == null) focus.requestFocus() }
}

@Composable
private fun CountryField(code: String?, language: String, onPick: () -> Unit, onClear: () -> Unit) {
    val shown = remember(code, language) { code?.let { "${Countries.flagOf(it)}  ${countryName(it, language)}" } }
    PickerBox("Country", shown ?: "Add Country", onClick = onPick, placeholder = shown == null, onClear = onClear.takeIf { code != null })
}

@Composable
internal fun BottomSheetScope.PlaceForm(venue: Venue, preferredLanguages: List<String>, onChange: (Venue) -> Unit, onSearch: () -> Unit, onDone: () -> Unit) {
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
