package blue.deniz.vantage.app.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import blue.deniz.vantage.app.AppState
import blue.deniz.vantage.app.ui.AppIcons
import blue.deniz.vantage.app.ui.ListRow
import blue.deniz.vantage.core.CalendarPeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil

fun daysAwayText(today: LocalDate, day: LocalDate): String = when (val days = today.daysUntil(day)) {
    -1 -> "Yesterday"
    0 -> "Today"
    1 -> "Tomorrow"
    else -> if (days > 0) "In $days days" else "${-days} days ago"
}

fun periodProgressText(day: LocalDate, firstDay: LocalDate, lastDay: LocalDate): String = when {
    day < firstDay -> "Starts ${daysAwayText(day, firstDay).lowercase()}"
    day > lastDay -> "Ended ${daysAwayText(day, lastDay).lowercase()}"
    else -> "Day ${firstDay.daysUntil(day) + 1} of ${firstDay.daysUntil(lastDay) + 1}"
}

@Composable
fun PeriodsSection(state: AppState, periods: List<CalendarPeriod>, day: LocalDate?, beforeOpening: () -> Unit) {
    if (periods.isEmpty()) return
    val formatting = state.formatting
    Column {
        for (period in periods) {
            val first = formatting.localDateOrNull(period.start)
            val last = formatting.localDateOrNull(period.end)
            val supporting = if (first == null || last == null) null else {
                listOfNotNull(formatting.formatDayRange(first, last), day?.let { periodProgressText(it, first, last) }).joinToString(" · ")
            }
            ListRow(period.label, icon = AppIcons.DateRange, supporting = supporting, onClick = {
                beforeOpening()
                state.openMarkSheet(MarkSheetTarget(MarkKind.Period, period.id))
            })
        }
    }
}
