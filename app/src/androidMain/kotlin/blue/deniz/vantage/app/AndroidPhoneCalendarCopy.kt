package blue.deniz.vantage.app

import android.Manifest
import android.content.ContentProviderOperation
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.CalendarContract
import android.provider.CalendarContract.Calendars
import android.provider.CalendarContract.Events
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val ACCOUNT_NAME = "Vantage"
private const val CALENDAR_DISPLAY_NAME = "Vantage"
private const val CALENDAR_COLOR = 0xFFA97DFF.toInt()

const val OWN_CALENDAR_SELECTION = "${Calendars.ACCOUNT_TYPE} IS ? AND ${Calendars.ACCOUNT_NAME} IS ? AND ${Calendars.NAME} IS ?"

fun ownCalendarSelectionArgs(context: Context) = arrayOf(CalendarContract.ACCOUNT_TYPE_LOCAL, ACCOUNT_NAME, context.packageName)

private class CopiedRow(val id: Long, val hash: String?, val deleted: Boolean)

class AndroidPhoneCalendarCopy(private val context: Context) : PhoneCalendarCopy {
    private val resolver get() = context.contentResolver

    override fun hasPermission() = listOf(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR)
        .all { context.checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED }

    override suspend fun replaceEntries(entries: List<PhoneCalendarEntry>) = withContext(Dispatchers.IO) {
        val calendarId = findCalendarId() ?: createCalendar()
        val copied = copiedRows(calendarId)
        val keys = entries.mapTo(HashSet()) { it.key }
        val operations = ArrayList<ContentProviderOperation>()
        for ((key, row) in copied) {
            if (row.deleted || key !in keys) operations += ContentProviderOperation.newDelete(asSyncAdapter(eventUriOf(row.id))).build()
        }
        for (entry in entries) {
            val row = copied[entry.key]?.takeUnless { it.deleted }
            val hash = entry.hashCode().toString()
            val values = entry.toValues(calendarId, hash)
            when {
                row == null -> operations += ContentProviderOperation.newInsert(asSyncAdapter(Events.CONTENT_URI)).withValues(values).build()
                row.hash != hash -> operations += ContentProviderOperation.newUpdate(asSyncAdapter(eventUriOf(row.id))).withValues(values).build()
            }
        }
        if (operations.isNotEmpty()) resolver.applyBatch(CalendarContract.AUTHORITY, operations)
        Unit
    }

    override suspend fun delete() = withContext(Dispatchers.IO) {
        val calendarId = findCalendarId() ?: return@withContext
        resolver.delete(asSyncAdapter(ContentUris.withAppendedId(Calendars.CONTENT_URI, calendarId)), null, null)
        Unit
    }

    private fun findCalendarId(): Long? =
        resolver.query(Calendars.CONTENT_URI, arrayOf(Calendars._ID), OWN_CALENDAR_SELECTION, ownCalendarSelectionArgs(context), null)
            ?.use { cursor -> if (cursor.moveToFirst()) cursor.getLong(0) else null }

    private fun createCalendar(): Long {
        val values = ContentValues().apply {
            put(Calendars.ACCOUNT_NAME, ACCOUNT_NAME)
            put(Calendars.ACCOUNT_TYPE, CalendarContract.ACCOUNT_TYPE_LOCAL)
            put(Calendars.NAME, context.packageName)
            put(Calendars.CALENDAR_DISPLAY_NAME, CALENDAR_DISPLAY_NAME)
            put(Calendars.CALENDAR_COLOR, CALENDAR_COLOR)
            put(Calendars.CALENDAR_ACCESS_LEVEL, Calendars.CAL_ACCESS_READ)
            put(Calendars.OWNER_ACCOUNT, ACCOUNT_NAME)
            put(Calendars.VISIBLE, 1)
            put(Calendars.SYNC_EVENTS, 1)
        }
        val uri = resolver.insert(asSyncAdapter(Calendars.CONTENT_URI), values) ?: error("Android did not create the Vantage calendar")
        return ContentUris.parseId(uri)
    }

    private fun copiedRows(calendarId: Long): Map<String, CopiedRow> {
        val columns = arrayOf(Events._ID, Events.SYNC_DATA1, Events.SYNC_DATA2, Events.DELETED)
        return resolver.query(asSyncAdapter(Events.CONTENT_URI), columns, "${Events.CALENDAR_ID} = ?", arrayOf(calendarId.toString()), null)?.use { cursor ->
            buildMap {
                while (cursor.moveToNext()) {
                    val key = cursor.getString(1) ?: continue
                    put(key, CopiedRow(cursor.getLong(0), cursor.getString(2), cursor.getInt(3) == 1))
                }
            }
        }.orEmpty()
    }
}

private fun eventUriOf(id: Long) = ContentUris.withAppendedId(Events.CONTENT_URI, id)

private fun asSyncAdapter(uri: Uri): Uri = uri.buildUpon()
    .appendQueryParameter(CalendarContract.CALLER_IS_SYNCADAPTER, "true")
    .appendQueryParameter(Calendars.ACCOUNT_NAME, ACCOUNT_NAME)
    .appendQueryParameter(Calendars.ACCOUNT_TYPE, CalendarContract.ACCOUNT_TYPE_LOCAL)
    .build()

private fun PhoneCalendarEntry.toValues(calendarId: Long, hash: String) = ContentValues().apply {
    put(Events.CALENDAR_ID, calendarId)
    put(Events.TITLE, title)
    put(Events.DTSTART, start.toEpochMilliseconds())
    put(Events.DTEND, end.toEpochMilliseconds())
    put(Events.ALL_DAY, if (allDay) 1 else 0)
    put(Events.EVENT_TIMEZONE, timeZoneId)
    put(Events.EVENT_LOCATION, location)
    put(Events.DESCRIPTION, description)
    put(Events.STATUS, if (cancelled) Events.STATUS_CANCELED else Events.STATUS_CONFIRMED)
    put(Events.SYNC_DATA1, key)
    put(Events.SYNC_DATA2, hash)
}
