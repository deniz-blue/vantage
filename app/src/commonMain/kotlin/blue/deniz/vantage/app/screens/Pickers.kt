package blue.deniz.vantage.app.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import blue.deniz.vantage.app.SystemBackHandler
import blue.deniz.vantage.app.countryName
import blue.deniz.vantage.app.contentLanguage
import blue.deniz.vantage.app.ui.AppIcons
import blue.deniz.vantage.app.ui.BottomSheetScope
import blue.deniz.vantage.app.ui.Icon
import blue.deniz.vantage.app.ui.IconButton
import blue.deniz.vantage.app.ui.IconButtonInset
import blue.deniz.vantage.app.ui.IconSize
import blue.deniz.vantage.app.ui.ListRow
import blue.deniz.vantage.app.ui.SearchField
import blue.deniz.vantage.app.ui.Spacing
import blue.deniz.vantage.app.ui.Text
import blue.deniz.vantage.app.ui.SheetTitle
import blue.deniz.vantage.core.Countries
import blue.deniz.vantage.core.Country
import blue.deniz.vantage.core.Languages
import blue.deniz.vantage.core.timeZoneOffsetLabel
import kotlin.time.Instant
import kotlinx.datetime.TimeZone

@Composable
internal fun PickerRow(
    headline: String,
    supporting: String?,
    selected: Boolean,
    onClick: () -> Unit,
    partlySelected: Boolean = false,
    trailingText: String? = null,
    leading: (@Composable () -> Unit)? = null,
) {
    ListRow(
        headline,
        supporting = supporting,
        onClick = onClick,
        leading = leading,
        trailing = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                trailingText?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelLarge) }
                when {
                    selected -> Icon(AppIcons.Check, tint = MaterialTheme.colorScheme.primary)
                    partlySelected -> Icon(AppIcons.Minus, tint = MaterialTheme.colorScheme.primary)
                    else -> Spacer(Modifier.size(IconSize.md))
                }
            }
        },
    )
}

@Composable
internal fun PickerHeader(title: String, onBack: (() -> Unit)?, modifier: Modifier = Modifier, actions: @Composable RowScope.() -> Unit = {}) {
    if (onBack == null) {
        SheetTitle(title, modifier)
        return
    }
    SystemBackHandler(enabled = true, onBack = onBack)
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Row(
            Modifier.weight(1f).offset(x = -IconButtonInset),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            IconButton(AppIcons.ArrowLeft, "Back", onBack)
            SheetTitle(title)
        }
        actions()
    }
}

@Composable
private fun <T> BottomSheetScope.PickerBody(
    title: String,
    onBack: (() -> Unit)?,
    searchPlaceholder: String,
    options: List<T>,
    search: (List<T>, String) -> List<T>,
    isSelected: (T) -> Boolean,
    row: @Composable (T) -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    val matching = remember(options, query) { search(options, query.trim()) }
    val list = rememberLazyListState(initialFirstVisibleItemIndex = remember(options) { options.indexOfFirst(isSelected).coerceAtLeast(0) })
    LaunchedEffect(query) { if (query.isNotEmpty()) list.scrollToItem(0) }
    ListBody(
        header = { PickerHeader(title, onBack, Modifier.padding(horizontal = Spacing.md)) },
        overlay = { SearchField(query, { query = it }, placeholder = searchPlaceholder, modifier = Modifier.fillMaxWidth(), focusesWhenShown = true) },
        state = list,
    ) {
        items(matching) { row(it) }
    }
}

private val ZONE_REGIONS = setOf("Africa", "America", "Antarctica", "Arctic", "Asia", "Atlantic", "Australia", "Europe", "Indian", "Pacific")

private fun isRegionZone(id: String) = id == "UTC" || id.substringBefore('/', "") in ZONE_REGIONS

@Composable
fun BottomSheetScope.TimeZonePickerBody(title: String, selectedId: String?, now: Instant, onBack: (() -> Unit)? = null, onPick: (String) -> Unit) {
    val zones = remember(selectedId) { (TimeZone.availableZoneIds.filter(::isRegionZone) + listOfNotNull(selectedId?.takeIf { it in TimeZone.availableZoneIds })).distinct().sorted() }
    PickerBody(
        title,
        onBack,
        "Search time zones",
        zones,
        search = { all, query -> all.filter { it.contains(query.replace(' ', '_'), ignoreCase = true) } },
        isSelected = { it == selectedId },
    ) { zone ->
        val offset = remember(zone) { timeZoneOffsetLabel(TimeZone.of(zone), now) }
        PickerRow(zone.replace('_', ' '), supporting = null, selected = zone == selectedId, onClick = { onPick(zone) }, trailingText = offset)
    }
}

@Composable
fun BottomSheetScope.LanguagePickerBody(title: String, selectedCode: String, onBack: (() -> Unit)? = null, onPick: (String) -> Unit) {
    val languages = remember { Languages.ENGLISH_NAMES.keys.map(::contentLanguage) }
    PickerBody(
        title,
        onBack,
        "Search languages",
        languages,
        search = { all, query -> all.filter { it.matches(query) } },
        isSelected = { it.code == selectedCode },
    ) { language ->
        PickerRow(
            language.autonym,
            supporting = language.englishName.takeIf { it != language.autonym },
            selected = language.code == selectedCode,
            onClick = { onPick(language.code) },
        )
    }
}

@Composable
fun BottomSheetScope.CountryPickerBody(
    title: String,
    selectedCode: String?,
    displayLanguage: String,
    onBack: (() -> Unit)? = null,
    onPick: (String) -> Unit,
) {
    val countries = remember(displayLanguage) { Countries.CODES.map { Country(it, countryName(it, displayLanguage), countryName(it, "en")) } }
    PickerBody(
        title,
        onBack,
        "Search countries",
        countries,
        search = Countries::search,
        isSelected = { it.code == selectedCode },
    ) { country ->
        PickerRow(
            "${country.flag}  ${country.name}",
            supporting = country.englishName.takeIf { it != country.name },
            selected = country.code == selectedCode,
            onClick = { onPick(country.code) },
            trailingText = country.code,
        )
    }
}
