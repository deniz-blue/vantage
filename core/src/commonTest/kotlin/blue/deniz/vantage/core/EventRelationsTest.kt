package blue.deniz.vantage.core

import directory.evnt.Component
import directory.evnt.OpenEvnt
import kotlin.test.Test
import kotlin.test.assertEquals

private fun stored(id: String, vararg relations: Component.Relation) =
    ResolvedEvent(id = id, data = OpenEvnt(id = "urn:uuid:$id", name = mapOf("en" to id), components = relations.toList().ifEmpty { null }))

class EventRelationsTest {
    private val convention = stored("convention")
    private val meetup = stored("meetup", Component.Relation(Component.Relation.PARENT, "urn:uuid:convention"), Component.Relation("com.example.rivalOf", "https://example.org/rival.evnt.json"))
    private val library = listOf(convention, meetup)

    @Test
    fun statedRelationsFindTheirEventInTheLibrary() {
        val related = meetup.relatedEvents(library)
        assertEquals(listOf(Component.Relation.PARENT to "convention", "com.example.rivalOf" to null), related.map { it.type to it.event?.id })
    }

    @Test
    fun anEventPointedAtShowsTheReverseRelation() {
        val related = convention.relatedEvents(library)
        assertEquals(listOf(Component.Relation.CHILD to "meetup"), related.map { it.type to it.event?.id })
    }
}
