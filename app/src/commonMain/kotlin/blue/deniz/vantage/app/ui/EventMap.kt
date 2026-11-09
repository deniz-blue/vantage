package blue.deniz.vantage.app.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import blue.deniz.vantage.core.EventSource
import blue.deniz.vantage.core.GeoArea
import blue.deniz.vantage.core.EventCollection
import blue.deniz.vantage.core.EventQuery
import blue.deniz.vantage.core.ResolvedEvent
import blue.deniz.vantage.core.coordinates
import directory.evnt.Component

const val MAP_STYLE_URL = "https://tiles.openfreemap.org/styles/dark"

private const val SMALLEST_FITTED_SPAN_DEGREES = 0.02
const val MAP_EVENT_LIMIT = 500
const val MY_LOCATION_ZOOM = 11.0
const val SPOT_PICKER_ZOOM = 16.5
const val SAME_SPOT_DEGREES = 1e-6

object SpotPinShape {
    const val WIDTH = 34
    const val HEIGHT = 44
    const val TIP_Y = 43
    const val PATH = "M17 43c-1-6-15-15-15-26a15 15 0 0 1 30 0c0 11-14 20-15 26z"
    const val DOT_CENTRE = 17f
    const val DOT_RADIUS = 5.5f
    const val OUTLINE = 2f
}

data class MapCamera(val latitude: Double, val longitude: Double, val zoom: Double)

class MapPin(val markerId: String, val latitude: Double, val longitude: Double, val event: ResolvedEvent)

private fun ResolvedEvent.markerId(): String? = id ?: (source as? EventSource.Remote)?.location

fun pinsOf(events: List<ResolvedEvent>): List<MapPin> = events.mapNotNull { event ->
    val markerId = event.markerId() ?: return@mapNotNull null
    val coordinates = event.data?.venue?.coordinates ?: return@mapNotNull null
    MapPin(markerId, coordinates.latitude, coordinates.longitude, event)
}.distinctBy { it.markerId }

fun areaAround(pins: List<MapPin>): GeoArea? {
    if (pins.isEmpty()) return null
    val halfHeight = maxOf(pins.maxOf { it.latitude } - pins.minOf { it.latitude }, SMALLEST_FITTED_SPAN_DEGREES) / 2
    val halfWidth = maxOf(pins.maxOf { it.longitude } - pins.minOf { it.longitude }, SMALLEST_FITTED_SPAN_DEGREES) / 2
    val centerLatitude = (pins.maxOf { it.latitude } + pins.minOf { it.latitude }) / 2
    val centerLongitude = (pins.maxOf { it.longitude } + pins.minOf { it.longitude }) / 2
    return GeoArea(
        south = centerLatitude - halfHeight,
        west = centerLongitude - halfWidth,
        north = centerLatitude + halfHeight,
        east = centerLongitude + halfWidth,
    )
}

@Composable
expect fun SpotPickerMap(
    center: Component.Coordinates,
    onMoved: (Component.Coordinates) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
)

@Composable
expect fun EventMap(
    events: EventCollection,
    query: EventQuery,
    start: MapCamera?,
    onOpenEvent: (ResolvedEvent) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
)
