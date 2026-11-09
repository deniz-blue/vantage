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
import androidx.compose.ui.draw.clip
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
import blue.deniz.vantage.core.CountryTimeZones
import blue.deniz.vantage.core.MapLinks
import blue.deniz.vantage.app.ui.CardShape
import blue.deniz.vantage.app.ui.Icon
import blue.deniz.vantage.app.ui.SpotPickerMap
import androidx.compose.foundation.layout.height
import androidx.compose.ui.unit.dp
import directory.evnt.Component
import blue.deniz.vantage.app.ui.displayName
import blue.deniz.vantage.core.Place
import blue.deniz.vantage.core.ResolvedEvent
import directory.evnt.Venue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay

private const val MAP_SEARCH_MIN_LENGTH = 3
private const val MAP_SEARCH_DELAY_MILLIS = 400L
private const val USED_PLACES_SHOWN = 5
private const val COORDINATE_DECIMALS = 100_000
private val SPOT_MAP_HEIGHT = 260.dp

private fun Component.Coordinates.shortText() =
    "${(latitude * COORDINATE_DECIMALS).toLong().toDouble() / COORDINATE_DECIMALS}, ${(longitude * COORDINATE_DECIMALS).toLong().toDouble() / COORDINATE_DECIMALS}"

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
    val linked = MapLinks.coordinatesIn(query)
    var choosingOnMap by rememberSaveable { mutableStateOf(false) }
    var showsAllUsed by rememberSaveable { mutableStateOf(false) }
    var movedMapCentre by remember { mutableStateOf<Component.Coordinates?>(null) }
    val mapCentre = movedMapCentre ?: near ?: state.settings.myLocation?.let { Component.Coordinates(it.latitude, it.longitude) }
        ?: CountryTimeZones.of(state.timeZone.id)?.coordinates ?: Component.Coordinates(0.0, 0.0)
    var linkedAddress by remember { mutableStateOf<Place?>(null) }
    LaunchedEffect(linked) {
        linkedAddress = null
        val point = linked ?: return@LaunchedEffect
        linkedAddress = try {
            state.places.addressAt(point)
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            null
        }
    }
    if (choosingOnMap) {
        SpotPickerBody(state, mapCentre, onMoved = { movedMapCentre = it }, onBack = { choosingOnMap = false }, onPick = { onPick(it.toVenue(language)) })
        return
    }
    var mapResults by remember { mutableStateOf(emptyList<Place>()) }
    var mapProblem by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(query) {
        val text = query.trim()
        if (text.length < MAP_SEARCH_MIN_LENGTH || MapLinks.coordinatesIn(text) != null) {
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
                focusesWhenShown = false,
                trailing = if (query.isEmpty()) null else ({ IconButton(AppIcons.X, "Clear", onClick = { query = "" }) }),
            )
        },
    ) {
        if (unsetLabel != null && query.isBlank()) {
            item(key = "unset") { ListRow(unsetLabel, icon = AppIcons.World, onClick = onUnset) }
        }
        if (query.isBlank()) {
            item(key = "open-map") {
                ListRow("Map", icon = AppIcons.Map, onClick = { choosingOnMap = true }, trailing = { NextLevelChevron() })
            }
        }
        linked?.let { point ->
            item(key = "link") {
                ListRow(
                    linkedAddress?.name ?: point.shortText(),
                    icon = AppIcons.MapPin,
                    color = MaterialTheme.colorScheme.primary,
                    supporting = listOfNotNull(linkedAddress?.address, point.shortText()).joinToString(" · "),
                    onClick = {
                        movedMapCentre = point
                        choosingOnMap = true
                    },
                    trailing = { NextLevelChevron() },
                )
            }
        }
        val usedShown = if (query.isBlank() && !showsAllUsed) matches.take(USED_PLACES_SHOWN) else matches
        if (matches.isNotEmpty() && linked == null) {
            item(key = "used") { PlaceSectionTitle("Used Before") }
            items(usedShown, key = { "used:${it.name}:${it.venue.address}" }) { place ->
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
            if (usedShown.size < matches.size) {
                item(key = "used-all") {
                    ListRow(
                        "Show All",
                        icon = AppIcons.ChevronDown,
                        onClick = { showsAllUsed = true },
                        trailing = { Text(matches.size.toString(), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelLarge) },
                    )
                }
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
        if (onNewPlace != null && query.isNotBlank() && linked == null) {
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
private fun BottomSheetScope.SpotPickerBody(
    state: AppState,
    centre: Component.Coordinates,
    onMoved: (Component.Coordinates) -> Unit,
    onBack: () -> Unit,
    onPick: (Place) -> Unit,
) {
    var spot by remember { mutableStateOf<Place?>(null) }
    var nearby by remember { mutableStateOf(emptyList<Place>()) }
    var problem by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(centre) {
        delay(MAP_SEARCH_DELAY_MILLIS)
        try {
            spot = state.places.addressAt(centre)
            nearby = state.places.placesNear(centre)
            problem = null
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            problem = "Map search failed: ${error.message ?: "no answer"}"
        }
    }
    ListBody(
        header = {
            PickerHeader("Map", onBack = onBack, Modifier.padding(horizontal = Spacing.md))
            SpotPickerMap(centre, onMoved, Modifier.fillMaxWidth().padding(horizontal = Spacing.md).height(SPOT_MAP_HEIGHT).clip(CardShape))
        },
        overlay = {},
    ) {
        spot?.let { place ->
            item(key = "spot") {
                ListRow(
                    place.name,
                    icon = AppIcons.MapPin,
                    color = MaterialTheme.colorScheme.primary,
                    supporting = listOfNotNull(place.address, place.coordinates?.shortText()).joinToString(" · "),
                    onClick = { onPick(place) },
                )
            }
        }
        problem?.let { item(key = "problem") { FailureRow(it, onRetry = null, Modifier.padding(Spacing.md)) } }
        if (nearby.isNotEmpty()) {
            item(key = "nearby") { PlaceSectionTitle("Nearby") }
            items(nearby, key = { "nearby:${it.placeId?.id ?: it.name}:${it.address}" }) { place ->
                ListRow(place.name, icon = AppIcons.MapPin, supporting = place.address, onClick = { onPick(place) })
            }
        }
    }
}

@Composable
private fun NextLevelChevron() {
    Icon(AppIcons.ChevronRight, tint = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun PlaceSectionTitle(text: String) {
    SectionTitle(text, Modifier.padding(start = Spacing.md, end = Spacing.md, top = Spacing.md, bottom = Spacing.xs))
}
