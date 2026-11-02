package blue.deniz.vantage.app.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import blue.deniz.vantage.app.ui.AppIcons
import blue.deniz.vantage.app.ui.Button
import blue.deniz.vantage.app.ui.ListRow
import blue.deniz.vantage.app.ui.OpenEvntImage
import blue.deniz.vantage.app.ui.Radius
import blue.deniz.vantage.app.ui.RemoveButton
import blue.deniz.vantage.app.ui.Spacing
import blue.deniz.vantage.app.ui.Text
import blue.deniz.vantage.app.ui.SheetTitle
import blue.deniz.vantage.app.ui.TextInput
import blue.deniz.vantage.core.withImageAddress
import directory.evnt.Media
import directory.evnt.textToEdit
import directory.evnt.withEditedText
import io.ktor.http.parseUrl
import kotlinx.coroutines.delay

private const val COVER_ASPECT_RATIO = 16f / 9f
private const val PREVIEW_DELAY_MILLIS = 400L
private val THUMBNAIL_WIDTH = 56.dp
private val THUMBNAIL_HEIGHT = 32.dp

@Composable
internal fun CoverImageRow(media: Media?, preferredLanguages: List<String>, onEdit: () -> Unit, onRemove: () -> Unit, onAdd: () -> Unit) {
    if (media == null) {
        ListRow("Add Cover Image", icon = AppIcons.AddImage, onClick = onAdd)
        return
    }
    val address = media.sources.firstOrNull()?.url
    ListRow(
        "Cover Image",
        icon = AppIcons.Image,
        supporting = address?.let { parseUrl(it)?.host?.removePrefix("www.") ?: it },
        onClick = onEdit,
        trailing = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OpenEvntImage(
                    media,
                    preferredLanguages,
                    Modifier
                        .size(THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT)
                        .clip(RoundedCornerShape(Radius.sm))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                )
                RemoveButton("Remove Cover Image", onRemove)
            }
        },
    )
}

@Composable
internal fun CoverImageForm(media: Media?, preferredLanguages: List<String>, onSave: (Media) -> Unit) {
    var address by rememberSaveable { mutableStateOf(media?.sources?.firstOrNull()?.url.orEmpty()) }
    var alt by rememberSaveable { mutableStateOf(media?.alt.textToEdit(preferredLanguages)) }
    val url = normalizedLinkUrl(address)
    var previewUrl by remember { mutableStateOf(url) }
    val focus = remember { FocusRequester() }
    fun edited(imageUrl: String) = media.withImageAddress(imageUrl).copy(alt = media?.alt.withEditedText(preferredLanguages, alt).nullIfBlank())
    fun submit() {
        normalizedLinkUrl(address)?.let { onSave(edited(it)) }
    }
    LaunchedEffect(url) {
        if (url != null) delay(PREVIEW_DELAY_MILLIS)
        previewUrl = url
    }
    Column(Modifier.padding(horizontal = Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
        SheetTitle(if (media == null) "Add Cover Image" else "Cover Image")
        TextInput(
            value = address,
            onValueChange = { address = it },
            modifier = Modifier.fillMaxWidth().focusRequester(focus),
            label = "Image Address",
            problem = WEB_ADDRESS_PROBLEM.takeIf { address.isNotBlank() && url == null },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Next),
        )
        AnimatedVisibility(previewUrl != null, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
            val shown = previewUrl ?: return@AnimatedVisibility
            OpenEvntImage(
                remember(shown) { edited(shown) },
                preferredLanguages,
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(COVER_ASPECT_RATIO)
                    .clip(RoundedCornerShape(Radius.md))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            )
        }
        TextInput(
            value = alt,
            onValueChange = { alt = it },
            modifier = Modifier.fillMaxWidth(),
            label = "Alt Text",
            singleLine = false,
            allowsLineBreaks = false,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { submit() }),
        )
        if (url != null) Button(if (media == null) "Add" else "Save", onClick = ::submit, Modifier.align(Alignment.End), primary = true)
    }
    LaunchedEffect(Unit) { if (media == null) focus.requestFocus() }
}
