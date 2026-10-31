package blue.deniz.vantage.core

import directory.evnt.OpenEvnt
import kotlin.test.Test
import kotlin.test.assertEquals

class EventDifferencesTest {
    private fun event(occurrences: String, extra: String = "") =
        OpenEvnt.parse("""{"v":"0.2","id":"urn:uuid:1","name":{"en":"Picnic"},"occurrences":[$occurrences]$extra}""")

    private val tenth = """{"start":"2026-07-10[Europe/Vilnius]"}"""
    private val eleventh = """{"start":"2026-07-11[Europe/Vilnius]"}"""
    private val twelfth = """{"start":"2026-07-12[Europe/Vilnius]"}"""

    @Test
    fun findsNothingInTheSameEvent() {
        assertEquals(emptyList(), event(tenth).differencesFrom(event(tenth)))
    }

    @Test
    fun tellsAMovedDayFromAnAddedOne() {
        val before = event("$tenth,$eleventh")
        val moved = event("$tenth,$twelfth").differencesFrom(before)
        assertEquals(listOf(EventDifference.OccurrenceMoved(before.occurrences!![1], event(twelfth).occurrences!![0])), moved)
        val added = event("$tenth,$eleventh,$twelfth").differencesFrom(before)
        assertEquals(listOf(EventDifference.OccurrenceAdded(event(twelfth).occurrences!![0])), added)
        val removed = event(eleventh).differencesFrom(before)
        assertEquals(listOf(EventDifference.OccurrenceRemoved(before.occurrences!![0])), removed)
    }

    @Test
    fun notesACancellationAndChangedDetails() {
        val before = event(tenth)
        val after = event(
            """{"start":"2026-07-10[Europe/Vilnius]","status":"cancelled"}""",
            ""","status":"cancelled","venue":{"name":{"en":"Park"}}""",
        )
        assertEquals(
            listOf(
                EventDifference.StatusChanged(null, OpenEvnt.Status.Cancelled),
                EventDifference.VenueChanged(null, after.venue),
                EventDifference.OccurrenceStatusChanged(before.occurrences!![0], after.occurrences!![0]),
            ),
            after.differencesFrom(before),
        )
    }
}
