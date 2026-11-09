package blue.deniz.vantage.core

import directory.evnt.Component
import directory.evnt.Venue
import io.ktor.http.encodeURLParameter

val Venue.coordinates: Component.Coordinates?
    get() = components.orEmpty().filterIsInstance<Component.Coordinates>().firstOrNull()

sealed interface MapPlace {
    data class Point(val latitude: Double, val longitude: Double, val label: String?) : MapPlace
    data class Search(val text: String) : MapPlace

    companion object {
        fun of(venue: Venue, name: String?): MapPlace? {
            venue.coordinates?.let { return Point(it.latitude, it.longitude, name ?: venue.address) }
            return listOfNotNull(name, venue.address).joinToString(", ").takeIf { it.isNotBlank() }?.let(::Search)
        }
    }
}

private val COORDINATES_IN_MAP_LINKS = listOf(
    Regex("""@(-?\d+\.\d+),(-?\d+\.\d+)"""),
    Regex("""[?&]mlat=(-?\d+\.\d+)&mlon=(-?\d+\.\d+)"""),
    Regex("""#map=\d+(?:\.\d+)?/(-?\d+\.\d+)/(-?\d+\.\d+)"""),
    Regex("""[?&](?:q|query|ll)=(-?\d+\.\d+)(?:,|%2C)\s*(-?\d+\.\d+)"""),
)

object MapLinks {
    fun coordinatesIn(link: String): Component.Coordinates? = COORDINATES_IN_MAP_LINKS.firstNotNullOfOrNull { pattern ->
        pattern.find(link.trim())?.let { Component.Coordinates(it.groupValues[1].toDouble(), it.groupValues[2].toDouble()) }
    }

    fun geo(place: MapPlace): String = when (place) {
        is MapPlace.Point -> "geo:${place.position}?q=${place.position}" + place.label?.let { "(${it.encodeURLParameter()})" }.orEmpty()
        is MapPlace.Search -> "geo:0,0?q=${place.text.encodeURLParameter()}"
    }

    fun googleMaps(place: MapPlace) = "https://www.google.com/maps/search/?api=1&query=" + place.query

    fun directions(place: MapPlace) = "https://www.google.com/maps/dir/?api=1&destination=" + place.query

    fun openStreetMap(place: MapPlace): String = when (place) {
        is MapPlace.Point -> "https://www.openstreetmap.org/?mlat=${place.latitude}&mlon=${place.longitude}#map=17/${place.latitude}/${place.longitude}"
        is MapPlace.Search -> "https://www.openstreetmap.org/search?query=" + place.text.encodeURLParameter()
    }

    private val MapPlace.Point.position get() = "$latitude,$longitude"

    private val MapPlace.query
        get() = when (this) {
            is MapPlace.Point -> position.encodeURLParameter()
            is MapPlace.Search -> text.encodeURLParameter()
        }
}
