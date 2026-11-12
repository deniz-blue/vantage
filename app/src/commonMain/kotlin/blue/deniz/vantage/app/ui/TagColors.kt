package blue.deniz.vantage.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import blue.deniz.vantage.storage.TagEntity

val TagPalette = listOf("#a97dff", "#e879c6", "#ff7a7a", "#ffa35c", "#f0c84a", "#8fd16f", "#43c6a8", "#56b3f5", "#8b93ff", "#a3a3a3")

fun tagColorOrNull(hex: String?): Color? =
    hex?.removePrefix("#")?.takeIf { it.length == 6 }?.toLongOrNull(16)?.let { Color(0xFF000000L or it) }

fun leastUsedTagColor(existing: List<TagEntity>): String = TagPalette.minBy { color -> existing.count { it.color == color } }

@Composable
fun tagColor(hex: String?): Color = tagColorOrNull(hex) ?: MaterialTheme.colorScheme.onSurfaceVariant

@Composable
fun TagFilterChips(tags: List<TagEntity>, chosenIds: Set<String>, onToggle: (TagEntity) -> Unit, modifier: Modifier = Modifier) {
    TagChoicePills(tags, InclusionFilter(only = chosenIds), onTap = onToggle, modifier = modifier)
}
