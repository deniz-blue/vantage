package blue.deniz.vantage.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import blue.deniz.vantage.core.EventSourceType
import blue.deniz.vantage.storage.TagEntity
import blue.deniz.vantage.ui.AppIcons
import blue.deniz.vantage.ui.Icon
import blue.deniz.vantage.ui.IconSize
import blue.deniz.vantage.ui.Spacing
import blue.deniz.vantage.ui.Text
import blue.deniz.vantage.ui.floatingBorder
import blue.deniz.vantage.ui.floatingSurface

enum class InclusionChoice { Off, Only, Hidden }

data class InclusionFilter<T>(val only: Set<T> = emptySet(), val hidden: Set<T> = emptySet()) {
    val isEmpty: Boolean get() = only.isEmpty() && hidden.isEmpty()

    fun choiceOf(item: T): InclusionChoice = when (item) {
        in only -> InclusionChoice.Only
        in hidden -> InclusionChoice.Hidden
        else -> InclusionChoice.Off
    }

    fun withNextChoice(item: T): InclusionFilter<T> = when (choiceOf(item)) {
        InclusionChoice.Off -> copy(only = only + item)
        InclusionChoice.Only -> copy(only = only - item, hidden = hidden + item)
        InclusionChoice.Hidden -> copy(hidden = hidden - item)
    }

    fun without(item: T): InclusionFilter<T> = InclusionFilter(only - item, hidden - item)
}

private val ChoicePillHeight = 34.dp
private val ChoicePillDotSize = 10.dp
private val ChoicePillRing = 1.5.dp
private const val CHOICE_PILL_FILL_ALPHA = 0.28f
private const val HIDDEN_CONTENT_ALPHA = 0.6f

@Composable
fun ChoicePill(
    label: String,
    choice: InclusionChoice,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    offIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    floating: Boolean = false,
) {
    val colors = MaterialTheme.colorScheme
    val muted = colors.onSurfaceVariant.copy(alpha = HIDDEN_CONTENT_ALPHA)
    val fill = when (choice) {
        InclusionChoice.Off -> colors.surfaceContainerHigh
        InclusionChoice.Only -> accent.copy(alpha = CHOICE_PILL_FILL_ALPHA).compositeOver(colors.surfaceContainerHigh)
        InclusionChoice.Hidden -> colors.surfaceContainerLowest
    }
    val surface = when {
        floating -> modifier.floatingSurface(CircleShape).border(floatingBorder(), CircleShape)
        choice == InclusionChoice.Only -> modifier.background(fill, CircleShape).border(ChoicePillRing, accent, CircleShape)
        else -> modifier.background(fill, CircleShape)
    }
    Row(
        surface.clip(CircleShape).clickable(onClick = onClick).heightIn(min = ChoicePillHeight).padding(horizontal = Spacing.md - Spacing.xs),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when {
            choice == InclusionChoice.Only -> Icon(AppIcons.Check, size = IconSize.xs, tint = accent)
            choice == InclusionChoice.Hidden -> Icon(AppIcons.Hidden, size = IconSize.xs, tint = muted)
            offIcon != null -> Icon(offIcon, size = IconSize.xs, tint = colors.onSurfaceVariant)
            else -> Box(Modifier.size(ChoicePillDotSize).background(accent, CircleShape))
        }
        Text(
            label,
            maxLines = 1,
            color = if (choice == InclusionChoice.Hidden) muted else colors.onSurface,
            fontWeight = if (choice == InclusionChoice.Only) FontWeight.SemiBold else null,
            style = LocalTextStyle.current.copy(textDecoration = if (choice == InclusionChoice.Hidden) TextDecoration.LineThrough else null),
        )
        if (trailingIcon != null) Icon(trailingIcon, size = IconSize.xs, tint = colors.onSurfaceVariant)
    }
}

@Composable
fun PillIconButton(icon: ImageVector, contentDescription: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(ChoicePillHeight)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(onClick = onClick)
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, size = IconSize.xs, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun TagChoicePills(tags: List<TagEntity>, filter: InclusionFilter<String>, onTap: (TagEntity) -> Unit, modifier: Modifier = Modifier) {
    FlowRow(modifier, horizontalArrangement = Arrangement.spacedBy(Spacing.sm), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        for (tag in tags) ChoicePill(tag.name, filter.choiceOf(tag.id), tagColor(tag.color), onClick = { onTap(tag) })
    }
}

@Composable
fun SourceChoicePills(types: List<EventSourceType>, filter: InclusionFilter<EventSourceType>, onTap: (EventSourceType) -> Unit, modifier: Modifier = Modifier) {
    FlowRow(modifier, horizontalArrangement = Arrangement.spacedBy(Spacing.sm), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        for (type in types) ChoicePill(type.label, filter.choiceOf(type), MaterialTheme.colorScheme.primary, onClick = { onTap(type) }, offIcon = type.icon)
    }
}
