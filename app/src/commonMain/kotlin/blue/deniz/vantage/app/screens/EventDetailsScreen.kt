package blue.deniz.vantage.app.screens

import blue.deniz.vantage.app.ui.color
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.ui.layout.ContentScale
import blue.deniz.vantage.app.SystemBackHandler
import blue.deniz.vantage.app.ui.BACKGROUND_DIM_ALPHA
import blue.deniz.vantage.app.ui.OpenEvntImage
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.draw.shadow
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.material3.LocalContentColor
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.ui.text.font.FontWeight
import blue.deniz.vantage.app.ui.Spinner
import blue.deniz.vantage.app.ui.EventBackground
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import blue.deniz.vantage.app.collectAcrossSession
import blue.deniz.vantage.core.statusOn
import blue.deniz.vantage.core.CalendarMarks
import blue.deniz.vantage.core.nextOrLastOccurrenceAt
import directory.evnt.sortedByStart
import blue.deniz.vantage.app.AppState
import blue.deniz.vantage.app.Route
import blue.deniz.vantage.app.ui.FailureRow
import blue.deniz.vantage.app.ui.describe
import blue.deniz.vantage.app.ui.AppIcons
import blue.deniz.vantage.app.ui.Button
import blue.deniz.vantage.app.ui.Card
import blue.deniz.vantage.app.ui.EventFormatting
import blue.deniz.vantage.app.ui.IconButton
import blue.deniz.vantage.app.ui.IconSize
import blue.deniz.vantage.app.ui.ListSection
import blue.deniz.vantage.app.ui.MarkdownView
import blue.deniz.vantage.app.ui.ScreenHeader
import blue.deniz.vantage.app.ui.SectionTitle
import blue.deniz.vantage.app.ui.Spacing
import blue.deniz.vantage.app.ui.UNTITLED_EVENT
import blue.deniz.vantage.app.ui.Text
import blue.deniz.vantage.core.backgroundSplashMedia
import blue.deniz.vantage.app.ui.label
import blue.deniz.vantage.app.ui.translatedName
import blue.deniz.vantage.core.EventError
import blue.deniz.vantage.core.EventFormat
import blue.deniz.vantage.core.EventSource
import blue.deniz.vantage.core.isEditableHere
import blue.deniz.vantage.core.EventsLink
import blue.deniz.vantage.core.Markdown
import blue.deniz.vantage.core.MarkdownBlock
import blue.deniz.vantage.core.ResolvedEvent
import directory.evnt.Component
import directory.evnt.Media
import directory.evnt.Occurrence
import directory.evnt.OpenEvnt
import directory.evnt.translate
import kotlin.time.Instant
import kotlinx.coroutines.launch
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.ui.draw.alpha
import blue.deniz.vantage.app.ui.ContentMaxWidth

private val COVER_HEIGHT = 80.dp
private val PULL_STRETCH_AT_THRESHOLD = 160.dp
private const val PULLED_COVER_DIM_ALPHA = 0.25f
private const val EVENT_ART_KEY = "event art"
private const val REFRESH_PILL_ALPHA = 0.7f
private val REFRESH_PILL_BORDER_WIDTH = 1.dp
private const val COVER_ANIMATION_MILLIS = 300
private val HEADER_SHADOW_ELEVATION = 4.dp
private const val LONG_HEADER_TITLE_LENGTH = 40
private val WIDE_LAYOUT_MIN_WIDTH = 768.dp
private val WIDE_CONTENT_MAX_WIDTH = 1080.dp

fun eventPageKey(id: String) = "event $id"

@Composable
fun EventDetailsScreen(state: AppState, id: String) {
    val event = collectAcrossSession(eventPageKey(id), null) { state.store.observe(id) }
    val scope = rememberCoroutineScope()
    var refreshing by remember { mutableStateOf(false) }

    fun refresh() {
        if (refreshing) return
        refreshing = true
        scope.launch {
            try {
                state.store.refresh(id)
            } finally {
                refreshing = false
            }
        }
    }

    val current = event
    EventDetailsLayout(
        state,
        current,
        actions = {
            if (current?.source?.isEditableHere == true) {
                IconButton(AppIcons.Pencil, "Edit", onClick = { state.navigator.open(Route.EditEvent(id)) })
            }
            IconButton(AppIcons.DotsVertical, "More", onClick = { current?.let(state::openActionsSheet) ?: state.openActionsSheet(id) })
        },
        tags = { EventTags(state, id, shareLink = current?.let { EventsLink.shareLinkTo(it.source) }) },
        changes = { UnseenChanges(state, id) },
        onRefresh = if (current?.source is EventSource.Remote) ::refresh else null,
        refreshing = refreshing,
    )
}

@Composable
private fun UnseenChanges(state: AppState, id: String) {
    val formatting = state.formatting
    val unseenByEvent = collectAcrossSession("unseen differences", null) { state.store.observeUnseenDifferences() }
    val unseen = unseenByEvent?.get(id).orEmpty()
    var shown by remember(id) { mutableStateOf(unseen) }
    LaunchedEffect(unseen) {
        if (unseen.isEmpty()) return@LaunchedEffect
        shown = (shown + unseen).distinct()
        state.store.markChangesSeen(id)
    }
    if (shown.isEmpty()) return
    Card(Modifier.fillMaxWidth()) {
        Text("What Changed", style = MaterialTheme.typography.titleSmall)
        for (difference in shown) Text(formatting.describe(difference, state.preferredLanguages), style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun EventFromLinkScreen(state: AppState, intent: EventsLink.Intent) {
    val linked = remember(intent) { ResolvedEvent.fromEventsLinkIntent(intent) }
    if (linked == null) {
        Column(Modifier.fillMaxSize()) {
            ScreenHeader("Event", onBack = { state.navigator.goBack() })
            Text("No event reference provided.", Modifier.padding(Spacing.md), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }
    var storedCopy by remember(linked) { mutableStateOf<StoredCopy?>(null) }
    LaunchedEffect(linked) { storedCopy = StoredCopy(state.store.findStoredCopyId(linked)) }
    val copy = storedCopy
    when {
        copy == null -> EventDetailsLayout(state, state.previews.lastResolved(linked))
        copy.id != null -> EventDetailsScreen(state, copy.id)
        else -> RemoteEventDetails(state, linked, onStoredCopyFound = { storedCopy = StoredCopy(it) })
    }
}

private data class StoredCopy(val id: String?)

@Composable
private fun RemoteEventDetails(state: AppState, linked: ResolvedEvent, onStoredCopyFound: (id: String) -> Unit) {
    var attempt by remember { mutableIntStateOf(0) }
    var retrying by remember { mutableStateOf(false) }
    val event by produceState(state.previews.lastResolved(linked), linked, attempt) {
        value = state.previews.resolve(linked)
        retrying = false
    }
    LaunchedEffect(event) { event?.let { state.store.findStoredCopyId(it) }?.let(onStoredCopyFound) }
    EventDetailsLayout(state, event, actions = {
        event?.let { resolved -> IconButton(AppIcons.DotsVertical, "More", onClick = { state.openActionsSheet(resolved) }) }
    }, importAction = {
        event?.let { resolved -> AddToMyEventsButton(state, resolved) }
    }, onRefresh = {
        retrying = true
        attempt++
    }, refreshing = retrying)
}

@Composable
private fun AddToMyEventsButton(state: AppState, event: ResolvedEvent) {
    if (event.source == EventSource.Unknown || event.format == EventFormat.Unknown) return
    val scope = rememberCoroutineScope()
    var adding by remember { mutableStateOf(false) }
    Button(
        "Add to My Events",
        busy = adding,
        onClick = {
            adding = true
            scope.launch {
                try {
                    state.navigator.replace(Route.EventDetails(state.store.add(event)))
                } finally {
                    adding = false
                }
            }
        },
        modifier = Modifier.fillMaxWidth(),
        primary = true,
        enabled = !adding,
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
private fun EventDetailsLayout(
    state: AppState,
    event: ResolvedEvent?,
    actions: @Composable RowScope.() -> Unit = {},
    importAction: @Composable () -> Unit = {},
    tags: @Composable () -> Unit = {},
    changes: @Composable () -> Unit = {},
    onRefresh: (() -> Unit)? = null,
    refreshing: Boolean = false,
) {
    val formatting = state.formatting
    val marks = rememberCalendarMarks(state, shown = true)
    val cover = remember(event?.data) { event?.data?.backgroundSplashMedia() }
    val title = event?.data?.translatedName(state.preferredLanguages) ?: UNTITLED_EVENT.takeIf { event != null }
    val scroll = rememberScrollState()
    val density = LocalDensity.current
    var titleRowHeight by remember { mutableIntStateOf(0) }
    val coverHeight by animateDpAsState(if (cover != null) COVER_HEIGHT else 0.dp, tween(COVER_ANIMATION_MILLIS))
    val shownCoverHeight = (coverHeight - with(density) { scroll.value.toDp() }).coerceAtLeast(0.dp)
    val pull = rememberPullToRefreshState()
    val pullStretch = PULL_STRETCH_AT_THRESHOLD * pull.distanceFraction.coerceAtLeast(0f)
    val coverDimAlpha = BACKGROUND_DIM_ALPHA + (PULLED_COVER_DIM_ALPHA - BACKGROUND_DIM_ALPHA) * pull.distanceFraction.coerceIn(0f, 1f)
    var viewingArt by remember { mutableStateOf(false) }

    SharedTransitionLayout {
        val art = rememberSharedContentState(EVENT_ART_KEY)
        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                .pullToRefresh(refreshing, pull, enabled = cover != null || onRefresh != null, onRefresh = { onRefresh?.invoke() }),
        ) {
            val wide = maxWidth >= WIDE_LAYOUT_MIN_WIDTH
            val contentWidth = if (wide) WIDE_CONTENT_MAX_WIDTH else ContentMaxWidth
            Column(Modifier.fillMaxSize().verticalScroll(scroll)) {
                Spacer(Modifier.statusBarsPadding().height(with(density) { titleRowHeight.toDp() } + coverHeight + pullStretch))
                if (event != null) {
                    EventPageBody(
                        state,
                        event,
                        formatting,
                        marks,
                        state.preferredLanguages,
                        now = state.now,
                        importAction,
                        tags,
                        changes,
                        onRefresh,
                        refreshing,
                        onOpenLinkActions = state::openLinkActionsSheet,
                        wide = wide,
                        modifier = Modifier.widthIn(max = contentWidth).fillMaxWidth().align(Alignment.CenterHorizontally),
                    )
                }
                Spacer(Modifier.navigationBarsPadding().height(Spacing.xl))
            }
            EventPageHeader(
                title = title,
                cover = cover,
                coverHeight = shownCoverHeight + pullStretch,
                refreshPullFraction = if (onRefresh != null) pull.distanceFraction else 0f,
                loading = event == null || refreshing,
                contentWidth = contentWidth,
                onBack = { state.navigator.goBack() },
                onTitleRowHeightChanged = { titleRowHeight = it },
                onCoverClick = { viewingArt = true },
                background = {
                    if (cover != null) {
                        AnimatedVisibility(!viewingArt, Modifier.matchParentSize(), enter = fadeIn(), exit = fadeOut()) {
                            EventBackground(cover, state.preferredLanguages, Modifier.sharedBounds(art, this).fillMaxSize(), dimAlpha = coverDimAlpha)
                        }
                    }
                },
                actions = actions,
            )
            if (cover != null) {
                AnimatedVisibility(viewingArt, enter = fadeIn(), exit = fadeOut()) {
                    EventArtViewer(cover, state.preferredLanguages, Modifier.sharedBounds(art, this), onClose = { viewingArt = false })
                }
            }
        }
    }
}

@Composable
private fun EventArtViewer(media: Media, preferredLanguages: List<String>, artModifier: Modifier, onClose: () -> Unit) {
    SystemBackHandler(enabled = true, handlesEscape = true, onBack = onClose)
    Box(Modifier.fillMaxSize().background(Color.Black).clickable(onClickLabel = "Close", onClick = onClose)) {
        OpenEvntImage(media, preferredLanguages, artModifier.fillMaxSize().systemBarsPadding(), contentScale = ContentScale.Fit)
    }
}

@Composable
private fun EventPageHeader(
    title: String?,
    cover: Media?,
    coverHeight: Dp,
    refreshPullFraction: Float,
    loading: Boolean,
    contentWidth: Dp,
    onBack: () -> Unit,
    onTitleRowHeightChanged: (Int) -> Unit,
    onCoverClick: () -> Unit,
    background: @Composable BoxScope.() -> Unit,
    actions: @Composable RowScope.() -> Unit,
) {
    val contentColor = if (cover != null) Color.White else MaterialTheme.colorScheme.onSurface
    Box(Modifier.fillMaxWidth().shadow(HEADER_SHADOW_ELEVATION).background(MaterialTheme.colorScheme.surface)) {
        background()
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            Column(Modifier.fillMaxWidth().statusBarsPadding()) {
                Box(
                    Modifier.fillMaxWidth().height(coverHeight).clickable(enabled = cover != null, onClickLabel = "View Art", onClick = onCoverClick),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    if (refreshPullFraction > 0f) {
                        Surface(
                            Modifier.padding(top = Spacing.sm).alpha(refreshPullFraction.coerceAtMost(1f)),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.background.copy(alpha = REFRESH_PILL_ALPHA),
                            border = BorderStroke(REFRESH_PILL_BORDER_WIDTH, MaterialTheme.colorScheme.outlineVariant),
                        ) {
                            if (loading) {
                                Box(Modifier.padding(Spacing.xs)) { Spinner(IconSize.md) }
                            } else {
                                Text(
                                    if (refreshPullFraction >= 1f) "Release to Refresh" else "Pull to Refresh",
                                    Modifier.padding(horizontal = Spacing.md, vertical = Spacing.xs),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    style = MaterialTheme.typography.labelLarge,
                                )
                            }
                        }
                    }
                }
                Row(
                    Modifier
                        .widthIn(max = contentWidth)
                        .fillMaxWidth()
                        .align(Alignment.CenterHorizontally)
                        .onSizeChanged { onTitleRowHeightChanged(it.height) }
                        .padding(horizontal = Spacing.xs, vertical = Spacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                ) {
                    IconButton(AppIcons.ArrowLeft, "Back", onClick = onBack)
                    if (loading) Spinner(IconSize.md, contentColor)
                    Text(
                        title.orEmpty(),
                        Modifier.weight(1f),
                        color = contentColor,
                        fontWeight = FontWeight.Bold,
                        style = if (title.orEmpty().length > LONG_HEADER_TITLE_LENGTH) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineSmall,
                    )
                    actions()
                }
            }
        }
    }
}

@Composable
private fun EventPageBody(
    state: AppState,
    event: ResolvedEvent,
    formatting: EventFormatting,
    marks: CalendarMarks,
    preferredLanguages: List<String>,
    now: Instant,
    importAction: @Composable () -> Unit,
    tags: @Composable () -> Unit,
    changes: @Composable () -> Unit,
    onRetry: (() -> Unit)?,
    retrying: Boolean,
    onOpenLinkActions: (url: String) -> Unit,
    wide: Boolean,
    modifier: Modifier = Modifier,
) {
    val data = event.data
    val occurrences = remember(data) { data?.occurrences.orEmpty().sortedByStart() }
    val next = remember(data, now) { data?.nextOrLastOccurrenceAt(now) }
    Column(modifier) {
        EventHeadline(data?.statusOn(next), next, formatting, now)
        Column(Modifier.padding(horizontal = Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            tags()
            changes()
            event.error?.let { EventErrorRow(it, onRetry, retrying) }
            importAction()
        }
        val links = data?.components.orEmpty().filterIsInstance<Component.Link>()
        val whenAndWhere = @Composable {
            ListSection(Modifier.padding(top = Spacing.sm)) {
                OccurrenceRows(state, data?.translatedName(preferredLanguages) ?: UNTITLED_EVENT, data, occurrences, marks)
                data?.venue?.let { VenueCard(it, preferredLanguages) }
            }
        }
        if (wide) {
            Row {
                Column(Modifier.weight(2f)) {
                    whenAndWhere()
                    data?.let { Description(it, preferredLanguages, onOpenLinkActions) }
                }
                Column(Modifier.weight(1f)) {
                    LinkSection(links, preferredLanguages, onOpenLinkActions, atColumnTop = true)
                    DataSection(event, formatting, onOpenLinkActions, atColumnTop = links.isEmpty())
                }
            }
        } else {
            whenAndWhere()
            data?.let { Description(it, preferredLanguages, onOpenLinkActions) }
            LinkSection(links, preferredLanguages, onOpenLinkActions)
            DataSection(event, formatting, onOpenLinkActions)
        }
    }
}

@Composable
private fun EventHeadline(status: OpenEvnt.Status?, next: Occurrence?, formatting: EventFormatting, now: Instant) {
    val start = next?.start
    val parts = listOfNotNull(
        status?.label,
        start?.let(formatting::localDateOrNull)?.let(formatting::relativeDay),
        next?.let { formatting.formatStartingSoon(it, now, status) },
    )
    if (parts.isEmpty()) return
    Text(
        parts.joinToString(" · "),
        Modifier.padding(start = Spacing.md, end = Spacing.md, top = Spacing.sm),
        color = status?.color ?: MaterialTheme.colorScheme.primary,
        style = MaterialTheme.typography.labelLarge,
    )
}

@Composable
private fun SectionLabel(text: String, atColumnTop: Boolean = false) {
    SectionTitle(text, Modifier.padding(start = Spacing.md, end = Spacing.md, top = if (atColumnTop) 0.dp else Spacing.lg, bottom = Spacing.xs))
}

@Composable
private fun EventErrorRow(error: EventError, onRetry: (() -> Unit)?, retrying: Boolean) {
    FailureRow(
        "${headingOf(error.kind)}: ${error.message}",
        onRetry,
        details = error.issues.orEmpty().map { "${it.path}: ${it.message}" },
        retrying = retrying,
    )
}

private fun headingOf(kind: EventError.Kind) = when (kind) {
    EventError.Kind.Fetch -> "Fetch Error"
    EventError.Kind.JsonParse -> "JSON Parse Error"
    EventError.Kind.Validation -> "Validation Error"
    EventError.Kind.Xrpc -> "XRPC Error"
    EventError.Kind.Parse -> "Parse Error"
    EventError.Kind.UnknownFormat -> "Unknown Format"
    EventError.Kind.Unknown -> "Error"
}

private sealed interface DescriptionText {
    data class Plain(val text: String) : DescriptionText
    data class Formatted(val blocks: List<MarkdownBlock>) : DescriptionText
}

private fun OpenEvnt.descriptionTexts(preferredLanguages: List<String>) = components.orEmpty().mapNotNull { component ->
    when (component) {
        is Component.PlainText -> component.content.translate(preferredLanguages).takeIf { it.isNotBlank() }?.let(DescriptionText::Plain)
        is Component.MarkdownText -> Markdown.parse(component.content.translate(preferredLanguages)).takeIf { it.isNotEmpty() }?.let(DescriptionText::Formatted)
        else -> null
    }
}

@Composable
private fun Description(data: OpenEvnt, preferredLanguages: List<String>, onLinkClick: (url: String) -> Unit) {
    val texts = remember(data, preferredLanguages) { data.descriptionTexts(preferredLanguages) }
    if (texts.isEmpty()) return
    SectionLabel("About")
    SelectionContainer {
        Column(Modifier.padding(horizontal = Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            for (text in texts) when (text) {
                is DescriptionText.Plain -> Text(text.text, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyLarge)
                is DescriptionText.Formatted -> MarkdownView(text.blocks, onLinkClick)
            }
        }
    }
}
