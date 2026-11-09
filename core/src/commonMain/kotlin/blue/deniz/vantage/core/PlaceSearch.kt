package blue.deniz.vantage.core

import directory.evnt.Component
import directory.evnt.Venue
import io.ktor.client.HttpClient
import io.ktor.client.statement.bodyAsText
import io.ktor.http.URLBuilder
import io.ktor.http.path
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
private const val ADDRESS_CANDIDATE_LIMIT = 5
private const val NEARBY_RESULT_LIMIT = 15
private const val NEARBY_RADIUS_KILOMETRES = 0.3
private val NEARBY_OSM_KEYS = listOf("amenity", "leisure", "tourism", "shop")
private val ADDRESS_FEATURE_TYPES = setOf("house", "street")
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

data class Address(
    val text: String,
    val countryCode: String?,
    val placeId: Component.PlaceId?,
    val coordinates: Component.Coordinates,
) {
    fun toVenue() = Venue(address = text, countryCode = countryCode, components = listOfNotNull(placeId, coordinates))
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
        return featuresAt(url).mapNotNull { it.toNamedPlace() }.distinctBy { Triple(it.name, it.address, it.countryCode) }
    }

    suspend fun addressAt(at: Component.Coordinates): Address? {
        val feature = reverse(at, listOf("limit" to ADDRESS_CANDIDATE_LIMIT.toString())).firstOrNull { it.text("type") in ADDRESS_FEATURE_TYPES }
            ?: return null
        val street = feature.street ?: feature.text("name") ?: return null
        return feature.toAddress(listOfNotNull(street, feature.locality).joinToString(", "), at)
    }

    suspend fun placesNear(at: Component.Coordinates): List<Place> {
        val parameters = listOf("limit" to NEARBY_RESULT_LIMIT.toString(), "radius" to NEARBY_RADIUS_KILOMETRES.toString()) +
            NEARBY_OSM_KEYS.map { "osm_tag" to it }
        return reverse(at, parameters).filter { it.text("name") != null }.mapNotNull { it.toNamedPlace() }
            .distinctBy { Triple(it.name, it.address, it.countryCode) }
    }

    private suspend fun reverse(at: Component.Coordinates, extra: List<Pair<String, String>>): List<PhotonFeature> {
        val url = URLBuilder(endpoint).apply {
            path("reverse")
            parameters.append("lat", at.latitude.toString())
            parameters.append("lon", at.longitude.toString())
            for ((name, value) in extra) parameters.append(name, value)
        }.buildString()
        return featuresAt(url)
    }

    private suspend fun featuresAt(url: String): List<PhotonFeature> {
        val results = parseJsonObject(client.getOk(url).bodyAsText(), "The place search")
        return (results["features"] as? JsonArray).orEmpty().filterIsInstance<JsonObject>().mapNotNull(::photonFeatureOf)
    }
}

private fun JsonElement?.primitiveTextOrNull() = (this as? JsonPrimitive)?.takeUnless { it is JsonNull }?.content

private class PhotonFeature(private val properties: JsonObject, private val geometry: JsonObject?) {
    fun text(key: String) = properties[key].stringOrNull()?.takeIf { it.isNotBlank() }

    val street get() = listOfNotNull(text("street"), text("housenumber")).joinToString(" ").ifEmpty { null }

    val locality get() = text("city") ?: text("town") ?: text("village") ?: text("county")

    private val position: Component.Coordinates?
        get() {
            val position = geometry?.get("coordinates") as? JsonArray
            val longitude = (position?.getOrNull(0) as? JsonPrimitive)?.doubleOrNull ?: return null
            val latitude = (position.getOrNull(1) as? JsonPrimitive)?.doubleOrNull ?: return null
            return Component.Coordinates(latitude, longitude)
        }

    private val placeId: Component.PlaceId?
        get() {
            val elementType = OSM_ELEMENT_TYPES[text("osm_type")] ?: return null
            val elementId = properties["osm_id"].primitiveTextOrNull() ?: return null
            return Component.PlaceId("osm", "$elementType/$elementId")
        }

    fun toPlace(name: String, address: String?, coordinates: Component.Coordinates? = position) =
        Place(name = name, address = address, countryCode = text("countrycode")?.uppercase(), placeId = placeId, coordinates = coordinates)

    fun toAddress(text: String, coordinates: Component.Coordinates) =
        Address(text = text, countryCode = text("countrycode")?.uppercase(), placeId = placeId, coordinates = coordinates)

    fun toNamedPlace(): Place? {
        val street = street
        val locality = locality
        val name = text("name") ?: street ?: locality ?: return null
        return toPlace(name, listOfNotNull(street, locality).filter { it != name }.joinToString(", ").ifEmpty { null })
    }
}

private fun photonFeatureOf(feature: JsonObject): PhotonFeature? =
    (feature["properties"] as? JsonObject)?.let { PhotonFeature(it, feature["geometry"] as? JsonObject) }
