package blue.deniz.vantage.core

import directory.evnt.OpenEvnt
import directory.evnt.convert.CommunityLexicon
import directory.evnt.convert.ConvertOptions
import directory.evnt.convert.ICalendar
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

@Serializable
enum class EventFormat {
    @SerialName("unknown")
    Unknown,

    @SerialName("directory.evnt.event")
    OpenEvnt,

    @SerialName("community.lexicon.calendar.event")
    CommunityLexicon,

    @SerialName("ics")
    ICalendar,
}

private fun JsonObject.string(key: String) = (this[key] as? JsonPrimitive)?.takeIf { it.isString }?.content

fun inferEventFormat(raw: String): EventFormat {
    if (ICalendar.looksLikeICalendar(raw)) return EventFormat.ICalendar
    val json = try {
        Json.parseToJsonElement(raw) as? JsonObject
    } catch (_: IllegalArgumentException) {
        null
    } ?: return EventFormat.Unknown
    return when {
        json.string("\$type") == OpenEvnt.AT_RECORD_TYPE -> EventFormat.OpenEvnt
        json.string("\$type") == CommunityLexicon.EVENT_TYPE -> EventFormat.CommunityLexicon
        json.string("v") != null && json["name"] is JsonObject -> EventFormat.OpenEvnt
        else -> EventFormat.Unknown
    }
}

fun parseEvent(raw: String, format: EventFormat, source: EventSource, options: ConvertOptions = ConvertOptions()): OpenEvnt {
    val readOptions = options.copy(
        did = (source as? EventSource.At)?.uri?.takeIf { it.repoIsDid }?.repo ?: options.did,
        id = options.id ?: (source as? EventSource.Remote)?.location,
    )
    return when (format) {
        EventFormat.OpenEvnt -> OpenEvnt.parse(raw)
        EventFormat.CommunityLexicon -> CommunityLexicon.read(raw, readOptions)
        EventFormat.ICalendar -> ICalendar.read(raw, readOptions)
        EventFormat.Unknown -> throw EventException(EventError(EventError.Kind.UnknownFormat, "The format of this event is not known"))
    }
}
