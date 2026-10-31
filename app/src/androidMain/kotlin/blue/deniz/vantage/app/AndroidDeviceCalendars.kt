package blue.deniz.vantage.app

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract.Calendars
import android.provider.CalendarContract.Events
import android.provider.CalendarContract.Instances
import blue.deniz.vantage.core.CalendarEntry
import blue.deniz.vantage.core.DeviceCalendar
import blue.deniz.vantage.core.DeviceCalendars
import directory.evnt.Component
import directory.evnt.Occurrence
import directory.evnt.OpenEvnt
import directory.evnt.PartialDate
import directory.evnt.Venue
import directory.evnt.convert.ConvertOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

private class InstanceRow(
    val eventId: Long,
    val begin: Instant,
    val end: Instant,
    val allDay: Boolean,
    val title: String?,
    val description: String?,
    val location: String?,
    val timeZoneId: String?,
    val cancelled: Boolean,
)

private val INSTANCE_COLUMNS = arrayOf(
    Instances.EVENT_ID, Instances.BEGIN, Instances.END, Instances.ALL_DAY, Instances.TITLE,
    Instances.DESCRIPTION, Instances.EVENT_LOCATION, Instances.EVENT_TIMEZONE, Instances.STATUS,
)

class AndroidDeviceCalendars(private val context: Context) : DeviceCalendars {
    override fun hasPermission() = context.checkSelfPermission(Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED

    override suspend fun list(): List<DeviceCalendar> = withContext(Dispatchers.IO) {
        if (!hasPermission()) return@withContext emptyList()
        val columns = arrayOf(Calendars._ID, Calendars.CALENDAR_DISPLAY_NAME, Calendars.ACCOUNT_NAME, Calendars.CALENDAR_COLOR)
        context.contentResolver.query(Calendars.CONTENT_URI, columns, "NOT ($OWN_CALENDAR_SELECTION)", ownCalendarSelectionArgs(context), "${Calendars.ACCOUNT_NAME}, ${Calendars.CALENDAR_DISPLAY_NAME}")?.use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    add(DeviceCalendar(cursor.getLong(0), cursor.getString(1) ?: "Calendar", cursor.getString(2), if (cursor.isNull(3)) null else cursor.getInt(3)))
                }
            }
        }.orEmpty()
    }

    override suspend fun read(calendarId: Long, from: Instant, until: Instant, options: ConvertOptions): List<CalendarEntry> = withContext(Dispatchers.IO) {
        if (!hasPermission()) return@withContext emptyList()
        val uri = Instances.CONTENT_URI.buildUpon().also { builder ->
            ContentUris.appendId(builder, from.toEpochMilliseconds())
            ContentUris.appendId(builder, until.toEpochMilliseconds())
        }.build()
        val rows = context.contentResolver.query(uri, INSTANCE_COLUMNS, "${Instances.CALENDAR_ID} = ?", arrayOf(calendarId.toString()), "${Instances.BEGIN} ASC")?.use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    add(
                        InstanceRow(
                            eventId = cursor.getLong(0),
                            begin = Instant.fromEpochMilliseconds(cursor.getLong(1)),
                            end = Instant.fromEpochMilliseconds(cursor.getLong(2)),
                            allDay = cursor.getInt(3) == 1,
                            title = cursor.getString(4),
                            description = cursor.getString(5),
                            location = cursor.getString(6),
                            timeZoneId = cursor.getString(7),
                            cancelled = !cursor.isNull(8) && cursor.getInt(8) == Events.STATUS_CANCELED,
                        ),
                    )
                }
            }
        }.orEmpty()
        rows.groupBy { it.eventId }.map { (eventId, instances) ->
            val first = instances.first()
            val language = options.language
            CalendarEntry(
                uid = eventId.toString(),
                event = OpenEvnt(
                    id = "device-calendar:$calendarId:$eventId",
                    name = mapOf(language to (first.title?.takeIf { it.isNotBlank() } ?: "Untitled Event")),
                    venue = first.location?.takeIf { it.isNotBlank() }?.let { Venue(name = mapOf(language to it)) },
                    occurrences = instances.map { it.toOccurrence(options.timeZoneId) },
                    components = first.description?.takeIf { it.isNotBlank() }?.let { listOf(Component.PlainText(mapOf(language to it))) },
                ),
            )
        }
    }
}

private fun InstanceRow.toOccurrence(fallbackTimeZoneId: String): Occurrence {
    val status = Occurrence.Status.Cancelled.takeIf { cancelled }
    if (allDay) {
        val firstDay = begin.toLocalDateTime(TimeZone.UTC).date
        val lastDay = end.toLocalDateTime(TimeZone.UTC).date.minus(1, DateTimeUnit.DAY)
        return Occurrence(
            start = PartialDate.Day(firstDay, fallbackTimeZoneId),
            end = lastDay.takeIf { it > firstDay }?.let { PartialDate.Day(it, fallbackTimeZoneId) },
            status = status,
        )
    }
    val zone = timeZoneId?.takeIf(PartialDate::isTimeZoneId) ?: fallbackTimeZoneId
    return Occurrence(
        start = PartialDate.fromInstant(begin, zone),
        end = end.takeIf { it > begin }?.let { PartialDate.fromInstant(it, zone) },
        status = status,
    )
}
