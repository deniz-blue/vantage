package blue.deniz.vantage.core

import directory.evnt.Component
import directory.evnt.Venue
import io.ktor.client.HttpClient
import io.ktor.client.statement.bodyAsText
import io.ktor.http.URLBuilder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days

const val PHOTON_ENDPOINT = "https://photon.komoot.io/api/"
private const val PLACE_RESULT_LIMIT = 6
private val TIME_ZONE_PLACE_FRESH_FOR = 1.days
private val TIME_ZONE_AREAS_NAMED_BY_CITY = setOf("Africa", "America", "Antarctica", "Arctic", "Asia", "Atlantic", "Australia", "Europe", "Indian", "Pacific")

private val OSM_ELEMENT_TYPES = mapOf("N" to "node", "W" to "way", "R" to "relation")

data class Place(
    val name: String,
    val address: String?,
    val countryCode: String?,
    val placeId: Component.PlaceId?,
    val coordinates: Component.Coordinates?,
) {
    fun toVenue(language: String) = Venue(
        name = mapOf(language to name),
        address = address,
        countryCode = countryCode,
        components = listOfNotNull(placeId, coordinates).ifEmpty { null },
    )
}

fun cityNameOfTimeZone(zoneId: String): String? {
    val parts = zoneId.split('/')
    if (parts.size < 2 || parts.first() !in TIME_ZONE_AREAS_NAMED_BY_CITY) return null
    return parts.last().replace('_', ' ')
}

class PlaceSearch(private val client: HttpClient, private val endpoint: String = PHOTON_ENDPOINT, clock: Clock = Clock.System) {
    private val timeZoneCities = ResultCache<String, Place>(TIME_ZONE_PLACE_FRESH_FOR, clock)

    fun lastCityOfTimeZone(zoneId: String): Place? = timeZoneCities.last(zoneId)

    suspend fun cityOfTimeZone(zoneId: String): Place? {
        timeZoneCities.fresh(zoneId)?.let { return it }
        val name = cityNameOfTimeZone(zoneId) ?: return null
        val city = search(name, citiesOnly = true).firstOrNull { it.coordinates != null } ?: return null
        timeZoneCities.store(zoneId, city)
        return city
    }

    suspend fun search(query: String, near: Component.Coordinates? = null, citiesOnly: Boolean = false): List<Place> {
        val url = URLBuilder(endpoint).apply {
            parameters.append("q", query)
            parameters.append("limit", PLACE_RESULT_LIMIT.toString())
            if (citiesOnly) parameters.append("layer", "city")
            if (near != null) {
                parameters.append("lat", near.latitude.toString())
                parameters.append("lon", near.longitude.toString())
            }
        }.buildString()
        val results = parseJsonObject(client.getOk(url).bodyAsText(), "The place search")
        return (results["features"] as? JsonArray).orEmpty().filterIsInstance<JsonObject>().mapNotNull(::placeOfPhotonFeature)
            .distinctBy { Triple(it.name, it.address, it.countryCode) }
    }
}

private fun JsonElement?.primitiveTextOrNull() = (this as? JsonPrimitive)?.takeUnless { it is JsonNull }?.content

private fun placeOfPhotonFeature(feature: JsonObject): Place? {
    val properties = feature["properties"] as? JsonObject ?: return null
    fun text(key: String) = properties[key].stringOrNull()?.takeIf { it.isNotBlank() }
    val street = listOfNotNull(text("street"), text("housenumber")).joinToString(" ").ifEmpty { null }
    val locality = text("city") ?: text("town") ?: text("village") ?: text("county")
    val name = text("name") ?: street ?: locality ?: return null
    val address = listOfNotNull(street, locality).filter { it != name }.joinToString(", ").ifEmpty { null }
    val elementType = OSM_ELEMENT_TYPES[text("osm_type")]
    val elementId = properties["osm_id"].primitiveTextOrNull()
    val position = (feature["geometry"] as? JsonObject)?.get("coordinates") as? JsonArray
    val longitude = (position?.getOrNull(0) as? JsonPrimitive)?.doubleOrNull
    val latitude = (position?.getOrNull(1) as? JsonPrimitive)?.doubleOrNull
    return Place(
        name = name,
        address = address,
        countryCode = text("countrycode")?.uppercase(),
        placeId = if (elementType != null && elementId != null) Component.PlaceId("osm", "$elementType/$elementId") else null,
        coordinates = if (latitude != null && longitude != null) Component.Coordinates(latitude, longitude) else null,
    )
}
