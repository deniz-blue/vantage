package blue.deniz.vantage.storage

import blue.deniz.vantage.core.EventDifference
import blue.deniz.vantage.core.differencesFrom
import blue.deniz.vantage.core.REFRESHES_CONTINUE_AFTER_END
import blue.deniz.vantage.core.isDueForRefresh
import blue.deniz.vantage.core.CalendarEntry
import blue.deniz.vantage.core.CalendarMarker
import blue.deniz.vantage.core.CalendarPeriod
import blue.deniz.vantage.core.EventFormat
import blue.deniz.vantage.core.EventResolver
import blue.deniz.vantage.core.EventSource
import blue.deniz.vantage.core.EventSourceType
import blue.deniz.vantage.core.FolioClient
import blue.deniz.vantage.core.canUploadToFolio
import blue.deniz.vantage.core.ResolvedEvent
import kotlin.time.Instant
import blue.deniz.vantage.core.Revision
import blue.deniz.vantage.core.SubscribedCalendar
import blue.deniz.vantage.core.timeRanges
import directory.evnt.OpenEvnt
import directory.evnt.translate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

internal fun StoredEvent.toResolvedEvent() = ResolvedEvent(
    id = event.id,
    source = event.source,
    format = event.format,
    raw = cache?.raw,
    data = cache?.data,
    fetchError = cache?.fetchError,
    parseError = cache?.parseError,
    revision = cache?.revision ?: Revision(),
    updatedAt = cache?.fetchedAt,
)

class RemovedEvent internal constructor(
    internal val stored: StoredEvent,
    internal val timeRanges: List<EventTimeRangeEntity>,
    internal val tagLinks: List<EventTagEntity>,
) {
    val source: EventSource get() = stored.event.source
}

data class HiddenCalendarEvent(val source: EventSource.Calendar, val title: String?, val startsAt: Instant?)

class ForgottenCalendar internal constructor(
    val calendar: SubscribedCalendar,
    internal val events: List<RemovedEvent>,
    internal val hidden: List<HiddenCalendarEventEntity>,
)

class RemovedTag internal constructor(
    val tag: TagEntity,
    internal val eventLinks: List<EventTagEntity>,
    internal val parentLinks: List<TagParentEntity>,
)

class EventStore(
    private val database: VantageDatabase,
    val resolver: EventResolver,
    private val folio: FolioClient,
    private val preferredLanguages: List<String> = listOf("en"),
    private val clock: Clock = Clock.System,
) {
    private val events get() = database.events()
    private val tags get() = database.tags()
    private val hiddenCalendarEvents get() = database.hiddenCalendarEvents()
    private val testEventsShownState = MutableStateFlow(false)

    var testEventsShown: Boolean
        get() = testEventsShownState.value
        set(value) {
            testEventsShownState.value = value
        }

    private fun cacheOf(id: String, event: ResolvedEvent) = EventCacheEntity(
        eventId = id,
        raw = event.raw,
        data = event.data,
        title = event.data?.name?.translate(preferredLanguages),
        fetchError = event.fetchError,
        parseError = event.parseError,
        revision = event.revision,
        fetchedAt = event.updatedAt,
    )

    private fun timeRangesOf(id: String, event: ResolvedEvent) =
        event.data?.timeRanges().orEmpty().map { EventTimeRangeEntity(id, it.start, it.end) }

    @OptIn(ExperimentalUuidApi::class)
    suspend fun add(event: ResolvedEvent): String {
        val id = Uuid.random().toString()
        events.insertEventWithCache(
            EventEntity(id = id, source = event.source, format = event.format, addedAt = clock.now()),
            cacheOf(id, event),
            timeRangesOf(id, event),
        )
        return id
    }

    suspend fun addFromLink(text: String): String? =
        ResolvedEvent.fromLink(text)?.let { add(resolver.resolve(it)) }

    suspend fun previewLink(link: String): LinkPreview {
        val recognized = ResolvedEvent.fromLink(link) ?: return LinkPreview.NotAnEvent("Not a link Vantage can read")
        storedPreviewOf(recognized)?.let { return it }
        val resolved = resolver.resolve(recognized)
        storedPreviewOf(resolved)?.let { return it }
        val problem = resolved.error?.message ?: "No event at this link".takeIf { resolved.data == null }
        return when {
            problem == null -> LinkPreview.New(resolved)
            recognized.format == EventFormat.Unknown -> LinkPreview.NotAnEvent(problem)
            else -> LinkPreview.Broken(resolved, problem)
        }
    }

    suspend fun save(event: ResolvedEvent) {
        val id = requireNotNull(event.id) { "Only a stored event can be saved" }
        events.updateFormat(id, event.format)
        events.saveCache(cacheOf(id, event), timeRangesOf(id, event))
    }

    private fun localEventOf(data: OpenEvnt, isTestEvent: Boolean = false) = ResolvedEvent(
        source = EventSource.Local(isTestEvent),
        format = EventFormat.OpenEvnt,
        raw = data.toJson().toString(),
        data = data,
        updatedAt = clock.now(),
    )

    suspend fun addLocal(data: OpenEvnt): String = add(localEventOf(data))

    suspend fun addTestEvent(data: OpenEvnt): String = add(localEventOf(data, isTestEvent = true))

    suspend fun addToFolio(data: OpenEvnt): String {
        val uploaded = folio.create(data.toJson().toString())
        return add(resolver.resolve(ResolvedEvent(source = uploaded, format = EventFormat.OpenEvnt)))
    }

    suspend fun removeTestEvents() {
        val ids = events.allEvents().filter { (it.event.source as? EventSource.Local)?.isTestEvent == true }.map { it.event.id }
        events.applyEventChanges(EventChanges(added = emptyList(), changed = emptyList(), removedIds = ids))
    }

    @OptIn(ExperimentalUuidApi::class)
    suspend fun syncCalendar(calendar: SubscribedCalendar, entries: List<CalendarEntry>) {
        val stored = storedEventsOf(calendar)
        val hidden = hiddenUidsOf(calendar)
        val now = clock.now()
        val added = mutableListOf<EventWrite>()
        val changed = mutableListOf<CacheWrite>()
        for (entry in entries.distinctBy { it.uid }.filterNot { it.uid in hidden }) {
            val existing = stored[entry.uid]
            val event = ResolvedEvent(
                id = existing?.event?.id,
                source = EventSource.Calendar(calendar, entry.uid),
                format = EventFormat.OpenEvnt,
                raw = entry.event.toJson().toString(),
                data = entry.event,
                updatedAt = now,
            )
            when {
                existing == null -> {
                    val id = Uuid.random().toString()
                    added += EventWrite(
                        EventEntity(id = id, source = event.source, format = event.format, addedAt = now),
                        cacheOf(id, event),
                        timeRangesOf(id, event),
                    )
                }
                existing.cache?.data != entry.event -> changed += CacheWrite(cacheOf(existing.event.id, event), timeRangesOf(existing.event.id, event), replacedAt = now)
            }
        }
        val uids = entries.mapTo(HashSet()) { it.uid }
        events.applyEventChanges(EventChanges(added, changed, stored.filterKeys { it !in uids }.values.map { it.event.id }))
    }

    suspend fun forgetCalendar(calendar: SubscribedCalendar): ForgottenCalendar {
        val stored = storedEventsOf(calendar).values
        val hidden = hiddenCalendarEvents.all().filter { (it.source as? EventSource.Calendar)?.calendar == calendar }
        val forgotten = ForgottenCalendar(
            calendar,
            stored.map { RemovedEvent(it, events.timeRangesOf(it.event.id), tags.tagLinksOf(it.event.id)) },
            hidden,
        )
        events.applyEventChanges(EventChanges(emptyList(), emptyList(), stored.map { it.event.id }))
        hidden.forEach { hiddenCalendarEvents.delete(it.source) }
        return forgotten
    }

    suspend fun restoreCalendar(forgotten: ForgottenCalendar) {
        forgotten.hidden.forEach { hiddenCalendarEvents.upsert(it) }
        forgotten.events.forEach { reinsert(it) }
    }

    fun observeHiddenCalendarEvents(): Flow<List<HiddenCalendarEvent>> =
        hiddenCalendarEvents.observeAll().map { rows -> rows.mapNotNull { row -> (row.source as? EventSource.Calendar)?.let { HiddenCalendarEvent(it, row.title, row.startsAt) } } }

    suspend fun unhideCalendarEvent(source: EventSource.Calendar) = hiddenCalendarEvents.delete(source)

    private suspend fun hiddenUidsOf(calendar: SubscribedCalendar): Set<String> =
        hiddenCalendarEvents.allSources().mapNotNullTo(HashSet()) { source -> (source as? EventSource.Calendar)?.takeIf { it.calendar == calendar }?.uid }

    suspend fun countEventsOf(calendar: SubscribedCalendar): Int = storedEventsOf(calendar).size

    private suspend fun storedEventsOf(calendar: SubscribedCalendar): Map<String, StoredEvent> =
        events.allEvents()
            .mapNotNull { stored -> (stored.event.source as? EventSource.Calendar)?.takeIf { it.calendar == calendar }?.let { it.uid to stored } }
            .toMap()

    suspend fun saveEdited(id: String, data: OpenEvnt) {
        val stored = checkNotNull(events.findEvent(id)) { "No event with id $id" }
        when (val source = stored.event.source) {
            is EventSource.Local -> save(localEventOf(data).copy(id = id))
            is EventSource.Folio -> save(savedOnFolio(id, source, data))
            else -> error("Only an event made on this device can be edited")
        }
    }

    private suspend fun savedOnFolio(id: String, source: EventSource.Folio, data: OpenEvnt): ResolvedEvent {
        val raw = folio.replace(source, data.toJson().toString())
        return resolver.parse(ResolvedEvent(id, source, EventFormat.OpenEvnt, raw, updatedAt = clock.now()))
    }

    suspend fun uploadToFolio(id: String): EventSource.Folio {
        val stored = checkNotNull(events.findEvent(id)) { "No event with id $id" }
        check(stored.event.source.canUploadToFolio) { "Only an event made on this device can be uploaded" }
        val raw = checkNotNull(stored.cache?.raw) { "This event has no data to upload" }
        val shared = folio.create(raw)
        events.updateSource(id, shared)
        refresh(id)
        return shared
    }

    suspend fun refresh(id: String): ResolvedEvent? {
        val stored = events.findEvent(id) ?: return null
        val refreshed = resolver.refresh(stored.toResolvedEvent())
        events.updateFormat(id, refreshed.format)
        events.saveCacheKeepingSeenVersion(cacheOf(id, refreshed), timeRangesOf(id, refreshed), replacedAt = clock.now())
        return refreshed
    }

    suspend fun refreshDueRemoteEvents(): List<ChangedEvent> {
        val now = clock.now()
        return events.idsOfEventsEndingAfter(now - REFRESHES_CONTINUE_AFTER_END).mapNotNull { id -> refreshIfDue(id, now) }
    }

    private suspend fun refreshIfDue(id: String, now: Instant): ChangedEvent? {
        val stored = events.findEvent(id)?.takeIf { it.event.source is EventSource.Remote } ?: return null
        if (!stored.toResolvedEvent().isDueForRefresh(now)) return null
        val before = stored.cache?.data ?: return null
        val refreshed = refresh(id)?.takeIf { it.fetchError == null } ?: return null
        val after = refreshed.data ?: return null
        return after.differencesFrom(before).takeIf { it.isNotEmpty() }?.let { ChangedEvent(id, after, it) }
    }

    suspend fun markChangesSeen(id: String) = events.forgetSeenVersion(id)

    fun observeMarkers(): Flow<List<CalendarMarker>> = database.markers().observeAll().map { rows -> rows.map { it.toMarker() } }

    fun observePeriods(): Flow<List<CalendarPeriod>> = database.periods().observeAll().map { rows -> rows.map { it.toPeriod() } }

    suspend fun findMarker(id: String): CalendarMarker? = database.markers().find(id)?.toMarker()

    suspend fun findPeriod(id: String): CalendarPeriod? = database.periods().find(id)?.toPeriod()

    suspend fun saveMarker(marker: CalendarMarker) =
        database.markers().upsert(MarkerEntity(marker.id, marker.label, marker.at, marker.instant))

    suspend fun savePeriod(period: CalendarPeriod) =
        database.periods().upsert(PeriodEntity(period.id, period.label, period.start, period.end, period.startsAt, period.endsAt))

    suspend fun removeMarker(id: String): CalendarMarker? = findMarker(id)?.also { database.markers().delete(id) }

    suspend fun removePeriod(id: String): CalendarPeriod? = findPeriod(id)?.also { database.periods().delete(id) }

    fun observeUnseenDifferences(): Flow<Map<String, List<EventDifference>>> =
        events.observeSeenAndCurrentEvents().map { rows ->
            rows.mapNotNull { row -> row.differences().takeIf { it.isNotEmpty() }?.let { row.eventId to it } }.toMap()
        }

    fun observeUnseenDifferencesOf(id: String): Flow<List<EventDifference>> =
        events.observeSeenAndCurrentEvent(id).map { it?.differences().orEmpty() }

    private fun SeenAndCurrentEvent.differences(): List<EventDifference> = currentData?.differencesFrom(seenData).orEmpty()

    suspend fun remove(id: String): RemovedEvent? {
        val stored = events.findEvent(id) ?: return null
        val removed = RemovedEvent(stored, events.timeRangesOf(id), tags.tagLinksOf(id))
        events.deleteEvent(id)
        (stored.event.source as? EventSource.Calendar)?.let { source ->
            hiddenCalendarEvents.upsert(HiddenCalendarEventEntity(source, stored.cache?.title, removed.timeRanges.minOfOrNull { it.startsAt }))
        }
        return removed
    }

    suspend fun restore(removed: RemovedEvent) {
        (removed.source as? EventSource.Calendar)?.let { hiddenCalendarEvents.delete(it) }
        reinsert(removed)
    }

    private suspend fun reinsert(removed: RemovedEvent) {
        if (events.findEvent(removed.stored.event.id) != null) return
        events.insertStoredEvent(removed.stored, removed.timeRanges)
        removed.tagLinks.filter { tags.find(it.tagId) != null }.forEach { tags.tagEvent(it) }
    }

    suspend fun removeAll() = events.deleteAllEvents()

    suspend fun find(id: String): ResolvedEvent? = events.findEvent(id)?.toResolvedEvent()

    suspend fun findStoredCopyId(event: ResolvedEvent): String? =
        if (event.source == EventSource.Unknown) null else events.findEventIdBySource(event.source)

    private suspend fun storedPreviewOf(event: ResolvedEvent): LinkPreview.AlreadyStored? {
        val id = findStoredCopyId(event) ?: return null
        return LinkPreview.AlreadyStored(id, find(id) ?: return null)
    }

    fun observe(id: String): Flow<ResolvedEvent?> = events.observeEvent(id).map { it?.toResolvedEvent() }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeList(query: EventListQuery = EventListQuery()): Flow<List<ResolvedEvent>> =
        testEventsShownState.flatMapLatest { shown -> events.observeEvents(query.toRawQuery(includesTestEvents = shown)) }
            .map { rows -> rows.map { it.toResolvedEvent() } }

    fun observeIds(query: EventListQuery): Flow<List<String>> = observeList(query).map { events -> events.mapNotNull { it.id } }

    fun observeTags(): Flow<List<TagEntity>> = tags.observeAll()

    fun observeTagsOf(eventId: String): Flow<List<TagEntity>> = tags.observeTagsOf(eventId)

    fun observeTagsByEvent(): Flow<Map<String, List<TagEntity>>> =
        tags.observeTagsOfAllEvents().map { links -> links.groupBy({ it.eventId }, { it.tag }) }

    fun observeSourceTypes(): Flow<List<EventSourceType>> =
        events.observeSourceTypeNames().map { names -> EventSourceType.entries.filter { it.serialName in names } }

    fun observeTagsWithEventCounts(): Flow<List<TagWithEventCount>> = tags.observeAllWithEventCounts()

    @OptIn(ExperimentalUuidApi::class)
    suspend fun addTag(name: String, color: String? = null): String {
        val id = Uuid.random().toString()
        tags.upsert(TagEntity(id, name.trim(), color, updatedAt = clock.now()))
        return id
    }

    suspend fun renameTag(id: String, name: String) {
        val tag = tags.find(id) ?: return
        tags.upsert(tag.copy(name = name.trim(), updatedAt = clock.now()))
    }

    suspend fun recolorTag(id: String, color: String?) {
        val tag = tags.find(id) ?: return
        tags.upsert(tag.copy(color = color, updatedAt = clock.now()))
    }

    suspend fun deleteTag(id: String): RemovedTag? {
        val tag = tags.find(id) ?: return null
        val removed = RemovedTag(tag, tags.eventLinksOf(id), tags.parentLinksOf(id))
        tags.delete(id)
        return removed
    }

    suspend fun restoreTag(removed: RemovedTag) {
        if (tags.find(removed.tag.id) != null) return
        tags.upsert(removed.tag)
        removed.eventLinks.filter { events.findEvent(it.eventId) != null }.forEach { tags.tagEvent(it) }
        removed.parentLinks.filter { tags.find(it.parentId) != null && tags.find(it.childId) != null }.forEach { tags.addParent(it) }
    }

    suspend fun tagEvent(eventId: String, tagId: String) = tagEvents(listOf(eventId), tagId)

    suspend fun untagEvent(eventId: String, tagId: String) = untagEvents(listOf(eventId), tagId)

    suspend fun tagEvents(eventIds: Collection<String>, tagId: String) = eventIds.forEach { tags.tagEvent(EventTagEntity(it, tagId)) }

    suspend fun untagEvents(eventIds: Collection<String>, tagId: String) = eventIds.forEach { tags.untagEvent(EventTagEntity(it, tagId)) }

    suspend fun exportBackup(): Backup {
        val tagIdsByEvent = tags.observeTagsOfAllEvents().first().groupBy({ it.eventId }, { it.tag.id })
        val parentIdsByTag = tags.observeParents().first().groupBy({ it.childId }, { it.parentId })
        return Backup(
            exportedAt = clock.now(),
            events = events.allEvents().filter { it.event.source.isKeptInBackup() }.map { stored ->
                BackupEvent(
                    id = stored.event.id,
                    source = stored.event.source,
                    format = stored.event.format,
                    addedAt = stored.event.addedAt,
                    document = stored.cache?.raw?.let(::documentOf),
                    tags = tagIdsByEvent[stored.event.id].orEmpty(),
                )
            },
            tags = tags.observeAll().first().map { BackupTag(it.id, it.name, it.color, it.updatedAt, parentIdsByTag[it.id].orEmpty()) },
            markers = observeMarkers().first(),
            periods = observePeriods().first(),
            hiddenCalendarEvents = hiddenCalendarEvents.all().map { BackupHiddenCalendarEvent(it.source, it.title, it.startsAt) },
        )
    }

    suspend fun importBackup(backup: Backup) {
        backup.tags.forEach { tags.upsert(TagEntity(it.id, it.name, it.color, it.updatedAt)) }
        val tagIds = tags.observeAll().first().mapTo(HashSet()) { it.id }
        backup.tags.forEach { tag -> tag.parents.filter { it in tagIds }.forEach { tags.addParent(TagParentEntity(it, tag.id)) } }
        for (event in backup.events.filter { it.source.isKeptInBackup() }) {
            val resolved = resolver.parse(
                ResolvedEvent(
                    id = event.id,
                    source = event.source,
                    format = event.format,
                    raw = event.raw,
                    updatedAt = clock.now().takeIf { event.source !is EventSource.Remote },
                ),
            )
            if (events.findEvent(event.id) == null) {
                events.insertEventWithCache(
                    EventEntity(event.id, event.source, resolved.format, event.addedAt),
                    cacheOf(event.id, resolved),
                    timeRangesOf(event.id, resolved),
                )
            } else {
                events.updateSource(event.id, event.source)
                save(resolved)
            }
            val kept = event.tags.filter { it in tagIds }
            tags.tagLinksOf(event.id).filter { it.tagId !in kept }.forEach { tags.untagEvent(it) }
            kept.forEach { tags.tagEvent(EventTagEntity(event.id, it)) }
        }
        backup.markers.forEach { saveMarker(it) }
        backup.periods.forEach { savePeriod(it) }
        backup.hiddenCalendarEvents.forEach { hiddenCalendarEvents.upsert(HiddenCalendarEventEntity(it.source, it.title, it.startsAt)) }
    }
}

private fun MarkerEntity.toMarker() = CalendarMarker(id, label, at)

private fun PeriodEntity.toPeriod() = CalendarPeriod(id, label, start, end)
