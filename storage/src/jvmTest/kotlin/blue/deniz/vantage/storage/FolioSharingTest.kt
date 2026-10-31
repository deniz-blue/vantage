package blue.deniz.vantage.storage

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import blue.deniz.vantage.core.EventException
import blue.deniz.vantage.core.EventFetcher
import blue.deniz.vantage.core.EventFormat
import blue.deniz.vantage.core.EventResolver
import blue.deniz.vantage.core.EventSource
import blue.deniz.vantage.core.FolioClient
import blue.deniz.vantage.core.ResolvedEvent
import directory.evnt.OpenEvnt
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

private const val FOLIO = "https://folio.test"
private const val SHARED_URL = "$FOLIO/events/f1"
private const val EDIT_TOKEN = "k1"

private class FakeFolio {
    val documents = mutableMapOf<String, String>()
    val requests = mutableListOf<String>()
    var refusesUploads = false

    fun MockRequestHandleScope.answer(request: HttpRequestData) = when {
        request.method == HttpMethod.Post && refusesUploads ->
            respond("""{"error":"Invalid evnt schema payload","details":[{"path":["name"],"message":"Required"}]}""", HttpStatusCode.BadRequest)
        request.method == HttpMethod.Post && request.url.toString() == "$FOLIO/events" -> {
            requests += "POST"
            documents[SHARED_URL] = withId(bodyOf(request))
            respond("""{"id":"f1","edit_token":"$EDIT_TOKEN"}""", HttpStatusCode.Created)
        }
        request.method == HttpMethod.Put && request.headers[HttpHeaders.Authorization] != "Bearer $EDIT_TOKEN" ->
            respond("""{"error":"Invalid edit token"}""", HttpStatusCode.Forbidden)
        request.method == HttpMethod.Put -> {
            requests += "PUT"
            documents[SHARED_URL] = withId(bodyOf(request))
            respond(documents.getValue(SHARED_URL))
        }
        else -> documents[request.url.toString()]?.let { respond(it) } ?: respond("""{"error":"Event not found"}""", HttpStatusCode.NotFound)
    }

    private fun bodyOf(request: HttpRequestData) = (request.body as TextContent).text

    private fun withId(json: String) = JsonObject(Json.parseToJsonElement(json).jsonObject + ("id" to JsonPrimitive(SHARED_URL))).toString()
}

private fun made(name: String) =
    OpenEvnt.parse(Json.parseToJsonElement("""{"v":"0.2","id":"urn:uuid:7","name":{"en":"$name"},"occurrences":[{"start":"2026-11-07T19:00[Europe/Vilnius]"}]}"""))

class FolioSharingTest {
    private val folio = FakeFolio()

    private val client = HttpClient(MockEngine { request -> with(folio) { answer(request) } })

    private val database = Room.inMemoryDatabaseBuilder<VantageDatabase>()
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()

    private val store = EventStore(database, EventResolver(EventFetcher(client)), FolioClient(client, FOLIO))

    @AfterTest
    fun close() = database.close()

    @Test
    fun uploadingKeepsTheEditToken() = runTest {
        val id = store.addLocal(made("Movie night"))
        assertEquals(EventSource.Folio("f1", FOLIO, EDIT_TOKEN), store.uploadToFolio(id))
        val shared = assertNotNull(store.find(id))
        assertEquals(EventSource.Folio("f1", FOLIO, EDIT_TOKEN), shared.source)
        assertEquals(SHARED_URL, shared.data?.id)
        assertEquals("Movie night", shared.data?.name?.get("en"))
    }

    @Test
    fun editingAnUploadedEventSendsItToFolio() = runTest {
        val id = store.addLocal(made("Movie night"))
        store.uploadToFolio(id)
        store.saveEdited(id, made("Movie night, moved"))
        assertEquals(listOf("POST", "PUT"), folio.requests)
        assertTrue(folio.documents.getValue(SHARED_URL).contains("Movie night, moved"))
        val saved = assertNotNull(store.find(id))
        assertEquals("Movie night, moved", saved.data?.name?.get("en"))
        assertEquals(SHARED_URL, saved.data?.id)
    }

    @Test
    fun aRefusedEditKeepsTheEventAsItWas() = runTest {
        val id = store.add(ResolvedEvent(source = EventSource.Folio("f1", FOLIO, "wrong"), format = EventFormat.OpenEvnt))
        val before = store.find(id)
        val failure = assertFailsWith<EventException> { store.saveEdited(id, made("Hijacked")) }
        assertEquals("Invalid edit token", failure.message)
        assertEquals(403, failure.error.status)
        assertEquals(before, store.find(id))
    }

    @Test
    fun anEventMadeOnFolioIsStoredAsAFolioEvent() = runTest {
        val id = store.addToFolio(made("Movie night"))
        assertEquals(listOf("POST"), folio.requests)
        val made = assertNotNull(store.find(id))
        assertEquals(EventSource.Folio("f1", FOLIO, EDIT_TOKEN), made.source)
        assertEquals(SHARED_URL, made.data?.id)
        assertEquals("Movie night", made.data?.name?.get("en"))
    }

    @Test
    fun aRefusedUploadAddsNothing() = runTest {
        folio.refusesUploads = true
        val failure = assertFailsWith<EventException> { store.addToFolio(made("Movie night")) }
        assertEquals("Invalid evnt schema payload\nname Required", failure.message)
        assertEquals(emptyList(), store.observeList().first())
    }

    @Test
    fun aTestEventIsNotUploaded() = runTest {
        val id = store.addTestEvent(made("Test"))
        assertFailsWith<IllegalStateException> { store.uploadToFolio(id) }
        assertEquals(emptyList(), folio.requests)
    }
}
