package blue.deniz.vantage.app.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import blue.deniz.vantage.app.ui.UNTITLED_EVENT

import blue.deniz.vantage.storage.HiddenCalendarEvent

import blue.deniz.vantage.app.ui.LoadingSpinner
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import blue.deniz.vantage.app.collectAcrossSession
import blue.deniz.vantage.app.rememberCalendarPermissionRequest
import blue.deniz.vantage.app.rememberNotificationPermissionRequest
import blue.deniz.vantage.app.ui.CheckBoxIcon
import blue.deniz.vantage.app.ui.Button
import blue.deniz.vantage.app.ui.IconButton
import blue.deniz.vantage.app.ui.SectionTitle
import blue.deniz.vantage.app.ui.SheetTitle
import blue.deniz.vantage.app.ui.TextInput
import blue.deniz.vantage.core.DeviceCalendar
import blue.deniz.vantage.core.SubscribedCalendar
import blue.deniz.vantage.core.timeZoneOffsetLabel
import kotlinx.datetime.toLocalDateTime
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import kotlinx.serialization.SerializationException
import kotlinx.coroutines.launch
import blue.deniz.vantage.storage.counts
import blue.deniz.vantage.storage.BackupCounts
import blue.deniz.vantage.storage.Backup
import blue.deniz.vantage.app.rememberSaveTextFile
import blue.deniz.vantage.app.rememberOpenTextFile
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import blue.deniz.vantage.app.AppState
import blue.deniz.vantage.app.CalendarSync
import blue.deniz.vantage.app.DEFAULT_CONTENT_LANGUAGE
import blue.deniz.vantage.app.Route
import blue.deniz.vantage.app.countryName
import blue.deniz.vantage.app.contentLanguage
import blue.deniz.vantage.core.Countries
import blue.deniz.vantage.app.ui.AppIcons
import blue.deniz.vantage.app.ThemeAccent
import blue.deniz.vantage.app.ui.BottomSheet
import blue.deniz.vantage.app.ui.accentColor
import blue.deniz.vantage.app.ui.isWallpaperAccentAvailable
import blue.deniz.vantage.app.ui.BottomSheetFrame
import blue.deniz.vantage.app.ui.Icon
import blue.deniz.vantage.app.ui.ListRow
import blue.deniz.vantage.app.ui.ListSection
import blue.deniz.vantage.app.ui.LocalTabBarHeight
import blue.deniz.vantage.app.ui.backdropSource
import blue.deniz.vantage.app.ui.LocalBackdrop
import blue.deniz.vantage.app.ui.ContentUnderOverlay
import blue.deniz.vantage.app.ui.MenuItem
import blue.deniz.vantage.app.ui.ScreenHeader
import blue.deniz.vantage.app.ui.Spacing
import blue.deniz.vantage.app.ui.Text
import kotlinx.datetime.TimeZone
import blue.deniz.vantage.app.ui.ContentMaxWidth
import kotlinx.coroutines.flow.flow

private enum class SettingsSheet { Language, TimeZone, HomeCountry, Accent }

private data class AppLink(val label: String, val url: String)

private val APP_LINKS = listOf(
    AppLink("Bug Reports / Feedback", "https://github.com/deniz-blue/vantage/issues"),
    AppLink("Beta Testers Group", "https://groups.google.com/g/tsxlt"),
    AppLink("App on Google Play Store", "https://play.google.com/store/apps/details?id=lt.tsx.vantage"),
)

@Composable
fun SettingsScreen(state: AppState) {
    val settings = state.settings
    val now = state.now
    val tagCount = collectAcrossSession("tags", null) { state.store.observeTags() }
    var openSheet by rememberSaveable { mutableStateOf<SettingsSheet?>(null) }

    SettingsPage("Settings") {
        ListSection {
            val language = settings.contentLanguage
            ListRow(
                "Content Language",
                icon = AppIcons.Language,
                supporting = contentLanguage(language).autonymWithEnglishName,
                onClick = { openSheet = SettingsSheet.Language },
            )
            if (language != DEFAULT_CONTENT_LANGUAGE) {
                ListRow("Use English", icon = AppIcons.ArrowUp, onClick = { settings.changeContentLanguage(DEFAULT_CONTENT_LANGUAGE) })
            }

            val zone = settings.timeZone
            val detected = settings.detectedTimeZone
            ListRow(
                "Time Zone",
                icon = AppIcons.World,
                supporting = "${zone.id.replace('_', ' ')} · ${timeZoneOffsetLabel(zone, now)}",
                onClick = { openSheet = SettingsSheet.TimeZone },
            )
            if (settings.chosenTimeZone != null) {
                ListRow(
                    "Use Detected: ${detected.id} (${timeZoneOffsetLabel(detected, now)})",
                    icon = AppIcons.ArrowUp,
                    onClick = { settings.changeTimeZone(detected) },
                )
            }

            val homeCountry = settings.homeCountry
            ListRow(
                "Home Country",
                icon = AppIcons.Flag,
                supporting = homeCountry?.let { "${Countries.flagOf(it)}  ${countryName(it, language)}" } ?: "Not Set",
                onClick = { openSheet = SettingsSheet.HomeCountry },
            )
            if (homeCountry != null) {
                ListRow("Show Every Flag", icon = AppIcons.X, onClick = { settings.changeHomeCountry(null) })
            }

            ListRow(
                "Accent",
                leading = { ColorDot(MaterialTheme.colorScheme.primary) },
                supporting = settings.themeAccent.name,
                onClick = { openSheet = SettingsSheet.Accent },
            )

            if (state.backgroundChecks != null) ChangeNotificationsRow(state)
        }
        ListSection(Modifier.padding(top = Spacing.md)) {
            SubpageRow("Home", AppIcons.Home, supporting = tileCountLabel(settings.homeTiles.size)) { state.navigator.open(Route.HomeSettings) }
            SubpageRow("Tags", AppIcons.Tag, supporting = tagCount?.size?.let(::tagCountLabel)) { state.navigator.open(Route.TagSettings) }
            SubpageRow("System Calendars", AppIcons.Calendar, supporting = calendarCountLabel(settings.subscribedCalendars.size)) {
                state.navigator.open(Route.CalendarSettings)
            }
        }
        ListSection(Modifier.padding(top = Spacing.md)) {
            BackupRows(state)
        }
        ListSection(Modifier.padding(top = Spacing.md)) {
            SubpageRow("About", AppIcons.Info) { state.navigator.open(Route.About) }
            SubpageRow("Developer Tools", AppIcons.Code) { state.navigator.open(Route.DeveloperTools) }
        }
    }

    BottomSheetFrame(visible = openSheet == SettingsSheet.Language, onDismiss = { openSheet = null }) {
        LanguagePickerBody("Content Language", settings.contentLanguage) { picked ->
            openSheet = null
            settings.changeContentLanguage(picked)
        }
    }
    BottomSheetFrame(visible = openSheet == SettingsSheet.HomeCountry, onDismiss = { openSheet = null }) {
        CountryPickerBody("Home Country", settings.homeCountry, settings.contentLanguage) { picked ->
            openSheet = null
            settings.changeHomeCountry(picked)
        }
    }
    BottomSheet(visible = openSheet == SettingsSheet.Accent, onDismiss = { openSheet = null }) {
        AccentPicker(settings.themeAccent, onPick = settings::changeThemeAccent)
    }
    BottomSheetFrame(visible = openSheet == SettingsSheet.TimeZone, onDismiss = { openSheet = null }) {
        TimeZonePickerBody("Time Zone", settings.timeZone.id, now) { picked ->
            openSheet = null
            settings.changeTimeZone(TimeZone.of(picked))
        }
    }
}

private fun backupCountsLabel(counts: BackupCounts) = listOf(
    eventCountLabel(counts.events),
    tagCountLabel(counts.tags),
    if (counts.markers == 1) "1 Marker" else "${counts.markers} Markers",
    if (counts.periods == 1) "1 Period" else "${counts.periods} Periods",
).joinToString(" · ")

@Composable
private fun BackupRows(state: AppState) {
    val scope = rememberCoroutineScope()
    var exported by remember { mutableStateOf<BackupCounts?>(null) }
    var exportStatus by remember { mutableStateOf<String?>(null) }
    var importStatus by remember { mutableStateOf<String?>(null) }
    val saveFile = rememberSaveTextFile { saved ->
        exportStatus = if (saved) exported?.let { "Saved ${backupCountsLabel(it)}" } else "Couldn't write the file"
    }
    val openFile = rememberOpenTextFile { text ->
        scope.launch {
            importStatus = "Importing…"
            importStatus = try {
                val backup = Backup.fromJson(text)
                state.importBackup(backup)
                "Imported ${backupCountsLabel(backup.counts)}"
            } catch (_: SerializationException) {
                "Not a Vantage backup"
            } catch (_: IllegalArgumentException) {
                "Not a Vantage backup"
            }
        }
    }
    ListRow(
        "Export Data",
        icon = AppIcons.FileUpload,
        supporting = exportStatus ?: "Events, tags, markers, periods and settings in one file",
        onClick = {
            scope.launch {
                val backup = state.exportBackup()
                exported = backup.counts
                saveFile("vantage-${state.today}.json", backup.toJson())
            }
        },
    )
    ListRow(
        "Import Data",
        icon = AppIcons.FileDownload,
        supporting = importStatus ?: "Adds what's in a backup; items it shares with this one are replaced",
        onClick = openFile,
    )
}

@Composable
private fun ChangeNotificationsRow(state: AppState) {
    val on = state.settings.notifiesAboutChanges
    var refused by remember { mutableStateOf(false) }
    val requestPermission = rememberNotificationPermissionRequest { granted ->
        refused = !granted
        if (granted) state.changeNotifiesAboutChanges(true)
    }
    ListRow(
        "Notify About Changes",
        icon = AppIcons.Bell,
        supporting = if (refused) "Android has notifications turned off for Vantage" else "Checks more often as events get closer",
        color = if (refused) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
        trailing = { CheckBoxIcon(on) },
        onClick = { if (on) state.changeNotifiesAboutChanges(false) else requestPermission() },
    )
}

@Composable
private fun PhoneCalendarCopyRow(state: AppState, onPermitted: () -> Unit) {
    val on = state.settings.copiesEventsToPhoneCalendar
    var refused by remember { mutableStateOf(false) }
    val requestPermission = rememberCalendarPermissionRequest(writes = true) { granted ->
        refused = !granted
        if (granted) {
            onPermitted()
            state.changeCopiesEventsToPhoneCalendar(true)
        }
    }
    val failure = if (refused) "Android has calendar access turned off for Vantage" else state.phoneCalendarCopyFailure
    ListRow(
        "Copy Events to Phone Calendar",
        icon = AppIcons.Copy,
        supporting = failure ?: "Shows your events in other calendar apps",
        color = if (failure != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
        trailing = { CheckBoxIcon(on) },
        onClick = { if (on) state.changeCopiesEventsToPhoneCalendar(false) else requestPermission() },
    )
}

private fun tileCountLabel(count: Int) = when (count) {
    0 -> "Empty"
    1 -> "1 Tile"
    else -> "$count Tiles"
}

private fun calendarCountLabel(count: Int) = when (count) {
    0 -> "None"
    1 -> "1 Calendar"
    else -> "$count Calendars"
}

@Composable
fun CalendarSettingsScreen(state: AppState) {
    val sync = state.calendars
    val subscribed = state.settings.subscribedCalendars
    val device = sync.deviceCalendars
    var permitted by remember { mutableStateOf(device?.hasPermission() == true) }
    val requestPermission = rememberCalendarPermissionRequest { granted -> permitted = granted }
    val deviceCalendars = collectAcrossSession("phone calendars", permitted) { flow { emit(if (permitted) device?.list() else null) } }
    var linkAddress by rememberSaveable { mutableStateOf("") }
    var showsLinkProblem by rememberSaveable { mutableStateOf(false) }
    fun newLinkFrom(address: String) = SubscribedCalendar.linkFromAddress(address)?.takeIf { it !in subscribed }
    val link = newLinkFrom(linkAddress)
    val linkProblem = when {
        linkAddress.isBlank() -> null
        SubscribedCalendar.linkFromAddress(linkAddress) == null -> "Calendar links start with https:// or webcal://"
        link == null -> "Already in the list"
        else -> null
    }
    fun addLink() {
        val added = newLinkFrom(linkAddress)
        if (added == null) {
            showsLinkProblem = true
            return
        }
        sync.subscribe(added)
        linkAddress = ""
        showsLinkProblem = false
    }
    var choosingPhoneCalendars by rememberSaveable { mutableStateOf(false) }
    var choosingHiddenEvents by rememberSaveable { mutableStateOf(false) }
    val hidden = collectAcrossSession("hidden calendar events", state.store) { state.store.observeHiddenCalendarEvents() }.orEmpty()

    SettingsPage("System Calendars", onBack = { state.navigator.goBack() }) {
        if (device != null || state.phoneCalendarCopy != null) {
            ListSection(title = "On This Phone") {
                if (device != null) {
                    val listed = deviceCalendars
                    when {
                        !permitted -> ListRow(
                            "Allow Calendar Access",
                            icon = AppIcons.Calendar,
                            onClick = requestPermission,
                        )
                        listed == null -> LoadingSpinner()
                        listed.isEmpty() -> ListRow("No Calendars on This Phone", icon = AppIcons.CalendarOff)
                        else -> {
                            val failure = listed.firstNotNullOfOrNull { sync.failures[SubscribedCalendar.OnDevice(it.id)] }
                            val shownCount = listed.count { SubscribedCalendar.OnDevice(it.id) in subscribed }
                            ListRow(
                                "Phone Calendars",
                                icon = AppIcons.Calendar,
                                supporting = failure ?: "$shownCount of ${listed.size} shown",
                                color = if (failure != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                                onClick = { choosingPhoneCalendars = true },
                            )
                        }
                    }
                }
                if (state.phoneCalendarCopy != null) PhoneCalendarCopyRow(state, onPermitted = { permitted = true })
            }
        }

        val links = subscribed.filterIsInstance<SubscribedCalendar.ICalendarLink>()
        if (links.isEmpty()) {
            SectionTitle("Calendar Links", Modifier.padding(start = Spacing.md, end = Spacing.md, top = Spacing.lg, bottom = Spacing.xs))
        } else ListSection(Modifier.padding(top = Spacing.md), title = "Calendar Links") {
            for (subscription in links) {
                val name = sync.linkNames[subscription]
                ListRow(
                    name ?: subscription.url,
                    icon = AppIcons.Link,
                    supporting = sync.failures[subscription] ?: subscription.url.takeIf { name != null },
                    color = if (subscription in sync.failures) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                    trailing = {
                        IconButton(AppIcons.Trash, "Remove", onClick = {
                            sync.unsubscribe(subscription) { removed -> state.offerUndo("Calendar Link Removed") { sync.restore(removed) } }
                        })
                    },
                )
            }
        }
        Column(
            Modifier.padding(horizontal = Spacing.md).padding(top = if (links.isEmpty()) 0.dp else Spacing.sm),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            TextInput(
                value = linkAddress,
                onValueChange = { linkAddress = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = "https://….ics or webcal://…",
                supportingText = "Google Calendar's \"Secret address in iCal format\" works here",
                problem = linkProblem,
                showsProblem = showsLinkProblem,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { addLink() }),
            )
            link?.let { Button("Add Calendar", onClick = ::addLink, primary = true, modifier = Modifier.fillMaxWidth()) }
        }

        if (subscribed.isNotEmpty()) {
            ListSection(Modifier.padding(top = Spacing.md)) {
                MenuItem(
                    "Sync Now",
                    onClick = sync::syncAll,
                    leadingIcon = AppIcons.Refresh,
                    supportingText = sync.lastSyncedAt?.let { synced -> "Last synced at ${state.formatting.formatClock(synced.toLocalDateTime(state.timeZone).time)}" },
                    busy = sync.syncing,
                )
                AnimatedVisibility(hidden.isNotEmpty(), enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                    ListRow(
                        "Hidden Events",
                        icon = AppIcons.Hidden,
                        supporting = eventCountLabel(hidden.size),
                        onClick = { choosingHiddenEvents = true },
                    )
                }
            }
        }
    }

    BottomSheet(visible = choosingHiddenEvents && hidden.isNotEmpty(), onDismiss = { choosingHiddenEvents = false }) {
        HiddenEventList(state, hidden, sync)
    }

    BottomSheet(visible = choosingPhoneCalendars && !deviceCalendars.isNullOrEmpty(), onDismiss = { choosingPhoneCalendars = false }) {
        PhoneCalendarChecklist(deviceCalendars.orEmpty(), subscribed, sync)
    }
}

@Composable
private fun HiddenEventList(state: AppState, hidden: List<HiddenCalendarEvent>, sync: CalendarSync) {
    val formatting = state.formatting
    Column {
        SectionTitle("Tap to Show Again", Modifier.padding(start = Spacing.md, end = Spacing.md, top = Spacing.sm, bottom = Spacing.xs))
        for (event in hidden) {
            ListRow(
                event.title ?: UNTITLED_EVENT,
                supporting = event.startsAt?.let { formatting.formatDay(it.toLocalDateTime(state.timeZone).date) },
                trailing = { Icon(AppIcons.Hidden, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                enabled = !sync.syncing,
                onClick = { sync.unhide(event.source) },
            )
        }
    }
}

@Composable
private fun PhoneCalendarChecklist(calendars: List<DeviceCalendar>, subscribed: List<SubscribedCalendar>, sync: CalendarSync) {
    Column {
        for ((account, accountCalendars) in calendars.groupBy { it.account }) {
            SectionTitle(account ?: "This Phone", Modifier.padding(start = Spacing.md, end = Spacing.md, top = Spacing.sm, bottom = Spacing.xs))
            for (calendar in accountCalendars) {
                val subscription = SubscribedCalendar.OnDevice(calendar.id)
                val on = subscription in subscribed
                ListRow(
                    calendar.name,
                    supporting = sync.failures[subscription],
                    color = if (subscription in sync.failures) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                    leading = { CalendarColorDot(calendar.color) },
                    trailing = { CheckBoxIcon(on) },
                    onClick = { if (on) sync.unsubscribe(subscription) else sync.subscribe(subscription) },
                )
            }
        }
    }
}

@Composable
private fun AccentPicker(picked: ThemeAccent, onPick: (ThemeAccent) -> Unit) {
    val dark = isSystemInDarkTheme()
    val wallpaperAvailable = isWallpaperAccentAvailable()
    SheetTitle("Accent", Modifier.padding(horizontal = Spacing.md))
    Column {
        for (accent in ThemeAccent.entries) {
            if (accent == ThemeAccent.Wallpaper && !wallpaperAvailable) continue
            PickerRow(accent.name, supporting = null, selected = accent == picked, onClick = { onPick(accent) }, leading = { ColorDot(accentColor(accent, dark)) })
        }
    }
}

@Composable
private fun ColorDot(color: Color) {
    Box(Modifier.size(Spacing.md).clip(CircleShape).background(color))
}

@Composable
private fun CalendarColorDot(color: Int?) {
    ColorDot(color?.let(::Color) ?: MaterialTheme.colorScheme.primary)
}

@Composable
fun AboutScreen(state: AppState) {
    val uriHandler = LocalUriHandler.current
    SettingsPage("About", onBack = { state.navigator.goBack() }) {
        ListSection {
            for (link in APP_LINKS) {
                ListRow(
                    link.label,
                    onClick = { uriHandler.openUri(link.url) },
                    onLongClick = { state.openLinkActionsSheet(link.url) },
                    trailing = { Icon(AppIcons.ExternalLink) },
                )
            }
        }
        Text(
            "Flags from Twemoji, CC BY 4.0\nLexend by The Lexend Project Authors, SIL OFL 1.1",
            Modifier.padding(start = Spacing.md, end = Spacing.md, top = Spacing.md),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
        )
        Text(
            "Vantage (Kotlin preview)",
            Modifier.padding(start = Spacing.md, end = Spacing.md, top = Spacing.md),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
internal fun SettingsPage(
    title: String,
    onBack: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    ContentUnderOverlay(overlay = { ScreenHeader(title, onBack = onBack) }, modifier = Modifier.fillMaxSize()) { headerHeight ->
        Column(
            Modifier.fillMaxSize().backdropSource(LocalBackdrop.current).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val bottomInset = if (onBack != null) Modifier.navigationBarsPadding() else Modifier
            Column(
                Modifier.widthIn(max = ContentMaxWidth).fillMaxWidth().then(bottomInset).padding(top = headerHeight, bottom = Spacing.md + LocalTabBarHeight.current),
                content = content,
            )
        }
    }
}

@Composable
private fun SubpageRow(label: String, icon: ImageVector, supporting: String? = null, onClick: () -> Unit) {
    ListRow(label, icon = icon, supporting = supporting, onClick = onClick, trailing = { Icon(AppIcons.ChevronRight) })
}
