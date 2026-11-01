package blue.deniz.vantage.storage

import blue.deniz.vantage.core.CalendarMarker
import blue.deniz.vantage.core.CalendarPeriod
import blue.deniz.vantage.core.EventFormat
import blue.deniz.vantage.core.EventSource
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlin.time.Instant

const val BACKUP_FORMAT_VERSION = 1

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class Backup(
    @EncodeDefault val vantageBackup: Int = BACKUP_FORMAT_VERSION,
    val exportedAt: Instant,
    val events: List<BackupEvent> = emptyList(),
    val tags: List<BackupTag> = emptyList(),
    val markers: List<CalendarMarker> = emptyList(),
    val periods: List<CalendarPeriod> = emptyList(),
    val hiddenCalendarEvents: List<BackupHiddenCalendarEvent> = emptyList(),
    val settings: Map<String, String> = emptyMap(),
) {
    fun toJson(): String = backupJson.encodeToString(serializer(), this)

    companion object {
        fun fromJson(text: String): Backup = backupJson.decodeFromString(serializer(), text)
    }
}

@Serializable
data class BackupEvent(
    val id: String,
    val source: EventSource,
    val format: EventFormat,
    val addedAt: Instant,
    val document: JsonElement? = null,
    val tags: List<String> = emptyList(),
)

@Serializable
data class BackupTag(
    val id: String,
    val name: String,
    val color: String? = null,
    val updatedAt: Instant,
    val parents: List<String> = emptyList(),
)

@Serializable
data class BackupHiddenCalendarEvent(val source: EventSource, val title: String? = null, val startsAt: Instant? = null)

data class BackupCounts(val events: Int, val tags: Int, val markers: Int, val periods: Int)

val Backup.counts get() = BackupCounts(events.size, tags.size, markers.size, periods.size)

private val backupJson = Json {
    prettyPrint = true
    ignoreUnknownKeys = true
}

internal fun documentOf(raw: String): JsonElement = try {
    backupJson.parseToJsonElement(raw)
} catch (_: SerializationException) {
    JsonPrimitive(raw)
}

internal val BackupEvent.raw: String?
    get() = when (val document = document) {
        null -> null
        is JsonPrimitive -> if (document.isString) document.content else document.toString()
        else -> document.toString()
    }

internal fun EventSource.isKeptInBackup() = when (this) {
    is EventSource.Calendar -> false
    is EventSource.Local -> !isTestEvent
    else -> true
}
