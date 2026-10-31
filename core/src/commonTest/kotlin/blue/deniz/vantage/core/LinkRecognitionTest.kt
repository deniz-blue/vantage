package blue.deniz.vantage.core

import io.ktor.http.Url
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class LinkRecognitionTest {
    private val record = AtUri("did:plc:abc", "community.lexicon.calendar.event", "3kq")

    @Test
    fun recognizesAnEventRecord() {
        assertEquals(
            ResolvedEvent(source = EventSource.At(record), format = EventFormat.CommunityLexicon),
            ResolvedEvent.fromLink(" at://did:plc:abc/community.lexicon.calendar.event/3kq "),
        )
        assertNull(ResolvedEvent.fromLink("at://did:plc:abc/app.bsky.feed.post/3kq"))
        assertNull(ResolvedEvent.fromLink("at://did:plc:abc/directory.evnt.event"))
    }

    @Test
    fun recognizesAFolioEventWithItsEditToken() {
        assertEquals(
            ResolvedEvent(
                source = EventSource.Folio(id = "e1", baseUrl = "https://folio.denizblue.workers.dev", editToken = "t0k"),
                format = EventFormat.OpenEvnt,
            ),
            ResolvedEvent.fromLink("https://folio.denizblue.workers.dev/events/e1?token=t0k"),
        )
    }

    @Test
    fun recognizesAMediaWikiPage() {
        assertEquals(
            ResolvedEvent(source = EventSource.MediaWiki("https://wiki.example/", "Events/Meetup 2026"), format = EventFormat.OpenEvnt),
            ResolvedEvent.fromLink("https://wiki.example/rest.php/v1/page/Events%2FMeetup%202026"),
        )
    }

    @Test
    fun followsAnEventsLinkToWhatItPointsAt() {
        assertEquals(
            ResolvedEvent(source = EventSource.At(record), format = EventFormat.CommunityLexicon),
            ResolvedEvent.fromLink("https://eventsl.ink/e?at=at%3A%2F%2Fdid%3Aplc%3Aabc%2Fcommunity.lexicon.calendar.event%2F3kq"),
        )
        assertEquals(
            ResolvedEvent(source = EventSource.Http("https://example.com/cal.ics"), format = EventFormat.ICalendar),
            ResolvedEvent.fromLink("https://eventsl.ink/event?url=https%3A%2F%2Fexample.com%2Fcal.ics"),
        )
        assertEquals(
            ResolvedEvent(format = EventFormat.OpenEvnt, raw = """{"v":"0.2"}"""),
            ResolvedEvent.fromLink("https://eventsl.ink/?type=event&data=%7B%22v%22%3A%220.2%22%7D"),
        )
        assertNull(ResolvedEvent.fromLink("https://eventsl.ink/e"))
    }

    @Test
    fun followsTheAppsOwnEventPage() {
        val intent = assertNotNull(EventsLink.parseIntent(Url("https://vantage.tsx.lt/event?url=https%3A%2F%2Fexample.com%2Fcal.ics")))
        assertEquals(
            ResolvedEvent(source = EventSource.Http("https://example.com/cal.ics"), format = EventFormat.ICalendar),
            ResolvedEvent.fromEventsLinkIntent(intent),
        )
        assertNull(EventsLink.parseIntent(Url("https://vantage.tsx.lt/settings")))
        assertNull(ResolvedEvent.fromEventsLinkIntent(EventsLink.Intent()))
    }

    @Test
    fun readsAnIndexLink() {
        val index = EventFeed.OpenEvntIndex("https://deniz.blue/events-data/index.json")
        assertEquals(index, EventsLink.parseIndexLink(Url("https://eventsl.ink/index?url=https%3A%2F%2Fdeniz.blue%2Fevents-data%2Findex.json")))
        assertEquals(index, EventsLink.parseIndexLink(Url("https://vantage.tsx.lt/?type=index&url=https%3A%2F%2Fdeniz.blue%2Fevents-data%2Findex.json")))
        assertNull(EventsLink.parseIndexLink(Url("https://eventsl.ink/e?url=https%3A%2F%2Fexample.com%2Fa.evnt.json")))
        assertNull(EventsLink.parseIndexLink(Url("https://eventsl.ink/index?url=at%3A%2F%2Fdid%3Aplc%3Aabc")))
    }

    @Test
    fun guessesTheFormatOfAPlainUrlFromItsPath() {
        assertEquals(EventFormat.OpenEvnt, ResolvedEvent.fromLink("https://example.com/a.evnt.json?x=1")?.format)
        assertEquals(EventFormat.ICalendar, ResolvedEvent.fromLink("https://example.com/a.ics")?.format)
        assertEquals(EventFormat.Unknown, ResolvedEvent.fromLink("https://example.com/event/7")?.format)
        assertNull(ResolvedEvent.fromLink("next tuesday at the pub"))
        assertNull(ResolvedEvent.fromLink("ftp://example.com/a.ics"))
    }
}

class EventSourceTest {
    @Test
    fun buildsTheAddressEachSourceIsReadFrom() {
        assertEquals("https://folio.denizblue.workers.dev/events/e1", EventSource.Folio("e1").location)
        assertEquals("https://wiki.example/rest.php/v1/page/Events%2FMeetup%202026", EventSource.MediaWiki("https://wiki.example/", "Events/Meetup 2026").location)
        assertEquals("https://dav.example/files/events/a.ics", EventSource.WebDav("https://dav.example/files/", "events/a.ics").location)
        assertEquals("https://dav.example/a.ics", EventSource.WebDav("https://dav.example/files/", "/a.ics").location)
    }

    @Test
    fun aFolioEditLinkOpensTheSameEventAgain() {
        val source = EventSource.Folio("e1", editToken = "t0k")
        assertEquals("https://folio.denizblue.workers.dev/events/e1?token=t0k", source.editLink)
        assertEquals(source, ResolvedEvent.fromLink(source.editLink!!)?.source)
        assertNull(EventSource.Folio("e1").editLink)
    }

    @Test
    fun linksBackThroughEventsLink() {
        assertEquals(
            "https://eventsl.ink/e?at=at%3A%2F%2Fdid%3Aplc%3Aabc%2Fdirectory.evnt.event%2F1",
            EventsLink.shareLinkTo(EventSource.At(AtUri("did:plc:abc", "directory.evnt.event", "1"))),
        )
        assertEquals("https://eventsl.ink/e?url=https%3A%2F%2Ffolio.denizblue.workers.dev%2Fevents%2Fe1", EventsLink.shareLinkTo(EventSource.Folio("e1", editToken = "secret")))
        assertNull(EventsLink.shareLinkTo(EventSource.Local()))
    }

    @Test
    fun aShareLinkLeadsBackToTheSameSource() {
        val sources = listOf(
            EventSource.At(AtUri("did:plc:abc", "directory.evnt.event", "1")),
            EventSource.Http("https://example.com/a.ics"),
            EventSource.Folio("e1"),
        )
        for (source in sources) assertEquals(source, ResolvedEvent.fromLink(EventsLink.shareLinkTo(source)!!)?.source)
    }

    @Test
    fun writesTheTypeNextToTheFields() {
        val json = kotlinx.serialization.json.Json
        val source: EventSource = EventSource.At(AtUri("did:plc:abc", "directory.evnt.event", "1"))
        val text = json.encodeToString(EventSource.serializer(), source)
        assertEquals("""{"type":"at","uri":"at://did:plc:abc/directory.evnt.event/1"}""", text)
        assertEquals(source, json.decodeFromString(EventSource.serializer(), text))
        assertEquals(EventSource.Local(), json.decodeFromString(EventSource.serializer(), """{"type":"local"}"""))
    }

    @Test
    fun namesEachSourceTypeAsItsJsonDoes() {
        val sources = listOf(
            EventSource.Unknown,
            EventSource.Local(),
            EventSource.Http("https://example.com/e.json"),
            EventSource.At(AtUri("did:plc:abc", "directory.evnt.event", "1")),
            EventSource.Folio("e1"),
            EventSource.MediaWiki("https://wiki.example", "Event"),
            EventSource.WebDav("https://dav.example", "/e.json"),
            EventSource.Calendar(SubscribedCalendar.OnDevice(1), "uid"),
        )
        assertEquals(EventSourceType.entries.toSet(), sources.map { it.type }.toSet())
        for (source in sources) {
            val written = kotlinx.serialization.json.Json.encodeToJsonElement(EventSource.serializer(), source)
            assertEquals(source.type.serialName, written.jsonObject["type"]?.jsonPrimitive?.content)
        }
    }
}
