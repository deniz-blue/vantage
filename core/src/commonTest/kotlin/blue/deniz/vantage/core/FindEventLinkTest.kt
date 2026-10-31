package blue.deniz.vantage.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class FindEventLinkTest {
    @Test
    fun findsALinkThatIsTheWholeText() {
        assertEquals("https://example.com/picnic.ics", findEventLink("  https://example.com/picnic.ics\n"))
    }

    @Test
    fun findsALinkInsideSharedText() {
        assertEquals(
            "https://eventsl.ink/e?url=https%3A%2F%2Fexample.com%2Fa.json",
            findEventLink("Coming to this? https://eventsl.ink/e?url=https%3A%2F%2Fexample.com%2Fa.json see you there"),
        )
    }

    @Test
    fun dropsPunctuationAroundTheLink() {
        assertEquals("https://example.com/a.json", findEventLink("(https://example.com/a.json)."))
        assertEquals("https://example.com/a.json", findEventLink("<https://example.com/a.json>"))
        assertEquals("https://example.com/a.json", findEventLink("\"https://example.com/a.json\","))
    }

    @Test
    fun findsAnEventRecordUri() {
        assertEquals(
            "at://did:plc:abc/community.lexicon.calendar.event/3kq",
            findEventLink("record: at://did:plc:abc/community.lexicon.calendar.event/3kq"),
        )
    }

    @Test
    fun skipsWordsAndRecordsThatAreNotEvents() {
        assertNull(findEventLink("Picnic at the park"))
        assertNull(findEventLink(""))
        assertNull(findEventLink("at://did:plc:abc/app.bsky.feed.post/3kq"))
        assertNull(findEventLink("https://eventsl.ink/e"))
    }

    @Test
    fun takesTheFirstEventLink() {
        assertEquals(
            "https://example.com/first.json",
            findEventLink("at://did:plc:abc/app.bsky.feed.post/3kq https://example.com/first.json https://example.com/second.json"),
        )
    }
}
