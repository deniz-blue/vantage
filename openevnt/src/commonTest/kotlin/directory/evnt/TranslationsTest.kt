package directory.evnt

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

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

    @Test
    fun showsAPreferredLanguageFirst() {
        assertEquals("lt", mapOf("und" to "Susitikimas", "lt" to "Susitikimas", "en" to "Meetup").shownLanguage(listOf("lt", "en")))
    }

    @Test
    fun matchesARegionalVariantByItsLanguage() {
        assertEquals("en-GB", mapOf("lt" to "Susitikimas", "en-GB" to "Meetup").shownLanguage(listOf("en")))
    }

    @Test
    fun prefersLanguageNeutralTextOverAnUnaskedLanguage() {
        assertEquals("und", mapOf("lt" to "Susitikimas", "und" to "Meetup").shownLanguage(listOf("en")))
    }

    @Test
    fun hasNoShownLanguageWithoutText() {
        assertNull(mapOf("en" to "").shownLanguage(listOf("en")))
    }

    @Test
    fun editsTheTextThatIsShown() {
        val name = mapOf("und" to "Meet at the park")
        assertEquals("Meet at the park", name.textToEdit(listOf("en")))
        assertEquals(mapOf("und" to "Meet at the lake"), name.withEditedText(listOf("en"), "Meet at the lake"))
    }

    @Test
    fun keepsEditingTheSameKeyAfterTheTextIsCleared() {
        assertEquals("und", mapOf("und" to "").languageToEdit(listOf("en")))
    }

    @Test
    fun writesNewTextInTheFirstPreferredLanguage() {
        assertEquals(mapOf("lt" to "Susitikimas"), null.withEditedText(listOf("lt", "en"), "Susitikimas"))
    }
}
