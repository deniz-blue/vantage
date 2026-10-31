package blue.deniz.vantage.app

import android.content.ContentProvider
import android.content.ContentValues
import android.content.pm.PackageManager
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.Binder
import blue.deniz.vantage.core.CalendarMarker
import blue.deniz.vantage.core.CalendarPeriod
import blue.deniz.vantage.core.isEditableHere
import blue.deniz.vantage.core.ResolvedEvent
import blue.deniz.vantage.storage.EventListQuery
import blue.deniz.vantage.storage.EventStore
import directory.evnt.OpenEvnt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.time.Instant

object EventsContract {
    const val VERSION = "v1"
    const val EVENTS_PATH = "events"
    const val MARKERS_PATH = "markers"
    const val PERIODS_PATH = "periods"
    const val ROW_ID = "_id"
    const val ID = "id"
    const val JSON = "json"
    const val AFTER = "after"
    const val BEFORE = "before"
    const val SEARCH = "search"
    const val EVENT_LIST_TYPE = "vnd.android.cursor.dir/vnd.evnt.event"
    const val EVENT_TYPE = "vnd.android.cursor.item/vnd.evnt.event"
    val DEFAULT_COLUMNS = arrayOf(ROW_ID, ID, JSON)

    fun authorityOf(packageName: String) = "$packageName.$VERSION.events"
    fun readPermissionOf(packageName: String) = "$packageName.permission.READ_EVENTS"
    fun writePermissionOf(packageName: String) = "$packageName.permission.WRITE_EVENTS"
    fun eventsUriOf(packageName: String): Uri = Uri.parse("content://${authorityOf(packageName)}/$EVENTS_PATH")
    fun eventUriOf(packageName: String, id: String): Uri = eventsUriOf(packageName).buildUpon().appendPath(id).build()
}

// adb shell runs as AID_SHELL, uid 2000.
private const val SHELL_UID = 2000

private sealed interface EventsTarget {
    data object All : EventsTarget
    data class One(val id: String) : EventsTarget
}

private enum class StoredKind(val path: String) { Events(EventsContract.EVENTS_PATH), Markers(EventsContract.MARKERS_PATH), Periods(EventsContract.PERIODS_PATH) }

class EventsProvider : ContentProvider() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val packageName get() = requireNotNull(context).packageName
    private val eventsUri get() = EventsContract.eventsUriOf(packageName)

    private val store: EventStore by lazy {
        val store = (requireNotNull(context).applicationContext as AppStateOwner).appState.store
        scope.launch { store.observeList().drop(1).collect { notifyEventsChanged() } }
        store
    }

    override fun onCreate() = true

    override fun getType(uri: Uri): String = when (targetOf(uri)) {
        EventsTarget.All -> EventsContract.EVENT_LIST_TYPE
        is EventsTarget.One -> EventsContract.EVENT_TYPE
    }

    override fun query(uri: Uri, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?): Cursor {
        requireCallerHolds(EventsContract.readPermissionOf(packageName))
        require(selection == null && sortOrder == null) { "Filter with the after, before and search query parameters" }
        if (kindOf(uri) != StoredKind.Events) return queryMarks(uri, projection)
        val events = runBlocking {
            when (val target = targetOf(uri)) {
                EventsTarget.All -> store.observeList(listQueryOf(uri)).first()
                is EventsTarget.One -> listOfNotNull(store.find(target.id))
            }
        }
        val columns = projection ?: EventsContract.DEFAULT_COLUMNS
        return MatrixCursor(columns, events.size).apply {
            events.forEachIndexed { index, event -> addRow(columns.map { column -> event.columnValue(column, index) }) }
            setNotificationUri(requireNotNull(context).contentResolver, eventsUri)
        }
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri {
        requireCallerHolds(EventsContract.writePermissionOf(packageName))
        require(targetOf(uri) == EventsTarget.All) { "Insert into ${eventsUri}" }
        val json = jsonFrom(values)
        when (kindOf(uri)) {
            StoredKind.Markers -> return runBlocking { OpenEvnt.json.decodeFromString(CalendarMarker.serializer(), json).also { store.saveMarker(it) } }.let { uri.buildUpon().appendPath(it.id).build() }
            StoredKind.Periods -> return runBlocking { OpenEvnt.json.decodeFromString(CalendarPeriod.serializer(), json).also { store.savePeriod(it) } }.let { uri.buildUpon().appendPath(it.id).build() }
            StoredKind.Events -> {}
        }
        val id = runBlocking { store.addLocal(OpenEvnt.parse(json)) }
        notifyEventsChanged()
        return EventsContract.eventUriOf(packageName, id)
    }

    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int {
        requireCallerHolds(EventsContract.writePermissionOf(packageName))
        val target = targetOf(uri) as? EventsTarget.One ?: throw IllegalArgumentException("Update one event at ${eventsUri}/<id>")
        val data = eventFrom(values)
        val updated = runBlocking {
            val stored = store.find(target.id)
            if (stored?.source?.isEditableHere != true) return@runBlocking false
            store.saveEdited(target.id, data)
            true
        }
        if (updated) notifyEventsChanged()
        return if (updated) 1 else 0
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int {
        requireCallerHolds(EventsContract.writePermissionOf(packageName))
        val target = targetOf(uri) as? EventsTarget.One ?: throw IllegalArgumentException("Delete one event at ${eventsUri}/<id>")
        val removed = runBlocking {
            when (kindOf(uri)) {
                StoredKind.Events -> store.remove(target.id)
                StoredKind.Markers -> store.removeMarker(target.id)
                StoredKind.Periods -> store.removePeriod(target.id)
            }
        } != null
        if (removed) notifyEventsChanged()
        return if (removed) 1 else 0
    }

    private fun notifyEventsChanged() {
        context?.contentResolver?.notifyChange(eventsUri, null)
    }

    private fun requireCallerHolds(permission: String) {
        if (Binder.getCallingUid() == SHELL_UID) return
        if (requireNotNull(context).checkCallingOrSelfPermission(permission) == PackageManager.PERMISSION_GRANTED) return
        throw SecurityException("Needs $permission")
    }

    private fun targetOf(uri: Uri): EventsTarget {
        val segments = uri.pathSegments
        require(uri.authority == EventsContract.authorityOf(packageName) && segments.size in 1..2) { "Unknown address $uri" }
        kindOf(uri)
        return if (segments.size == 1) EventsTarget.All else EventsTarget.One(segments[1])
    }

    private fun kindOf(uri: Uri): StoredKind =
        StoredKind.entries.find { it.path == uri.pathSegments.firstOrNull() } ?: throw IllegalArgumentException("Unknown address $uri")

    private fun queryMarks(uri: Uri, projection: Array<out String>?): Cursor {
        val rows = runBlocking {
            when (kindOf(uri)) {
                StoredKind.Markers -> store.observeMarkers().first().map { it.id to OpenEvnt.json.encodeToString(CalendarMarker.serializer(), it) }
                else -> store.observePeriods().first().map { it.id to OpenEvnt.json.encodeToString(CalendarPeriod.serializer(), it) }
            }
        }
        val columns = projection ?: EventsContract.DEFAULT_COLUMNS
        return MatrixCursor(columns, rows.size).apply {
            rows.forEachIndexed { index, (id, json) ->
                addRow(columns.map { column ->
                    when (column) {
                        EventsContract.ROW_ID -> index.toLong()
                        EventsContract.ID -> id
                        EventsContract.JSON -> json
                        else -> null
                    }
                })
            }
        }
    }
}

private fun listQueryOf(uri: Uri) = EventListQuery(
    search = uri.getQueryParameter(EventsContract.SEARCH),
    from = uri.getQueryParameter(EventsContract.AFTER)?.let(Instant::parse),
    until = uri.getQueryParameter(EventsContract.BEFORE)?.let(Instant::parse),
    order = EventListQuery.Order.Start,
)

private fun jsonFrom(values: ContentValues?): String =
    values?.getAsString(EventsContract.JSON) ?: throw IllegalArgumentException("Needs a ${EventsContract.JSON} value")

private fun eventFrom(values: ContentValues?): OpenEvnt = OpenEvnt.parse(jsonFrom(values))

private fun ResolvedEvent.columnValue(column: String, index: Int): Any? = when (column) {
    EventsContract.ROW_ID -> index.toLong()
    EventsContract.ID -> id
    EventsContract.JSON -> data?.toJson()?.toString()
    else -> null
}
