package blue.deniz.vantage.core

import io.ktor.http.Url
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class EventsLinkEditTest {
    private val request = EventsLink.EditRequest(EventsLink.Intent(url = "https://example.com/a.evnt.json"), "https://site.example/done?x=1#old", "s 1")

    @Test
    fun readsAnEditLinkFromEventsLinkAndFromAnAppRedirect() {
        val direct = Url("https://eventsl.ink/edit?url=https%3A%2F%2Fexample.com%2Fa.evnt.json&return=https%3A%2F%2Fsite.example%2Fdone%3Fx%3D1%23old&state=s%201")
        val redirected = Url("https://vantage.tsx.lt/?type=edit&url=https%3A%2F%2Fexample.com%2Fa.evnt.json&return=https%3A%2F%2Fsite.example%2Fdone%3Fx%3D1%23old&state=s%201")
        assertEquals(request, EventsLink.parseEditRequest(direct))
        assertEquals(request, EventsLink.parseEditRequest(redirected))
        assertEquals("site.example", request.returnHost)
    }

    @Test
    fun anEditLinkNeedsAWebAddressToReturnTo() {
        assertNull(EventsLink.parseEditRequest(Url("https://eventsl.ink/edit?url=https%3A%2F%2Fexample.com%2Fa.evnt.json")))
        assertNull(EventsLink.parseEditRequest(Url("https://eventsl.ink/edit?return=javascript%3Aalert(1)")))
        assertNull(EventsLink.parseEditRequest(Url("https://eventsl.ink/e?url=https%3A%2F%2Fexample.com%2Fa.evnt.json&return=https%3A%2F%2Fsite.example")))
    }

    @Test
    fun repliesGoBackInTheReturnAddressFragment() {
        assertEquals(
            "https://site.example/done?x=1#event=%7B%22v%22%3A%220.2%22%7D&state=s+1",
            EventsLink.returnLinkFor(EventsLink.EditReply.Saved(request, """{"v":"0.2"}""")),
        )
        assertEquals(
            "https://site.example/done?x=1#cancelled=true",
            EventsLink.returnLinkFor(EventsLink.EditReply.Cancelled(request.copy(state = null))),
        )
    }

    @Test
    fun aPopupReplyCarriesTheEventAsJson() {
        val message = Json.parseToJsonElement(EventsLink.replyMessageFor(EventsLink.EditReply.Saved(request, """{"v":"0.2"}"""))).jsonObject
        assertEquals("evnt-edit", message["type"]?.jsonPrimitive?.content)
        assertEquals("0.2", message["event"]?.jsonObject?.get("v")?.jsonPrimitive?.content)
        assertEquals("s 1", message["state"]?.jsonPrimitive?.content)
    }
}
