package blue.deniz.vantage.core

import directory.evnt.Component
import kotlin.test.Test
import kotlin.test.assertEquals

class ComponentEditingTest {
    private val a = Component.Link("https://a.example")
    private val b = Component.Link("https://b.example")
    private val c = Component.Link("https://c.example")
    private val text = Component.PlainText(mapOf("en" to "hi"))
    private val place = Component.Coordinates(54.7, 25.3)

    @Test
    fun aLinkMovesAmongLinksWhileOtherComponentsStayPut() {
        assertEquals(listOf(text, c, place, a, b), listOf(text, a, place, b, c).withLinkMoved(2, 0))
        assertEquals(listOf(text, b, place, c, a), listOf(text, a, place, b, c).withLinkMoved(0, 2))
    }

    @Test
    fun indexesOutsideTheLinksChangeNothing() {
        val components = listOf(a, text, b)
        assertEquals(components, components.withLinkMoved(0, 2))
        assertEquals(components, components.withLinkMoved(-1, 0))
    }
}
