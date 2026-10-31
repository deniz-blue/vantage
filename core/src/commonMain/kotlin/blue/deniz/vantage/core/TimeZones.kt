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

fun timeZoneShortLabel(zone: TimeZone, at: Instant): String = cityNameOfTimeZone(zone.id) ?: timeZoneOffsetLabel(zone, at)
