package blue.deniz.vantage.app.screens

import blue.deniz.vantage.app.ui.removal

import blue.deniz.vantage.app.ui.floatingContainer
import blue.deniz.vantage.app.ui.floatingBorder
import blue.deniz.vantage.app.ui.floatingSurface
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import blue.deniz.vantage.app.collectAcrossSession
import blue.deniz.vantage.app.AppState
import blue.deniz.vantage.app.SystemBackHandler
import blue.deniz.vantage.app.rememberCopyText
import blue.deniz.vantage.app.rememberShareText
import blue.deniz.vantage.app.ui.AppIcons
import blue.deniz.vantage.app.ui.BottomSheetFrame
import blue.deniz.vantage.app.ui.ControlHeight
import blue.deniz.vantage.app.ui.IconButton
import blue.deniz.vantage.app.ui.Radius
import blue.deniz.vantage.app.ui.Spacing
import blue.deniz.vantage.app.ui.Text
import blue.deniz.vantage.app.ui.markdownSummary
import blue.deniz.vantage.core.EventsLink
import blue.deniz.vantage.storage.EventListQuery
import kotlinx.coroutines.delay

private const val COPIED_LABEL_MILLIS = 2000L

@Composable
fun EventSelectionBar(state: AppState, matchingIds: List<String>?, order: EventListQuery.Order) {
    val selected = state.selectedEventIds
    val events = collectAcrossSession("selected events", selected to order) { state.store.observeList(EventListQuery(ids = selected, order = order)) }.orEmpty().filter { it.id in selected }
    val formatting = state.formatting
    val share = rememberShareText()
    var choosingTags by remember { mutableStateOf(false) }
    val shareLinks = events.mapNotNull { EventsLink.shareLinkTo(it.source) }
    val summaries = events.mapNotNull { it.data?.markdownSummary(formatting, state.preferredLanguages) }
    val hiddenCount = matchingIds?.let { ids -> selected.count { it !in ids } } ?: 0
    val unselectedIds = matchingIds.orEmpty().filter { it !in selected }

    SelectionBar(
        selectedCount = selected.size,
        onClear = state::clearEventSelection,
        onSelectAll = if (unselectedIds.isNotEmpty()) ({ state.selectEvents(unselectedIds) }) else null,
        hiddenCount = hiddenCount,
    ) {
        IconButton(AppIcons.Tag, "Tags", onClick = { choosingTags = true })
        if (shareLinks.isNotEmpty()) IconButton(AppIcons.Share, "Share", onClick = { share(shareLinks.joinToString("\n")) })
        if (summaries.isNotEmpty()) CopySummariesButton(summaries)
        selected.singleOrNull()?.let { id ->
            IconButton(AppIcons.DotsVertical, "More", onClick = { events.find { it.id == id }?.let(state::openActionsSheet) ?: state.openActionsSheet(id) })
        }
        val removal = events.map { it.source.removal }.distinct().singleOrNull()
        IconButton(removal?.icon ?: AppIcons.Trash, removal?.action ?: "Remove", onClick = { state.removeEvents(selected) })
    }
    BottomSheetFrame(visible = choosingTags, onDismiss = { choosingTags = false }) { EventTagPickerBody(state, selected) }
}

@Composable
fun SelectionBar(
    selectedCount: Int,
    onClear: () -> Unit,
    onSelectAll: (() -> Unit)?,
    hiddenCount: Int = 0,
    actions: @Composable RowScope.() -> Unit,
) {
    SystemBackHandler(enabled = true, handlesEscape = true, onBack = onClear)
    Surface(
        Modifier.fillMaxWidth().floatingSurface(RoundedCornerShape(Radius.md)),
        shape = RoundedCornerShape(Radius.md),
        color = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = floatingBorder(),
    ) {
        Row(Modifier.heightIn(min = ControlHeight.lg), verticalAlignment = Alignment.CenterVertically) {
            IconButton(AppIcons.X, "Clear Selection", onClear)
            Text("$selectedCount", maxLines = 1, style = MaterialTheme.typography.titleMedium)
            if (hiddenCount > 0) Text("+$hiddenCount Hidden", Modifier.padding(start = Spacing.xs), color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            Row(
                Modifier.weight(1f).horizontalScroll(rememberScrollState()).padding(horizontal = Spacing.xs),
                verticalAlignment = Alignment.CenterVertically,
                content = actions,
            )
            if (onSelectAll != null) IconButton(AppIcons.SelectAll, "Select All", onClick = onSelectAll)
        }
    }
}

@Composable
fun CopySummariesButton(summaries: List<String>) {
    val copy = rememberCopyText()
    var copied by remember { mutableStateOf(false) }
    LaunchedEffect(copied) {
        if (copied) {
            delay(COPIED_LABEL_MILLIS)
            copied = false
        }
    }
    IconButton(
        if (copied) AppIcons.Check else AppIcons.Copy,
        if (copied) "Copied!" else if (summaries.size == 1) "Copy Summary" else "Copy Summaries",
        onClick = {
            copy(summaries.joinToString("\n\n"))
            copied = true
        },
    )
}
