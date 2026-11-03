package blue.deniz.vantage.storage

import androidx.room3.RoomRawQuery
import blue.deniz.vantage.core.EventFormat
import blue.deniz.vantage.core.EventSourceType
import blue.deniz.vantage.core.GeoArea
import directory.evnt.Component
import kotlin.time.Instant

data class EventListQuery(
    val ids: Set<String>? = null,
    val search: String? = null,
    val hasError: Boolean? = null,
    val sourceTypes: Set<EventSourceType> = emptySet(),
    val onlyTestEvents: Boolean = false,
    val format: EventFormat? = null,
    val from: Instant? = null,
    val until: Instant? = null,
    val endedBy: Instant? = null,
    val allTagIds: Set<String> = emptySet(),
    val anyTagIds: Set<String> = emptySet(),
    val excludedTagIds: Set<String> = emptySet(),
    val area: GeoArea? = null,
    val order: Order = Order.RecentlyAdded,
    val limit: Int? = null,
    val offset: Int = 0,
) {
    enum class Order { RecentlyAdded, Name, Start, StartDescending }
}

private fun escapeLike(text: String) = text.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")

private fun placeholders(count: Int) = List(count) { "?" }.joinToString(", ")

internal fun EventListQuery.toRawQuery(includesTestEvents: Boolean = false): RoomRawQuery {
    val conditions = mutableListOf<String>()
    val arguments = mutableListOf<Any>()

    ids?.let {
        conditions += if (it.isEmpty()) "0" else "e.id IN (${placeholders(it.size)})"
        arguments.addAll(it)
    }
    search?.trim()?.takeIf { it.isNotEmpty() }?.let { text ->
        val pattern = "%${escapeLike(text)}%"
        conditions += "(EXISTS (SELECT 1 FROM json_each(c.data, '$.name') WHERE value LIKE ? ESCAPE '\\')" +
            " OR EXISTS (SELECT 1 FROM json_each(c.data, '$.venue.name') WHERE value LIKE ? ESCAPE '\\')" +
            " OR json_extract(c.data, '$.venue.address') LIKE ? ESCAPE '\\')"
        repeat(3) { arguments += pattern }
    }
    when (hasError) {
        true -> conditions += "(c.fetchError IS NOT NULL OR c.parseError IS NOT NULL)"
        false -> conditions += "(c.fetchError IS NULL AND c.parseError IS NULL)"
        null -> {}
    }
    if (sourceTypes.isNotEmpty()) {
        conditions += "json_extract(e.source, '$.type') IN (${placeholders(sourceTypes.size)})"
        arguments.addAll(sourceTypes.map { it.serialName })
    }
    when {
        onlyTestEvents -> conditions += "json_extract(e.source, '$.isTestEvent') = 1"
        !includesTestEvents -> conditions += "json_extract(e.source, '$.isTestEvent') IS NOT 1"
    }
    format?.let {
        conditions += "e.format = ?"
        arguments += it.serialName
    }

    val rangeConditions = buildList {
        from?.let {
            add("r.endsAt > ?")
            arguments += it.toEpochMilliseconds()
        }
        until?.let {
            add("r.startsAt < ?")
            arguments += it.toEpochMilliseconds()
        }
    }
    if (rangeConditions.isNotEmpty()) {
        val inWindow = "EXISTS (SELECT 1 FROM event_time_ranges r WHERE r.eventId = e.id AND ${rangeConditions.joinToString(" AND ")})"
        conditions += if (until == null) "($inWindow OR NOT EXISTS (SELECT 1 FROM event_time_ranges r WHERE r.eventId = e.id))" else inWindow
    }
    endedBy?.let {
        conditions += "EXISTS (SELECT 1 FROM event_time_ranges r WHERE r.eventId = e.id)" +
            " AND NOT EXISTS (SELECT 1 FROM event_time_ranges r WHERE r.eventId = e.id AND r.endsAt > ?)"
        arguments += it.toEpochMilliseconds()
    }

    if (allTagIds.isNotEmpty()) {
        conditions += "(SELECT COUNT(DISTINCT t.tagId) FROM event_tags t WHERE t.eventId = e.id AND t.tagId IN (${placeholders(allTagIds.size)})) = ?"
        arguments.addAll(allTagIds)
        arguments += allTagIds.size.toLong()
    }
    if (anyTagIds.isNotEmpty()) {
        conditions += "EXISTS (SELECT 1 FROM event_tags t WHERE t.eventId = e.id AND t.tagId IN (${placeholders(anyTagIds.size)}))"
        arguments.addAll(anyTagIds)
    }
    if (excludedTagIds.isNotEmpty()) {
        conditions += "NOT EXISTS (SELECT 1 FROM event_tags t WHERE t.eventId = e.id AND t.tagId IN (${placeholders(excludedTagIds.size)}))"
        arguments.addAll(excludedTagIds)
    }
    area?.let {
        conditions += "EXISTS (SELECT 1 FROM json_each(c.data, '$.venue.components') p" +
            " WHERE json_extract(p.value, '$.\"\$type\"') = ?" +
            " AND json_extract(p.value, '$.latitude') BETWEEN ? AND ?" +
            " AND json_extract(p.value, '$.longitude') BETWEEN ? AND ?)"
        arguments += Component.Coordinates.TYPE_ID
        arguments.addAll(listOf(it.south, it.north, it.west, it.east))
    }

    val orderBy = when (order) {
        EventListQuery.Order.RecentlyAdded -> "e.addedAt DESC"
        EventListQuery.Order.Name -> "c.title COLLATE NOCASE NULLS LAST"
        EventListQuery.Order.Start -> {
            val window = from?.let {
                arguments += it.toEpochMilliseconds()
                " AND r.endsAt > ?"
            } ?: ""
            "(SELECT MIN(r.startsAt) FROM event_time_ranges r WHERE r.eventId = e.id$window) NULLS LAST"
        }
        EventListQuery.Order.StartDescending -> "(SELECT MAX(r.startsAt) FROM event_time_ranges r WHERE r.eventId = e.id) DESC NULLS LAST"
    }

    val sql = buildString {
        append("SELECT e.* FROM events e LEFT JOIN event_cache c ON c.eventId = e.id")
        if (conditions.isNotEmpty()) append(" WHERE ").append(conditions.joinToString(" AND "))
        append(" ORDER BY ").append(orderBy).append(", e.id")
        append(" LIMIT ? OFFSET ?")
    }
    arguments += (limit ?: -1).toLong()
    arguments += offset.toLong()

    return RoomRawQuery(sql) { statement ->
        arguments.forEachIndexed { index, argument ->
            when (argument) {
                is String -> statement.bindText(index + 1, argument)
                is Long -> statement.bindLong(index + 1, argument)
                is Double -> statement.bindDouble(index + 1, argument)
                else -> error("Cannot bind $argument")
            }
        }
    }
}
