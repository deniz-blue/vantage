package blue.deniz.vantage.core

import directory.evnt.PartialDate
import kotlin.math.absoluteValue
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.offsetAt

private val UTC_ZONE_NAMES = setOf("UTC", "UCT", "Universal", "Zulu", "Z", "GMT", "GMT0", "GMT+0", "GMT-0", "Greenwich")

fun PartialDate.Time.statedTimeZoneIfOffsetDiffers(from: TimeZone): TimeZone? {
    if (timeZoneId.removePrefix("Etc/") in UTC_ZONE_NAMES) return null
    return timeZone.takeIf { it.offsetAt(startInstant) != from.offsetAt(startInstant) }
}

fun timeZoneOffsetLabel(zone: TimeZone, at: Instant): String {
    val seconds = zone.offsetAt(at).totalSeconds
    if (seconds == 0) return "UTC"
    val minutes = seconds.absoluteValue / 60
    val sign = if (seconds < 0) "-" else "+"
    val rest = minutes % 60
    return "UTC$sign${minutes / 60}" + if (rest == 0) "" else ":${rest.toString().padStart(2, '0')}"
}

private val OFFSET_QUERY = Regex("""^(?:utc|gmt)?\s*([+\-−])\s*(\d{1,2})(?:[:.]?(\d{2}))?$""", RegexOption.IGNORE_CASE)

fun offsetLabelOfQuery(query: String): String? {
    val text = query.trim()
    if (text.equals("utc", ignoreCase = true) || text.equals("gmt", ignoreCase = true)) return "UTC"
    val match = OFFSET_QUERY.matchEntire(text) ?: return null
    val hours = match.groupValues[2].toInt()
    val minutes = match.groupValues[3].ifEmpty { "0" }.toInt()
    if (hours == 0 && minutes == 0) return "UTC"
    val sign = if (match.groupValues[1] == "+") "+" else "-"
    return "UTC$sign$hours" + if (minutes == 0) "" else ":${minutes.toString().padStart(2, '0')}"
}

fun timeZoneDisplayName(zone: TimeZone, at: Instant): String {
    val offset = timeZoneOffsetLabel(zone, at)
    val country = CountryTimeZones.of(zone.id) ?: return if (zone.id.removePrefix("Etc/") in UTC_ZONE_NAMES) offset else "${zone.id.replace('_', ' ')} · $offset"
    return "${Countries.flagOf(country.countryCode)}  ${country.city} · $offset"
}

fun timeZoneShortLabel(zone: TimeZone, at: Instant): String = cityNameOfTimeZone(zone.id) ?: timeZoneOffsetLabel(zone, at)
