package blue.deniz.vantage.core

import directory.evnt.Translations
import directory.evnt.translate
import io.ktor.http.URLBuilder
import kotlin.time.Instant

private const val INDEX_SEARCH_PARAMETER = "search"
private const val INDEX_AFTER_PARAMETER = "after"
private const val INDEX_BEFORE_PARAMETER = "before"

data class GeoArea(val south: Double, val west: Double, val north: Double, val east: Double)

enum class TimeFilter { Past, All, Future }

enum class EventOrder { Listed, Start, StartDescending, Name }

data class EventQuery(
    val search: String = "",
    val from: Instant? = null,
    val until: Instant? = null,
    val endedBy: Instant? = null,
    val area: GeoArea? = null,
    val order: EventOrder = EventOrder.Listed,
) {
    val filtersOrSorts: Boolean get() = search.isNotBlank() || from != null || until != null || endedBy != null || area != null || order != EventOrder.Listed

    fun indexPageUrl(indexUrl: String, supports: Set<String>): String {
        val parameters = buildMap {
            if (search.isNotBlank() && INDEX_SEARCH_PARAMETER in supports) put(INDEX_SEARCH_PARAMETER, search.trim())
            if (from != null && INDEX_AFTER_PARAMETER in supports) put(INDEX_AFTER_PARAMETER, from.toString())
            if (until != null && INDEX_BEFORE_PARAMETER in supports) put(INDEX_BEFORE_PARAMETER, until.toString())
        }
        if (parameters.isEmpty()) return indexUrl
        return URLBuilder(indexUrl).apply { parameters.forEach { (name, value) -> this.parameters[name] = value } }.buildString()
    }

    fun unsupportedPart(supports: Set<String>): EventQuery = copy(
        search = if (INDEX_SEARCH_PARAMETER in supports) "" else search,
        from = if (INDEX_AFTER_PARAMETER in supports) null else from,
        until = if (INDEX_BEFORE_PARAMETER in supports) null else until,
    )

    fun filterAndSort(
        events: List<ResolvedEvent>,
        preferredLanguages: List<String>,
        listedNames: Map<EventSource, Translations> = emptyMap(),
    ): List<ResolvedEvent> {
        val matching = events.filter { matches(it, listedNames[it.source]) }
        return when (order) {
            EventOrder.Listed -> matching
            EventOrder.Start -> matching.sortedWith(compareBy(nullsLast()) { it.data?.nextStartAt(from ?: Instant.DISTANT_PAST) })
            EventOrder.StartDescending -> matching.sortedWith(compareBy(nullsLast(reverseOrder())) { event -> event.data?.timeRanges()?.maxOfOrNull { it.start } })
            EventOrder.Name -> matching.sortedBy { it.nameIn(preferredLanguages, listedNames[it.source]).lowercase() }
        }
    }

    private fun matches(event: ResolvedEvent, listedName: Translations?): Boolean {
        val text = search.trim()
        if (text.isNotEmpty()) {
            val names = event.data?.name?.values.orEmpty() + listedName?.values.orEmpty()
            if (names.none { it.contains(text, ignoreCase = true) }) return false
        }
        val ranges = if (from != null || until != null || endedBy != null) event.data?.timeRanges().orEmpty() else emptyList()
        if (from != null || until != null) {
            val undatedCountsAsAhead = until == null && ranges.isEmpty()
            if (!undatedCountsAsAhead && ranges.none { (from == null || it.end > from) && (until == null || it.start < until) }) return false
        }
        if (endedBy != null && (ranges.isEmpty() || ranges.any { it.end > endedBy })) return false
        if (area != null) {
            val coordinates = event.data?.venue?.coordinates ?: return false
            if (coordinates.latitude !in area.south..area.north || coordinates.longitude !in area.west..area.east) return false
        }
        return true
    }
}

fun TimeFilter.toQuery(now: Instant, search: String = "", order: EventOrder = EventOrder.Listed) = EventQuery(
    search = search,
    from = now.takeIf { this == TimeFilter.Future },
    endedBy = now.takeIf { this == TimeFilter.Past },
    order = order,
)

private fun ResolvedEvent.nameIn(preferredLanguages: List<String>, listedName: Translations?): String =
    data?.name.translate(preferredLanguages).ifEmpty { listedName.translate(preferredLanguages) }
        .ifEmpty { (source as? EventSource.Remote)?.location.orEmpty() }
