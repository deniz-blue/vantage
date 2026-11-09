package blue.deniz.vantage.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.PathParser
import directory.evnt.Component
import kotlin.math.abs
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import blue.deniz.vantage.app.rememberAcrossNavigation
import blue.deniz.vantage.core.GeoArea
import blue.deniz.vantage.core.EventCollection
import blue.deniz.vantage.core.EventQuery
import blue.deniz.vantage.core.ResolvedEvent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.maplibre.compose.camera.CameraFit
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.expressions.dsl.const
import org.maplibre.compose.interaction.ClickResult
import org.maplibre.compose.layers.CircleLayer
import org.maplibre.compose.map.MaplibreMap
import org.maplibre.compose.map.rememberMapState
import org.maplibre.compose.overlay.DisappearingCompassButton
import org.maplibre.compose.overlay.DisappearingScaleBar
import org.maplibre.compose.overlay.ExpandingAttributionButton
import org.maplibre.compose.overlay.MapOverlay
import org.maplibre.compose.sources.GeoJsonData
import org.maplibre.compose.sources.rememberGeoJsonSource
import org.maplibre.compose.style.BaseStyle
import org.maplibre.compose.util.DpPadding
import org.maplibre.compose.util.VisibleBounds
import org.maplibre.spatialk.geojson.BoundingBox
import org.maplibre.spatialk.geojson.Feature
import org.maplibre.spatialk.geojson.FeatureCollection
import org.maplibre.spatialk.geojson.Point
import org.maplibre.spatialk.geojson.Position

private const val MARKER_ID_PROPERTY = "markerId"
private val SPOT_DOT_SIZE = 6.dp
private const val SPOT_DOT_ALPHA = 0.6f
private const val PIN_LAYER_ID = "event-pins"
private val PIN_RADIUS = 7.dp
private val PIN_OUTLINE = 2.dp
private val FIT_PADDING = 48.dp

private fun VisibleBounds.toGeoArea() = GeoArea(
    south = southwest.latitude,
    west = southwest.longitude.coerceIn(-180.0, 180.0),
    north = northeast.latitude,
    east = northeast.longitude.coerceIn(-180.0, 180.0),
)

private fun featuresOf(pins: List<MapPin>) = FeatureCollection(
    pins.map { pin -> Feature(Point(Position(pin.longitude, pin.latitude)), buildJsonObject { put(MARKER_ID_PROPERTY, pin.markerId) }) },
)

@OptIn(ExperimentalCoroutinesApi::class)
@Composable
actual fun EventMap(
    events: EventCollection,
    query: EventQuery,
    start: MapCamera?,
    onOpenEvent: (ResolvedEvent) -> Unit,
    modifier: Modifier,
    contentPadding: PaddingValues,
) {
    var pins by rememberAcrossNavigation("map pins") { mutableStateOf(emptyList<MapPin>()) }
    val currentOnOpenEvent by rememberUpdatedState(onOpenEvent)
    val features = remember(pins) { featuresOf(pins) }
    val pinColor = MaterialTheme.colorScheme.primary

    val mapState = rememberMapState(
        baseStyle = BaseStyle.Uri(MAP_STYLE_URL),
        initialCameraPosition = start?.let { CameraPosition(Position(it.longitude, it.latitude), zoom = it.zoom) } ?: CameraPosition(),
    ) {
        val pinSource = rememberGeoJsonSource(GeoJsonData.Features(features))
        CircleLayer(
            id = PIN_LAYER_ID,
            source = pinSource,
            color = const(pinColor),
            radius = const(PIN_RADIUS),
            strokeColor = const(Color.White),
            strokeWidth = const(PIN_OUTLINE),
            onClick = { clicked ->
                val markerId = clicked.firstNotNullOfOrNull { it.properties?.get(MARKER_ID_PROPERTY)?.jsonPrimitive?.contentOrNull }
                pins.firstOrNull { it.markerId == markerId }?.let { currentOnOpenEvent(it.event) }
                ClickResult.Consume
            },
        )
    }

    LaunchedEffect(mapState, events, query) {
        snapshotFlow { mapState.viewport?.takeIf { !mapState.isCameraMoving }?.visibleBounds }
            .filterNotNull()
            .map { it.toGeoArea() }
            .distinctUntilChanged()
            .flatMapLatest { area -> events.list(query.copy(area = area), MAP_EVENT_LIMIT) }
            .collect { listing -> pins = pinsOf(listing.events) }
    }

    var fittedToPins by remember(mapState) { mutableStateOf(start != null) }
    LaunchedEffect(mapState, pins) {
        if (fittedToPins) return@LaunchedEffect
        val area = areaAround(pins) ?: return@LaunchedEffect
        fittedToPins = true
        mapState.fitCameraToBounds(
            BoundingBox(west = area.west, south = area.south, east = area.east, north = area.north),
            CameraFit(fitPadding = DpPadding(FIT_PADDING, FIT_PADDING, FIT_PADDING, FIT_PADDING)),
        )
    }

    MaplibreMap(modifier, state = mapState) {
        Box(Modifier.fillMaxSize().padding(contentPadding).padding(MapOverlay.Spacing)) {
            DisappearingScaleBar(
                metersPerDp = { mapState.viewport?.metersPerDpAtCenter ?: 0.0 },
                zoom = { mapState.cameraPosition.zoom },
                modifier = Modifier.align(Alignment.TopStart),
            )
            DisappearingCompassButton(Modifier.align(Alignment.TopEnd))
            ExpandingAttributionButton(Modifier.align(Alignment.BottomEnd))
        }
    }
}

@Composable
private fun SpotPin(modifier: Modifier = Modifier) {
    val fill = MaterialTheme.colorScheme.primary
    val dot = MaterialTheme.colorScheme.surface
    val pin = remember { PathParser().parsePathString(SpotPinShape.PATH).toPath() }
    Canvas(modifier.size(SpotPinShape.WIDTH.dp, SpotPinShape.HEIGHT.dp)) {
        scale(density, pivot = Offset.Zero) {
            drawPath(pin, fill)
            drawPath(pin, Color.White, style = Stroke(SpotPinShape.OUTLINE))
            drawCircle(dot, radius = SpotPinShape.DOT_RADIUS, center = Offset(SpotPinShape.DOT_CENTRE, SpotPinShape.DOT_CENTRE))
        }
    }
}

@Composable
actual fun SpotPickerMap(center: Component.Coordinates, onMoved: (Component.Coordinates) -> Unit, modifier: Modifier) {
    val currentOnMoved by rememberUpdatedState(onMoved)
    val mapState = rememberMapState(
        baseStyle = BaseStyle.Uri(MAP_STYLE_URL),
        initialCameraPosition = CameraPosition(Position(center.longitude, center.latitude), zoom = SPOT_PICKER_ZOOM),
    )
    LaunchedEffect(mapState) {
        snapshotFlow { mapState.cameraPosition.center.takeIf { !mapState.isCameraMoving } }
            .filterNotNull()
            .distinctUntilChanged()
            .collect { currentOnMoved(Component.Coordinates(it.latitude, it.longitude)) }
    }
    LaunchedEffect(mapState, center) {
        val shown = mapState.cameraPosition.center
        if (abs(shown.latitude - center.latitude) > SAME_SPOT_DEGREES || abs(shown.longitude - center.longitude) > SAME_SPOT_DEGREES) {
            mapState.setCameraPosition(mapState.cameraPosition.copy(center = Position(center.longitude, center.latitude)))
        }
    }
    MaplibreMap(modifier, state = mapState) {
        Box(Modifier.fillMaxSize()) {
            Box(Modifier.align(Alignment.Center).size(SPOT_DOT_SIZE).background(Color.Black.copy(alpha = SPOT_DOT_ALPHA), CircleShape))
            SpotPin(Modifier.align(Alignment.Center).offset(y = (SpotPinShape.HEIGHT / 2 - SpotPinShape.TIP_Y).dp))
            ExpandingAttributionButton(Modifier.align(Alignment.BottomEnd).padding(MapOverlay.Spacing))
        }
    }
}
