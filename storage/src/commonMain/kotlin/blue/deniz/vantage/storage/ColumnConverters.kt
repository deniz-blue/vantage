package blue.deniz.vantage.storage

import androidx.room3.ColumnTypeConverter
import blue.deniz.vantage.core.EventError
import blue.deniz.vantage.core.EventFormat
import blue.deniz.vantage.core.EventSource
import blue.deniz.vantage.core.Revision
import directory.evnt.OpenEvnt
import directory.evnt.PartialDate
import kotlin.time.Instant

private val json = OpenEvnt.json

internal val EventFormat.serialName get() = EventFormat.serializer().descriptor.getElementName(ordinal)

object ColumnConverters {
    @ColumnTypeConverter
    fun instantToEpochMilliseconds(value: Instant): Long = value.toEpochMilliseconds()

    @ColumnTypeConverter
    fun epochMillisecondsToInstant(value: Long): Instant = Instant.fromEpochMilliseconds(value)

    @ColumnTypeConverter
    fun sourceToJson(value: EventSource): String = json.encodeToString(EventSource.serializer(), value)

    @ColumnTypeConverter
    fun jsonToSource(value: String): EventSource = json.decodeFromString(EventSource.serializer(), value)

    @ColumnTypeConverter
    fun formatToName(value: EventFormat): String = value.serialName

    @ColumnTypeConverter
    fun nameToFormat(value: String): EventFormat = EventFormat.entries.firstOrNull { it.serialName == value } ?: EventFormat.Unknown

    @ColumnTypeConverter
    fun eventToJson(value: OpenEvnt): String = json.encodeToString(OpenEvnt.serializer(), value)

    @ColumnTypeConverter
    fun jsonToEvent(value: String): OpenEvnt = json.decodeFromString(OpenEvnt.serializer(), value)

    @ColumnTypeConverter
    fun errorToJson(value: EventError): String = json.encodeToString(EventError.serializer(), value)

    @ColumnTypeConverter
    fun jsonToError(value: String): EventError = json.decodeFromString(EventError.serializer(), value)

    @ColumnTypeConverter
    fun partialDateToText(value: PartialDate): String = value.toString()

    @ColumnTypeConverter
    fun textToPartialDate(value: String): PartialDate = PartialDate.parse(value)

    @ColumnTypeConverter
    fun revisionToJson(value: Revision): String = json.encodeToString(Revision.serializer(), value)

    @ColumnTypeConverter
    fun jsonToRevision(value: String): Revision = json.decodeFromString(Revision.serializer(), value)
}
