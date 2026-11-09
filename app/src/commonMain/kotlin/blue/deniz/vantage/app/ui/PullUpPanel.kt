package blue.deniz.vantage.app.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

private val PanelMotion = spring<Float>(stiffness = Spring.StiffnessMediumLow)
private val FLING_VELOCITY = 400.dp

@Stable
class PullUpPanelState internal constructor(private val scope: CoroutineScope) {
    var raisedFraction by mutableFloatStateOf(0f)
        private set
    internal var travelPx = 1f
    internal var flingVelocityPx = 1f
    private var settling: Job? = null

    fun raise() = settleAt(1f, velocity = 0f)

    fun lower() = settleAt(0f, velocity = 0f)

    fun heightBetween(lowered: Dp, raised: Dp): Dp = lowered + (raised - lowered) * raisedFraction

    internal fun toggle() = settleAt(if (raisedFraction < 0.5f) 1f else 0f, velocity = 0f)

    internal fun dragBy(deltaY: Float): Float {
        settling?.cancel()
        val before = raisedFraction
        raisedFraction = (raisedFraction - deltaY / travelPx).coerceIn(0f, 1f)
        return (before - raisedFraction) * travelPx
    }

    internal fun settle(velocityY: Float) {
        val target = when {
            abs(velocityY) < flingVelocityPx -> raisedFraction.roundToInt().toFloat()
            velocityY < 0 -> 1f
            else -> 0f
        }
        settleAt(target, velocity = -velocityY / travelPx)
    }

    private fun settleAt(target: Float, velocity: Float) {
        settling?.cancel()
        settling = scope.launch {
            animate(raisedFraction, target, velocity, PanelMotion) { value, _ -> raisedFraction = value }
        }
    }

    internal val listScroll = object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset =
            if (available.y < 0 && source == NestedScrollSource.UserInput && raisedFraction < 1f) Offset(0f, dragBy(available.y)) else Offset.Zero

        override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset =
            if (available.y > 0 && source == NestedScrollSource.UserInput) Offset(0f, dragBy(available.y)) else Offset.Zero

        override suspend fun onPreFling(available: Velocity): Velocity {
            if (raisedFraction == 0f || raisedFraction == 1f) return Velocity.Zero
            settle(available.y)
            return available
        }
    }
}

@Composable
fun rememberPullUpPanelState(): PullUpPanelState {
    val scope = rememberCoroutineScope()
    return remember(scope) { PullUpPanelState(scope) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PullUpPanel(
    state: PullUpPanelState,
    loweredHeight: Dp,
    raisedHeight: Dp,
    modifier: Modifier = Modifier,
    items: LazyListScope.() -> Unit,
) {
    val density = LocalDensity.current
    SideEffect {
        state.travelPx = with(density) { (raisedHeight - loweredHeight).toPx() }.coerceAtLeast(1f)
        state.flingVelocityPx = with(density) { FLING_VELOCITY.toPx() }
    }
    val bottomInset = WindowInsets.navigationBars.exclude(WindowInsets.ime).only(WindowInsetsSides.Bottom).asPaddingValues().calculateBottomPadding()
    Surface(
        modifier.fillMaxWidth().height(state.heightBetween(loweredHeight, raisedHeight)),
        shape = BottomSheetDefaults.ExpandedShape,
        color = BottomSheetDefaults.ContainerColor,
    ) {
        Column {
            Box(
                Modifier
                    .fillMaxWidth()
                    .draggable(rememberDraggableState { state.dragBy(it) }, Orientation.Vertical, onDragStopped = { state.settle(it) })
                    .clickable(remember { MutableInteractionSource() }, indication = null, onClick = state::toggle),
                contentAlignment = Alignment.Center,
            ) { BottomSheetDefaults.DragHandle() }
            LazyColumn(
                Modifier.fillMaxWidth().weight(1f).nestedScroll(state.listScroll),
                contentPadding = PaddingValues(bottom = bottomInset + Spacing.md),
                content = items,
            )
        }
    }
}
