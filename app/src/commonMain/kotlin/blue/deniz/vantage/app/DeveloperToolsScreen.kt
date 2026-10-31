package blue.deniz.vantage.app

import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import blue.deniz.vantage.app.ui.ListRow
import blue.deniz.vantage.app.ui.CheckBoxIcon
import blue.deniz.vantage.app.ui.AppIcons
import blue.deniz.vantage.app.ui.ScreenHeader
import blue.deniz.vantage.app.ui.Button
import blue.deniz.vantage.app.ui.Card
import blue.deniz.vantage.app.ui.FontSize
import blue.deniz.vantage.app.ui.IconButton
import blue.deniz.vantage.app.ui.ListSection
import blue.deniz.vantage.app.ui.TypographyStyle
import blue.deniz.vantage.app.ui.defaultFontSize
import blue.deniz.vantage.app.ui.MenuItem
import blue.deniz.vantage.app.ui.Spacing
import blue.deniz.vantage.app.ui.Text
import blue.deniz.vantage.app.ui.TextInput
import blue.deniz.vantage.storage.EventListQuery
import directory.evnt.InvalidOpenEvntException
import directory.evnt.OpenEvnt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import blue.deniz.vantage.app.ui.ContentMaxWidth
import androidx.compose.foundation.layout.widthIn

private const val IMPORT_ISSUES_SHOWN = 5
private const val IMPORT_INPUT_LINES = 10

@Composable
fun DeveloperToolsScreen(state: AppState) {
    val scope = rememberCoroutineScope()
    var addingTestEvents by remember { mutableStateOf(false) }

    fun addTestEvents() {
        addingTestEvents = true
        scope.launch {
            try {
                for (event in testEvents(state.clock.now(), state.timeZone)) state.store.addTestEvent(event)
                state.changeTestEventsShown(true)
            } finally {
                addingTestEvents = false
            }
        }
    }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Developer Tools", onBack = { state.navigator.goBack() })
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).navigationBarsPadding().imePadding(),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            DeveloperTools(state, scope, addingTestEvents, ::addTestEvents, Modifier.widthIn(max = ContentMaxWidth).fillMaxWidth())
        }
    }
}

@Composable
private fun DeveloperTools(
    state: AppState,
    scope: CoroutineScope,
    addingTestEvents: Boolean,
    onAddTestEvents: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var importingJson by rememberSaveable { mutableStateOf(false) }
    SystemBackHandler(enabled = importingJson) { importingJson = false }
    if (importingJson) {
        Column(
            modifier.padding(horizontal = Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            JsonImport(state, scope, onCancel = { importingJson = false })
        }
        return
    }
    Column(modifier) {
        ListSection {
            ToolButton("Import JSON") { importingJson = true }
            ToolButton("Add Test Events", busy = addingTestEvents, onClick = onAddTestEvents)
            TestEventRows(state, scope)
        }
        ListSection(Modifier.padding(top = Spacing.md)) {
            MenuItem(
                "Delete ALL Events (hold to confirm)",
                onClick = {},
                onLongClick = { scope.launch { state.store.removeAll() } },
                danger = true,
            )
        }
        FontSizeRows(state.settings)
    }
}

@Composable
private fun FontSizeRows(settings: AppSettings) {
    ListSection(Modifier.padding(top = Spacing.md), title = "Text Sizes") {
        for (style in TypographyStyle.entries) {
            val default = style.defaultFontSize
            val size = settings.fontSizes[style] ?: default
            fun change(to: Int) = settings.changeFontSize(style, to.takeIf { it != default })
            ListRow(
                style.label,
                supporting = if (size == default) "$size sp" else "$size sp, default $default",
                trailing = {
                    Row {
                        IconButton(AppIcons.Minus, "Smaller", { change(size - 1) }, enabled = size > 1)
                        IconButton(AppIcons.Plus, "Larger", { change(size + 1) })
                    }
                },
            )
        }
        if (settings.fontSizes.isNotEmpty()) {
            MenuItem("Reset Text Sizes", onClick = { for (style in TypographyStyle.entries) settings.changeFontSize(style, null) })
        }
    }
}

@Composable
private fun TestEventRows(state: AppState, scope: CoroutineScope) {
    val testEventCount = collectAcrossSession("test event count", null) {
        state.store.observeList(EventListQuery(onlyTestEvents = true)).map { it.size }
    } ?: 0
    if (testEventCount == 0) return
    val shown = state.settings.testEventsShown
    ListRow(
        "Show Test Events",
        supporting = testEventCountLabel(testEventCount),
        trailing = { CheckBoxIcon(shown) },
        onClick = { state.changeTestEventsShown(!shown) },
    )
    MenuItem("Remove Test Events", onClick = { scope.launch { state.store.removeTestEvents() } })
}

private fun testEventCountLabel(count: Int) = if (count == 1) "1 Test Event" else "$count Test Events"

@Composable
private fun ToolButton(label: String, enabled: Boolean = true, busy: Boolean = false, onClick: () -> Unit) {
    MenuItem(label, onClick, enabled = enabled, busy = busy)
}

private data class ImportResult(val saved: Boolean, val message: String)

@Composable
private fun JsonImport(state: AppState, scope: CoroutineScope, onCancel: () -> Unit) {
    var text by rememberSaveable { mutableStateOf("") }
    var result by remember { mutableStateOf<ImportResult?>(null) }
    var importing by remember { mutableStateOf(false) }

    fun importEvent() {
        result = null
        importing = true
        scope.launch {
            result = try {
                state.store.addLocal(OpenEvnt.parse(text))
                text = ""
                ImportResult(saved = true, "Event saved!")
            } catch (e: InvalidOpenEvntException) {
                val issues = e.issues.take(IMPORT_ISSUES_SHOWN).joinToString("\n") { "  • $it" }
                ImportResult(saved = false, "Validation failed:\n$issues")
            } catch (e: IllegalArgumentException) {
                ImportResult(saved = false, "JSON parse error: ${e.message}")
            } finally {
                importing = false
            }
        }
    }

    Text("Import Event JSON", fontSize = FontSize.h1, fontWeight = FontWeight.Bold)
    Text("Paste an OpenEvnt event as JSON below. It will be validated and saved as a local event.", color = MaterialTheme.colorScheme.onSurfaceVariant)
    TextInput(
        text,
        { text = it },
        Modifier.fillMaxWidth(),
        label = "JSON",
        placeholder = """{ "v": "${OpenEvnt.VERSION}", "name": { "en": "My Event" } }""",
        singleLine = false,
        minLines = IMPORT_INPUT_LINES,
    )
    result?.let {
        Card(Modifier.fillMaxWidth(), background = if (it.saved) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.errorContainer) {
            Text(it.message, style = MaterialTheme.typography.bodyMedium)
        }
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.sm, Alignment.End)) {
        Button("Cancel", onClick = onCancel)
        Button("Import", onClick = ::importEvent, primary = true, enabled = text.isNotBlank(), busy = importing)
    }
}
