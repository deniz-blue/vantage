package blue.deniz.vantage.app.ui

import androidx.compose.animation.core.animate
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
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

private val PULL_THRESHOLD = 96.dp
private const val PULL_RESISTANCE = 0.5f
private const val RELEASE_AFTER_WHEEL_IDLE_MILLIS = 200L

@Stable
class PullAboveTop internal constructor(private val threshold: Float, private val onReleased: () -> Unit) : NestedScrollConnection {
    var enabled by mutableStateOf(true)
        internal set
    var distance by mutableFloatStateOf(0f)
        private set
    internal var held by mutableStateOf(false)
    internal var scrolls by mutableIntStateOf(0)
        private set
    private var releasing = false

    val armed: Boolean get() = distance >= threshold
    val progress: Float get() = (distance / threshold).coerceIn(0f, 1f)

    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        if (distance <= 0f || available.y >= 0f || source != NestedScrollSource.UserInput) return Offset.Zero
        val taken = maxOf(available.y * PULL_RESISTANCE, -distance)
        distance += taken
        scrolls++
        return Offset(0f, taken / PULL_RESISTANCE)
    }

    override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
        if (!enabled || available.y <= 0f || source != NestedScrollSource.UserInput) return Offset.Zero
        distance += available.y * PULL_RESISTANCE
        scrolls++
        return Offset(0f, available.y)
    }

    override suspend fun onPreFling(available: Velocity): Velocity {
        if (distance <= 0f) return Velocity.Zero
        release()
        return available
    }

    internal suspend fun release() {
        if (releasing || distance <= 0f) return
        releasing = true
        try {
            if (armed && enabled) onReleased()
            animate(distance, 0f) { value, _ -> distance = value }
        } finally {
            releasing = false
        }
    }
}

@Composable
fun rememberPullAboveTop(enabled: Boolean, onReleased: () -> Unit): PullAboveTop {
    val threshold = with(LocalDensity.current) { PULL_THRESHOLD.toPx() }
    val currentOnReleased by rememberUpdatedState(onReleased)
    val pull = remember(threshold) { PullAboveTop(threshold) { currentOnReleased() } }
    SideEffect { pull.enabled = enabled }
    val haptics = LocalHapticFeedback.current
    LaunchedEffect(pull) {
        snapshotFlow { pull.armed }.collect { armed -> if (armed) haptics.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate) }
    }
    LaunchedEffect(pull) {
        snapshotFlow { pull.scrolls to pull.held }.collectLatest { (_, held) ->
            if (held || pull.distance <= 0f) return@collectLatest
            delay(RELEASE_AFTER_WHEEL_IDLE_MILLIS)
            pull.release()
        }
    }
    return pull
}

fun Modifier.pullAboveTop(pull: PullAboveTop): Modifier = this
    .pointerInput(pull) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                pull.held = event.changes.any { it.pressed }
            }
        }
    }
    .nestedScroll(pull)

@Composable
fun PullAboveTopHint(pull: PullAboveTop, label: String, modifier: Modifier = Modifier) {
    if (pull.distance <= 0f) return
    val color = if (pull.armed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    Box(
        modifier
            .fillMaxWidth()
            .height(with(LocalDensity.current) { pull.distance.toDp() })
            .clipToBounds()
            .graphicsLayer { alpha = pull.progress },
        contentAlignment = Alignment.Center,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs), verticalAlignment = Alignment.CenterVertically) {
            Icon(AppIcons.ArrowUp, tint = color)
            Text(label, color = color, style = MaterialTheme.typography.labelLarge)
        }
    }
}
