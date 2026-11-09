package blue.deniz.vantage.app.ui

import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.launch

private val PASS_THRESHOLD = 96.dp
private const val PULL_RESISTANCE = 0.5f
private const val RELEASE_AFTER_WHEEL_IDLE_MILLIS = 200L

@Stable
class LazyGridScrollStop internal constructor(
    private val grid: LazyGridState,
    private val threshold: Float,
    passedState: MutableState<Boolean>,
) : NestedScrollConnection {
    var stopIndex by mutableIntStateOf(0)
    var passed by passedState
        private set
    var endFillerHeight by mutableIntStateOf(0)
        private set
    internal var held by mutableStateOf(false)
    internal var scrolls by mutableIntStateOf(0)
        private set
    private var pulledByUser = false
    private var releasing = false

    val firstReachableIndex: Int get() = if (passed) 0 else stopIndex

    private val stopRowOffset: Float?
        get() {
            if (stopIndex == 0) return null
            val visible = grid.layoutInfo.visibleItemsInfo
            if (visible.isEmpty()) return null
            visible.firstOrNull { it.index == stopIndex }?.let { return it.offset.y.toFloat() }
            return if (visible.first().index > stopIndex) Float.NEGATIVE_INFINITY else null
        }

    val distancePastStop: Float? get() = if (passed) null else stopRowOffset

    internal val scrolledBelowStopAfterPassing: Boolean get() = passed && (stopRowOffset ?: 0f) < 0f

    internal fun stopAgain() {
        passed = false
    }

    val armed: Boolean get() = pulledByUser && (distancePastStop ?: 0f) >= threshold

    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        val byUser = source == NestedScrollSource.UserInput
        if (byUser) scrolls++
        if (available.y <= 0f) return Offset.Zero
        val past = distancePastStop ?: return Offset.Zero
        val beyond = available.y - (-past).coerceAtLeast(0f)
        if (beyond <= 0f) return Offset.Zero
        if (!byUser) return Offset(0f, beyond)
        pulledByUser = true
        return Offset(0f, beyond * (1 - PULL_RESISTANCE))
    }

    override suspend fun onPreFling(available: Velocity): Velocity {
        val past = distancePastStop ?: return Velocity.Zero
        if (past <= 0f) return Velocity.Zero
        return if (release()) Velocity.Zero else available
    }

    override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
        if (!held) settleAtStop()
        return Velocity.Zero
    }

    internal suspend fun release(): Boolean {
        if (armed) {
            passed = true
            pulledByUser = false
            return true
        }
        settleAtStop()
        return false
    }

    private suspend fun settleAtStop() {
        val past = distancePastStop ?: return
        if (releasing || past <= 0f) return
        releasing = true
        try {
            grid.animateScrollBy(past)
            pulledByUser = false
        } finally {
            releasing = false
        }
    }

    internal val untouched: Boolean get() = scrolls == 0 && !held

    internal suspend fun keepAtStopUntilTouched() {
        snapshotFlow { if (untouched) Triple(stopIndex, distancePastStop, grid.canScrollForward) else null }
            .takeWhile { it != null }
            .filterNotNull()
            .collect { (index, past, _) -> if (past != 0f) grid.scrollToItem(index) }
    }

    internal fun neededEndFillerHeight(): Int? {
        if (stopIndex == 0) return 0
        val info = grid.layoutInfo
        val visible = info.visibleItemsInfo
        val stopItem = visible.firstOrNull { it.index == stopIndex } ?: return null
        val lastContentIndex = info.totalItemsCount - 2
        if (visible.none { it.index == lastContentIndex }) return if (stopItem.offset.y <= 0) 0 else null
        val contentBottom = visible.filter { it.index <= lastContentIndex }.maxOf { it.offset.y + it.size.height }
        val wanted = info.viewportEndOffset - info.afterContentPadding - info.mainAxisItemSpacing
        return (wanted - (contentBottom - stopItem.offset.y)).coerceAtLeast(0)
    }

    internal fun updateEndFiller(height: Int) {
        endFillerHeight = height
    }
}

@Composable
fun rememberLazyGridScrollStop(grid: LazyGridState, resetKey: Any?, ready: Boolean): LazyGridScrollStop {
    val threshold = with(LocalDensity.current) { PASS_THRESHOLD.toPx() }
    val passed = rememberSaveable(resetKey) { mutableStateOf(false) }
    var positioned by rememberSaveable(resetKey) { mutableStateOf(false) }
    val stop = remember(grid, threshold, passed) { LazyGridScrollStop(grid, threshold, passed) }
    val haptics = LocalHapticFeedback.current
    LaunchedEffect(stop, ready, positioned) {
        if (!ready || positioned) return@LaunchedEffect
        stop.keepAtStopUntilTouched()
        positioned = true
    }
    LaunchedEffect(stop) {
        snapshotFlow { stop.neededEndFillerHeight() }.collect { height -> if (height != null) stop.updateEndFiller(height) }
    }
    LaunchedEffect(stop) {
        snapshotFlow { stop.scrolledBelowStopAfterPassing }.collect { below -> if (below) stop.stopAgain() }
    }
    LaunchedEffect(stop) {
        snapshotFlow { stop.armed }.collect { armed -> if (armed) haptics.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate) }
    }
    LaunchedEffect(stop) {
        val effectScope = this
        snapshotFlow { Triple(stop.scrolls, stop.held, grid.isScrollInProgress) }.collectLatest { (_, held, scrolling) ->
            if (held || scrolling || (stop.distancePastStop ?: 0f) <= 0f) return@collectLatest
            delay(RELEASE_AFTER_WHEEL_IDLE_MILLIS)
            effectScope.launch { stop.release() }
        }
    }
    return stop
}

fun Modifier.lazyGridScrollStop(stop: LazyGridScrollStop): Modifier = this
    .pointerInput(stop) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                stop.held = event.changes.any { it.pressed }
            }
        }
    }
    .nestedScroll(stop)
