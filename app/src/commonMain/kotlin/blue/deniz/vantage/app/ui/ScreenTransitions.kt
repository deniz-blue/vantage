package blue.deniz.vantage.app.ui

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.animateDp
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalSharedTransitionApi::class)
class ScreenTransition(val sharedElements: SharedTransitionScope, val screenVisibility: AnimatedVisibilityScope)

val LocalScreenTransition = staticCompositionLocalOf<ScreenTransition?> { null }

private data class EventPageBoundsKey(val eventId: String)

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.growsIntoEventPage(eventId: String?): Modifier {
    val transition = LocalScreenTransition.current
    if (transition == null || eventId == null) return this
    return with(transition.sharedElements) {
        sharedBounds(
            rememberSharedContentState(EventPageBoundsKey(eventId)),
            transition.screenVisibility,
            clipInOverlayDuringTransition = OverlayClip(CardShape),
        )
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun EventPageGrowingFromCard(eventId: String, content: @Composable () -> Unit) {
    val transition = LocalScreenTransition.current ?: return content()
    val corner = transition.screenVisibility.transition.animateDp(label = "event page corner") { if (it == EnterExitState.Visible) 0.dp else Radius.md }
    val shape = remember(corner) { RoundedCornerShapeOf(corner) }
    val bounds = with(transition.sharedElements) {
        Modifier.fillMaxSize().sharedBounds(
            rememberSharedContentState(EventPageBoundsKey(eventId)),
            transition.screenVisibility,
            resizeMode = SharedTransitionScope.ResizeMode.scaleToBounds(ContentScale.FillWidth, Alignment.TopCenter),
            clipInOverlayDuringTransition = OverlayClip(shape),
        )
    }
    Box(bounds.background(MaterialTheme.colorScheme.background)) { content() }
}

private class RoundedCornerShapeOf(private val radius: State<Dp>) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline =
        RoundedCornerShape(radius.value).createOutline(size, layoutDirection, density)
}
