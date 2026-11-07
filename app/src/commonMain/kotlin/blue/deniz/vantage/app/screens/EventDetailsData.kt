package blue.deniz.vantage.app.screens

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import blue.deniz.vantage.app.ui.AppIcons
import blue.deniz.vantage.app.ui.icon
import blue.deniz.vantage.app.ui.EventFormatting
import blue.deniz.vantage.app.ui.Icon
import blue.deniz.vantage.app.ui.ListRow
import blue.deniz.vantage.app.ui.ListSection
import blue.deniz.vantage.app.ui.Spacing
import blue.deniz.vantage.core.EventFormat
import blue.deniz.vantage.core.EventSource
import blue.deniz.vantage.core.SubscribedCalendar
import blue.deniz.vantage.core.ResolvedEvent
import directory.evnt.Component
import kotlinx.datetime.toLocalDateTime

@Composable
internal fun DataSection(event: ResolvedEvent, formatting: EventFormatting, onOpenLinkActions: (url: String) -> Unit, atColumnTop: Boolean = false) {
    val uriHandler = LocalUriHandler.current
    val updatedAt = event.updatedAt?.toLocalDateTime(formatting.timeZone)
    val sources = event.data?.components.orEmpty().filterIsInstance<Component.Source>()
    ListSection(Modifier.padding(top = if (atColumnTop) 0.dp else Spacing.md), title = "Data") {
        ListRow(
            "${sourceLabelOf(event.source)} · ${formatLabelOf(event.format)}",
            icon = event.source.icon,
            supporting = if (updatedAt == null) "Last Updated: Unknown"
            else "Last Updated: ${formatting.formatDay(updatedAt.date)} ${formatting.formatClock(updatedAt.time)}",
        )
        sources.forEachIndexed { index, source ->
            ListRow(
                if (sources.size > 1) "Source Link ${index + 1}" else "Source Link",
                alignWithIcons = true,
                supporting = source.url.substringAfter("://"),
                onClick = { uriHandler.openUri(source.url) },
                onLongClick = { onOpenLinkActions(source.url) },
                trailing = { Icon(AppIcons.ExternalLink) },
            )
        }
    }
}

private fun sourceLabelOf(source: EventSource) = when (source) {
    EventSource.Unknown -> "Unknown Source"
    is EventSource.Local -> if (source.isTestEvent) "Test Event" else "Browser/Device"
    is EventSource.At -> "Atmosphere (AT Protocol)"
    is EventSource.Http -> "Internet (HTTP)"
    is EventSource.Folio -> "Internet (Folio)"
    is EventSource.MediaWiki -> "MediaWiki"
    is EventSource.WebDav -> "WebDAV"
    is EventSource.Calendar -> when (source.calendar) {
        is SubscribedCalendar.OnDevice -> "Calendar on This Phone"
        is SubscribedCalendar.Link -> "Calendar Link"
    }
}

private fun formatLabelOf(format: EventFormat) = when (format) {
    EventFormat.Unknown -> "Unknown Format"
    EventFormat.OpenEvnt -> "Open Evnt"
    EventFormat.ICalendar -> "iCalendar (ICS)"
    EventFormat.CommunityLexicon -> "Community Lexicon"
}
