package directory.evnt

fun Translations?.translate(preferredLanguages: List<String> = listOf("en")): String {
    if (this == null) return ""
    for (language in preferredLanguages) this[language]?.takeIf { it.isNotEmpty() }?.let { return it }
    return values.firstOrNull { it.isNotEmpty() } ?: ""
}

fun Translations?.hasText() = this != null && values.any { it.isNotEmpty() }

fun Translations.withoutBlankTexts(): Translations = filterValues { it.isNotBlank() }

fun mergeTranslations(vararg translations: Translations?): Translations = buildMap {
    for (entry in translations) entry?.forEach { (language, text) -> if (text.isNotEmpty()) put(language, text) }
}

fun Translations?.firstContaining(query: String): Translations? {
    val (language, text) = this?.entries?.firstOrNull { (_, text) -> text.contains(query, ignoreCase = true) } ?: return null
    return mapOf(language to text)
}
