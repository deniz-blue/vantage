package blue.deniz.vantage.app.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.viewinterop.WebElementView
import blue.deniz.vantage.app.rememberAcrossNavigation
import blue.deniz.vantage.core.GeoArea
import blue.deniz.vantage.core.EventCollection
import blue.deniz.vantage.core.EventQuery
import blue.deniz.vantage.core.ResolvedEvent
import kotlinx.browser.document
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import org.w3c.dom.HTMLDivElement
import directory.evnt.Component
import kotlin.math.abs

private const val PIN_SOURCE_ID = "event-pins"
private const val PIN_LAYER_ID = "event-pins"
private const val FIT_PADDING_PX = 48
private const val MAP_ATTRIBUTION_HTML =
    """<a href="https://openfreemap.org" target="_blank">OpenFreeMap</a> © <a href="https://www.openmaptiles.org/" target="_blank">OpenMapTiles</a> Data from <a href="https://www.openstreetmap.org/copyright" target="_blank">OpenStreetMap</a>"""

private fun mapOptions(container: HTMLDivElement, style: String): JsAny =
    js("({ container: container, style: style, attributionControl: false, dragRotate: false, pitchWithRotate: false })")

private fun bundledMapLibreWorkerUrl(): String =
    js("new URL('../../../node_modules/maplibre-gl/dist/maplibre-gl-worker.mjs', import.meta.url).href")

private fun addMapLibreBaseCss(): Unit = js("""{
    if (document.getElementById('maplibre-base-css')) return;
    const style = document.createElement('style');
    style.id = 'maplibre-base-css';
    style.textContent = '.maplibregl-map{overflow:hidden;position:relative;-webkit-tap-highlight-color:transparent}'
        + '.maplibregl-canvas{position:absolute;left:0;top:0}'
        + '.maplibregl-canvas-container.maplibregl-interactive{cursor:grab}'
        + '.maplibregl-canvas-container.maplibregl-interactive:active{cursor:grabbing}'
        + '.vantage-map-attribution{position:absolute;right:8px;bottom:8px;z-index:1;display:flex;align-items:center;max-width:calc(100% - 16px);box-sizing:border-box;border-radius:14px;font:11px sans-serif;color:#ddd;background:rgba(30,28,36,.72);backdrop-filter:blur(12px);-webkit-backdrop-filter:blur(12px);border:1px solid rgba(255,255,255,.14)}'
        + '.vantage-map-attribution a{color:inherit}'
        + '.vantage-map-attribution-text{display:none;padding:0 2px 0 10px}'
        + '.vantage-map-attribution.open .vantage-map-attribution-text{display:inline}'
        + '.vantage-map-attribution button{all:unset;flex:none;width:26px;height:26px;display:flex;align-items:center;justify-content:center;font:italic bold 14px serif;cursor:pointer}';
    document.head.appendChild(style);
}""")

private fun addAttributionButton(container: HTMLDivElement, html: String): Unit = js("""{
    const attribution = document.createElement('div');
    attribution.className = 'vantage-map-attribution';
    const text = document.createElement('span');
    text.className = 'vantage-map-attribution-text';
    text.innerHTML = html;
    const button = document.createElement('button');
    button.type = 'button';
    button.textContent = 'i';
    button.title = 'Map Credits';
    button.addEventListener('click', () => attribution.classList.toggle('open'));
    attribution.append(text, button);
    container.appendChild(attribution);
}""")

private fun addPinLayer(map: MapLibreMap, sourceId: String, layerId: String, color: String): Unit = js("""{
    map.addSource(sourceId, { type: 'geojson', data: { type: 'FeatureCollection', features: [] } });
    map.addLayer({ id: layerId, type: 'circle', source: sourceId, paint: {
        'circle-color': color, 'circle-radius': 7, 'circle-stroke-color': '#ffffff', 'circle-stroke-width': 2 } });
    map.on('mouseenter', layerId, () => { map.getCanvas().style.cursor = 'pointer'; });
    map.on('mouseleave', layerId, () => { map.getCanvas().style.cursor = ''; });
}""")

private fun setPinData(map: MapLibreMap, sourceId: String, geoJson: String): Unit =
    js("{ const source = map.getSource(sourceId); if (source) source.setData(JSON.parse(geoJson)); }")

private fun fitMapTo(map: MapLibreMap, west: Double, south: Double, east: Double, north: Double, padding: Int): Unit =
    js("map.fitBounds([[west, south], [east, north]], { padding: padding, animate: false })")

private fun jumpMapTo(map: MapLibreMap, latitude: Double, longitude: Double, zoom: Double): Unit =
    js("map.jumpTo({ center: [longitude, latitude], zoom: zoom })")

private fun visibleSouth(map: MapLibreMap): Double = js("map.getBounds().getSouth()")
private fun visibleWest(map: MapLibreMap): Double = js("Math.max(-180, map.getBounds().getWest())")
private fun visibleNorth(map: MapLibreMap): Double = js("map.getBounds().getNorth()")
private fun visibleEast(map: MapLibreMap): Double = js("Math.min(180, map.getBounds().getEast())")

private fun clickedMarkerId(event: JsAny): String? =
    js("(event.features && event.features.length) ? String(event.features[0].properties.markerId) : null")

private fun visibleAreaOf(map: MapLibreMap) =
    GeoArea(south = visibleSouth(map), west = visibleWest(map), north = visibleNorth(map), east = visibleEast(map))

private fun geoJsonOf(pins: List<MapPin>): String = buildJsonObject {
    put("type", "FeatureCollection")
    putJsonArray("features") {
        for (pin in pins) add(buildJsonObject {
            put("type", "Feature")
            putJsonObject("geometry") {
                put("type", "Point")
                putJsonArray("coordinates") { add(pin.longitude); add(pin.latitude) }
            }
            putJsonObject("properties") { put("markerId", pin.markerId) }
        })
    }
}.toString()

private fun cssColor(argb: Int) = "#" + (argb and 0xFFFFFF).toString(16).padStart(6, '0')

private fun spotPinSvg(fill: String, dot: String) =
    """<svg viewBox="0 0 ${SpotPinShape.WIDTH} ${SpotPinShape.HEIGHT}" width="${SpotPinShape.WIDTH}" height="${SpotPinShape.HEIGHT}">""" +
        """<path d="${SpotPinShape.PATH}" fill="$fill" stroke="#ffffff" stroke-width="${SpotPinShape.OUTLINE}"/>""" +
        """<circle cx="${SpotPinShape.DOT_CENTRE}" cy="${SpotPinShape.DOT_CENTRE}" r="${SpotPinShape.DOT_RADIUS}" fill="$dot"/></svg>"""

private fun addSpotPin(container: HTMLDivElement, svg: String, tipY: Int): Unit = js("""{
    const dot = document.createElement('div');
    dot.style.cssText = 'position:absolute;left:50%;top:50%;z-index:1;width:6px;height:6px;border-radius:3px;background:rgba(0,0,0,.6);transform:translate(-50%,-50%);pointer-events:none';
    const pin = document.createElement('div');
    pin.style.cssText = 'position:absolute;left:50%;top:50%;z-index:1;line-height:0;transform:translate(-50%,-' + tipY + 'px);pointer-events:none';
    pin.innerHTML = svg;
    container.append(dot, pin);
}""")

private fun centreLatitude(map: MapLibreMap): Double = js("map.getCenter().lat")
private fun centreLongitude(map: MapLibreMap): Double = js("map.getCenter().lng")
private fun centreMapOn(map: MapLibreMap, latitude: Double, longitude: Double): Unit = js("map.jumpTo({ center: [longitude, latitude] })")

@OptIn(ExperimentalComposeUiApi::class)
@Composable
actual fun SpotPickerMap(center: Component.Coordinates, onMoved: (Component.Coordinates) -> Unit, modifier: Modifier) {
    val currentOnMoved by rememberUpdatedState(onMoved)
    val pinSvg = spotPinSvg(cssColor(MaterialTheme.colorScheme.primary.toArgb()), cssColor(MaterialTheme.colorScheme.surface.toArgb()))
    var map by remember { mutableStateOf<MapLibreMap?>(null) }
    val container = remember {
        addMapLibreBaseCss()
        setWorkerUrl(bundledMapLibreWorkerUrl())
        val created = document.createElement("div") as HTMLDivElement
        created.style.width = "100%"
        created.style.height = "100%"
        created.style.borderRadius = "${Radius.md.value}px"
        addSpotPin(created, pinSvg, SpotPinShape.TIP_Y)
        addAttributionButton(created, MAP_ATTRIBUTION_HTML)
        created
    }
    DisposableEffect(container) {
        val created = MapLibreMap(mapOptions(container, MAP_STYLE_URL))
        jumpMapTo(created, center.latitude, center.longitude, SPOT_PICKER_ZOOM)
        created.on("moveend") { currentOnMoved(Component.Coordinates(centreLatitude(created), centreLongitude(created))) }
        map = created
        onDispose { created.remove() }
    }
    LaunchedEffect(map, center) {
        val shown = map ?: return@LaunchedEffect
        if (abs(centreLatitude(shown) - center.latitude) > SAME_SPOT_DEGREES || abs(centreLongitude(shown) - center.longitude) > SAME_SPOT_DEGREES) {
            centreMapOn(shown, center.latitude, center.longitude)
        }
    }
    WebElementView(factory = { container }, modifier = modifier)
}

@OptIn(ExperimentalComposeUiApi::class, ExperimentalCoroutinesApi::class)
@Composable
actual fun EventMap(
    events: EventCollection,
    query: EventQuery,
    start: MapCamera?,
    onOpenEvent: (ResolvedEvent) -> Unit,
    modifier: Modifier,
    contentPadding: PaddingValues,
) {
    val bottomSheetOpen by rememberAnyBottomSheetOpen()
    var pins by rememberAcrossNavigation("map pins") { mutableStateOf(emptyList<MapPin>()) }
    val currentPins by rememberUpdatedState(pins)
    val currentOnOpenEvent by rememberUpdatedState(onOpenEvent)
    val pinColor = cssColor(MaterialTheme.colorScheme.primary.toArgb())
    var map by remember { mutableStateOf<MapLibreMap?>(null) }
    var styleLoaded by remember { mutableStateOf(false) }
    val visibleArea = remember { MutableStateFlow<GeoArea?>(null) }

    LaunchedEffect(events, query) {
        visibleArea.filterNotNull()
            .distinctUntilChanged()
            .flatMapLatest { area -> events.list(query.copy(area = area), MAP_EVENT_LIMIT) }
            .collect { listing -> pins = pinsOf(listing.events) }
    }

    var fittedToPins by remember { mutableStateOf(start != null) }
    LaunchedEffect(map, styleLoaded, pins) {
        val shown = map?.takeIf { styleLoaded } ?: return@LaunchedEffect
        setPinData(shown, PIN_SOURCE_ID, geoJsonOf(pins))
        if (fittedToPins) return@LaunchedEffect
        val area = areaAround(pins) ?: return@LaunchedEffect
        fittedToPins = true
        fitMapTo(shown, area.west, area.south, area.east, area.north, FIT_PADDING_PX)
    }

    val container = remember {
        addMapLibreBaseCss()
        setWorkerUrl(bundledMapLibreWorkerUrl())
        val created = document.createElement("div") as HTMLDivElement
        created.style.width = "100%"
        created.style.height = "100%"
        addAttributionButton(created, MAP_ATTRIBUTION_HTML)
        created
    }

    DisposableEffect(container) {
        val created = MapLibreMap(mapOptions(container, MAP_STYLE_URL))
        start?.let { jumpMapTo(created, it.latitude, it.longitude, it.zoom) }
        created.on("load") {
            addPinLayer(created, PIN_SOURCE_ID, PIN_LAYER_ID, pinColor)
            styleLoaded = true
            visibleArea.value = visibleAreaOf(created)
        }
        created.on("moveend") {
            visibleArea.value = visibleAreaOf(created)
        }
        created.on("click", PIN_LAYER_ID) { event ->
            val markerId = clickedMarkerId(event)
            currentPins.firstOrNull { it.markerId == markerId }?.let { currentOnOpenEvent(it.event) }
        }
        map = created
        onDispose { created.remove() }
    }

    if (!bottomSheetOpen) {
        WebElementView(factory = { container }, modifier = modifier.padding(contentPadding))
    }
}
