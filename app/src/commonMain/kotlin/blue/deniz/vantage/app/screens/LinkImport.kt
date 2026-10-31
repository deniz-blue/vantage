package blue.deniz.vantage.app.screens

import blue.deniz.vantage.app.ui.Spinner
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import blue.deniz.vantage.app.AppState
import blue.deniz.vantage.app.Route
import blue.deniz.vantage.app.ui.AppIcons
import blue.deniz.vantage.app.ui.Button
import blue.deniz.vantage.app.ui.Card
import blue.deniz.vantage.app.ui.EventCard
import blue.deniz.vantage.app.ui.Icon
import blue.deniz.vantage.app.ui.IconButton
import blue.deniz.vantage.app.ui.IconButtonInset
import blue.deniz.vantage.app.ui.IconSize
import blue.deniz.vantage.app.ui.Spacing
import blue.deniz.vantage.app.ui.Text
import blue.deniz.vantage.core.ResolvedEvent
import blue.deniz.vantage.storage.LinkPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val LINK_READ_DELAY_MILLIS = 400L

@Composable
fun LinkImportCard(state: AppState, link: String, onAddAsLink: () -> Unit, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    val preview by produceState<LinkPreview?>(null, link) {
        value = null
        delay(LINK_READ_DELAY_MILLIS)
        value = when (val loaded = loadCatching { state.store.previewLink(link) }) {
            is LoadState.Ready -> loaded.value
            is LoadState.Failed -> LinkPreview.NotAnEvent(loaded.message)
            LoadState.Loading -> null
        }
    }
    Card(
        modifier.fillMaxWidth().padding(horizontal = Spacing.md, vertical = Spacing.xs),
        background = MaterialTheme.colorScheme.surfaceContainer,
        contentPadding = PaddingValues(start = Spacing.md, end = Spacing.md, top = Spacing.xs, bottom = Spacing.md),
    ) {
        AnimatedContent(
            preview,
            contentKey = { it?.let { shown -> shown::class } },
            transitionSpec = { fadeIn(tween(220, delayMillis = 90)) togetherWith fadeOut(tween(90)) },
        ) { shown ->
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                when (shown) {
                    null -> LinkImportHeader("Event Link", link, onDismiss) { Spinner(IconSize.md, MaterialTheme.colorScheme.onSurfaceVariant) }
                    is LinkPreview.New -> {
                        LinkImportHeader("Event Link", link, onDismiss) { Icon(AppIcons.WorldDownload, tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                        PreviewedEvent(state, shown.event)
                        ImportButton(state, shown.event, "Import This Event", primary = true, modifier = Modifier.fillMaxWidth())
                    }
                    is LinkPreview.AlreadyStored -> {
                        LinkImportHeader("Already in My Events", link, onDismiss) { Icon(AppIcons.Check, tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                        PreviewedEvent(state, shown.event, onClick = { state.navigator.replace(Route.EventDetails(shown.id)) })
                    }
                    is LinkPreview.Broken -> {
                        LinkImportHeader("Couldn't Read the Link", shown.message, onDismiss) {
                            Icon(AppIcons.AlertTriangle, tint = MaterialTheme.colorScheme.error)
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                            Button("Add as Link", onClick = onAddAsLink, modifier = Modifier.weight(1f))
                            ImportButton(state, shown.event, "Add Anyway", primary = false, modifier = Modifier.weight(1f))
                        }
                    }
                    is LinkPreview.NotAnEvent -> {
                        LinkImportHeader("No Event at This Link", shown.message, onDismiss) {
                            Icon(AppIcons.AlertTriangle, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Button("Add as Link", onClick = onAddAsLink, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        }
    }
}

@Composable
private fun LinkImportHeader(
    title: String,
    supporting: String,
    onDismiss: () -> Unit,
    leading: @Composable () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
        leading()
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, maxLines = 1)
            Text(supporting, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall, maxLines = 2)
        }
        IconButton(AppIcons.X, "Dismiss", onClick = onDismiss, Modifier.offset(x = IconButtonInset))
    }
}

@Composable
private fun PreviewedEvent(state: AppState, event: ResolvedEvent, onClick: (() -> Unit)? = null) {
    val formatting = state.formatting
    EventCard(event, formatting, state.preferredLanguages, onClick = onClick)
}

@Composable
private fun ImportButton(state: AppState, event: ResolvedEvent, label: String, primary: Boolean, modifier: Modifier) {
    val scope = rememberCoroutineScope()
    var importing by remember(event) { mutableStateOf(false) }
    Button(
        label,
        busy = importing,
        onClick = {
            if (importing) return@Button
            importing = true
            scope.launch {
                try {
                    state.navigator.replace(Route.EventDetails(state.store.add(event)))
                } finally {
                    importing = false
                }
            }
        },
        modifier = modifier,
        primary = primary,
    )
}
