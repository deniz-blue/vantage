package blue.deniz.vantage.core

import directory.evnt.Occurrence
import directory.evnt.PartialDate
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

private const val VILNIUS = "Europe/Vilnius"

class OccurrenceEditingTest {
    private val evening = Occurrence(
        start = PartialDate.Time(2026, 10, 21, 20, 0, VILNIUS),
        end = PartialDate.Time(2026, 10, 21, 23, 0, VILNIUS),
    )

    @Test
    fun movingTheStartDayMovesTheEndWithIt() {
        val moved = evening.withStart(evening.start!!.withLocalDate(LocalDate(2026, 10, 29)))
        assertEquals(PartialDate.Time(2026, 10, 29, 20, 0, VILNIUS), moved.start)
        assertEquals(PartialDate.Time(2026, 10, 29, 23, 0, VILNIUS), moved.end)
    }

    @Test
    fun anEndMovesAsManyDaysAsItsStart() {
        val end = PartialDate.Day(2026, 11, 2, VILNIUS)
        val moved = end.movedWithStart(PartialDate.Day(2026, 10, 31, VILNIUS), PartialDate.Day(2026, 11, 5, VILNIUS))
        assertEquals(PartialDate.Day(2026, 11, 7, VILNIUS), moved)
    }

    @Test
    fun anEndBeforeTheNewStartIsDropped() {
        val late = evening.withStart(evening.start!!.withTimeOfDay(23, 30))
        assertNull(late.end)
    }

    @Test
    fun anEndTimeNotAfterTheStartTimeIsTheNextDay() {
        val party = Occurrence(start = PartialDate.Time(2026, 10, 31, 22, 0, VILNIUS), end = PartialDate.Time(2026, 10, 31, 23, 0, VILNIUS))
        val overnight = party.withEndTimeOfDay(4, 0)
        assertEquals(PartialDate.Time(2026, 11, 1, 4, 0, VILNIUS), overnight.end)
        assertEquals(party.end, overnight.withEndTimeOfDay(23, 0).end)
    }

    @Test
    fun anEndDaysAfterTheStartKeepsItsDay() {
        val weekend = Occurrence(start = PartialDate.Time(2026, 10, 31, 10, 0, VILNIUS), end = PartialDate.Time(2026, 11, 1, 18, 0, VILNIUS))
        assertEquals(PartialDate.Time(2026, 11, 1, 9, 0, VILNIUS), weekend.withEndTimeOfDay(9, 0).end)
    }

    @Test
    fun coarserPrecisionFillsTheEndFromTheLastDay() {
        val days = Occurrence(start = PartialDate.Day(2026, 10, 3, VILNIUS), end = PartialDate.Day(2026, 11, 5, VILNIUS))
        val months = days.withDatePrecision(PartialDate.Precision.Month)
        assertEquals(PartialDate.Month(2026, 10, VILNIUS), months.start)
        assertEquals(PartialDate.Month(2026, 11, VILNIUS), months.end)
        val backToDays = months.withDatePrecision(PartialDate.Precision.Day)
        assertEquals(PartialDate.Day(2026, 10, 1, VILNIUS), backToDays.start)
        assertEquals(PartialDate.Day(2026, 11, 30, VILNIUS), backToDays.end)
    }

    @Test
    fun dayPrecisionKeepsATime() {
        assertEquals(evening, evening.withDatePrecision(PartialDate.Precision.Day))
    }

    @Test
    fun aDefaultEndIsAnHourLaterOrTheSameDay() {
        assertEquals(PartialDate.Time(2026, 10, 21, 21, 0, VILNIUS), evening.copy(end = null).withDefaultEnd().end)
        val lastHour = Occurrence(start = PartialDate.Time(2026, 10, 21, 23, 15, VILNIUS)).withDefaultEnd()
        assertEquals(PartialDate.Time(2026, 10, 21, 23, 59, VILNIUS), lastHour.end)
        val day = Occurrence(start = PartialDate.Day(2026, 10, 21, VILNIUS)).withDefaultEnd()
        assertEquals(day.start, day.end)
    }

    @Test
    fun timeZoneChangesKeepTheWallClock() {
        assertEquals(PartialDate.Time(2026, 10, 21, 20, 0, "Europe/Berlin"), evening.start!!.withTimeZone("Europe/Berlin"))
    }
}
