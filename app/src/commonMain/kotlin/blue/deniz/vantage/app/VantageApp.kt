package blue.deniz.vantage.app

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.asPaddingValues
import blue.deniz.vantage.app.ui.Backdrop
import blue.deniz.vantage.app.ui.ClearFocusWhenKeyboardCloses

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import blue.deniz.vantage.app.ui.EventPageGrowingFromCard
import blue.deniz.vantage.app.ui.LocalScreenTransition
import blue.deniz.vantage.app.ui.ScreenTransition
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.ui.Alignment
import blue.deniz.vantage.app.ui.LocalTabBarHeight
import blue.deniz.vantage.app.ui.rememberBackdrop
import blue.deniz.vantage.app.ui.LocalBackdrop
import blue.deniz.vantage.app.ui.glassBar
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.drawBehind
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusTarget
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import blue.deniz.vantage.app.screens.MyEventsScreen
import blue.deniz.vantage.app.screens.EventsViewControlsClearance
import blue.deniz.vantage.app.screens.ChooseFeedSheet
import blue.deniz.vantage.app.screens.CreateEventScreen
import blue.deniz.vantage.app.screens.MarkKind
import blue.deniz.vantage.app.screens.MarkSheet
import blue.deniz.vantage.app.screens.NewEntrySheet
import blue.deniz.vantage.app.screens.EditEventScreen
import blue.deniz.vantage.app.screens.EventDetailsScreen
import blue.deniz.vantage.app.screens.EditFromLinkScreen
import blue.deniz.vantage.app.screens.EventFromLinkScreen
import blue.deniz.vantage.app.screens.EventListFiltersSheet

import blue.deniz.vantage.app.screens.FeedScreen
import blue.deniz.vantage.app.screens.HomeScreen
import blue.deniz.vantage.app.screens.AboutScreen
import blue.deniz.vantage.app.screens.FollowingSettingsScreen
import blue.deniz.vantage.app.screens.SystemCalendarSettingsScreen
import blue.deniz.vantage.app.screens.HomeSettingsScreen
import blue.deniz.vantage.app.screens.AppearanceSettingsScreen
import blue.deniz.vantage.app.screens.NotificationSettingsScreen
import blue.deniz.vantage.app.screens.BackupSettingsScreen
import blue.deniz.vantage.app.screens.SetupScreen
import blue.deniz.vantage.app.screens.SettingsScreen
import blue.deniz.vantage.app.screens.TagSettingsScreen
import blue.deniz.vantage.app.ui.AppIcons
import blue.deniz.vantage.app.ui.BottomSheetHost
import blue.deniz.vantage.app.ui.LocalScreenOpacity
import blue.deniz.vantage.app.ui.Icon
import blue.deniz.vantage.app.ui.SnackbarHost
import blue.deniz.vantage.app.ui.rememberAnyBottomSheetOpen
import blue.deniz.vantage.app.ui.Text
import blue.deniz.vantage.app.ui.VantageTheme
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.DisposableEffect

sealed interface AddedEvent {
    data class Added(val id: String) : AddedEvent
    data class AlreadyStored(val id: String) : AddedEvent
    data class Failed(val problem: String) : AddedEvent
}

private data class TabItem(val tab: Route.Tab, val label: String, val icon: ImageVector, val selectedIcon: ImageVector)

private val TAB_BAR_HEIGHT = 52.dp

private val tabs = listOf(
    TabItem(Route.Home, "Home", AppIcons.Home, AppIcons.HomeSelected),
    TabItem(Route.MyEvents, "My Events", AppIcons.Calendar, AppIcons.CalendarSelected),
    TabItem(Route.Settings, "Settings", AppIcons.Settings, AppIcons.SettingsSelected),
)

private data class StackEntry(val depth: Int, val screen: OpenedScreen) {
    val route get() = screen.route
    val screenStateKey get() = if (route is Route.Tab) "tabs" else "screen/${screen.openingId}"
}

private fun Modifier.blockingPointerInputWhile(blocked: () -> Boolean) = pointerInput(Unit) {
    awaitPointerEventScope {
        while (true) {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            if (blocked()) event.changes.forEach { it.consume() }
        }
    }
}

private val ScreenFadeIn = tween<Float>(210, delayMillis = 90)
private val ScreenFadeOut = tween<Float>(90)

private fun AnimatedContentTransitionScope<StackEntry>.screenTransition(): ContentTransform {
    val pushing = targetState.depth > initialState.depth
    val popping = targetState.depth < initialState.depth
    if (!pushing && !popping) return fadeIn(ScreenFadeIn) togetherWith fadeOut(ScreenFadeOut)
    val page = if (pushing) targetState.route else initialState.route
    if (page is Route.EventDetails && page.openedFromCard) {
        return (EnterTransition.None togetherWith ExitTransition.KeepUntilTransitionsFinished).apply { if (popping) targetContentZIndex = -1f }
    }
    val direction = if (pushing) 1 else -1
    return (slideInHorizontally(tween(300)) { direction * it / 4 } + fadeIn(ScreenFadeIn)) togetherWith
        (slideOutHorizontally(tween(300)) { -direction * it / 4 } + fadeOut(ScreenFadeOut))
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun VantageApp(state: AppState) {
    val navigator = state.navigator
    val screenStates = rememberSaveableStateHolder()
    val screenValues = remember { ScreenValueStore() }
    val sessionValues = remember { mutableMapOf<String, MutableState<*>>() }
    val stackedScreenKeys = navigator.screens.withIndex()
        .map { (depth, screen) -> StackEntry(depth, screen).screenStateKey }
        .filterNot { it == "tabs" }
        .toSet()
    var keptScreenKeys by remember { mutableStateOf(emptySet<String>()) }
    LaunchedEffect(stackedScreenKeys) {
        for (gone in keptScreenKeys - stackedScreenKeys) {
            screenStates.removeState(gone)
            screenValues.forget(gone)
        }
        keptScreenKeys = stackedScreenKeys
    }
    SystemBackHandler(enabled = navigator.canGoBack) { navigator.goBack() }
    ClearFocusWhenKeyboardCloses()
    LaunchedEffect(Unit) {
        if (!state.settings.setupFinished && navigator.current is Route.Tab) navigator.open(Route.Setup)
    }
    val tabBackdrop = rememberBackdrop()
    CompositionLocalProvider(LocalSessionValues provides sessionValues) {
        VantageTheme(state.settings.themeAccent, state.settings.fontSizes) {
            KeyboardShortcutHost(onShortcut = state::handleShortcut) {
                BottomSheetHost(aboveSheets = { SnackbarsOutsideTabBar(state, onTab = navigator.current is Route.Tab) }) {
                    Surface(Modifier.fillMaxSize()) {
                        Column(Modifier.fillMaxSize()) {
                            if (runsInAndroidBrowser) AndroidAppBanner()
                            Box(Modifier.fillMaxWidth().weight(1f)) {
                            SharedTransitionLayout(Modifier.fillMaxSize()) {
                                AnimatedContent(
                                    targetState = StackEntry(navigator.screens.lastIndex, navigator.screens.last()),
                                    transitionSpec = { screenTransition() },
                                    contentKey = { it.screenStateKey },
                                ) { entry ->
                                    val screenTransition = remember(this) { ScreenTransition(this@SharedTransitionLayout, this) }
                                    val opacity = transition.animateFloat(
                                        transitionSpec = { if (targetState == EnterExitState.Visible) ScreenFadeIn else ScreenFadeOut },
                                        label = "screen opacity",
                                    ) { if (it == EnterExitState.Visible) 1f else 0f }
                                    screenStates.SaveableStateProvider(entry.screenStateKey) {
                                        CompositionLocalProvider(
                                            LocalScreenValues provides screenValues.valuesFor(entry.screenStateKey),
                                            LocalStackDepth provides entry.depth,
                                            LocalScreenOpacity provides remember(opacity) { { opacity.value } },
                                            LocalScreenTransition provides screenTransition,
                                        ) {
                                            Box(Modifier.blockingPointerInputWhile { transition.targetState != EnterExitState.Visible }, propagateMinConstraints = true) {
                                                when (val route = entry.route) {
                                                    is Route.Tab -> TabShell(state, route, tabBackdrop, showsSnackbars = route == navigator.current)
                                                    is Route.EventDetails -> if (route.openedFromCard) {
                                                        EventPageGrowingFromCard(route.id) { EventDetailsScreen(state, route.id) }
                                                    } else {
                                                        EventDetailsScreen(state, route.id)
                                                    }
                                                    is Route.EventFromLink -> EventFromLinkScreen(state, route.intent)
                                                    is Route.Feed -> FeedScreen(state, route.feed)
                                                    is Route.CreateEvent -> CreateEventScreen(state, route.sharedText, route.day, route.draft)
                                                    is Route.EditEvent -> EditEventScreen(state, route.id, route.draft)
                                                    is Route.EditFromLink -> EditFromLinkScreen(state, route.request)
                                                    Route.AppearanceSettings -> AppearanceSettingsScreen(state)
                                                    Route.NotificationSettings -> NotificationSettingsScreen(state)
                                                    Route.BackupSettings -> BackupSettingsScreen(state)
                                                    Route.Setup -> SetupScreen(state)
                                                    Route.HomeSettings -> HomeSettingsScreen(state)
                                                    Route.TagSettings -> TagSettingsScreen(state)
                                                    Route.SystemCalendarSettings -> SystemCalendarSettingsScreen(state)
                                                    Route.FollowingSettings -> FollowingSettingsScreen(state)
                                                    Route.About -> AboutScreen(state)
                                                    Route.DeveloperTools -> DeveloperToolsScreen(state)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            SlidingTabBar(
                                visible = navigator.current is Route.Tab,
                                selected = navigator.currentTab,
                                backdrop = tabBackdrop,
                                onSelect = navigator::switchTab,
                                modifier = Modifier.align(Alignment.BottomCenter),
                            )
                            }
                        }
                        EventActionsSheet(state)
                        LinkActionsSheet(state)
                        MarkSheet(state)
                        NewEntrySheet(state)
                        ChooseFeedSheet(state)
                    }
                }
            }
        }
    }
}

@Composable
private fun KeyboardShortcutHost(onShortcut: (KeyEvent) -> Boolean, content: @Composable () -> Unit) {
    val focus = remember { FocusRequester() }
    var holdsFocus by remember { mutableStateOf(false) }
    LaunchedEffect(holdsFocus) { if (!holdsFocus) focus.requestFocus() }
    Box(
        Modifier
            .fillMaxSize()
            .onKeyEvent(onShortcut)
            .onFocusChanged { holdsFocus = it.hasFocus }
            .focusRequester(focus)
            .focusTarget(),
    ) { content() }
}

@Composable
private fun SlidingTabBar(visible: Boolean, selected: Route.Tab, backdrop: Backdrop, onSelect: (Route.Tab) -> Unit, modifier: Modifier = Modifier) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = slideInVertically(tween(300)) { it },
        exit = slideOutVertically(tween(300)) { it },
    ) {
        CompositionLocalProvider(LocalBackdrop provides backdrop) { TabBar(selected, onSelect) }
    }
}

@Composable
private fun TabBar(selected: Route.Tab, onSelect: (Route.Tab) -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(
        Modifier
            .fillMaxWidth()
            .glassBar(colors.surfaceContainer)
            .drawBehind { drawLine(colors.outlineVariant, Offset.Zero, Offset(size.width, 0f), 1f) }
            .navigationBarsPadding()
            .height(TAB_BAR_HEIGHT)
            .selectableGroup(),
    ) {
        for (item in tabs) {
            val isSelected = item.tab == selected
            val tint = if (isSelected) colors.primary else colors.onSurfaceVariant
            Column(
                Modifier.weight(1f).fillMaxHeight().selectable(isSelected, role = Role.Tab, onClick = { onSelect(item.tab) }),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(if (isSelected) item.selectedIcon else item.icon, tint = tint)
                Text(item.label, color = tint, maxLines = 1, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun SnackbarsOutsideTabBar(state: AppState, onTab: Boolean) {
    val anySheetOpen by rememberAnyBottomSheetOpen()
    if (onTab && !anySheetOpen) return
    Box(
        Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime)),
        contentAlignment = Alignment.BottomCenter,
    ) { SnackbarHost(state.snackbars) }
}

@Composable
private fun TabShell(state: AppState, tab: Route.Tab, backdrop: Backdrop, showsSnackbars: Boolean) {
    val anySheetOpen by rememberAnyBottomSheetOpen()
    var showingListFilters by rememberSaveable { mutableStateOf(false) }
    val tabStates = rememberSaveableStateHolder()
    SystemBackHandler(enabled = tab != Route.Home) { state.navigator.switchTab(Route.Home) }
    val tabBarHeight = TAB_BAR_HEIGHT + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    CompositionLocalProvider(LocalBackdrop provides backdrop) {
        Scaffold(
            snackbarHost = {
                if (showsSnackbars && !anySheetOpen) {
                    SnackbarHost(state.snackbars, Modifier.padding(bottom = tabBarHeight + if (tab == Route.MyEvents) EventsViewControlsClearance else 0.dp))
                }
            },
            contentWindowInsets = WindowInsets(0),
        ) {
            CompositionLocalProvider(LocalTabBarHeight provides tabBarHeight) {
                AnimatedContent(
                    targetState = tab,
                    transitionSpec = {
                        (fadeIn(tween(210, delayMillis = 90)) + scaleIn(tween(210, delayMillis = 90), initialScale = 0.92f)) togetherWith
                            fadeOut(tween(90))
                    },
                ) { shown ->
                    tabStates.SaveableStateProvider(shown.toString()) {
                        when (shown) {
                            Route.Home -> HomeScreen(state)
                            Route.MyEvents -> MyEventsScreen(state, onOpenFilters = { showingListFilters = true })
                            Route.Settings -> SettingsScreen(state)
                        }
                    }
                }
            }
        }
    }
    if (tab == Route.MyEvents) EventListFiltersSheet(state, visible = showingListFilters, onDismiss = { showingListFilters = false })
}

@Composable
fun SelectAllShortcut(state: AppState, onSelectAll: () -> Unit) {
    val currentOnSelectAll by rememberUpdatedState(onSelectAll)
    DisposableEffect(state) {
        val action = { currentOnSelectAll() }
        state.addSelectAllAction(action)
        onDispose { state.removeSelectAllAction(action) }
    }
}
