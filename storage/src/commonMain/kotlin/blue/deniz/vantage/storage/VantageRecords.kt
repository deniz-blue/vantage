package blue.deniz.vantage.storage

import blue.deniz.vantage.core.CalendarMarker
import blue.deniz.vantage.core.CalendarPeriod
import blue.deniz.vantage.core.EventFormat
import blue.deniz.vantage.core.EventSource
import blue.deniz.vantage.core.ResolvedEvent
import directory.evnt.PartialDate
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlin.time.Instant

object VantageRecordTypes {
    const val EVENT = "lt.tsx.vantage.event#0"
    const val TAG = "lt.tsx.vantage.tag#0"
    const val TAGGING = "lt.tsx.vantage.tagging#0"
    const val MARKER = "lt.tsx.vantage.marker#0"
    const val PERIOD = "lt.tsx.vantage.period#0"
    const val CREDENTIAL = "lt.tsx.vantage.credential#0"

    val all = listOf(EVENT, TAG, TAGGING, MARKER, PERIOD, CREDENTIAL)
}

data class RecordKey(val type: String, val id: String)

@Serializable
internal data class EventRecord(val source: EventSource, val format: EventFormat, val createdAt: Instant, val document: JsonElement? = null)

@Serializable
internal data class TagRecord(val name: String, val color: String? = null, val parents: List<String> = emptyList())

@Serializable
internal data class TaggingRecord(val event: String, val tag: String)

@Serializable
internal data class MarkerRecord(val label: String, val at: PartialDate)

@Serializable
internal data class PeriodRecord(val label: String, val start: PartialDate, val end: PartialDate)

@Serializable
internal data class CredentialRecord(val url: String, val secret: CredentialSecret)

internal val recordJson = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
}

private fun <T> encodeRecord(serializer: KSerializer<T>, value: T): JsonElement = recordJson.encodeToJsonElement(serializer, value)

internal fun <T> decodeRecord(serializer: KSerializer<T>, value: JsonElement): T? = try {
    recordJson.decodeFromJsonElement(serializer, value)
} catch (_: SerializationException) {
    null
} catch (_: IllegalArgumentException) {
    null
}

internal fun taggingId(eventId: String, tagId: String) = "$eventId:$tagId"

internal suspend fun VantageDatabase.currentRecords(): Map<RecordKey, JsonElement> {
    val records = HashMap<RecordKey, JsonElement>()
    val portableEvents = events().allEvents().filter { it.event.source.isPortable() }
    for (stored in portableEvents) {
        val event = stored.event
        val document = if (event.source is EventSource.Local) stored.cache?.raw?.let(::documentOf) else null
        records[RecordKey(VantageRecordTypes.EVENT, event.id)] =
            encodeRecord(EventRecord.serializer(), EventRecord(event.source, event.format, event.createdAt, document))
    }
    val parentsByTag = tags().allParentLinks().groupBy({ it.childId }, { it.parentId })
    for (tag in tags().all()) {
        records[RecordKey(VantageRecordTypes.TAG, tag.id)] =
            encodeRecord(TagRecord.serializer(), TagRecord(tag.name, tag.color, parentsByTag[tag.id].orEmpty().sorted()))
    }
    val portableEventIds = portableEvents.mapTo(HashSet()) { it.event.id }
    for (link in tags().allEventLinks().filter { it.eventId in portableEventIds }) {
        records[RecordKey(VantageRecordTypes.TAGGING, taggingId(link.eventId, link.tagId))] =
            encodeRecord(TaggingRecord.serializer(), TaggingRecord(link.eventId, link.tagId))
    }
    for (marker in markers().all()) {
        records[RecordKey(VantageRecordTypes.MARKER, marker.id)] = encodeRecord(MarkerRecord.serializer(), MarkerRecord(marker.label, marker.at))
    }
    for (period in periods().all()) {
        records[RecordKey(VantageRecordTypes.PERIOD, period.id)] =
            encodeRecord(PeriodRecord.serializer(), PeriodRecord(period.label, period.start, period.end))
    }
    for (credential in credentials().all()) {
        records[RecordKey(VantageRecordTypes.CREDENTIAL, credential.id)] =
            encodeRecord(CredentialRecord.serializer(), CredentialRecord(credential.url, credential.secret))
    }
    return records
}

internal class PreparedEvent(val record: EventRecord, val resolved: ResolvedEvent?)

internal class RecordWriter(private val database: VantageDatabase, private val store: EventStore, private val now: Instant) {
    private val events get() = database.events()
    private val tags get() = database.tags()

    suspend fun prepareEvent(id: String, value: JsonElement): PreparedEvent? {
        val record = decodeRecord(EventRecord.serializer(), value)?.takeIf { it.source.isPortable() } ?: return null
        val existing = events.findEvent(id)?.event
        val needsData = when (record.source) {
            is EventSource.Local -> true
            else -> existing?.source != record.source
        }
        if (!needsData) return PreparedEvent(record, null)
        val event = ResolvedEvent(
            id = id,
            source = record.source,
            format = record.format,
            raw = record.document?.let(::rawOf),
            updatedAt = now.takeIf { record.source !is EventSource.Remote },
        )
        val resolved = if (record.source is EventSource.Remote) store.resolver.resolve(event) else store.resolver.parse(event)
        return PreparedEvent(record, resolved)
    }

    suspend fun write(key: RecordKey, value: JsonElement?, preparedEvent: PreparedEvent?): Boolean =
        if (value == null) {
            remove(key)
            true
        } else {
            save(key, value, preparedEvent)
        }

    private suspend fun save(key: RecordKey, value: JsonElement, preparedEvent: PreparedEvent?): Boolean = when (key.type) {
        VantageRecordTypes.EVENT -> preparedEvent?.let { saveEvent(key.id, it) } ?: false
        VantageRecordTypes.TAG -> decodeRecord(TagRecord.serializer(), value)?.let { saveTag(key.id, it) } ?: false
        VantageRecordTypes.TAGGING -> decodeRecord(TaggingRecord.serializer(), value)?.let { saveTagging(key.id, it) } ?: false
        VantageRecordTypes.MARKER -> decodeRecord(MarkerRecord.serializer(), value)?.let { record ->
            val marker = runCatching { CalendarMarker(key.id, record.label, record.at) }.getOrNull() ?: return false
            database.markers().upsert(MarkerEntity(marker.id, marker.label, marker.at, marker.instant))
            true
        } ?: false
        VantageRecordTypes.PERIOD -> decodeRecord(PeriodRecord.serializer(), value)?.let { record ->
            val period = runCatching { CalendarPeriod(key.id, record.label, record.start, record.end) }.getOrNull() ?: return false
            database.periods().upsert(PeriodEntity(period.id, period.label, period.start, period.end, period.startsAt, period.endsAt))
            true
        } ?: false
        VantageRecordTypes.CREDENTIAL -> decodeRecord(CredentialRecord.serializer(), value)?.let { record ->
            database.credentials().upsert(CredentialEntity(key.id, record.url, record.secret, now))
            true
        } ?: false
        else -> false
    }

    private suspend fun saveEvent(id: String, prepared: PreparedEvent): Boolean {
        val record = prepared.record
        val resolved = prepared.resolved
        val existing = events.findEvent(id)
        if (existing == null) {
            resolved ?: return false
            events.insertEventWithCache(
                EventEntity(id, record.source, resolved.format, record.createdAt),
                store.cacheOf(id, resolved),
                store.timeRangesOf(id, resolved),
            )
            return true
        }
        events.updateEvent(EventEntity(id, record.source, resolved?.format ?: record.format, record.createdAt))
        if (resolved != null) events.saveCache(store.cacheOf(id, resolved), store.timeRangesOf(id, resolved))
        return true
    }

    private suspend fun saveTag(id: String, record: TagRecord): Boolean {
        val existing = tags.find(id)
        if (existing == null || existing.name != record.name || existing.color != record.color) {
            tags.upsert(TagEntity(id, record.name, record.color, updatedAt = now))
        }
        return true
    }

    suspend fun saveTagParents(id: String, value: JsonElement) {
        val record = decodeRecord(TagRecord.serializer(), value) ?: return
        if (tags.find(id) == null) return
        tags.removeParentsOf(id)
        record.parents.filter { it != id && tags.find(it) != null }.forEach { tags.addParent(TagParentEntity(it, id)) }
    }

    private suspend fun saveTagging(id: String, record: TaggingRecord): Boolean {
        if (id != taggingId(record.event, record.tag)) return false
        if (events.findEvent(record.event) == null || tags.find(record.tag) == null) return false
        tags.tagEvent(EventTagEntity(record.event, record.tag))
        return true
    }

    private suspend fun remove(key: RecordKey) {
        when (key.type) {
            VantageRecordTypes.EVENT -> events.findEvent(key.id)?.takeIf { it.event.source.isPortable() }?.let { events.deleteEvent(key.id) }
            VantageRecordTypes.TAG -> tags.delete(key.id)
            VantageRecordTypes.TAGGING -> {
                val (eventId, tagId) = key.id.split(":", limit = 2).takeIf { it.size == 2 } ?: return
                tags.untagEvent(EventTagEntity(eventId, tagId))
            }
            VantageRecordTypes.MARKER -> database.markers().delete(key.id)
            VantageRecordTypes.PERIOD -> database.periods().delete(key.id)
            VantageRecordTypes.CREDENTIAL -> database.credentials().delete(key.id)
        }
    }
}
