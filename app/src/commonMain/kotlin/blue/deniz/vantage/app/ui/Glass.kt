package blue.deniz.vantage.app.ui

import androidx.compose.foundation.background
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

private val GLASS_BLUR = 24.dp
private const val GLASS_TINT_ALPHA = 0.55f
private const val GLASS_BAR_TINT_ALPHA = 0.8f

@Stable
class Backdrop {
    internal var layer by mutableStateOf<GraphicsLayer?>(null)
    internal var origin by mutableStateOf(Offset.Zero)
    internal var drawCount by mutableIntStateOf(0)
}

val LocalBackdrop = staticCompositionLocalOf<Backdrop?> { null }

val ColorScheme.glassTint: Color
    get() = floatingContainer.copy(alpha = GLASS_TINT_ALPHA)

@Composable
fun rememberBackdrop(): Backdrop = remember { Backdrop() }

@Composable
fun Modifier.backdropSource(backdrop: Backdrop?): Modifier {
    if (backdrop == null) return this
    val layer = rememberGraphicsLayer()
    DisposableEffect(backdrop, layer) {
        backdrop.layer = layer
        onDispose { if (backdrop.layer === layer) backdrop.layer = null }
    }
    return this
        .onGloballyPositioned { backdrop.origin = it.positionInRoot() }
        .drawWithContent {
            layer.record { this@drawWithContent.drawContent() }
            drawLayer(layer)
            Snapshot.withoutReadObservation { backdrop.drawCount++ }
        }
}

@Composable
fun Modifier.floatingSurface(shape: Shape): Modifier {
    val backdrop = LocalBackdrop.current
    if (backdrop?.layer == null) return this.shadow(FloatingShadowElevation, shape).background(MaterialTheme.colorScheme.floatingContainer, shape)
    return glassBehind(backdrop, shape, MaterialTheme.colorScheme.glassTint)
}

@Composable
fun Modifier.glassBar(color: Color): Modifier {
    val backdrop = LocalBackdrop.current
    if (backdrop?.layer == null) return this.background(color)
    return glassBehind(backdrop, RectangleShape, color.copy(alpha = GLASS_BAR_TINT_ALPHA))
}

@Composable
private fun Modifier.glassBehind(backdrop: Backdrop, shape: Shape, tint: Color): Modifier {
    val under = MaterialTheme.colorScheme.background
    val blurred = rememberGraphicsLayer()
    val radius = with(LocalDensity.current) { GLASS_BLUR.toPx() }
    val blur = remember(radius) { BlurEffect(radius, radius, TileMode.Clamp) }
    var position by remember { mutableStateOf(Offset.Zero) }
    return this
        .onGloballyPositioned { position = it.positionInRoot() }
        .drawBehind {
            val source = backdrop.layer ?: return@drawBehind
            backdrop.drawCount
            val shift = backdrop.origin - position
            blurred.renderEffect = blur
            blurred.record {
                drawRect(under)
                translate(shift.x, shift.y) { drawLayer(source) }
            }
            val outline = shape.createOutline(size, layoutDirection, this)
            clipPath(Path().apply { addOutline(outline) }) { drawLayer(blurred) }
            drawOutline(outline, tint)
        }
}
