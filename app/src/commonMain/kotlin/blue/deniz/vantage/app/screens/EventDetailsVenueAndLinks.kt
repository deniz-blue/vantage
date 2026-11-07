package blue.deniz.vantage.app.screens

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import blue.deniz.vantage.app.CopyMenuItem
import blue.deniz.vantage.app.mapsAppLink
import blue.deniz.vantage.app.ui.AppIcons
import blue.deniz.vantage.app.ui.addressLine
import blue.deniz.vantage.app.ui.addressLineWithFlag
import blue.deniz.vantage.app.ui.BottomSheet
import blue.deniz.vantage.app.ui.ControlHeight
import blue.deniz.vantage.app.ui.DISABLED_ALPHA
import blue.deniz.vantage.app.ui.Icon
import blue.deniz.vantage.app.ui.IconSize
import blue.deniz.vantage.app.ui.ListRow
import blue.deniz.vantage.app.ui.dividerAboveInListSection
import blue.deniz.vantage.app.ui.ListRowPadding
import blue.deniz.vantage.app.ui.ListSection
import blue.deniz.vantage.app.ui.SheetTitle
import blue.deniz.vantage.app.ui.Spacing
import blue.deniz.vantage.app.ui.MenuItem
import blue.deniz.vantage.app.ui.Text
import blue.deniz.vantage.core.coordinates
import blue.deniz.vantage.app.ui.linkText
import blue.deniz.vantage.app.ui.LinkText
import blue.deniz.vantage.core.MapLinks
import blue.deniz.vantage.core.MapPlace
import directory.evnt.Component
import directory.evnt.Venue
import directory.evnt.hasText
import directory.evnt.translate
import androidx.compose.ui.draw.alpha

@Composable
internal fun VenueCard(venue: Venue, preferredLanguages: List<String>) {
    val name = venue.name.takeIf { it.hasText() }?.translate(preferredLanguages)
    var showingDetails by rememberSaveable { mutableStateOf(false) }
    val addressLine = venue.addressLine(preferredLanguages)
    ListRow(
        name ?: addressLine ?: "Unnamed Place",
        leading = { DetailTile { Icon(AppIcons.MapPin, tint = MaterialTheme.colorScheme.onSurfaceVariant) } },
        supporting = addressLine.takeIf { name != null },
        onClick = { showingDetails = true },
    )
    BottomSheet(visible = showingDetails, onDismiss = { showingDetails = false }) { VenueDetails(venue, name, venue.addressLineWithFlag(preferredLanguages)) }
}

@Composable
private fun VenueDetails(venue: Venue, name: String?, addressLine: String?) {
    val uriHandler = LocalUriHandler.current
    val place = remember(venue, name) { MapPlace.of(venue, name) }
    val coordinates = venue.coordinates?.let { "${it.latitude}, ${it.longitude}" }
    Column(Modifier.padding(horizontal = Spacing.md)) {
        SheetTitle(name ?: addressLine ?: "Unnamed Place")
        if (name != null) addressLine?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium) }
        coordinates?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall) }
    }
    Column {
        if (place != null) {
            MenuItem("Open in Maps", onClick = { uriHandler.openMapLink(place) }, leadingIcon = AppIcons.MapPin)
            MenuItem("Directions", onClick = { uriHandler.openUri(MapLinks.directions(place)) }, leadingIcon = AppIcons.Directions)
            if (mapsAppLink(place) != MapLinks.googleMaps(place)) {
                MenuItem("Google Maps", onClick = { uriHandler.openUri(MapLinks.googleMaps(place)) }, leadingIcon = AppIcons.ExternalLink)
            }
            MenuItem("OpenStreetMap", onClick = { uriHandler.openUri(MapLinks.openStreetMap(place)) }, leadingIcon = AppIcons.ExternalLink)
        }
        venue.address?.let { address -> CopyMenuItem("Copy Address", supportingText = address) { address } }
        coordinates?.let { CopyMenuItem("Copy Coordinates", supportingText = it) { it } }
    }
}

private fun UriHandler.openMapLink(place: MapPlace) {
    try {
        openUri(mapsAppLink(place))
    } catch (_: IllegalArgumentException) {
        openUri(MapLinks.googleMaps(place))
    }
}

@Composable
internal fun LinkSection(links: List<Component.Link>, preferredLanguages: List<String>, onLongPress: (url: String) -> Unit, atColumnTop: Boolean = false) {
    if (links.isEmpty()) return
    val uriHandler = LocalUriHandler.current
    ListSection(Modifier.padding(top = if (atColumnTop) 0.dp else Spacing.md), title = "Links") {
        for (link in links) {
            LinkButton(
                link.linkText(preferredLanguages),
                onClick = { uriHandler.openUri(link.url) },
                onLongClick = { onLongPress(link.url) },
                enabled = link.disabled != true,
            )
        }
    }
}

@Composable
private fun LinkButton(text: LinkText, onClick: () -> Unit, onLongClick: () -> Unit, enabled: Boolean) {
    Row(
        Modifier
            .fillMaxWidth()
            .dividerAboveInListSection()
            .heightIn(min = ControlHeight.md)
            .alpha(if (enabled) 1f else DISABLED_ALPHA)
            .combinedClickable(enabled = enabled, role = Role.Button, onLongClick = onLongClick, onClick = onClick)
            .padding(horizontal = ListRowPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Icon(AppIcons.ExternalLink, size = IconSize.xs, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text.headline, maxLines = 1, style = MaterialTheme.typography.bodyMedium)
        text.supporting?.let {
            Text(it, Modifier.weight(1f, fill = false), color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, style = MaterialTheme.typography.bodySmall)
        }
    }
}
