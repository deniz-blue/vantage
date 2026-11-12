package blue.deniz.vantage.core

import directory.evnt.Occurrence
import directory.evnt.OpenEvnt
import kotlin.time.Instant

data class TimeRange(val start: Instant, val end: Instant) : Comparable<TimeRange> {
    operator fun contains(instant: Instant) = instant >= start && instant < end

    override fun compareTo(other: TimeRange) = compareValuesBy(this, other, TimeRange::start, TimeRange::end)
}

fun Occurrence.timeRange(): TimeRange? {
    val start = this.start ?: return null
    return TimeRange(start.startInstant, (end ?: start).endInstant)
}

val Occurrence.isCancelled: Boolean get() = status == Occurrence.Status.Cancelled

fun OpenEvnt.isCancelledOn(occurrence: Occurrence): Boolean = status == OpenEvnt.Status.Cancelled || occurrence.isCancelled

fun OpenEvnt.statusOn(occurrence: Occurrence?): OpenEvnt.Status? = if (occurrence?.isCancelled == true) OpenEvnt.Status.Cancelled else status

fun OpenEvnt.shownOccurrences(): List<Occurrence> {
    val all = occurrences.orEmpty()
    return all.filterNot { it.isCancelled }.ifEmpty { all }
}

fun OpenEvnt.timeRanges(): List<TimeRange> = shownOccurrences().mapNotNull { it.timeRange() }

fun List<ResolvedEvent>.upcomingAt(now: Instant): List<ResolvedEvent> =
    mapNotNull { event -> event.data?.nextTimeRangeAt(now)?.let { event to it } }
        .sortedBy { (_, range) -> range }
        .map { (event, _) -> event }

private fun List<Occurrence>.latest(): Occurrence? = maxByOrNull { it.start?.startInstant ?: Instant.DISTANT_PAST }

fun OpenEvnt.nextOccurrenceAt(now: Instant): Occurrence? = shownOccurrences()
    .mapNotNull { occurrence -> occurrence.timeRange()?.let { occurrence to it } }
    .filter { (_, range) -> range.end > now }
    .minByOrNull { (_, range) -> range }
    ?.first

fun OpenEvnt.nextTimeRangeAt(now: Instant): TimeRange? = nextOccurrenceAt(now)?.timeRange()

fun OpenEvnt.nextStartAt(now: Instant): Instant? = nextTimeRangeAt(now)?.start

fun OpenEvnt.latestTimeRange(): TimeRange? = timeRanges().maxWithOrNull(compareBy<TimeRange> { it.start }.thenByDescending { it.end })

fun OpenEvnt.nextOrLastOccurrenceAt(now: Instant): Occurrence? = nextOccurrenceAt(now) ?: shownOccurrences().latest()

fun OpenEvnt.firstOccurrenceDuring(window: TimeRange): Occurrence? = shownOccurrences()
    .mapNotNull { occurrence -> occurrence.timeRange()?.let { occurrence to it } }
    .filter { (_, range) -> range.end > window.start && range.start < window.end }
    .minByOrNull { (_, range) -> range.start }
    ?.first
