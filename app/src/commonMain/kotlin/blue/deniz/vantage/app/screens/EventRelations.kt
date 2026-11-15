package blue.deniz.vantage.app.screens

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import blue.deniz.vantage.app.AppState
import blue.deniz.vantage.app.ui.AppIcons
import blue.deniz.vantage.app.ui.BottomSheetFrame
import blue.deniz.vantage.app.ui.EventFormatting
import blue.deniz.vantage.app.ui.Icon
import blue.deniz.vantage.app.ui.IconSize
import blue.deniz.vantage.app.ui.ListRow
import blue.deniz.vantage.app.ui.ListSection
import blue.deniz.vantage.app.ui.RemoveButton
import blue.deniz.vantage.app.ui.SearchField
import blue.deniz.vantage.app.ui.SegmentedControl
import blue.deniz.vantage.app.ui.Spacing
import blue.deniz.vantage.app.ui.TextInput
import blue.deniz.vantage.app.ui.translatedName
import blue.deniz.vantage.app.collectAcrossSession
import blue.deniz.vantage.core.RelatedEvent
import blue.deniz.vantage.core.ResolvedEvent
import blue.deniz.vantage.core.addressedBy
import blue.deniz.vantage.core.isAddressedBy
import blue.deniz.vantage.core.nextOrLastOccurrenceAt
import blue.deniz.vantage.core.relatedEvents
import directory.evnt.Component
import directory.evnt.OpenEvnt
import kotlin.time.Instant

private val NAMED_RELATION_TYPES = listOf(
    Component.Relation.PARENT to "Parent",
    Component.Relation.CHILD to "Child",
    Component.Relation.CANONICAL to "Main Version",
)

fun relationTypeLabel(type: String): String = NAMED_RELATION_TYPES.firstOrNull { it.first == type }?.second ?: type

private val ABSOLUTE_URI = Regex("""[A-Za-z][A-Za-z0-9+.-]*:\S+""")

private fun hostOf(target: String): String? = target.substringAfter("://", "").substringBefore('/').ifEmpty { null }

private fun EventFormatting.dayOf(event: ResolvedEvent, now: Instant): String? =
    event.data?.nextOrLastOccurrenceAt(now)?.start?.let(::formatShortDay)

private fun relationRowText(type: String, target: String, event: ResolvedEvent?, preferredLanguages: List<String>, day: String?): Pair<String, String> {
    val headline = event?.data?.translatedName(preferredLanguages) ?: target
    val detail = if (event != null) day else hostOf(target).takeIf { NAMED_RELATION_TYPES.none { it.first == type } }
    return headline to listOfNotNull(relationTypeLabel(type), detail).joinToString(" · ")
}

@Composable
internal fun RelatedRows(state: AppState, event: OpenEvnt, onEdit: (Int) -> Unit, onRemove: (Int) -> Unit, onAdd: () -> Unit) {
    val library = collectAcrossSession("all events", null) { state.store.observeList() }.orEmpty()
    for ((index, component) in event.components.orEmpty().withIndex()) {
        val relation = component as? Component.Relation ?: continue
        val other = library.addressedBy(relation.target)
        val (headline, supporting) = relationRowText(relation.type, relation.target, other, state.preferredLanguages, other?.let { state.formatting.dayOf(it, state.now) })
        ListRow(
            headline,
            icon = if (other != null) AppIcons.Calendar else AppIcons.Link,
            supporting = supporting,
            onClick = { onEdit(index) },
            trailing = { RemoveButton("Remove Related Event") { onRemove(index) } },
        )
    }
    ListRow("Add Related Event", icon = AppIcons.Plus, onClick = onAdd)
}

@Composable
internal fun RelatedEventSheet(state: AppState, event: OpenEvnt, editing: Int?, onDismiss: () -> Unit, onSave: (Int?, Component.Relation) -> Unit) {
    BottomSheetFrame(item = editing, onDismiss = onDismiss) { index ->
        val existing = event.components?.getOrNull(index) as? Component.Relation
        val library = collectAcrossSession("all events", null) { state.store.observeList() }.orEmpty()
        val startsNamed = existing == null || NAMED_RELATION_TYPES.any { it.first == existing.type }
        var namedType by rememberSaveable(index) { mutableStateOf<String?>(if (startsNamed) existing?.type ?: Component.Relation.PARENT else null) }
        var otherType by rememberSaveable(index) { mutableStateOf(existing?.type?.takeUnless { startsNamed }.orEmpty()) }
        var query by rememberSaveable(index) { mutableStateOf("") }
        val type = namedType ?: otherType.trim()
        val typeIsValid = Component.Relation.isValidType(type)
        val text = query.trim()
        val pasted = text.takeIf { ABSOLUTE_URI.matches(it) }
        val pastedEvent = pasted?.let(library::addressedBy)
        val matches = if (pasted != null) listOfNotNull(pastedEvent) else library.filter { other ->
            other.data?.id != event.id && text.isNotEmpty() && other.data?.translatedName(state.preferredLanguages)?.contains(text, ignoreCase = true) == true
        }

        fun pick(target: String) {
            if (typeIsValid) onSave(index.takeIf { existing != null }, Component.Relation(type, target))
        }

        ListBody(
            header = {
                PickerHeader("Related Event", onBack = null, Modifier.padding(horizontal = Spacing.md))
                SegmentedControl(
                    options = NAMED_RELATION_TYPES.map { (value, label) -> value as String? to label } + (null to "Other"),
                    selected = namedType,
                    onSelect = { namedType = it },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.md),
                )
                if (namedType == null) {
                    TextInput(
                        otherType,
                        { otherType = it },
                        Modifier.fillMaxWidth().padding(horizontal = Spacing.md),
                        placeholder = "com.example.rivalOf",
                        leading = { Icon(AppIcons.Braces, size = IconSize.sm) },
                        isError = otherType.isNotBlank() && !typeIsValid,
                    )
                }
            },
            overlay = { SearchField(query, { query = it }, placeholder = "Search or paste a link", modifier = Modifier.fillMaxWidth(), focusesWhenShown = existing == null) },
        ) {
            if (!typeIsValid) return@ListBody
            items(matches, key = { "event ${it.id}" }) { other ->
                PickerRow(
                    other.data?.translatedName(state.preferredLanguages) ?: other.data?.id.orEmpty(),
                    supporting = state.formatting.dayOf(other, state.now),
                    selected = existing?.let { other.isAddressedBy(it.target) } == true,
                    onClick = { other.data?.id?.let(::pick) },
                    leading = { Icon(AppIcons.Calendar) },
                )
            }
            if (pasted != null && pastedEvent == null) {
                item(key = "pasted") { PickerRow(pasted, supporting = hostOf(pasted), selected = false, onClick = { pick(pasted) }, leading = { Icon(AppIcons.Link) }) }
            }
        }
    }
}

@Composable
internal fun rememberRelatedEvents(state: AppState, event: ResolvedEvent): List<RelatedEvent> {
    val library = collectAcrossSession("all events", null) { state.store.observeList() }.orEmpty()
    return remember(event, library) { event.relatedEvents(library) }
}

@Composable
internal fun RelatedSection(state: AppState, related: List<RelatedEvent>, atColumnTop: Boolean = false) {
    if (related.isEmpty()) return
    ListSection(Modifier.padding(top = if (atColumnTop) 0.dp else Spacing.md), title = "Related") {
        for (item in related) RelatedEventRow(state, item)
    }
}

@Composable
private fun RelatedEventRow(state: AppState, item: RelatedEvent) {
    val uriHandler = LocalUriHandler.current
    val other = item.event
    val otherId = other?.id
    val (headline, supporting) = relationRowText(item.type, item.target, other, state.preferredLanguages, other?.let { state.formatting.dayOf(it, state.now) })
    val opensLink = other == null && (item.target.startsWith("https://") || item.target.startsWith("http://"))
    val trailingIcon = when {
        otherId != null -> AppIcons.ChevronRight
        opensLink -> AppIcons.ExternalLink
        else -> null
    }
    ListRow(
        headline,
        icon = if (other != null) AppIcons.Calendar else AppIcons.Link,
        supporting = supporting,
        onClick = when {
            otherId != null -> ({ state.openEvent(otherId) })
            opensLink -> ({ uriHandler.openUri(item.target) })
            else -> null
        },
        onLongClick = if (opensLink) ({ state.openLinkActionsSheet(item.target) }) else null,
        trailing = trailingIcon?.let { icon -> { Icon(icon, size = IconSize.sm, tint = MaterialTheme.colorScheme.onSurfaceVariant) } },
    )
}
