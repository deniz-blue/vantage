package blue.deniz.vantage.core

import io.ktor.client.HttpClient
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

@Serializable
data class SmallsyncStoreInfo(val uuid: String, val scopes: List<String> = emptyList())

@Serializable
enum class SmallsyncOperation {
    @SerialName("set")
    Set,

    @SerialName("patch")
    Patch,

    @SerialName("delete")
    Delete,
}

@Serializable
data class SmallsyncEntry(
    val seq: Long,
    val op: SmallsyncOperation,
    val type: String,
    val id: String,
    val at: Long,
    val value: JsonElement? = null,
)

@Serializable
data class SmallsyncChange(
    val key: String,
    val op: SmallsyncOperation,
    val type: String,
    val id: String,
    val value: JsonElement? = null,
)

@Serializable
data class SmallsyncPushResult(val seq: Long? = null, val error: String? = null, val message: String? = null)

data class SmallsyncAccess(val storeUrl: String, val token: String) {
    val normalizedStoreUrl get() = storeUrl.trim().let { if (it.endsWith("/")) it else "$it/" }
}

class SmallsyncException(val status: Int, val error: String?, message: String) : Exception(message) {
    val isTokenRefused get() = status == HttpStatusCode.Unauthorized.value
}

@Serializable
private class PullAnswer(val entries: List<SmallsyncEntry>)

@Serializable
private class PushRequest(val entries: List<SmallsyncChange>)

@Serializable
private class PushAnswer(val results: List<SmallsyncPushResult>)

@Serializable
private class ErrorAnswer(val error: String? = null, val message: String? = null)

private val smallsyncJson = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
}

class SmallsyncClient(private val client: HttpClient) {
    suspend fun describeStore(access: SmallsyncAccess): SmallsyncStoreInfo =
        decode(SmallsyncStoreInfo.serializer(), client.get(access.normalizedStoreUrl) { bearerAuth(access.token) })

    suspend fun pull(access: SmallsyncAccess, since: Long, types: Collection<String>): List<SmallsyncEntry> {
        val entries = mutableListOf<SmallsyncEntry>()
        var cursor = since
        while (true) {
            val page = decode(
                PullAnswer.serializer(),
                client.get(access.normalizedStoreUrl + "pull") {
                    bearerAuth(access.token)
                    parameter("since", cursor)
                    parameter("types", types.joinToString(","))
                },
            ).entries
            if (page.isEmpty()) return entries
            entries += page
            cursor = page.last().seq
        }
    }

    suspend fun push(access: SmallsyncAccess, changes: List<SmallsyncChange>): List<SmallsyncPushResult> {
        val results = mutableListOf<SmallsyncPushResult>()
        for (batch in batchesUnder(changes, MAX_PUSH_BYTES)) {
            results += decode(
                PushAnswer.serializer(),
                client.post(access.normalizedStoreUrl + "push") {
                    bearerAuth(access.token)
                    jsonBody(smallsyncJson.encodeToString(PushRequest.serializer(), PushRequest(batch)))
                },
            ).results
        }
        return results
    }

    private fun batchesUnder(changes: List<SmallsyncChange>, maxBytes: Int): List<List<SmallsyncChange>> {
        val batches = mutableListOf<MutableList<SmallsyncChange>>()
        var size = 0
        for (change in changes) {
            val changeSize = smallsyncJson.encodeToString(SmallsyncChange.serializer(), change).encodeToByteArray().size + 1
            if (batches.isEmpty() || size + changeSize > maxBytes) {
                batches += mutableListOf<SmallsyncChange>()
                size = 0
            }
            batches.last() += change
            size += changeSize
        }
        return batches
    }

    private fun HttpRequestBuilder.jsonBody(json: String) {
        contentType(ContentType.Application.Json)
        setBody(json)
    }

    private suspend fun <T> decode(serializer: kotlinx.serialization.KSerializer<T>, response: HttpResponse): T {
        val body = response.bodyAsText()
        if (!response.status.isSuccess()) {
            val problem = try {
                smallsyncJson.decodeFromString(ErrorAnswer.serializer(), body)
            } catch (_: SerializationException) {
                ErrorAnswer()
            } catch (_: IllegalArgumentException) {
                ErrorAnswer()
            }
            throw SmallsyncException(response.status.value, problem.error, problem.message ?: "The store answered ${response.status}")
        }
        return smallsyncJson.decodeFromString(serializer, body)
    }

    private companion object {
        const val MAX_PUSH_BYTES = 512 * 1024
    }
}
