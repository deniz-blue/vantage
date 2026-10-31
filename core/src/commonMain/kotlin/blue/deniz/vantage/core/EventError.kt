package blue.deniz.vantage.core

import directory.evnt.ValidationIssue
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class EventError(
    val kind: Kind,
    val message: String,
    val status: Int? = null,
    val issues: List<ValidationIssue>? = null,
) {
    @Serializable
    enum class Kind {
        @SerialName("fetch")
        Fetch,

        @SerialName("xrpc")
        Xrpc,

        @SerialName("json-parse")
        JsonParse,

        @SerialName("parse-error")
        Parse,

        @SerialName("validation")
        Validation,

        @SerialName("unknown-format")
        UnknownFormat,

        @SerialName("unknown")
        Unknown,
    }
}

class EventException(val error: EventError) : Exception(error.message)
