package blue.deniz.vantage.app.screens

import directory.evnt.textIn
import directory.evnt.withTextIn
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import blue.deniz.vantage.core.isCancelled
import blue.deniz.vantage.app.ui.ListRowGap
import blue.deniz.vantage.app.ui.ListRowIconGap
import blue.deniz.vantage.app.ui.ListRowPadding
import blue.deniz.vantage.app.ui.ListRowHeight
import blue.deniz.vantage.app.ui.IconSize
import blue.deniz.vantage.app.ui.AppIcons
import blue.deniz.vantage.app.ui.BottomSheet
import blue.deniz.vantage.app.ui.EventFormatting
import blue.deniz.vantage.app.ui.Icon
import blue.deniz.vantage.app.ui.IconButton
import blue.deniz.vantage.app.ui.MarkdownView
import blue.deniz.vantage.app.ui.rememberMarkdownHighlighting
import blue.deniz.vantage.app.ui.ListRow
import blue.deniz.vantage.app.ui.dividerAboveInListSection
import blue.deniz.vantage.app.ui.RemoveButton
import blue.deniz.vantage.app.ui.Spacing
import blue.deniz.vantage.app.ui.Text
import blue.deniz.vantage.app.ui.SheetTitle
import blue.deniz.vantage.app.ui.TextInput
import blue.deniz.vantage.app.ui.linkText
import blue.deniz.vantage.core.Markdown
import directory.evnt.Component
import directory.evnt.Occurrence
import directory.evnt.OpenEvnt
import directory.evnt.Venue
import directory.evnt.hasText
import directory.evnt.textToEdit
import directory.evnt.translate
import directory.evnt.withEditedText
import sh.calvin.reorderable.ReorderableColumn

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

private val NewEventDestination.icon get() = if (this == NewEventDestination.Device) AppIcons.Smartphone else AppIcons.WorldUpload

@Composable
internal fun DestinationRow(destination: NewEventDestination, onSelect: (NewEventDestination) -> Unit) {
    ChoiceRow("Save To", NewEventDestination.entries, destination, label = { it.label }, icon = { it.icon }, onSelect = onSelect)
}

@Composable
internal fun StatusRow(status: OpenEvnt.Status?, onSelect: (OpenEvnt.Status?) -> Unit) {
    ChoiceRow(
        "Status",
        STATUS_OPTIONS.map { it.first },
        status,
        label = { option -> STATUS_OPTIONS.first { it.first == option }.second },
        icon = { it.icon },
        onSelect = onSelect,
        selectedColor = if (status == OpenEvnt.Status.Cancelled) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun <T> ChoiceRow(
    title: String,
    options: List<T>,
    selected: T,
    label: (T) -> String,
    icon: (T) -> ImageVector,
    onSelect: (T) -> Unit,
    selectedColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    var open by rememberSaveable { mutableStateOf(false) }
    ListRow(
        title,
        icon = icon(selected),
        onClick = { open = true },
        trailing = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Text(label(selected), color = selectedColor, style = MaterialTheme.typography.bodyLarge)
                Icon(AppIcons.ChevronRight)
            }
        },
    )
    BottomSheet(visible = open, onDismiss = { open = false }) {
        SheetTitle(title, Modifier.padding(horizontal = Spacing.md))
        Column {
            for (option in options) {
                ListRow(
                    label(option),
                    icon = icon(option),
                    onClick = {
                        open = false
                        onSelect(option)
                    },
                    trailing = { if (option == selected) Icon(AppIcons.Check, tint = MaterialTheme.colorScheme.primary) },
                )
            }
        }
    }
}

@Composable
internal fun NameField(
    event: OpenEvnt,
    language: String,
    preferredLanguages: List<String>,
    focus: FocusRequester,
    isError: Boolean,
    onFocusChange: (Boolean) -> Unit,
    onChange: (OpenEvnt) -> Unit,
) {
    TextInput(
        value = event.name.textIn(language),
        onValueChange = { onChange(event.copy(name = event.name.withTextIn(language, it))) },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.md, vertical = ListRowGap / 2)
            .focusRequester(focus)
            .onFocusChanged { onFocusChange(it.isFocused) }
            .semantics { contentDescription = "Event Name" },
        placeholder = if (isError) "Give it a name" else event.name.translate(preferredLanguages).ifEmpty { "Event Name" },
        isError = isError,
        singleLine = false,
        allowsLineBreaks = false,
        maxLines = 3,
    )
}

@Composable
internal fun DateRows(
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
internal fun PlaceRow(venue: Venue?, preferredLanguages: List<String>, onEdit: () -> Unit, onRemove: () -> Unit, onAdd: () -> Unit) {
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
internal fun LinkRows(
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
internal fun DescriptionField(
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
