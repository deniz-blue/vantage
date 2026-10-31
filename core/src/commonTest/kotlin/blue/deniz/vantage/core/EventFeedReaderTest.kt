package blue.deniz.vantage.core

import io.ktor.client.engine.mock.respond
import io.ktor.http.URLBuilder
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.minutes

private const val DID_DOCUMENT =
    """{"id":"did:plc:abc","alsoKnownAs":["at://Alice.example"],"service":[{"id":"#atproto_pds","type":"AtprotoPersonalDataServer","serviceEndpoint":"https://pds.example"}]}"""

private const val MEETUP = """{"${'$'}type":"community.lexicon.calendar.event","name":"Lexicon Meetup","startsAt":"2026-07-10T15:30:00.000Z"}"""

class EventFeedReaderTest {
    private fun FakeServer.reader() = EventFeedReader(client, resolver())

    @Test
    fun listsTheEventsInAnAtRepository() = runTest {
        val server = FakeServer(mapOf(
            "https://cloudflare-dns.com/dns-query?name=_atproto.alice.example&type=TXT" to {
                respond("""{"Answer":[{"name":"_atproto.alice.example","type":16,"data":"\"did=did:plc:abc\""}]}""")
            },
            "https://plc.directory/did:plc:abc" to { respond(DID_DOCUMENT) },
            "https://pds.example/xrpc/com.atproto.repo.listRecords?repo=did%3Aplc%3Aabc&collection=community.lexicon.calendar.event&limit=50" to {
                respond("""{"records":[{"uri":"at://did:plc:abc/community.lexicon.calendar.event/1","cid":"bafy1","value":$MEETUP}],"cursor":"next"}""")
            },
        ))
        val reader = server.reader()
        val actor = reader.resolveActor("@Alice.example")
        assertEquals(AtActor("did:plc:abc", "alice.example", "https://pds.example"), actor)
        val page = reader.listAtEvents(actor, "community.lexicon.calendar.event")
        assertEquals("next", page.cursor)
        val event = page.events.single()
        assertEquals(EventSource.At(AtUri("did:plc:abc", "community.lexicon.calendar.event", "1")), event.source)
        assertEquals(mapOf("en" to "Lexicon Meetup"), event.data?.name, event.error.toString())
        assertEquals("bafy1", event.revision.cid)
    }

    @Test
    fun dropsAHandleThatDoesNotPointBack() = runTest {
        val server = FakeServer(mapOf("https://plc.directory/did:plc:abc" to { respond(DID_DOCUMENT) }))
        val actor = server.reader().resolveActor("did:plc:abc")
        assertEquals("did:plc:abc", actor.did)
        assertNull(actor.handle)
    }

    @Test
    fun aFeedIsReadAgainOnlyOnceItIsStale() = runTest {
        val feedUrl = "https://example.com/index.json"
        val server = FakeServer(mapOf(
            feedUrl to { respond("""{"version":1,"items":[{"href":"/picnic.evnt.json"}]}""") },
            "https://example.com/picnic.evnt.json" to {
                respond("""{"v":"0.2","id":"urn:uuid:7","name":{"en":"Picnic"},"occurrences":[{"start":"2026-07-10[Europe/Vilnius]"}]}""")
            },
        ))
        val clock = SteppedClock()
        val reader = EventFeedReader(server.client, server.resolver(), clock = clock)
        val feed = EventFeed.OpenEvntIndex(feedUrl)
        assertNull(reader.lastReadEvents(feed))
        reader.readEvents(feed)
        reader.readEvents(feed)
        assertEquals(listOf(feedUrl, "https://example.com/picnic.evnt.json"), server.requested)
        assertEquals(mapOf("en" to "Picnic"), reader.lastReadEvents(feed)?.single()?.data?.name)
        clock.advance(10.minutes)
        reader.readEvents(feed)
        assertEquals(4, server.requested.size)
    }

    @Test
    fun readsEveryPageOfAnIndexThatCannotFilter() = runTest {
        val feedUrl = "https://example.com/index.json"
        val server = FakeServer(mapOf(
            feedUrl to { respond("""{"version":1,"next":"/index.json?page=2","items":[{"href":"/old.evnt.json"}]}""") },
            "https://example.com/index.json?page=2" to { respond("""{"version":1,"next":"/index.json?page=2","items":[{"href":"/new.evnt.json"}]}""") },
            "https://example.com/old.evnt.json" to {
                respond("""{"v":"0.2","id":"urn:uuid:1","name":{"en":"Old"},"occurrences":[{"start":"2025-07-10[Europe/Vilnius]"}]}""")
            },
            "https://example.com/new.evnt.json" to {
                respond("""{"v":"0.2","id":"urn:uuid:2","name":{"en":"New"},"occurrences":[{"start":"2026-11-10[Europe/Vilnius]"}]}""")
            },
        ))
        val clock = SteppedClock()
        val events = EventFeedReader(server.client, server.resolver(), clock = clock).readEvents(EventFeed.OpenEvntIndex(feedUrl))
        assertEquals(listOf("Old", "New"), events.map { it.data?.name?.get("en") })
        assertEquals(listOf("New"), events.upcomingAt(clock.now()).map { it.data?.name?.get("en") })
        assertEquals(1, server.requested.count { it == "https://example.com/index.json?page=2" })
    }

    @Test
    fun asksAnIndexThatFiltersForWhatIsStillOn() = runTest {
        val feedUrl = "https://example.com/index.json"
        val clock = SteppedClock()
        val filteredUrl = URLBuilder(feedUrl).apply { parameters.append("after", clock.now().toString()) }.buildString()
        val server = FakeServer(mapOf(
            feedUrl to { respond("""{"version":1,"supports":["after"],"items":[{"href":"/old.evnt.json"}]}""") },
            filteredUrl to { respond("""{"version":1,"supports":["after"],"items":[{"href":"/new.evnt.json"}]}""") },
            "https://example.com/new.evnt.json" to {
                respond("""{"v":"0.2","id":"urn:uuid:2","name":{"en":"New"},"occurrences":[{"start":"2026-11-10[Europe/Vilnius]"}]}""")
            },
        ))
        val events = EventFeedReader(server.client, server.resolver(), clock = clock).readEvents(EventFeed.OpenEvntIndex(feedUrl))
        assertEquals(listOf("New"), events.map { it.data?.name?.get("en") })
        assertEquals(listOf(feedUrl, filteredUrl, "https://example.com/new.evnt.json"), server.requested)
    }

    @Test
    fun readsAnOpenEvntIndexPage() = runTest {
        val server = FakeServer(mapOf(
            "https://example.com/events/index.json" to {
                respond(
                    """{"version":1,"total":3,"next":"/events/index.json?cursor=2","items":[
                        {"href":"/events/picnic.evnt.json","name":{"en":"Picnic","lt":""}},
                        {"href":"gala.evnt.json"},
                        {"href":"https://other.example/a.evnt.json","name":{"lt":"Kermošius"}},
                        {"href":"/events/picnic.evnt.json"},
                        {"name":{"en":"No link"}}
                    ]}""",
                )
            },
        ))
        assertEquals(
            OpenEvntIndexPage(
                listOf(
                    OpenEvntIndexItem("https://example.com/events/picnic.evnt.json", mapOf("en" to "Picnic")),
                    OpenEvntIndexItem("https://example.com/gala.evnt.json", null),
                    OpenEvntIndexItem("https://other.example/a.evnt.json", mapOf("lt" to "Kermošius")),
                ),
                next = "https://example.com/events/index.json?cursor=2",
                total = 3,
            ),
            server.reader().readIndexPage("https://example.com/events/index.json"),
        )
    }

    @Test
    fun refusesAnIndexOfAnUnknownVersion() = runTest {
        val server = FakeServer(mapOf("https://example.com/index.json" to { respond("""{"version":2,"items":[]}""") }))
        val refused = assertFailsWith<EventException> { server.reader().readIndexPage("https://example.com/index.json") }
        assertEquals(EventError.Kind.UnknownFormat, refused.error.kind)
    }
}
