package blue.deniz.vantage.core

import directory.evnt.Component
import io.ktor.client.engine.mock.respond
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class PlaceSearchTest {
    @Test
    fun readsPhotonFeaturesAsPlaces() = runTest {
        val server = FakeServer(mapOf(
            "https://photon.example/api/?q=lof&limit=6" to {
                respond(
                    """{"type":"FeatureCollection","features":[
                        {"type":"Feature","geometry":{"type":"Point","coordinates":[25.2621,54.6795]},
                         "properties":{"osm_type":"N","osm_id":4411,"name":"Loftas","street":"Švitrigailos gatvė","housenumber":"29","city":"Vilnius","countrycode":"LT"}},
                        {"type":"Feature","geometry":{"type":"Point","coordinates":[25.27,54.68]},
                         "properties":{"osm_type":"W","osm_id":77,"street":"Loftų gatvė","city":"Klaipėda","countrycode":"lt"}},
                        {"type":"Feature","properties":{"osm_type":"R","osm_id":1}}
                    ]}""",
                )
            },
        ))
        assertEquals(
            listOf(
                Place("Loftas", "Švitrigailos gatvė 29, Vilnius", "LT", Component.PlaceId("osm", "node/4411"), Component.Coordinates(54.6795, 25.2621)),
                Place("Loftų gatvė", "Klaipėda", "LT", Component.PlaceId("osm", "way/77"), Component.Coordinates(54.68, 25.27)),
            ),
            PlaceSearch(server.client, "https://photon.example/api/").search("lof"),
        )
    }

    @Test
    fun showsAStreetMadeOfSeveralWaysOnce() = runTest {
        val server = FakeServer(mapOf(
            "https://photon.example/api/?q=Vilniaus+g&limit=6" to {
                respond(
                    """{"type":"FeatureCollection","features":[
                        {"type":"Feature","geometry":{"type":"Point","coordinates":[25.2861,54.6801]},
                         "properties":{"osm_type":"W","osm_id":1237292339,"name":"Vilniaus g.","city":"Vilnius","postcode":"01119","countrycode":"LT"}},
                        {"type":"Feature","geometry":{"type":"Point","coordinates":[25.2832,54.6824]},
                         "properties":{"osm_type":"W","osm_id":4853621,"name":"Vilniaus g.","city":"Vilnius","postcode":"01103","countrycode":"LT"}},
                        {"type":"Feature","geometry":{"type":"Point","coordinates":[23.9,54.9]},
                         "properties":{"osm_type":"W","osm_id":88,"name":"Vilniaus g.","city":"Kaunas","countrycode":"LT"}}
                    ]}""",
                )
            },
        ))
        assertEquals(
            listOf(
                Place("Vilniaus g.", "Vilnius", "LT", Component.PlaceId("osm", "way/1237292339"), Component.Coordinates(54.6801, 25.2861)),
                Place("Vilniaus g.", "Kaunas", "LT", Component.PlaceId("osm", "way/88"), Component.Coordinates(54.9, 23.9)),
            ),
            PlaceSearch(server.client, "https://photon.example/api/").search("Vilniaus g"),
        )
    }

    @Test
    fun namesTheCityOfARegionalTimeZone() {
        assertEquals("Vilnius", cityNameOfTimeZone("Europe/Vilnius"))
        assertEquals("Buenos Aires", cityNameOfTimeZone("America/Argentina/Buenos_Aires"))
        assertEquals(null, cityNameOfTimeZone("UTC"))
        assertEquals(null, cityNameOfTimeZone("Etc/GMT+3"))
        assertEquals(null, cityNameOfTimeZone("US/Eastern"))
    }

    @Test
    fun findsTheCityOfATimeZoneOnce() = runTest {
        val server = FakeServer(mapOf(
            "https://photon.example/api/?q=Vilnius&limit=6&layer=city" to {
                respond(
                    """{"type":"FeatureCollection","features":[
                        {"type":"Feature","properties":{"osm_type":"R","osm_id":5}},
                        {"type":"Feature","geometry":{"type":"Point","coordinates":[25.28,54.68]},
                         "properties":{"osm_type":"R","osm_id":1529146,"name":"Vilnius","countrycode":"LT"}}
                    ]}""",
                )
            },
        ))
        val places = PlaceSearch(server.client, "https://photon.example/api/")
        val vilnius = Place("Vilnius", null, "LT", Component.PlaceId("osm", "relation/1529146"), Component.Coordinates(54.68, 25.28))
        assertEquals(vilnius, places.cityOfTimeZone("Europe/Vilnius"))
        assertEquals(vilnius, places.cityOfTimeZone("Europe/Vilnius"))
        assertEquals(1, server.requested.size)
        assertEquals(null, places.cityOfTimeZone("UTC"))
    }

    @Test
    fun findsTheAddressAtASpotAndKeepsItsExactPoint() = runTest {
        val server = FakeServer(mapOf(
            "https://photon.example/reverse?lat=54.6946&lon=25.2859&limit=5" to {
                respond(
                    """{"type":"FeatureCollection","features":[
                        {"type":"Feature","geometry":{"type":"Point","coordinates":[25.2858,54.6947]},
                         "properties":{"type":"other","osm_type":"N","osm_id":3,"name":"09312","city":"Vilnius","countrycode":"LT"}},
                        {"type":"Feature","geometry":{"type":"Point","coordinates":[25.2858,54.6947]},
                         "properties":{"type":"house","osm_type":"W","osm_id":41,"name":"Luna16","street":"Šeimyniškių g.","housenumber":"16","city":"Vilnius","countrycode":"LT"}}
                    ]}""",
                )
            },
        ))
        val spot = Component.Coordinates(54.6946, 25.2859)
        assertEquals(
            Address("Šeimyniškių g. 16, Vilnius", "LT", Component.PlaceId("osm", "way/41"), spot),
            PlaceSearch(server.client, "https://photon.example/api/").addressAt(spot),
        )
    }

    @Test
    fun listsNamedPlacesNearASpot() = runTest {
        val server = FakeServer(mapOf(
            "https://photon.example/reverse?lat=54.6946&lon=25.2859&limit=15&radius=0.3&osm_tag=amenity&osm_tag=leisure&osm_tag=tourism&osm_tag=shop" to {
                respond(
                    """{"type":"FeatureCollection","features":[
                        {"type":"Feature","geometry":{"type":"Point","coordinates":[25.2858,54.6947]},
                         "properties":{"type":"house","osm_type":"W","osm_id":41,"name":"Luna16","street":"Šeimyniškių g.","housenumber":"16","city":"Vilnius","countrycode":"LT"}},
                        {"type":"Feature","geometry":{"type":"Point","coordinates":[25.2861,54.6950]},
                         "properties":{"type":"house","osm_type":"N","osm_id":9,"street":"Rinktinės g.","housenumber":"18","city":"Vilnius","countrycode":"LT"}}
                    ]}""",
                )
            },
        ))
        assertEquals(
            listOf(Place("Luna16", "Šeimyniškių g. 16, Vilnius", "LT", Component.PlaceId("osm", "way/41"), Component.Coordinates(54.6947, 25.2858))),
            PlaceSearch(server.client, "https://photon.example/api/").placesNear(Component.Coordinates(54.6946, 25.2859)),
        )
    }
}
