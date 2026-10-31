package blue.deniz.vantage.app

import blue.deniz.vantage.app.ui.removal

import blue.deniz.vantage.app.ui.EventRemoval

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontFamily
import blue.deniz.vantage.app.screens.EventTagPickerBody
import blue.deniz.vantage.app.ui.AppIcons
import blue.deniz.vantage.app.ui.BottomSheet
import blue.deniz.vantage.app.ui.BottomSheetFrame
import blue.deniz.vantage.app.ui.Button
import blue.deniz.vantage.app.ui.EventTitle
import blue.deniz.vantage.app.ui.FontSize
import blue.deniz.vantage.app.ui.MenuItem
import blue.deniz.vantage.app.ui.Spacing
import blue.deniz.vantage.app.ui.Text
import blue.deniz.vantage.app.ui.markdownSummary
import blue.deniz.vantage.core.EventFormat
import blue.deniz.vantage.core.EventSource
import blue.deniz.vantage.core.EventsLink
import blue.deniz.vantage.core.ResolvedEvent
import blue.deniz.vantage.core.canUploadToFolio
import blue.deniz.vantage.core.isEditableHere
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

private const val COPIED_LABEL_MILLIS = 2000L

fun actionsSheetEventKey(id: String) = "actions sheet event $id"

@Composable
fun EventActionsSheet(state: AppState) {
    val id = state.actionsSheetEventId
    val stored = id?.let { collectAcrossSession(actionsSheetEventKey(it), null) { state.store.observe(it).filterNotNull() } }
    val scope = rememberCoroutineScope()
    var choosingTags by remember(id) { mutableStateOf(false) }
    LaunchedEffect(state.actionsSheetOpen) { if (state.actionsSheetOpen) choosingTags = false }
    val current = stored ?: state.actionsSheetEvent
    BottomSheetFrame(visible = state.actionsSheetOpen, onDismiss = state::closeActionsSheet) {
        when {
            id != null && current != null -> key(id) {
                if (choosingTags) {
                    EventTagPickerBody(state, setOf(id), onBack = { choosingTags = false })
                } else {
                    ScrollingBody { EventActions(state, id, current, scope, onChooseTags = { choosingTags = true }) }
                }
            }
            current != null -> key(current) { ScrollingBody { EventActions(state, null, current, scope, onChooseTags = {}) } }
        }
    }
}

@Composable
private fun EventActions(state: AppState, id: String?, event: ResolvedEvent, scope: CoroutineScope, onChooseTags: () -> Unit) {
    val share = rememberShareText()
    val formatting = state.formatting
    var refreshing by remember { mutableStateOf(false) }
    var adding by remember { mutableStateOf(false) }
    var uploading by remember { mutableStateOf(false) }
    var uploadProblem by remember { mutableStateOf<String?>(null) }
    var confirmingUpload by remember { mutableStateOf(false) }
    var viewedData by remember { mutableStateOf<String?>(null) }
    val source = event.source
    val shareLink = EventsLink.shareLinkTo(source)

    SystemBackHandler(enabled = viewedData != null) { viewedData = null }
    EventTitle(event, state.preferredLanguages, Modifier.padding(horizontal = Spacing.md))
    viewedData?.let { text ->
        DataView(text)
        return
    }
    SystemBackHandler(enabled = confirmingUpload && !uploading) { confirmingUpload = false }
    if (id != null && confirmingUpload) {
        FolioUploadConfirmation(
            uploading = uploading,
            problem = uploadProblem,
            onCancel = {
                confirmingUpload = false
                uploadProblem = null
            },
            onUpload = {
                uploading = true
                uploadProblem = null
                scope.launch {
                    try {
                        val uploaded = state.store.uploadToFolio(id)
                        confirmingUpload = false
                        EventsLink.shareLinkTo(uploaded)?.let(share)
                    } catch (error: CancellationException) {
                        throw error
                    } catch (error: Exception) {
                        uploadProblem = "Couldn't upload to Folio: ${error.message ?: "no answer"}"
                    } finally {
                        uploading = false
                    }
                }
            },
        )
        return
    }
    Column {
        if (id == null && source != EventSource.Unknown && event.format != EventFormat.Unknown) {
            ActionButton("Add to My Events", AppIcons.Plus, busy = adding) {
                adding = true
                scope.launch {
                    try {
                        val addedId = state.store.add(event)
                        state.closeActionsSheet()
                        if (state.navigator.current is Route.EventFromLink) state.navigator.replace(Route.EventDetails(addedId))
                    } finally {
                        adding = false
                    }
                }
            }
        }
        if (id != null && source is EventSource.Remote) {
            ActionButton("Refresh", AppIcons.Refresh, busy = refreshing) {
                refreshing = true
                scope.launch {
                    try {
                        state.store.refresh(id)
                    } finally {
                        refreshing = false
                    }
                }
            }
        }
        if (event.error != null) {
            event.data?.let { data ->
                ActionButton("View: Resolved Data", AppIcons.Code) { viewedData = data.toJson().toString() }
            }
            event.raw?.let { raw -> ActionButton("View: Raw Data", AppIcons.Code) { viewedData = raw } }
            if (event.format != EventFormat.OpenEvnt) event.raw?.let { raw -> CopyMenuItem("Copy Raw Data") { raw } }
            when (source) {
                is EventSource.Http -> CopyMenuItem("Copy Data URL") { source.url }
                is EventSource.At -> CopyMenuItem("Copy at-uri") { source.uri.toString() }
                else -> {}
            }
        }
        if (id != null) ActionButton("Tags", AppIcons.Tag, onClick = onChooseTags)
        if (shareLink != null) ActionButton("Share", AppIcons.Share) { share(shareLink) }
        if (id != null && source.canUploadToFolio) {
            ActionButton("Upload to Folio", AppIcons.WorldUpload) {
                confirmingUpload = true
            }
        }
        event.data?.let { data ->
            CopyMenuItem("Copy Summary") { data.markdownSummary(formatting, state.preferredLanguages) }
        }
        if (source is EventSource.Folio) {
            source.editLink?.let { link -> ActionButton("Share Editing Link", AppIcons.UserEdit) { share(link) } }
        }
        if (id != null && source.isEditableHere) {
            ActionButton("Edit", AppIcons.Pencil) {
                state.closeActionsSheet()
                state.navigator.open(Route.EditEvent(id))
            }
        }
        if (id != null) {
            val removal = source.removal
            ActionButton(removal.action, removal.icon, danger = removal == EventRemoval.Delete) {
                state.closeActionsSheet()
                if ((state.navigator.current as? Route.EventDetails)?.id == id) state.navigator.goBack()
                state.removeEvent(id)
            }
        }
    }
}

@Composable
private fun FolioUploadConfirmation(uploading: Boolean, problem: String?, onCancel: () -> Unit, onUpload: () -> Unit) {
    Column(Modifier.padding(horizontal = Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Text("Upload to Folio?", style = MaterialTheme.typography.titleMedium)
        Text("Anyone with the link can see it.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
        problem?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium) }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.sm, Alignment.End)) {
            Button("Cancel", onClick = onCancel, enabled = !uploading)
            Button(if (problem == null) "Upload" else "Try Again", onClick = onUpload, primary = true, busy = uploading)
        }
    }
}

@Composable
private fun DataView(text: String) {
    val shown = remember(text) { prettyPrintJsonOrNull(text) ?: text }
    CopyMenuItem("Copy") { shown }
    SelectionContainer(Modifier.padding(horizontal = Spacing.md)) {
        Text(shown, fontSize = FontSize.xs, fontFamily = FontFamily.Monospace)
    }
}

@OptIn(ExperimentalSerializationApi::class)
private val prettyJson = Json {
    prettyPrint = true
    prettyPrintIndent = "  "
}

private fun prettyPrintJsonOrNull(text: String) = try {
    prettyJson.encodeToString(JsonElement.serializer(), Json.parseToJsonElement(text))
} catch (_: IllegalArgumentException) {
    null
}

@Composable
fun LinkActionsSheet(state: AppState) {
    val url = state.linkActionsSheetUrl
    val uriHandler = LocalUriHandler.current
    val share = rememberShareText()
    BottomSheet(visible = state.linkActionsSheetOpen, onDismiss = state::closeLinkActionsSheet) {
        if (url == null) return@BottomSheet
        Text(
            url,
            Modifier.padding(horizontal = Spacing.md),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 3,
            style = MaterialTheme.typography.bodyMedium,
        )
        Column {
            ActionButton("Open", AppIcons.ExternalLink) {
                state.closeLinkActionsSheet()
                uriHandler.openUri(url)
            }
            key(url) { CopyMenuItem("Copy", text = { url }) }
            ActionButton("Share", AppIcons.Share) {
                state.closeLinkActionsSheet()
                share(url)
            }
        }
    }
}

@Composable
fun CopyMenuItem(label: String, supportingText: String? = null, text: () -> String) {
    val copy = rememberCopyText()
    var copied by remember { mutableStateOf(false) }
    LaunchedEffect(copied) {
        if (copied) {
            delay(COPIED_LABEL_MILLIS)
            copied = false
        }
    }
    ActionButton(if (copied) "Copied!" else label, if (copied) AppIcons.Check else AppIcons.Copy, supportingText = supportingText) {
        copy(text())
        copied = true
    }
}

@Composable
private fun ActionButton(
    label: String,
    icon: ImageVector,
    danger: Boolean = false,
    enabled: Boolean = true,
    busy: Boolean = false,
    supportingText: String? = null,
    onClick: () -> Unit,
) {
    MenuItem(label, onClick, leadingIcon = icon, supportingText = supportingText, busy = busy, danger = danger, enabled = enabled)
}
