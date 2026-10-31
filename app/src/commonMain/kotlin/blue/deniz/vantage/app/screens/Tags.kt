package blue.deniz.vantage.app.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import blue.deniz.vantage.app.collectAcrossSession
import blue.deniz.vantage.app.AppState
import blue.deniz.vantage.app.rememberShareText
import blue.deniz.vantage.app.ui.AppIcons
import blue.deniz.vantage.app.ui.BottomSheet
import blue.deniz.vantage.app.ui.BottomSheetFrame
import blue.deniz.vantage.app.ui.BottomSheetScope
import blue.deniz.vantage.app.ui.Icon
import blue.deniz.vantage.app.ui.IconSize
import blue.deniz.vantage.app.ui.ListRow
import blue.deniz.vantage.app.ui.ListUnderOverlay
import blue.deniz.vantage.app.ui.MenuItem
import blue.deniz.vantage.app.ui.ScreenHeader
import blue.deniz.vantage.app.ui.SearchField
import blue.deniz.vantage.app.ui.FieldFrame
import blue.deniz.vantage.app.ui.Spacing
import blue.deniz.vantage.app.ui.TagChip
import blue.deniz.vantage.app.ui.TagPalette
import blue.deniz.vantage.app.ui.Text
import blue.deniz.vantage.app.ui.TextInput
import blue.deniz.vantage.app.ui.leastUsedTagColor
import blue.deniz.vantage.app.ui.tagColor
import blue.deniz.vantage.app.ui.tagColorOrNull
import blue.deniz.vantage.storage.TagEntity
import kotlinx.coroutines.launch
import blue.deniz.vantage.app.ui.ContentMaxWidth

private val TagSwatchMaxSize = 44.dp

fun tagCountLabel(count: Int) = if (count == 1) "1 Tag" else "$count Tags"

fun eventCountLabel(count: Int) = if (count == 1) "1 Event" else "$count Events"

private fun List<TagEntity>.matching(query: String) = filter { it.name.contains(query, ignoreCase = true) }
    .sortedBy { tag ->
        when {
            tag.name.equals(query, ignoreCase = true) -> 0
            tag.name.startsWith(query, ignoreCase = true) -> 1
            else -> 2
        }
    }

private fun List<TagEntity>.named(name: String) = find { it.name.equals(name, ignoreCase = true) }

@Composable
fun EventTags(state: AppState, eventId: String, shareLink: String?) {
    val share = rememberShareText()
    val tagsByEvent = collectAcrossSession("tags by event", null) { state.store.observeTagsByEvent() }
    var choosing by rememberSaveable { mutableStateOf(false) }
    val shown = (tagsByEvent ?: return)[eventId].orEmpty()
    FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        for (tag in shown) TagChip(tag, onClick = { choosing = true })
        AssistChip(
            onClick = { choosing = true },
            label = { Text("Add Tag", maxLines = 1) },
            leadingIcon = { Icon(AppIcons.Plus, size = IconSize.xs, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
        )
        if (shareLink != null) {
            AssistChip(
                onClick = { share(shareLink) },
                label = { Text("Share", maxLines = 1) },
                leadingIcon = { Icon(AppIcons.Share, size = IconSize.xs, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
            )
        }
    }
    BottomSheetFrame(visible = choosing, onDismiss = { choosing = false }) { EventTagPickerBody(state, setOf(eventId)) }
}

@Composable
fun BottomSheetScope.EventTagPickerBody(state: AppState, eventIds: Set<String>, onBack: (() -> Unit)? = null) {
    val tags = collectAcrossSession("tags", null) { state.store.observeTags() }.orEmpty()
    val tagsByEvent = collectAcrossSession("tags by event", null) { state.store.observeTagsByEvent() }.orEmpty()
    val taggedCounts = eventIds.flatMap { id -> tagsByEvent[id].orEmpty() }.groupingBy { it.id }.eachCount()
    val scope = rememberCoroutineScope()
    var query by rememberSaveable { mutableStateOf("") }
    val name = query.trim()
    val existing = tags.named(name)

    fun isOnAll(tag: TagEntity) = taggedCounts[tag.id] == eventIds.size

    fun toggle(tag: TagEntity) {
        scope.launch { if (isOnAll(tag)) state.store.untagEvents(eventIds, tag.id) else state.store.tagEvents(eventIds, tag.id) }
    }

    fun create() {
        val typed = query.trim()
        if (typed.isEmpty()) return
        scope.launch { state.store.tagEvents(eventIds, state.store.addTag(typed, leastUsedTagColor(tags))) }
        query = ""
    }

    fun submit() {
        val match = tags.matching(query.trim()).firstOrNull()
        if (match == null) return create()
        toggle(match)
        query = ""
    }

    ListBody(
        header = { PickerHeader("Tags", onBack, Modifier.padding(horizontal = Spacing.md)) },
        overlay = { SearchField(query, { query = it }, placeholder = "Search or create tags", modifier = Modifier.fillMaxWidth(), onSubmit = ::submit, focusesWhenShown = true) },
    ) {
        items(tags.matching(name), key = { it.id }) { tag ->
            PickerRow(
                tag.name,
                supporting = null,
                selected = isOnAll(tag),
                partlySelected = (taggedCounts[tag.id] ?: 0) > 0,
                onClick = { toggle(tag) },
                leading = { Icon(AppIcons.Tag, tint = tagColor(tag.color)) },
            )
        }
        if (name.isNotEmpty() && existing == null) {
            item(key = "create") { PickerRow("Create “$name”", supporting = null, selected = false, onClick = ::create, leading = { Icon(AppIcons.Plus) }) }
        }
    }
}

@Composable
fun TagSettingsScreen(state: AppState) {
    val counted = collectAcrossSession("tags with event counts", null) { state.store.observeTagsWithEventCounts() }.orEmpty()
    val tags = counted.map { it.tag }
    val countedById = counted.associateBy { it.tag.id }
    val scope = rememberCoroutineScope()
    var query by rememberSaveable { mutableStateOf("") }
    var editedId by rememberSaveable { mutableStateOf<String?>(null) }
    val name = query.trim()
    val existing = tags.named(name)

    fun create() {
        val typed = query.trim()
        if (typed.isEmpty()) return
        scope.launch { state.store.addTag(typed, leastUsedTagColor(tags)) }
        query = ""
    }

    fun submit() {
        val match = tags.matching(query.trim()).firstOrNull()
        if (match == null) return create()
        editedId = match.id
        query = ""
    }

    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        ScreenHeader("Tags", onBack = { state.navigator.goBack() })
        ListUnderOverlay(
            overlay = { SearchField(query, { query = it }, placeholder = "Search or create tags", modifier = Modifier.fillMaxWidth(), onSubmit = ::submit) },
            bottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + Spacing.md,
            modifier = Modifier.weight(1f).widthIn(max = ContentMaxWidth),
        ) {
            items(tags.matching(name).mapNotNull { countedById[it.id] }, key = { it.tag.id }) { (tag, eventCount) ->
                ListRow(
                    tag.name,
                    leading = { Icon(AppIcons.Tag, tint = tagColor(tag.color)) },
                    supporting = if (eventCount == 0) "No Events" else eventCountLabel(eventCount),
                    onClick = { editedId = tag.id },
                )
            }
            if (name.isNotEmpty() && existing == null) {
                item(key = "create") { ListRow("Create “$name”", icon = AppIcons.Plus, onClick = ::create) }
            }
        }
    }
    BottomSheet(item = tags.find { it.id == editedId }, onDismiss = { editedId = null }) { tag ->
        key(tag.id) { TagEditor(state, tag, tags, onClose = { editedId = null }) }
    }
}

@Composable
private fun TagEditor(state: AppState, tag: TagEntity, tags: List<TagEntity>, onClose: () -> Unit) {
    val scope = rememberCoroutineScope()
    var name by rememberSaveable { mutableStateOf(tag.name) }
    val otherTagWithName = tags.named(name.trim())?.takeIf { it.id != tag.id }
    LaunchedEffect(name) { if (name.isNotBlank() && otherTagWithName == null && name.trim() != tag.name) state.store.renameTag(tag.id, name) }
    Column(Modifier.padding(horizontal = Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
        TextInput(
            name,
            { name = it },
            Modifier.fillMaxWidth(),
            label = "Name",
            supportingText = otherTagWithName?.let { "Another tag is called “${it.name}”" },
            isError = otherTagWithName != null,
            leading = { Icon(AppIcons.Tag, tint = tagColor(tag.color)) },
        )
        FieldFrame("Color") { TagColorSwatches(tag.color) { color -> scope.launch { state.store.recolorTag(tag.id, color) } } }
    }
    Column {
        MenuItem("Show Events", onClick = { onClose(); state.showEventsTagged(tag.id) }, leadingIcon = AppIcons.List)
        MenuItem("Delete", onClick = { onClose(); state.deleteTag(tag.id) }, leadingIcon = AppIcons.Trash, danger = true)
    }
}

@Composable
private fun TagColorSwatches(selected: String?, onPick: (String) -> Unit) {
    Row(Modifier.widthIn(max = TagSwatchMaxSize * TagPalette.size).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        for (hex in TagPalette) {
            val color = tagColorOrNull(hex) ?: Color.Unspecified
            val chosen = hex.equals(selected, ignoreCase = true)
            Box(
                Modifier
                    .weight(1f)
                    .aspectRatio(1f)
                    .clip(CircleShape)
                    .background(color)
                    .clickable(role = Role.RadioButton) { onPick(hex) }
                    .semantics {
                        contentDescription = hex
                        this.selected = chosen
                    },
                contentAlignment = Alignment.Center,
            ) {
                if (chosen) Icon(AppIcons.Check, tint = MaterialTheme.colorScheme.surface)
            }
        }
    }
}
