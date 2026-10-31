package blue.deniz.vantage.app.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import blue.deniz.vantage.app.AppState
import blue.deniz.vantage.app.Route
import blue.deniz.vantage.app.SystemBackHandler
import blue.deniz.vantage.app.ui.AppIcons
import blue.deniz.vantage.app.ui.BottomSheetFrame
import blue.deniz.vantage.app.ui.Button
import blue.deniz.vantage.app.ui.EventFormatting
import blue.deniz.vantage.app.ui.FailureRow
import blue.deniz.vantage.app.ui.MenuItem
import blue.deniz.vantage.app.ui.SegmentedControl
import blue.deniz.vantage.app.ui.Spacing
import blue.deniz.vantage.app.ui.Text
import blue.deniz.vantage.app.ui.SheetTitle
import blue.deniz.vantage.app.ui.TextInput
import blue.deniz.vantage.core.CalendarMarker
import blue.deniz.vantage.core.CalendarMarks
import blue.deniz.vantage.core.CalendarPeriod
import blue.deniz.vantage.core.localDateOrNull
import blue.deniz.vantage.core.movedWithStart
import blue.deniz.vantage.core.withLocalDate
import blue.deniz.vantage.core.withTimeOfDay
import blue.deniz.vantage.core.withoutTimeOfDay
import directory.evnt.PartialDate
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import blue.deniz.vantage.app.EventsView
import kotlinx.datetime.plus
import kotlinx.datetime.daysUntil
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.DateTimeUnit
import directory.evnt.translate
import blue.deniz.vantage.app.ui.shownOccurrence
import blue.deniz.vantage.app.ui.ListSection
import blue.deniz.vantage.app.ui.ListRow
import blue.deniz.vantage.app.collectAcrossSession
import blue.deniz.vantage.storage.EventListQuery
import kotlinx.datetime.todayIn
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

enum class MarkKind(val label: String) { Marker("Marker"), Period("Period") }

data class MarkDraft(val label: String, val start: PartialDate, val end: PartialDate?) {
    val endsBeforeStart: Boolean get() = end != null && end.endInstant <= start.startInstant
}

class MarkSheetTarget(val kind: MarkKind, val id: String?, val day: LocalDate? = null, val draft: MarkDraft? = null)

private const val DEFAULT_MARK_HOUR = 23
private const val DEFAULT_MARK_MINUTE = 59

private enum class MarkDateField(val title: String) { StartDate("Date"), StartTime("Time"), EndDate("End Date"), EndTime("End Time") }

@Stable
private class MarkEditor(target: MarkSheetTarget, today: LocalDate, timeZoneId: String) {
    val kind = target.kind
    var draft by mutableStateOf(target.draft ?: if (target.id == null) newDraft(target.kind, target.day ?: today, timeZoneId) else null)
    var editedFrom by mutableStateOf<MarkDraft?>(null)
    var picking by mutableStateOf<MarkDateField?>(null)
    var problem by mutableStateOf<String?>(null)
    var showProblems by mutableStateOf(false)
    var saving by mutableStateOf(false)

    fun hasUnsavedChanges(isNew: Boolean): Boolean {
        val shown = draft ?: return false
        return if (isNew) shown.label.isNotBlank() else shown != editedFrom
    }

    fun edit(stored: MarkDraft) {
        editedFrom = stored
        draft = stored
    }

    fun stopEditing() {
        draft = null
        editedFrom = null
        picking = null
        problem = null
        showProblems = false
    }

    private companion object {
        fun newDraft(kind: MarkKind, day: LocalDate, timeZoneId: String): MarkDraft {
            val start = PartialDate.Day(day, timeZoneId)
            return MarkDraft("", start, if (kind == MarkKind.Period) start else null)
        }
    }
}

@Composable
fun MarkSheet(state: AppState) {
    val target = state.markSheetTarget
    val editor = remember(target) { target?.let { MarkEditor(it, state.clock.todayIn(state.timeZone), state.timeZone.id) } }
    BottomSheetFrame(
        visible = state.markSheetOpen,
        onDismiss = {
            state.closeMarkSheet()
            if (target != null && editor != null && editor.hasUnsavedChanges(isNew = target.id == null)) {
                val discarded = MarkSheetTarget(editor.kind, target.id, target.day, editor.draft)
                state.offerUndo("Draft Discarded") { state.openMarkSheet(discarded) }
            }
        },
    ) {
        if (target != null && editor != null) key(target) { ScrollingBody { MarkSheetBody(state, target, editor) } }
    }
}

@Composable
private fun ColumnScope.MarkSheetBody(state: AppState, target: MarkSheetTarget, editor: MarkEditor) {
    val id = target.id
    if (id == null) return MarkForm(state, target, editor)
    val marks = rememberLoadedCalendarMarks(state) ?: return
    val stored = marks.draftOf(target.kind, id)
    when {
        editor.draft != null -> MarkForm(state, target, editor)
        stored == null -> Text(
            "This ${target.kind.label.lowercase()} is gone",
            Modifier.padding(horizontal = Spacing.md),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        else -> MarkView(state, marks, target.kind, id, stored, onEdit = { editor.edit(stored) })
    }
}

private fun CalendarMarks.draftOf(kind: MarkKind, id: String): MarkDraft? = when (kind) {
    MarkKind.Marker -> markers.find { it.id == id }?.let { MarkDraft(it.label, it.at, null) }
    MarkKind.Period -> periods.find { it.id == id }?.let { MarkDraft(it.label, it.start, it.end) }
}

private fun EventFormatting.markDateText(start: PartialDate, end: PartialDate?): String {
    fun timeOf(date: PartialDate) = (date as? PartialDate.Time)?.let(::formatTime)
    if (end == null) return listOfNotNull(formatLongDate(start), timeOf(start)).joinToString(" · ")
    fun point(date: PartialDate) = listOfNotNull(formatDate(date), timeOf(date)).joinToString(" ")
    return "${point(start)} – ${point(end)}"
}

@Composable
private fun MarkView(state: AppState, marks: CalendarMarks, kind: MarkKind, id: String, stored: MarkDraft, onEdit: () -> Unit) {
    val scope = rememberCoroutineScope()
    val formatting = state.formatting
    val today = state.today
    val firstDay = formatting.localDateOrNull(stored.start)
    val lastDay = stored.end?.let(formatting::localDateOrNull) ?: firstDay
    Column(Modifier.fillMaxWidth().padding(horizontal = Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        SheetTitle(stored.label)
        Text(kind.label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
    }
    ListSection {
        val end = stored.end
        if (end == null) {
            ListRow(formatting.markDateText(stored.start, null), icon = AppIcons.Calendar, supporting = firstDay?.let { daysAwayText(today, it) })
        } else {
            ListRow(formatting.markDateText(stored.start, null), icon = AppIcons.Calendar, supporting = "Starts")
            ListRow(formatting.markDateText(end, null), icon = AppIcons.Calendar, supporting = "Ends")
            if (firstDay != null && lastDay != null) {
                val days = firstDay.daysUntil(lastDay) + 1
                ListRow(if (days == 1) "1 day" else "$days days", icon = AppIcons.Clock, supporting = periodProgressText(today, firstDay, lastDay))
            }
        }
    }
    if (kind == MarkKind.Marker) PeriodsSection(state, marks.periodsOn(stored.start), firstDay, beforeOpening = state::closeMarkSheet)
    if (firstDay != null && lastDay != null) {
        EventsSection(state, if (kind == MarkKind.Marker) "That Day" else "During These Days", firstDay, lastDay, exceptEventId = null, beforeOpening = state::closeMarkSheet)
    }
    Column {
        MenuItem("Edit", onClick = onEdit, leadingIcon = AppIcons.Pencil)
        MenuItem("Delete", onClick = {
            state.closeMarkSheet()
            scope.launch {
                when (kind) {
                    MarkKind.Marker -> state.store.removeMarker(id)?.let { removed -> state.offerUndo("Marker Deleted") { state.store.saveMarker(removed) } }
                    MarkKind.Period -> state.store.removePeriod(id)?.let { removed -> state.offerUndo("Period Deleted") { state.store.savePeriod(removed) } }
                }
            }
        }, leadingIcon = AppIcons.Trash, danger = true)
    }
}

@OptIn(ExperimentalUuidApi::class)
private fun newMarkId() = Uuid.random().toString()

@Composable
private fun ColumnScope.MarkForm(state: AppState, target: MarkSheetTarget, editor: MarkEditor) {
    val draft = editor.draft ?: return
    val isNew = target.id == null
    val scope = rememberCoroutineScope()
    val nameFocus = remember { FocusRequester() }

    fun valueOf(field: MarkDateField) = if (field == MarkDateField.StartDate || field == MarkDateField.StartTime) draft.start else checkNotNull(draft.end)

    fun changed(field: MarkDateField, date: PartialDate) {
        editor.draft = if (field == MarkDateField.StartDate || field == MarkDateField.StartTime) {
            draft.copy(start = date, end = draft.end?.movedWithStart(draft.start, date))
        } else {
            draft.copy(end = date)
        }
    }

    fun submit() {
        if (editor.saving) return
        val submitted = editor.draft ?: return
        val label = submitted.label.trim()
        val end = submitted.end
        if (label.isEmpty() || submitted.endsBeforeStart) {
            editor.showProblems = true
            if (label.isEmpty()) nameFocus.requestFocus()
            return
        }
        editor.problem = null
        editor.saving = true
        scope.launch {
            try {
                val id = target.id ?: newMarkId()
                when (editor.kind) {
                    MarkKind.Marker -> state.store.saveMarker(CalendarMarker(id, label, submitted.start))
                    MarkKind.Period -> state.store.savePeriod(CalendarPeriod(id, label, submitted.start, checkNotNull(end)))
                }
                if (isNew) state.closeMarkSheet() else editor.stopEditing()
            } catch (error: IllegalArgumentException) {
                editor.problem = error.message
            } finally {
                editor.saving = false
            }
        }
    }

    SystemBackHandler(enabled = !isNew) { editor.stopEditing() }
    SystemBackHandler(enabled = editor.picking != null) { editor.picking = null }
    LaunchedEffect(Unit) { if (isNew && draft.label.isEmpty()) nameFocus.requestFocus() }

    Row(Modifier.fillMaxWidth().padding(horizontal = Spacing.md), verticalAlignment = Alignment.CenterVertically) {
        SheetTitle(if (isNew) "New ${editor.kind.label}" else editor.kind.label, Modifier.weight(1f))
        if (!isNew) TextButton(onClick = editor::stopEditing) { Text("Cancel") }
        Button("Save", onClick = ::submit, primary = true, busy = editor.saving)
    }
    editor.problem?.let { FailureRow(it, onRetry = null, Modifier.padding(horizontal = Spacing.md)) }
    Column(Modifier.fillMaxWidth().padding(horizontal = Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
        val field = editor.picking
        if (field != null) {
            val value = valueOf(field)
            val close = { editor.picking = null }
            if (field == MarkDateField.StartDate || field == MarkDateField.EndDate) {
                DayPicker(field.title, value.localDateOrNull()!!, onBack = close) {
                    close()
                    changed(field, value.withLocalDate(it))
                }
            } else {
                val time = value as? PartialDate.Time
                TimePickerPage(field.title, time?.hour ?: DEFAULT_MARK_HOUR, time?.minute ?: DEFAULT_MARK_MINUTE, onBack = close) { hour, minute ->
                    close()
                    changed(field, value.withTimeOfDay(hour, minute))
                }
            }
            return@Column
        }
        val nameMissing = editor.showProblems && draft.label.isBlank()
        TextInput(
            draft.label,
            { editor.draft = draft.copy(label = it) },
            Modifier.fillMaxWidth().focusRequester(nameFocus),
            label = "Name",
            placeholder = "Give it a name".takeIf { nameMissing },
            isError = nameMissing,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { submit() }),
        )
        DateRow(
            if (draft.end == null) "Date" else "Starts",
            draft.start,
            state.formatting,
            onPickDate = { editor.picking = MarkDateField.StartDate },
            onPickTime = { editor.picking = MarkDateField.StartTime },
            onClearTime = { editor.draft = draft.copy(start = draft.start.withoutTimeOfDay()) },
        )
        val end = draft.end
        if (end != null) {
            val endMisplaced = editor.showProblems && draft.endsBeforeStart
            DateRow(
                if (endMisplaced) "Ends before the start" else "Ends",
                end,
                state.formatting,
                onPickDate = { editor.picking = MarkDateField.EndDate },
                onPickTime = { editor.picking = MarkDateField.EndTime },
                onClearTime = { editor.draft = draft.copy(end = end.withoutTimeOfDay()) },
                isError = endMisplaced,
            )
        }
    }
}
