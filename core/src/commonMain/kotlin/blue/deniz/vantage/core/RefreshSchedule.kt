package blue.deniz.vantage.core

import directory.evnt.OpenEvnt
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

val SHORTEST_REFRESH_INTERVAL = 30.minutes
private val LONGEST_REFRESH_INTERVAL = 7.days
private const val REFRESH_INTERVALS_PER_WAIT = 24
val REFRESHES_CONTINUE_AFTER_END = 30.days
private val SHORTEST_REFRESH_INTERVAL_AFTER_END = 1.days

fun refreshIntervalBefore(timeUntilStart: Duration): Duration =
    (timeUntilStart / REFRESH_INTERVALS_PER_WAIT).coerceIn(SHORTEST_REFRESH_INTERVAL, LONGEST_REFRESH_INTERVAL)

fun refreshIntervalAfter(timeSinceEnd: Duration): Duration =
    (timeSinceEnd / 2).coerceIn(SHORTEST_REFRESH_INTERVAL_AFTER_END, LONGEST_REFRESH_INTERVAL)

fun OpenEvnt.timeUntilNextStart(now: Instant): Duration? =
    nextStartAt(now)?.let { (it - now).coerceAtLeast(Duration.ZERO) }

fun OpenEvnt.timeSinceLastEnd(now: Instant): Duration? =
    occurrences.orEmpty()
        .mapNotNull { occurrence -> (occurrence.end ?: occurrence.start)?.endInstant }
        .maxOrNull()
        ?.let { (now - it).coerceAtLeast(Duration.ZERO) }

fun ResolvedEvent.isDueForRefresh(now: Instant): Boolean {
    val fetchedAt = updatedAt ?: return true
    val event = data ?: return false
    val interval = event.timeUntilNextStart(now)?.let(::refreshIntervalBefore)
        ?: event.timeSinceLastEnd(now)?.takeIf { it < REFRESHES_CONTINUE_AFTER_END }?.let(::refreshIntervalAfter)
        ?: return false
    return now - fetchedAt >= interval
}
