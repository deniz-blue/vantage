package blue.deniz.vantage.app.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import blue.deniz.vantage.app.AppState
import blue.deniz.vantage.app.ThemeAccent
import blue.deniz.vantage.app.rememberCalendarPermissionRequest
import blue.deniz.vantage.app.rememberNotificationPermissionRequest
import blue.deniz.vantage.app.ui.AppIcons
import blue.deniz.vantage.app.ui.BottomSheet
import blue.deniz.vantage.app.ui.Button
import blue.deniz.vantage.app.ui.CheckBoxIcon
import blue.deniz.vantage.app.ui.ContentMaxWidth
import blue.deniz.vantage.app.ui.EventCard
import blue.deniz.vantage.app.ui.Icon
import blue.deniz.vantage.app.ui.IconButton
import blue.deniz.vantage.app.ui.IconSize
import blue.deniz.vantage.app.ui.ListRow
import blue.deniz.vantage.app.ui.ListSection
import blue.deniz.vantage.app.ui.Radius
import blue.deniz.vantage.app.ui.SectionTitle
import blue.deniz.vantage.app.ui.Spacing
import blue.deniz.vantage.app.ui.Text
import blue.deniz.vantage.app.ui.accentColor
import blue.deniz.vantage.app.ui.isWallpaperAccentAvailable
import blue.deniz.vantage.app.collectAcrossSession
import blue.deniz.vantage.core.EventFormat
import blue.deniz.vantage.core.EventSource
import blue.deniz.vantage.core.ResolvedEvent
import directory.evnt.OpenEvnt
import kotlinx.coroutines.flow.flow
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus

private enum class SetupStep { Welcome, Appearance, Notifications, SystemCalendars }

private val MARK_RING_STOPS = arrayOf(
    0f to Color(0xFFFF4800),
    0.10f to Color(0xFFFF4800),
    0.35f to Color(0xFFDFD902),
    0.50f to Color(0xFF20DC68),
    0.61f to Color(0xFF0092F4),
    0.72f to Color(0xFFDA54D8),
    0.75f to Color(0xFFDA54D8),
    0.95f to Color(0xFFFF4800),
    1f to Color(0xFFFF4800),
)

private val MARK_GLYPH = listOf(
    "M11.5 21h-5.5a2 2 0 0 1 -2 -2v-12a2 2 0 0 1 2 -2h12a2 2 0 0 1 2 2v4.5",
    "M16 3v4",
    "M8 3v4",
    "M4 11h16",
    "M15 18a3 3 0 1 0 6 0a3 3 0 1 0 -6 0",
    "M20.2 20.2l1.8 1.8",
).map { PathParser().parsePathString(it).toPath() }

@Composable
fun SetupScreen(state: AppState) {
    var stepIndex by rememberSaveable { mutableIntStateOf(0) }
    val steps = remember(state) {
        SetupStep.entries.filter { step ->
            when (step) {
                SetupStep.Notifications -> state.backgroundChecks != null
                SetupStep.SystemCalendars -> state.calendars.deviceCalendars != null
                else -> true
            }
        }
    }
    val step = steps[stepIndex]
    fun finish() {
        state.settings.finishSetup()
        state.navigator.goBack()
    }
    fun next() = if (stepIndex == steps.lastIndex) finish() else stepIndex++

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface), contentAlignment = Alignment.TopCenter) {
        Column(Modifier.widthIn(max = ContentMaxWidth).fillMaxSize().systemBarsPadding()) {
            SetupTopBar(stepIndex, steps.size, onBack = if (stepIndex > 0) ({ stepIndex-- }) else null, onSkip = ::finish)
            AnimatedContent(
                step,
                Modifier.weight(1f),
                transitionSpec = {
                    val forward = targetState.ordinal > initialState.ordinal
                    (slideInHorizontally { width -> if (forward) width / 4 else -width / 4 } + fadeIn()) togetherWith
                        (slideOutHorizontally { width -> if (forward) -width / 4 else width / 4 } + fadeOut())
                },
            ) { shown ->
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = Spacing.md)) {
                    when (shown) {
                        SetupStep.Welcome -> WelcomeStep()
                        SetupStep.Appearance -> AppearanceStep(state)
                        SetupStep.Notifications -> NotificationsStep()
                        SetupStep.SystemCalendars -> SystemCalendarsStep(state)
                    }
                }
            }
            Column(Modifier.fillMaxWidth().padding(Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                when (step) {
                    SetupStep.Notifications -> {
                        val requestPermission = rememberNotificationPermissionRequest { granted ->
                            if (granted) state.changeNotifiesAboutChanges(true)
                            next()
                        }
                        Button("Allow Notifications", onClick = requestPermission, primary = true, modifier = Modifier.fillMaxWidth())
                        Button("Not Now", onClick = { next() }, modifier = Modifier.fillMaxWidth())
                    }
                    else -> Button(
                        when (step) {
                            SetupStep.Welcome -> "Get Started"
                            steps.last() -> "Done"
                            else -> "Next"
                        },
                        onClick = { next() },
                        primary = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Composable
private fun SetupTopBar(stepIndex: Int, stepCount: Int, onBack: (() -> Unit)?, onSkip: () -> Unit) {
    Row(Modifier.fillMaxWidth().height(56.dp).padding(horizontal = Spacing.xs), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
            if (onBack != null) IconButton(AppIcons.ArrowLeft, "Back", onClick = onBack)
        }
        Row(Modifier.weight(1f).padding(horizontal = Spacing.md), horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            repeat(stepCount) { index ->
                Box(
                    Modifier.weight(1f).height(4.dp).clip(CircleShape).background(
                        if (index <= stepIndex) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    ),
                )
            }
        }
        TextButton(onClick = onSkip) { Text("Skip", color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

@Composable
private fun ColumnScope.StepText(title: String, body: String) {
    StepTitle(title)
    StepBody(body)
}

@Composable
private fun StepTitle(title: String) {
    Text(
        title,
        Modifier.fillMaxWidth().padding(start = Spacing.md, end = Spacing.md, top = Spacing.lg, bottom = Spacing.sm),
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun StepBody(body: String) {
    Text(
        body,
        Modifier.fillMaxWidth().padding(start = Spacing.lg, end = Spacing.lg, top = Spacing.sm, bottom = Spacing.md),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodyMedium,
        textAlign = TextAlign.Center,
    )
}

@Composable
fun VantageMark(size: Dp, modifier: Modifier = Modifier) {
    val glyphColor = MaterialTheme.colorScheme.onSurfaceVariant
    Canvas(modifier.size(size)) {
        val unit = this.size.minDimension / 24f
        rotate(180f) {
            drawCircle(Brush.sweepGradient(*MARK_RING_STOPS, center = center), radius = 10.5f * unit, style = Stroke(3f * unit))
        }
        val glyphUnit = unit * 0.75f
        translate(left = center.x - 12f * glyphUnit, top = center.y - 12f * glyphUnit) {
            scale(glyphUnit, glyphUnit, pivot = Offset.Zero) {
                for (path in MARK_GLYPH) drawPath(path, glyphColor, style = Stroke(2f, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
        }
    }
}

@Composable
private fun ColumnScope.WelcomeStep() {
    VantageMark(96.dp, Modifier.align(Alignment.CenterHorizontally).padding(top = Spacing.xl))
    StepText(
        "Welcome to Vantage",
        "A calendar for events from anywhere.",
    )
    SourcesDiagram(Modifier.align(Alignment.CenterHorizontally))
}

@Composable
private fun SourcesDiagram(modifier: Modifier = Modifier) {
    Row(modifier.padding(horizontal = Spacing.md), verticalAlignment = Alignment.CenterVertically) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            SourcePill(AppIcons.Link, "Event Links")
            SourcePill(AppIcons.Rss, "Feeds")
            SourcePill(AppIcons.Calendar, "Calendars")
        }
        Icon(AppIcons.ChevronRight, Modifier.padding(horizontal = Spacing.sm), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Column(
            Modifier.width(176.dp).clip(RoundedCornerShape(Radius.md)).background(MaterialTheme.colorScheme.surfaceVariant).padding(Spacing.sm),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            Text("My Events", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            MiniEventRow("Sat", "Night Market")
            MiniEventRow("Sun", "Bar Meet")
            MiniEventRow("Mon", "Lecture")
        }
    }
}

@Composable
private fun SourcePill(icon: ImageVector, label: String) {
    Row(
        Modifier.clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant).padding(horizontal = Spacing.sm, vertical = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, Modifier.padding(end = Spacing.xs), size = IconSize.xs, tint = MaterialTheme.colorScheme.primary)
        Text(label, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun MiniEventRow(day: String, name: String) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(Radius.xs)).background(MaterialTheme.colorScheme.surface).padding(horizontal = Spacing.sm, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(day, Modifier.width(32.dp), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
        Text(name, style = MaterialTheme.typography.labelMedium, maxLines = 1)
    }
}

private fun previewEvent(today: LocalDate): ResolvedEvent {
    val day = today.plus(5, DateTimeUnit.DAY)
    val document = OpenEvnt.parse(
        """{"v":"0.2","id":"urn:uuid:vantage-setup-preview",
        "name":{"en":"Night Market","lt":"Naktinis turgus","de":"Nachtmarkt","pl":"Nocny targ","lv":"Nakts tirgus","fr":"Marché de nuit","es":"Mercado nocturno","uk":"Нічний ринок","ru":"Ночной рынок","ja":"ナイトマーケット"},
        "venue":{"name":{"en":"Town Hall Square","lt":"Rotušės aikštė","de":"Rathausplatz"},"address":"Vilnius","countryCode":"LT"},
        "occurrences":[{"start":"${day}T19:00[Europe/Vilnius]","end":"${day}T23:00[Europe/Vilnius]"}]}""",
    )
    return ResolvedEvent(source = EventSource.Local(isTestEvent = true), format = EventFormat.OpenEvnt, data = document)
}

@Composable
private fun ColumnScope.AppearanceStep(state: AppState) {
    var openSheet by rememberSaveable { mutableStateOf<SettingsSheet?>(null) }
    val dark = isSystemInDarkTheme()
    val picked = state.settings.themeAccent
    val event = remember(state.today) { previewEvent(state.today) }
    StepTitle("Appearance")
    Column(
        Modifier.padding(horizontal = Spacing.md).fillMaxWidth().clip(RoundedCornerShape(Radius.lg))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(Radius.lg)).padding(bottom = Spacing.sm),
    ) {
        SectionTitle("Coming Up", Modifier.padding(start = Spacing.md, top = Spacing.sm, bottom = Spacing.xs))
        EventCard(event, state.formatting, state.preferredLanguages, Modifier.padding(horizontal = Spacing.sm).fillMaxWidth())
        Row(Modifier.fillMaxWidth().padding(top = Spacing.sm), horizontalArrangement = Arrangement.SpaceEvenly) {
            Box(Modifier.clip(CircleShape).background(MaterialTheme.colorScheme.secondaryContainer).padding(horizontal = Spacing.md, vertical = Spacing.xs)) {
                Icon(AppIcons.HomeSelected, tint = MaterialTheme.colorScheme.onSecondaryContainer)
            }
            Box(Modifier.padding(vertical = Spacing.xs)) { Icon(AppIcons.Calendar, tint = MaterialTheme.colorScheme.onSurfaceVariant) }
            Box(Modifier.padding(vertical = Spacing.xs)) { Icon(AppIcons.Settings, tint = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
    StepBody("Choose how you want the app to look.")
    ListSection {
        LanguageAndRegionRows(state) { openSheet = it }
    }
    val wallpaperAvailable = isWallpaperAccentAvailable()
    Row(
        Modifier.fillMaxWidth().padding(top = Spacing.md, start = Spacing.md, end = Spacing.md),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        for (accent in ThemeAccent.entries) {
            if (accent == ThemeAccent.Wallpaper && !wallpaperAvailable) continue
            AccentSwatch(accent.name, accentColor(accent, dark), selected = accent == picked) { state.settings.changeThemeAccent(accent) }
        }
    }
    LanguageAndRegionSheets(state, openSheet, onDismiss = { openSheet = null })
}

@Composable
private fun AccentSwatch(label: String, color: Color, selected: Boolean, onClick: () -> Unit) {
    Column(Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onClick).padding(Spacing.xs), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.size(44.dp).clip(CircleShape)
                .border(3.dp, if (selected) MaterialTheme.colorScheme.onSurface else Color.Transparent, CircleShape)
                .padding(5.dp).clip(CircleShape).background(color),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) Icon(AppIcons.Check, size = 20.dp, tint = Color.White)
        }
        Text(
            label,
            Modifier.padding(top = Spacing.xs),
            color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelSmall,
        )
    }
}

@Composable
private fun ColumnScope.NotificationsStep() {
    StepText("Notifications", "Allow notifications to get notified about changes to your events")
    Column(Modifier.padding(horizontal = Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        NotificationPreview("Night Market moved", "Saturday, 19:00 → 20:00", "now")
        NotificationPreview("Bar Meet cancelled", "Sunday at Loftas", "2h ago")
    }
}

@Composable
private fun NotificationPreview(title: String, text: String, time: String) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            VantageMark(16.dp)
            Text("Vantage · $time", Modifier.padding(start = Spacing.xs), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
        }
        Text(title, Modifier.padding(top = Spacing.xs), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun ColumnScope.SystemCalendarsStep(state: AppState) {
    val sync = state.calendars
    val device = sync.deviceCalendars
    var permitted by remember { mutableStateOf(device?.hasPermission() == true) }
    val requestPermission = rememberCalendarPermissionRequest { granted -> permitted = granted }
    val deviceCalendars = collectAcrossSession("phone calendars", permitted) { flow { emit(if (permitted) device?.list() else null) } }
    var choosing by rememberSaveable { mutableStateOf(false) }
    val subscribed = state.settings.subscribedCalendars

    StepText("System Calendars", "You can sync events between Vantage and other apps.")
    CalendarFlowDiagram("Import", intoVantage = true, Modifier.align(Alignment.CenterHorizontally))
    ListSection(Modifier.padding(top = Spacing.md)) {
        val listed = deviceCalendars
        if (!permitted || listed == null) {
            ListRow("Show Phone Calendars", icon = AppIcons.Calendar, supporting = "Asks for calendar access", onClick = requestPermission)
        } else {
            val shownCount = listed.count { blue.deniz.vantage.core.SubscribedCalendar.OnDevice(it.id) in subscribed }
            ListRow("Phone Calendars", icon = AppIcons.Calendar, supporting = "$shownCount of ${listed.size} shown", onClick = { choosing = true })
        }
    }
    CalendarFlowDiagram("Export", intoVantage = false, Modifier.align(Alignment.CenterHorizontally).padding(top = Spacing.lg))
    ListSection(Modifier.padding(top = Spacing.md)) {
        if (state.phoneCalendarCopy != null) {
            PhoneCalendarCopyRow(state, onPermitted = { permitted = true })
        } else {
            ListRow("Copy Events to Phone Calendar", icon = AppIcons.Copy, supporting = "Shows your events in other calendar apps", trailing = { CheckBoxIcon(false) }, onClick = {})
        }
    }
    BottomSheet(visible = choosing && !deviceCalendars.isNullOrEmpty(), onDismiss = { choosing = false }) {
        PhoneCalendarChecklist(deviceCalendars.orEmpty(), subscribed, sync)
    }
}

@Composable
private fun CalendarFlowDiagram(label: String, intoVantage: Boolean, modifier: Modifier = Modifier) {
    val phone: @Composable () -> Unit = { DiagramEnd { Icon(AppIcons.Smartphone, size = 32.dp, tint = MaterialTheme.colorScheme.onSurfaceVariant) } }
    val vantage: @Composable () -> Unit = { DiagramEnd { VantageMark(36.dp) } }
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        phone()
        Row(Modifier.padding(horizontal = Spacing.sm), verticalAlignment = Alignment.CenterVertically) {
            if (!intoVantage) Icon(AppIcons.ChevronLeft, size = IconSize.xs, tint = MaterialTheme.colorScheme.primary)
            Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
            if (intoVantage) Icon(AppIcons.ChevronRight, size = IconSize.xs, tint = MaterialTheme.colorScheme.primary)
        }
        vantage()
    }
}

@Composable
private fun DiagramEnd(content: @Composable () -> Unit) {
    Box(Modifier.size(72.dp).clip(RoundedCornerShape(Radius.lg)).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
        content()
    }
}
