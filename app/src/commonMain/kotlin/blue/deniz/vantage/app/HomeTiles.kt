package blue.deniz.vantage.app

import blue.deniz.vantage.app.ui.displayName
import blue.deniz.vantage.core.EventFeed
import blue.deniz.vantage.core.coordinates
import directory.evnt.Component
import directory.evnt.OpenEvnt
import directory.evnt.Venue
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.put
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

const val DEFAULT_TILE_EVENT_COUNT = 5

sealed interface HomeTile {
    val id: String

    data class UpcomingEvents(
        override val id: String,
        val count: Int = DEFAULT_TILE_EVENT_COUNT,
        val tagIds: Set<String> = emptySet(),
        val layout: EventCardStyle = EventCardStyle.Rows,
    ) : HomeTile

    data class Weather(override val id: String, val place: NamedPlace? = null) : HomeTile

    data class AtAGlance(override val id: String) : HomeTile

    data class Feed(
        override val id: String,
        val feed: EventFeed,
        val count: Int = DEFAULT_TILE_EVENT_COUNT,
        val layout: EventCardStyle = EventCardStyle.Rows,
    ) : HomeTile

    data class AndroidApp(override val id: String) : HomeTile
}

data class NamedPlace(val name: String, val latitude: Double, val longitude: Double) {
    val coordinates get() = Component.Coordinates(latitude, longitude)
}

fun Venue.namedPlace(preferredLanguages: List<String>): NamedPlace? =
    coordinates?.let { NamedPlace(displayName(preferredLanguages) ?: address ?: "Unnamed Place", it.latitude, it.longitude) }

fun namedPlaceToJson(place: NamedPlace): String = buildJsonObject {
    put("name", place.name)
    put("latitude", place.latitude)
    put("longitude", place.longitude)
}.toString()

fun namedPlaceOfJson(text: String): NamedPlace? {
    val place = runCatching { Json.parseToJsonElement(text) as? JsonObject }.getOrNull() ?: return null
    val name = (place["name"] as? JsonPrimitive)?.takeIf { it.isString }?.content ?: return null
    val latitude = (place["latitude"] as? JsonPrimitive)?.doubleOrNull ?: return null
    val longitude = (place["longitude"] as? JsonPrimitive)?.doubleOrNull ?: return null
    return NamedPlace(name, latitude, longitude)
}

@OptIn(ExperimentalUuidApi::class)
fun newHomeTileId() = Uuid.random().toString()

val DEFAULT_HOME_TILES: List<HomeTile> = listOfNotNull(
    HomeTile.AndroidApp("android-app").takeIf { offersAndroidAppTile },
    HomeTile.AtAGlance("at-a-glance"),
    HomeTile.UpcomingEvents("upcoming", layout = EventCardStyle.Posters),
)

val offersAndroidAppTile: Boolean get() = runsInBrowser && !runsInAndroidBrowser

fun homeTilesToJson(tiles: List<HomeTile>): String = buildJsonArray {
    for (tile in tiles) {
        add(buildJsonObject {
            put("id", tile.id)
            when (tile) {
                is HomeTile.UpcomingEvents -> {
                    put("type", "upcoming")
                    put("count", tile.count)
                    if (tile.tagIds.isNotEmpty()) put("tags", JsonArray(tile.tagIds.map { JsonPrimitive(it) }))
                    put("layout", tile.layout.storedName)
                }
                is HomeTile.Weather -> {
                    put("type", "weather")
                    tile.place?.let { place ->
                        put("place", place.name)
                        put("latitude", place.latitude)
                        put("longitude", place.longitude)
                    }
                }
                is HomeTile.AtAGlance -> put("type", "atAGlance")
                is HomeTile.AndroidApp -> put("type", "androidApp")
                is HomeTile.Feed -> {
                    put("type", "feed")
                    put("count", tile.count)
                    put("layout", tile.layout.storedName)
                    when (val feed = tile.feed) {
                        is EventFeed.OpenEvntIndex -> put("openEvntIndex", feed.url)
                        is EventFeed.AtRepository -> {
                            put("atRepository", feed.repo)
                            put("atCollection", feed.collection)
                        }
                    }
                }
            }
        })
    }
}.toString()

fun homeTilesOfJson(text: String): List<HomeTile>? {
    val array = runCatching { Json.parseToJsonElement(text) as? JsonArray }.getOrNull() ?: return null
    return array.filterIsInstance<JsonObject>().mapNotNull(::homeTileOfJson)
}

private fun homeTileOfJson(tile: JsonObject): HomeTile? {
    fun text(key: String) = (tile[key] as? JsonPrimitive)?.takeIf { it.isString }?.content
    fun number(key: String) = (tile[key] as? JsonPrimitive)?.doubleOrNull
    fun texts(key: String) = (tile[key] as? JsonArray).orEmpty().mapNotNull { element -> (element as? JsonPrimitive)?.takeIf { it.isString }?.content }
    val id = text("id") ?: return null
    val count = (tile["count"] as? JsonPrimitive)?.intOrNull ?: DEFAULT_TILE_EVENT_COUNT
    val layout = EventCardStyle.entries.find { it.storedName == text("layout") } ?: EventCardStyle.Rows
    return when (text("type")) {
        "upcoming" -> HomeTile.UpcomingEvents(id, count, texts("tags").toSet(), layout)
        "weather" -> {
            val name = text("place")
            val latitude = number("latitude")
            val longitude = number("longitude")
            HomeTile.Weather(id, if (name != null && latitude != null && longitude != null) NamedPlace(name, latitude, longitude) else null)
        }
        "atAGlance", "periods" -> HomeTile.AtAGlance(id)
        "androidApp" -> HomeTile.AndroidApp(id)
        "feed" -> {
            val openEvntIndex = text("openEvntIndex")
            val atRepository = text("atRepository")
            val feed = when {
                openEvntIndex != null -> EventFeed.OpenEvntIndex(openEvntIndex)
                atRepository != null -> EventFeed.AtRepository(atRepository, text("atCollection") ?: OpenEvnt.AT_RECORD_TYPE)
                else -> return null
            }
            HomeTile.Feed(id, feed, count, layout)
        }
        else -> null
    }
}
