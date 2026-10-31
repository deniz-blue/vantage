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
import kotlin.math.abs

const val MAP_STYLE_URL = "https://tiles.openfreemap.org/styles/dark"

private const val SMALLEST_FITTED_SPAN_DEGREES = 0.02
const val MAP_EVENT_LIMIT = 500
private const val SAME_CAMERA_DEGREES = 0.0001
private const val SAME_CAMERA_ZOOM = 0.01

data class MapCamera(val latitude: Double, val longitude: Double, val zoom: Double) {
    val storedText get() = "$latitude,$longitude,$zoom"

    fun isAt(other: MapCamera) =
        abs(latitude - other.latitude) < SAME_CAMERA_DEGREES && abs(longitude - other.longitude) < SAME_CAMERA_DEGREES && abs(zoom - other.zoom) < SAME_CAMERA_ZOOM

    companion object {
        fun fromStoredText(text: String): MapCamera? {
            val (latitude, longitude, zoom) = text.split(',').map { it.toDoubleOrNull() ?: return null }.takeIf { it.size == 3 } ?: return null
            return MapCamera(latitude, longitude, zoom)
        }
    }
}

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
expect fun EventMap(
    events: EventCollection,
    query: EventQuery,
    start: MapCamera?,
    onCameraSettled: (MapCamera) -> Unit,
    onOpenEvent: (ResolvedEvent) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
)
