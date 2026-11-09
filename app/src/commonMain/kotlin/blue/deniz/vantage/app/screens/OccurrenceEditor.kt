package blue.deniz.vantage.app.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerSelectionMode
import androidx.compose.material3.TimePickerState
import androidx.compose.material3.DatePickerState
import blue.deniz.vantage.app.mondayFirstCalendarLocale
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusTarget
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.utf16CodePoint
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import blue.deniz.vantage.app.ui.AppIcons
import blue.deniz.vantage.app.ui.BottomSheetScope
import blue.deniz.vantage.app.ui.Button
import blue.deniz.vantage.app.ui.EventFormatting
import blue.deniz.vantage.app.ui.IconButton
import blue.deniz.vantage.app.ui.ListRowHeight
import blue.deniz.vantage.app.ui.Radius
import blue.deniz.vantage.app.ui.RemoveButton
import blue.deniz.vantage.app.ui.SegmentedControl
import blue.deniz.vantage.app.ui.Spacing
import blue.deniz.vantage.app.ui.FieldFrame
import blue.deniz.vantage.app.ui.InputShape
import blue.deniz.vantage.app.ui.inputOutline
import blue.deniz.vantage.app.ui.Text
import blue.deniz.vantage.app.ui.SheetTitle
import blue.deniz.vantage.core.TypedTimeOfDay
import blue.deniz.vantage.core.localDateOrNull
import blue.deniz.vantage.core.withDatePrecision
import blue.deniz.vantage.core.withDefaultEnd
import blue.deniz.vantage.core.withLocalDate
import blue.deniz.vantage.core.withStart
import blue.deniz.vantage.core.withEndTimeOfDay
import blue.deniz.vantage.core.withTimeOfDay
import blue.deniz.vantage.core.withTimeZone
import blue.deniz.vantage.core.withoutTimeOfDay
import directory.evnt.Occurrence
import directory.evnt.PartialDate
import kotlin.time.Instant
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.format.MonthNames
import kotlinx.datetime.toLocalDateTime

private val TIME_BOX_WIDTH = 116.dp
private val MONTH_BOX_HEIGHT = 48.dp
private const val MONTHS_PER_ROW = 3

private val PRECISION_OPTIONS = listOf(
    PartialDate.Precision.Day to "Day",
    PartialDate.Precision.Month to "Month",
    PartialDate.Precision.Year to "Year",
)

private enum class DateEnd { Start, End }

private fun PartialDate.shownPrecision() = if (precision == PartialDate.Precision.Time) PartialDate.Precision.Day else precision

private fun EventFormatting.wallClockTime(date: PartialDate) = (date as? PartialDate.Time)?.let { formatClock(LocalTime(it.hour, it.minute)) }

private fun EventFormatting.wallClockDate(date: PartialDate) = date.localDateOrNull()?.let(::formatDay) ?: formatDate(date)

@Composable
fun BottomSheetScope.OccurrenceEditor(
    occurrence: Occurrence,
    formatting: EventFormatting,
    defaultTimeZoneId: String,
    now: Instant,
    onChange: (Occurrence) -> Unit,
    onDelete: () -> Unit,
    onDone: () -> Unit,
) {
    val timeZoneId = occurrence.start?.timeZoneId ?: defaultTimeZoneId
    val start = occurrence.start ?: PartialDate.Day(now.toLocalDateTime(TimeZone.of(timeZoneId)).date, timeZoneId)
    var pickingTimeZone by rememberSaveable { mutableStateOf(false) }
    if (pickingTimeZone) {
        TimeZonePickerBody("Time Zone", timeZoneId, now, onBack = { pickingTimeZone = false }) { picked ->
            pickingTimeZone = false
            onChange(occurrence.copy(start = start.withTimeZone(picked), end = occurrence.end?.withTimeZone(picked)))
        }
        return
    }
    ScrollingBody {
        Column(Modifier.fillMaxWidth().padding(horizontal = Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
            OccurrenceForm(occurrence, start, timeZoneId, formatting, onChange, onDelete, onDone, onPickTimeZone = { pickingTimeZone = true })
        }
    }
}

@Composable
private fun ColumnScope.OccurrenceForm(
    occurrence: Occurrence,
    start: PartialDate,
    timeZoneId: String,
    formatting: EventFormatting,
    onChange: (Occurrence) -> Unit,
    onDelete: () -> Unit,
    onDone: () -> Unit,
    onPickTimeZone: () -> Unit,
) {
    val end = occurrence.end
    var pickingDate by rememberSaveable { mutableStateOf<DateEnd?>(null) }
    var pickingTime by rememberSaveable { mutableStateOf<DateEnd?>(null) }

    fun changed(which: DateEnd, date: PartialDate) {
        onChange(if (which == DateEnd.Start) occurrence.withStart(date) else occurrence.copy(start = start, end = date))
    }

    fun valueOf(which: DateEnd) = if (which == DateEnd.Start) start else end

    val dateTarget = pickingDate
    val dateValue = dateTarget?.let(::valueOf)
    if (dateTarget != null && dateValue != null) {
        val close = { pickingDate = null }
        val title = if (dateTarget == DateEnd.Start) "Start Date" else "End Date"
        when (dateValue.shownPrecision()) {
            PartialDate.Precision.Day -> DayPicker(title, dateValue.localDateOrNull()!!, onBack = close) {
                close()
                changed(dateTarget, dateValue.withLocalDate(it))
            }
            PartialDate.Precision.Month -> MonthPicker(title, dateValue.year, (dateValue as PartialDate.Month).month, onBack = close) { year, month ->
                close()
                changed(dateTarget, PartialDate.Month(year, month ?: 1, dateValue.timeZoneId))
            }
            else -> MonthPicker(title, dateValue.year, null, onBack = close) { year, _ ->
                close()
                changed(dateTarget, PartialDate.Year(year, dateValue.timeZoneId))
            }
        }
        return
    }

    val timeTarget = pickingTime
    val timeValue = timeTarget?.let(::valueOf)
    if (timeTarget != null && timeValue != null) {
        val current = timeValue as? PartialDate.Time
        val startTime = start as? PartialDate.Time
        TimePickerPage(
            title = if (timeTarget == DateEnd.Start) "Start Time" else "End Time",
            hour = current?.hour ?: startTime?.hour?.plus(1)?.coerceAtMost(23) ?: DEFAULT_START_HOUR,
            minute = current?.minute ?: startTime?.minute ?: 0,
            onBack = { pickingTime = null },
        ) { hour, minute ->
            pickingTime = null
            if (timeTarget == DateEnd.End) onChange(occurrence.copy(start = start).withEndTimeOfDay(hour, minute))
            else changed(timeTarget, timeValue.withTimeOfDay(hour, minute))
        }
        return
    }

    SheetTitle(formatting.occurrenceText(occurrence.copy(start = start)).headline)
    SegmentedControl(
        PRECISION_OPTIONS,
        selected = start.shownPrecision(),
        onSelect = { onChange(occurrence.copy(start = start).withDatePrecision(it)) },
    )
    DateRow(
        "Starts",
        start,
        formatting,
        onPickDate = { pickingDate = DateEnd.Start },
        onPickTime = { pickingTime = DateEnd.Start },
        onClearTime = { onChange(occurrence.copy(start = start.withoutTimeOfDay(), end = end?.withoutTimeOfDay())) },
    )
    if (end == null) {
        PickerBox("Ends", "Add End", onClick = { onChange(occurrence.copy(start = start).withDefaultEnd()) }, placeholder = true)
    } else {
        DateRow(
            "Ends",
            end,
            formatting,
            onPickDate = { pickingDate = DateEnd.End },
            onPickTime = { pickingTime = DateEnd.End },
            onClearTime = { onChange(occurrence.copy(end = end.withoutTimeOfDay())) },
            onRemove = { onChange(occurrence.copy(end = null)) },
        )
    }
    PickerBox("Time Zone", timeZoneId, onClick = onPickTimeZone)
    Row(verticalAlignment = Alignment.CenterVertically) {
        TextButton(onClick = onDelete) { Text("Remove Date", color = MaterialTheme.colorScheme.error) }
        Spacer(Modifier.weight(1f))
        Button("Done", onClick = onDone, primary = true)
    }
}

private const val DEFAULT_START_HOUR = 18

@Composable
internal fun DateRow(
    label: String,
    date: PartialDate,
    formatting: EventFormatting,
    onPickDate: () -> Unit,
    onPickTime: () -> Unit,
    onClearTime: () -> Unit,
    onRemove: (() -> Unit)? = null,
    isError: Boolean = false,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        PickerBox(label, formatting.wallClockDate(date), onPickDate, Modifier.weight(1f), onClear = onRemove, isError = isError)
        if (date.shownPrecision() == PartialDate.Precision.Day) {
            val time = formatting.wallClockTime(date)
            PickerBox(
                "Time",
                time ?: "Add Time",
                onPickTime,
                Modifier.width(TIME_BOX_WIDTH),
                placeholder = time == null,
                onClear = if (time != null) onClearTime else null,
            )
        }
    }
}

@Composable
internal fun PickerBox(
    label: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    placeholder: Boolean = false,
    onClear: (() -> Unit)? = null,
    isError: Boolean = false,
) {
    FieldFrame(label, modifier.fillMaxWidth(), isError = isError) {
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = ListRowHeight)
                .inputOutline(focused = false, isError = isError)
                .clip(InputShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .clickable(onClick = onClick)
                .padding(start = Spacing.sm, end = if (onClear == null) Spacing.sm else 0.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                value,
                Modifier.weight(1f),
                color = if (placeholder) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                style = MaterialTheme.typography.bodyLarge,
            )
            if (onClear != null) RemoveButton("Clear $label", onClear)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DayPicker(title: String, initial: LocalDate, onBack: () -> Unit, onPick: (LocalDate) -> Unit) {
    val initialMillis = initial.atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()
    val pickerState = remember { DatePickerState(locale = mondayFirstCalendarLocale, initialSelectedDateMillis = initialMillis) }
    val currentOnPick by rememberUpdatedState(onPick)
    LaunchedEffect(pickerState) {
        snapshotFlow { pickerState.selectedDateMillis }
            .filterNotNull()
            .filter { it != initialMillis }
            .collect { currentOnPick(Instant.fromEpochMilliseconds(it).toLocalDateTime(TimeZone.UTC).date) }
    }
    PickerHeader(title, onBack)
    DatePicker(pickerState, title = null, headline = null, showModeToggle = false, colors = DatePickerDefaults.colors(containerColor = BottomSheetDefaults.ContainerColor))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ColumnScope.TimePickerPage(title: String, hour: Int, minute: Int, onBack: () -> Unit, onPick: (hour: Int, minute: Int) -> Unit) {
    var pickerState by remember { mutableStateOf(TimePickerState(hour, minute, is24Hour = true)) }
    val focus = remember { FocusRequester() }
    var typed by remember { mutableStateOf<TypedTimeOfDay?>(null) }
    var selectionToApply by remember { mutableStateOf<TimePickerSelectionMode?>(null) }
    LaunchedEffect(focus) { focus.requestFocus() }
    LaunchedEffect(pickerState) {
        selectionToApply?.let { pickerState.selection = it }
        selectionToApply = null
    }

    fun onKey(event: KeyEvent): Boolean {
        if (event.type != KeyEventType.KeyDown) return false
        if (event.key == Key.Enter || event.key == Key.NumPadEnter) {
            onPick(pickerState.hour, pickerState.minute)
            return true
        }
        val selection = selectionToApply ?: pickerState.selection
        val shown = TypedTimeOfDay(pickerState.hour, pickerState.minute, typingMinute = selection == TimePickerSelectionMode.Minute)
        val current = typed?.takeIf { it.copy(firstDigit = null) == shown } ?: shown
        val char = event.utf16CodePoint.toChar()
        val next = when {
            char.isDigit() -> current.plusDigit(char.digitToInt())
            char == ':' -> current.typingMinuteNext()
            else -> return false
        }
        typed = next
        pickerState = TimePickerState(next.hour, next.minute, is24Hour = true)
        selectionToApply = TimePickerSelectionMode.Minute.takeIf { next.typingMinute }
        return true
    }

    PickerHeader(title, onBack) {
        Button("Set", onClick = { onPick(pickerState.hour, pickerState.minute) }, primary = true)
    }
    TimePicker(pickerState, Modifier.align(Alignment.CenterHorizontally).onKeyEvent(::onKey).focusRequester(focus).focusTarget())
}

@Composable
private fun ColumnScope.MonthPicker(title: String, initialYear: Int, initialMonth: Int?, onBack: () -> Unit, onPick: (year: Int, month: Int?) -> Unit) {
    var year by remember { mutableIntStateOf(initialYear) }
    PickerHeader(title, onBack) {
        if (initialMonth == null) Button("Set", onClick = { onPick(year, null) }, primary = true)
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(AppIcons.ChevronLeft, "Previous Year", onClick = { year-- })
        Text(year.toString(), Modifier.weight(1f), textAlign = TextAlign.Center, style = MaterialTheme.typography.titleLarge)
        IconButton(AppIcons.ChevronRight, "Next Year", onClick = { year++ })
    }
    if (initialMonth == null) return
    for (row in MonthNames.ENGLISH_ABBREVIATED.names.withIndex().chunked(MONTHS_PER_ROW)) {
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            for ((index, name) in row) {
                val month = index + 1
                val selected = year == initialYear && month == initialMonth
                Text(
                    name,
                    Modifier
                        .weight(1f)
                        .heightIn(min = MONTH_BOX_HEIGHT)
                        .clip(RoundedCornerShape(Radius.md))
                        .background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh)
                        .clickable { onPick(year, month) }
                        .padding(vertical = Spacing.md),
                    color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
