package blue.deniz.vantage.storage

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import blue.deniz.vantage.core.EventFetcher
import blue.deniz.vantage.core.EventResolver
import blue.deniz.vantage.core.EventSource
import blue.deniz.vantage.core.FolioClient
import blue.deniz.vantage.core.SmallsyncClient
import blue.deniz.vantage.core.SmallsyncException
import blue.deniz.vantage.core.patched
import directory.evnt.OpenEvnt
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

private const val STORE = "https://sync.test/deniz/"
private const val TOKEN = "t0k"
private const val WEB_EVENT = "https://events.test/party.evnt.json"

private class FakeStore {
    private class Row(val seq: Long, val op: String, val type: String, val id: String, val value: JsonElement?)

    private val log = mutableListOf<Row>()
    private val keys = mutableMapOf<String, Long>()
    private var nextSeq = 1L

    @OptIn(ExperimentalUuidApi::class)
    var uuid = Uuid.random().toString()
        private set

    var webEvent = """{"v":"0.2","id":"$WEB_EVENT","name":{"en":"Party"},"occurrences":[{"start":"2026-11-28T20:00[Europe/Vilnius]"}]}"""

    @OptIn(ExperimentalUuidApi::class)
    fun startOver() {
        log.clear()
        keys.clear()
        uuid = Uuid.random().toString()
    }

    fun valueOf(type: String, id: String): JsonElement? {
        var value: JsonElement? = null
        for (row in log.filter { it.type == type && it.id == id }) {
            value = when (row.op) {
                "set" -> row.value
                "patch" -> value!!.patched(row.value as JsonArray)
                else -> null
            }
        }
        return value
    }

    fun ids(type: String) = log.filter { it.type == type }.map { it.id }.distinct().filter { valueOf(type, it) != null }

    fun write(op: String, type: String, id: String, value: JsonElement?): String? {
        val current = valueOf(type, id)
        val error = when (op) {
            "set" -> null
            "patch" -> if (current == null) "missing" else runCatching { current.patched(value as JsonArray) }.exceptionOrNull()?.let { "test-failed" }
            else -> if (current == null) "missing" else null
        }
        if (error == null) log += Row(nextSeq++, op, type, id, value)
        return error
    }

    fun MockRequestHandleScope.answer(request: HttpRequestData) = when {
        request.url.toString() == WEB_EVENT -> respond(webEvent)
        !request.url.toString().startsWith(STORE) -> respond("", HttpStatusCode.NotFound)
        request.headers[HttpHeaders.Authorization] != "Bearer $TOKEN" ->
            respond("""{"error":"unauthorized","message":"bad token"}""", HttpStatusCode.Unauthorized)
        request.url.encodedPath.endsWith("/pull") -> {
            val since = request.url.parameters["since"]!!.toLong()
            val types = request.url.parameters["types"]!!.split(",")
            val entries = buildJsonArray {
                log.filter { it.seq > since && it.type in types }.take(2).forEach { row ->
                    add(buildJsonObject {
                        put("seq", row.seq)
                        put("op", row.op)
                        put("type", row.type)
                        put("id", row.id)
                        put("at", 0)
                        row.value?.let { put("value", it) }
                    })
                }
            }
            respond(buildJsonObject { put("entries", entries) }.toString())
        }
        request.url.encodedPath.endsWith("/push") -> {
            val body = Json.parseToJsonElement((request.body as TextContent).text).jsonObject
            val results = buildJsonArray {
                for (entry in body.getValue("entries").jsonArray.map { it.jsonObject }) {
                    val key = entry["key"]?.jsonPrimitive?.content
                    val known = key?.let { keys[it] }
                    if (known != null) {
                        add(buildJsonObject { put("seq", known) })
                        continue
                    }
                    val error = write(
                        entry.getValue("op").jsonPrimitive.content,
                        entry.getValue("type").jsonPrimitive.content,
                        entry.getValue("id").jsonPrimitive.content,
                        entry["value"],
                    )
                    if (error == null) key?.let { keys[it] = nextSeq - 1 }
                    add(buildJsonObject { if (error == null) put("seq", nextSeq - 1) else put("error", error) })
                }
            }
            respond(buildJsonObject { put("results", results) }.toString())
        }
        else -> respond(buildJsonObject { put("\$type", "blue.deniz.smallsync#0"); put("uuid", uuid); put("sse", false) }.toString())
    }
}

private class Device(server: FakeStore) {
    private val client = HttpClient(MockEngine { request -> with(server) { answer(request) } })
    val database = Room.inMemoryDatabaseBuilder<VantageDatabase>()
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()
    val store = EventStore(database, EventResolver(EventFetcher(client)), FolioClient(client, "https://folio.test"))
    val sync = StoreSync(database, store, SmallsyncClient(client))

    suspend fun names(): List<String> = store.observeList().first().mapNotNull { it.data?.name?.get("en") }.sorted()
}

private fun made(name: String) =
    OpenEvnt.parse(Json.parseToJsonElement("""{"v":"0.2","id":"urn:x:${name.replace(" ", "-")}","name":{"en":"$name"},"occurrences":[{"start":"2026-11-26T13:45[Europe/Vilnius]"}]}"""))

class StoreSyncTest {
    private val server = FakeStore()
    private val phone = Device(server)
    private val laptop = Device(server)

    @AfterTest
    fun close() {
        phone.database.close()
        laptop.database.close()
    }

    @Test
    fun anEventMadeOnOneDeviceShowsUpOnTheOtherWithItsTags() = runTest {
        val id = phone.store.addLocal(made("Give and Share"))
        val tag = phone.store.addTag("Uni", "#a78bfa")
        phone.store.tagEvent(id, tag)
        phone.sync.connect(STORE, TOKEN)
        laptop.sync.connect(STORE, TOKEN)

        assertEquals(listOf("Give and Share"), laptop.names())
        assertEquals(listOf("Uni"), laptop.store.observeTagsOf(id).first().map { it.name })
        assertEquals("#a78bfa", laptop.store.observeTags().first().single().color)
    }

    @Test
    fun aRemoteEventIsFetchedByTheDeviceThatReceivesIt() = runTest {
        phone.store.addFromLink(WEB_EVENT)
        phone.sync.connect(STORE, TOKEN)
        laptop.sync.connect(STORE, TOKEN)

        val received = laptop.store.observeList().first().single()
        assertEquals(EventSource.Http(WEB_EVENT), received.source)
        assertEquals("Party", received.data?.name?.get("en"))
        assertNull(server.valueOf(VantageRecordTypes.EVENT, received.id!!)!!.jsonObject["document"])
    }

    @Test
    fun editsToDifferentFieldsOfOneRecordBothSurvive() = runTest {
        val tag = phone.store.addTag("Uni")
        phone.sync.connect(STORE, TOKEN)
        laptop.sync.connect(STORE, TOKEN)

        phone.store.renameTag(tag, "University")
        laptop.store.recolorTag(tag, "#f472b6")
        phone.sync.sync()
        laptop.sync.sync()
        phone.sync.sync()

        for (device in listOf(phone, laptop)) {
            val synced = device.store.observeTags().first().single()
            assertEquals("University", synced.name)
            assertEquals("#f472b6", synced.color)
        }
    }

    @Test
    fun whenBothDevicesChangeTheSameFieldTheLaterSyncWins() = runTest {
        val tag = phone.store.addTag("Uni")
        phone.sync.connect(STORE, TOKEN)
        laptop.sync.connect(STORE, TOKEN)

        phone.store.renameTag(tag, "From the phone")
        laptop.store.renameTag(tag, "From the laptop")
        phone.sync.sync()
        laptop.sync.sync()
        phone.sync.sync()

        assertEquals("From the laptop", phone.store.observeTags().first().single().name)
        assertEquals("From the laptop", server.valueOf(VantageRecordTypes.TAG, tag)!!.jsonObject["name"]!!.jsonPrimitive.content)
    }

    @Test
    fun removingAnEventRemovesItAndItsTagsEverywhere() = runTest {
        val id = phone.store.addLocal(made("Gone soon"))
        phone.store.tagEvent(id, phone.store.addTag("Uni"))
        phone.sync.connect(STORE, TOKEN)
        laptop.sync.connect(STORE, TOKEN)

        laptop.store.remove(id)
        laptop.sync.sync()
        phone.sync.sync()

        assertEquals(emptyList<String>(), phone.names())
        assertEquals(emptyList(), server.ids(VantageRecordTypes.EVENT))
        assertEquals(emptyList(), server.ids(VantageRecordTypes.TAGGING))
        assertEquals(1, server.ids(VantageRecordTypes.TAG).size)
    }

    @Test
    fun anEventWrittenByAnotherClientIsReadAndItsLaterPatchApplied() = runTest {
        phone.sync.connect(STORE, TOKEN)
        val document = Json.parseToJsonElement(made("Give and Share").toJson().toString())
        server.write(
            "set",
            VantageRecordTypes.EVENT,
            "e1",
            buildJsonObject {
                put("source", buildJsonObject { put("type", "local") })
                put("format", "directory.evnt.event")
                put("createdAt", "2026-11-17T16:00:00Z")
                put("document", document)
            },
        )
        phone.sync.sync()
        assertEquals(listOf("Give and Share"), phone.names())

        val renamed = JsonObject(document.jsonObject + ("name" to buildJsonObject { put("en", "Thursday Giveaway") }))
        val patch = buildJsonArray {
            add(buildJsonObject { put("op", "test"); put("path", "/document"); put("value", document) })
            add(buildJsonObject { put("op", "replace"); put("path", "/document"); put("value", renamed) })
        }
        assertNull(server.write("patch", VantageRecordTypes.EVENT, "e1", patch))
        phone.sync.sync()
        assertEquals(listOf("Thursday Giveaway"), phone.names())
    }

    @Test
    fun aStoreThatStartsOverGetsEveryRecordBack() = runTest {
        phone.store.addLocal(made("Kept"))
        phone.store.addTag("Uni")
        phone.sync.connect(STORE, TOKEN)
        server.startOver()

        phone.sync.sync()

        assertEquals(1, server.ids(VantageRecordTypes.EVENT).size)
        assertEquals(1, server.ids(VantageRecordTypes.TAG).size)
    }

    @Test
    fun aDeviceWithNothingNewLeavesTheStoreAlone() = runTest {
        phone.store.addLocal(made("Once"))
        phone.sync.connect(STORE, TOKEN)
        val before = server.ids(VantageRecordTypes.EVENT)
        phone.sync.sync()
        laptop.sync.connect(STORE, TOKEN)
        laptop.sync.sync()
        phone.sync.sync()
        assertEquals(before, server.ids(VantageRecordTypes.EVENT))
        assertEquals(0, phone.sync.observeStatus().first()!!.waitingChanges)
    }

    @Test
    fun testEventsAndTagsOfThemStayOnTheDevice() = runTest {
        val id = phone.store.addTestEvent(made("Test"))
        phone.store.tagEvent(id, phone.store.addTag("Uni"))
        phone.sync.connect(STORE, TOKEN)
        assertEquals(emptyList(), server.ids(VantageRecordTypes.EVENT))
        assertEquals(emptyList(), server.ids(VantageRecordTypes.TAGGING))
    }

    @Test
    fun aRefusedTokenStopsConnectingAndIsReportedOnSync() = runTest {
        assertFailsWith<SmallsyncException> { phone.sync.connect(STORE, "wrong") }
        assertNull(phone.sync.observeStatus().first())

        phone.store.addLocal(made("Waiting"))
        phone.sync.connect(STORE, TOKEN)
        phone.database.sync().saveStore(phone.database.sync().store()!!.copy(token = "wrong"))
        phone.store.addLocal(made("Also waiting"))
        phone.sync.sync()

        val status = assertNotNull(phone.sync.observeStatus().first())
        assertTrue(status.isTokenRefused)
        assertEquals("Token refused", status.problem)
        assertEquals(1, status.waitingChanges)
    }

    @Test
    fun loginsAndFolioTokensSyncAsCredentials() = runTest {
        phone.database.credentials().upsert(CredentialEntity("c1", "https://dav.test/", CredentialSecret.Login("deniz", "pw"), kotlin.time.Clock.System.now()))
        phone.sync.connect(STORE, TOKEN)
        laptop.sync.connect(STORE, TOKEN)
        assertEquals(CredentialSecret.Login("deniz", "pw"), laptop.database.credentials().all().single().secret)
        assertEquals("login", server.valueOf(VantageRecordTypes.CREDENTIAL, "c1")!!.jsonObject["secret"]!!.jsonObject["type"]!!.jsonPrimitive.content)
    }
}
