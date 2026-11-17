package blue.deniz.vantage.storage

import androidx.room3.withWriteTransaction
import blue.deniz.vantage.core.JsonPatchException
import blue.deniz.vantage.core.SmallsyncAccess
import blue.deniz.vantage.core.SmallsyncChange
import blue.deniz.vantage.core.SmallsyncClient
import blue.deniz.vantage.core.SmallsyncEntry
import blue.deniz.vantage.core.SmallsyncException
import blue.deniz.vantage.core.SmallsyncOperation
import blue.deniz.vantage.core.jsonPointerOf
import blue.deniz.vantage.core.patched
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

data class SyncStatus(
    val storeUrl: String,
    val lastSyncedAt: Instant?,
    val isSyncing: Boolean,
    val problem: String?,
    val isTokenRefused: Boolean,
    val waitingChanges: Int,
    val syncedEvents: Int,
    val syncedTags: Int,
)

private data class SyncActivity(
    val isSyncing: Boolean = false,
    val problem: String? = null,
    val isTokenRefused: Boolean = false,
    val waitingChanges: Int = 0,
)

private val SYNCED_TABLES = arrayOf("events", "event_cache", "tags", "tag_parents", "event_tags", "markers", "periods", "credentials")
private const val MAX_ROUNDS = 4
private const val PARALLEL_FETCHES = 6
private val PULL_FROM_START_AFTER = 365.days

private class StoreStartedOver : Exception()

class StoreSync(
    private val database: VantageDatabase,
    private val store: EventStore,
    private val client: SmallsyncClient,
    private val clock: Clock = Clock.System,
) {
    private val dao get() = database.sync()
    private val running = Mutex()
    private val activity = MutableStateFlow(SyncActivity())

    fun observeStatus(): Flow<SyncStatus?> =
        combine(dao.observeStore(), dao.observeRecordCounts(), activity) { connection, counts, activity ->
            connection ?: return@combine null
            val countByType = counts.associate { it.type to it.count }
            SyncStatus(
                storeUrl = connection.url,
                lastSyncedAt = connection.lastSyncedAt,
                isSyncing = activity.isSyncing,
                problem = activity.problem,
                isTokenRefused = activity.isTokenRefused,
                waitingChanges = activity.waitingChanges,
                syncedEvents = countByType[VantageRecordTypes.EVENT] ?: 0,
                syncedTags = countByType[VantageRecordTypes.TAG] ?: 0,
            )
        }

    fun observeLocalChanges(): Flow<Unit> = database.invalidationTracker.createFlow(*SYNCED_TABLES).map { }

    suspend fun connect(storeUrl: String, token: String) {
        val access = SmallsyncAccess(storeUrl, token.trim())
        client.describeStore(access)
        running.withLock {
            database.withWriteTransaction {
                dao.deleteStore()
                dao.deleteAllRecords()
                dao.saveStore(SyncStoreEntity(access.normalizedStoreUrl, access.token, storeUuid = null, lastSeq = 0, lastSyncedAt = null))
            }
            activity.value = SyncActivity()
        }
        sync()
    }

    suspend fun changeToken(token: String) {
        val connection = dao.store() ?: return
        client.describeStore(SmallsyncAccess(connection.url, token.trim()))
        running.withLock { dao.saveStore(connection.copy(token = token.trim())) }
        sync()
    }

    suspend fun disconnect() = running.withLock {
        database.withWriteTransaction {
            dao.deleteStore()
            dao.deleteAllRecords()
        }
        activity.value = SyncActivity()
    }

    suspend fun sync() = running.withLock {
        if (dao.store() == null) return@withLock
        activity.update { it.copy(isSyncing = true) }
        try {
            var rounds = 0
            while (rounds < MAX_ROUNDS && syncRound()) rounds++
            activity.value = SyncActivity()
        } catch (cancelled: CancellationException) {
            activity.update { it.copy(isSyncing = false) }
            throw cancelled
        } catch (failed: Exception) {
            activity.value = SyncActivity(
                problem = syncProblemOf(failed),
                isTokenRefused = (failed as? SmallsyncException)?.isTokenRefused == true,
                waitingChanges = countWaitingChanges(),
            )
        }
    }

    private suspend fun countWaitingChanges(): Int {
        val synced = dao.allRecords().associateBy { RecordKey(it.type, it.id) }
        val local = database.currentRecords()
        return (synced.keys + local.keys).count { key ->
            val row = synced[key]
            row?.isAppliedHere != false && local[key] != row?.value?.let(recordJson::parseToJsonElement)
        }
    }

    private suspend fun syncRound(): Boolean {
        var connection = dao.store() ?: return false
        val access = SmallsyncAccess(connection.url, connection.token)
        val info = client.describeStore(access)
        val pullsFromStart = connection.lastSyncedAt?.let { clock.now() - it > PULL_FROM_START_AFTER } == true
        if (info.uuid != connection.storeUuid || pullsFromStart) {
            connection = startOver(connection, info.uuid)
        }
        val pulled = client.pull(access, connection.lastSeq, VantageRecordTypes.all)
        val syncedRows = dao.allRecords().associateBy { RecordKey(it.type, it.id) }
        val base = syncedRows.mapValues { recordJson.parseToJsonElement(it.value.value) }
        val server = try {
            serverValuesAfter(pulled, base)
        } catch (_: JsonPatchException) {
            startOver(connection, info.uuid)
            return true
        }
        val local = database.currentRecords()
        val now = clock.now()

        val writes = LinkedHashMap<RecordKey, JsonElement?>()
        val changes = mutableListOf<SmallsyncChange>()
        for (key in base.keys + local.keys + server.keys) {
            val before = base[key]
            val remote = if (key in server) server[key] else before
            val isAppliedHere = syncedRows[key]?.isAppliedHere ?: true
            val current = if (isAppliedHere) local[key] else before
            val merged = mergedRecord(before, current, remote)
            if (merged != current || !isAppliedHere) writes[key] = merged
            if (merged != remote) changes += changeBetween(key, remote, merged)
        }

        val writer = RecordWriter(database, store, now)
        val prepared = prepareEvents(writer, writes)
        database.withWriteTransaction {
            val applied = HashMap<RecordKey, Boolean>()
            for ((key, value) in writes.entries.sortedBy { writeOrder(it.key.type, it.value == null) }) {
                applied[key] = writer.write(key, value, prepared[key])
            }
            for ((key, value) in writes) {
                if (key.type == VantageRecordTypes.TAG && value != null && applied[key] == true) writer.saveTagParents(key.id, value)
            }
            for (key in server.keys + writes.keys) {
                val remote = if (key in server) server[key] else base[key]
                if (remote == null) {
                    dao.deleteRecord(key.type, key.id)
                } else if (key in server || key in applied) {
                    val isAppliedHere = applied[key] ?: syncedRows[key]?.isAppliedHere ?: true
                    dao.saveRecords(listOf(SyncedRecordEntity(key.type, key.id, remote.toString(), isAppliedHere)))
                }
            }
            dao.saveStore(connection.copy(lastSeq = pulled.lastOrNull()?.seq ?: connection.lastSeq, lastSyncedAt = now))
        }

        if (changes.isEmpty()) return false
        client.push(access, changes)
        return true
    }

    private suspend fun startOver(connection: SyncStoreEntity, storeUuid: String): SyncStoreEntity {
        val restarted = connection.copy(storeUuid = storeUuid, lastSeq = 0)
        database.withWriteTransaction {
            dao.deleteAllRecords()
            dao.saveStore(restarted)
        }
        return restarted
    }

    private suspend fun prepareEvents(writer: RecordWriter, writes: Map<RecordKey, JsonElement?>): Map<RecordKey, PreparedEvent> = coroutineScope {
        val fetches = Semaphore(PARALLEL_FETCHES)
        writes.entries
            .filter { it.key.type == VantageRecordTypes.EVENT && it.value != null }
            .map { (key, value) -> async { fetches.withPermit { writer.prepareEvent(key.id, value!!)?.let { key to it } } } }
            .awaitAll()
            .filterNotNull()
            .toMap()
    }
}

fun syncProblemOf(failed: Exception): String = when {
    failed is SmallsyncException && failed.isTokenRefused -> "Token refused"
    failed is SmallsyncException -> failed.message ?: "The store answered ${failed.status}"
    else -> failed.message ?: "Can't reach the store"
}

private fun writeOrder(type: String, isRemoval: Boolean): Int {
    val order = listOf(
        VantageRecordTypes.TAG,
        VantageRecordTypes.EVENT,
        VantageRecordTypes.TAGGING,
        VantageRecordTypes.MARKER,
        VantageRecordTypes.PERIOD,
        VantageRecordTypes.CREDENTIAL,
    )
    val index = order.indexOf(type)
    return if (isRemoval) order.size * 2 - index else index
}

internal fun serverValuesAfter(pulled: List<SmallsyncEntry>, base: Map<RecordKey, JsonElement>): Map<RecordKey, JsonElement?> {
    val values = LinkedHashMap<RecordKey, JsonElement?>()
    for (entry in pulled) {
        val key = RecordKey(entry.type, entry.id)
        val before = if (key in values) values[key] else base[key]
        values[key] = when (entry.op) {
            SmallsyncOperation.Set -> entry.value
            SmallsyncOperation.Delete -> null
            SmallsyncOperation.Patch -> {
                val operations = entry.value as? JsonArray ?: throw JsonPatchException("A patch entry has no operations")
                (before ?: throw JsonPatchException("A patch for a record this device doesn't have")).patched(operations)
            }
        }
    }
    return values
}

internal fun mergedRecord(before: JsonElement?, local: JsonElement?, remote: JsonElement?): JsonElement? = when {
    local == before -> remote
    remote == before -> local
    local == null || remote == null -> local
    local is JsonObject && remote is JsonObject -> {
        val previous = before as? JsonObject ?: JsonObject(emptyMap())
        val merged = remote.toMutableMap()
        for (field in previous.keys + local.keys) {
            if (local[field] == previous[field]) continue
            val value = local[field]
            if (value == null) merged.remove(field) else merged[field] = value
        }
        JsonObject(merged)
    }
    else -> local
}

@OptIn(ExperimentalUuidApi::class)
internal fun changeBetween(key: RecordKey, remote: JsonElement?, wanted: JsonElement?): SmallsyncChange {
    val changeKey = Uuid.random().toString()
    return when {
        wanted == null -> SmallsyncChange(changeKey, SmallsyncOperation.Delete, key.type, key.id)
        remote is JsonObject && wanted is JsonObject -> SmallsyncChange(changeKey, SmallsyncOperation.Patch, key.type, key.id, patchBetween(remote, wanted))
        else -> SmallsyncChange(changeKey, SmallsyncOperation.Set, key.type, key.id, wanted)
    }
}

private fun patchBetween(remote: JsonObject, wanted: JsonObject): JsonArray {
    val operations = mutableListOf<JsonElement>()
    for (field in remote.keys + wanted.keys) {
        val old = remote[field]
        val new = wanted[field]
        if (old == new) continue
        val path = jsonPointerOf(field)
        if (old != null) operations += operation("test", path, old)
        operations += when {
            new == null -> operation("remove", path, null)
            old == null -> operation("add", path, new)
            else -> operation("replace", path, new)
        }
    }
    return JsonArray(operations)
}

private fun operation(name: String, path: String, value: JsonElement?) = buildJsonObject {
    put("op", name)
    put("path", path)
    if (value != null) put("value", value)
}
