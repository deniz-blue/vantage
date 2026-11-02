package blue.deniz.vantage.core

import directory.evnt.convert.ConvertOptions
import io.ktor.client.engine.mock.respond
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class CalendarLinkReaderTest {
    private val within = LocalDate(2026, 1, 1)..LocalDate(2027, 1, 1)

    @Test
    fun anOpenEvntIndexIsReadAsCalendarEntriesKeyedByDocumentId() = runTest {
        val link = SubscribedCalendar.Link("https://meets.example/calendars/abc.json")
        val server = FakeServer(mapOf(
            link.url to { respond("""{"version":1,"items":[{"href":"/calendars/abc/events/1.evnt.json"}],"next":"/calendars/abc.json?page=2"}""") },
            "https://meets.example/calendars/abc.json?page=2" to { respond("""{"version":1,"items":[{"href":"https://meets.example/calendars/abc/events/2.evnt.json"}]}""") },
            "https://meets.example/calendars/abc/events/1.evnt.json" to {
                respond("""{"v":"0.2","id":"https://meets.example/events/1.evnt.json","name":{"und":"Bar meet"}}""")
            },
            "https://meets.example/calendars/abc/events/2.evnt.json" to {
                respond("""{"v":"0.2","id":"https://meets.example/events/2.evnt.json","name":{"und":"Picnic"}}""")
            },
        ))
        val contents = CalendarLinkReader(server.client).read(link, within, ConvertOptions())
        assertEquals(
            listOf("https://meets.example/events/1.evnt.json", "https://meets.example/events/2.evnt.json"),
            contents.entries.map { it.uid },
        )
        assertEquals(listOf(mapOf("und" to "Bar meet"), mapOf("und" to "Picnic")), contents.entries.map { it.event.name })
    }

    @Test
    fun aLinkThatIsNeitherFormatFails() = runTest {
        val link = SubscribedCalendar.Link("https://example.com/page")
        val server = FakeServer(mapOf(link.url to { respond("<html></html>") }))
        assertFailsWith<EventException> { CalendarLinkReader(server.client).read(link, within, ConvertOptions()) }
    }
}
