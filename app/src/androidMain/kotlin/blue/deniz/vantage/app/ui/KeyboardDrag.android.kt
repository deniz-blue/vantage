package blue.deniz.vantage.app.ui

import android.graphics.Insets
import android.os.Build
import android.os.CancellationSignal
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsAnimationControlListener
import android.view.WindowInsetsAnimationController
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

private val KeyboardFlingSpeed = 400.dp

@Composable
actual fun rememberKeyboardDrag(): KeyboardDrag? {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return null
    val view = LocalView.current
    val flingSpeed = with(LocalDensity.current) { KeyboardFlingSpeed.toPx() }
    val drag = remember(view, flingSpeed) { ControlledKeyboardDrag(view, flingSpeed) }
    DisposableEffect(drag) { onDispose { drag.release() } }
    return drag
}

@RequiresApi(Build.VERSION_CODES.R)
private class ControlledKeyboardDrag(private val view: View, private val flingSpeed: Float) : KeyboardDrag, WindowInsetsAnimationControlListener {
    private var controller: WindowInsetsAnimationController? = null
    private var request: CancellationSignal? = null
    private var lowerWhenReady = 0f
    private var bottom = 0f

    override val height: Int
        get() = controller?.currentInsets?.bottom ?: view.rootWindowInsets?.getInsets(WindowInsets.Type.ime())?.bottom ?: 0

    override fun lowerBy(px: Float): Float {
        val controller = controller
        if (controller == null) {
            if (px <= 0f || height == 0) return 0f
            lowerWhenReady += px
            requestControl()
            return px
        }
        val shown = controller.shownStateInsets.bottom.toFloat()
        val hidden = controller.hiddenStateInsets.bottom.toFloat()
        val next = (bottom - px).coerceIn(hidden, shown)
        val moved = bottom - next
        if (moved == 0f) return 0f
        bottom = next
        controller.setInsetsAndAlpha(Insets.of(0, 0, 0, next.roundToInt()), 1f, 0f)
        return moved
    }

    override suspend fun settle(velocityY: Float) {
        val controller = controller
        if (controller == null) {
            request?.cancel()
            clear()
            if (velocityY > 0f) view.windowInsetsController?.hide(WindowInsets.Type.ime())
            return
        }
        val shown = controller.shownStateInsets.bottom.toFloat()
        val hidden = controller.hiddenStateInsets.bottom.toFloat()
        val toShown = when {
            velocityY > flingSpeed -> false
            velocityY < -flingSpeed -> true
            else -> bottom > (shown + hidden) / 2
        }
        val target = if (toShown) shown else hidden
        if (bottom != target) {
            animate(bottom, target, initialVelocity = -velocityY, animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) { value, _ ->
                if (this.controller === controller) {
                    bottom = value.coerceIn(hidden, shown)
                    controller.setInsetsAndAlpha(Insets.of(0, 0, 0, bottom.roundToInt()), 1f, 0f)
                }
            }
        }
        if (this.controller === controller) controller.finish(toShown)
    }

    fun release() {
        request?.cancel()
        controller?.let { it.finish(bottom > (it.shownStateInsets.bottom + it.hiddenStateInsets.bottom) / 2f) }
        clear()
    }

    private fun requestControl() {
        if (request != null) return
        val signal = CancellationSignal()
        request = signal
        view.windowInsetsController?.controlWindowInsetsAnimation(WindowInsets.Type.ime(), -1L, null, signal, this)
    }

    private fun clear() {
        controller = null
        request = null
        lowerWhenReady = 0f
    }

    override fun onReady(controller: WindowInsetsAnimationController, types: Int) {
        this.controller = controller
        bottom = controller.currentInsets.bottom.toFloat()
        val pending = lowerWhenReady
        lowerWhenReady = 0f
        if (pending != 0f) lowerBy(pending)
    }

    override fun onFinished(controller: WindowInsetsAnimationController) = clear()

    override fun onCancelled(controller: WindowInsetsAnimationController?) = clear()
}
