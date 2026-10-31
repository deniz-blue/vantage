package blue.deniz.vantage.app.screens

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import blue.deniz.vantage.app.collectAcrossSession
import blue.deniz.vantage.app.AppState
import blue.deniz.vantage.app.ui.FailureRow
import blue.deniz.vantage.app.ui.AppIcons
import blue.deniz.vantage.app.ui.BottomSheetFrame
import blue.deniz.vantage.app.ui.BottomSheetScope
import blue.deniz.vantage.app.ui.IconButton
import blue.deniz.vantage.app.ui.ListRow
import blue.deniz.vantage.app.ui.SearchField
import blue.deniz.vantage.app.ui.SectionTitle
import blue.deniz.vantage.app.ui.Spacing
import blue.deniz.vantage.app.ui.Text
import blue.deniz.vantage.core.coordinates
import blue.deniz.vantage.app.ui.displayName
import blue.deniz.vantage.core.Place
import blue.deniz.vantage.core.ResolvedEvent
import directory.evnt.Venue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay

private const val MAP_SEARCH_MIN_LENGTH = 3
private const val MAP_SEARCH_DELAY_MILLIS = 400L

private data class UsedPlace(val venue: Venue, val name: String, val eventCount: Int) {
    val coordinates get() = venue.coordinates
}

private fun usedPlacesOf(events: List<ResolvedEvent>, preferredLanguages: List<String>): List<UsedPlace> =
    events.mapNotNull { it.data?.venue }
        .mapNotNull { venue -> venue.displayName(preferredLanguages)?.let { venue to it } }
        .groupBy { (venue, name) -> name.lowercase() to venue.address.orEmpty().lowercase() }
        .map { (_, uses) -> UsedPlace(uses.first().first, uses.first().second, uses.size) }
        .sortedByDescending { it.eventCount }

private fun UsedPlace.matches(query: String) =
    query.isBlank() || name.contains(query.trim(), ignoreCase = true) || venue.address.orEmpty().contains(query.trim(), ignoreCase = true)

@Composable
fun PlaceSearchSheet(
    visible: Boolean,
    title: String,
    state: AppState,
    onDismiss: () -> Unit,
    onPick: (Venue) -> Unit,
    onNewPlace: ((name: String) -> Unit)? = null,
    onlyMapped: Boolean = false,
    unsetLabel: String? = null,
    onUnset: () -> Unit = {},
) {
    BottomSheetFrame(visible = visible, onDismiss = onDismiss) {
        PlaceSearchBody(title, state, onPick, onNewPlace, onlyMapped, unsetLabel, onUnset)
    }
}

@Composable
private fun BottomSheetScope.PlaceSearchBody(
    title: String,
    state: AppState,
    onPick: (Venue) -> Unit,
    onNewPlace: ((String) -> Unit)?,
    onlyMapped: Boolean,
    unsetLabel: String?,
    onUnset: () -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    val language = state.preferredLanguages.first()
    val events = collectAcrossSession("all events", null) { state.store.observeList() }.orEmpty()
    val usedPlaces = remember(events, onlyMapped) { usedPlacesOf(events, state.preferredLanguages).filter { !onlyMapped || it.coordinates != null } }
    val matches = usedPlaces.filter { it.matches(query) }
    val near = usedPlaces.firstNotNullOfOrNull { it.coordinates }
    var mapResults by remember { mutableStateOf(emptyList<Place>()) }
    var mapProblem by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(query) {
        val text = query.trim()
        if (text.length < MAP_SEARCH_MIN_LENGTH) {
            mapResults = emptyList()
            mapProblem = null
            return@LaunchedEffect
        }
        delay(MAP_SEARCH_DELAY_MILLIS)
        try {
            mapResults = state.places.search(text, near)
            mapProblem = null
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            mapProblem = "Map search failed: ${error.message ?: "no answer"}"
        }
    }

    ListBody(
        header = { PickerHeader(title, onBack = null, Modifier.padding(horizontal = Spacing.md)) },
        overlay = {
            SearchField(
                query,
                onValueChange = { query = it },
                placeholder = "Search Places",
                modifier = Modifier.fillMaxWidth(),
                focusesWhenShown = true,
                trailing = if (query.isEmpty()) null else ({ IconButton(AppIcons.X, "Clear", onClick = { query = "" }) }),
            )
        },
    ) {
        if (unsetLabel != null && query.isBlank()) {
            item(key = "unset") { ListRow(unsetLabel, icon = AppIcons.World, onClick = onUnset) }
        }
        if (matches.isNotEmpty()) {
            item(key = "used") { PlaceSectionTitle("Used Before") }
            items(matches, key = { "used:${it.name}:${it.venue.address}" }) { place ->
                ListRow(
                    place.name,
                    icon = AppIcons.History,
                    supporting = listOfNotNull(
                        place.venue.address.takeIf { it != place.name },
                        eventCountLabel(place.eventCount),
                    ).joinToString(" · "),
                    onClick = { onPick(place.venue) },
                )
            }
        }
        if (mapResults.isNotEmpty()) {
            item(key = "map") { PlaceSectionTitle("On The Map") }
            items(mapResults, key = { "map:${it.placeId?.id ?: it.name}:${it.address}" }) { place ->
                ListRow(place.name, icon = AppIcons.MapPin, supporting = place.address, onClick = { onPick(place.toVenue(language)) })
            }
            item(key = "attribution") {
                Text(
                    "Map data © OpenStreetMap contributors",
                    Modifier.padding(horizontal = Spacing.md, vertical = Spacing.xs),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        mapProblem?.let { problem ->
            item(key = "problem") {
                FailureRow(problem, onRetry = null, Modifier.padding(Spacing.md))
            }
        }
        if (onNewPlace != null && query.isNotBlank()) {
            item(key = "new") {
                ListRow(
                    "New Place “${query.trim()}”",
                    icon = AppIcons.AddPlace,
                    onClick = { onNewPlace(query.trim()) },
                )
            }
        }
    }
}

@Composable
private fun PlaceSectionTitle(text: String) {
    SectionTitle(text, Modifier.padding(start = Spacing.md, end = Spacing.md, top = Spacing.md, bottom = Spacing.xs))
}
