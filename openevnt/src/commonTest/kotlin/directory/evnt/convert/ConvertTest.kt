package directory.evnt.convert

import directory.evnt.Component
import directory.evnt.Media
import directory.evnt.Occurrence
import directory.evnt.OpenEvnt
import directory.evnt.PartialDate
import directory.evnt.Venue
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

private fun assertValid(event: OpenEvnt) = assertEquals(emptyList(), OpenEvnt.issuesOf(event.toJson()))

private fun date(text: String) = PartialDate.parse(text)

class WrittenDateTest {
    @Test
    fun readsTheShapesOtherFormatsWrite() {
        assertEquals(date("2026-07-10T18:30[Europe/Vilnius]"), parseWrittenDate("2026-07-10T15:30:00.000Z", "Europe/Vilnius"))
        assertEquals(date("2026-07-10T18:30[Europe/Vilnius]"), parseWrittenDate("2026-07-10T17:30+02:00", "Europe/Vilnius"))
        assertEquals(date("2026-07-10T15:30[Europe/Vilnius]"), parseWrittenDate("2026-07-10 15:30", "Europe/Vilnius"))
        assertEquals(date("2026-07-10[UTC]"), parseWrittenDate("2026-07-10T00:00:00Z"))
        assertEquals(date("2026-07[UTC]"), parseWrittenDate("2026-7"))
        assertEquals(date("2026-07-10T18:30[Europe/Vilnius]"), parseWrittenDate("2026-07-10T18:30[Europe/Vilnius]", "UTC"))
        assertNull(parseWrittenDate("2026-02-30"))
        assertNull(parseWrittenDate("next tuesday"))
    }
}

class CommunityLexiconTest {
    @Test
    fun mergesThePhysicalLocationsIntoOneVenue() {
        val event = CommunityLexicon.read(
            """
            {
              "name": "Open Evnt Meetup",
              "startsAt": "2026-07-10T15:30:00.000Z",
              "endsAt": "2026-07-10T18:00:00.000Z",
              "status": "community.lexicon.calendar.event#postponed",
              "description": "Weekly gathering.",
              "locations": [
                { "${'$'}type": "community.lexicon.calendar.event#uri", "uri": "https://meet.example.com", "name": "Call" },
                { "${'$'}type": "community.lexicon.location.address", "name": "Community Hall", "street": "Main St 1", "locality": "Vilnius", "country": "lt" },
                { "${'$'}type": "community.lexicon.location.geo", "latitude": "54.68", "longitude": "25.28" },
                { "${'$'}type": "community.lexicon.location.fsq", "fsq_place_id": "4b5d" }
              ],
              "uris": [{ "uri": "https://example.com/meetup", "name": "Event page" }]
            }
            """,
            ConvertOptions(timeZoneId = "Europe/Vilnius"),
        )
        assertValid(event)
        assertEquals(OpenEvnt.Status.Postponed, event.status)
        assertEquals(listOf(Occurrence(date("2026-07-10T18:30[Europe/Vilnius]"), date("2026-07-10T21:00[Europe/Vilnius]"))), event.occurrences)
        assertEquals(
            Venue(
                name = mapOf("en" to "Community Hall"),
                address = "Main St 1 Vilnius",
                countryCode = "LT",
                components = listOf(Component.Coordinates(54.68, 25.28), Component.PlaceId("com.foursquare", "4b5d")),
            ),
            event.venue,
        )
        assertEquals(
            listOf(
                Component.Link("https://meet.example.com", mapOf("en" to "Call")),
                Component.Link("https://example.com/meetup", mapOf("en" to "Event page")),
                Component.PlainText(mapOf("en" to "Weekly gathering.")),
            ),
            event.components,
        )
    }

    @Test
    fun pointsMediaAtTheBlobOfTheGivenDid() {
        val event = CommunityLexicon.read(
            """
            {
              "name": "Poster",
              "media": [{ "role": "poster", "alt": "The poster", "content": { "${'$'}type": "blob", "ref": { "${'$'}link": "bafy" }, "size": 1, "mimeType": "image/png" } }],
              "additionalData": { "tickets": 3 }
            }
            """,
            ConvertOptions(did = "did:plc:example"),
        )
        assertValid(event)
        assertNull(event.venue)
        assertEquals(
            listOf(
                Component.SplashMedia(
                    Media(sources = listOf(Media.Source("https://blobs.blue/did:plc:example/blob/bafy", "image/png")), alt = mapOf("en" to "The poster")),
                    roles = listOf("poster", "background"),
                ),
                Component.Unknown(CommunityLexicon.ADDITIONAL_DATA_TYPE, JsonObject(mapOf("tickets" to JsonPrimitive(3)))),
            ),
            event.components,
        )
    }
}

class ICalendarTest {
    private fun calendar(vararg lines: String) = (listOf("BEGIN:VCALENDAR", "VERSION:2.0") + lines + "END:VCALENDAR").joinToString("\r\n")

    @Test
    fun readsATimedEvent() {
        val event = ICalendar.read(
            calendar(
                "BEGIN:VEVENT",
                "UID:1",
                "SUMMARY:Evening Meetup",
                "LOCATION:Hezarfen Pub",
                "DESCRIPTION:Bring a friend\\, or two.\\nSecond line",
                "URL:https://example.com/meetup",
                "DTSTART:20260906T150000Z",
                "DTEND:20260906T190000Z",
                "END:VEVENT",
            ),
        )
        assertValid(event)
        assertEquals(mapOf("en" to "Evening Meetup"), event.name)
        assertEquals(Venue(name = mapOf("en" to "Hezarfen Pub")), event.venue)
        assertEquals(listOf(Occurrence(date("2026-09-06T15:00[UTC]"), date("2026-09-06T19:00[UTC]"))), event.occurrences)
        assertEquals(
            listOf(
                Component.PlainText(mapOf("en" to "Bring a friend, or two.\nSecond line")),
                Component.Link("https://example.com/meetup"),
            ),
            event.components,
        )
    }

    @Test
    fun anAllDayEndIsTheLastDayItself() {
        val event = ICalendar.read(
            calendar("BEGIN:VEVENT", "SUMMARY:Weekend Festival", "DTSTART;VALUE=DATE:20260815", "DTEND;VALUE=DATE:20260817", "END:VEVENT"),
        )
        assertEquals(listOf(Occurrence(date("2026-08-15[UTC]"), date("2026-08-16[UTC]"))), event.occurrences)
    }

    @Test
    fun aSeriesBecomesSeparateOccurrences() {
        val event = ICalendar.read(
            calendar(
                "BEGIN:VEVENT", "UID:w#0", "SUMMARY:Weekly Meetup", "LOCATION:Hall A", "DTSTART:20260601T090000Z", "DTEND:20260601T170000Z", "END:VEVENT",
                "BEGIN:VEVENT", "UID:w#1", "SUMMARY:Weekly Meetup", "STATUS:CANCELLED", "DTSTART:20260608T090000Z", "DTEND:20260608T170000Z", "END:VEVENT",
                "BEGIN:VEVENT", "UID:w#2", "SUMMARY:Weekly Meetup", "DTSTART:20260615T090000Z", "DTEND:20260615T170000Z", "END:VEVENT",
            ),
        )
        assertValid(event)
        assertNull(event.status)
        assertEquals(listOf(null, Occurrence.Status.Cancelled, null), event.occurrences?.map { it.status })
        assertEquals("2026-06-15T09:00[UTC]", event.occurrences?.last()?.start.toString())
    }

    @Test
    fun readsAWallTimeInTheZoneItsTzidNames() {
        val event = ICalendar.read(
            calendar(
                "BEGIN:VEVENT", "UID:1", "SUMMARY:Zoned", "DTSTART;TZID=Europe/Vilnius:20261114T180000", "END:VEVENT",
                "BEGIN:VEVENT", "UID:2", "SUMMARY:Zoned", "DTSTART;TZID=FLE Standard Time:20261121T180000", "END:VEVENT",
                "BEGIN:VEVENT", "UID:3", "SUMMARY:Zoned", "DTSTART:20261128T180000", "END:VEVENT",
            ),
            ConvertOptions(timeZoneId = "Europe/Riga"),
        )
        assertEquals(
            listOf("2026-11-14T18:00[Europe/Vilnius]", "2026-11-21T18:00[Europe/Riga]", "2026-11-28T18:00[Europe/Riga]"),
            event.occurrences?.map { it.start.toString() },
        )
    }

    @Test
    fun unfoldsLongLines() {
        val event = ICalendar.read(calendar("BEGIN:VEVENT", "SUMMARY:A very long", "  title", "DTSTART:20260601T090000Z", "END:VEVENT"))
        assertEquals(mapOf("en" to "A very long title"), event.name)
    }

    @Test
    fun throwsWhenThereIsNoEvent() {
        assertTrue(ICalendar.looksLikeICalendar(calendar()))
        assertFailsWith<IllegalArgumentException> { ICalendar.read(calendar()) }
    }
}
