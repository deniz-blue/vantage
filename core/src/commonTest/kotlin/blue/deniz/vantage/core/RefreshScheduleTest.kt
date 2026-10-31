package blue.deniz.vantage.core

import directory.evnt.Occurrence
import directory.evnt.OpenEvnt
import directory.evnt.PartialDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

private val now = Instant.parse("2026-10-28T10:00:00Z")

private fun startingIn(wait: Duration, length: Duration = 2.hours) = OpenEvnt(
    id = "urn:uuid:test",
    name = mapOf("en" to "Test"),
    occurrences = listOf(Occurrence(start = PartialDate.fromInstant(now + wait, "Europe/Vilnius"), end = PartialDate.fromInstant(now + wait + length, "Europe/Vilnius"))),
)

private fun fetched(ago: Duration, event: OpenEvnt) = ResolvedEvent(data = event, updatedAt = now - ago)

class RefreshScheduleTest {
    @Test
    fun theIntervalShrinksAsTheStartGetsCloser() {
        assertEquals(7.days, refreshIntervalBefore(365.days))
        assertEquals(2.5.days, refreshIntervalBefore(60.days))
        assertEquals(1.hours, refreshIntervalBefore(1.days))
        assertEquals(30.minutes, refreshIntervalBefore(2.hours))
        assertEquals(30.minutes, refreshIntervalBefore(Duration.ZERO))
    }

    @Test
    fun anEventTomorrowIsRefreshedHourlyAndOneInTwoMonthsEveryFewDays() {
        assertTrue(fetched(61.minutes, startingIn(1.days)).isDueForRefresh(now))
        assertFalse(fetched(59.minutes, startingIn(1.days)).isDueForRefresh(now))
        assertFalse(fetched(2.days, startingIn(60.days)).isDueForRefresh(now))
        assertTrue(fetched(3.days, startingIn(60.days)).isDueForRefresh(now))
    }

    @Test
    fun anEventHappeningNowIsRefreshedMostOften() {
        assertTrue(fetched(31.minutes, startingIn((-1).hours)).isDueForRefresh(now))
        assertFalse(fetched(29.minutes, startingIn((-1).hours)).isDueForRefresh(now))
    }

    @Test
    fun anEventThatEndedThisMonthIsRefreshedLessOftenAsItRecedes() {
        assertEquals(1.days, refreshIntervalAfter(Duration.ZERO))
        assertEquals(2.days, refreshIntervalAfter(4.days))
        assertEquals(7.days, refreshIntervalAfter(20.days))
        assertTrue(fetched(25.hours, startingIn((-3).hours)).isDueForRefresh(now))
        assertFalse(fetched(23.hours, startingIn((-3).hours)).isDueForRefresh(now))
        assertTrue(fetched(5.days, startingIn((-10).days)).isDueForRefresh(now))
        assertFalse(fetched(4.days, startingIn((-10).days)).isDueForRefresh(now))
    }

    @Test
    fun anEventThatEndedOverAMonthAgoIsNotDueAndOneNeverFetchedIs() {
        assertFalse(fetched(30.days, startingIn((-31).days)).isDueForRefresh(now))
        assertTrue(ResolvedEvent(data = startingIn(60.days)).isDueForRefresh(now))
    }
}
