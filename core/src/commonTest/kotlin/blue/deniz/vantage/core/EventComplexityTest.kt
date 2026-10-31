package blue.deniz.vantage.core

import directory.evnt.Activity
import directory.evnt.Media
import directory.evnt.Occurrence
import directory.evnt.OpenEvnt
import directory.evnt.PartialDate
import directory.evnt.Venue
import kotlin.test.Test
import kotlin.test.assertEquals

private fun date(text: String) = PartialDate.parse("$text[Europe/Vilnius]")

private fun event(vararg occurrences: Occurrence, venue: Venue? = null) =
    OpenEvnt(id = "urn:uuid:test", name = mapOf("en" to "Test"), venue = venue, occurrences = occurrences.toList())

class EventComplexityTest {
    @Test
    fun aBareDayIsALine() {
        assertEquals(EventComplexity.Line, event(Occurrence(start = date("2026-11-03"))).complexity())
        assertEquals(EventComplexity.Line, event(Occurrence(start = date("2026-11-03"), end = date("2026-11-03"))).complexity())
        assertEquals(EventComplexity.Line, event().complexity())
        assertEquals(EventComplexity.Line, event(Occurrence(start = date("2026-11-03")), Occurrence(start = date("2027-11-03"))).complexity())
    }

    @Test
    fun aTimeOrAPlaceMakesItPlain() {
        assertEquals(EventComplexity.Plain, event(Occurrence(start = date("2026-10-31T20:00"))).complexity())
        assertEquals(EventComplexity.Plain, event(Occurrence(start = date("2026-11-03")), venue = Venue(name = mapOf("en" to "Luna16"))).complexity())
        assertEquals(EventComplexity.Plain, event(Occurrence(start = date("2026-10-31T22:00"), end = date("2026-11-01T04:00"))).complexity())
        assertEquals(EventComplexity.Plain, event(Occurrence(start = date("2026-11-04T18:30")), Occurrence(start = date("2026-11-11T18:30"))).complexity())
    }

    @Test
    fun aStatusMakesItPlainSoTheCardHasALineToSayIt() {
        assertEquals(EventComplexity.Plain, event().copy(status = OpenEvnt.Status.Postponed).complexity())
        assertEquals(EventComplexity.Plain, event(Occurrence(start = date("2026-11-03"), status = Occurrence.Status.Cancelled)).complexity())
    }

    @Test
    fun artSeveralDaysBackToBackDatesOrActivitiesMakeItFull() {
        val withArt = event(Occurrence(start = date("2026-11-03"))).withBackgroundSplashMedia(Media(sources = listOf(Media.Source("https://example.com/a.png"))))
        assertEquals(EventComplexity.Full, withArt.complexity())
        assertEquals(EventComplexity.Full, event(Occurrence(start = date("2026-10-30"), end = date("2026-11-08"))).complexity())
        assertEquals(EventComplexity.Full, event(Occurrence(start = date("2026-11-08")), Occurrence(start = date("2026-11-07"))).complexity())
        assertEquals(EventComplexity.Full, event(Occurrence(start = date("2026-10-30T10:00"), end = date("2026-10-30T21:00")), Occurrence(start = date("2026-10-31T10:00"), end = date("2026-10-31T21:00"))).complexity())
        assertEquals(EventComplexity.Full, event(Occurrence(start = date("2026-11-07"), activities = listOf(Activity(name = mapOf("en" to "Cosplay contest"))))).complexity())
    }
}
