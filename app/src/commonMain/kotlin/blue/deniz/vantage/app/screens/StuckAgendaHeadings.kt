package blue.deniz.vantage.app.screens

import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.core.Animatable
import blue.deniz.vantage.app.ui.floatingSurface
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.semantics.clearAndSetSemantics
import blue.deniz.vantage.app.ui.FloatingAgendaPeriodChip
import blue.deniz.vantage.core.CalendarPeriod
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.key

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import blue.deniz.vantage.app.ui.AgendaHeadingText
import blue.deniz.vantage.app.ui.AgendaDayLabelText
import blue.deniz.vantage.app.ui.AgendaGroupLabelText
import blue.deniz.vantage.app.ui.AgendaLabelGap
import blue.deniz.vantage.app.ui.AgendaLabelHeight
import blue.deniz.vantage.app.ui.EventFormatting
import blue.deniz.vantage.app.ui.Spacing
import blue.deniz.vantage.app.ui.floatingBorder

@Composable
internal fun StickyAgendaHeading(grid: LazyGridState, agenda: Agenda, topPadding: Dp, cardsStart: Dp, firstReachableRow: () -> Int) {
    val stuckRow by remember(grid, agenda) {
        derivedStateOf {
            val above = grid.layoutInfo.visibleItemsInfo.lastOrNull { it.offset.y < 0 && (agenda.rowIndexOf(it.key) ?: -1) >= firstReachableRow() }
            above?.let { agenda.rowIndexOf(it.key) }?.let(agenda::headingRowOf)
        }
    }
    val row = stuckRow ?: return
    val heading = agenda.rows[row] as? AgendaRow.Heading ?: return
    val pillAlpha = remember(row) { Animatable(0f) }
    LaunchedEffect(row) { pillAlpha.animateTo(1f) }
    Layout(
        content = {
            Box(Modifier.clearAndSetSemantics {}) {
                Box(Modifier.matchParentSize().graphicsLayer { alpha = pillAlpha.value }.floatingSurface(CircleShape).border(floatingBorder(), CircleShape))
                Row(Modifier.padding(horizontal = Spacing.md, vertical = Spacing.xs), verticalAlignment = Alignment.CenterVertically) {
                    AgendaHeadingText(heading.title, heading.isCurrent)
                }
            }
        },
        modifier = Modifier.fillMaxSize(),
    ) { measurables, constraints ->
        val pillStart = (cardsStart + Spacing.xs - Spacing.md).roundToPx()
        val pill = measurables.single().measure(Constraints(maxWidth = (constraints.maxWidth - pillStart * 2).coerceAtLeast(0)))
        layout(constraints.maxWidth, constraints.maxHeight) {
            val next = grid.layoutInfo.visibleItemsInfo.firstOrNull { item ->
                val index = agenda.rowIndexOf(item.key)
                index != null && index > row && agenda.rows[index] is AgendaRow.Heading
            }
            val pushedUp = next?.let { minOf(0, it.offset.y - pill.height) } ?: 0
            pill.place(pillStart, (topPadding + Spacing.xs).roundToPx() + pushedUp)
        }
    }
}

@Composable
internal fun StuckAgendaPeriods(periods: List<CalendarPeriod>, top: Dp, cardsStart: Dp, formatting: EventFormatting, onOpen: (CalendarPeriod) -> Unit) {
    if (periods.isEmpty()) return
    Column(
        Modifier.padding(start = cardsStart, top = top, end = Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        for (period in periods) key(period.id) {
            FloatingAgendaPeriodChip(period, formatting) { onOpen(period) }
        }
    }
}

@Composable
internal fun PinnedAgendaLabel(
    grid: LazyGridState,
    agenda: Agenda,
    pinnedRow: Int?,
    topPadding: Dp,
    cardsStart: Dp,
    formatting: EventFormatting,
    colors: AgendaGuideColors,
) {
    val row = pinnedRow ?: return
    val label = agenda.rows[row] as? AgendaRow.Label ?: return
    val time = agenda.rowTimes[row]
    val pillAlpha = remember(row) { Animatable(0f) }
    LaunchedEffect(row) { pillAlpha.animateTo(1f) }
    val pillStart = DAY_LINE_X - AgendaLabelHeight / 2
    val dotToText = cardsStart + Spacing.xs - DAY_LINE_X - DAY_DOT_RADIUS
    Layout(
        content = {
            Box(Modifier.clearAndSetSemantics {}) {
                Box(Modifier.matchParentSize().graphicsLayer { alpha = pillAlpha.value }.floatingSurface(CircleShape).border(floatingBorder(), CircleShape))
                Row(Modifier.heightIn(min = AgendaLabelHeight).padding(end = dotToText), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(width = cardsStart + Spacing.xs - pillStart, height = AgendaLabelHeight).drawBehind {
                            drawDayDot(time, Offset((DAY_LINE_X - pillStart).toPx(), size.height / 2), colors, solid = true)
                        },
                    )
                    when (label) {
                        is AgendaRow.Day -> AgendaDayLabelText(label.date, label.isToday, formatting)
                        is AgendaRow.Group -> AgendaGroupLabelText(label.title)
                    }
                }
            }
        },
        modifier = Modifier.fillMaxSize(),
    ) { measurables, constraints ->
        val pill = measurables.single().measure(Constraints(maxWidth = constraints.maxWidth))
        layout(constraints.maxWidth, constraints.maxHeight) {
            val next = grid.layoutInfo.visibleItemsInfo.firstOrNull { item ->
                val index = agenda.rowIndexOf(item.key)
                index != null && index > row && agenda.rows[index] is AgendaRow.Label
            }
            val pushedUp = next?.let { minOf(0, it.offset.y + AgendaLabelGap.roundToPx() - pill.height) } ?: 0
            pill.place(pillStart.roundToPx(), topPadding.roundToPx() + pushedUp)
        }
    }
}
