package blue.deniz.vantage.app.screens

import androidx.compose.runtime.Composable
import blue.deniz.vantage.app.AppState
import blue.deniz.vantage.app.collectAcrossSession
import blue.deniz.vantage.core.CalendarMarks
import kotlinx.coroutines.flow.combine

private const val CALENDAR_MARKS = "calendar marks"

@Composable
fun rememberCalendarMarks(state: AppState, shown: Boolean): CalendarMarks {
    if (!shown) return CalendarMarks.None
    return rememberLoadedCalendarMarks(state) ?: CalendarMarks.None
}

@Composable
fun rememberLoadedCalendarMarks(state: AppState): CalendarMarks? =
    collectAcrossSession(CALENDAR_MARKS, state.store) {
        combine(state.store.observeMarkers(), state.store.observePeriods(), ::CalendarMarks)
    }
