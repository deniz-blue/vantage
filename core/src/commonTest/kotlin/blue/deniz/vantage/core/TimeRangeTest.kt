package blue.deniz.vantage.core

import directory.evnt.Occurrence
import directory.evnt.OpenEvnt
import directory.evnt.PartialDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant
import kotlinx.datetime.LocalDate

private val now = Instant.parse("2026-10-28T10:00:00Z")

private fun startingIn(wait: Duration) =
    Occurrence(start = PartialDate.fromInstant(now + wait, "Europe/Vilnius"), end = PartialDate.fromInstant(now + wait + 2.hours, "Europe/Vilnius"))

private fun eventOn(vararg occurrences: Occurrence) = OpenEvnt(id = "urn:uuid:test", name = mapOf("en" to "Test"), occurrences = occurrences.toList())

class TimeRangeTest {
    @Test
    fun aRunningOccurrenceIsStillTheNextOne() {
        val running = startingIn(-1.hours)
        val event = eventOn(startingIn(7.days), running, startingIn(-7.days))
        assertEquals(running, event.nextOccurrenceAt(now))
        assertEquals(running, event.nextOrLastOccurrenceAt(now))
        assertEquals(now - 1.hours, event.nextStartAt(now))
        assertEquals(1, listOf(ResolvedEvent(data = event)).upcomingAt(now).size)
    }

    @Test
    fun anEventWithEveryDatePassedShowsItsLatest() {
        val latest = startingIn(-1.days)
        val event = eventOn(startingIn(-14.days), latest, startingIn(-7.days))
        assertEquals(null, event.nextOccurrenceAt(now))
        assertEquals(latest, event.nextOrLastOccurrenceAt(now))
        assertEquals(null, event.nextStartAt(now))
        assertEquals(emptyList(), listOf(ResolvedEvent(data = event)).upcomingAt(now))
    }

    @Test
    fun aTimeWithoutAnEndEndsWhenItStarts() {
        val occurrence = Occurrence(start = PartialDate.fromInstant(now, "Europe/Vilnius"))
        assertEquals(TimeRange(now, now), occurrence.timeRange())
        assertEquals(null, eventOn(occurrence).nextStartAt(now))
    }

    @Test
    fun aDayWithoutAnEndLastsTheWholeDay() {
        val occurrence = Occurrence(start = PartialDate.Day(LocalDate(2026, 10, 28), "Europe/Vilnius"))
        assertTrue(now in occurrence.timeRange()!!)
        assertEquals(Instant.parse("2026-10-27T22:00:00Z"), eventOn(occurrence).nextStartAt(now))
    }

    @Test
    fun aCancelledEventIsCancelledOnEveryDate() {
        val held = startingIn(4.days)
        val cancelledDate = startingIn(11.days).copy(status = Occurrence.Status.Cancelled)
        val event = eventOn(held, cancelledDate)
        assertEquals(listOf(false, true), event.occurrences!!.map(event::isCancelledOn))
        val cancelledEvent = event.copy(status = OpenEvnt.Status.Cancelled)
        assertEquals(listOf(true, true), event.occurrences!!.map(cancelledEvent::isCancelledOn))
    }

    @Test
    fun aCancelledDateShowsAsCancelledWhateverTheEventSays() {
        val held = startingIn(4.days)
        val cancelledDate = startingIn(11.days).copy(status = Occurrence.Status.Cancelled)
        val postponed = eventOn(held, cancelledDate).copy(status = OpenEvnt.Status.Postponed)
        assertEquals(listOf(OpenEvnt.Status.Postponed, OpenEvnt.Status.Cancelled), postponed.occurrences!!.map(postponed::statusOn))
        assertEquals(null, eventOn(held).statusOn(held))
        assertEquals(OpenEvnt.Status.Postponed, postponed.statusOn(null))
    }

    @Test
    fun aCancelledDateIsNeverTheNextOne() {
        val cancelled = startingIn(4.days).copy(status = Occurrence.Status.Cancelled)
        val held = startingIn(11.days)
        val event = eventOn(startingIn(-3.days), cancelled, held)
        assertEquals(held, event.nextOccurrenceAt(now))
        assertEquals(held, event.nextOrLastOccurrenceAt(now))
        assertEquals(held.timeRange()?.start, event.nextStartAt(now))
    }

    @Test
    fun withOnlyCancelledDatesAheadTheLatestHeldOneShows() {
        val held = startingIn(-3.days)
        val event = eventOn(held, startingIn(4.days).copy(status = Occurrence.Status.Cancelled))
        assertEquals(null, event.nextOccurrenceAt(now))
        assertEquals(held, event.nextOrLastOccurrenceAt(now))
    }

    @Test
    fun anEventWithEveryDateCancelledStillShowsItsLatest() {
        val latest = startingIn(4.days).copy(status = Occurrence.Status.Cancelled)
        val event = eventOn(startingIn(-3.days).copy(status = Occurrence.Status.Cancelled), latest)
        assertEquals(latest, event.nextOrLastOccurrenceAt(now))
        assertEquals(2, event.timeRanges().size)
    }

    @Test
    fun aCancelledDateIsNotOneOfTheEventsTimes() {
        val held = startingIn(11.days)
        val event = eventOn(startingIn(4.days).copy(status = Occurrence.Status.Cancelled), held)
        assertEquals(listOf(held.timeRange()), event.timeRanges())
    }

    @Test
    fun theOccurrenceDuringAWindowIsTheOneOnThatDay() {
        val thisWeek = startingIn(1.days)
        val nextWeek = startingIn(8.days)
        val event = eventOn(thisWeek, nextWeek)
        val window = TimeRange(now + 7.days, now + 9.days)
        assertEquals(nextWeek, event.firstOccurrenceDuring(window))
        assertEquals(null, event.firstOccurrenceDuring(TimeRange(now + 3.days, now + 4.days)))
    }

    @Test
    fun aHeldOccurrenceWinsOverACancelledOneInTheSameWindow() {
        val cancelled = startingIn(1.days).copy(status = Occurrence.Status.Cancelled)
        val held = startingIn(1.days + 3.hours)
        val window = TimeRange(now, now + 2.days)
        assertEquals(held, eventOn(cancelled, held).firstOccurrenceDuring(window))
        assertEquals(cancelled, eventOn(cancelled).firstOccurrenceDuring(window))
    }
}
