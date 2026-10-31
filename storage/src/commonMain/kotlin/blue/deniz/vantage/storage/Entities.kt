package blue.deniz.vantage.storage

import androidx.room3.Embedded
import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey
import androidx.room3.Relation
import blue.deniz.vantage.core.EventDifference
import blue.deniz.vantage.core.EventError
import blue.deniz.vantage.core.EventFormat
import blue.deniz.vantage.core.EventSource
import blue.deniz.vantage.core.Revision
import directory.evnt.OpenEvnt
import directory.evnt.PartialDate
import kotlin.time.Instant

@Entity(tableName = "events")
data class EventEntity(
    @PrimaryKey val id: String,
    val source: EventSource,
    val format: EventFormat,
    val addedAt: Instant,
)

@Entity(
    tableName = "event_cache",
    foreignKeys = [ForeignKey(EventEntity::class, ["id"], ["eventId"], onDelete = ForeignKey.CASCADE)],
)
data class EventCacheEntity(
    @PrimaryKey val eventId: String,
    val raw: String?,
    val data: OpenEvnt?,
    val title: String?,
    val fetchError: EventError?,
    val parseError: EventError?,
    val revision: Revision,
    val fetchedAt: Instant?,
)

@Entity(
    tableName = "event_time_ranges",
    primaryKeys = ["eventId", "startsAt", "endsAt"],
    foreignKeys = [ForeignKey(EventEntity::class, ["id"], ["eventId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("startsAt"), Index("endsAt")],
)
data class EventTimeRangeEntity(val eventId: String, val startsAt: Instant, val endsAt: Instant)

@Entity(
    tableName = "seen_event_versions",
    foreignKeys = [ForeignKey(EventEntity::class, ["id"], ["eventId"], onDelete = ForeignKey.CASCADE)],
)
data class SeenEventVersionEntity(@PrimaryKey val eventId: String, val data: OpenEvnt, val replacedAt: Instant)

data class ChangedEvent(val id: String, val data: OpenEvnt, val differences: List<EventDifference>)

data class SeenAndCurrentEvent(val eventId: String, val seenData: OpenEvnt, val currentData: OpenEvnt?)

@Entity(tableName = "tags")
data class TagEntity(
    @PrimaryKey val id: String,
    val name: String,
    val color: String?,
    val updatedAt: Instant,
)

@Entity(
    tableName = "tag_parents",
    primaryKeys = ["parentId", "childId"],
    foreignKeys = [
        ForeignKey(TagEntity::class, ["id"], ["parentId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(TagEntity::class, ["id"], ["childId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("childId")],
)
data class TagParentEntity(val parentId: String, val childId: String)

data class TagWithEventCount(@Embedded val tag: TagEntity, val eventCount: Int)

@Entity(
    tableName = "event_tags",
    primaryKeys = ["eventId", "tagId"],
    foreignKeys = [
        ForeignKey(EventEntity::class, ["id"], ["eventId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(TagEntity::class, ["id"], ["tagId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("tagId")],
)
data class EventTagEntity(val eventId: String, val tagId: String)

@Entity(tableName = "credentials")
data class CredentialEntity(
    @PrimaryKey val id: String,
    val url: String,
    val username: String,
    val password: String,
    val updatedAt: Instant,
)

data class StoredEvent(
    @Embedded val event: EventEntity,
    @Relation(parentColumns = ["id"], entityColumns = ["eventId"]) val cache: EventCacheEntity?,
)

data class EventWrite(val event: EventEntity, val cache: EventCacheEntity, val timeRanges: List<EventTimeRangeEntity>)

data class CacheWrite(val cache: EventCacheEntity, val timeRanges: List<EventTimeRangeEntity>, val replacedAt: Instant)

data class TagOfEvent(val eventId: String, @Embedded val tag: TagEntity)

data class EventChanges(val added: List<EventWrite>, val changed: List<CacheWrite>, val removedIds: List<String>)

@Entity(tableName = "markers", indices = [Index("atInstant")])
data class MarkerEntity(@PrimaryKey val id: String, val label: String, val at: PartialDate, val atInstant: Instant)

@Entity(tableName = "hidden_calendar_events")
data class HiddenCalendarEventEntity(@PrimaryKey val source: EventSource, val title: String?, val startsAt: Instant?)

@Entity(tableName = "periods", indices = [Index("startsAt"), Index("endsAt")])
data class PeriodEntity(@PrimaryKey val id: String, val label: String, val start: PartialDate, val end: PartialDate, val startsAt: Instant, val endsAt: Instant)
