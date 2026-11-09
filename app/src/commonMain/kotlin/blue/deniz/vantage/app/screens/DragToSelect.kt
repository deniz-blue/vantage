package blue.deniz.vantage.app.screens

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs
import androidx.compose.foundation.lazy.grid.LazyGridLayoutInfo
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import blue.deniz.vantage.app.ui.EventCardSelection
import blue.deniz.vantage.core.ResolvedEvent

private val CHECK_SIZE = 20.dp
private val CHECK_CORNER = 4.dp
private val CHECK_STROKE = 2.dp
private val AUTOSCROLL_ZONE = 64.dp
private val AUTOSCROLL_STEP = 16.dp

internal class CheckColors(val box: Color, val checked: Color, val mark: Color)

internal fun eventAt(layout: LazyGridLayoutInfo, eventsByKey: Map<String, ResolvedEvent>, position: Offset): ResolvedEvent? {
    val line = layout.visibleItemsInfo.filter { item ->
        val top = item.offset.y - layout.viewportStartOffset
        position.y >= top && position.y < top + item.size.height && (item.key as? String)?.let(eventsByKey::get) != null
    }
    val item = line.firstOrNull { position.x >= it.offset.x && position.x < it.offset.x + it.size.width }
        ?: line.minByOrNull { abs(it.offset.x + it.size.width / 2f - position.x) }
    return (item?.key as? String)?.let(eventsByKey::get)
}

internal fun DrawScope.drawSelectionChecks(layout: LazyGridLayoutInfo, eventsByKey: Map<String, ResolvedEvent>, actions: EventCardActions, margin: Dp, colors: CheckColors) {
    val box = CHECK_SIZE.toPx()
    val left = (margin.toPx() - box) / 2
    val corner = CornerRadius(CHECK_CORNER.toPx())
    val stroke = CHECK_STROKE.toPx()
    for (item in layout.visibleItemsInfo) {
        val event = (item.key as? String)?.let(eventsByKey::get) ?: continue
        val top = item.offset.y - layout.viewportStartOffset + (item.size.height - box) / 2
        if (actions.selectionOf(event) == EventCardSelection.Selected) {
            drawRoundRect(colors.checked, Offset(left, top), Size(box, box), corner)
            val mark = Path().apply {
                moveTo(left + box * 0.24f, top + box * 0.52f)
                lineTo(left + box * 0.42f, top + box * 0.7f)
                lineTo(left + box * 0.76f, top + box * 0.32f)
            }
            drawPath(mark, colors.mark, style = Stroke(stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
        } else {
            drawRoundRect(colors.box, Offset(left + stroke / 2, top + stroke / 2), Size(box - stroke, box - stroke), corner, style = Stroke(stroke))
        }
    }
}

internal fun Modifier.selectsByDragging(
    grid: LazyGridState,
    topPadding: () -> Dp,
    checkColumnWidth: () -> Float?,
    eventAt: (Offset) -> ResolvedEvent?,
    shown: () -> List<ResolvedEvent>,
    actions: () -> EventCardActions,
    onHeld: () -> Unit,
): Modifier = pointerInput(grid) {
    coroutineScope {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            if (!actions().isSelecting) return@awaitEachGesture
            val start = eventAt(down.position) ?: return@awaitEachGesture
            val checkColumn = checkColumnWidth()
            val inCheckColumn = checkColumn != null && down.position.x < checkColumn
            if (inCheckColumn) {
                down.consume()
            } else {
                var released = false
                var moved = false
                val held = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
                    while (!released && !moved) {
                        val change = awaitPointerEvent(PointerEventPass.Initial).changes.firstOrNull { it.id == down.id }
                        released = change == null || !change.pressed
                        moved = change != null && (change.position - down.position).getDistance() > viewConfiguration.touchSlop
                    }
                } == null
                if (!held) return@awaitEachGesture
                onHeld()
            }
            actions().toggleSelected(start)
            if (checkColumn != null && !inCheckColumn) {
                do {
                    val change = awaitPointerEvent(PointerEventPass.Initial).changes.firstOrNull { it.id == down.id } ?: break
                    change.consume()
                } while (change.pressed)
                return@awaitEachGesture
            }
            val selects = actions().selectionOf(start) == EventCardSelection.Selected
            val changedByDrag = mutableSetOf<String>()
            var current = start.listKey()
            var pointer = down.position
            val extendTo = { event: ResolvedEvent ->
                if (event.listKey() != current) {
                    current = event.listKey()
                    val list = shown()
                    val from = list.indexOfFirst { it.listKey() == start.listKey() }
                    val to = list.indexOfFirst { it.listKey() == current }
                    if (from >= 0 && to >= 0) {
                        val range = list.subList(minOf(from, to), maxOf(from, to) + 1)
                        val rangeKeys = range.mapTo(mutableSetOf()) { it.listKey() }
                        val acting = actions()
                        for (inRange in range) {
                            if ((acting.selectionOf(inRange) == EventCardSelection.Selected) != selects) {
                                acting.toggleSelected(inRange)
                                changedByDrag += inRange.listKey()
                            }
                        }
                        for (outside in list) {
                            val key = outside.listKey()
                            if (key in changedByDrag && key !in rangeKeys) {
                                acting.toggleSelected(outside)
                                changedByDrag -= key
                            }
                        }
                    }
                }
            }
            val zone = AUTOSCROLL_ZONE.toPx()
            val fastest = AUTOSCROLL_STEP.toPx()
            val autoscroll = launch {
                while (isActive) {
                    withFrameNanos {}
                    val top = topPadding().toPx()
                    val speed = when {
                        pointer.y < top + zone -> -fastest * ((top + zone - pointer.y) / zone).coerceIn(0f, 1f)
                        pointer.y > size.height - zone -> fastest * ((pointer.y - (size.height - zone)) / zone).coerceIn(0f, 1f)
                        else -> 0f
                    }
                    if (speed != 0f) {
                        grid.scrollBy(speed)
                        eventAt(pointer)?.let(extendTo)
                    }
                }
            }
            try {
                while (true) {
                    val change = awaitPointerEvent(PointerEventPass.Initial).changes.firstOrNull { it.id == down.id } ?: break
                    change.consume()
                    if (!change.pressed) break
                    pointer = change.position
                    eventAt(pointer)?.let(extendTo)
                }
            } finally {
                autoscroll.cancel()
            }
        }
    }
}
