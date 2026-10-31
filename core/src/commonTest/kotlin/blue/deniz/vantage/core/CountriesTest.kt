package blue.deniz.vantage.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CountriesTest {
    private val countries = listOf(
        Country("DE", "Vokietija", "Germany"),
        Country("LT", "Lietuva", "Lithuania"),
        Country("LV", "Latvija", "Latvia"),
        Country("SI", "Slovėnija", "Slovenia"),
        Country("SG", "Singapūras", "Singapore"),
        Country("RU", "Rusija", "Russia"),
    )

    private fun codes(found: List<Country>) = found.map { it.code }

    @Test
    fun flagsAreRegionalIndicatorPairs() {
        assertEquals("🇱🇹", Countries.flagOf("LT"))
        assertEquals("🇩🇪", Countries.flagOf("de"))
        assertEquals("", Countries.flagOf("L1"))
        assertEquals("", Countries.flagOf("LTU"))
    }

    @Test
    fun everyCodeIsTwoUniqueLetters() {
        assertEquals(249, Countries.CODES.toSet().size)
        assertTrue(Countries.CODES.all { Countries.flagOf(it).isNotEmpty() })
    }

    @Test
    fun anEmptySearchListsEveryCountryByName() {
        assertEquals(listOf("LV", "LT", "RU", "SG", "SI", "DE"), codes(Countries.search(countries, " ")))
    }

    @Test
    fun searchFindsTheNameInEitherLanguage() {
        assertEquals(listOf("DE"), codes(Countries.search(countries, "germ")))
        assertEquals(listOf("LT"), codes(Countries.search(countries, "LIETUVA")))
    }

    @Test
    fun anExactCodeComesBeforeNamesThatStartWithItAndThoseBeforeTheRest() {
        assertEquals(listOf("SI", "SG", "RU"), codes(Countries.search(countries, "si")))
    }
}
