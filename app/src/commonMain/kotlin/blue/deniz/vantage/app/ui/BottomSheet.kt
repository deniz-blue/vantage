package blue.deniz.vantage.app.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalOverscrollFactory
import androidx.compose.foundation.OverscrollEffect
import androidx.compose.foundation.OverscrollFactory
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.relocation.BringIntoViewResponder
import androidx.compose.foundation.relocation.bringIntoViewResponder
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import blue.deniz.vantage.app.SystemBackHandler
import blue.deniz.vantage.app.rememberSystemBack
import com.composeunstyled.DragHandle
import com.composeunstyled.DrawerHost
import com.composeunstyled.DrawerPresentation
import com.composeunstyled.DrawerPanelAlignment
import com.composeunstyled.DrawerSnapPoint
import com.composeunstyled.DrawerSnapPoints
import com.composeunstyled.Overlay
import com.composeunstyled.Panel
import com.composeunstyled.DrawerValueChange
import com.composeunstyled.UnstyledDrawer
import com.composeunstyled.UnstyledDrawerState
import com.composeunstyled.Viewport
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollDispatcher
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.input.pointer.util.addPointerInputChange
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.foundation.rememberOverscrollEffect
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.drop
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

private enum class BottomSheetHeight { Closed, Open, Expanded }

private const val OPEN_HEIGHT_SCREEN_FRACTION = 0.7f
private const val BEHIND_SHEET_SCALE = 0.94f
private val BehindSheetCornerRadius = 16.dp
private const val MAX_STRETCH = 0.2f
private val SheetMotion = spring<Float>(stiffness = Spring.StiffnessMediumLow)

private fun openBottomSheetHeight(screenHeight: Dp, contentHeight: Dp): Dp =
    minOf(contentHeight, screenHeight * OPEN_HEIGHT_SCREEN_FRACTION)

@Stable
private class OpenBottomSheets {
    private val openFractions = mutableStateListOf<() -> Float>()

    val largestOpenFraction: Float
        get() = openFractions.maxOfOrNull { it() } ?: 0f

    fun add(openFraction: () -> Float) {
        openFractions += openFraction
    }

    fun remove(openFraction: () -> Float) {
        openFractions -= openFraction
    }
}

private val LocalOpenBottomSheets = staticCompositionLocalOf { OpenBottomSheets() }

val LocalScreenOpacity = staticCompositionLocalOf<() -> Float> { { 1f } }

@Composable
fun rememberAnyBottomSheetOpen(): State<Boolean> {
    val openSheets = LocalOpenBottomSheets.current
    return remember(openSheets) { derivedStateOf { openSheets.largestOpenFraction > 0f } }
}

@Composable
fun BottomSheetHost(aboveSheets: @Composable () -> Unit = {}, content: @Composable () -> Unit) {
    val openSheets = remember { OpenBottomSheets() }
    CompositionLocalProvider(LocalOpenBottomSheets provides openSheets) {
        Box(Modifier.fillMaxSize()) {
            DrawerHost(Modifier.fillMaxSize().background(Color.Black)) {
                Box(
                    Modifier.fillMaxSize().graphicsLayer {
                        val fraction = openSheets.largestOpenFraction
                        val scale = 1f - (1f - BEHIND_SHEET_SCALE) * fraction
                        scaleX = scale
                        scaleY = scale
                        shape = RoundedCornerShape(BehindSheetCornerRadius.toPx() * fraction)
                        clip = fraction > 0f
                    },
                ) { content() }
            }
            aboveSheets()
        }
    }
}

@Composable
fun <T : Any> BottomSheet(item: T?, onDismiss: () -> Unit, content: @Composable ColumnScope.(T) -> Unit) {
    BottomSheetFrame(item, onDismiss) { shownItem -> ScrollingBody { content(shownItem) } }
}

@Composable
fun BottomSheet(visible: Boolean, onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    BottomSheetFrame(visible, onDismiss) { ScrollingBody(content) }
}

@Composable
fun <T : Any> BottomSheetFrame(item: T?, onDismiss: () -> Unit, body: @Composable BottomSheetScope.(T) -> Unit) {
    var lastItem by remember { mutableStateOf(item) }
    SideEffect { if (item != null) lastItem = item }
    val shownItem = item ?: lastItem
    BottomSheetFrame(visible = item != null, onDismiss = onDismiss) { shownItem?.let { body(it) } }
}

object BottomSheetScope {
    @Composable
    fun ScrollingBody(content: @Composable ColumnScope.() -> Unit) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom))
                .padding(bottom = Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
            content = content,
        )
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun ListBody(
        header: @Composable ColumnScope.() -> Unit,
        overlay: @Composable () -> Unit,
        state: LazyListState = rememberLazyListState(),
        items: LazyListScope.() -> Unit,
    ) {
        val bottomInset = with(LocalDensity.current) { sheetBottomInsets().getBottom(this).toDp() }
        val listOverscroll = rememberOverscrollEffect() ?: remember { StretchOverscrollEffect() }
        val listScroll = remember(listOverscroll, state) { ClosingDragsMoveSheetOnlyFromListTop(listOverscroll, listIsAtTop = { !state.canScrollBackward }) }
        val listBackdrop = rememberBackdrop(BottomSheetDefaults.ContainerColor)
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            header()
            ListUnderOverlay(
                { CompositionLocalProvider(LocalBackdrop provides listBackdrop, content = overlay) },
                bottomPadding = bottomInset + Spacing.md,
                state = state,
                listModifier = Modifier.backdropSource(listBackdrop).pointerInput(listScroll) { with(listScroll) { noteWhereDragsStart() } }.nestedScroll(listScroll),
                overscrollEffect = listScroll,
                items = items,
            )
        }
    }
}

@Composable
private fun sheetBottomInsets() = WindowInsets.navigationBars.exclude(WindowInsets.ime).only(WindowInsetsSides.Bottom)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun BottomSheetFrame(visible: Boolean, onDismiss: () -> Unit, body: @Composable BottomSheetScope.() -> Unit) {
    SystemBackHandler(enabled = visible, onBack = onDismiss)
    val currentVisible by rememberUpdatedState(visible)
    val currentOnDismiss by rememberUpdatedState(onDismiss)
    val currentSystemBack by rememberUpdatedState(rememberSystemBack())
    val focusManager = LocalFocusManager.current
    var bodyHasFocus by remember { mutableStateOf(false) }
    val keyboard = WindowInsets.ime
    val keyboardTarget = WindowInsets.keyboardTarget
    val density = LocalDensity.current
    var keyboardHeightWhenClosing by remember { mutableStateOf<Int?>(null) }
    val sheet = remember {
        UnstyledDrawerState(
            initialValue = if (visible) BottomSheetHeight.Open else BottomSheetHeight.Closed,
            snapPoints = DrawerSnapPoints {
                BottomSheetHeight.Closed at DrawerSnapPoint.Zero
                BottomSheetHeight.Open at DrawerSnapPoint(::openBottomSheetHeight)
                BottomSheetHeight.Expanded at DrawerSnapPoint.ContentSize
            },
            animationSpec = SheetMotion,
            confirmValueChange = { change ->
                if (change.reason == DrawerValueChange.Reason.NavigateBack && currentSystemBack()) {
                    false
                } else {
                    if (change.targetValue == BottomSheetHeight.Closed && currentVisible) currentOnDismiss()
                    true
                }
            },
        )
    }
    val onScreen by remember(sheet) {
        derivedStateOf {
            sheet.targetValue != BottomSheetHeight.Closed || sheet.currentValue != BottomSheetHeight.Closed || sheet.offset > 0.5f
        }
    }
    LaunchedEffect(visible) {
        keyboardHeightWhenClosing = if (visible) null else keyboard.getBottom(density)
        sheet.targetValue = if (visible) BottomSheetHeight.Open else BottomSheetHeight.Closed
        if (!visible && bodyHasFocus) focusManager.clearFocus()
    }
    LaunchedEffect(sheet) {
        snapshotFlow { onScreen }.drop(1).collect { if (!it && currentVisible) currentOnDismiss() }
    }
    if (!onScreen) return
    var sizedForKeyboardTarget by remember { mutableStateOf(false) }
    LaunchedEffect(sheet, keyboard, keyboardTarget, density) {
        snapshotFlow {
            when {
                sheet.currentValue == BottomSheetHeight.Closed && sheet.targetValue != BottomSheetHeight.Closed -> true
                sheet.isIdle && keyboard.getBottom(density) == keyboardTarget.getBottom(density) -> false
                else -> null
            }
        }.collect { if (it != null) sizedForKeyboardTarget = it }
    }

    val screenOpacity = LocalScreenOpacity.current
    val openFraction = remember(sheet, screenOpacity) { { sheet.progress(BottomSheetHeight.Closed, BottomSheetHeight.Open) * screenOpacity() } }
    val openSheets = LocalOpenBottomSheets.current
    DisposableEffect(openSheets, openFraction) {
        openSheets.add(openFraction)
        onDispose { openSheets.remove(openFraction) }
    }
    val scrim = BottomSheetDefaults.ScrimColor
    val panelStretch = remember { StretchOverscrollEffect() }
    fun expand() {
        if (sheet.targetValue == BottomSheetHeight.Open) sheet.targetValue = BottomSheetHeight.Expanded
    }
    LaunchedEffect(keyboardTarget, density) {
        snapshotFlow { keyboardTarget.getBottom(density) > 0 }.collect { if (it) expand() }
    }
    val keyboardDrag = rememberKeyboardDrag()
    val keyboardFirst = remember(keyboardDrag, focusManager) {
        SheetDragsMoveKeyboardFirst(keyboardDrag, onKeyboardHidden = { sheetWasDragged ->
            focusManager.clearFocus()
            if (!sheetWasDragged && sheet.targetValue == BottomSheetHeight.Expanded) sheet.targetValue = BottomSheetHeight.Open
        })
    }
    var keepsHeightWhileKeyboardHides by remember { mutableStateOf(false) }
    LaunchedEffect(keyboard, keyboardTarget, density, keyboardFirst) {
        snapshotFlow { keyboardTarget.getBottom(density) == 0 && keyboard.getBottom(density) > 0 }.collect { hiding ->
            if (hiding && !keyboardFirst.keepsSheetHeight && sheet.targetValue == BottomSheetHeight.Expanded) keepsHeightWhileKeyboardHides = true
        }
    }
    LaunchedEffect(keyboard, density) {
        snapshotFlow { keyboard.getBottom(density) == 0 }.collect { hidden ->
            if (hidden && keepsHeightWhileKeyboardHides) {
                if (sheet.targetValue == BottomSheetHeight.Expanded) sheet.targetValue = BottomSheetHeight.Open
                keepsHeightWhileKeyboardHides = false
            }
        }
    }
    val isExpanded = remember(sheet) { { sheet.targetValue == BottomSheetHeight.Expanded } }
    val expandToShowChild = remember(sheet) {
        object : BringIntoViewResponder {
            override fun calculateRectForParent(localRect: Rect) = localRect

            override suspend fun bringChildIntoView(localRect: () -> Rect?) {
                val rect = localRect() ?: return
                if (rect.bottom > sheet.offset) expand()
            }
        }
    }
    UnstyledDrawer(
        state = sheet,
        presentation = DrawerPresentation.Overlay,
        overlay = {
            Overlay {
                val dismissOnTap = if (currentVisible) Modifier.pointerInput(Unit) { detectTapGestures { currentOnDismiss() } } else Modifier
                Box(
                    Modifier
                        .fillMaxSize()
                        .graphicsLayer { alpha = openFraction() }
                        .background(scrim)
                        .pointerInput(keyboardFirst) { with(keyboardFirst) { moveKeyboardOnDirectDrag() } }
                        .then(dismissOnTap),
                )
            }
        },
    ) {
        Viewport(
            Modifier.fillMaxSize().then(if (keyboardHeightWhenClosing == null) Modifier.windowInsetsPadding(if (sizedForKeyboardTarget) keyboardTarget else keyboard) else Modifier.consumeWindowInsets(keyboard)),
            panelAlignment = DrawerPanelAlignment.Center,
            windowInsets = WindowInsets.statusBars.union(WindowInsets(bottom = keyboardHeightWhenClosing ?: 0)),
        ) {
            Panel(
                Modifier.widthIn(max = BottomSheetDefaults.SheetMaxWidth).fillMaxWidth().graphicsLayer {
                    alpha = screenOpacity()
                    translationY = if (keyboardHeightWhenClosing == null && sizedForKeyboardTarget) (keyboardTarget.getBottom(this) - keyboard.getBottom(this)).toFloat() else 0f
                },
                overscrollEffect = panelStretch,
            ) {
                Surface(shape = BottomSheetDefaults.ExpandedShape, color = BottomSheetDefaults.ContainerColor) {
                    Column(
                        Modifier
                            .animateVisibleSheetHeight(isExpanded, keepsHeight = { keyboardFirst.keepsSheetHeight || keepsHeightWhileKeyboardHides }, sheetIsSettled = { sheet.isIdle })
                            .bringIntoViewResponder(expandToShowChild)
                            .nestedScroll(keyboardFirst)
                            .nestedScroll(NoNestedScroll, keyboardFirst.directDrags)
                            .pointerInput(keyboardFirst) { with(keyboardFirst) { moveKeyboardOnDirectDrag() } }
                            .pointerInput(sheet) { expandOnUnusedWheelDown(::expand) }
                            .onFocusChanged { bodyHasFocus = it.hasFocus },
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        DragHandle { BottomSheetDefaults.DragHandle() }
                        CompositionLocalProvider(
                            LocalOverscrollFactory provides (LocalOverscrollFactory.current ?: StretchOverscrollFactory),
                            LocalListSection provides null,
                            LocalBackdrop provides null,
                        ) {
                            BottomSheetScope.body()
                        }
                    }
                }
            }
        }
    }
}

private fun Modifier.animateVisibleSheetHeight(isExpanded: () -> Boolean, keepsHeight: () -> Boolean, sheetIsSettled: () -> Boolean): Modifier =
    this then VisibleSheetHeightAnimation(isExpanded, keepsHeight, sheetIsSettled)

private data class VisibleSheetHeightAnimation(val isExpanded: () -> Boolean, val keepsHeight: () -> Boolean, val sheetIsSettled: () -> Boolean) : ModifierNodeElement<VisibleSheetHeightAnimationNode>() {
    override fun create() = VisibleSheetHeightAnimationNode(isExpanded, keepsHeight, sheetIsSettled)

    override fun update(node: VisibleSheetHeightAnimationNode) {
        node.isExpanded = isExpanded
        node.keepsHeight = keepsHeight
        node.sheetIsSettled = sheetIsSettled
    }
}

private class VisibleSheetHeightAnimationNode(var isExpanded: () -> Boolean, var keepsHeight: () -> Boolean, var sheetIsSettled: () -> Boolean) : Modifier.Node(), LayoutModifierNode {
    private var shownHeight: Animatable<Float, AnimationVector1D>? = null
    private var requestedHeight = 0f
    private var wasExpanded = false
    private var keptMaxHeight: Int? = null
    private var lastMaxHeight = 0

    override fun MeasureScope.measure(measurable: Measurable, constraints: Constraints): MeasureResult {
        val placeable = measurable.measure(constraints)
        val expanded = isExpanded()
        val kept = if (keepsHeight()) minOf(keptMaxHeight ?: lastMaxHeight.takeIf { it > 0 } ?: constraints.maxHeight, constraints.maxHeight) else null
        keptMaxHeight = kept
        val openHeight = openBottomSheetHeight(constraints.maxHeight.toDp(), placeable.height.toDp()).toPx()
        val visibleHeight = when {
            !expanded -> openHeight
            kept != null -> maxOf(minOf(placeable.height, kept).toFloat(), openHeight)
            else -> minOf(placeable.height, constraints.maxHeight).toFloat()
        }
        val shown = shownHeight
        val maxHeightChanged = constraints.maxHeight != lastMaxHeight
        lastMaxHeight = constraints.maxHeight
        if (shown == null || expanded != wasExpanded || maxHeightChanged || !sheetIsSettled()) {
            shownHeight = Animatable(visibleHeight)
            requestedHeight = visibleHeight
            wasExpanded = expanded
        } else if (requestedHeight != visibleHeight) {
            requestedHeight = visibleHeight
            coroutineScope.launch { shown.animateTo(visibleHeight, SheetMotion) }
        }
        val height = shownHeight!!.value.let { if (it == visibleHeight && kept == null) placeable.height else it.roundToInt() }
        return layout(placeable.width, height) { placeable.place(0, 0) }
    }
}

private object NoNestedScroll : NestedScrollConnection

private suspend fun PointerInputScope.expandOnUnusedWheelDown(expand: () -> Unit) = awaitPointerEventScope {
    while (true) {
        val event = awaitPointerEvent()
        if (event.type == PointerEventType.Scroll && event.changes.any { !it.isConsumed && it.scrollDelta.y > 0f }) expand()
    }
}

private class SheetDragsMoveKeyboardFirst(
    private val keyboard: KeyboardDrag?,
    private val onKeyboardHidden: (sheetWasDragged: Boolean) -> Unit,
) : NestedScrollConnection {
    val directDrags = NestedScrollDispatcher()
    var keepsSheetHeight by mutableStateOf(false)
        private set
    private var fingerDown = false
    private var movingKeyboard = false
    private var sheetWasDragged = false

    private fun keyboardHeight() = keyboard?.height ?: 0

    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset =
        if (keyboard != null && movingKeyboard && available.y < 0f) Offset(0f, keyboard.lowerBy(available.y)) else Offset.Zero

    override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
        if (keyboard == null || !fingerDown || source != NestedScrollSource.UserInput || available.y <= 0f) return Offset.Zero
        if (keyboardHeight() == 0) {
            if (movingKeyboard) sheetWasDragged = true
            return Offset.Zero
        }
        movingKeyboard = true
        keepsSheetHeight = true
        val lowered = keyboard.lowerBy(available.y)
        if (available.y - lowered > 0.5f) sheetWasDragged = true
        return Offset(0f, lowered)
    }

    override suspend fun onPreFling(available: Velocity): Velocity {
        if (keyboard == null || !movingKeyboard) return Velocity.Zero
        movingKeyboard = false
        val keyboardWasUp = keyboardHeight() > 0
        keyboard.settle(available.y)
        val keyboardHidden = keyboardHeight() == 0
        if (keyboardHidden) onKeyboardHidden(sheetWasDragged)
        keepsSheetHeight = false
        return if (keyboardWasUp || (keyboardHidden && !sheetWasDragged)) available else Velocity.Zero
    }

    suspend fun PointerInputScope.moveKeyboardOnDirectDrag() = awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        movingKeyboard = false
        sheetWasDragged = false
        fingerDown = true
        try {
            var watchingDirectDrag = keyboardHeight() > 0
            var draggingDirectly = false
            var travelled = Offset.Zero
            val velocity = VelocityTracker()
            while (true) {
                val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: break
                velocity.addPointerInputChange(change)
                if (!change.pressed) break
                if (draggingDirectly) {
                    dragDirectly(change.positionChange().y)
                    change.consume()
                    continue
                }
                if (!watchingDirectDrag) continue
                if (change.isConsumed) {
                    watchingDirectDrag = false
                    continue
                }
                travelled += change.positionChange()
                if (abs(travelled.y) < viewConfiguration.touchSlop) continue
                watchingDirectDrag = false
                if (abs(travelled.y) > abs(travelled.x)) {
                    draggingDirectly = true
                    change.consume()
                }
            }
            if (draggingDirectly) flingDirectly(velocity.calculateVelocity())
        } finally {
            fingerDown = false
        }
    }

    private fun dragDirectly(deltaY: Float) {
        val delta = Offset(0f, deltaY)
        val takenBefore = directDrags.dispatchPreScroll(delta, NestedScrollSource.UserInput)
        directDrags.dispatchPostScroll(Offset.Zero, delta - takenBefore, NestedScrollSource.UserInput)
    }

    private fun flingDirectly(velocity: Velocity) {
        val vertical = Velocity(0f, velocity.y)
        directDrags.coroutineScope.launch {
            val takenBefore = directDrags.dispatchPreFling(vertical)
            directDrags.dispatchPostFling(Velocity.Zero, vertical - takenBefore)
        }
    }
}

private class ClosingDragsMoveSheetOnlyFromListTop(
    private val listOverscroll: OverscrollEffect,
    private val listIsAtTop: () -> Boolean,
) : OverscrollEffect by listOverscroll, NestedScrollConnection {
    private var keptScroll = Offset.Zero
    private var keptFling = Velocity.Zero
    private var fingerDown = false
    private var dragStartedAtTop = false

    suspend fun PointerInputScope.noteWhereDragsStart() = awaitEachGesture {
        awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        dragStartedAtTop = listIsAtTop()
        fingerDown = true
        do {
            val event = awaitPointerEvent(PointerEventPass.Initial)
        } while (event.changes.any { it.pressed })
        fingerDown = false
    }

    override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
        keptScroll = if (fingerDown && dragStartedAtTop) Offset.Zero else Offset(0f, available.y.coerceAtLeast(0f))
        return keptScroll
    }

    override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
        keptFling = if (dragStartedAtTop) Velocity.Zero else Velocity(0f, available.y.coerceAtLeast(0f))
        return keptFling
    }

    override fun applyToScroll(delta: Offset, source: NestedScrollSource, performScroll: (Offset) -> Offset): Offset =
        listOverscroll.applyToScroll(delta, source) { available ->
            keptScroll = Offset.Zero
            performScroll(available) - keptScroll
        }

    override suspend fun applyToFling(velocity: Velocity, performFling: suspend (Velocity) -> Velocity) =
        listOverscroll.applyToFling(velocity) { available ->
            keptFling = Velocity.Zero
            performFling(available) - keptFling
        }
}

private data object StretchOverscrollFactory : OverscrollFactory {
    override fun createOverscrollEffect(): OverscrollEffect = StretchOverscrollEffect()
}

private class StretchOverscrollEffect : OverscrollEffect {
    private var pullPx by mutableFloatStateOf(0f)
    private var heightPx = 0

    override val isInProgress: Boolean
        get() = pullPx != 0f

    override fun applyToScroll(delta: Offset, source: NestedScrollSource, performScroll: (Offset) -> Offset): Offset {
        val released = releasePull(delta.y)
        val remaining = delta.copy(y = delta.y - released)
        val consumed = performScroll(remaining)
        val pulled = if (source == NestedScrollSource.UserInput) remaining.y - consumed.y else 0f
        pullPx += pulled
        return consumed + Offset(0f, released + pulled)
    }

    private fun releasePull(deltaY: Float): Float {
        val released = when {
            pullPx < 0f && deltaY > 0f -> minOf(deltaY, -pullPx)
            pullPx > 0f && deltaY < 0f -> maxOf(deltaY, -pullPx)
            else -> 0f
        }
        pullPx += released
        return released
    }

    override suspend fun applyToFling(velocity: Velocity, performFling: suspend (Velocity) -> Velocity) {
        if (pullPx == 0f) {
            performFling(velocity)
            return
        }
        coroutineScope {
            launch {
                animate(pullPx, 0f, animationSpec = SheetMotion) { value, _ -> pullPx = value }
            }
            performFling(velocity)
        }
    }

    private val stretch: Float
        get() {
            val fraction = abs(pullPx) / heightPx.coerceAtLeast(1)
            return 1f + MAX_STRETCH * fraction / (1f + fraction)
        }

    override val node: DelegatableNode = object : Modifier.Node(), LayoutModifierNode {
        override fun MeasureScope.measure(measurable: Measurable, constraints: Constraints): MeasureResult {
            val placeable = measurable.measure(constraints)
            heightPx = placeable.height
            return layout(placeable.width, placeable.height) {
                placeable.placeWithLayer(0, 0) {
                    scaleY = stretch
                    transformOrigin = TransformOrigin(0.5f, if (pullPx < 0f) 1f else 0f)
                }
            }
        }
    }
}
