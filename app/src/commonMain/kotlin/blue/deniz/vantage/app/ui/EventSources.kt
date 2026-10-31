package blue.deniz.vantage.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import blue.deniz.vantage.core.EventSource
import blue.deniz.vantage.core.EventSourceType
import blue.deniz.vantage.core.type

val EventSourceType.icon: ImageVector
    get() = when (this) {
        EventSourceType.Unknown -> AppIcons.Braces
        EventSourceType.Local -> AppIcons.Database
        EventSourceType.Http, EventSourceType.At, EventSourceType.MediaWiki, EventSourceType.WebDav -> AppIcons.World
        EventSourceType.Folio -> AppIcons.Broadcast
        EventSourceType.Calendar -> AppIcons.Calendar
    }

val EventSourceType.label: String
    get() = when (this) {
        EventSourceType.Unknown -> "Unknown"
        EventSourceType.Local -> "Browser/Device"
        EventSourceType.Http -> "Web"
        EventSourceType.At -> "Atmosphere"
        EventSourceType.Folio -> "Folio"
        EventSourceType.MediaWiki -> "MediaWiki"
        EventSourceType.WebDav -> "WebDAV"
        EventSourceType.Calendar -> "Calendars"
    }

enum class EventRemoval(val action: String, val done: String, val icon: ImageVector) {
    Delete("Delete", "Deleted", AppIcons.Trash),
    Hide("Hide", "Hidden", AppIcons.Hidden),
    Unfollow("Unfollow", "Unfollowed", AppIcons.Minus),
}

val EventSource.removal: EventRemoval
    get() = when (this) {
        is EventSource.Local, EventSource.Unknown -> EventRemoval.Delete
        is EventSource.Calendar -> EventRemoval.Hide
        is EventSource.Remote -> EventRemoval.Unfollow
    }

val EventSource.icon: ImageVector
    get() = if (this is EventSource.Local && isTestEvent) AppIcons.Code else type.icon

@Composable
fun SourceTypeChip(type: EventSourceType, onClick: () -> Unit, modifier: Modifier = Modifier) {
    AssistChip(
        onClick = onClick,
        label = { Text(type.label, maxLines = 1) },
        modifier = modifier.floatingSurface(AssistChipDefaults.shape),
        leadingIcon = { Icon(type.icon, size = IconSize.xs) },
        trailingIcon = { Icon(AppIcons.X, size = IconSize.xs) },
        colors = AssistChipDefaults.assistChipColors(containerColor = Color.Transparent),
        border = floatingBorder(),
    )
}

@Composable
fun SourceTypeFilterChips(types: List<EventSourceType>, chosen: Set<EventSourceType>, onToggle: (EventSourceType) -> Unit, modifier: Modifier = Modifier) {
    FlowRow(modifier, horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        for (type in types) {
            FilterChip(
                selected = type in chosen,
                onClick = { onToggle(type) },
                label = { Text(type.label, maxLines = 1) },
                leadingIcon = { Icon(if (type in chosen) AppIcons.Check else type.icon, size = IconSize.xs) },
            )
        }
    }
}
