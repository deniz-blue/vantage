package directory.evnt.convert

import directory.evnt.Component
import directory.evnt.PartialDate
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

data class ConvertOptions(
    val language: String = "en",
    val timeZoneId: String = "UTC",
    val did: String? = null,
    val id: String? = null,
)

@OptIn(ExperimentalUuidApi::class)
internal fun ConvertOptions.documentId() = id ?: "urn:uuid:${Uuid.random()}"

internal fun <Item> List<Item>.orNullIfEmpty() = ifEmpty { null }

internal fun textIn(language: String, text: String?) = text?.takeIf { it.isNotEmpty() }?.let { mapOf(language to it) }

internal fun linkTo(url: String, name: String?, language: String) = Component.Link(url, textIn(language, name))

internal fun plainText(text: String, language: String) = Component.PlainText(mapOf(language to text))

private val WRITTEN_DATE_PATTERN = Regex(
    """(\d{4})(?:-(\d{1,2})(?:-(\d{1,2})(?:[T ](\d{1,2}):(\d{2})(?::(\d{2})(?:[.,]\d+)?)?(Z|[+-]\d{1,2}(?::?\d{2})?)?)?)?)?""",
    RegexOption.IGNORE_CASE,
)

private val OFFSET_PATTERN = Regex("""([+-])(\d{1,2}):?(\d{2})?""")

private fun offsetSeconds(offset: String): Int {
    if (offset.equals("Z", ignoreCase = true)) return 0
    val (sign, hours, minutes) = OFFSET_PATTERN.matchEntire(offset)!!.destructured
    return (if (sign == "-") -1 else 1) * (hours.toInt() * 60 + (minutes.toIntOrNull() ?: 0)) * 60
}

private fun PartialDate.Time.dayIfMidnight(): PartialDate = if (hour == 0 && minute == 0) toDay() else this

fun parseWrittenDate(value: String, timeZoneId: String = "UTC"): PartialDate? {
    PartialDate.parseOrNull(value)?.let { return it }
    return try {
        val written = WRITTEN_DATE_PATTERN.matchEntire(value.trim())?.groupValues
            ?: return PartialDate.fromInstant(Instant.parse(value), timeZoneId).dayIfMidnight()
        fun number(index: Int) = written[index].takeIf { it.isNotEmpty() }?.toInt()
        val year = written[1].toInt()
        val hour = number(4) ?: return PartialDate.of(year, number(2), number(3), timeZoneId = timeZoneId)
        val offset = written[7].takeIf { it.isNotEmpty() }
            ?: return PartialDate.Time(year, number(2)!!, number(3)!!, hour, number(5)!!, timeZoneId).dayIfMidnight()
        val wallClockInUtc = PartialDate.Time(year, number(2)!!, number(3)!!, hour, number(5)!!, "UTC").startInstant
        val instant = Instant.fromEpochSeconds(wallClockInUtc.epochSeconds + (number(6) ?: 0) - offsetSeconds(offset))
        PartialDate.fromInstant(instant, timeZoneId).dayIfMidnight()
    } catch (_: IllegalArgumentException) {
        null
    }
}
