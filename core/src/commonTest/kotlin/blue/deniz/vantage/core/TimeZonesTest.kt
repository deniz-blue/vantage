package blue.deniz.vantage.core

import directory.evnt.PartialDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Instant
import kotlinx.datetime.TimeZone

private val vilnius = TimeZone.of("Europe/Vilnius")
private val wednesdayAfternoon = Instant.parse("2026-11-04T15:00:00Z")

private fun statedIn(zoneId: String) = PartialDate.fromInstant(wednesdayAfternoon, zoneId)

class TimeZonesTest {
    @Test
    fun aTimeStatedInAnotherOffsetKeepsItsZone() {
        assertEquals("Europe/London", statedIn("Europe/London").statedTimeZoneIfOffsetDiffers(vilnius)?.id)
    }

    @Test
    fun aZoneWithTheSameOffsetIsNotWorthShowing() {
        assertNull(statedIn("Europe/Riga").statedTimeZoneIfOffsetDiffers(vilnius))
    }

    @Test
    fun utcMeansNoZoneWasStated() {
        for (id in listOf("UTC", "Etc/UTC", "Etc/GMT")) assertNull(statedIn(id).statedTimeZoneIfOffsetDiffers(vilnius), id)
    }

    @Test
    fun zonesAreLabelledByCityOrElseByOffset() {
        assertEquals("New York", timeZoneShortLabel(TimeZone.of("America/New_York"), wednesdayAfternoon))
        assertEquals("UTC-5", timeZoneShortLabel(TimeZone.of("Etc/GMT+5"), wednesdayAfternoon))
        assertEquals("UTC+5:30", timeZoneOffsetLabel(TimeZone.of("Asia/Kolkata"), wednesdayAfternoon))
    }
}
