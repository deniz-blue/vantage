package blue.deniz.vantage.core

import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.minutes

private const val PICNIC = """{"v":"0.2","id":"urn:uuid:7","name":{"en":"Picnic"},"occurrences":[{"start":"2026-07-10[Europe/Vilnius]"}]}"""
private const val PICNIC_URL = "https://example.com/picnic.json"

class EventPreviewsTest {
    private val picnicLink = checkNotNull(ResolvedEvent.fromLink(PICNIC_URL))

    @Test
    fun aResolvedLinkIsReusedWhileFresh() = runTest {
        val server = FakeServer(mapOf(PICNIC_URL to { respond(PICNIC) }))
        val clock = SteppedClock()
        val previews = EventPreviews(server.resolver(), clock)
        assertNull(previews.lastResolved(picnicLink))
        previews.resolve(picnicLink)
        assertEquals(mapOf("en" to "Picnic"), previews.lastResolved(picnicLink)?.data?.name)
        previews.resolve(picnicLink)
        assertEquals(1, server.requested.size)
        clock.advance(10.minutes)
        previews.resolve(picnicLink)
        assertEquals(2, server.requested.size)
    }

    @Test
    fun aLinkThatFailedToFetchIsTriedAgain() = runTest {
        var answer = HttpStatusCode.ServiceUnavailable
        val server = FakeServer(mapOf(PICNIC_URL to { respond(PICNIC, answer) }))
        val previews = EventPreviews(server.resolver(), SteppedClock())
        assertNotNull(previews.resolve(picnicLink).fetchError)
        assertNull(previews.lastResolved(picnicLink))
        answer = HttpStatusCode.OK
        assertNull(previews.resolve(picnicLink).fetchError)
        assertEquals(2, server.requested.size)
    }
}
