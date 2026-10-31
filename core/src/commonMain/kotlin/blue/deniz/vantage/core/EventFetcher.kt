package blue.deniz.vantage.core

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.URLBuilder
import io.ktor.http.appendPathSegments
import io.ktor.http.isSuccess
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlin.time.Instant

data class FetchedEvent(val raw: String, val revision: Revision)

internal fun JsonElement?.stringOrNull() = (this as? JsonPrimitive)?.takeIf { it.isString }?.content

internal fun failure(kind: EventError.Kind, message: String, status: Int? = null): Nothing =
    throw EventException(EventError(kind, message, status))

internal fun parseJsonObject(text: String, what: String): JsonObject =
    try {
        Json.parseToJsonElement(text) as? JsonObject
    } catch (_: IllegalArgumentException) {
        null
    } ?: failure(EventError.Kind.JsonParse, "$what is not a JSON object")

private fun HttpResponse.revision() = Revision(etag = headers[HttpHeaders.ETag], lastModified = headers[HttpHeaders.LastModified])

internal suspend fun HttpClient.getOk(url: String): HttpResponse {
    val response = get(url)
    if (!response.status.isSuccess()) failure(EventError.Kind.Fetch, "$url answered ${response.status}", response.status.value)
    return response
}

internal suspend fun HttpClient.getXrpc(url: String): HttpResponse {
    val response = get(url)
    if (!response.status.isSuccess()) {
        val body = response.bodyAsText()
        val error = runCatching { parseJsonObject(body, "The error") }.getOrNull()
        val message = error?.get("message").stringOrNull() ?: error?.get("error").stringOrNull() ?: response.status.toString()
        failure(EventError.Kind.Xrpc, message, response.status.value)
    }
    return response
}

class EventFetcher(private val client: HttpClient) {
    private val identity = AtIdentityResolver(client)

    suspend fun fetch(source: EventSource.Remote): FetchedEvent = when (source) {
        is EventSource.Http, is EventSource.Folio, is EventSource.WebDav -> fetchText(source.location)
        is EventSource.MediaWiki -> fetchMediaWikiPage(source)
        is EventSource.At -> fetchAtRecord(source.uri)
    }

    private suspend fun fetchText(url: String): FetchedEvent {
        val response = client.getOk(url)
        return FetchedEvent(response.bodyAsText(), response.revision())
    }

    private suspend fun fetchMediaWikiPage(source: EventSource.MediaWiki): FetchedEvent {
        val response = client.getOk(source.location)
        val page = parseJsonObject(response.bodyAsText(), "The MediaWiki page")
        val latest = page["latest"] as? JsonObject
        val raw = page["source"].stringOrNull() ?: failure(EventError.Kind.Fetch, "The MediaWiki page has no source")
        return FetchedEvent(
            raw = raw,
            revision = response.revision().copy(
                revisionId = (latest?.get("id") as? JsonPrimitive)?.longOrNull,
                revisionTimestamp = latest?.get("timestamp").stringOrNull()?.let(Instant::parseOrNull),
            ),
        )
    }

    private suspend fun fetchAtRecord(uri: AtUri): FetchedEvent {
        if (!uri.repoIsDid) failure(EventError.Kind.Fetch, "${uri.repo} is a handle; reading a record needs the DID")
        val endpoint = URLBuilder(identity.personalDataServerOf(uri.repo)).apply {
            appendPathSegments("xrpc", "com.atproto.repo.getRecord")
            parameters.append("repo", uri.repo)
            parameters.append("collection", uri.collection)
            parameters.append("rkey", uri.rkey)
        }.buildString()
        val response = client.getXrpc(endpoint)
        val body = response.bodyAsText()
        val record = parseJsonObject(body, "The record")
        val value = record["value"] as? JsonObject ?: failure(EventError.Kind.Xrpc, "The record has no value")
        return FetchedEvent(raw = value.toString(), revision = response.revision().copy(cid = record["cid"].stringOrNull()))
    }
}
