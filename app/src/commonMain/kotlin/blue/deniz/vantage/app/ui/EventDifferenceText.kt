package blue.deniz.vantage.app.ui

import blue.deniz.vantage.core.EventDifference
import blue.deniz.vantage.core.isCancelled
import directory.evnt.Occurrence
import directory.evnt.OpenEvnt
import directory.evnt.Venue
import directory.evnt.translate

fun EventFormatting.describe(difference: EventDifference, preferredLanguages: List<String>): String = when (difference) {
    is EventDifference.Renamed -> "Renamed from “${difference.from.translate(preferredLanguages)}”"
    is EventDifference.StatusChanged -> when (difference.to) {
        OpenEvnt.Status.Cancelled -> "Cancelled"
        OpenEvnt.Status.Postponed -> "Postponed"
        OpenEvnt.Status.Uncertain -> "Marked as uncertain"
        null -> when (difference.from) {
            OpenEvnt.Status.Cancelled -> "No longer cancelled"
            OpenEvnt.Status.Postponed -> "No longer postponed"
            else -> "No longer uncertain"
        }
    }
    is EventDifference.VenueChanged -> {
        val from = difference.from
        val to = difference.to
        when {
            to == null -> "Venue removed"
            from == null -> "Venue added: ${to.label(preferredLanguages)}"
            else -> "Venue changed from ${from.label(preferredLanguages)} to ${to.label(preferredLanguages)}"
        }
    }
    is EventDifference.OccurrenceMoved -> "Moved from ${occurrenceLine(difference.from)} to ${occurrenceLine(difference.to)}"
    is EventDifference.OccurrenceAdded -> "New date: ${occurrenceLine(difference.occurrence)}"
    is EventDifference.OccurrenceRemoved -> "Date removed: ${occurrenceLine(difference.occurrence)}"
    is EventDifference.OccurrenceStatusChanged ->
        if (difference.to.isCancelled) "Cancelled on ${occurrenceLine(difference.to)}"
        else "Back on: ${occurrenceLine(difference.to)}"
    EventDifference.DetailsChanged -> "Description, links or pictures changed"
}

fun List<EventDifference>.shortLabel(): String = when {
    any { it is EventDifference.StatusChanged && it.to == OpenEvnt.Status.Cancelled } -> "Cancelled"
    any { it is EventDifference.StatusChanged && it.to == OpenEvnt.Status.Postponed } -> "Postponed"
    any { it is EventDifference.OccurrenceStatusChanged && it.to.isCancelled } -> "Date Cancelled"
    any { it is EventDifference.OccurrenceMoved || it is EventDifference.OccurrenceRemoved } -> "Moved"
    any { it is EventDifference.OccurrenceAdded } -> "New Date"
    any { it is EventDifference.VenueChanged } -> "New Venue"
    any { it is EventDifference.Renamed } -> "Renamed"
    else -> "Updated"
}

private fun EventFormatting.occurrenceLine(occurrence: Occurrence): String =
    occurrenceText(occurrence).let { text -> listOfNotNull(text.headline, text.supporting).joinToString(", ") }

private fun Venue.label(preferredLanguages: List<String>): String =
    name.translate(preferredLanguages).ifEmpty { address.orEmpty() }.ifEmpty { "a place with no name" }
