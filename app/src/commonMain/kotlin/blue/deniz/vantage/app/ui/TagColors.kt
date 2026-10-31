package blue.deniz.vantage.app.ui

import androidx.compose.ui.graphics.compositeOver
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import blue.deniz.vantage.storage.TagEntity

private const val TAG_CHIP_FILL_ALPHA = 0.16f

val TagPalette = listOf("#a97dff", "#e879c6", "#ff7a7a", "#ffa35c", "#f0c84a", "#8fd16f", "#43c6a8", "#56b3f5", "#8b93ff", "#a3a3a3")

fun tagColorOrNull(hex: String?): Color? =
    hex?.removePrefix("#")?.takeIf { it.length == 6 }?.toLongOrNull(16)?.let { Color(0xFF000000L or it) }

fun leastUsedTagColor(existing: List<TagEntity>): String = TagPalette.minBy { color -> existing.count { it.color == color } }

@Composable
fun tagColor(hex: String?): Color = tagColorOrNull(hex) ?: MaterialTheme.colorScheme.onSurfaceVariant

@Composable
fun TagChip(
    tag: TagEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailingIcon: ImageVector? = null,
    hidden: Boolean = false,
    floating: Boolean = false,
) {
    val color = tagColor(tag.color)
    val fill = color.copy(alpha = TAG_CHIP_FILL_ALPHA)
    AssistChip(
        onClick = onClick,
        label = { Text(tag.name, maxLines = 1) },
        modifier = if (floating) modifier.floatingSurface(AssistChipDefaults.shape) else modifier,
        leadingIcon = { Icon(if (hidden) AppIcons.Hidden else AppIcons.Tag, size = IconSize.xs, tint = color) },
        trailingIcon = trailingIcon?.let { { Icon(it, size = IconSize.xs) } },
        colors = AssistChipDefaults.assistChipColors(containerColor = if (floating) fill else fill.compositeOver(MaterialTheme.colorScheme.background)),
        border = if (floating) floatingBorder() else null,
    )
}

@Composable
fun TagFilterChips(
    tags: List<TagEntity>,
    chosenIds: Set<String>,
    onToggle: (TagEntity) -> Unit,
    modifier: Modifier = Modifier,
    chosenIcon: ImageVector = AppIcons.Check,
) {
    FlowRow(modifier, horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        for (tag in tags) {
            val chosen = tag.id in chosenIds
            val color = tagColor(tag.color)
            FilterChip(
                selected = chosen,
                onClick = { onToggle(tag) },
                label = { Text(tag.name, maxLines = 1) },
                leadingIcon = { Icon(if (chosen) chosenIcon else AppIcons.Tag, size = IconSize.xs, tint = color) },
                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = color.copy(alpha = TAG_CHIP_FILL_ALPHA)),
            )
        }
    }
}
