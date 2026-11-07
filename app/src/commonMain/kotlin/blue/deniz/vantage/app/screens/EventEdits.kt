package blue.deniz.vantage.app.screens

import directory.evnt.Component
import directory.evnt.Occurrence
import directory.evnt.OpenEvnt
import directory.evnt.PartialDate
import directory.evnt.Translations
import directory.evnt.ValidationIssue
import directory.evnt.Venue
import directory.evnt.withEditedText
import directory.evnt.withoutBlankTexts
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.datetime.LocalDate

internal fun Translations?.nullIfBlank() = this?.withoutBlankTexts()?.takeIf { it.isNotEmpty() }

internal fun OpenEvnt.withDescription(preferredLanguages: List<String>, text: String): OpenEvnt {
    val current = components.orEmpty()
    val index = current.indexOfFirst { it is Component.MarkdownText }
    val existing = current.getOrNull(index) as Component.MarkdownText?
    val description = Component.MarkdownText(existing?.content.withEditedText(preferredLanguages, text), existing?.flavor ?: "gfm")
    return copy(components = if (index < 0) current + description else current.toMutableList().apply { set(index, description) })
}

internal fun OpenEvnt.links() = components.orEmpty().withIndex().filter { it.value is Component.Link }

internal fun OpenEvnt.withOccurrence(index: Int, occurrence: Occurrence?): OpenEvnt =
    copy(occurrences = occurrences.orEmpty().toMutableList().apply { if (occurrence == null) removeAt(index) else set(index, occurrence) })

internal fun OpenEvnt.cleaned(): OpenEvnt = copy(
    name = name.withoutBlankTexts(),
    venue = venue?.let {
        Venue(
            name = it.name.nullIfBlank(),
            address = it.address?.takeIf(String::isNotBlank),
            countryCode = it.countryCode?.takeIf(String::isNotBlank),
            components = it.components,
        )
    }?.takeIf { it != Venue() },
    occurrences = occurrences?.filter { it.start != null }?.takeIf { it.isNotEmpty() },
    components = components?.mapNotNull {
        when (it) {
            is Component.MarkdownText -> it.content.nullIfBlank()?.let { content -> it.copy(content = content) }
            is Component.Link -> it.takeIf { link -> link.url.isNotBlank() }?.copy(name = it.name.nullIfBlank())
            else -> it
        }
    }?.takeIf { it.isNotEmpty() },
)

private val FIELD_LABELS = mapOf("url" to "URL", "countryCode" to "country", "content" to "text")

private fun fieldLabel(key: String) = FIELD_LABELS[key] ?: key

private fun OpenEvnt.componentLabel(index: Int?): String = when (val component = components?.getOrNull(index ?: -1)) {
    is Component.Link -> "Link ${links().indexOfFirst { it.index == index } + 1}"
    is Component.MarkdownText -> "Description"
    else -> component?.typeId ?: "Component"
}

internal fun OpenEvnt.describe(issue: ValidationIssue): String {
    val path = issue.path
    val index = path.getOrNull(1)?.toIntOrNull()
    val subject = when (path.firstOrNull()) {
        "name" -> "Event Name"
        "status" -> "Status"
        "venue" -> listOfNotNull("Location", path.getOrNull(1)?.let(::fieldLabel)).joinToString(" ")
        "occurrences" -> listOfNotNull("Date ${(index ?: 0) + 1}", path.getOrNull(2)?.let(::fieldLabel)).joinToString(" ")
        "components" -> listOfNotNull(componentLabel(index), path.getOrNull(2)?.let(::fieldLabel)).joinToString(" ")
        else -> path.joinToString(".")
    }
    return "$subject ${issue.message}"
}

@OptIn(ExperimentalUuidApi::class)
internal fun newBlankEvent(day: LocalDate?, timeZoneId: String) = OpenEvnt(
    id = "urn:uuid:${Uuid.random()}",
    name = emptyMap(),
    occurrences = day?.let { listOf(Occurrence(start = PartialDate.Day(it, timeZoneId))) },
)
