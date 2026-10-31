package blue.deniz.vantage.core

import io.ktor.http.Url
import kotlin.test.Test
import kotlin.test.assertEquals

class AppSchemesTest {
    @Test
    fun theHostOfAnAppSchemeLinkIsItsFirstPathSegment() {
        assertEquals(Url("https://vantage.tsx.lt/event/abc/edit"), AppSchemes.webAddressOf(Url("vantage://event/abc/edit")))
        assertEquals(Url("https://vantage.tsx.lt/calendar"), AppSchemes.webAddressOf(Url("vantage://calendar")))
    }

    @Test
    fun queryParametersSurvive() {
        val url = AppSchemes.webAddressOf(Url("evnt://feed?type=index&url=https%3A%2F%2Fa.example%2Findex.json"))
        assertEquals(listOf("feed"), url.segments)
        assertEquals("index", url.parameters["type"])
        assertEquals("https://a.example/index.json", url.parameters["url"])
    }

    @Test
    fun anEmptyHostLeavesThePathAlone() {
        assertEquals(listOf("list"), AppSchemes.webAddressOf(Url("web+evnt:///list")).segments)
    }

    @Test
    fun otherSchemesAreUnchanged() {
        val url = Url("https://eventsl.ink/e?url=https%3A%2F%2Fa.example")
        assertEquals(url, AppSchemes.webAddressOf(url))
    }
}
