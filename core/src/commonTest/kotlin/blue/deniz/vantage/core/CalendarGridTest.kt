package blue.deniz.vantage.core

import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CalendarGridTest {
    private val week = CalendarGrid.weeksOf(LocalDate(2026, 11, 1))[1]

    private fun span(name: String, firstDay: Int, lastDay: Int = firstDay) =
        DaySpan(name, LocalDate(2026, 11, firstDay), LocalDate(2026, 11, lastDay))

    @Test
    fun aSpanCountsItsDaysFromOne() {
        val asWeek = span("AS week", 14, 20)
        assertEquals(7, asWeek.dayCount)
        assertEquals(1, asWeek.dayNumberOf(LocalDate(2026, 11, 14)))
        assertEquals(3, asWeek.dayNumberOf(LocalDate(2026, 11, 16)))
        assertTrue(LocalDate(2026, 11, 20) in asWeek)
        assertTrue(LocalDate(2026, 11, 21) !in asWeek)
    }

    @Test
    fun weeksStartOnMondayAndCoverTheWholeMonth() {
        val weeks = CalendarGrid.weeksOf(LocalDate(2026, 11, 14))
        assertEquals(6, weeks.size)
        assertEquals(LocalDate(2026, 10, 26), weeks.first().first())
        assertEquals(LocalDate(2026, 12, 6), weeks.last().last())
        assertTrue(weeks.all { week -> week.size == 7 && week.first().dayOfWeek == DayOfWeek.MONDAY })
    }

    @Test
    fun aMonthThatFitsInFourWeeksShowsFour() {
        val weeks = CalendarGrid.weeksOf(LocalDate(2027, 2, 1))
        assertEquals(LocalDate(2027, 2, 1), weeks.first().first())
        assertEquals(LocalDate(2027, 2, 28), weeks.last().last())
        assertEquals(4, weeks.size)
    }

    @Test
    fun aMultiDayEventIsOneBarAcrossItsDays() {
        val layout = CalendarGrid.layOutWeek(week, listOf(span("fest", 3, 5)), rows = 3)
        assertEquals(listOf(WeekBar("fest", 1, 3, 0, continuesBefore = false, continuesAfter = false)), layout.bars)
        assertEquals(List(7) { 0 }, layout.hiddenCounts)
    }

    @Test
    fun anEventRunningPastTheWeekIsCutAtItsEdges() {
        val layout = CalendarGrid.layOutWeek(week, listOf(span("trip", 1, 10)), rows = 3)
        assertEquals(listOf(WeekBar("trip", 0, 6, 0, continuesBefore = true, continuesAfter = true)), layout.bars)
    }

    @Test
    fun longerEventsTakeTheTopRowsAndShortOnesFillTheGaps() {
        val layout = CalendarGrid.layOutWeek(week, listOf(span("a", 2), span("b", 2, 4), span("c", 4)), rows = 3)
        val rows = layout.bars.associate { it.item to it.row }
        assertEquals(mapOf("b" to 0, "a" to 1, "c" to 1), rows)
    }

    @Test
    fun aFullDayShowsPlusTheRestInItsLastRow() {
        val spans = listOf(span("a", 3), span("b", 3), span("c", 3), span("d", 3), span("e", 4))
        val layout = CalendarGrid.layOutWeek(week, spans, rows = 3)
        assertEquals(listOf("a", "b", "e"), layout.bars.map { it.item })
        assertEquals(listOf(0, 2, 0, 0, 0, 0, 0), layout.hiddenCounts)
    }

    @Test
    fun aBarInTheLastRowBreaksAroundAFullDay() {
        val spans = listOf(span("a", 2, 4), span("long", 2, 4), span("c", 3))
        val layout = CalendarGrid.layOutWeek(week, spans, rows = 2)
        assertEquals(listOf(0, 2, 0, 0, 0, 0, 0), layout.hiddenCounts)
        val long = layout.bars.filter { it.item == "long" }
        assertEquals(listOf(0 to 0, 2 to 2), long.map { it.firstColumn to it.lastColumn }.sortedBy { it.first })
        assertTrue(long.first { it.firstColumn == 0 }.continuesAfter)
        assertTrue(long.first { it.firstColumn == 2 }.continuesBefore)
    }
}
