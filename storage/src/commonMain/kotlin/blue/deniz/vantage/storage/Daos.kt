package blue.deniz.vantage.storage

import androidx.room3.Dao
import androidx.room3.Delete
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.RawQuery
import androidx.room3.RoomRawQuery
import androidx.room3.Transaction
import androidx.room3.Upsert
import blue.deniz.vantage.core.EventFormat
import blue.deniz.vantage.core.EventSource
import kotlinx.coroutines.flow.Flow
import kotlin.time.Instant

@Dao
abstract class EventDao {
    @Insert
    abstract suspend fun insertEvent(event: EventEntity)

    @Query("UPDATE events SET format = :format WHERE id = :id")
    abstract suspend fun updateFormat(id: String, format: EventFormat)

    @Query("UPDATE events SET source = :source WHERE id = :id")
    abstract suspend fun updateSource(id: String, source: EventSource)

    @Upsert
    abstract suspend fun upsertCache(cache: EventCacheEntity)

    @Query("DELETE FROM event_time_ranges WHERE eventId = :eventId")
    abstract suspend fun deleteTimeRanges(eventId: String)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertTimeRanges(ranges: List<EventTimeRangeEntity>)

    @Query("DELETE FROM events WHERE id = :id")
    abstract suspend fun deleteEvent(id: String)

    @Query("SELECT * FROM event_time_ranges WHERE eventId = :eventId")
    abstract suspend fun timeRangesOf(eventId: String): List<EventTimeRangeEntity>

    @Query("DELETE FROM events")
    abstract suspend fun deleteAllEvents()

    @Query("SELECT DISTINCT json_extract(source, '$.type') FROM events")
    abstract fun observeSourceTypeNames(): Flow<List<String>>

    @Transaction
    @Query("SELECT * FROM events WHERE id = :id")
    abstract suspend fun findEvent(id: String): StoredEvent?

    @Query("SELECT id FROM events WHERE source = :source LIMIT 1")
    abstract suspend fun findEventIdBySource(source: EventSource): String?

    @Transaction
    @Query("SELECT * FROM events WHERE id = :id")
    abstract fun observeEvent(id: String): Flow<StoredEvent?>

    @Transaction
    @RawQuery(observedEntities = [EventEntity::class, EventCacheEntity::class, EventTimeRangeEntity::class, EventTagEntity::class])
    abstract fun observeEvents(query: RoomRawQuery): Flow<List<StoredEvent>>

    @Transaction
    open suspend fun saveCache(cache: EventCacheEntity, ranges: List<EventTimeRangeEntity>) {
        upsertCache(cache)
        deleteTimeRanges(cache.eventId)
        insertTimeRanges(ranges)
    }

    @Query("SELECT DISTINCT eventId FROM event_time_ranges WHERE endsAt > :instant")
    abstract suspend fun idsOfEventsEndingAfter(instant: Instant): List<String>

    @Query("SELECT * FROM event_cache WHERE eventId = :eventId")
    abstract suspend fun findCache(eventId: String): EventCacheEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun keepSeenVersion(version: SeenEventVersionEntity)

    @Query("DELETE FROM seen_event_versions WHERE eventId = :eventId")
    abstract suspend fun forgetSeenVersion(eventId: String)

    @Query("SELECT v.eventId AS eventId, v.data AS seenData, c.data AS currentData FROM seen_event_versions v JOIN event_cache c ON c.eventId = v.eventId")
    abstract fun observeSeenAndCurrentEvents(): Flow<List<SeenAndCurrentEvent>>

    @Query("SELECT v.eventId AS eventId, v.data AS seenData, c.data AS currentData FROM seen_event_versions v JOIN event_cache c ON c.eventId = v.eventId WHERE v.eventId = :eventId")
    abstract fun observeSeenAndCurrentEvent(eventId: String): Flow<SeenAndCurrentEvent?>

    @Transaction
    open suspend fun saveCacheKeepingSeenVersion(cache: EventCacheEntity, ranges: List<EventTimeRangeEntity>, replacedAt: Instant) {
        val previous = findCache(cache.eventId)?.data
        if (previous != null && cache.data != null && previous != cache.data) keepSeenVersion(SeenEventVersionEntity(cache.eventId, previous, replacedAt))
        saveCache(cache, ranges)
    }

    @Transaction
    open suspend fun insertStoredEvent(stored: StoredEvent, ranges: List<EventTimeRangeEntity>) {
        insertEvent(stored.event)
        stored.cache?.let { upsertCache(it) }
        insertTimeRanges(ranges)
    }

    @Transaction
    open suspend fun insertEventWithCache(event: EventEntity, cache: EventCacheEntity, ranges: List<EventTimeRangeEntity>) {
        insertEvent(event)
        saveCache(cache, ranges)
    }

    @Transaction
    @Query("SELECT * FROM events")
    abstract suspend fun allEvents(): List<StoredEvent>

    @Transaction
    open suspend fun applyEventChanges(changes: EventChanges) {
        for (added in changes.added) insertEventWithCache(added.event, added.cache, added.timeRanges)
        for (changed in changes.changed) saveCacheKeepingSeenVersion(changed.cache, changed.timeRanges, changed.replacedAt)
        for (id in changes.removedIds) deleteEvent(id)
    }
}

@Dao
interface TagDao {
    @Query("SELECT * FROM tags ORDER BY name COLLATE NOCASE")
    fun observeAll(): Flow<List<TagEntity>>

    @Query(
        "SELECT tags.*, COUNT(event_tags.eventId) AS eventCount FROM tags LEFT JOIN event_tags ON event_tags.tagId = tags.id " +
            "GROUP BY tags.id ORDER BY tags.name COLLATE NOCASE",
    )
    fun observeAllWithEventCounts(): Flow<List<TagWithEventCount>>

    @Query("SELECT * FROM tags WHERE id = :id")
    suspend fun find(id: String): TagEntity?

    @Upsert
    suspend fun upsert(tag: TagEntity)

    @Query("DELETE FROM tags WHERE id = :id")
    suspend fun delete(id: String)

    @Query(
        "SELECT tags.* FROM tags JOIN event_tags ON event_tags.tagId = tags.id " +
            "WHERE event_tags.eventId = :eventId ORDER BY tags.name COLLATE NOCASE",
    )
    fun observeTagsOf(eventId: String): Flow<List<TagEntity>>

    @Query(
        "SELECT event_tags.eventId AS eventId, tags.* FROM event_tags JOIN tags ON tags.id = event_tags.tagId " +
            "ORDER BY tags.name COLLATE NOCASE",
    )
    fun observeTagsOfAllEvents(): Flow<List<TagOfEvent>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun tagEvent(link: EventTagEntity)

    @Query("SELECT * FROM event_tags WHERE eventId = :eventId")
    suspend fun tagLinksOf(eventId: String): List<EventTagEntity>

    @Query("SELECT * FROM event_tags WHERE tagId = :tagId")
    suspend fun eventLinksOf(tagId: String): List<EventTagEntity>

    @Delete
    suspend fun untagEvent(link: EventTagEntity)

    @Query("SELECT * FROM tag_parents WHERE parentId = :tagId OR childId = :tagId")
    suspend fun parentLinksOf(tagId: String): List<TagParentEntity>

    @Query("SELECT * FROM tag_parents")
    fun observeParents(): Flow<List<TagParentEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addParent(link: TagParentEntity)

    @Delete
    suspend fun removeParent(link: TagParentEntity)
}

@Dao
interface CredentialDao {
    @Query("SELECT * FROM credentials ORDER BY url")
    fun observeAll(): Flow<List<CredentialEntity>>

    @Query("SELECT * FROM credentials WHERE substr(:url, 1, length(url)) = url ORDER BY length(url) DESC")
    suspend fun matching(url: String): List<CredentialEntity>

    @Upsert
    suspend fun upsert(credential: CredentialEntity)

    @Query("DELETE FROM credentials WHERE id = :id")
    suspend fun delete(id: String)
}

@Dao
interface MarkerDao {
    @Query("SELECT * FROM markers ORDER BY atInstant")
    fun observeAll(): Flow<List<MarkerEntity>>

    @Query("SELECT * FROM markers WHERE id = :id")
    suspend fun find(id: String): MarkerEntity?

    @Upsert
    suspend fun upsert(marker: MarkerEntity)

    @Query("DELETE FROM markers WHERE id = :id")
    suspend fun delete(id: String)
}

@Dao
interface HiddenCalendarEventDao {
    @Query("SELECT source FROM hidden_calendar_events")
    suspend fun allSources(): List<EventSource>

    @Query("SELECT * FROM hidden_calendar_events")
    suspend fun all(): List<HiddenCalendarEventEntity>

    @Query("SELECT * FROM hidden_calendar_events ORDER BY startsAt")
    fun observeAll(): Flow<List<HiddenCalendarEventEntity>>

    @Upsert
    suspend fun upsert(hidden: HiddenCalendarEventEntity)

    @Query("DELETE FROM hidden_calendar_events WHERE source = :source")
    suspend fun delete(source: EventSource)
}

@Dao
interface PeriodDao {
    @Query("SELECT * FROM periods ORDER BY startsAt")
    fun observeAll(): Flow<List<PeriodEntity>>

    @Query("SELECT * FROM periods WHERE id = :id")
    suspend fun find(id: String): PeriodEntity?

    @Upsert
    suspend fun upsert(period: PeriodEntity)

    @Query("DELETE FROM periods WHERE id = :id")
    suspend fun delete(id: String)
}
