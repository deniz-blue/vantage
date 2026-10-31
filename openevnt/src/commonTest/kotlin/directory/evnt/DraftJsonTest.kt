package directory.evnt

import kotlin.test.Test
import kotlin.test.assertEquals

class DraftJsonTest {
    private fun roundTrip(event: OpenEvnt): OpenEvnt =
        OpenEvnt.json.decodeFromString(OpenEvnt.serializer(), OpenEvnt.json.encodeToString(OpenEvnt.serializer(), event))

    @Test
    fun anEventHalfWayThroughTheFormSurvivesItsJson() {
        val draft = OpenEvnt(
            id = "urn:uuid:00000000-0000-4000-8000-000000000000",
            name = emptyMap(),
            status = OpenEvnt.Status.Postponed,
            venue = Venue(name = mapOf("en" to ""), address = " ", countryCode = "l"),
            occurrences = listOf(
                Occurrence(start = PartialDate.Month(2026, 11, "Europe/Vilnius")),
                Occurrence(start = PartialDate.Time(2026, 11, 3, 18, 30, "UTC"), end = PartialDate.Year(2027, "UTC")),
                Occurrence(),
            ),
            components = listOf(
                Component.MarkdownText(mapOf("en" to ""), "gfm"),
                Component.Link(url = "", name = mapOf("en" to "Tickets")),
                Component.Link(url = "not a url"),
            ),
        )
        assertEquals(draft, roundTrip(draft))
    }

    @Test
    fun aNewEmptyEventSurvivesItsJson() {
        val draft = OpenEvnt(id = "urn:uuid:00000000-0000-4000-8000-000000000001", name = emptyMap())
        assertEquals(draft, roundTrip(draft))
    }
}
