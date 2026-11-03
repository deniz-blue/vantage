package blue.deniz.vantage.storage

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import blue.deniz.vantage.core.CalendarEntry
import blue.deniz.vantage.core.CalendarMarker
import blue.deniz.vantage.core.CalendarPeriod
import blue.deniz.vantage.core.EventDifference
import blue.deniz.vantage.core.EventError
import blue.deniz.vantage.core.EventFetcher
import blue.deniz.vantage.core.EventFormat
import blue.deniz.vantage.core.EventResolver
import blue.deniz.vantage.core.EventSource
import blue.deniz.vantage.core.EventSourceType
import blue.deniz.vantage.core.FolioClient
import blue.deniz.vantage.core.GeoArea
import blue.deniz.vantage.core.ResolvedEvent
import blue.deniz.vantage.core.SubscribedCalendar
import directory.evnt.Component
import directory.evnt.OpenEvnt
import directory.evnt.PartialDate
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

private fun openEvnt(name: String, vararg starts: String) =
    """{"v":"0.2","id":"urn:example:${name.filter(Char::isLetterOrDigit)}","name":{"en":"$name"},"occurrences":[${starts.joinToString(",") { """{"start":"$it"}""" }}]}"""

private fun openEvntAt(name: String, latitude: Double, longitude: Double) =
    """{"v":"0.2","id":"urn:example:$name","name":{"en":"$name"},"venue":{"components":[{"${'$'}type":"${Component.Coordinates.TYPE_ID}","latitude":$latitude,"longitude":$longitude}]}}"""

private object FixedClock : Clock {
    var now = Instant.parse("2026-10-21T00:00:00Z")
    override fun now() = now
}

class EventStoreTest {
    private val pages = mutableMapOf<String, String>()

    private val client = HttpClient(MockEngine { request ->
        pages[request.url.toString()]?.let { respond(it) } ?: respond("gone", HttpStatusCode.NotFound)
    })

    private val database = Room.inMemoryDatabaseBuilder<VantageDatabase>()
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()

    private val store = EventStore(database, EventResolver(EventFetcher(client), clock = FixedClock), FolioClient(client), clock = FixedClock)

    @AfterTest
    fun close() = database.close()

    private suspend fun local(name: String, vararg starts: String) =
        store.add(EventResolver(EventFetcher(client)).parse(ResolvedEvent(source = EventSource.Local(), format = EventFormat.OpenEvnt, raw = openEvnt(name, *starts))))

    private suspend fun placed(name: String, latitude: Double, longitude: Double) =
        store.add(EventResolver(EventFetcher(client)).parse(ResolvedEvent(source = EventSource.Local(), format = EventFormat.OpenEvnt, raw = openEvntAt(name, latitude, longitude))))

    private suspend fun names(query: EventListQuery) = store.observeList(query).first().map { it.data?.name?.get("en") }

    @Test
    fun addsAnEventFromALinkAndReadsItBack() = runTest {
        pages["https://example.com/picnic.json"] = openEvnt("Picnic", "2026-07-10[Europe/Vilnius]")
        val id = assertNotNull(store.addFromLink("https://example.com/picnic.json"))
        val stored = assertNotNull(store.find(id))
        assertEquals(id, stored.id)
        assertEquals(EventSource.Http("https://example.com/picnic.json"), stored.source)
        assertEquals(mapOf("en" to "Picnic"), stored.data?.name)
        assertEquals(FixedClock.now, stored.updatedAt)
        assertEquals(id, store.findStoredCopyId(ResolvedEvent(source = EventSource.Http("https://example.com/picnic.json"))))
        assertEquals(id, store.findStoredCopyId(ResolvedEvent(source = EventSource.Http("https://example.com/picnic.json"), format = EventFormat.ICalendar)))
        assertNull(store.findStoredCopyId(ResolvedEvent(source = EventSource.Http("https://example.com/other.json"))))
        assertNull(store.findStoredCopyId(ResolvedEvent()))
    }

    @Test
    fun previewsALinkBeforeAndAfterItIsAdded() = runTest {
        pages["https://example.com/picnic.json"] = openEvnt("Picnic", "2026-07-10[Europe/Vilnius]")
        val preview = assertIs<LinkPreview.New>(store.previewLink("https://example.com/picnic.json"))
        assertEquals(mapOf("en" to "Picnic"), preview.event.data?.name)
        assertEquals(emptyList(), names(EventListQuery()))

        val id = store.add(preview.event)
        assertEquals(id, assertIs<LinkPreview.AlreadyStored>(store.previewLink(" https://example.com/picnic.json ")).id)
    }

    @Test
    fun aLinkWithoutAReadableEventIsNotAnEvent() = runTest {
        assertIs<LinkPreview.NotAnEvent>(store.previewLink("Picnic at the park"))
        pages["https://example.com/page"] = "<html><body>Hello</body></html>"
        assertIs<LinkPreview.NotAnEvent>(store.previewLink("https://example.com/page"))
        assertIs<LinkPreview.NotAnEvent>(store.previewLink("https://example.com/missing-page"))
        assertEquals(emptyList(), names(EventListQuery()))
    }

    @Test
    fun aBrokenEventLinkCanBeAddedAndFixedLater() = runTest {
        val broken = assertIs<LinkPreview.Broken>(store.previewLink("https://example.com/later.json"))
        assertNull(broken.event.data)

        val id = store.add(broken.event)
        assertEquals(id, assertIs<LinkPreview.AlreadyStored>(store.previewLink("https://example.com/later.json")).id)
        assertNotNull(store.find(id)?.error)

        pages["https://example.com/later.json"] = openEvnt("Picnic", "2026-07-10[Europe/Vilnius]")
        val refreshed = assertNotNull(store.refresh(id))
        assertNull(refreshed.error)
        assertEquals(mapOf("en" to "Picnic"), store.find(id)?.data?.name)
    }

    @Test
    fun hidesTestEventsUntilShownAndRemovesThemTogether() = runTest {
        val made = OpenEvnt.parse(Json.parseToJsonElement(openEvnt("Real", "2026-11-01[Europe/Vilnius]")))
        store.addLocal(made)
        val testId = store.addTestEvent(made.copy(name = mapOf("en" to "Fake")))
        assertEquals(EventSource.Local(isTestEvent = true), store.find(testId)?.source)
        assertEquals(listOf("Real"), names(EventListQuery()))
        assertEquals(listOf("Fake"), names(EventListQuery(onlyTestEvents = true)))
        store.testEventsShown = true
        assertEquals(setOf("Real", "Fake"), names(EventListQuery()).toSet())
        store.removeTestEvents()
        assertEquals(listOf("Real"), names(EventListQuery()))
    }

    @Test
    fun savesAnEventMadeOnThisDevice() = runTest {
        val made = OpenEvnt.parse(Json.parseToJsonElement(openEvnt("Draft", "2026-11-01[Europe/Vilnius]")))
        val id = store.addLocal(made)
        store.saveEdited(id, made.copy(name = mapOf("en" to "Final")))
        val stored = assertNotNull(store.find(id))
        assertEquals(EventSource.Local(), stored.source)
        assertEquals("Final", stored.data?.name?.get("en"))
        assertEquals(stored.data, OpenEvnt.parse(Json.parseToJsonElement(assertNotNull(stored.raw))))

        pages["https://example.com/picnic.json"] = openEvnt("Picnic", "2026-07-10[Europe/Vilnius]")
        val remote = assertNotNull(store.addFromLink("https://example.com/picnic.json"))
        assertFailsWith<IllegalStateException> { store.saveEdited(remote, made) }
    }

    @Test
    fun keepsTheSeenVersionUntilItsChangesAreSeen() = runTest {
        fun occurrenceOn(start: String) = OpenEvnt.parse(Json.parseToJsonElement(openEvnt("Picnic", start))).occurrences!!.single()
        pages["https://example.com/picnic.json"] = openEvnt("Picnic", "2026-07-10[Europe/Vilnius]")
        val id = assertNotNull(store.addFromLink("https://example.com/picnic.json"))
        store.refresh(id)
        assertEquals(emptyMap(), store.observeUnseenDifferences().first())

        pages["https://example.com/picnic.json"] = openEvnt("Picnic", "2026-07-11[Europe/Vilnius]")
        store.refresh(id)
        pages["https://example.com/picnic.json"] = openEvnt("Picnic", "2026-07-12[Europe/Vilnius]")
        store.refresh(id)
        val moved = EventDifference.OccurrenceMoved(occurrenceOn("2026-07-10[Europe/Vilnius]"), occurrenceOn("2026-07-12[Europe/Vilnius]"))
        assertEquals(listOf(moved), store.observeUnseenDifferencesOf(id).first())
        assertEquals(mapOf(id to listOf(moved)), store.observeUnseenDifferences().first())

        store.markChangesSeen(id)
        assertEquals(emptyList(), store.observeUnseenDifferencesOf(id).first())
    }

    @Test
    fun checksOnlyRemoteEventsThatAreNotLongOver() = runTest {
        pages["https://example.com/picnic.json"] = openEvnt("Picnic", "2026-11-10[Europe/Vilnius]")
        pages["https://example.com/gala.json"] = openEvnt("Gala", "2026-07-10[Europe/Vilnius]")
        val picnic = assertNotNull(store.addFromLink("https://example.com/picnic.json"))
        assertNotNull(store.addFromLink("https://example.com/gala.json"))
        local("Own", "2026-11-12[Europe/Vilnius]")
        assertEquals(emptyList(), store.refreshDueRemoteEvents())

        pages["https://example.com/picnic.json"] = openEvnt("Picnic", "2026-11-11[Europe/Vilnius]")
        pages["https://example.com/gala.json"] = openEvnt("Gala", "2026-07-11[Europe/Vilnius]")
        assertEquals(emptyList(), store.refreshDueRemoteEvents())
        val addedAt = FixedClock.now
        try {
            FixedClock.now = addedAt + 1.days
            val changed = store.refreshDueRemoteEvents().single()
            assertEquals(picnic, changed.id)
            assertIs<EventDifference.OccurrenceMoved>(changed.differences.single())
            assertEquals(emptyList(), store.refreshDueRemoteEvents())
        } finally {
            FixedClock.now = addedAt
        }
    }

    @Test
    fun checksAnEventThatEndedThisMonthAgainDaysLater() = runTest {
        pages["https://example.com/meetup.json"] = openEvnt("Meetup", "2026-10-19[Europe/Vilnius]")
        val meetup = assertNotNull(store.addFromLink("https://example.com/meetup.json"))
        pages["https://example.com/meetup.json"] = openEvnt("Meetup", "2026-10-20[Europe/Vilnius]")
        assertEquals(emptyList(), store.refreshDueRemoteEvents())
        val addedAt = FixedClock.now
        try {
            FixedClock.now = addedAt + 2.days
            assertEquals(meetup, store.refreshDueRemoteEvents().single().id)
        } finally {
            FixedClock.now = addedAt
        }
    }

    @Test
    fun markersAndPeriodsAreSavedInOrderAndRemoved() = runTest {
        val exams = CalendarPeriod("exams", "Exam session", PartialDate.parse("2027-01-04[Europe/Vilnius]"), PartialDate.parse("2027-01-24[Europe/Vilnius]"))
        val upload = CalendarMarker("upload", "LW3 upload cut-off", PartialDate.parse("2026-11-23[Europe/Vilnius]"))
        val quiz = CalendarMarker("quiz", "Quiz closes", PartialDate.parse("2026-10-30T23:59[Europe/Vilnius]"))
        store.saveMarker(upload)
        store.saveMarker(quiz)
        store.savePeriod(exams)
        assertEquals(listOf(quiz, upload), store.observeMarkers().first())
        assertEquals(listOf(exams), store.observePeriods().first())
        assertEquals(upload, store.removeMarker("upload"))
        assertEquals(listOf(quiz), store.observeMarkers().first())
    }

    @Test
    fun aFailedRefreshKeepsWhatWasStored() = runTest {
        pages["https://example.com/picnic.json"] = openEvnt("Picnic", "2026-07-10[Europe/Vilnius]")
        val id = assertNotNull(store.addFromLink("https://example.com/picnic.json"))
        val before = store.find(id)
        pages.clear()
        store.refresh(id)
        val stored = assertNotNull(store.find(id))
        assertEquals(before?.data, stored.data)
        assertEquals(EventError.Kind.Fetch, stored.fetchError?.kind)
        assertEquals(listOf("Picnic"), names(EventListQuery(hasError = true)))
        assertEquals(emptyList(), names(EventListQuery(hasError = false)))
    }

    @Test
    fun filtersByTimeWindowAndOrdersByTheNextStart() = runTest {
        local("Past", "2026-01-05[UTC]")
        local("Weekly", "2026-10-01[UTC]", "2026-10-29[UTC]")
        local("Soon", "2026-10-25[UTC]")
        local("Undated")
        val upcoming = EventListQuery(from = Instant.parse("2026-10-21T00:00:00Z"), order = EventListQuery.Order.Start)
        assertEquals(listOf("Soon", "Weekly", "Undated"), names(upcoming))
        assertEquals(listOf("Past", "Weekly"), names(EventListQuery(until = Instant.parse("2026-10-21T00:00:00Z"), order = EventListQuery.Order.Start)))
        assertEquals(listOf("Soon"), names(upcoming.copy(until = Instant.parse("2026-10-26T00:00:00Z"))))
        assertEquals(listOf("Past", "Weekly", "Soon", "Undated"), names(EventListQuery(order = EventListQuery.Order.Start)))
        assertEquals(listOf("Undated"), names(EventListQuery(order = EventListQuery.Order.Start, offset = 3)))
        assertEquals(listOf("Past", "Soon"), names(EventListQuery(order = EventListQuery.Order.Name, limit = 2)))
    }

    @Test
    fun listsEndedEventsNewestFirst() = runTest {
        local("Older", "2026-01-05[UTC]")
        local("Weekly", "2026-10-01[UTC]", "2026-10-29[UTC]")
        local("Recent", "2026-10-10[UTC]")
        local("Undated")
        val ended = EventListQuery(endedBy = Instant.parse("2026-10-21T00:00:00Z"), order = EventListQuery.Order.StartDescending)
        assertEquals(listOf("Recent", "Older"), names(ended))
    }

    @Test
    fun tagsAndUntagsEvents() = runTest {
        val picnic = local("Picnic in the park")
        val uni = store.addTag("  Uni ")
        val concerts = store.addTag("Concerts")
        store.tagEvent(picnic, uni)
        store.tagEvent(picnic, uni)
        store.tagEvent(picnic, concerts)
        assertEquals(listOf("Concerts", "Uni"), store.observeTagsOf(picnic).first().map { it.name })

        store.untagEvent(picnic, concerts)
        assertEquals(listOf("Uni"), store.observeTagsOf(picnic).first().map { it.name })
        assertEquals(listOf("Concerts", "Uni"), store.observeTags().first().map { it.name })
    }

    @Test
    fun tagsSeveralEventsAtOnce() = runTest {
        val picnic = local("Picnic in the park")
        val lecture = local("Lecture")
        val concert = local("Concert")
        val uni = store.addTag("Uni")
        store.tagEvent(picnic, uni)

        store.tagEvents(listOf(picnic, lecture), uni)
        assertEquals(listOf("Lecture", "Picnic in the park"), names(EventListQuery(anyTagIds = setOf(uni), order = EventListQuery.Order.Name)))

        store.untagEvents(listOf(picnic, concert), uni)
        assertEquals(listOf("Lecture"), names(EventListQuery(anyTagIds = setOf(uni))))
    }

    @Test
    fun listsOnlyTheGivenIds() = runTest {
        val picnic = local("Picnic in the park")
        local("Lecture")
        val concert = local("Concert")

        assertEquals(listOf("Concert", "Picnic in the park"), names(EventListQuery(ids = setOf(picnic, concert), order = EventListQuery.Order.Name)))
        assertEquals(emptyList(), names(EventListQuery(ids = emptySet())))
        assertEquals(listOf(concert), store.observeIds(EventListQuery(ids = setOf(concert, "gone"))).first())
    }

    @Test
    fun renamesAndDeletesTags() = runTest {
        val picnic = local("Picnic in the park")
        val uni = store.addTag("Uni")
        store.tagEvent(picnic, uni)

        store.renameTag(uni, " Lectures ")
        assertEquals(listOf("Lectures"), store.observeTagsOf(picnic).first().map { it.name })

        store.deleteTag(uni)
        assertEquals(emptyList(), store.observeTagsOf(picnic).first())
        assertEquals(emptyList(), store.observeTags().first())
        assertEquals(listOf("Picnic in the park"), names(EventListQuery()))
    }

    @Test
    fun aDeletedTagComesBackWithItsEvents() = runTest {
        val picnic = local("Picnic in the park")
        val lecture = local("Lecture")
        val uni = store.addTag("Uni", color = "#a97dff")
        store.tagEvent(picnic, uni)
        store.tagEvent(lecture, uni)
        assertEquals(listOf(2), store.observeTagsWithEventCounts().first().map { it.eventCount })

        val removed = assertNotNull(store.deleteTag(uni))
        store.remove(lecture)
        store.restoreTag(removed)

        assertEquals(listOf("Uni"), store.observeTagsOf(picnic).first().map { it.name })
        assertEquals(listOf("#a97dff" to 1), store.observeTagsWithEventCounts().first().map { it.tag.color to it.eventCount })
    }

    @Test
    fun aRemovedEventComesBackAsItWas() = runTest {
        val picnic = local("Picnic in the park", "2026-11-01[Europe/Vilnius]")
        val uni = store.addTag("Uni")
        val gone = store.addTag("Gone")
        store.tagEvent(picnic, uni)
        store.tagEvent(picnic, gone)
        val before = store.find(picnic)

        val removed = assertNotNull(store.remove(picnic))
        assertNull(store.find(picnic))
        store.deleteTag(gone)
        store.restore(removed)

        assertEquals(before, store.find(picnic))
        assertEquals(listOf("Uni"), store.observeTagsOf(picnic).first().map { it.name })
        assertEquals(listOf("Picnic in the park"), names(EventListQuery(from = Instant.parse("2026-10-31T12:00:00Z"))))
        store.restore(removed)
        assertEquals(listOf("Picnic in the park"), names(EventListQuery()))
    }

    @Test
    fun searchFindsAnEventByItsPlace() = runTest {
        local("Picnic in the park")
        store.add(
            EventResolver(EventFetcher(client)).parse(
                ResolvedEvent(
                    source = EventSource.Local(),
                    format = EventFormat.OpenEvnt,
                    raw = """{"v":"0.2","id":"urn:example:concert","name":{"en":"Concert"},"venue":{"name":{"lt":"Rotušė","en":"Town Hall"},"address":"Didžioji g. 31, Vilnius"}}""",
                ),
            ),
        )
        assertEquals(listOf("Concert"), names(EventListQuery(search = "vilnius")))
        assertEquals(listOf("Concert"), names(EventListQuery(search = "town hall")))
        assertEquals(listOf("Concert"), names(EventListQuery(search = "Rotušė")))
        assertEquals(listOf("Picnic in the park"), names(EventListQuery(search = "park")))
    }

    @Test
    fun searchesNamesAndFiltersByTags() = runTest {
        val picnic = local("Picnic in the park")
        val meetup = local("Open Evnt meetup")
        local("100% off_sale")
        val tags = database.tags()
        tags.upsert(TagEntity("outdoors", "Outdoors", null, FixedClock.now))
        tags.upsert(TagEntity("social", "Social", "#a97dff", FixedClock.now))
        tags.tagEvent(EventTagEntity(picnic, "outdoors"))
        tags.tagEvent(EventTagEntity(picnic, "social"))
        tags.tagEvent(EventTagEntity(meetup, "social"))

        assertEquals(listOf("Picnic in the park"), names(EventListQuery(search = "PARK")))
        assertEquals(listOf("100% off_sale"), names(EventListQuery(search = "0% off_")))
        assertEquals(emptyList(), names(EventListQuery(search = "_x")))
        assertEquals(listOf("Picnic in the park"), names(EventListQuery(allTagIds = setOf("outdoors", "social"))))
        assertEquals(setOf("Picnic in the park", "Open Evnt meetup"), names(EventListQuery(anyTagIds = setOf("outdoors", "social"))).toSet())
        assertEquals(listOf("Outdoors", "Social"), tags.observeTagsOf(picnic).first().map { it.name })
        assertEquals(setOf("Open Evnt meetup", "100% off_sale"), names(EventListQuery(excludedTagIds = setOf("outdoors"))).toSet())
        assertEquals(listOf("100% off_sale"), names(EventListQuery(excludedTagIds = setOf("social"))))
        assertEquals(
            mapOf(picnic to listOf("Outdoors", "Social"), meetup to listOf("Social")),
            store.observeTagsByEvent().first().mapValues { (_, tags) -> tags.map { it.name } },
        )

        store.remove(picnic)
        assertNull(store.find(picnic))
        assertEquals(listOf("Open Evnt meetup"), names(EventListQuery(anyTagIds = setOf("outdoors", "social"))))

        store.removeAll()
        assertEquals(emptyList(), names(EventListQuery()))
        assertEquals(emptyList(), names(EventListQuery(anyTagIds = setOf("social"))))
        assertEquals(listOf("Outdoors", "Social"), tags.observeAll().first().map { it.name })
    }

    @Test
    fun filtersBySourceTypeAndFormat() = runTest {
        pages["https://example.com/a.json"] = openEvnt("Remote", "2026-07-10[UTC]")
        store.addFromLink("https://example.com/a.json")
        local("Here")
        assertEquals(listOf("Remote"), names(EventListQuery(sourceTypes = setOf(EventSourceType.Http))))
        assertEquals(listOf("Here"), names(EventListQuery(sourceTypes = setOf(EventSourceType.Local))))
        assertEquals(setOf(EventSourceType.Http, EventSourceType.Local), store.observeSourceTypes().first().toSet())
        assertEquals(2, names(EventListQuery(format = EventFormat.OpenEvnt)).size)
        assertEquals(emptyList(), names(EventListQuery(format = EventFormat.ICalendar)))
    }

    @Test
    fun findsTheCredentialWithTheLongestMatchingPrefix() = runTest {
        val credentials = database.credentials()
        credentials.upsert(CredentialEntity("1", "https://dav.example/", "a", "x", FixedClock.now))
        credentials.upsert(CredentialEntity("2", "https://dav.example/team/", "b", "y", FixedClock.now))
        credentials.upsert(CredentialEntity("3", "https://other.example/", "c", "z", FixedClock.now))
        assertEquals(listOf("b", "a"), credentials.matching("https://dav.example/team/cal.ics").map { it.username })
        assertEquals(emptyList(), credentials.matching("https://dav.example"))
    }

    @Test
    fun anAreaKeepsOnlyEventsPlacedInsideIt() = runTest {
        placed("Vilnius", 54.6872, 25.2797)
        placed("Kaunas", 54.8985, 23.9036)
        local("Nowhere")
        assertEquals(listOf("Vilnius"), names(EventListQuery(area = GeoArea(south = 54.6, west = 25.1, north = 54.8, east = 25.4))))
    }

    @Test
    fun syncsACalendarKeepingIdsAndDroppingWhatIsGone() = runTest {
        val calendar = SubscribedCalendar.Link("https://example.com/uni.ics")
        fun entry(uid: String, name: String) = CalendarEntry(uid, OpenEvnt.parse(openEvnt(name, "2026-11-02[Europe/Vilnius]")))
        local("Mine", "2026-11-01[Europe/Vilnius]")

        store.syncCalendar(calendar, listOf(entry("a", "Physics"), entry("b", "Math")))
        val physicsId = assertNotNull(store.findStoredCopyId(ResolvedEvent(source = EventSource.Calendar(calendar, "a"))))
        assertEquals(listOf("Math", "Mine", "Physics"), names(EventListQuery(order = EventListQuery.Order.Name)))

        store.syncCalendar(calendar, listOf(entry("a", "Physics Lab")))
        assertEquals(physicsId, store.findStoredCopyId(ResolvedEvent(source = EventSource.Calendar(calendar, "a"))))
        assertEquals(listOf(EventDifference.Renamed(mapOf("en" to "Physics"), mapOf("en" to "Physics Lab"))), store.observeUnseenDifferencesOf(physicsId).first())
        assertEquals(listOf("Mine", "Physics Lab"), names(EventListQuery(order = EventListQuery.Order.Name)))
        assertEquals(1, store.countEventsOf(calendar))

        store.forgetCalendar(calendar)
        assertEquals(listOf("Mine"), names(EventListQuery(order = EventListQuery.Order.Name)))
    }

    @Test
    fun aForgottenCalendarComesBackWithItsTagsAndHiddenEvents() = runTest {
        val calendar = SubscribedCalendar.Link("https://example.com/uni.ics")
        fun entry(uid: String, name: String) = CalendarEntry(uid, OpenEvnt.parse(openEvnt(name, "2026-11-02[Europe/Vilnius]")))
        store.syncCalendar(calendar, listOf(entry("a", "Physics"), entry("b", "Math")))
        val physicsId = assertNotNull(store.findStoredCopyId(ResolvedEvent(source = EventSource.Calendar(calendar, "a"))))
        store.tagEvent(physicsId, store.addTag("Uni"))
        store.remove(assertNotNull(store.findStoredCopyId(ResolvedEvent(source = EventSource.Calendar(calendar, "b")))))

        val forgotten = store.forgetCalendar(calendar)
        assertEquals(0, store.countEventsOf(calendar))
        assertEquals(emptyList(), store.observeHiddenCalendarEvents().first())

        store.restoreCalendar(forgotten)
        assertEquals(physicsId, store.findStoredCopyId(ResolvedEvent(source = EventSource.Calendar(calendar, "a"))))
        assertEquals(listOf("Uni"), store.observeTagsOf(physicsId).first().map { it.name })
        assertEquals(listOf("Math"), store.observeHiddenCalendarEvents().first().map { it.title })
    }

    @Test
    fun aHiddenCalendarEventStaysOutOfSyncsUntilShownAgain() = runTest {
        val calendar = SubscribedCalendar.OnDevice(7)
        val entries = listOf(CalendarEntry("a", OpenEvnt.parse(openEvnt("Name Day", "2026-11-02[Europe/Vilnius]"))))
        store.syncCalendar(calendar, entries)
        val source = EventSource.Calendar(calendar, "a")
        val removed = assertNotNull(store.remove(assertNotNull(store.findStoredCopyId(ResolvedEvent(source = source)))))

        store.syncCalendar(calendar, entries)
        assertEquals(0, store.countEventsOf(calendar))
        assertEquals(listOf(HiddenCalendarEvent(source, "Name Day", store.observeHiddenCalendarEvents().first().single().startsAt)), store.observeHiddenCalendarEvents().first())

        store.restore(removed)
        assertEquals(emptyList(), store.observeHiddenCalendarEvents().first())
        store.remove(assertNotNull(store.findStoredCopyId(ResolvedEvent(source = source))))
        store.unhideCalendarEvent(source)
        store.syncCalendar(calendar, entries)
        assertEquals(1, store.countEventsOf(calendar))
    }
}
