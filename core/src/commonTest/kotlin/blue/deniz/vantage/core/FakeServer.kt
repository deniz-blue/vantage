package blue.deniz.vantage.core

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpStatusCode
import kotlin.time.Clock
import kotlin.time.Instant

internal object FixedClock : Clock {
    override fun now() = Instant.parse("2026-10-21T00:00:00Z")
}

internal class FakeServer(private val routes: Map<String, MockRequestHandleScope.(HttpRequestData) -> HttpResponseData>) {
    val requested = mutableListOf<String>()

    val client = HttpClient(MockEngine { request ->
        val url = request.url.toString()
        requested += url
        routes[url]?.invoke(this, request) ?: respond("missing", HttpStatusCode.NotFound)
    })

    fun resolver() = EventResolver(EventFetcher(client), clock = FixedClock)
}

internal class SteppedClock(private var current: Instant = Instant.parse("2026-10-21T00:00:00Z")) : Clock {
    override fun now() = current

    fun advance(by: kotlin.time.Duration) {
        current += by
    }
}
