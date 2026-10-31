package directory.evnt

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

private fun occurrence(start: String? = null, end: String? = null) =
    Occurrence(start = start?.let(PartialDate::parse), end = end?.let(PartialDate::parse))

class DateGroupsTest {
    private val istanbul = listOf(
        occurrence("2024-12-22T14:00[Europe/Istanbul]"),
        occurrence("2024-12-20T17:00[Europe/Istanbul]"),
        occurrence("2024-12-21T14:00[Europe/Istanbul]"),
    )

    private val litExpo = (22..24).map { day ->
        occurrence("2026-05-${day}T13:00[Europe/Vilnius]", "2026-05-${day}T21:00[Europe/Vilnius]")
    }

    @Test
    fun sortsOccurrencesByStart() {
        val groups = groupDates(istanbul)
        assertEquals("2024-12-20T17:00[Europe/Istanbul]", groups[0].times[0].start.toString())
        assertIs<DateGroup.Dates.Range>(groups[1].dates)
    }

    @Test
    fun consecutiveDaysWithTheSameTimesBecomeOneRange() {
        val groups = groupDates(litExpo)
        assertEquals(1, groups.size)
        assertEquals(
            DateGroup.Dates.Range(PartialDate.parse("2026-05-22[Europe/Vilnius]"), PartialDate.parse("2026-05-24[Europe/Vilnius]")),
            groups[0].dates,
        )
        assertEquals(listOf(TimeSlot(litExpo[0].start as PartialDate.Time, litExpo[0].end as PartialDate.Time)), groups[0].times)
    }

    @Test
    fun aDayWithItsOwnTimeStaysSeparate() {
        val groups = groupDates(istanbul)
        assertEquals(2, groups.size)
        assertIs<DateGroup.Dates.Single>(groups[0].dates)
        assertIs<DateGroup.Dates.Range>(groups[1].dates)
    }

    @Test
    fun withoutGroupingEveryOccurrenceIsItsOwnGroup() {
        assertEquals(3, groupDates(litExpo, groupConsecutiveDates = false).size)
        assertEquals(3, groupDates(istanbul, groupConsecutiveDates = false).size)
    }

    @Test
    fun sameTimesOnDaysApartAreListed() {
        val groups = groupDates(listOf(occurrence("2026-05-01T10:00[UTC]"), occurrence("2026-05-08T10:00[UTC]")))
        assertEquals(
            DateGroup.Dates.Days(listOf(PartialDate.parse("2026-05-01[UTC]"), PartialDate.parse("2026-05-08[UTC]"))),
            groups.single().dates,
        )
    }

    @Test
    fun oneOccurrenceOverSeveralDaysIsARange() {
        val overnight = occurrence("2027-04-03T20:00[Australia/Sydney]", "2027-04-04T06:00[Australia/Sydney]")
        assertIs<DateGroup.Dates.Range>(groupDates(listOf(overnight)).single().dates)
        assertIs<DateGroup.Dates.Single>(groupDates(listOf(occurrence("2026-06[UTC]"))).single().dates)
        assertEquals(emptyList(), groupDates(listOf(occurrence())))
    }
}

class TranslationsTest {
    private val sample = mapOf("en" to "Hello", "fr" to "Bonjour", "de" to "Hallo")

    @Test
    fun translatePicksTheFirstPreferredLanguageWithText() {
        assertEquals("Bonjour", sample.translate(listOf("fr", "en")))
        assertEquals("Bonjour", sample.translate(listOf("es", "fr")))
        assertEquals("Hello", sample.translate(listOf("es")))
        assertEquals("", null.translate())
        assertEquals("", emptyMap<String, String>().translate())
        assertEquals("Hello", mapOf("fr" to "", "en" to "Hello").translate(listOf("fr")))
    }

    @Test
    fun hasText() {
        assertEquals(false, null.hasText())
        assertEquals(false, mapOf("en" to "").hasText())
        assertEquals(true, sample.hasText())
    }

    @Test
    fun withoutBlankTexts() {
        assertEquals(mapOf("en" to "Hello"), mapOf("en" to "Hello", "fr" to "", "de" to "  ").withoutBlankTexts())
    }

    @Test
    fun mergeKeepsTheLastTextForALanguage() {
        assertEquals(mapOf("en" to "Hi", "fr" to "Bonjour"), mergeTranslations(mapOf("en" to "Hello"), mapOf("fr" to "Bonjour"), null, mapOf("en" to "Hi")))
    }

    @Test
    fun firstContainingIgnoresCase() {
        assertEquals(mapOf("fr" to "Bonjour"), sample.firstContaining("Bonjour"))
        assertEquals(mapOf("en" to "Hello"), sample.firstContaining("HELLO"))
        assertEquals(null, sample.firstContaining("xyz"))
    }
}
