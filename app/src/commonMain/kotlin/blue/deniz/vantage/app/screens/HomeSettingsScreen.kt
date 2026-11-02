package blue.deniz.vantage.app.screens

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import blue.deniz.vantage.app.AppState
import blue.deniz.vantage.app.DEFAULT_TILE_EVENT_COUNT
import blue.deniz.vantage.app.EventCardStyle
import blue.deniz.vantage.app.HomeTile
import blue.deniz.vantage.app.WeatherPlace
import blue.deniz.vantage.app.collectAcrossSession
import blue.deniz.vantage.app.newHomeTileId
import blue.deniz.vantage.app.offersAndroidAppTile
import blue.deniz.vantage.app.ui.AppIcons
import blue.deniz.vantage.app.ui.BottomSheet
import blue.deniz.vantage.app.ui.Button
import blue.deniz.vantage.app.ui.IconButton
import blue.deniz.vantage.app.ui.ListRow
import blue.deniz.vantage.app.ui.ListSection
import blue.deniz.vantage.app.ui.MenuItem
import blue.deniz.vantage.app.ui.RemoveButton
import blue.deniz.vantage.app.ui.SegmentedControl
import blue.deniz.vantage.app.ui.Spacing
import blue.deniz.vantage.app.ui.TagFilterChips
import blue.deniz.vantage.app.ui.Text
import blue.deniz.vantage.app.ui.SheetTitle
import blue.deniz.vantage.app.ui.TextInput
import blue.deniz.vantage.app.ui.TitledSection
import blue.deniz.vantage.app.ui.displayName
import blue.deniz.vantage.core.EventFeed
import blue.deniz.vantage.core.address
import blue.deniz.vantage.core.coordinates
import blue.deniz.vantage.storage.TagEntity
import sh.calvin.reorderable.ReorderableColumn
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.ReorderableListItemScope

private val DRAGGED_TILE_ELEVATION = 8.dp
private const val NEW_TILE = "new"
private val EVENT_COUNT_OPTIONS = listOf(3, 5, 10).map { it to it.toString() }
private val EVENT_CARD_STYLE_OPTIONS = EventCardStyle.entries.map { it to it.label }

private fun HomeTile.icon(): ImageVector = when (this) {
    is HomeTile.UpcomingEvents -> AppIcons.Calendar
    is HomeTile.Weather -> AppIcons.Sunny
    is HomeTile.Periods -> AppIcons.DateRange
    is HomeTile.Feed -> AppIcons.Rss
    is HomeTile.AndroidApp -> AppIcons.Smartphone
}

private fun HomeTile.summary(): String? = when (this) {
    is HomeTile.UpcomingEvents -> "Next ${eventCountLabel(count)}"
    is HomeTile.Weather -> if (place == null) "From Time Zone" else null
    is HomeTile.Periods -> null
    is HomeTile.Feed -> "Next ${eventCountLabel(count)}"
    is HomeTile.AndroidApp -> null
}

private fun List<HomeTile>.replacing(tile: HomeTile) =
    if (any { it.id == tile.id }) map { if (it.id == tile.id) tile else it } else this + tile

private fun List<HomeTile>.restoring(tile: HomeTile, index: Int) =
    if (any { it.id == tile.id }) this else toMutableList().apply { add(index.coerceIn(0, size), tile) }

private fun List<HomeTile>.moving(from: Int, to: Int) =
    toMutableList().apply { add(to, removeAt(from)) }

private fun AppState.saveHomeTile(tile: HomeTile) = settings.changeHomeTiles(settings.homeTiles.replacing(tile))

@Composable
fun HomeSettingsScreen(state: AppState) {
    val tiles = state.settings.homeTiles
    var choosingTile by rememberSaveable { mutableStateOf(false) }
    var weatherPlaceFor by rememberSaveable { mutableStateOf<String?>(null) }
    var settingsFor by rememberSaveable { mutableStateOf<String?>(null) }
    var newFeedTile by rememberSaveable { mutableStateOf(false) }

    val tags = collectAcrossSession("tags", null) { state.store.observeTags() }.orEmpty()
    val tagNames = tags.associate { it.id to it.name }

    fun removeTile(tile: HomeTile) {
        val index = state.settings.homeTiles.indexOf(tile)
        state.settings.changeHomeTiles(state.settings.homeTiles - tile)
        state.offerUndo("Tile Removed") { state.settings.changeHomeTiles(state.settings.homeTiles.restoring(tile, index)) }
    }

    val haptics = LocalHapticFeedback.current

    SettingsPage("Home", onBack = { state.navigator.goBack() }) {
        ListSection {
            ReorderableColumn(
                tiles,
                onSettle = { from, to -> state.settings.changeHomeTiles(state.settings.homeTiles.moving(from, to)) },
                modifier = Modifier.zIndex(1f),
                onMove = { haptics.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick) },
            ) { _, tile, dragging ->
                key(tile.id) {
                    ReorderableItem {
                        EditedTileRow(
                            tile,
                            tile.title(tagNames),
                            dragging = dragging,
                            reorderable = tiles.size > 1,
                            onDragStarted = { haptics.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate) },
                            onDragStopped = { haptics.performHapticFeedback(HapticFeedbackType.GestureEnd) },
                            onRemove = { removeTile(tile) },
                            onEdit = when (tile) {
                                is HomeTile.Weather -> ({ weatherPlaceFor = tile.id })
                                is HomeTile.Periods, is HomeTile.AndroidApp -> null
                                else -> ({ settingsFor = tile.id })
                            },
                        )
                    }
                }
            }
            ListRow("Add Tile", icon = AppIcons.Plus, onClick = { choosingTile = true })
        }
    }

    WeatherPlaceSheet(state, weatherPlaceFor, onDismiss = { weatherPlaceFor = null })

    BottomSheet(visible = choosingTile, onDismiss = { choosingTile = false }) {
        SheetTitle("Add Tile", Modifier.padding(horizontal = Spacing.md))
        Column {
            MenuItem("Upcoming Events", onClick = {
                choosingTile = false
                state.saveHomeTile(HomeTile.UpcomingEvents(newHomeTileId()))
            }, leadingIcon = AppIcons.Calendar)
            MenuItem("Weather", onClick = {
                choosingTile = false
                state.saveHomeTile(HomeTile.Weather(newHomeTileId()))
            }, leadingIcon = AppIcons.Sunny)
            if (tiles.none { it is HomeTile.Periods }) {
                MenuItem("Periods", onClick = {
                    choosingTile = false
                    state.saveHomeTile(HomeTile.Periods(newHomeTileId()))
                }, leadingIcon = AppIcons.DateRange)
            }
            MenuItem("Feed", onClick = {
                choosingTile = false
                newFeedTile = true
            }, leadingIcon = AppIcons.Rss)
            if (offersAndroidAppTile && tiles.none { it is HomeTile.AndroidApp }) {
                MenuItem("Vantage for Android", onClick = {
                    choosingTile = false
                    state.saveHomeTile(HomeTile.AndroidApp(newHomeTileId()))
                }, leadingIcon = AppIcons.Smartphone)
            }
        }
    }

    val editedTile = tiles.firstOrNull { it.id == settingsFor }
    val closeSettings = {
        newFeedTile = false
        settingsFor = null
    }
    BottomSheet(item = editedTile ?: HomeTile.Feed(NEW_TILE, EventFeed.OpenEvntIndex("")).takeIf { newFeedTile }, onDismiss = closeSettings) { tile ->
        key(tile.id) {
            TileSettings(
                tile,
                title = if (tile.id == NEW_TILE) "Add Feed" else tile.title(tagNames),
                tags = tags,
                onSave = { saved ->
                    closeSettings()
                    state.saveHomeTile(if (saved is HomeTile.Feed && saved.id == NEW_TILE) saved.copy(id = newHomeTileId()) else saved)
                },
            )
        }
    }
}

@Composable
internal fun WeatherPlaceSheet(state: AppState, tileId: String?, onDismiss: () -> Unit) {
    val tile = state.settings.homeTiles.firstOrNull { it.id == tileId } as? HomeTile.Weather
    PlaceSearchSheet(
        visible = tile != null,
        title = "Weather Place",
        state = state,
        onDismiss = onDismiss,
        onPick = { venue ->
            onDismiss()
            val coordinates = venue.coordinates
            if (tile != null && coordinates != null) {
                val name = venue.displayName(state.preferredLanguages) ?: venue.address ?: "Unnamed Place"
                state.saveHomeTile(tile.copy(place = WeatherPlace(name, coordinates.latitude, coordinates.longitude)))
            }
        },
        onlyMapped = true,
        unsetLabel = "Use Time Zone".takeIf { tile?.place != null },
        onUnset = {
            onDismiss()
            if (tile != null) state.saveHomeTile(tile.copy(place = null))
        },
    )
}

@Composable
private fun ReorderableListItemScope.EditedTileRow(
    tile: HomeTile,
    title: String,
    dragging: Boolean,
    reorderable: Boolean,
    onDragStarted: () -> Unit,
    onDragStopped: () -> Unit,
    onRemove: () -> Unit,
    onEdit: (() -> Unit)?,
) {
    val elevation by animateDpAsState(if (dragging) DRAGGED_TILE_ELEVATION else 0.dp)
    ListRow(
        title,
        modifier = Modifier.longPressDraggableHandle(onDragStarted = { onDragStarted() }, onDragStopped = { onDragStopped() }),
        icon = tile.icon(),
        supporting = tile.summary(),
        onClick = onEdit,
        elevation = elevation,
        trailing = {
            Row {
                RemoveButton("Remove Tile", onRemove)
                if (reorderable) {
                    IconButton(
                        AppIcons.DragHandle,
                        "Reorder Tile",
                        onClick = {},
                        modifier = Modifier.draggableHandle(onDragStarted = { onDragStarted() }, onDragStopped = { onDragStopped() }),
                    )
                }
            }
        },
    )
}

@Composable
private fun ColumnScope.TileSettings(tile: HomeTile, title: String, tags: List<TagEntity>, onSave: (HomeTile) -> Unit) {
    var count by rememberSaveable {
        mutableIntStateOf(
            when (tile) {
                is HomeTile.UpcomingEvents -> tile.count
                is HomeTile.Feed -> tile.count
                is HomeTile.Weather, is HomeTile.Periods, is HomeTile.AndroidApp -> DEFAULT_TILE_EVENT_COUNT
            },
        )
    }
    var tagIds by rememberSaveable { mutableStateOf((tile as? HomeTile.UpcomingEvents)?.tagIds.orEmpty().toList()) }
    var layout by rememberSaveable {
        mutableStateOf(
            when (tile) {
                is HomeTile.UpcomingEvents -> tile.layout
                is HomeTile.Feed -> tile.layout
                is HomeTile.Weather, is HomeTile.Periods, is HomeTile.AndroidApp -> EventCardStyle.Rows
            },
        )
    }
    var address by rememberSaveable { mutableStateOf((tile as? HomeTile.Feed)?.feed?.address.orEmpty()) }
    var showsAddressProblem by rememberSaveable { mutableStateOf(false) }
    val feed = EventFeed.fromAddress(address)
    val focus = remember { FocusRequester() }
    fun editedTile(): HomeTile? = when (tile) {
        is HomeTile.UpcomingEvents -> tile.copy(count = count, tagIds = tagIds.toSet(), layout = layout)
        is HomeTile.Feed -> EventFeed.fromAddress(address)?.let { tile.copy(feed = it, count = count, layout = layout) }
        is HomeTile.Weather, is HomeTile.Periods, is HomeTile.AndroidApp -> tile
    }
    fun save() {
        val edited = editedTile()
        if (edited == null) {
            showsAddressProblem = true
            return
        }
        onSave(edited)
    }
    SheetTitle(title, Modifier.padding(horizontal = Spacing.md))
    if (tile is HomeTile.Feed) {
        TextInput(
            value = address,
            onValueChange = { address = it },
            modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.md).focusRequester(focus),
            label = "Feed Address",
            placeholder = "https://… or alice.bsky.social",
            problem = FEED_ADDRESS_PROBLEM.takeIf { address.isNotBlank() && feed == null },
            showsProblem = showsAddressProblem,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { save() }),
        )
    }
    if (tile is HomeTile.UpcomingEvents && tags.isNotEmpty()) {
        TitledSection("Tags") {
            TagFilterChips(tags, tagIds.toSet(), onToggle = { tag -> tagIds = if (tag.id in tagIds) tagIds - tag.id else tagIds + tag.id })
        }
    }
    TitledSection("Events Shown") {
        SegmentedControl(EVENT_COUNT_OPTIONS, selected = count, onSelect = { count = it })
    }
    TitledSection("Layout") {
        SegmentedControl(EVENT_CARD_STYLE_OPTIONS, selected = layout, onSelect = { layout = it })
    }
    if (editedTile() != null) {
        Button(
            if (tile.id == NEW_TILE) "Add" else "Save",
            onClick = ::save,
            Modifier.align(Alignment.End).padding(horizontal = Spacing.md),
            primary = true,
        )
    }
    LaunchedEffect(Unit) { if (tile.id == NEW_TILE) focus.requestFocus() }
}
