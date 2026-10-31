package directory.evnt

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull

@Serializable
data class ValidationIssue(val path: List<String>, val message: String) {
    override fun toString() = if (path.isEmpty()) message else "${path.joinToString(".")}: $message"
}

class InvalidOpenEvntException(val issues: List<ValidationIssue>) :
    IllegalArgumentException(issues.joinToString("\n"))

private typealias Check = (value: JsonElement, path: List<String>, issues: MutableList<ValidationIssue>) -> Unit

private val COMPONENT_TYPE_PATTERN = Regex("""[A-Za-z0-9_-]+(\.[A-Za-z0-9_-]+)+#(0|[1-9][0-9]*)""")
private val LANGUAGE_TAG_PATTERN = Regex("""[A-Za-z]{1,8}(-[A-Za-z0-9]{1,8})*""")
private val URI_SCHEME_PATTERN = Regex("""([A-Za-z][A-Za-z0-9+.-]*):(.*)""")
private val SCHEMES_WITH_HOST = setOf("http", "https", "ws", "wss", "ftp")

private fun JsonElement.stringOrNull() = (this as? JsonPrimitive)?.takeIf { it.isString }?.content

private fun JsonElement.numberOrNull() =
    (this as? JsonPrimitive)?.takeIf { !it.isString && it !is JsonNull && it.booleanOrNull == null }?.doubleOrNull

private fun isAbsoluteUri(text: String): Boolean {
    val (scheme, rest) = URI_SCHEME_PATTERN.matchEntire(text)?.destructured ?: return false
    if (text.any { it.isWhitespace() || it.isISOControl() }) return false
    if (scheme.lowercase() !in SCHEMES_WITH_HOST) return true
    return rest.removePrefix("//").takeWhile { it !in "/?#" }.substringAfterLast('@').isNotEmpty()
}

private val string: Check = { value, path, issues ->
    if (value.stringOrNull() == null) issues += ValidationIssue(path, "must be a string")
}

private val boolean: Check = { value, path, issues ->
    if ((value as? JsonPrimitive)?.takeIf { !it.isString }?.booleanOrNull == null)
        issues += ValidationIssue(path, "must be true or false")
}

private fun numberWhere(description: String, isAllowed: (Double) -> Boolean): Check = { value, path, issues ->
    val number = value.numberOrNull()
    if (number == null || !number.isFinite() || !isAllowed(number)) issues += ValidationIssue(path, "must be $description")
}

private fun matching(pattern: Regex, description: String): Check = { value, path, issues ->
    if (value.stringOrNull()?.let(pattern::matches) != true) issues += ValidationIssue(path, "must be $description")
}

private fun oneOf(vararg allowed: String): Check = { value, path, issues ->
    if (value.stringOrNull() !in allowed)
        issues += ValidationIssue(path, "must be ${allowed.joinToString(" or ") { "\"$it\"" }}")
}

private fun arrayOf(item: Check, nonEmpty: Boolean = false): Check = { value, path, issues ->
    if (value !is JsonArray) {
        issues += ValidationIssue(path, "must be an array")
    } else {
        if (nonEmpty && value.isEmpty()) issues += ValidationIssue(path, "must have at least one item")
        value.forEachIndexed { index, element -> item(element, path + index.toString(), issues) }
    }
}

private fun objectOf(
    required: Map<String, Check> = emptyMap(),
    optional: Map<String, Check> = emptyMap(),
    closed: Boolean = false,
    nonEmpty: Boolean = false,
): Check = { value, path, issues ->
    if (value !is JsonObject) {
        issues += ValidationIssue(path, "must be an object")
    } else {
        for ((key, check) in required) {
            val field = value[key]
            if (field != null) check(field, path + key, issues) else issues += ValidationIssue(path + key, "is required")
        }
        for ((key, check) in optional) value[key]?.let { check(it, path + key, issues) }
        if (closed) {
            for (key in value.keys) {
                if (key !in required && key !in optional) issues += ValidationIssue(path + key, "is not a field of this object")
            }
        }
        if (nonEmpty && value.isEmpty()) issues += ValidationIssue(path, "must have at least one field")
    }
}

private val translations: Check = { value, path, issues ->
    if (value !is JsonObject) {
        issues += ValidationIssue(path, "must be an object of language tags to text")
    } else {
        if (value.isEmpty()) issues += ValidationIssue(path, "must have text in at least one language")
        val seen = mutableMapOf<String, String>()
        for ((key, text) in value) {
            if (!LANGUAGE_TAG_PATTERN.matches(key))
                issues += ValidationIssue(path + key, "must be a BCP 47 language tag such as en or zh-Hant")
            val earlier = seen[key.lowercase()]
            if (earlier != null) issues += ValidationIssue(path + key, "differs from $earlier only in case")
            else seen[key.lowercase()] = key
            if (text.stringOrNull().isNullOrEmpty()) issues += ValidationIssue(path + key, "must be text that is not empty")
        }
    }
}

private val absoluteUri: Check = { value, path, issues ->
    if (value.stringOrNull()?.let(::isAbsoluteUri) != true)
        issues += ValidationIssue(path, "must be an absolute URI such as https://example.com/events/1")
}

private val partialDate: Check = { value, path, issues ->
    if (value.stringOrNull()?.let(PartialDate::isValid) != true)
        issues += ValidationIssue(path, "must be a PartialDate such as 2026-06-15T09:00[Europe/Vilnius]")
}

private val anyNumber = numberWhere("a number") { true }

private val mediaSource = objectOf(
    required = mapOf("url" to absoluteUri),
    optional = mapOf(
        "mimeType" to string,
        "dimensions" to objectOf(required = mapOf("width" to anyNumber, "height" to anyNumber)),
    ),
)

private val media = objectOf(
    required = mapOf("sources" to arrayOf(mediaSource, nonEmpty = true)),
    optional = mapOf(
        "alt" to translations,
        "presentation" to objectOf(
            optional = mapOf(
                "blurhash" to string,
                "dominantColor" to matching(Regex("#[0-9A-Fa-f]{6}"), "a hex colour such as #1a1a2e"),
            ),
        ),
    ),
)

private val KNOWN_COMPONENT_CHECKS: Map<String, Check> = mapOf(
    Component.MarkdownText.TYPE_ID to objectOf(required = mapOf("content" to translations), optional = mapOf("flavor" to string)),
    Component.PlainText.TYPE_ID to objectOf(required = mapOf("content" to translations)),
    Component.Link.TYPE_ID to objectOf(
        required = mapOf("url" to absoluteUri),
        optional = mapOf("name" to translations, "disabled" to boolean),
    ),
    Component.Source.TYPE_ID to objectOf(required = mapOf("url" to absoluteUri)),
    Component.SplashMedia.TYPE_ID to objectOf(required = mapOf("media" to media), optional = mapOf("roles" to arrayOf(string))),
    Component.Languages.TYPE_ID to objectOf(
        required = mapOf(
            "languages" to arrayOf(
                objectOf(required = mapOf("code" to matching(LANGUAGE_TAG_PATTERN, "a BCP 47 language tag such as en"))),
            ),
        ),
    ),
    Component.PlaceId.TYPE_ID to objectOf(required = mapOf("type" to string, "id" to string)),
    Component.Relation.TYPE_ID to objectOf(required = mapOf("type" to string, "target" to absoluteUri)),
    Component.Coordinates.TYPE_ID to objectOf(
        required = mapOf(
            "latitude" to numberWhere("a number from -90 to 90") { it in -90.0..90.0 },
            "longitude" to numberWhere("a number from -180 to 180") { it in -180.0..180.0 },
        ),
        optional = mapOf("uncertainty" to numberWhere("a number above 0") { it > 0 }),
    ),
)

private val component: Check = { value, path, issues ->
    if (value !is JsonObject) {
        issues += ValidationIssue(path, "must be a component object with a \$type")
    } else {
        val typeField = value["\$type"]
        val typeId = typeField?.stringOrNull()
        when {
            typeField == null -> issues += ValidationIssue(path + "\$type", "is required")
            typeId == null || !COMPONENT_TYPE_PATTERN.matches(typeId) -> issues += ValidationIssue(
                path + "\$type",
                "must be a reverse domain name and a version, such as com.example.ticketTier#0",
            )
            else -> KNOWN_COMPONENT_CHECKS[typeId]?.invoke(value, path, issues)
        }
    }
}

private val components = arrayOf(component)

private val venueComponents: Check = { value, path, issues ->
    components(value, path, issues)
    val coordinates = (value as? JsonArray)?.count { (it as? JsonObject)?.get("\$type")?.stringOrNull() == Component.Coordinates.TYPE_ID }
    if ((coordinates ?: 0) > 1) issues += ValidationIssue(path, "must not hold more than one coordinates component")
}

private val venue = objectOf(
    optional = mapOf(
        "name" to translations,
        "address" to string,
        "countryCode" to matching(Regex("[A-Z]{2}"), "an uppercase ISO 3166-1 alpha-2 code such as LT"),
        "components" to venueComponents,
    ),
    closed = true,
    nonEmpty = true,
)

private val activity = objectOf(
    required = mapOf("name" to translations),
    optional = mapOf("status" to oneOf("cancelled")),
    closed = true,
)

private val occurrenceFields = objectOf(
    optional = mapOf(
        "start" to partialDate,
        "end" to partialDate,
        "status" to oneOf("cancelled"),
        "activities" to arrayOf(activity),
        "components" to components,
    ),
    closed = true,
)

private val occurrence: Check = { value, path, issues ->
    occurrenceFields(value, path, issues)
    val start = (value as? JsonObject)?.get("start")?.stringOrNull()?.let(PartialDate::parseOrNull)
    val end = (value as? JsonObject)?.get("end")?.stringOrNull()?.let(PartialDate::parseOrNull)
    if (start != null && end != null && start.isAfter(end)) issues += ValidationIssue(path + "start", "must not be after end")
}

private val openEvnt = objectOf(
    required = mapOf("v" to oneOf(OpenEvnt.VERSION), "id" to absoluteUri, "name" to translations),
    optional = mapOf(
        "\$type" to oneOf(OpenEvnt.AT_RECORD_TYPE),
        "status" to oneOf("uncertain", "cancelled", "postponed"),
        "venue" to venue,
        "occurrences" to arrayOf(occurrence),
        "components" to components,
    ),
    closed = true,
)

internal fun validateOpenEvnt(element: JsonElement): List<ValidationIssue> =
    mutableListOf<ValidationIssue>().also { openEvnt(element, emptyList(), it) }
