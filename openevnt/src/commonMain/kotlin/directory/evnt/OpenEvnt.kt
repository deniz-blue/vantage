package directory.evnt

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

typealias Translations = Map<String, String>

@Serializable
data class OpenEvnt(
    val v: String = VERSION,
    val id: String,
    val name: Translations,
    val status: Status? = null,
    val venue: Venue? = null,
    val occurrences: List<Occurrence>? = null,
    val components: List<Component>? = null,
) {
    @Serializable
    enum class Status {
        @SerialName("uncertain") Uncertain,
        @SerialName("cancelled") Cancelled,
        @SerialName("postponed") Postponed,
    }

    fun toJson(): JsonElement = json.encodeToJsonElement(serializer(), this)

    companion object {
        const val VERSION = "0.2"
        const val AT_RECORD_TYPE = "directory.evnt.event"

        val json = Json {
            ignoreUnknownKeys = true
            explicitNulls = false
            encodeDefaults = true
        }

        fun issuesOf(element: JsonElement): List<ValidationIssue> = validateOpenEvnt(element)

        fun parse(element: JsonElement): OpenEvnt {
            val issues = issuesOf(element)
            if (issues.isNotEmpty()) throw InvalidOpenEvntException(issues)
            return json.decodeFromJsonElement(serializer(), element)
        }

        fun parse(text: String): OpenEvnt = parse(json.parseToJsonElement(text))
    }
}

@Serializable
data class Venue(
    val name: Translations? = null,
    val address: String? = null,
    val countryCode: String? = null,
    val components: List<Component>? = null,
)

@Serializable
data class Occurrence(
    val start: PartialDate? = null,
    val end: PartialDate? = null,
    val status: Status? = null,
    val activities: List<Activity>? = null,
    val components: List<Component>? = null,
) {
    @Serializable
    enum class Status { @SerialName("cancelled") Cancelled }
}

@Serializable
data class Activity(val name: Translations, val status: Status? = null) {
    @Serializable
    enum class Status { @SerialName("cancelled") Cancelled }
}

@Serializable
data class Media(
    val sources: List<Source>,
    val alt: Translations? = null,
    val presentation: Presentation? = null,
) {
    @Serializable
    data class Source(val url: String, val mimeType: String? = null, val dimensions: Dimensions? = null)

    @Serializable
    data class Dimensions(val width: Double, val height: Double)

    @Serializable
    data class Presentation(val blurhash: String? = null, val dominantColor: String? = null)
}
