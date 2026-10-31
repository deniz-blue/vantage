package blue.deniz.vantage.core

import io.ktor.client.HttpClient
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.URLBuilder
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.http.path
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

class FolioClient(private val client: HttpClient, private val baseUrl: String = EventSource.Folio.DEFAULT_BASE_URL) {
    suspend fun create(eventJson: String): EventSource.Folio {
        val url = URLBuilder(baseUrl).apply { path("events") }.buildString()
        val created = parseJsonObject(bodyOf(client.post(url) { jsonBody(eventJson) }), "Folio's answer")
        val id = created["id"].stringOrNull() ?: failure(EventError.Kind.Fetch, "Folio gave the event no id")
        return EventSource.Folio(id, baseUrl, created["edit_token"].stringOrNull())
    }

    suspend fun replace(source: EventSource.Folio, eventJson: String): String =
        bodyOf(client.put(source.location) {
            source.editToken?.let(::bearerAuth)
            jsonBody(eventJson)
        })

    private fun HttpRequestBuilder.jsonBody(json: String) {
        contentType(ContentType.Application.Json)
        setBody(json)
    }

    private suspend fun bodyOf(response: HttpResponse): String {
        val body = response.bodyAsText()
        if (!response.status.isSuccess()) {
            failure(EventError.Kind.Fetch, folioProblemIn(body) ?: "Folio answered ${response.status}", response.status.value)
        }
        return body
    }
}

private fun folioProblemIn(body: String): String? {
    val answer = runCatching { parseJsonObject(body, "Folio's answer") }.getOrNull() ?: return null
    val error = answer["error"].stringOrNull() ?: return null
    val details = (answer["details"] as? JsonArray)?.mapNotNull(::describeIssue).orEmpty()
    return (listOf(error) + details).joinToString("\n")
}

private fun describeIssue(issue: JsonElement): String? {
    val fields = issue as? JsonObject ?: return null
    val message = fields["message"].stringOrNull() ?: return null
    val path = (fields["path"] as? JsonArray)?.joinToString(".") { segment -> (segment as? JsonPrimitive)?.content ?: "?" }
    return if (path.isNullOrEmpty()) message else "$path $message"
}
