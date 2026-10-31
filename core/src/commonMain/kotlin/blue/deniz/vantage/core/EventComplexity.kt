package blue.deniz.vantage.core

import directory.evnt.Occurrence
import directory.evnt.OpenEvnt
import directory.evnt.PartialDate
import directory.evnt.Venue
import kotlin.time.Duration.Companion.days

enum class EventComplexity { Line, Plain, Full }

fun OpenEvnt.complexity(): EventComplexity {
    val occurrences = occurrences.orEmpty()
    return when {
        backgroundSplashMedia() != null -> EventComplexity.Full
        occurrences.any { it.activities.orEmpty().isNotEmpty() || it.spansDays() } -> EventComplexity.Full
        occurrences.runBackToBack() -> EventComplexity.Full
        status != null || occurrences.any { it.isCancelled } -> EventComplexity.Plain
        venue.hasPlace() || occurrences.any { it.start is PartialDate.Time } -> EventComplexity.Plain
        else -> EventComplexity.Line
    }
}

private fun Occurrence.spansDays(): Boolean {
    val start = start ?: return false
    val end = end ?: return false
    return end.endInstant - start.startInstant > 1.days
}

private fun List<Occurrence>.runBackToBack(): Boolean {
    val dated = mapNotNull { occurrence -> occurrence.start?.let { start -> start to (occurrence.end ?: start) } }.sortedBy { it.first.startInstant }
    return dated.zipWithNext().any { (earlier, later) -> later.first.startInstant - earlier.second.endInstant <= 1.days }
}

private fun Venue?.hasPlace() = this != null && (name != null || address != null)
