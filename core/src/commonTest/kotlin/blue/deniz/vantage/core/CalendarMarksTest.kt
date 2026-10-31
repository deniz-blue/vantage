package blue.deniz.vantage.core

import directory.evnt.PartialDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.time.Instant

class CalendarMarksTest {
    @Test
    fun aPeriodOfDaysRunsToTheEndOfItsLastDay() {
        val week = CalendarPeriod("as", "AS week", PartialDate.parse("2026-12-14[Europe/Vilnius]"), PartialDate.parse("2026-12-20[Europe/Vilnius]"))
        assertEquals(Instant.parse("2026-12-13T22:00:00Z"), week.startsAt)
        assertEquals(Instant.parse("2026-12-20T22:00:00Z"), week.endsAt)
    }

    @Test
    fun anInstantIsInEveryPeriodThatHoldsIt() {
        val session = CalendarPeriod("s", "Exam Session", PartialDate.parse("2027-01-04[Europe/Vilnius]"), PartialDate.parse("2027-01-24[Europe/Vilnius]"))
        val week = CalendarPeriod("w", "Retake Week", PartialDate.parse("2027-01-18[Europe/Vilnius]"), PartialDate.parse("2027-01-24[Europe/Vilnius]"))
        val marks = CalendarMarks(emptyList(), listOf(session, week))
        assertEquals(listOf(session), marks.periodsAt(Instant.parse("2027-01-04T08:00:00Z")))
        assertEquals(listOf(session, week), marks.periodsAt(Instant.parse("2027-01-24T21:59:00Z")))
        assertEquals(emptyList(), marks.periodsAt(Instant.parse("2027-01-24T22:00:00Z")))
    }

    @Test
    fun aPeriodOverlapsARangeItRunsInto() {
        val session = CalendarPeriod("s", "Exam Session", PartialDate.parse("2027-01-04[Europe/Vilnius]"), PartialDate.parse("2027-01-24[Europe/Vilnius]"))
        val week = CalendarPeriod("w", "AS Week", PartialDate.parse("2026-12-14[Europe/Vilnius]"), PartialDate.parse("2026-12-20[Europe/Vilnius]"))
        val marks = CalendarMarks(emptyList(), listOf(session, week))
        assertEquals(listOf(week, session), marks.periodsOverlapping(null, null))
        assertEquals(listOf(session), marks.periodsOverlapping(Instant.parse("2027-01-10T00:00:00Z"), null))
        assertEquals(listOf(week), marks.periodsOverlapping(null, Instant.parse("2027-01-03T22:00:00Z")))
        assertEquals(emptyList(), marks.periodsOverlapping(Instant.parse("2027-01-24T22:00:00Z"), null))
    }

    @Test
    fun aDayOrSpanIsInEveryPeriodThatTouchesItAndATimeOnlyInThoseHoldingIt() {
        val session = CalendarPeriod("s", "Exam Session", PartialDate.parse("2027-01-04[Europe/Vilnius]"), PartialDate.parse("2027-01-24[Europe/Vilnius]"))
        val rush = CalendarPeriod("r", "Report Rush", PartialDate.parse("2027-01-17T18:00[Europe/Vilnius]"), PartialDate.parse("2027-01-19[Europe/Vilnius]"))
        val marks = CalendarMarks(emptyList(), listOf(rush, session))
        assertEquals(listOf(session, rush), marks.periodsOn(PartialDate.parse("2027-01-17[Europe/Vilnius]")))
        assertEquals(listOf(session), marks.periodsOn(PartialDate.parse("2027-01-17T12:00[Europe/Vilnius]")))
        assertEquals(listOf(session, rush), marks.periodsOn(PartialDate.parse("2027-01-17T19:00[Europe/Vilnius]")))
        assertEquals(emptyList(), marks.periodsOn(PartialDate.parse("2027-01-25[Europe/Vilnius]")))
        assertEquals(listOf(session, rush), marks.periodsOn(PartialDate.parse("2027-01-17T17:00[Europe/Vilnius]"), PartialDate.parse("2027-01-17T19:00[Europe/Vilnius]")))
        assertEquals(listOf(session), marks.periodsOn(PartialDate.parse("2027-01-17T16:00[Europe/Vilnius]"), PartialDate.parse("2027-01-17T18:00[Europe/Vilnius]")))
    }

    @Test
    fun markersAndPeriodsNeedADayOrATime() {
        assertFailsWith<IllegalArgumentException> { CalendarMarker("m", "Sometime", PartialDate.parse("2026-11[Europe/Vilnius]")) }
        assertFailsWith<IllegalArgumentException> {
            CalendarPeriod("p", "Backwards", PartialDate.parse("2026-12-20[Europe/Vilnius]"), PartialDate.parse("2026-12-14[Europe/Vilnius]"))
        }
    }
}
