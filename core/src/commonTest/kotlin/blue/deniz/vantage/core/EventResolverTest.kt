package blue.deniz.vantage.core

import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.time.Instant

private val CALENDAR = listOf(
    "BEGIN:VCALENDAR", "VERSION:2.0",
    "BEGIN:VEVENT", "UID:1", "SUMMARY:Evening Meetup", "DTSTART:20260906T150000Z", "DTEND:20260906T190000Z", "END:VEVENT",
    "END:VCALENDAR",
).joinToString("\r\n")

private val OPENEVNT = """{"v":"0.2","id":"urn:uuid:7","name":{"en":"Picnic"},"occurrences":[{"start":"2026-07-10[Europe/Vilnius]"}]}"""

class EventResolverTest {
    @Test
    fun fetchesInfersAndParsesACalendarFile() = runTest {
        val server = FakeServer(mapOf("https://example.com/feed" to { respond(CALENDAR, headers = headersOf(HttpHeaders.ETag, "\"v1\"")) }))
        val event = server.resolver().resolve(ResolvedEvent(source = EventSource.Http("https://example.com/feed")))
        assertEquals(EventFormat.ICalendar, event.format)
        assertEquals(mapOf("en" to "Evening Meetup"), event.data?.name)
        assertEquals("https://example.com/feed", event.data?.id)
        assertEquals("\"v1\"", event.revision.etag)
        assertEquals(FixedClock.now(), event.updatedAt)
        assertNull(event.error)
        assertEquals(
            listOf(TimeRange(Instant.parse("2026-09-06T15:00:00Z"), Instant.parse("2026-09-06T19:00:00Z"))),
            event.data?.timeRanges(),
        )
    }

    @Test
    fun aCachedEventIsNotFetchedAgainUntilRefreshed() = runTest {
        val server = FakeServer(mapOf("https://example.com/a.json" to { respond(OPENEVNT) }))
        val cached = ResolvedEvent(source = EventSource.Http("https://example.com/a.json"), format = EventFormat.OpenEvnt, raw = OPENEVNT)
        assertEquals(mapOf("en" to "Picnic"), server.resolver().resolve(cached).data?.name)
        assertEquals(emptyList(), server.requested)
        server.resolver().refresh(cached)
        assertEquals(listOf("https://example.com/a.json"), server.requested)
    }

    @Test
    fun aFailedRefreshKeepsTheOldEvent() = runTest {
        val server = FakeServer(emptyMap())
        val old = server.resolver().parse(ResolvedEvent(source = EventSource.Http("https://example.com/a.json"), format = EventFormat.OpenEvnt, raw = OPENEVNT))
        val refreshed = server.resolver().refresh(old)
        assertEquals(old.data, refreshed.data)
        assertEquals(EventError.Kind.Fetch, refreshed.fetchError?.kind)
        assertEquals(404, refreshed.fetchError?.status)
        assertNull(refreshed.parseError)
    }

    @Test
    fun anInvalidDocumentGivesItsIssues() = runTest {
        val event = FakeServer(emptyMap()).resolver().parse(
            ResolvedEvent(source = EventSource.Local(), format = EventFormat.OpenEvnt, raw = """{"v":"0.2","id":"urn:uuid:7","name":{}}"""),
        )
        assertNull(event.data)
        assertEquals(EventError.Kind.Validation, event.parseError?.kind)
        assertNotNull(event.parseError?.issues?.singleOrNull())
    }

    @Test
    fun readsAMediaWikiPageAndItsRevision() = runTest {
        val server = FakeServer(mapOf(
            "https://wiki.example/rest.php/v1/page/Picnic" to {
                respond("""{"id":1,"key":"Picnic","title":"Picnic","latest":{"id":42,"timestamp":"2026-10-01T12:00:00Z"},"source":${kotlinx.serialization.json.JsonPrimitive(OPENEVNT)}}""")
            },
        ))
        val event = server.resolver().resolve(ResolvedEvent(source = EventSource.MediaWiki("https://wiki.example/", "Picnic"), format = EventFormat.OpenEvnt))
        assertEquals(mapOf("en" to "Picnic"), event.data?.name)
        assertEquals(Revision(revisionId = 42, revisionTimestamp = Instant.parse("2026-10-01T12:00:00Z")), event.revision)
    }

    @Test
    fun readsARecordFromThePdsItsDidNames() = runTest {
        val record = """{"${'$'}type":"community.lexicon.calendar.event","name":"Lexicon Meetup","startsAt":"2026-07-10T15:30:00.000Z"}"""
        val server = FakeServer(mapOf(
            "https://plc.directory/did:plc:abc" to {
                respond("""{"id":"did:plc:abc","service":[{"id":"#atproto_pds","type":"AtprotoPersonalDataServer","serviceEndpoint":"https://pds.example"}]}""")
            },
            "https://pds.example/xrpc/com.atproto.repo.getRecord?repo=did%3Aplc%3Aabc&collection=community.lexicon.calendar.event&rkey=3kq" to {
                respond("""{"uri":"at://did:plc:abc/community.lexicon.calendar.event/3kq","cid":"bafyrec","value":$record}""")
            },
        ))
        val event = server.resolver().resolve(ResolvedEvent.fromLink("at://did:plc:abc/community.lexicon.calendar.event/3kq")!!)
        assertNull(event.error, server.requested.toString())
        assertEquals("bafyrec", event.revision.cid)
        assertEquals(mapOf("en" to "Lexicon Meetup"), event.data?.name)
        assertEquals("at://did:plc:abc/community.lexicon.calendar.event/3kq", event.data?.id)
    }

    @Test
    fun anXrpcFailureCarriesTheServersMessage() = runTest {
        val server = FakeServer(mapOf(
            "https://plc.directory/did:plc:abc" to { respond("""{"id":"did:plc:abc","service":[]}""") },
            "https://bsky.social/xrpc/com.atproto.repo.getRecord?repo=did%3Aplc%3Aabc&collection=directory.evnt.event&rkey=1" to {
                respond("""{"error":"RecordNotFound","message":"Could not locate record"}""", HttpStatusCode.BadRequest)
            },
        ))
        val event = server.resolver().resolve(ResolvedEvent.fromLink("at://did:plc:abc/directory.evnt.event/1")!!)
        assertEquals(EventError(EventError.Kind.Xrpc, "Could not locate record", 400), event.fetchError)
    }
}

class FormatInferenceTest {
    @Test
    fun tellsFormatsApartByTheirContent() {
        assertEquals(EventFormat.ICalendar, inferEventFormat(CALENDAR))
        assertEquals(EventFormat.OpenEvnt, inferEventFormat(OPENEVNT))
        assertEquals(EventFormat.CommunityLexicon, inferEventFormat("""{"${'$'}type":"community.lexicon.calendar.event","name":"x"}"""))
        assertEquals(EventFormat.Unknown, inferEventFormat("""{"name":"x"}"""))
        assertEquals(EventFormat.Unknown, inferEventFormat("<html>"))
    }
}
