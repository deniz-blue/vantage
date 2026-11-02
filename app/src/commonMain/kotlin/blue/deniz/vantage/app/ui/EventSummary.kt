package blue.deniz.vantage.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import blue.deniz.vantage.app.countryName
import blue.deniz.vantage.core.Countries
import blue.deniz.vantage.core.shownOccurrences
import directory.evnt.Component
import directory.evnt.OpenEvnt
import directory.evnt.Venue
import directory.evnt.groupDates
import directory.evnt.hasText
import directory.evnt.translate

private const val CARD_DATE_GROUP_LIMIT = 3
private const val COPIED_SUMMARY_DATE_GROUP_LIMIT = 5

const val UNTITLED_EVENT = "Untitled Event"

fun OpenEvnt.translatedName(preferredLanguages: List<String>): String? =
    name.takeIf { it.hasText() }?.translate(preferredLanguages)

enum class SummaryLineKind(val emoji: String) {
    Venue("📍"),
    Date("📅"),
    Time("🕐");

    val icon: ImageVector
        get() = when (this) {
            Venue -> AppIcons.MapPin
            Date -> AppIcons.Calendar
            Time -> AppIcons.Clock
        }
}

val OpenEvnt.Status.label: String
    get() = when (this) {
        OpenEvnt.Status.Uncertain -> "Uncertain"
        OpenEvnt.Status.Cancelled -> "Cancelled"
        OpenEvnt.Status.Postponed -> "Postponed"
    }

val OpenEvnt.Status.color: Color
    @Composable get() = if (this == OpenEvnt.Status.Cancelled) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary

data class SummaryLine(val kind: SummaryLineKind, val text: String)

fun OpenEvnt.summaryLines(
    formatting: EventFormatting,
    preferredLanguages: List<String>,
    dateGroupLimit: Int = CARD_DATE_GROUP_LIMIT,
): List<SummaryLine> = buildList {
    venueLine(preferredLanguages)?.let { add(SummaryLine(SummaryLineKind.Venue, it)) }
    val groups = groupDates(shownOccurrences())
    for (group in groups.take(dateGroupLimit)) {
        for (date in formatting.formatDates(group.dates)) add(SummaryLine(SummaryLineKind.Date, date))
        for (slot in group.times) add(SummaryLine(SummaryLineKind.Time, formatting.formatTimeSlot(slot)))
    }
    val overflow = groups.size - dateGroupLimit
    if (overflow > 0) add(SummaryLine(SummaryLineKind.Date, "+$overflow more"))
}

fun Venue.displayName(preferredLanguages: List<String>): String? =
    name.takeIf { it.hasText() }?.translate(preferredLanguages) ?: address

private val Venue.flag: String get() = countryCode?.let(Countries::flagOf).orEmpty()

fun Venue.addressLine(preferredLanguages: List<String>): String? =
    address?.takeIf { it.isNotBlank() }
        ?: countryCode?.takeIf { flag.isNotEmpty() }?.let { countryName(it, preferredLanguages.firstOrNull() ?: "en") }

fun Venue.addressLineWithFlag(preferredLanguages: List<String>): String? =
    addressLine(preferredLanguages)?.let { if (flag.isEmpty()) it else "$it $flag" }

fun OpenEvnt.venueLine(preferredLanguages: List<String>): String? {
    val venue = venue ?: return null
    return venue.name.takeIf { it.hasText() }?.translate(preferredLanguages) ?: venue.addressLine(preferredLanguages) ?: "Unnamed Place"
}

data class LinkText(val headline: String, val supporting: String?)

fun Component.Link.linkText(preferredLanguages: List<String>): LinkText {
    val name = name.takeIf { it.hasText() }?.translate(preferredLanguages)
    val address = url.substringAfter("://").removePrefix("www.").trimEnd('/')
    val host = address.substringBefore('/')
    return if (name != null) LinkText(name, host) else LinkText(host, address.takeIf { it != host })
}

fun OpenEvnt.cardLines(formatting: EventFormatting, preferredLanguages: List<String>): List<SummaryLine> = buildList {
    val groups = groupDates(shownOccurrences())
    groups.firstOrNull()?.let { group ->
        val more = if (groups.size > 1) " (+${groups.size - 1} more)" else ""
        add(SummaryLine(SummaryLineKind.Date, formatting.formatDates(group.dates).joinToString(" · ") + more))
        val times = group.times.map(formatting::formatTimeSlot).filter { it.isNotEmpty() }
        if (times.isNotEmpty()) add(SummaryLine(SummaryLineKind.Time, times.joinToString(" · ")))
    }
    venueLine(preferredLanguages)?.let { add(SummaryLine(SummaryLineKind.Venue, it)) }
}

fun OpenEvnt.markdownSummary(formatting: EventFormatting, preferredLanguages: List<String>): String {
    val title = translatedName(preferredLanguages) ?: UNTITLED_EVENT
    val statusText = status?.let { " (${it.label})" }.orEmpty()
    val lines = summaryLines(formatting, preferredLanguages, COPIED_SUMMARY_DATE_GROUP_LIMIT)
    return (listOf("**$title**$statusText") + lines.map { "${it.kind.emoji} ${it.text}" }).joinToString("\n")
}
