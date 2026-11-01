package blue.deniz.vantage.app.screens

import androidx.compose.ui.platform.LocalUriHandler
import blue.deniz.vantage.app.ui.growsIntoEventPage
import blue.deniz.vantage.app.ui.LoadingSpinner
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import blue.deniz.vantage.app.collectAcrossSession
import blue.deniz.vantage.app.AppState
import blue.deniz.vantage.app.EventCardStyle
import blue.deniz.vantage.app.HomeTile
import blue.deniz.vantage.app.EventsView
import blue.deniz.vantage.app.Route
import blue.deniz.vantage.app.ui.ListPlaceholder
import blue.deniz.vantage.app.ui.shortLabel
import blue.deniz.vantage.app.ui.AppIcons
import blue.deniz.vantage.app.ui.AgendaEventCard
import blue.deniz.vantage.app.ui.EventCard
import blue.deniz.vantage.app.ui.EventPosterCard
import blue.deniz.vantage.app.ui.EventTicketCard
import blue.deniz.vantage.app.ui.LocalTabBarHeight
import blue.deniz.vantage.app.ui.backdropSource
import blue.deniz.vantage.app.ui.LocalBackdrop
import blue.deniz.vantage.app.ui.ContentUnderOverlay
import blue.deniz.vantage.app.ui.FailureRow
import blue.deniz.vantage.app.ui.Icon
import blue.deniz.vantage.app.ui.IconButton
import blue.deniz.vantage.app.ui.ListRow
import blue.deniz.vantage.app.ui.Radius
import blue.deniz.vantage.app.ui.ScreenHeader
import blue.deniz.vantage.app.ui.SectionTitle
import blue.deniz.vantage.app.ui.Spacing
import blue.deniz.vantage.app.ui.Text
import blue.deniz.vantage.core.AppSchemes
import blue.deniz.vantage.core.TimeFilter
import blue.deniz.vantage.core.coordinates
import blue.deniz.vantage.core.EventFeed
import blue.deniz.vantage.core.EventsLink
import blue.deniz.vantage.core.ResolvedEvent
import blue.deniz.vantage.core.WeatherCondition
import blue.deniz.vantage.core.WeatherForecast
import blue.deniz.vantage.core.upcomingAt
import blue.deniz.vantage.storage.EventListQuery
import io.ktor.http.parseUrl
import kotlin.math.roundToInt
import kotlinx.datetime.format.DayOfWeekNames
import androidx.compose.ui.graphics.Color
import blue.deniz.vantage.app.ui.ContentMaxWidth
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.layout.layout
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.clickable
import androidx.compose.runtime.produceState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import blue.deniz.vantage.app.WeatherPlace
import blue.deniz.vantage.core.Place

private val WEATHER_ICON_SIZE = 40.dp
private const val WEATHER_DAYS_SHOWN = 3
private const val ANDROID_EARLY_ACCESS_URL = "https://${AppSchemes.WEB_HOST}/android"

internal fun HomeTile.title(tagNames: Map<String, String>): String = when (this) {
    is HomeTile.UpcomingEvents -> tagIds.mapNotNull(tagNames::get).sorted().let { names ->
        if (names.isEmpty()) "Upcoming Events" else "Upcoming in ${names.joinToString()}"
    }
    is HomeTile.Weather -> weatherTitle(place)
    is HomeTile.Periods -> "Periods"
    is HomeTile.AndroidApp -> "Vantage for Android"
    is HomeTile.Feed -> "From ${feedName(feed)}"
}

private fun weatherTitle(place: WeatherPlace?) = place?.let { "Weather in ${it.name}" } ?: "Weather"

private fun feedName(feed: EventFeed) = when (feed) {
    is EventFeed.OpenEvntIndex -> parseUrl(feed.url)?.host?.removePrefix("www.") ?: feed.url
    is EventFeed.AtRepository -> "@${feed.repo}"
}

@Composable
fun HomeScreen(state: AppState) {
    val tiles = state.settings.homeTiles
    var weatherPlaceFor by rememberSaveable { mutableStateOf<String?>(null) }

    val tags = collectAcrossSession("tags", null) { state.store.observeTags() }.orEmpty()
    val tagNames = tags.associate { it.id to it.name }

    Box(Modifier.fillMaxSize()) {
        ContentUnderOverlay(
            overlay = {
                ScreenHeader("Home", actions = {
                    IconButton(AppIcons.Rss, "Browse a Feed", onClick = { state.navigator.open(Route.ChooseFeed) })
                    IconButton(AppIcons.Plus, "New", onClick = { state.openNewEntrySheet() })
                })
            },
            modifier = Modifier.fillMaxSize(),
        ) { headerHeight ->
            BoxWithConstraints(Modifier.fillMaxSize()) {
                val spaceBesideColumn = ((maxWidth - ContentMaxWidth) / 2).coerceAtLeast(0.dp)
                LazyColumn(
                    Modifier.fillMaxSize().backdropSource(LocalBackdrop.current),
                    contentPadding = PaddingValues(top = headerHeight, bottom = Spacing.md + LocalTabBarHeight.current),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    items(tiles, key = { it.id }) { tile ->
                        Column(Modifier.widthIn(max = ContentMaxWidth).fillMaxWidth().animateItem().animateContentSize()) {
                            when (tile) {
                                is HomeTile.UpcomingEvents -> UpcomingEventsTile(state, tile, tile.title(tagNames), spaceBesideColumn)
                                is HomeTile.Weather -> WeatherTile(state, tile, onChoosePlace = { weatherPlaceFor = tile.id })
                                is HomeTile.Periods -> PeriodsTile(state, tile.title(tagNames))
                                is HomeTile.AndroidApp -> AndroidAppTile(tile.title(tagNames))
                                is HomeTile.Feed -> FeedTile(state, tile, tile.title(tagNames), spaceBesideColumn)
                            }
                        }
                    }
                    if (tiles.isEmpty()) {
                        item(key = "add") {
                            ListRow(
                                "Add Tile",
                                Modifier.widthIn(max = ContentMaxWidth).fillMaxWidth(),
                                icon = AppIcons.Plus,
                                onClick = { state.navigator.open(Route.HomeSettings) },
                            )
                        }
                    }
                }
            }
        }
    }

    WeatherPlaceSheet(state, weatherPlaceFor, onDismiss = { weatherPlaceFor = null })
}

@Composable
private fun PeriodsTile(state: AppState, title: String) {
    val marks = rememberLoadedCalendarMarks(state)
    val formatting = state.formatting
    val now = state.now
    val today = state.today
    TileHeader(title, "Add" to { state.openMarkSheet(MarkSheetTarget(MarkKind.Period, id = null, day = today)) })
    val current = remember(marks, formatting, now) { marks?.let { formatting.periodSpans(it.periodsAt(now)) } } ?: return
    if (current.isEmpty()) {
        TileMessage("No periods today")
        return
    }
    Column(Modifier.padding(horizontal = Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        for (span in current) {
            PeriodDayRow(span.item.label, span.dayNumberOf(today), span.dayCount) {
                state.openMarkSheet(MarkSheetTarget(MarkKind.Period, span.item.id))
            }
        }
    }
}

@Composable
private fun AndroidAppTile(title: String) {
    val uriHandler = LocalUriHandler.current
    TileHeader(title)
    ListRow(
        "Get Early Access",
        icon = AppIcons.Smartphone,
        supporting = "Install Vantage from Google Play",
        onClick = { uriHandler.openUri(ANDROID_EARLY_ACCESS_URL) },
    )
}

@Composable
private fun TileHeader(title: String, action: Pair<String, () -> Unit>? = null) {
    Row(
        Modifier.fillMaxWidth().padding(start = Spacing.md, end = Spacing.xs, top = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SectionTitle(title, Modifier.weight(1f).padding(vertical = Spacing.sm))
        action?.let { (label, onClick) -> TextButton(onClick = onClick) { Text(label) } }
    }
}

@Composable
private fun TileMessage(text: String) {
    ListPlaceholder(text, Modifier.padding(horizontal = Spacing.md))
}

private fun Modifier.extendPastSides(by: Dp) = layout { measurable, constraints ->
    val extra = by.roundToPx()
    if (extra == 0 || !constraints.hasBoundedWidth) {
        val placeable = measurable.measure(constraints)
        return@layout layout(placeable.width, placeable.height) { placeable.place(0, 0) }
    }
    val placeable = measurable.measure(constraints.copy(minWidth = constraints.maxWidth + 2 * extra, maxWidth = constraints.maxWidth + 2 * extra))
    layout(constraints.maxWidth, placeable.height) { placeable.place(-extra, 0) }
}

@Composable
private fun TileEvents(
    state: AppState,
    events: List<ResolvedEvent>,
    layout: EventCardStyle,
    spaceBesideColumn: Dp,
    open: (ResolvedEvent) -> (() -> Unit)?,
    actions: (ResolvedEvent) -> (() -> Unit)? = { event -> { state.openActionsSheet(event) } },
) {
    val formatting = state.formatting
    val tagsByEvent = collectAcrossSession("tags by event", null) { state.store.observeTagsByEvent() }.orEmpty()
    val unseenDifferences = collectAcrossSession("unseen differences", null) { state.store.observeUnseenDifferences() }.orEmpty()
    if (layout == EventCardStyle.Posters || layout == EventCardStyle.Tickets) {
        val now = state.now
        val row = rememberLazyListState()
        LazyRow(
            Modifier.extendPastSides(spaceBesideColumn),
            state = row,
            contentPadding = PaddingValues(horizontal = Spacing.md + spaceBesideColumn),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            flingBehavior = rememberSnapFlingBehavior(row, SnapPosition.Start),
        ) {
            items(events, key = { it.id ?: it.hashCode() }) { event ->
                val changeLabel = unseenDifferences[event.id]?.shortLabel()
                when (layout) {
                    EventCardStyle.Posters -> EventPosterCard(event, formatting, state.preferredLanguages, now, Modifier.growsIntoEventPage(event.id), onClick = open(event), onLongClick = actions(event), changeLabel = changeLabel)
                    else -> EventTicketCard(event, formatting, state.preferredLanguages, now, Modifier.growsIntoEventPage(event.id), onClick = open(event), onLongClick = actions(event), changeLabel = changeLabel)
                }
            }
        }
        return
    }
    Column(Modifier.padding(horizontal = Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        val now = state.now
        for (event in events) {
            if (layout == EventCardStyle.Agenda) AgendaEventCard(
                event,
                formatting,
                state.preferredLanguages,
                now,
                Modifier.growsIntoEventPage(event.id),
                onClick = open(event),
                onLongClick = actions(event),
                tags = tagsByEvent[event.id].orEmpty(),
                changeLabel = unseenDifferences[event.id]?.shortLabel(),
                accentColor = state.calendars.colorOf(event.source)?.let(::Color),
            )
            else EventCard(
                event,
                formatting,
                state.preferredLanguages,
                Modifier.growsIntoEventPage(event.id),
                onClick = open(event),
                onOpenActions = actions(event),
                tags = tagsByEvent[event.id].orEmpty(),
                changeLabel = unseenDifferences[event.id]?.shortLabel(),
                accentColor = state.calendars.colorOf(event.source)?.let(::Color),
            )
        }
    }
}

@Composable
private fun UpcomingEventsTile(state: AppState, tile: HomeTile.UpcomingEvents, title: String, spaceBesideColumn: Dp) {
    val query = remember(tile.count, tile.tagIds, state.listsSplitAt) {
        EventListQuery(from = state.listsSplitAt, anyTagIds = tile.tagIds, order = EventListQuery.Order.Start, limit = tile.count)
    }
    val upcoming = collectAcrossSession("upcoming ${tile.id}", query) { state.store.observeList(query) }
    TileHeader(title, "See All" to {
        state.myEventsFilters.time = TimeFilter.Future
        state.myEventsFilters.tagIds = tile.tagIds
        state.openMyEvents(EventsView.List)
    })
    val events = upcoming ?: return
    if (events.isEmpty()) {
        TileMessage("Nothing coming up")
        return
    }
    TileEvents(
        state,
        events,
        tile.layout,
        spaceBesideColumn,
        open = { event -> event.id?.let { id -> { state.openEvent(id, fromCard = true) } } },
    )
}

@Composable
private fun FeedTile(state: AppState, tile: HomeTile.Feed, title: String, spaceBesideColumn: Dp) {
    var attempt by remember { mutableIntStateOf(0) }
    val loaded by produceLoadState(tile.feed, attempt, last = { state.feeds.lastReadEvents(tile.feed) }) {
        state.feeds.readEvents(tile.feed)
    }
    TileHeader(title, "See All" to { state.navigator.open(Route.Feed(tile.feed)) })
    TileLoadState(loaded, onRetry = { attempt++ }) { listing ->
        val events = listing.upcomingAt(state.listsSplitAt).take(tile.count)
        if (events.isEmpty()) {
            TileMessage("Nothing coming up")
        } else {
            TileEvents(state, events, tile.layout, spaceBesideColumn, open = { event ->
                EventsLink.intentFor(event.source)?.let { intent -> { state.navigator.open(Route.EventFromLink(intent)) } }
            })
        }
    }
}

@Composable
private fun <T> TileLoadState(loaded: LoadState<T>, onRetry: () -> Unit, ready: @Composable (T) -> Unit) {
    AnimatedContent(
        loaded,
        contentKey = { it::class },
        transitionSpec = { fadeIn(tween(220, delayMillis = 90)) togetherWith fadeOut(tween(90)) using null },
    ) { shown ->
        Column {
            when (shown) {
                LoadState.Loading -> LoadingSpinner()
                is LoadState.Failed -> FailureRow(shown.message, onRetry, Modifier.padding(horizontal = Spacing.md))
                is LoadState.Ready -> ready(shown.value)
            }
        }
    }
}

private fun conditionLabel(condition: WeatherCondition) = when (condition) {
    WeatherCondition.Clear -> "Clear"
    WeatherCondition.PartlyCloudy -> "Partly Cloudy"
    WeatherCondition.Cloudy -> "Cloudy"
    WeatherCondition.Fog -> "Fog"
    WeatherCondition.Drizzle -> "Drizzle"
    WeatherCondition.Rain -> "Rain"
    WeatherCondition.Snow -> "Snow"
    WeatherCondition.Thunderstorm -> "Thunderstorm"
    WeatherCondition.Unknown -> "Unknown"
}

private fun conditionIcon(condition: WeatherCondition, isDay: Boolean = true) = when (condition) {
    WeatherCondition.Clear -> if (isDay) AppIcons.Sunny else AppIcons.ClearNight
    WeatherCondition.PartlyCloudy -> if (isDay) AppIcons.PartlyCloudy else AppIcons.PartlyCloudyNight
    WeatherCondition.Cloudy -> AppIcons.Cloudy
    WeatherCondition.Fog -> AppIcons.Fog
    WeatherCondition.Drizzle -> AppIcons.Drizzle
    WeatherCondition.Rain -> AppIcons.Rain
    WeatherCondition.Snow -> AppIcons.Snow
    WeatherCondition.Thunderstorm -> AppIcons.Thunderstorm
    WeatherCondition.Unknown -> AppIcons.Thermometer
}

private fun degrees(value: Double) = "${value.roundToInt()}°"

@Composable
private fun WeatherTile(state: AppState, tile: HomeTile.Weather, onChoosePlace: () -> Unit) {
    val zoneId = state.settings.timeZone.id
    val guessed by produceState<LoadState<WeatherPlace?>>(
        state.places.lastCityOfTimeZone(zoneId)?.toWeatherPlace()?.let { LoadState.Ready(it) } ?: LoadState.Loading,
        zoneId,
        tile.place,
    ) {
        if (tile.place == null) value = loadCatching { state.places.cityOfTimeZone(zoneId)?.toWeatherPlace() }
    }
    val place = tile.place ?: guessed.let { if (it is LoadState.Ready) it.value else null }
    if (place == null) {
        TileHeader(weatherTitle(null))
        if (guessed is LoadState.Loading) LoadingSpinner() else WeatherPlaceholder(onChoosePlace)
        return
    }
    var attempt by remember { mutableIntStateOf(0) }
    val loaded by produceLoadState(place.latitude, place.longitude, attempt, last = { state.weather.lastForecastFor(place.latitude, place.longitude) }) {
        state.weather.forecastFor(place.latitude, place.longitude)
    }
    TileHeader(weatherTitle(place))
    TileLoadState(loaded, onRetry = { attempt++ }) { WeatherSummary(it) }
}

private fun Place.toWeatherPlace() = coordinates?.let { WeatherPlace(name, it.latitude, it.longitude) }

@Composable
private fun WeatherPlaceholder(onChoosePlace: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.md)
            .clip(RoundedCornerShape(Radius.md))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable(onClick = onChoosePlace)
            .padding(Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Icon(AppIcons.Thermometer, size = WEATHER_ICON_SIZE, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("Choose a Place", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun WeatherSummary(forecast: WeatherForecast) {
    val today = forecast.days.firstOrNull()
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.md)
            .clip(RoundedCornerShape(Radius.md))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Icon(conditionIcon(forecast.current.condition, forecast.current.isDay), size = WEATHER_ICON_SIZE, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Column(Modifier.weight(1f)) {
            Text(degrees(forecast.current.temperature), style = MaterialTheme.typography.headlineMedium)
            Text(
                listOfNotNull(
                    conditionLabel(forecast.current.condition),
                    today?.let { "${degrees(it.low)} to ${degrees(it.high)}" },
                ).joinToString(" · "),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        for (day in forecast.days.drop(1).take(WEATHER_DAYS_SHOWN)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(DayOfWeekNames.ENGLISH_ABBREVIATED.names[day.date.dayOfWeek.ordinal], style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.padding(top = Spacing.xs))
                Icon(conditionIcon(day.condition), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(degrees(day.high), style = MaterialTheme.typography.labelMedium)
                Text(degrees(day.low), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}
