package blue.deniz.vantage.app.screens

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.ceil
import androidx.compose.foundation.lazy.grid.LazyGridLayoutInfo
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import blue.deniz.vantage.app.ui.AgendaLabelGap
import blue.deniz.vantage.app.ui.AgendaIconSize
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.painter.Painter
import kotlin.time.Instant

internal val DAY_LINE_X = 18.dp
private val DAY_LINE_WIDTH = 2.dp
private val DAY_LINE_DASH = 4.dp
internal val DAY_DOT_RADIUS = 4.dp
private val TODAY_DOT_RADIUS = 5.dp
private val TODAY_LINE_FADE = 40.dp
private val MARKER_LINE_GAP_RADIUS = 9.dp

internal class AgendaGuideColors(val marker: Color, val pastMarker: Color, val line: Color, val dot: Color, val today: Color, val background: Color)

internal fun DrawScope.drawAgendaGuides(layout: LazyGridLayoutInfo, agenda: Agenda, now: Instant, markerIcon: Painter, colors: AgendaGuideColors, pinnedLabelRow: Int?) {
    val spans = HashMap<Int, ClosedFloatingPointRange<Float>>()
    for (item in layout.visibleItemsInfo) {
        val row = agenda.rowIndexOf(item.key) ?: continue
        val top = (item.offset.y - layout.viewportStartOffset).toFloat()
        val span = top..top + item.size.height
        spans[row] = spans[row]?.let { minOf(it.start, span.start)..maxOf(it.endInclusive, span.endInclusive) } ?: span
    }
    if (spans.isEmpty()) return
    drawDayLine(agenda, spans, colors, pinnedLabelRow)
    drawMarkerIcons(agenda, spans, now, markerIcon, colors)
}

private fun DrawScope.drawMarkerIcons(agenda: Agenda, spans: Map<Int, ClosedFloatingPointRange<Float>>, now: Instant, icon: Painter, colors: AgendaGuideColors) {
    val size = AgendaIconSize.toPx()
    val upcomingTint = ColorFilter.tint(colors.marker)
    val pastTint = ColorFilter.tint(colors.pastMarker)
    for ((row, span) in spans) {
        val marker = (agenda.rows[row] as? AgendaRow.Marker)?.marker ?: continue
        val tint = if (marker.endsAt <= now) pastTint else upcomingTint
        val center = Offset(DAY_LINE_X.toPx(), (span.start + span.endInclusive) / 2)
        drawCircle(colors.background, MARKER_LINE_GAP_RADIUS.toPx(), center)
        translate(center.x - size / 2, center.y - size / 2) {
            with(icon) { draw(Size(size, size), colorFilter = tint) }
        }
    }
}

private fun DrawScope.drawDayLine(agenda: Agenda, spans: Map<Int, ClosedFloatingPointRange<Float>>, colors: AgendaGuideColors, pinnedLabelRow: Int?) {
    val x = DAY_LINE_X.toPx()
    val width = DAY_LINE_WIDTH.toPx()
    val dash = DAY_LINE_DASH.toPx()
    val dashes = PathEffect.dashPathEffect(floatArrayOf(dash, dash))
    val shown = spans.keys.sorted()
    for ((position, row) in shown.withIndex()) {
        val time = agenda.rowTimes[row]
        val span = spans.getValue(row)
        val top = if (position == 0 && span.start > 0) span.start - ceil(span.start / (2 * dash)) * 2 * dash else span.start
        val bottom = shown.getOrNull(position + 1)?.let { spans.getValue(it).start } ?: size.height
        val dashedFrom = when {
            time == AgendaTime.Past -> bottom
            time == AgendaTime.Today && agenda.rows[row] is AgendaRow.Label -> dayDotY(span).coerceIn(top, bottom)
            else -> top
        }
        if (dashedFrom > top) drawLine(colors.line, Offset(x, top), Offset(x, dashedFrom), width)
        if (bottom > dashedFrom) drawLine(colors.line, Offset(x, dashedFrom), Offset(x, bottom), width, pathEffect = dashes)
    }
    val fade = TODAY_LINE_FADE.toPx()
    for (row in shown) {
        if (agenda.rows[row] !is AgendaRow.Label || row == pinnedLabelRow) continue
        val span = spans.getValue(row)
        val center = Offset(x, dayDotY(span))
        if (agenda.rowTimes[row] == AgendaTime.Today) {
            val fadeTop = Offset(x, center.y - fade)
            drawLine(Brush.verticalGradient(listOf(colors.today.copy(alpha = 0f), colors.today), startY = fadeTop.y, endY = center.y), fadeTop, center, width)
        }
        drawDayDot(agenda.rowTimes[row], center, colors)
    }
}

private fun DrawScope.dayDotY(labelSpan: ClosedFloatingPointRange<Float>) = (labelSpan.start + AgendaLabelGap.toPx() + labelSpan.endInclusive) / 2

internal fun DrawScope.drawDayDot(time: AgendaTime, center: Offset, colors: AgendaGuideColors, solid: Boolean = false) {
    when (time) {
        AgendaTime.Today -> drawCircle(colors.today, TODAY_DOT_RADIUS.toPx(), center)
        AgendaTime.Past -> drawCircle(colors.line, DAY_DOT_RADIUS.toPx(), center)
        AgendaTime.Future, AgendaTime.Undated -> if (solid) {
            drawCircle(colors.dot, DAY_DOT_RADIUS.toPx(), center)
        } else {
            val width = DAY_LINE_WIDTH.toPx()
            drawCircle(colors.background, DAY_DOT_RADIUS.toPx(), center)
            drawCircle(colors.dot, DAY_DOT_RADIUS.toPx() - width / 2, center, style = Stroke(width))
        }
    }
}
