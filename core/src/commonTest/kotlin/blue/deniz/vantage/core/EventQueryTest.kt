package blue.deniz.vantage.core

import directory.evnt.Component
import directory.evnt.Occurrence
import directory.evnt.OpenEvnt
import directory.evnt.PartialDate
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlinx.datetime.LocalDate

private const val INDEX_URL = "https://example.com/index.json"
private val NOW = Instant.parse("2026-10-21T00:00:00Z")

private fun event(id: Int, name: String, start: String, venue: String = "") =
    """{"v":"0.2","id":"urn:uuid:$id","name":{"en":"$name"},"occurrences":[{"start":"$start[Europe/Vilnius]"}]$venue}"""

private const val IN_VILNIUS = ""","venue":{"components":[{"${'$'}type":"${Component.Coordinates.TYPE_ID}","latitude":54.68,"longitude":25.28}]}"""

private val THREE_EVENTS = mapOf<String, MockRequestHandleScope.(HttpRequestData) -> HttpResponseData>(
    "https://example.com/old.evnt.json" to { respond(event(1, "Archive Night", "2025-07-10")) },
    "https://example.com/gala.evnt.json" to { respond(event(2, "Gala", "2026-12-01", IN_VILNIUS)) },
    "https://example.com/picnic.evnt.json" to { respond(event(3, "Picnic", "2026-11-10")) },
)

private fun eventOnDays(name: String, vararg days: String) = ResolvedEvent(
    data = OpenEvnt(
        id = "urn:uuid:$name",
        name = mapOf("en" to name),
        occurrences = days.map { Occurrence(start = PartialDate.Day(LocalDate.parse(it), "Europe/Vilnius")) },
    ),
)

private const val THREE_ITEMS = """[{"href":"/old.evnt.json"},{"href":"/gala.evnt.json","name":{"en":"Gala"}},{"href":"/picnic.evnt.json"}]"""

class EventQueryTest {
    private fun ResolvedEvent.englishName() = data?.name?.get("en")

    @Test
    fun asksTheIndexForWhatItSupportsAndKeepsTheRest() {
        val query = EventQuery(search = " jam ", from = NOW, order = EventOrder.Start)
        assertEquals(
            "https://example.com/index.json?search=jam&after=2026-10-21T00%3A00%3A00Z",
            query.indexPageUrl(INDEX_URL, setOf("search", "after")),
        )
        val unsupported = query.unsupportedPart(setOf("search", "after"))
        assertEquals(EventQuery(order = EventOrder.Start), unsupported)
        assertTrue(unsupported.filtersOrSorts)
        assertFalse(EventQuery(search = "jam", until = NOW).unsupportedPart(setOf("search", "before")).filtersOrSorts)
    }

    @Test
    fun leavesTheAddressOfAnIndexThatCannotFilter() {
        val query = EventQuery(search = "jam", until = NOW)
        assertEquals(INDEX_URL, query.indexPageUrl(INDEX_URL, emptySet()))
        assertEquals(query, query.unsupportedPart(emptySet()))
    }

    @Test
    fun listsEndedEventsNewestFirst() {
        val events = listOf(
            eventOnDays("Older", "2026-01-05"),
            eventOnDays("Weekly", "2026-10-01", "2026-10-29"),
            eventOnDays("Recent", "2026-10-10"),
            eventOnDays("Undated"),
        )
        val ended = EventQuery(endedBy = NOW, order = EventOrder.StartDescending)
        assertEquals(listOf("Recent", "Older"), ended.filterAndSort(events, listOf("en")).map { it.englishName() })
    }

    @Test
    fun filtersAndSortsEventsOnTheDevice() = runTest {
        val server = FakeServer(THREE_EVENTS + (INDEX_URL to { respond("""{"version":1,"items":$THREE_ITEMS}""") }))
        val reader = EventFeedReader(server.client, server.resolver(), clock = SteppedClock(NOW))
        val read = reader.readIndexEntries(INDEX_URL, EventQuery())
        assertTrue(read.readEveryItem)
        val events = read.entries.mapNotNull { it.event }
        fun names(query: EventQuery) = query.filterAndSort(events, listOf("en")).map { it.englishName() }
        assertEquals(listOf("Picnic", "Gala"), names(EventQuery(from = NOW, order = EventOrder.Start)))
        assertEquals(listOf("Archive Night", "Gala", "Picnic"), names(EventQuery(order = EventOrder.Name)))
        assertEquals(listOf("Archive Night"), names(EventQuery(until = NOW)))
        assertEquals(listOf("Picnic"), names(EventQuery(search = "PIC")))
        assertEquals(listOf("Gala"), names(EventQuery(area = GeoArea(south = 54.0, west = 25.0, north = 55.0, east = 26.0))))
        assertEquals(listOf("Picnic"), names(EventQuery(from = Instant.parse("2026-11-01T00:00:00Z"), until = Instant.parse("2026-11-30T00:00:00Z"))))
    }

    @Test
    fun sortsAnEventWithManyDatesByItsNextOne() {
        val events = listOf(
            eventOnDays("Workshop Series", "2026-10-01", "2026-12-20"),
            eventOnDays("Gala", "2026-12-01"),
            eventOnDays("Fair", "2026-10-20", "2026-10-21"),
            eventOnDays("Undated"),
        )
        val sorted = EventQuery(from = NOW, order = EventOrder.Start).filterAndSort(events, listOf("en"))
        assertEquals(listOf("Fair", "Gala", "Workshop Series", "Undated"), sorted.map { it.englishName() })
        assertEquals(listOf("Workshop Series", "Fair"), EventQuery(until = NOW).filterAndSort(events, listOf("en")).map { it.englishName() })
    }

    @Test
    fun anIndexListsItsPagesInItsOwnOrder() = runTest {
        val server = FakeServer(THREE_EVENTS + mapOf(
            INDEX_URL to { respond("""{"version":1,"total":3,"next":"/page2.json","items":[{"href":"/old.evnt.json"},{"href":"/gala.evnt.json","name":{"en":"Gala"}}]}""") },
            "https://example.com/page2.json" to { respond("""{"version":1,"items":[{"href":"/picnic.evnt.json"}]}""") },
        ))
        val events = EventFeedReader(server.client, server.resolver(), clock = SteppedClock(NOW)).eventsOf(EventFeed.OpenEvntIndex(INDEX_URL), listOf("en"))

        val firstPage = events.list(EventQuery(), limit = 2).last()
        assertEquals(EventListingStatus.HasMore, firstPage.status)
        assertEquals(listOf("Archive Night", "Gala"), firstPage.events.map { it.englishName() })
        assertEquals(EventCount.Total(3), firstPage.count)
        assertEquals(mapOf("en" to "Gala"), firstPage.listedNames[EventSource.Http("https://example.com/gala.evnt.json")])

        val everything = events.list(EventQuery(), limit = 4).last()
        assertEquals(EventListingStatus.Complete, everything.status)
        assertEquals(listOf("Archive Night", "Gala", "Picnic"), everything.events.map { it.englishName() })
    }

    @Test
    fun anIndexThatCannotFilterIsReadWholeAndFilteredHere() = runTest {
        val server = FakeServer(THREE_EVENTS + (INDEX_URL to { respond("""{"version":1,"items":$THREE_ITEMS}""") }))
        val events = EventFeedReader(server.client, server.resolver(), clock = SteppedClock(NOW)).eventsOf(EventFeed.OpenEvntIndex(INDEX_URL), listOf("en"))
        val listings = events.list(EventQuery(from = NOW, order = EventOrder.Start), limit = 1).toList()
        assertIs<EventListingStatus.Loading>(listings.first().status)
        val last = listings.last()
        assertEquals(listOf("Picnic"), last.events.map { it.englishName() })
        assertEquals(EventListingStatus.HasMore, last.status)
        assertEquals(EventCount.Matching(matching = 2, read = 3, readWholeSource = true), last.count)
    }

    @Test
    fun aBrokenIndexEndsInAFailure() = runTest {
        val server = FakeServer(mapOf(INDEX_URL to { respond("""{"version":9,"items":[]}""") }))
        val events = EventFeedReader(server.client, server.resolver()).eventsOf(EventFeed.OpenEvntIndex(INDEX_URL), listOf("en"))
        assertIs<EventListingStatus.Failed>(events.list(EventQuery(), limit = 10).last().status)
    }
}
