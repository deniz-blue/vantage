package blue.deniz.vantage.app.ui

import directory.evnt.OpenEvnt
import blue.deniz.vantage.core.statusOn
import blue.deniz.vantage.core.shownOccurrences
import androidx.compose.foundation.layout.height
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.overscroll
import androidx.compose.foundation.rememberOverscrollEffect
import androidx.compose.foundation.withoutVisualEffect
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text as MaterialText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import blue.deniz.vantage.core.backgroundSplashMedia
import blue.deniz.vantage.core.EventSource
import blue.deniz.vantage.core.ResolvedEvent
import blue.deniz.vantage.storage.TagEntity
import directory.evnt.Media

private val ACCENT_WIDTH = 4.dp
private val SELECTED_BORDER_WIDTH = 2.dp
private val SELECTION_MARK_SIZE = 48.dp
val EventCardHeight = 114.dp
private val EVENT_CARD_MIN_WIDTH = 320.dp

@Composable
fun EventCardGrid(
    state: LazyGridState,
    topPadding: Dp,
    bottomPadding: Dp,
    startGutter: Dp = 0.dp,
    decoration: (columns: Int) -> Modifier = { Modifier },
    content: LazyGridScope.(cardMinHeight: Dp) -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val columns = eventCardColumns(maxWidth, startGutter)
        val overscroll = rememberOverscrollEffect()
        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            state = state,
            modifier = Modifier.fillMaxSize().then(if (overscroll != null) Modifier.overscroll(overscroll) else Modifier).then(decoration(columns)),
            overscrollEffect = overscroll?.withoutVisualEffect(),
            contentPadding = PaddingValues(start = Spacing.md + startGutter, end = Spacing.md, top = topPadding, bottom = bottomPadding),
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            content(if (columns > 1) EventCardHeight else 0.dp)
        }
    }
}

fun eventCardColumns(width: Dp, startGutter: Dp): Int =
    ((width - startGutter - Spacing.md * 2 + Spacing.xs) / (EVENT_CARD_MIN_WIDTH + Spacing.xs)).toInt().coerceAtLeast(1)

fun Modifier.accentStripe(color: Color?): Modifier =
    if (color == null) this
    else drawWithContent {
        drawContent()
        drawRect(color, size = Size(ACCENT_WIDTH.toPx(), size.height))
    }

@Composable
fun EventTitle(
    event: ResolvedEvent,
    preferredLanguages: List<String>,
    modifier: Modifier = Modifier,
    maxLines: Int = 1,
    minLines: Int = 1,
    style: TextStyle = MaterialTheme.typography.titleMedium,
    fallbackTitle: String? = null,
) {
    val name = event.data?.translatedName(preferredLanguages) ?: fallbackTitle
    if (name != null) Text(name, modifier, fontWeight = FontWeight.Bold, maxLines = maxLines, minLines = minLines, style = style)
    else Text(UNTITLED_EVENT, modifier, color = MaterialTheme.colorScheme.onSurfaceVariant, fontStyle = FontStyle.Italic, fontWeight = FontWeight.Bold, maxLines = maxLines, minLines = minLines, style = style)
}

@Composable
fun EventTitleRow(
    event: ResolvedEvent,
    preferredLanguages: List<String>,
    maxLines: Int = 1,
    minLines: Int = 1,
    style: TextStyle = MaterialTheme.typography.titleMedium,
    changeLabel: String? = null,
    fallbackTitle: String? = null,
) {
    val firstLineHeight = with(LocalDensity.current) { style.lineHeight.toDp() }
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        EventTitle(event, preferredLanguages, Modifier.weight(1f, fill = false), maxLines = maxLines, minLines = minLines, style = style, fallbackTitle = fallbackTitle)
        Row(
            Modifier.height(firstLineHeight),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            if (event.source != EventSource.Unknown) {
                Icon(event.source.icon, size = IconSize.xs, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (event.error != null) {
                Icon(AppIcons.AlertTriangle, Modifier.semantics { contentDescription = "Has an Error" }, size = IconSize.xs, tint = MaterialTheme.colorScheme.error)
            }
            if (changeLabel != null) {
                Text(changeLabel, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
fun IconLine(
    icon: ImageVector,
    text: String,
    color: Color = MaterialTheme.colorScheme.onSurface,
    style: TextStyle = MaterialTheme.typography.bodyMedium,
    fontStyle: FontStyle? = null,
    status: OpenEvnt.Status? = null,
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Icon(icon, size = IconSize.sm, tint = color)
        TextAfterStatus(status, text, color, style, fontStyle)
    }
}

@Composable
fun TextAfterStatus(status: OpenEvnt.Status?, text: String, color: Color, style: TextStyle, fontStyle: FontStyle? = null) {
    Row {
        if (status != null) Text(status.label, color = status.color, fontWeight = FontWeight.Bold, maxLines = 1, style = style)
        val rest = if (status != null && text.isNotEmpty()) " · $text" else text
        if (rest.isNotEmpty()) Text(rest, Modifier.weight(1f, fill = false), color = color, fontStyle = fontStyle, maxLines = 1, style = style)
    }
}

@Composable
fun EventCard(
    event: ResolvedEvent,
    formatting: EventFormatting,
    preferredLanguages: List<String>,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    onOpenActions: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = onOpenActions,
    selection: EventCardSelection = EventCardSelection.NotSelecting,
    tags: List<TagEntity> = emptyList(),
    accentColor: Color? = null,
    changeLabel: String? = null,
    fallbackTitle: String? = null,
    problem: String? = null,
    minHeight: Dp = 0.dp,
) {
    val lines = remember(event.data, formatting, preferredLanguages) { event.data?.cardLines(formatting, preferredLanguages).orEmpty() }
    val status = remember(event.data) { event.data?.let { data -> data.statusOn(data.shownOccurrences().firstOrNull()) } }
    val splash = remember(event.data) { event.data?.backgroundSplashMedia() }
    val hasTrailingControl = selection != EventCardSelection.NotSelecting || onOpenActions != null
    Card(
        modifier = modifier,
        onClick = onClick,
        onLongClick = onLongClick,
        border = selection.border(),
        contentPadding = PaddingValues(),
    ) {
        Box(Modifier.accentStripe(accentColor).heightIn(min = minHeight), propagateMinConstraints = true) {
            if (splash != null) EventImageBackdrop(splash, preferredLanguages)
            Row(
                Modifier.padding(start = Spacing.sm, top = Spacing.sm, bottom = Spacing.sm, end = if (hasTrailingControl) 0.dp else Spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    EventTitleRow(event, preferredLanguages, changeLabel = changeLabel, fallbackTitle = fallbackTitle)
                    for ((index, line) in lines.withIndex()) IconLine(line.kind.icon, line.text, status = status.takeIf { index == 0 })
                    if (lines.isEmpty() && status != null) IconLine(AppIcons.Calendar, "", status = status)
                    if (problem != null) IconLine(AppIcons.AlertTriangle, problem, color = MaterialTheme.colorScheme.error)
                    if (tags.isNotEmpty()) EventCardTags(tags)
                }
                when {
                    selection != EventCardSelection.NotSelecting -> SelectionMark(selection == EventCardSelection.Selected)
                    onOpenActions != null -> IconButton(AppIcons.DotsVertical, "Event Actions", onOpenActions)
                }
            }
        }
    }
}

enum class EventCardSelection { NotSelecting, Unselected, Selected }

@Composable
fun EventCardSelection.border(): BorderStroke? =
    if (this == EventCardSelection.Selected) BorderStroke(SELECTED_BORDER_WIDTH, MaterialTheme.colorScheme.primary) else null

@Composable
fun SelectionMark(selected: Boolean, modifier: Modifier = Modifier) {
    Box(modifier.size(SELECTION_MARK_SIZE), contentAlignment = Alignment.Center) {
        if (selected) Icon(AppIcons.CheckCircle, Modifier.semantics { contentDescription = "Selected" }, tint = MaterialTheme.colorScheme.primary)
        else Icon(AppIcons.Circle, Modifier.semantics { contentDescription = "Not Selected" }, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
internal fun EventCardTags(tags: List<TagEntity>) {
    val names = buildAnnotatedString {
        for ((index, tag) in tags.withIndex()) {
            if (index > 0) append("  ")
            withStyle(SpanStyle(color = tagColor(tag.color))) { append(tag.name) }
        }
    }
    MaterialText(names, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelSmall)
}

private const val BACKDROP_DIM_ALPHA = 0.8f
private const val BACKDROP_EDGE_SHADOW_ALPHA = 0.5f

fun Modifier.dimAsBackdrop(): Modifier = drawWithContent {
    drawContent()
    drawRect(Color.Black.copy(alpha = BACKDROP_DIM_ALPHA))
    val edge = Color.Black.copy(alpha = BACKDROP_EDGE_SHADOW_ALPHA)
    drawRect(Brush.horizontalGradient(listOf(edge, Color.Transparent, edge)))
    drawRect(Brush.verticalGradient(listOf(edge, Color.Transparent, edge)))
}

@Composable
fun BoxScope.EventImageBackdrop(media: Media, preferredLanguages: List<String>, whenImageFails: @Composable BoxScope.() -> Unit = {}) {
    var failed by remember(media) { mutableStateOf(false) }
    if (failed) whenImageFails()
    else OpenEvntImage(media, preferredLanguages, Modifier.matchParentSize().clearAndSetSemantics {}.dimAsBackdrop(), onFailed = { failed = true })
}
