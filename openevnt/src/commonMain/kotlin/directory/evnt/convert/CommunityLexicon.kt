package directory.evnt.convert

import directory.evnt.Component
import directory.evnt.Media
import directory.evnt.Occurrence
import directory.evnt.OpenEvnt
import directory.evnt.Venue
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlin.math.abs

private fun JsonObject.string(key: String) = (this[key] as? JsonPrimitive)?.takeIf { it.isString }?.content

private fun JsonObject.objects(key: String) = (this[key] as? JsonArray).orEmpty().filterIsInstance<JsonObject>()

private fun JsonElement?.asNumber() = (this as? JsonPrimitive)?.content?.toDoubleOrNull()?.takeIf { it.isFinite() }

private fun coordinatesOf(location: JsonObject): Component.Coordinates? {
    val latitude = location["latitude"].asNumber() ?: return null
    val longitude = location["longitude"].asNumber() ?: return null
    return if (abs(latitude) <= 90 && abs(longitude) <= 180) Component.Coordinates(latitude, longitude) else null
}

object CommunityLexicon {
    const val EVENT_TYPE = "community.lexicon.calendar.event"
    const val ADDITIONAL_DATA_TYPE = "community.lexicon.calendar.event.additionalData#0"

    fun read(text: String, options: ConvertOptions = ConvertOptions()): OpenEvnt {
        val language = options.language
        val event = Json.parseToJsonElement(text).jsonObject

        val components = mutableListOf<Component>()
        val venueComponents = mutableListOf<Component>()
        var venueName: Map<String, String>? = null
        var address: String? = null
        var countryCode: String? = null

        for (location in event.objects("locations")) {
            val name = location.string("name")
            val type = location.string("\$type")
            if (type == "$EVENT_TYPE#uri") {
                location.string("uri")?.let { components += linkTo(it, name, language) }
                continue
            }
            venueName = venueName ?: textIn(language, name)
            when (type) {
                "community.lexicon.location.address" -> {
                    address = address ?: listOfNotNull(location.string("street"), location.string("locality"), location.string("region"))
                        .filter { it.isNotEmpty() }.joinToString(" ").ifEmpty { null }
                    val country = location.string("country")
                    if (country != null && Regex("[A-Za-z]{2}").matches(country)) countryCode = countryCode ?: country.uppercase()
                }
                "community.lexicon.location.fsq" -> {
                    location.string("fsq_place_id")?.let { venueComponents += Component.PlaceId("com.foursquare", it) }
                    coordinatesOf(location)?.let { venueComponents += it }
                }
                "community.lexicon.location.geo" -> coordinatesOf(location)?.let { venueComponents += it }
            }
        }

        val firstCoordinates = venueComponents.firstOrNull { it is Component.Coordinates }
        val venue = Venue(
            name = venueName,
            address = address,
            countryCode = countryCode,
            components = venueComponents.filter { it !is Component.Coordinates || it === firstCoordinates }.orNullIfEmpty(),
        ).takeIf { it != Venue() }

        val start = event.string("startsAt")?.let { parseWrittenDate(it, options.timeZoneId) }
        val end = event.string("endsAt")?.let { parseWrittenDate(it, options.timeZoneId) }

        for (uri in event.objects("uris")) uri.string("uri")?.let { components += linkTo(it, uri.string("name"), language) }

        for (media in event.objects("media")) {
            val content = media["content"] as? JsonObject ?: continue
            val blob = (content["ref"] as? JsonObject)?.string("\$link") ?: continue
            val url = options.did?.let { "https://blobs.blue/$it/blob/$blob" } ?: "at://$blob"
            components += Component.SplashMedia(
                media = Media(sources = listOf(Media.Source(url, content.string("mimeType"))), alt = textIn(language, media.string("alt"))),
                roles = listOfNotNull(media.string("role"), "background", "poster").distinct(),
            )
        }

        event.string("description")?.takeIf { it.isNotEmpty() }?.let { components += plainText(it, language) }

        (event["additionalData"] as? JsonObject)?.let { components += Component.Unknown(ADDITIONAL_DATA_TYPE, it) }

        return OpenEvnt(
            id = options.documentId(),
            name = textIn(language, event.string("name")) ?: mapOf(language to "Untitled Event"),
            status = when (event.string("status")) {
                "$EVENT_TYPE#cancelled" -> OpenEvnt.Status.Cancelled
                "$EVENT_TYPE#postponed" -> OpenEvnt.Status.Postponed
                else -> null
            },
            venue = venue,
            occurrences = if (start != null || end != null) listOf(Occurrence(start = start, end = end)) else null,
            components = components.orNullIfEmpty(),
        )
    }
}
