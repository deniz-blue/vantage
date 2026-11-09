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
import blue.deniz.vantage.core.CountryTimeZone
import blue.deniz.vantage.core.CountryTimeZones
import blue.deniz.vantage.core.offsetLabelOfQuery
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.datetime.offsetAt
import kotlin.time.Duration.Companion.days
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

private const val TIME_ZONE_SUGGESTIONS_SHOWN = 5
private const val OFFSET_FLAGS_SHOWN = 8
private val HALF_A_YEAR = 182.days

data class TimeZoneSuggestions(
    val placeZoneId: String? = null,
    val placeName: String? = null,
    val homeZoneId: String? = null,
    val usedZoneIds: List<String> = emptyList(),
)

private class ZoneAtOffset(val zone: CountryTimeZone, val offsetLabel: String, val offsetSeconds: Int, val offsetSecondsInHalfAYear: Int)

@Composable
fun BottomSheetScope.TimeZonePickerBody(
    title: String,
    selectedId: String?,
    now: Instant,
    displayLanguage: String,
    suggestions: TimeZoneSuggestions = TimeZoneSuggestions(),
    onBack: (() -> Unit)? = null,
    onPick: (String) -> Unit,
) {
    val available = remember { TimeZone.availableZoneIds }
    val zones = remember(now) {
        CountryTimeZones.all.filter { it.id in available }.map {
            val zone = TimeZone.of(it.id)
            ZoneAtOffset(it, timeZoneOffsetLabel(zone, now), zone.offsetAt(now).totalSeconds, zone.offsetAt(now + HALF_A_YEAR).totalSeconds)
        }
    }
    val countryNames = remember(displayLanguage) { mutableMapOf<String, String>() }
    fun countryOf(code: String) = countryNames.getOrPut(code) { countryName(code, displayLanguage) }
    fun offsetOf(id: String) = zones.firstOrNull { it.zone.id == id }?.offsetLabel ?: timeZoneOffsetLabel(TimeZone.of(id), now)
    fun oneZonePerCountryAt(label: String) = zones.filter { it.offsetLabel == label }.distinctBy { it.zone.countryCode }.sortedBy { countryOf(it.zone.countryCode) }
    val homeCountry = suggestions.homeZoneId?.let(CountryTimeZones::of)?.countryCode
    val offsets = remember(zones) { zones.groupBy { it.offsetSeconds }.toList().sortedBy { it.first }.map { it.second } }

    var query by rememberSaveable { mutableStateOf("") }
    var browsingOffsets by rememberSaveable { mutableStateOf(false) }
    var openOffset by rememberSaveable { mutableStateOf<String?>(null) }
    val text = query.trim()
    val list = rememberLazyListState()
    LaunchedEffect(text.isEmpty(), browsingOffsets, openOffset) {
        val homeOffset = suggestions.homeZoneId?.let(::offsetOf)
        val showingOffsets = text.isEmpty() && browsingOffsets && openOffset == null
        list.scrollToItem(if (showingOffsets) (offsets.indexOfFirst { it.first().offsetLabel == homeOffset } - 2).coerceAtLeast(0) else 0)
    }

    @Composable
    fun ZoneRow(id: String, supporting: String?, offsetLabel: String?) {
        val zone = CountryTimeZones.of(id)
        PickerRow(
            zone?.let { "${Countries.flagOf(it.countryCode)}  ${it.city}" } ?: id.replace('_', ' '),
            supporting = supporting,
            selected = id == selectedId,
            onClick = { onPick(id) },
            trailingText = offsetLabel,
        )
    }

    @Composable
    fun NextLevelRow(headline: String, supporting: String?, icon: ImageVector?, onClick: () -> Unit) {
        ListRow(
            headline,
            icon = icon,
            supporting = supporting,
            onClick = onClick,
            trailing = { Icon(AppIcons.ChevronRight, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
        )
    }

    val shownOffset = openOffset
    ListBody(
        header = {
            PickerHeader(
                shownOffset ?: if (browsingOffsets) "By Offset" else title,
                onBack = when {
                    shownOffset != null -> ({ openOffset = null })
                    browsingOffsets -> ({ browsingOffsets = false })
                    else -> onBack
                },
                Modifier.padding(horizontal = Spacing.md),
            )
        },
        overlay = {
            SearchField(
                query,
                { query = it },
                placeholder = "City, Country or +2",
                modifier = Modifier.fillMaxWidth(),
                trailing = if (query.isEmpty()) null else ({ IconButton(AppIcons.X, "Clear", onClick = { query = "" }) }),
            )
        },
        state = list,
    ) {
        when {
            text.isNotEmpty() -> {
                val offsetLabel = offsetLabelOfQuery(text)
                val found = if (offsetLabel != null) {
                    oneZonePerCountryAt(offsetLabel)
                } else {
                    val byCity = zones.filter { it.zone.city.contains(text, ignoreCase = true) || it.zone.id.contains(text.replace(' ', '_'), ignoreCase = true) }
                        .sortedBy { if (it.zone.city.startsWith(text, ignoreCase = true)) 0 else 1 }
                    val byCountry = zones.filter { it.zone.countryCode.equals(text, ignoreCase = true) || countryOf(it.zone.countryCode).contains(text, ignoreCase = true) }
                        .distinctBy { Triple(it.zone.countryCode, it.offsetSeconds, it.offsetSecondsInHalfAYear) }
                    (byCity + byCountry).distinctBy { it.zone.id }
                }
                if (offsetLabel == "UTC") item(key = "UTC") { ZoneRow("UTC", supporting = null, offsetLabel = "UTC") }
                items(found, key = { it.zone.id }) { ZoneRow(it.zone.id, countryOf(it.zone.countryCode), it.offsetLabel) }
            }
            shownOffset != null -> items(oneZonePerCountryAt(shownOffset), key = { it.zone.id }) {
                ZoneRow(it.zone.id, countryOf(it.zone.countryCode), offsetLabel = null)
            }
            browsingOffsets -> items(offsets, key = { it.first().offsetSeconds }) { group ->
                val countries = group.map { it.zone.countryCode }.distinct().sortedWith(compareBy({ it != homeCountry }, { countryOf(it) }))
                val flags = countries.take(OFFSET_FLAGS_SHOWN).joinToString(" ") { Countries.flagOf(it) } +
                    if (countries.size > OFFSET_FLAGS_SHOWN) "  +${countries.size - OFFSET_FLAGS_SHOWN}" else ""
                val label = group.first().offsetLabel
                NextLevelRow(label, flags, icon = null, onClick = { openOffset = label })
            }
            else -> {
                val shortList = (listOfNotNull(suggestions.placeZoneId, selectedId, suggestions.homeZoneId) + suggestions.usedZoneIds)
                    .filter { it in available }.distinct().take(TIME_ZONE_SUGGESTIONS_SHOWN)
                items(shortList, key = { it }) { id ->
                    val supporting = suggestions.placeName?.takeIf { id == suggestions.placeZoneId } ?: CountryTimeZones.of(id)?.let { countryOf(it.countryCode) }
                    ZoneRow(id, supporting, offsetOf(id))
                }
                item(key = "by-offset") { NextLevelRow("By Offset", supporting = null, icon = AppIcons.Clock, onClick = { browsingOffsets = true }) }
            }
        }
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
