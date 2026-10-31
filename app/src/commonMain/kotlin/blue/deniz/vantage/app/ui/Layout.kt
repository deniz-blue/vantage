package blue.deniz.vantage.app.ui

import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.OverscrollEffect
import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.material3.LocalTextStyle
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberOverscrollEffect
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.offset

val ContentMaxWidth = 640.dp
val FloatingShadowElevation = 6.dp
private const val FLOATING_OUTLINE_ALPHA = 0.5f
private val FLOATING_TEXT_HALO_RADIUS = 6.dp

val ColorScheme.floatingContainer: Color
    get() = if (background.luminance() < 0.5f) surfaceContainerHighest else surfaceContainerLowest

@Composable
fun floatingBorder() = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = FLOATING_OUTLINE_ALPHA))

@Composable
fun FloatingText(text: String, color: Color = MaterialTheme.colorScheme.onSurfaceVariant) {
    val background = MaterialTheme.colorScheme.background
    val haloRadius = with(LocalDensity.current) { FLOATING_TEXT_HALO_RADIUS.toPx() }
    val style = LocalTextStyle.current
    Box {
        Text(text, color = background, style = style.copy(drawStyle = Stroke(width = haloRadius, join = StrokeJoin.Round)))
        Text(text, color = color, style = style.copy(shadow = Shadow(background, blurRadius = haloRadius)))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenHeader(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    onClose: (() -> Unit)? = null,
    maxWidth: Dp = ContentMaxWidth,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Box(modifier.fillMaxWidth().glassBar(MaterialTheme.colorScheme.surface), contentAlignment = Alignment.TopCenter) {
        TopAppBar(
            title = { Text(title, maxLines = 1) },
            modifier = Modifier.widthIn(max = maxWidth),
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent, scrolledContainerColor = Color.Transparent),
            navigationIcon = {
                when {
                    onBack != null -> IconButton(AppIcons.ArrowLeft, "Back", onBack)
                    onClose != null -> IconButton(AppIcons.X, "Close", onClose)
                }
            },
            actions = actions,
        )
    }
}

@Composable
fun HeaderButton(label: String, onClick: () -> Unit, enabled: Boolean = true, busy: Boolean = false) {
    Button(label, onClick, Modifier.padding(end = Spacing.sm), primary = true, enabled = enabled, busy = busy)
}

val LocalTabBarHeight = staticCompositionLocalOf { 0.dp }

private enum class ContentUnderOverlaySlot { Overlay, Content }

@Composable
fun ContentUnderOverlay(
    overlay: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (overlayHeight: Dp) -> Unit,
) {
    SubcomposeLayout(modifier.fillMaxWidth()) { constraints ->
        val overlayPlaceable = subcompose(ContentUnderOverlaySlot.Overlay) { Box { overlay() } }
            .single().measure(constraints.copy(minHeight = 0))
        val overlayHeight = overlayPlaceable.height.toDp()
        val contentPlaceable = subcompose(ContentUnderOverlaySlot.Content) { Box { content(overlayHeight) } }
            .single().measure(constraints.copy(minHeight = 0))
        layout(constraints.maxWidth, constraints.maxHeight) {
            contentPlaceable.place(0, 0)
            overlayPlaceable.place(0, 0)
        }
    }
}

@Composable
fun ListUnderOverlay(
    overlay: @Composable () -> Unit,
    bottomPadding: Dp,
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    listModifier: Modifier = Modifier,
    overscrollEffect: OverscrollEffect? = rememberOverscrollEffect(),
    items: LazyListScope.() -> Unit,
) {
    ContentUnderOverlay(
        overlay = { Box(Modifier.padding(start = Spacing.md, end = Spacing.md, bottom = Spacing.sm)) { overlay() } },
        modifier = modifier,
    ) { overlayHeight ->
        LazyColumn(
            listModifier.fillMaxWidth(),
            state = state,
            contentPadding = PaddingValues(top = overlayHeight, bottom = bottomPadding),
            overscrollEffect = overscrollEffect,
            content = items,
        )
    }
}

@Composable
fun FloatingControls(modifier: Modifier = Modifier, backdrop: Boolean = true, content: @Composable ColumnScope.() -> Unit) {
    val placement = modifier
        .windowInsetsPadding(WindowInsets.statusBars)
        .padding(horizontal = Spacing.sm, vertical = Spacing.sm)
        .fillMaxWidth()
    if (!backdrop) {
        Column(placement.padding(Spacing.sm), verticalArrangement = Arrangement.spacedBy(Spacing.sm), content = content)
        return
    }
    Surface(
        modifier = placement.floatingSurface(RoundedCornerShape(Radius.md)),
        shape = RoundedCornerShape(Radius.md),
        color = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = floatingBorder(),
    ) {
        Column(Modifier.padding(Spacing.sm), verticalArrangement = Arrangement.spacedBy(Spacing.sm), content = content)
    }
}

@Composable
fun FloatingIconButton(icon: ImageVector, label: String, onClick: () -> Unit, modifier: Modifier = Modifier, size: Dp = ControlHeight.lg) {
    Surface(
        modifier = modifier.floatingSurface(CircleShape),
        shape = CircleShape,
        color = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = floatingBorder(),
    ) {
        IconButton(icon, label, onClick, Modifier.size(size))
    }
}

@Composable
fun FloatingHeader(title: String, onBack: () -> Unit, actions: @Composable RowScope.() -> Unit = {}) {
    Row(
        Modifier.layout { measurable, constraints ->
            val inset = IconButtonInset.roundToPx()
            val placeable = measurable.measure(constraints.offset(horizontal = 2 * inset))
            layout(placeable.width - 2 * inset, placeable.height) { placeable.placeRelative(-inset, 0) }
        },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(AppIcons.ArrowLeft, "Back", onBack)
        Text(title, Modifier.weight(1f).padding(start = Spacing.xs, end = Spacing.sm), maxLines = 1, style = MaterialTheme.typography.titleLarge)
        actions()
    }
}

@Composable
fun AtLeastAsTallAs(reference: @Composable () -> Unit, content: @Composable () -> Unit) {
    Box {
        Box(Modifier.alpha(0f).clearAndSetSemantics {}) { reference() }
        content()
    }
}
