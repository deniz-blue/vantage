package directory.evnt.convert

import directory.evnt.Occurrence
import directory.evnt.OpenEvnt
import directory.evnt.PartialDate
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class ICalendarEntriesTest {
    private fun calendar(vararg lines: String) =
        (listOf("BEGIN:VCALENDAR", "VERSION:2.0", "X-WR-CALNAME:Uni") + lines + "END:VCALENDAR").joinToString("\r\n")

    private val autumn = LocalDate(2026, 10, 1)..LocalDate(2026, 12, 31)
    private val options = ConvertOptions(timeZoneId = "Europe/Vilnius")

    private fun starts(event: OpenEvnt) = event.occurrences.orEmpty().map { it.start.toString() }

    @Test
    fun splitsTheCalendarByUid() {
        val text = calendar(
            "BEGIN:VEVENT", "UID:a", "SUMMARY:Physics", "DTSTART;TZID=Europe/Vilnius:20261026T181000", "DTEND;TZID=Europe/Vilnius:20261026T194500", "END:VEVENT",
            "BEGIN:VEVENT", "UID:b", "SUMMARY:Math", "DTSTART;VALUE=DATE:20261102", "DTEND;VALUE=DATE:20261103", "END:VEVENT",
        )
        val entries = ICalendar.readEntries(text, autumn, options)
        assertEquals(listOf("a", "b"), entries.map { it.uid })
        assertEquals(mapOf("en" to "Physics"), entries[0].event.name)
        assertEquals("a", entries[0].event.id)
        assertEquals(PartialDate.Day(LocalDate(2026, 11, 2), "Europe/Vilnius"), entries[1].event.occurrences?.single()?.start)
        assertEquals("Uni", ICalendar.calendarName(text))
    }

    @Test
    fun expandsAWeeklyRuleWithExceptionsAndAMovedInstance() {
        val text = calendar(
            "BEGIN:VEVENT", "UID:lab", "SUMMARY:Lab",
            "DTSTART;TZID=Europe/Vilnius:20260907T181000", "DTEND;TZID=Europe/Vilnius:20260907T194500",
            "RRULE:FREQ=WEEKLY;INTERVAL=2;UNTIL=20261130T000000Z",
            "EXDATE;TZID=Europe/Vilnius:20261019T181000",
            "END:VEVENT",
            "BEGIN:VEVENT", "UID:lab", "RECURRENCE-ID;TZID=Europe/Vilnius:20261102T181000",
            "DTSTART;TZID=Europe/Vilnius:20261103T120000", "DTEND;TZID=Europe/Vilnius:20261103T133000", "END:VEVENT",
        )
        val lab = ICalendar.readEntries(text, autumn, options).single().event
        assertEquals(
            listOf("2026-10-05T18:10[Europe/Vilnius]", "2026-11-03T12:00[Europe/Vilnius]", "2026-11-16T18:10[Europe/Vilnius]"),
            starts(lab),
        )
        assertEquals(PartialDate.parse("2026-10-05T19:45[Europe/Vilnius]"), lab.occurrences?.first()?.end)
    }

    @Test
    fun readsMonthlyWeekdayRulesAndCounts() {
        val text = calendar(
            "BEGIN:VEVENT", "UID:meet", "SUMMARY:Meet", "DTSTART;VALUE=DATE:20261003", "RRULE:FREQ=MONTHLY;BYDAY=1SA;COUNT=3", "END:VEVENT",
            "BEGIN:VEVENT", "UID:last", "SUMMARY:Rent", "DTSTART;VALUE=DATE:20261031", "RRULE:FREQ=MONTHLY;BYMONTHDAY=-1", "END:VEVENT",
        )
        val (meet, rent) = ICalendar.readEntries(text, autumn, options).map { it.event }
        assertEquals(listOf("2026-10-03[Europe/Vilnius]", "2026-11-07[Europe/Vilnius]", "2026-12-05[Europe/Vilnius]"), starts(meet))
        assertEquals(listOf("2026-10-31[Europe/Vilnius]", "2026-11-30[Europe/Vilnius]", "2026-12-31[Europe/Vilnius]"), starts(rent))
    }

    @Test
    fun leavesOutWhatEndedBeforeTheWindowAndMarksCancelledInstances() {
        val text = calendar(
            "BEGIN:VEVENT", "UID:old", "SUMMARY:Old", "DTSTART;VALUE=DATE:20200101", "END:VEVENT",
            "BEGIN:VEVENT", "UID:daily", "SUMMARY:Daily", "DTSTART;VALUE=DATE:20260101", "RRULE:FREQ=DAILY", "END:VEVENT",
            "BEGIN:VEVENT", "UID:daily", "RECURRENCE-ID;VALUE=DATE:20261002", "DTSTART;VALUE=DATE:20261002", "STATUS:CANCELLED", "END:VEVENT",
        )
        val daily = ICalendar.readEntries(text, autumn, options).single().event
        assertEquals(92, daily.occurrences?.size)
        assertEquals("2026-10-01[Europe/Vilnius]", starts(daily).first())
        assertEquals(Occurrence.Status.Cancelled, daily.occurrences?.get(1)?.status)
    }
}
