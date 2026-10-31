package blue.deniz.vantage.core

import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus

data class DaySpan<T>(val item: T, val first: LocalDate, val last: LocalDate) {
    val dayCount: Int get() = first.daysUntil(last) + 1

    operator fun contains(day: LocalDate) = day in first..last

    fun dayNumberOf(day: LocalDate): Int = first.daysUntil(day) + 1
}

data class WeekBar<T>(
    val item: T,
    val firstColumn: Int,
    val lastColumn: Int,
    val row: Int,
    val continuesBefore: Boolean,
    val continuesAfter: Boolean,
)

data class WeekLayout<T>(val bars: List<WeekBar<T>>, val hiddenCounts: List<Int>)

object CalendarGrid {
    const val DAYS_IN_WEEK = 7

    fun weeksOf(month: LocalDate): List<List<LocalDate>> {
        val first = LocalDate(month.year, month.month, 1)
        val start = first.minus(DatePeriod(days = first.dayOfWeek.isoDayNumber - DayOfWeek.MONDAY.isoDayNumber))
        return generateSequence(start) { it.plus(DatePeriod(days = DAYS_IN_WEEK)) }
            .takeWhile { weekStart -> weekStart == start || weekStart.month == first.month }
            .map { weekStart -> (0 until DAYS_IN_WEEK).map { weekStart.plus(DatePeriod(days = it)) } }
            .toList()
    }

    fun <T> layOutWeek(week: List<LocalDate>, spans: List<DaySpan<T>>, rows: Int): WeekLayout<T> {
        val placed = spans
            .filter { it.last >= week.first() && it.first <= week.last() }
            .map { span -> span to (columnOf(week, span.first)..columnOf(week, span.last)) }
            .sortedWith(compareBy<Pair<DaySpan<T>, IntRange>> { it.second.first }.thenByDescending { it.second.last - it.second.first })
        val taken = mutableListOf<BooleanArray>()
        val rowOf = placed.map { (_, columns) ->
            val row = taken.indexOfFirst { used -> columns.none { used[it] } }.takeIf { it >= 0 }
                ?: taken.size.also { taken += BooleanArray(DAYS_IN_WEEK) }
            columns.forEach { taken[row][it] = true }
            row
        }
        val overflowing = (0 until DAYS_IN_WEEK).map { column ->
            placed.indices.any { column in placed[it].second && rowOf[it] >= rows }
        }
        val shownRows = overflowing.map { if (it) rows - 1 else rows }
        val hiddenCounts = (0 until DAYS_IN_WEEK).map { column ->
            if (!overflowing[column]) 0 else placed.indices.count { column in placed[it].second && rowOf[it] >= shownRows[column] }
        }
        val bars = placed.indices.flatMap { index ->
            val (span, columns) = placed[index]
            val row = rowOf[index]
            columns.filter { row < shownRows[it] }.runs().map { run ->
                WeekBar(
                    item = span.item,
                    firstColumn = run.first,
                    lastColumn = run.last,
                    row = row,
                    continuesBefore = span.first < week[run.first],
                    continuesAfter = span.last > week[run.last],
                )
            }
        }
        return WeekLayout(bars, hiddenCounts)
    }

    private fun columnOf(week: List<LocalDate>, day: LocalDate): Int = when {
        day < week.first() -> 0
        day > week.last() -> week.lastIndex
        else -> week.indexOf(day)
    }

    private fun List<Int>.runs(): List<IntRange> = fold(mutableListOf()) { runs, column ->
        val last = runs.lastOrNull()
        if (last != null && last.last + 1 == column) runs[runs.lastIndex] = last.first..column else runs += column..column
        runs
    }
}
