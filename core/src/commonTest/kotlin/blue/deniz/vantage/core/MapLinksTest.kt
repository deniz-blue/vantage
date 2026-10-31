package blue.deniz.vantage.core

import directory.evnt.Component
import directory.evnt.Venue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MapLinksTest {
    private val mapped = Venue(address = "Gedimino pr. 9", components = listOf(Component.Coordinates(54.6872, 25.2797)))
    private val unmapped = Venue(address = "Gedimino pr. 9, Vilnius")

    @Test
    fun aPlaceWithCoordinatesIsAPointLabelledByItsName() {
        assertEquals(MapPlace.Point(54.6872, 25.2797, "Hall B"), MapPlace.of(mapped, "Hall B"))
        assertEquals(MapPlace.Point(54.6872, 25.2797, "Gedimino pr. 9"), MapPlace.of(mapped, null))
    }

    @Test
    fun aPlaceWithoutCoordinatesIsSearchedByNameAndAddress() {
        assertEquals(MapPlace.Search("Hall B, Gedimino pr. 9, Vilnius"), MapPlace.of(unmapped, "Hall B"))
        assertNull(MapPlace.of(Venue(), null))
    }

    @Test
    fun geoLinksOpenTheSystemMapsApp() {
        assertEquals("geo:54.6872,25.2797?q=54.6872,25.2797(Hall%20B)", MapLinks.geo(MapPlace.Point(54.6872, 25.2797, "Hall B")))
        assertEquals("geo:0,0?q=Hall%20B%2C%20Vilnius", MapLinks.geo(MapPlace.Search("Hall B, Vilnius")))
    }

    @Test
    fun webMapLinks() {
        val point = MapPlace.Point(54.6872, 25.2797, null)
        assertEquals("https://www.openstreetmap.org/?mlat=54.6872&mlon=25.2797#map=17/54.6872/25.2797", MapLinks.openStreetMap(point))
        assertEquals("https://www.openstreetmap.org/search?query=Hall%20B", MapLinks.openStreetMap(MapPlace.Search("Hall B")))
        assertEquals("https://www.google.com/maps/search/?api=1&query=54.6872%2C25.2797", MapLinks.googleMaps(point))
        assertEquals("https://www.google.com/maps/dir/?api=1&destination=Hall%20B", MapLinks.directions(MapPlace.Search("Hall B")))
    }
}
