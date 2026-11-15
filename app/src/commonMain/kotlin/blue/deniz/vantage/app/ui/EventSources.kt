package blue.deniz.vantage.app.ui

import androidx.compose.ui.graphics.vector.ImageVector
import blue.deniz.vantage.core.EventSource
import blue.deniz.vantage.core.EventSourceType
import blue.deniz.vantage.core.type
import blue.deniz.vantage.ui.AppIcons

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
