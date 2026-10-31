package blue.deniz.vantage.core

import directory.evnt.OpenEvnt
import directory.evnt.Occurrence
import directory.evnt.Translations
import directory.evnt.Venue

sealed interface EventDifference {
    data class Renamed(val from: Translations, val to: Translations) : EventDifference
    data class StatusChanged(val from: OpenEvnt.Status?, val to: OpenEvnt.Status?) : EventDifference
    data class VenueChanged(val from: Venue?, val to: Venue?) : EventDifference
    data class OccurrenceMoved(val from: Occurrence, val to: Occurrence) : EventDifference
    data class OccurrenceAdded(val occurrence: Occurrence) : EventDifference
    data class OccurrenceRemoved(val occurrence: Occurrence) : EventDifference
    data class OccurrenceStatusChanged(val from: Occurrence, val to: Occurrence) : EventDifference
    data object DetailsChanged : EventDifference
}

fun OpenEvnt.differencesFrom(previous: OpenEvnt): List<EventDifference> = buildList {
    if (name != previous.name) add(EventDifference.Renamed(previous.name, name))
    if (status != previous.status) add(EventDifference.StatusChanged(previous.status, status))
    if (venue.withoutComponents() != previous.venue.withoutComponents()) add(EventDifference.VenueChanged(previous.venue, venue))
    addAll(occurrenceDifferences(previous.occurrences.orEmpty(), occurrences.orEmpty()))
    if (components != previous.components || venue?.components != previous.venue?.components) add(EventDifference.DetailsChanged)
}

private fun Venue?.withoutComponents(): Venue? = this?.copy(components = null)

private fun Occurrence.sameTimes(other: Occurrence) = start == other.start && end == other.end

private fun occurrenceDifferences(previous: List<Occurrence>, current: List<Occurrence>): List<EventDifference> {
    val unmatchedPrevious = previous.toMutableList()
    val unmatchedCurrent = mutableListOf<Occurrence>()
    val differences = mutableListOf<EventDifference>()
    for (occurrence in current) {
        val same = unmatchedPrevious.firstOrNull { it.sameTimes(occurrence) }
        if (same == null) {
            unmatchedCurrent += occurrence
            continue
        }
        unmatchedPrevious -= same
        if (same.status != occurrence.status) differences += EventDifference.OccurrenceStatusChanged(same, occurrence)
    }
    unmatchedPrevious.zip(unmatchedCurrent).forEach { (from, to) -> differences += EventDifference.OccurrenceMoved(from, to) }
    unmatchedPrevious.drop(unmatchedCurrent.size).forEach { differences += EventDifference.OccurrenceRemoved(it) }
    unmatchedCurrent.drop(unmatchedPrevious.size).forEach { differences += EventDifference.OccurrenceAdded(it) }
    return differences
}
