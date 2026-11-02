package blue.deniz.vantage.storage

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import blue.deniz.vantage.core.CalendarMarker
import blue.deniz.vantage.core.CalendarPeriod
import blue.deniz.vantage.core.EventFetcher
import blue.deniz.vantage.core.EventFormat
import blue.deniz.vantage.core.EventResolver
import blue.deniz.vantage.core.EventSource
import blue.deniz.vantage.core.FolioClient
import blue.deniz.vantage.core.ResolvedEvent
import blue.deniz.vantage.core.SubscribedCalendar
import directory.evnt.PartialDate
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

private fun document(name: String, start: String) =
    """{"v":"0.2","id":"urn:example:${name.filter(Char::isLetterOrDigit)}","name":{"en":"$name"},"occurrences":[{"start":"$start"}]}"""

class BackupTest {
    private val client = HttpClient(MockEngine { respond("gone", HttpStatusCode.NotFound) })
    private val resolver = EventResolver(EventFetcher(client))

    private fun inMemoryDatabase() = Room.inMemoryDatabaseBuilder<VantageDatabase>()
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()

    private val oldPhone = inMemoryDatabase()
    private val newPhone = inMemoryDatabase()
    private val exporting = EventStore(oldPhone, resolver, FolioClient(client))
    private val importing = EventStore(newPhone, resolver, FolioClient(client))

    @AfterTest
    fun close() {
        oldPhone.close()
        newPhone.close()
    }

    private suspend fun EventStore.addFrom(source: EventSource, name: String, start: String) =
        add(resolver.parse(ResolvedEvent(source = source, format = EventFormat.OpenEvnt, raw = document(name, start))))

    private suspend fun EventStore.names() = observeList().first().map { it.data?.name?.get("en") }.toSet()

    @Test
    fun aBackupCarriesEventsTagsMarksAndHiddenEventsToAnotherInstall() = runTest {
        val calendar = SubscribedCalendar.Link("https://example.com/uni.ics")
        val picnic = exporting.addFrom(EventSource.Local(), "Picnic", "2026-07-10[Europe/Vilnius]")
        exporting.addFrom(EventSource.Http("https://example.com/talk.json"), "Talk", "2026-11-12T18:00[Europe/Vilnius]")
        exporting.addFrom(EventSource.Local(isTestEvent = true), "Test Event", "2026-11-13[Europe/Vilnius]")
        exporting.addFrom(EventSource.Calendar(calendar, "lecture"), "Lecture", "2026-11-03T10:20[Europe/Vilnius]")
        val uni = exporting.addTag("Uni", color = "#a97dff")
        val outdoors = exporting.addTag("Outdoors")
        oldPhone.tags().addParent(TagParentEntity(uni, outdoors))
        exporting.tagEvent(picnic, outdoors)
        val marker = CalendarMarker("upload", "LW3 upload cut-off", PartialDate.parse("2026-11-23[Europe/Vilnius]"))
        val period = CalendarPeriod("exams", "Exam session", PartialDate.parse("2027-01-04[Europe/Vilnius]"), PartialDate.parse("2027-01-24[Europe/Vilnius]"))
        exporting.saveMarker(marker)
        exporting.savePeriod(period)
        oldPhone.hiddenCalendarEvents().upsert(HiddenCalendarEventEntity(EventSource.Calendar(calendar, "cancelled"), "Cancelled Lab", null))
        importing.addFrom(EventSource.Local(), "Already Here", "2026-12-01[Europe/Vilnius]")

        val text = exporting.exportBackup().toJson()
        importing.importBackup(Backup.fromJson(text))
        importing.importBackup(Backup.fromJson(text))

        assertEquals(setOf("Picnic", "Talk", "Already Here"), importing.names())
        assertEquals(listOf("Outdoors"), importing.observeTagsOf(picnic).first().map { it.name })
        assertEquals(mapOf("Outdoors" to null, "Uni" to "#a97dff"), importing.observeTags().first().associate { it.name to it.color })
        assertEquals(listOf(TagParentEntity(uni, outdoors)), newPhone.tags().observeParents().first())
        assertEquals(listOf(marker), importing.observeMarkers().first())
        assertEquals(listOf(period), importing.observePeriods().first())
        assertEquals(listOf("Cancelled Lab"), importing.observeHiddenCalendarEvents().first().map { it.title })
    }

    @Test
    fun importingReplacesAnEventWithTheSameIdAndItsTags() = runTest {
        val picnic = exporting.addFrom(EventSource.Local(), "Picnic", "2026-07-10[Europe/Vilnius]")
        val backup = exporting.exportBackup()
        importing.importBackup(backup)
        val stale = importing.addTag("Stale")
        importing.tagEvent(picnic, stale)

        exporting.saveEdited(picnic, resolver.parse(ResolvedEvent(format = EventFormat.OpenEvnt, raw = document("Picnic in the Park", "2026-07-11[Europe/Vilnius]"))).data!!)
        importing.importBackup(exporting.exportBackup())

        assertEquals(setOf("Picnic in the Park"), importing.names())
        assertEquals(emptyList(), importing.observeTagsOf(picnic).first())
    }
}
