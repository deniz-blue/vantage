package blue.deniz.vantage.core

import directory.evnt.Occurrence
import directory.evnt.PartialDate
import directory.evnt.Venue
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

private const val VILNIUS = "Europe/Vilnius"
private val MONDAY = LocalDate(2026, 10, 26)

private fun guess(text: String) = guessEventFromText(text, MONDAY, VILNIUS, "en")

class EventTextGuessTest {
    @Test
    fun readsAnAnnouncement() {
        val guessed = guess(
            """
            Halloween Party
            Sat, Oct 31, 20:00–02:00
            📍 Loftas, Švitrigailos g. 29, Vilnius
            Tickets: https://example.com/tickets.
            """.trimIndent(),
        )
        assertEquals("Halloween Party", guessed.name)
        assertEquals(
            listOf(Occurrence(start = PartialDate.Time(2026, 10, 31, 20, 0, VILNIUS), end = PartialDate.Time(2026, 11, 1, 2, 0, VILNIUS))),
            guessed.occurrences,
        )
        assertEquals(Venue(name = mapOf("en" to "Loftas"), address = "Švitrigailos g. 29, Vilnius"), guessed.venue)
        assertEquals(listOf("https://example.com/tickets"), guessed.links)
        assertEquals("Sat, Oct 31, 20:00–02:00\n📍 Loftas, Švitrigailos g. 29, Vilnius\nTickets: https://example.com/tickets.", guessed.description)
    }

    @Test
    fun takesTheNameAndPlaceFromADateLine() {
        val guessed = guess("Board games night — 30.10 at 18:00 @ Pinavija")
        assertEquals("Board games night", guessed.name)
        assertEquals(listOf(Occurrence(start = PartialDate.Time(2026, 10, 30, 18, 0, VILNIUS))), guessed.occurrences)
        assertEquals(Venue(name = mapOf("en" to "Pinavija")), guessed.venue)
    }

    @Test
    fun readsLithuanianDates() {
        val guessed = guess("Koncertas\n2026 m. lapkričio 7 d. 19 val.\nVieta: Tamsta, A. Strazdelio g. 1")
        assertEquals("Koncertas", guessed.name)
        assertEquals(listOf(Occurrence(start = PartialDate.Time(2026, 11, 7, 19, 0, VILNIUS))), guessed.occurrences)
        assertEquals(Venue(name = mapOf("en" to "Tamsta"), address = "A. Strazdelio g. 1"), guessed.venue)
    }

    @Test
    fun aLineWithAStreetIsThePlace() {
        val guessed = guess("Halloween Rave\nSaturday, 31 October 2026, 22:00–04:00\nLoftas, Švitrigailos g. 29, Vilnius\nDress code: costumes!")
        assertEquals(Venue(name = mapOf("en" to "Loftas"), address = "Švitrigailos g. 29, Vilnius"), guessed.venue)
    }

    @Test
    fun aPlaceStartingWithTheStreetIsAllAddress() {
        val guessed = guess("Gedimino pr. 9, Vilnius\nJazz night\nNov 6, 19:00")
        assertEquals("Jazz night", guessed.name)
        assertEquals(Venue(address = "Gedimino pr. 9, Vilnius"), guessed.venue)
    }

    @Test
    fun givesATimeOnTheNextLineToTheDateAbove() {
        val guessed = guess("Workshop\nNovember 3\n10am - 1pm")
        assertEquals(
            listOf(Occurrence(start = PartialDate.Time(2026, 11, 3, 10, 0, VILNIUS), end = PartialDate.Time(2026, 11, 3, 13, 0, VILNIUS))),
            guessed.occurrences,
        )
    }

    @Test
    fun aDateWithoutAYearIsTheNextOne() {
        assertEquals(listOf(Occurrence(start = PartialDate.Day(2027, 1, 5, VILNIUS))), guess("Jan 5").occurrences)
    }

    @Test
    fun aWeekdayIsTheComingOne() {
        val guessed = guess("Party on Friday")
        assertEquals("Party", guessed.name)
        assertEquals(listOf(Occurrence(start = PartialDate.Day(2026, 10, 30, VILNIUS))), guessed.occurrences)
    }

    @Test
    fun aLithuanianDayWordIsADate() {
        val guessed = guess("Trečiadienį 18:30 kalbų mainai")
        assertEquals("kalbų mainai", guessed.name)
        assertEquals(listOf(Occurrence(start = PartialDate.Time(2026, 10, 28, 18, 30, VILNIUS))), guessed.occurrences)
        assertEquals(listOf(Occurrence(start = PartialDate.Day(2026, 10, 27, VILNIUS))), guess("Kino vakaras rytoj").occurrences)
        assertEquals(listOf(Occurrence(start = PartialDate.Day(2026, 10, 31, VILNIUS))), guess("Šeštadienį – žygis").occurrences)
    }

    @Test
    fun readsALithuanianDayRange() {
        val guessed = guess("Lapkričio 21–22 d.\nVilnius Comic Con\nLITEXPO, Laisvės pr. 5")
        assertEquals("Vilnius Comic Con", guessed.name)
        assertEquals(listOf(Occurrence(start = PartialDate.Day(2026, 11, 21, VILNIUS), end = PartialDate.Day(2026, 11, 22, VILNIUS))), guessed.occurrences)
    }

    @Test
    fun readsADayRange() {
        val guessed = guess("Festival 12–14 Nov 2026")
        assertEquals("Festival", guessed.name)
        assertEquals(listOf(Occurrence(start = PartialDate.Day(2026, 11, 12, VILNIUS), end = PartialDate.Day(2026, 11, 14, VILNIUS))), guessed.occurrences)
    }

    @Test
    fun readsTwoDatesJoinedByADash() {
        val guessed = guess("2026-12-28 – 2027-01-02")
        assertEquals(listOf(Occurrence(start = PartialDate.Day(2026, 12, 28, VILNIUS), end = PartialDate.Day(2027, 1, 2, VILNIUS))), guessed.occurrences)
    }

    @Test
    fun textWithoutDatesStillGivesANameAndDescription() {
        val guessed = guess("Book club\nWe read Piranesi this time.")
        assertEquals("Book club", guessed.name)
        assertEquals(emptyList(), guessed.occurrences)
        assertNull(guessed.venue)
        assertEquals("We read Piranesi this time.", guessed.description)
    }

    @Test
    fun aVersionNumberIsNotADate() {
        assertEquals(emptyList(), guess("Release party for v1.2").occurrences)
    }
}
