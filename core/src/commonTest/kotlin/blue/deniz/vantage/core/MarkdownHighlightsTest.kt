package blue.deniz.vantage.core

import blue.deniz.vantage.core.MarkdownHighlight.Kind
import kotlin.test.Test
import kotlin.test.assertEquals

class MarkdownHighlightsTest {
    private fun highlighted(text: String, kind: Kind) =
        Markdown.highlight(text).filter { it.kind == kind }.sortedBy { it.start }.map { text.substring(it.start, it.end).trim() }

    @Test
    fun emphasisKeepsItsMarkersApart() {
        val text = "**bold**, *italic* and ~~gone~~"
        assertEquals(listOf("**bold**"), highlighted(text, Kind.Bold))
        assertEquals(listOf("*italic*"), highlighted(text, Kind.Italic))
        assertEquals(listOf("~~gone~~"), highlighted(text, Kind.Strikethrough))
        assertEquals(listOf("**", "**", "*", "*", "~~", "~~"), highlighted(text, Kind.Marker))
    }

    @Test
    fun loneAsterisksAreText() {
        assertEquals(emptyList(), Markdown.highlight("2 * 3 * 4"))
    }

    @Test
    fun headingsKnowTheirLevel() {
        val text = "# One\n\n### Three"
        assertEquals(listOf("# One"), highlighted(text, Kind.Heading(1)))
        assertEquals(listOf("### Three"), highlighted(text, Kind.Heading(3)))
        assertEquals(listOf("#", "###"), highlighted(text, Kind.Marker))
    }

    @Test
    fun linkTextIsTheLinkAndTheRestIsMarkers() {
        val text = "see [the site](https://example.com) or <https://a.b>"
        assertEquals(listOf("the site", "<https://a.b>"), highlighted(text, Kind.Link))
        assertEquals("[](https://example.com)", highlighted(text, Kind.Marker).joinToString(""))
    }

    @Test
    fun codeListsAndQuotes() {
        val text = "- `x`\n1. two\n\n> said"
        assertEquals(listOf("`x`"), highlighted(text, Kind.Code))
        assertEquals(listOf("> said"), highlighted(text, Kind.Quote))
        assertEquals(listOf("- `", "`", "1.", ">"), highlighted(text, Kind.Marker))
    }
}
