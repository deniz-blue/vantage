package blue.deniz.vantage.core

import directory.evnt.Component
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

    @Test
    fun readsAnOffsetTypedIntoSearch() {
        assertEquals("UTC+2", offsetLabelOfQuery("+2"))
        assertEquals("UTC-5", offsetLabelOfQuery("utc-5"))
        assertEquals("UTC+5:30", offsetLabelOfQuery("GMT+5:30"))
        assertEquals("UTC+5:45", offsetLabelOfQuery("+0545"))
        assertEquals("UTC", offsetLabelOfQuery("utc"))
        assertEquals("UTC", offsetLabelOfQuery("+0"))
        assertEquals(null, offsetLabelOfQuery("Vilnius"))
    }

    @Test
    fun namesAZoneByFlagAndCity() {
        assertEquals("🇱🇹  Vilnius · UTC+2", timeZoneDisplayName(TimeZone.of("Europe/Vilnius"), wednesdayAfternoon))
        assertEquals("UTC", timeZoneDisplayName(TimeZone.UTC, wednesdayAfternoon))
    }

    @Test
    fun findsTheZoneOfAPlace() {
        assertEquals("Europe/Vilnius", CountryTimeZones.ofPlace("LT", null)?.id)
        assertEquals("America/Los_Angeles", CountryTimeZones.ofPlace("US", Component.Coordinates(34.05, -118.25))?.id)
        assertEquals(null, CountryTimeZones.ofPlace("US", null))
        assertEquals("Asia/Tokyo", CountryTimeZones.ofPlace(null, Component.Coordinates(35.6298, 139.7942))?.id)
    }
}
