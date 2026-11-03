package blue.deniz.vantage.app.screens

import blue.deniz.vantage.app.ui.color
import kotlinx.datetime.daysUntil
import blue.deniz.vantage.app.EventsView
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
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import blue.deniz.vantage.app.collectAcrossSession
import blue.deniz.vantage.core.statusOn
import blue.deniz.vantage.core.CalendarMarks
import blue.deniz.vantage.core.isCancelled
import blue.deniz.vantage.core.isCancelledOn
import blue.deniz.vantage.core.CalendarPeriod
import blue.deniz.vantage.core.nextOrLastOccurrenceAt
import directory.evnt.sortedByStart
import blue.deniz.vantage.app.AppState
import blue.deniz.vantage.app.CopyMenuItem
import blue.deniz.vantage.app.Route
import blue.deniz.vantage.app.mapsAppLink
import blue.deniz.vantage.app.ui.FailureRow
import blue.deniz.vantage.app.ui.describe
import blue.deniz.vantage.app.ui.AppIcons
import blue.deniz.vantage.app.ui.addressLine
import blue.deniz.vantage.app.ui.addressLineWithFlag
import blue.deniz.vantage.app.ui.icon
import blue.deniz.vantage.app.ui.BottomSheet
import blue.deniz.vantage.app.ui.Button
import blue.deniz.vantage.app.ui.Card
import blue.deniz.vantage.app.ui.ControlHeight
import blue.deniz.vantage.app.ui.DISABLED_ALPHA
import blue.deniz.vantage.app.ui.EventFormatting
import blue.deniz.vantage.app.ui.Icon
import blue.deniz.vantage.app.ui.IconButton
import blue.deniz.vantage.app.ui.IconSize
import blue.deniz.vantage.app.ui.ListRow
import blue.deniz.vantage.app.ui.dividerAboveInListSection
import blue.deniz.vantage.app.ui.ListRowPadding
import blue.deniz.vantage.app.ui.ListSection
import blue.deniz.vantage.app.ui.MarkdownView
import blue.deniz.vantage.app.ui.ScreenHeader
import blue.deniz.vantage.app.ui.SectionTitle
import blue.deniz.vantage.app.ui.SheetTitle
import blue.deniz.vantage.app.ui.Spacing
import blue.deniz.vantage.app.ui.UNTITLED_EVENT
import blue.deniz.vantage.app.ui.MenuItem
import blue.deniz.vantage.app.ui.Text
import blue.deniz.vantage.core.coordinates
import blue.deniz.vantage.core.backgroundSplashMedia
import blue.deniz.vantage.app.ui.label
import blue.deniz.vantage.app.ui.linkText
import blue.deniz.vantage.app.ui.LinkText
import blue.deniz.vantage.app.ui.translatedName
import blue.deniz.vantage.core.EventError
import blue.deniz.vantage.core.EventFormat
import blue.deniz.vantage.core.EventSource
import blue.deniz.vantage.core.isEditableHere
import blue.deniz.vantage.core.SubscribedCalendar
import blue.deniz.vantage.core.EventsLink
import blue.deniz.vantage.core.MapLinks
import blue.deniz.vantage.core.MapPlace
import blue.deniz.vantage.core.Markdown
import blue.deniz.vantage.core.MarkdownBlock
import blue.deniz.vantage.core.ResolvedEvent
import directory.evnt.Component
import directory.evnt.Media
import directory.evnt.Activity
import directory.evnt.Occurrence
import directory.evnt.OpenEvnt
import directory.evnt.PartialDate
import directory.evnt.Venue
import directory.evnt.hasText
import directory.evnt.translate
import kotlin.time.Instant
import kotlinx.coroutines.launch
import kotlinx.datetime.toLocalDateTime
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import blue.deniz.vantage.app.ui.Radius
import kotlin.time.Duration.Companion.days
import kotlinx.coroutines.delay
import kotlinx.datetime.LocalDate
import kotlinx.datetime.format.MonthNames
import kotlinx.datetime.number
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
private val DETAIL_TILE_SIZE = 48.dp

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
private fun OccurrenceRows(state: AppState, eventTitle: String, eventData: OpenEvnt?, occurrences: List<Occurrence>, marks: CalendarMarks) {
    var shownIndex by rememberSaveable { mutableStateOf<Int?>(null) }
    for ((index, occurrence) in occurrences.withIndex()) {
        val periods = remember(occurrence, marks) { occurrence.start?.let { marks.periodsOn(it, occurrence.end) }.orEmpty() }
        OccurrenceCard(occurrence, state.formatting, periods) { shownIndex = index }
    }
    val shown = shownIndex?.let { index -> occurrences.getOrNull(index)?.let { IndexedValue(index, it) } }
    BottomSheet(item = shown, onDismiss = { shownIndex = null }) { (index, occurrence) ->
        val position = if (occurrences.size > 1) "${index + 1} of ${occurrences.size}" else null
        val cancelled = eventData?.isCancelledOn(occurrence) ?: occurrence.isCancelled
        OccurrenceDetails(state, eventTitle, occurrence, position, cancelled, marks, onClose = { shownIndex = null })
    }
}

private fun activityCountLabel(count: Int) = if (count == 1) "1 activity" else "$count activities"

@Composable
private fun OccurrenceCard(occurrence: Occurrence, formatting: EventFormatting, periods: List<CalendarPeriod>, onOpen: () -> Unit) {
    val text = formatting.occurrenceText(occurrence)
    val cancelled = occurrence.isCancelled
    val activityCount = occurrence.activities.orEmpty().size
    val supporting = listOfNotNull(
        text.supporting,
        "Cancelled".takeIf { cancelled },
        activityCount.takeIf { it > 0 }?.let(::activityCountLabel),
        periods.takeIf { it.isNotEmpty() }?.joinToString(" and ", prefix = "During ") { it.label },
    ).joinToString(" · ").ifEmpty { null }
    ListRow(
        text.headline,
        leading = { DateTile(occurrence.start?.let(formatting::localDateOrNull)) },
        supporting = supporting,
        enabled = !cancelled || activityCount > 0,
        onClick = onOpen.takeIf { occurrence.start != null || activityCount > 0 },
        trailing = { if (activityCount > 0) Icon(AppIcons.ChevronRight) },
    )
}

@Composable
private fun DetailTile(content: @Composable () -> Unit) {
    Box(
        Modifier.size(DETAIL_TILE_SIZE).clip(RoundedCornerShape(Radius.sm)).background(MaterialTheme.colorScheme.surfaceContainerHighest),
        contentAlignment = Alignment.Center,
    ) { content() }
}

@Composable
private fun DateTile(date: LocalDate?) {
    DetailTile {
        if (date == null) {
            Icon(AppIcons.Calendar, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            return@DetailTile
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(MonthNames.ENGLISH_ABBREVIATED.names[date.month.number - 1].uppercase(), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
            Text(date.day.toString(), style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun OccurrenceDetails(
    state: AppState,
    eventTitle: String,
    occurrence: Occurrence,
    position: String?,
    cancelled: Boolean,
    marks: CalendarMarks,
    onClose: () -> Unit,
) {
    val formatting = state.formatting
    val preferredLanguages = state.preferredLanguages
    Column(Modifier.fillMaxWidth().padding(horizontal = Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        SheetTitle(eventTitle)
        Text(listOfNotNull("Date", position).joinToString(" "), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
        if (cancelled) Text("Cancelled", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelLarge)
    }
    val start = occurrence.start
    if (start == null) {
        ActivityRows(occurrence.activities.orEmpty(), preferredLanguages)
        return
    }
    val end = occurrence.end
    val firstDay = formatting.localDateOrNull(start)
    val lastDay = end?.let(formatting::localDateOrNull)?.takeIf { firstDay != null && it > firstDay } ?: firstDay
    val endInstant = end?.endInstant ?: start.takeIf { it !is PartialDate.Time }?.endInstant
    val now by produceState(state.clock.now(), state.clock) {
        while (true) {
            delay(1_000 - value.toEpochMilliseconds() % 1_000)
            value = state.clock.now()
        }
    }
    ListSection {
        if (end != null && firstDay != null && lastDay != null && lastDay > firstDay) {
            ListRow(formatting.formatLongDate(start), icon = AppIcons.Calendar, supporting = "Starts")
            ListRow(formatting.formatLongDate(end), icon = AppIcons.Calendar, supporting = "Ends")
            val days = firstDay.daysUntil(lastDay) + 1
            ListRow("$days days", icon = AppIcons.DateRange, supporting = periodProgressText(state.today, firstDay, lastDay))
        } else {
            ListRow(formatting.formatLongDate(start), icon = AppIcons.Calendar, supporting = firstDay?.let { daysAwayText(state.today, it) })
        }
        val time = formatting.occurrenceTimeText(occurrence, showsStatedTime = false)
        if (time != null) {
            val length = (end as? PartialDate.Time)?.let { formatting.formatDuration(it.startInstant - start.startInstant) }
            ListRow(listOfNotNull(time, length).joinToString(" · "), icon = AppIcons.Clock, supporting = if (cancelled) null else formatting.formatTimeFromNow(now, start.startInstant, endInstant))
        }
        (start as? PartialDate.Time)?.let(formatting::statedTimeText)?.let { ListRow(it, icon = AppIcons.World, supporting = "Local Time There") }
    }
    PeriodsSection(state, remember(start, end, marks) { marks.periodsOn(start, end) }, firstDay, beforeOpening = onClose)
    ActivityRows(occurrence.activities.orEmpty(), preferredLanguages)
    Column {
        if (firstDay != null) {
            MenuItem("Show in Calendar", onClick = {
                onClose()
                state.calendarSelection.select(firstDay)
                state.openMyEvents(EventsView.Calendar)
            }, leadingIcon = AppIcons.Calendar)
        }
        if (start is PartialDate.Time) {
            val timestamp = discordTimestamp(start, end as? PartialDate.Time)
            val countdown = "<t:${start.startInstant.epochSeconds}:R>"
            CopyMenuItem("Copy Discord Timestamp", supportingText = timestamp) { timestamp }
            CopyMenuItem("Copy Discord Countdown", supportingText = countdown) { countdown }
        }
        val text = formatting.occurrenceText(occurrence)
        CopyMenuItem("Copy as Text") { listOfNotNull(eventTitle, text.headline, text.supporting).joinToString(" · ") }
    }
}

@Composable
private fun ActivityRows(activities: List<Activity>, preferredLanguages: List<String>) {
    if (activities.isEmpty()) return
    Column(Modifier.padding(horizontal = Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        SectionTitle("Activities", Modifier.padding(top = Spacing.md))
        for (activity in activities) {
            val cancelled = activity.status == Activity.Status.Cancelled
            ListRow(
                activity.name.translate(preferredLanguages),
                supporting = "Cancelled".takeIf { cancelled },
                color = if (cancelled) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

private fun discordTimestamp(start: PartialDate.Time, end: PartialDate.Time?): String {
    val from = "<t:${start.startInstant.epochSeconds}:F>"
    if (end == null) return from
    val endStyle = if (end.startInstant - start.startInstant < 1.days) "t" else "F"
    return "$from – <t:${end.startInstant.epochSeconds}:$endStyle>"
}

@Composable
private fun VenueCard(venue: Venue, preferredLanguages: List<String>) {
    val name = venue.name.takeIf { it.hasText() }?.translate(preferredLanguages)
    var showingDetails by rememberSaveable { mutableStateOf(false) }
    val addressLine = venue.addressLine(preferredLanguages)
    ListRow(
        name ?: addressLine ?: "Unnamed Place",
        leading = { DetailTile { Icon(AppIcons.MapPin, tint = MaterialTheme.colorScheme.onSurfaceVariant) } },
        supporting = addressLine.takeIf { name != null },
        onClick = { showingDetails = true },
    )
    BottomSheet(visible = showingDetails, onDismiss = { showingDetails = false }) { VenueDetails(venue, name, venue.addressLineWithFlag(preferredLanguages)) }
}

@Composable
private fun VenueDetails(venue: Venue, name: String?, addressLine: String?) {
    val uriHandler = LocalUriHandler.current
    val place = remember(venue, name) { MapPlace.of(venue, name) }
    val coordinates = venue.coordinates?.let { "${it.latitude}, ${it.longitude}" }
    Column(Modifier.padding(horizontal = Spacing.md)) {
        SheetTitle(name ?: addressLine ?: "Unnamed Place")
        if (name != null) addressLine?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium) }
        coordinates?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall) }
    }
    Column {
        if (place != null) {
            MenuItem("Open in Maps", onClick = { uriHandler.openMapLink(place) }, leadingIcon = AppIcons.MapPin)
            MenuItem("Directions", onClick = { uriHandler.openUri(MapLinks.directions(place)) }, leadingIcon = AppIcons.Directions)
            if (mapsAppLink(place) != MapLinks.googleMaps(place)) {
                MenuItem("Google Maps", onClick = { uriHandler.openUri(MapLinks.googleMaps(place)) }, leadingIcon = AppIcons.ExternalLink)
            }
            MenuItem("OpenStreetMap", onClick = { uriHandler.openUri(MapLinks.openStreetMap(place)) }, leadingIcon = AppIcons.ExternalLink)
        }
        venue.address?.let { address -> CopyMenuItem("Copy Address", supportingText = address) { address } }
        coordinates?.let { CopyMenuItem("Copy Coordinates", supportingText = it) { it } }
    }
}

private fun UriHandler.openMapLink(place: MapPlace) {
    try {
        openUri(mapsAppLink(place))
    } catch (_: IllegalArgumentException) {
        openUri(MapLinks.googleMaps(place))
    }
}

@Composable
private fun LinkSection(links: List<Component.Link>, preferredLanguages: List<String>, onLongPress: (url: String) -> Unit, atColumnTop: Boolean = false) {
    if (links.isEmpty()) return
    val uriHandler = LocalUriHandler.current
    ListSection(Modifier.padding(top = if (atColumnTop) 0.dp else Spacing.md), title = "Links") {
        for (link in links) {
            LinkButton(
                link.linkText(preferredLanguages),
                onClick = { uriHandler.openUri(link.url) },
                onLongClick = { onLongPress(link.url) },
                enabled = link.disabled != true,
            )
        }
    }
}

@Composable
private fun LinkButton(text: LinkText, onClick: () -> Unit, onLongClick: () -> Unit, enabled: Boolean) {
    Row(
        Modifier
            .fillMaxWidth()
            .dividerAboveInListSection()
            .heightIn(min = ControlHeight.md)
            .alpha(if (enabled) 1f else DISABLED_ALPHA)
            .combinedClickable(enabled = enabled, role = Role.Button, onLongClick = onLongClick, onClick = onClick)
            .padding(horizontal = ListRowPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Icon(AppIcons.ExternalLink, size = IconSize.xs, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text.headline, maxLines = 1, style = MaterialTheme.typography.bodyMedium)
        text.supporting?.let {
            Text(it, Modifier.weight(1f, fill = false), color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun SectionLabel(text: String, atColumnTop: Boolean = false) {
    SectionTitle(text, Modifier.padding(start = Spacing.md, end = Spacing.md, top = if (atColumnTop) 0.dp else Spacing.lg, bottom = Spacing.xs))
}

@Composable
private fun DataSection(event: ResolvedEvent, formatting: EventFormatting, onOpenLinkActions: (url: String) -> Unit, atColumnTop: Boolean = false) {
    val uriHandler = LocalUriHandler.current
    val updatedAt = event.updatedAt?.toLocalDateTime(formatting.timeZone)
    val sources = event.data?.components.orEmpty().filterIsInstance<Component.Source>()
    ListSection(Modifier.padding(top = if (atColumnTop) 0.dp else Spacing.md), title = "Data") {
        ListRow(
            "${sourceLabelOf(event.source)} · ${formatLabelOf(event.format)}",
            icon = event.source.icon,
            supporting = if (updatedAt == null) "Last Updated: Unknown"
            else "Last Updated: ${formatting.formatDay(updatedAt.date)} ${formatting.formatClock(updatedAt.time)}",
        )
        sources.forEachIndexed { index, source ->
            ListRow(
                if (sources.size > 1) "Source Link ${index + 1}" else "Source Link",
                alignWithIcons = true,
                supporting = source.url.substringAfter("://"),
                onClick = { uriHandler.openUri(source.url) },
                onLongClick = { onOpenLinkActions(source.url) },
                trailing = { Icon(AppIcons.ExternalLink) },
            )
        }
    }
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

private fun sourceLabelOf(source: EventSource) = when (source) {
    EventSource.Unknown -> "Unknown Source"
    is EventSource.Local -> if (source.isTestEvent) "Test Event" else "Browser/Device"
    is EventSource.At -> "Atmosphere (AT Protocol)"
    is EventSource.Http -> "Internet (HTTP)"
    is EventSource.Folio -> "Internet (Folio)"
    is EventSource.MediaWiki -> "MediaWiki"
    is EventSource.WebDav -> "WebDAV"
    is EventSource.Calendar -> when (source.calendar) {
        is SubscribedCalendar.OnDevice -> "Calendar on This Phone"
        is SubscribedCalendar.Link -> "Calendar Link"
    }
}

private fun formatLabelOf(format: EventFormat) = when (format) {
    EventFormat.Unknown -> "Unknown Format"
    EventFormat.OpenEvnt -> "Open Evnt"
    EventFormat.ICalendar -> "iCalendar (ICS)"
    EventFormat.CommunityLexicon -> "Community Lexicon"
}
