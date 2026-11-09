package directory.evnt

private val LANGUAGE_NEUTRAL_KEYS = listOf("mul", "und", "zxx")

private fun String.primaryLanguage() = substringBefore('-').lowercase()

fun Translations?.shownLanguage(preferredLanguages: List<String> = listOf("en")): String? {
    val written = this?.filterValues { it.isNotEmpty() }?.keys?.takeIf { it.isNotEmpty() } ?: return null
    return preferredLanguages.firstOrNull { it in written }
        ?: preferredLanguages.firstNotNullOfOrNull { preferred -> written.firstOrNull { it.primaryLanguage() == preferred.primaryLanguage() } }
        ?: LANGUAGE_NEUTRAL_KEYS.firstOrNull { it in written }
        ?: written.first()
}

fun Translations?.translate(preferredLanguages: List<String> = listOf("en")): String =
    shownLanguage(preferredLanguages)?.let { this?.get(it) } ?: ""

fun Translations?.languageToEdit(preferredLanguages: List<String>): String =
    shownLanguage(preferredLanguages) ?: this?.keys?.firstOrNull() ?: preferredLanguages.firstOrNull() ?: "und"

fun Translations?.textToEdit(preferredLanguages: List<String>): String = textIn(languageToEdit(preferredLanguages))

fun Translations?.withEditedText(preferredLanguages: List<String>, text: String): Translations =
    withTextIn(languageToEdit(preferredLanguages), text)

fun Translations?.textIn(language: String): String = this?.get(language).orEmpty()

fun Translations?.withTextIn(language: String, text: String): Translations = orEmpty() + (language to text)

fun Translations?.writtenLanguages(): List<String> = this?.filterValues { it.isNotEmpty() }?.keys?.toList().orEmpty()

fun Translations?.hasText() = this != null && values.any { it.isNotEmpty() }

fun Translations.withoutBlankTexts(): Translations = filterValues { it.isNotBlank() }

fun mergeTranslations(vararg translations: Translations?): Translations = buildMap {
    for (entry in translations) entry?.forEach { (language, text) -> if (text.isNotEmpty()) put(language, text) }
}

fun Translations?.firstContaining(query: String): Translations? {
    val (language, text) = this?.entries?.firstOrNull { (_, text) -> text.contains(query, ignoreCase = true) } ?: return null
    return mapOf(language to text)
}
