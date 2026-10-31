package directory.evnt.convert

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.number
import kotlinx.datetime.plus

private const val MAX_PERIODS = 20_000

internal data class WeekdayRule(val ordinal: Int?, val day: DayOfWeek)

internal data class RecurrenceRule(
    val frequency: Frequency,
    val interval: Int = 1,
    val count: Int? = null,
    val lastDate: LocalDate? = null,
    val byDay: List<WeekdayRule> = emptyList(),
    val byMonthDay: List<Int> = emptyList(),
    val byMonth: List<Int> = emptyList(),
) {
    enum class Frequency { Daily, Weekly, Monthly, Yearly }

    fun datesFrom(start: LocalDate): Sequence<LocalDate> = sequence {
        var emitted = 0
        for (period in 0 until MAX_PERIODS) {
            for (date in datesInPeriod(start, period).filter { it >= start }.distinct().sorted()) {
                if (lastDate != null && date > lastDate) return@sequence
                if (count != null && emitted >= count) return@sequence
                yield(date)
                emitted++
            }
        }
    }

    private fun datesInPeriod(start: LocalDate, period: Int): List<LocalDate> {
        val step = period * interval
        val dates = when (frequency) {
            Frequency.Daily -> listOf(start.plus(step, DateTimeUnit.DAY))
            Frequency.Weekly -> {
                val weekStart = start.plus(1 - start.dayOfWeek.isoDayNumber, DateTimeUnit.DAY).plus(step, DateTimeUnit.WEEK)
                byDay.map { it.day }.ifEmpty { listOf(start.dayOfWeek) }.map { weekStart.plus(it.isoDayNumber - 1, DateTimeUnit.DAY) }
            }
            Frequency.Monthly -> {
                val month = LocalDate(start.year, start.month, 1).plus(step, DateTimeUnit.MONTH)
                datesInMonth(month.year, month.month.number, start)
            }
            Frequency.Yearly -> byMonth.ifEmpty { listOf(start.month.number) }.flatMap { datesInMonth(start.year + step, it, start) }
        }
        return if (byMonth.isEmpty() || frequency == Frequency.Yearly) dates else dates.filter { it.month.number in byMonth }
    }

    private fun datesInMonth(year: Int, month: Int, start: LocalDate): List<LocalDate> {
        val first = LocalDate(year, month, 1)
        val length = first.plus(1, DateTimeUnit.MONTH).toEpochDays() - first.toEpochDays()
        val days = (1..length.toInt()).map { LocalDate(year, month, it) }
        return when {
            byMonthDay.isNotEmpty() -> byMonthDay.mapNotNull { day -> days.getOrNull(if (day > 0) day - 1 else days.size + day) }
            byDay.isNotEmpty() -> byDay.flatMap { rule ->
                val matching = days.filter { it.dayOfWeek == rule.day }
                when {
                    rule.ordinal == null -> matching
                    rule.ordinal > 0 -> listOfNotNull(matching.getOrNull(rule.ordinal - 1))
                    else -> listOfNotNull(matching.getOrNull(matching.size + rule.ordinal))
                }
            }
            else -> listOfNotNull(days.getOrNull(start.day - 1))
        }
    }

    companion object {
        private val WEEKDAY = Regex("""([+-]?\d{1,2})?(MO|TU|WE|TH|FR|SA|SU)""")
        private val WEEKDAYS = mapOf(
            "MO" to DayOfWeek.MONDAY, "TU" to DayOfWeek.TUESDAY, "WE" to DayOfWeek.WEDNESDAY, "TH" to DayOfWeek.THURSDAY,
            "FR" to DayOfWeek.FRIDAY, "SA" to DayOfWeek.SATURDAY, "SU" to DayOfWeek.SUNDAY,
        )

        fun parse(value: String, parseDate: (String) -> LocalDate?): RecurrenceRule? {
            val parts = value.split(';').associate { it.substringBefore('=').trim().uppercase() to it.substringAfter('=', "").trim() }
            val frequency = when (parts["FREQ"]?.uppercase()) {
                "DAILY" -> Frequency.Daily
                "WEEKLY" -> Frequency.Weekly
                "MONTHLY" -> Frequency.Monthly
                "YEARLY" -> Frequency.Yearly
                else -> return null
            }
            fun numbers(key: String) = parts[key]?.split(',')?.mapNotNull { it.trim().toIntOrNull() }.orEmpty()
            return RecurrenceRule(
                frequency = frequency,
                interval = parts["INTERVAL"]?.toIntOrNull()?.coerceAtLeast(1) ?: 1,
                count = parts["COUNT"]?.toIntOrNull(),
                lastDate = parts["UNTIL"]?.let(parseDate),
                byDay = parts["BYDAY"]?.split(',')?.mapNotNull { day ->
                    WEEKDAY.matchEntire(day.trim().uppercase())?.destructured?.let { (ordinal, name) ->
                        WeekdayRule(ordinal.takeIf { it.isNotEmpty() }?.removePrefix("+")?.toInt(), WEEKDAYS.getValue(name))
                    }
                }.orEmpty(),
                byMonthDay = numbers("BYMONTHDAY"),
                byMonth = numbers("BYMONTH").filter { it in 1..12 },
            )
        }
    }
}
