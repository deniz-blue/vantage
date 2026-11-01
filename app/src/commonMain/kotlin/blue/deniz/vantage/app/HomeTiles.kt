package blue.deniz.vantage.app

import blue.deniz.vantage.core.EventFeed
import directory.evnt.OpenEvnt
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

    data class Weather(override val id: String, val place: WeatherPlace? = null) : HomeTile

    data class Periods(override val id: String) : HomeTile

    data class Feed(
        override val id: String,
        val feed: EventFeed,
        val count: Int = DEFAULT_TILE_EVENT_COUNT,
        val layout: EventCardStyle = EventCardStyle.Rows,
    ) : HomeTile

    data class AndroidApp(override val id: String) : HomeTile
}

data class WeatherPlace(val name: String, val latitude: Double, val longitude: Double)

@OptIn(ExperimentalUuidApi::class)
fun newHomeTileId() = Uuid.random().toString()

val DEFAULT_HOME_TILES: List<HomeTile> = listOfNotNull(
    HomeTile.AndroidApp("android-app").takeIf { runsInBrowser },
    HomeTile.Weather("weather"),
    HomeTile.Periods("periods"),
    HomeTile.UpcomingEvents("upcoming", layout = EventCardStyle.Posters),
)

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
                is HomeTile.Periods -> put("type", "periods")
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
            HomeTile.Weather(id, if (name != null && latitude != null && longitude != null) WeatherPlace(name, latitude, longitude) else null)
        }
        "periods" -> HomeTile.Periods(id)
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
