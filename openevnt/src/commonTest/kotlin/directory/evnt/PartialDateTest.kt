package directory.evnt

import directory.evnt.PartialDate.Fill
import directory.evnt.PartialDate.Precision
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.atTime
import kotlinx.datetime.plus
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

private fun date(text: String) = PartialDate.parse(text)

private fun instant(text: String) = Instant.parse(text)

class PartialDateValidityTest {
    @Test
    fun eachPrecision() {
        assertTrue(PartialDate.isValid("2024[UTC]"))
        assertTrue(PartialDate.isValid("2024-05[UTC]"))
        assertTrue(PartialDate.isValid("2024-05-20[UTC]"))
        assertTrue(PartialDate.isValid("2024-05-20T14:30[America/New_York]"))
        assertTrue(PartialDate.isValid("2026-06-15T18:00[Etc/GMT+5]"))
    }

    @Test
    fun malformedStrings() {
        for (text in listOf("2024-1[UTC]", "2024-05-[UTC]", "2024-05-5[UTC]", "2024-05-20T14[UTC]", "2024-05-20T14:30", "20240520")) {
            assertFalse(PartialDate.isValid(text), text)
        }
    }

    @Test
    fun datesAndTimesThatDoNotExist() {
        assertFalse(PartialDate.isValid("2026-02-29[UTC]"))
        assertTrue(PartialDate.isValid("2024-02-29[UTC]"))
        assertFalse(PartialDate.isValid("2024-13[UTC]"))
        assertFalse(PartialDate.isValid("2024-04-31[UTC]"))
        assertFalse(PartialDate.isValid("2024-05-20T24:00[UTC]"))
    }

    @Test
    fun timeZonesMustBeIanaNames() {
        for (text in listOf("2026-11-14[Europe/Atlantis]", "2026-11-14[+02:00]", "2026-11-14[+0200]", "2026-11-14[Z]")) {
            assertFalse(PartialDate.isValid(text), text)
        }
    }
}

class PartialDateCreationTest {
    @Test
    fun fromString() {
        val time = assertIs<PartialDate.Time>(date("2024-05-20T14:30[America/New_York]"))
        assertEquals(PartialDate.Time(2024, 5, 20, 14, 30, "America/New_York"), time)
        assertEquals(Precision.Time, time.precision)
    }

    @Test
    fun fromFields() {
        assertEquals("2026-03[Europe/Vilnius]", PartialDate.of(2026, 3, timeZoneId = "Europe/Vilnius").toString())
        assertEquals("0812[UTC]", PartialDate.of(812, timeZoneId = "UTC").toString())
    }

    @Test
    fun fromInstantIsTheWallClockInAZone() {
        val moment = instant("2026-11-14T16:05:59Z")
        assertEquals("2026-11-14T18:05[Europe/Vilnius]", PartialDate.fromInstant(moment, "Europe/Vilnius").toString())
        assertEquals("2026-11-14T16:05[UTC]", PartialDate.fromInstant(moment, "UTC").toString())
        assertFailsWith<IllegalArgumentException> { PartialDate.fromInstant(moment, "+02:00") }
    }

    @Test
    fun eachPrecisionParsesToItsClass() {
        assertIs<PartialDate.Year>(date("2026[UTC]"))
        assertIs<PartialDate.Month>(date("2026-11[UTC]"))
        assertIs<PartialDate.Day>(date("2026-11-14[UTC]"))
        assertIs<PartialDate.Time>(date("2026-11-14T18:00[UTC]"))
    }

    @Test
    fun whenOnTheClassNarrows() {
        fun summarize(date: PartialDate) = when (date) {
            is PartialDate.Year -> "${date.year}"
            is PartialDate.Month -> "${date.year}/${date.month}"
            is PartialDate.Day -> "${date.year}/${date.month}/${date.day}"
            is PartialDate.Time -> "${date.day} ${date.hour}:${date.minute}"
        }
        assertEquals("14 18:5", summarize(date("2026-11-14T18:05[UTC]")))
    }

    @Test
    fun constructorsValidate() {
        assertEquals("2026-11-14[Europe/Vilnius]", PartialDate.Day(2026, 11, 14, "Europe/Vilnius").toString())
        assertFailsWith<IllegalArgumentException> { PartialDate.Day(2026, 2, 29, "UTC") }
        assertFailsWith<IllegalArgumentException> { PartialDate.Time(2026, 2, 28, 24, 0, "UTC") }
        assertFailsWith<IllegalArgumentException> { PartialDate.Year(2026, "+02:00") }
    }

    @Test
    fun invalidInputThrows() {
        assertFailsWith<IllegalArgumentException> { date("2026-02-29[UTC]") }
        assertFailsWith<IllegalArgumentException> { date("tomorrow") }
        assertFailsWith<IllegalArgumentException> { PartialDate.of(2026, day = 4, timeZoneId = "UTC") }
        assertFailsWith<IllegalArgumentException> { PartialDate.of(2026, 1, 4, hour = 10, timeZoneId = "UTC") }
    }

    @Serializable
    private data class Holder(val start: PartialDate)

    @Test
    fun roundTripsThroughStringsAndJson() {
        for (text in listOf("2024[UTC]", "2024-05[UTC]", "2024-05-20[UTC]", "2024-05-20T14:30[Europe/London]")) {
            assertEquals(text, date(text).toString())
            val json = Json.encodeToString(Holder(date(text)))
            assertEquals("""{"start":"$text"}""", json)
            assertEquals(date(text), Json.decodeFromString<Holder>(json).start)
        }
    }
}

class PartialDatePeriodTest {
    @Test
    fun eachPrecisionCoversItsWholePeriod() {
        val day = date("2026-11-14[Europe/Vilnius]")
        assertEquals(instant("2026-11-13T22:00:00Z"), day.startInstant)
        assertEquals(instant("2026-11-14T22:00:00Z"), day.endInstant)
        assertEquals(instant("2027-01-01T00:00:00Z"), date("2026-12[UTC]").endInstant)
        val time = date("2026-11-14T18:00[Europe/Vilnius]")
        assertEquals(instant("2026-11-14T16:00:00Z"), time.startInstant)
        assertEquals(time.startInstant, time.endInstant)
    }

    @Test
    fun aDayIs23HoursLongWhenClocksGoForward() {
        val day = date("2026-03-29[Europe/Vilnius]")
        assertEquals(23.hours, day.endInstant - day.startInstant)
    }

    @Test
    fun aSkippedTimeUsesTheOffsetFromBeforeTheChange() {
        assertEquals(date("2026-03-29T04:30[Europe/Vilnius]").startInstant, date("2026-03-29T03:30[Europe/Vilnius]").startInstant)
    }

    @Test
    fun aRepeatedTimeIsTheFirstOfTheTwo() {
        assertEquals(instant("2026-10-25T00:30:00Z"), date("2026-10-25T03:30[Europe/Vilnius]").startInstant)
    }
}

class PartialDateOrderTest {
    @Test
    fun theSpecExamplesAreNotAfter() {
        assertFalse(date("2025-11-12[Europe/London]").isAfter(date("2025-11-12T18:00[Europe/London]")))
        assertFalse(date("2025-11-12T10:00[Europe/London]").isAfter(date("2025-11-12[Europe/London]")))
    }

    @Test
    fun isAfterAndIsBefore() {
        assertTrue(date("2026[UTC]").isAfter(date("2025[UTC]")))
        assertFalse(date("2024[UTC]").isAfter(date("2024[UTC]")))
        assertFalse(date("2024-05-01[UTC]").isAfter(date("2024-05[UTC]")))
        assertTrue(date("2024-05-02T00:00[UTC]").isAfter(date("2024-05-01[UTC]")))
        assertFalse(date("2024-05-20T10:00[UTC]").isAfter(date("2024-05-20T10:00[UTC]")))
        assertTrue(date("2024-05-20T10:00[UTC]").isBefore(date("2024-05-20T10:01[UTC]")))
        assertFalse(date("2024-05[UTC]").isBefore(date("2024-05-01[UTC]")))
    }

    @Test
    fun isAfterAcrossTimeZones() {
        assertFalse(date("2026-11-14T01:00[Europe/Vilnius]").isAfter(date("2026-11-13T23:30[UTC]")))
        assertTrue(date("2026-11-14T02:00[Europe/Vilnius]").isAfter(date("2026-11-13T23:30[UTC]")))
    }

    @Test
    fun compareSortsByStartWithFinerPrecisionFirstOnATie() {
        val sorted = listOf("2026-11[UTC]", "2026-11-01[UTC]", "2026-11-01T00:00[UTC]", "2026[UTC]", "2025-12-31[UTC]")
            .map(::date)
            .sortedWith(PartialDate::compare)
            .map(PartialDate::toString)
        assertEquals(listOf("2025-12-31[UTC]", "2026[UTC]", "2026-11-01T00:00[UTC]", "2026-11-01[UTC]", "2026-11[UTC]"), sorted)
    }
}

class PartialDateContainmentTest {
    @Test
    fun contains() {
        assertTrue(date("2024-05[UTC]") in date("2024[UTC]"))
        assertTrue(date("2024-05[UTC]") in date("2024-05[UTC]"))
        assertFalse(date("2024[UTC]") in date("2024-05[UTC]"))
        assertTrue(date("2024-05-01T23:59[UTC]") in date("2024-05-01[UTC]"))
        assertFalse(date("2024-05-02T00:00[UTC]") in date("2024-05-01[UTC]"))
    }

    @Test
    fun intersects() {
        assertTrue(date("2024-05[UTC]").intersects(date("2024[UTC]")))
        assertFalse(date("2024-05[UTC]").intersects(date("2024-06[UTC]")))
        assertTrue(date("2024-05-01T10:00[UTC]").intersects(date("2024-05-01T10:00[UTC]")))
        assertFalse(date("2024-05-01T10:00[UTC]").intersects(date("2024-05-01T10:01[UTC]")))
        assertTrue(date("2024-05-01T10:00[UTC]").intersects(date("2024-05-01[UTC]")))
    }
}

class PartialDatePrecisionTest {
    @Test
    fun has() {
        assertFalse(date("2024[UTC]").has(Precision.Month))
        assertTrue(date("2024-05[UTC]").has(Precision.Month))
        assertTrue(date("2024-05-20[UTC]").has(Precision.Day))
        assertFalse(date("2024-05-20[UTC]").has(Precision.Time))
        assertTrue(date("2024-05-20T14:30[UTC]").has(Precision.Time))
    }

    @Test
    fun toACoarserPrecision() {
        val time = PartialDate.Time(2024, 5, 20, 14, 30, "UTC")
        assertEquals("2024-05-20[UTC]", time.toDay().toString())
        assertEquals("2024-05[UTC]", time.toMonth().toString())
        assertEquals("2024[UTC]", time.toYear().toString())
    }

    @Test
    fun toAFinerPrecision() {
        val year = PartialDate.Year(2024, "UTC")
        assertEquals("2024-02-29T18:00[UTC]", year.toMonth(2).toDay(29).toTime(18, 0).toString())
        assertFailsWith<IllegalArgumentException> { year.toMonth(2).toDay(30) }
    }

    @Test
    fun withPrecisionFillsTheFirstOrLastValue() {
        assertEquals("2024[UTC]", date("2024-05-20T14:30[UTC]").withPrecision(Precision.Year).toString())
        assertEquals("2024-01[UTC]", date("2024[UTC]").withPrecision(Precision.Month).toString())
        assertEquals("2024-12[UTC]", date("2024[UTC]").withPrecision(Precision.Month, Fill.Last).toString())
        assertEquals("2024-02-29[UTC]", date("2024-02[UTC]").withPrecision(Precision.Day, Fill.Last).toString())
        assertEquals("2024-05-20T00:00[UTC]", date("2024-05-20[UTC]").withPrecision(Precision.Time).toString())
        assertEquals("2024-05-20T23:59[UTC]", date("2024-05-20[UTC]").withPrecision(Precision.Time, Fill.Last).toString())
        assertEquals("2024-05-20T14:30[UTC]", date("2024-05-20T14:30[UTC]").withPrecision(Precision.Time, Fill.Last).toString())
    }

    @Test
    fun daysInMonth() {
        assertEquals(29, PartialDate.Month(2024, 2, "UTC").daysInMonth)
        assertEquals(28, PartialDate.Month(2100, 2, "UTC").daysInMonth)
    }

    @Test
    fun copyReplacesFieldsAndValidates() {
        val time = PartialDate.Time(2024, 5, 20, 14, 30, "UTC")
        assertEquals("2024-05-20T14:30[Europe/Berlin]", time.copy(timeZoneId = "Europe/Berlin").toString())
        assertEquals("2024-05-21T14:30[UTC]", time.copy(day = 21).toString())
        assertFailsWith<IllegalArgumentException> { time.copy(month = 2, day = 30) }
    }

    @Test
    fun kotlinxDatetimeValues() {
        val time = PartialDate.Time(2026, 11, 14, 18, 0, "Europe/Vilnius")
        assertEquals(LocalDate(2026, 11, 14), time.toLocalDate())
        val nextWeek = PartialDate.Time(time.toLocalDate().plus(DatePeriod(days = 7)).atTime(time.hour, time.minute), time.timeZoneId)
        assertEquals("2026-11-21T18:00[Europe/Vilnius]", nextWeek.toString())
        val day = PartialDate.Day(2026, 11, 30, "Europe/Vilnius")
        assertEquals("2026-12-01[Europe/Vilnius]", PartialDate.Day(day.toLocalDate().plus(1, DateTimeUnit.DAY), day.timeZoneId).toString())
        assertEquals(time, PartialDate.fromInstant(time.startInstant, time.timeZoneId))
    }
}
