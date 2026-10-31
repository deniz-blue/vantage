package blue.deniz.vantage.core

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import io.ktor.http.parseUrl
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject

private const val PLC_DIRECTORY = "https://plc.directory"
private const val FALLBACK_PDS = "https://bsky.social"
private const val DNS_OVER_HTTPS = "https://cloudflare-dns.com/dns-query"
private const val HANDLE_RESOLVER = "https://public.api.bsky.app"

fun isAtActorIdentifier(text: String) = text.removePrefix("@").let { DID_PATTERN.matches(it) || HANDLE_PATTERN.matches(it) }

data class AtActor(val did: String, val handle: String?, val personalDataServer: String)

class AtIdentityResolver(private val client: HttpClient) {
    suspend fun resolve(handleOrDid: String): AtActor {
        val actor = handleOrDid.removePrefix("@")
        val typedHandle = actor.takeUnless { it.startsWith("did:") }?.lowercase()
        val did = typedHandle?.let { didOfHandle(it) ?: failure(EventError.Kind.Fetch, "$it does not resolve to a DID") } ?: actor
        val document = didDocumentOf(did)
        val claimedHandle = document.claimedHandle()
        val handle = when {
            claimedHandle == null -> null
            claimedHandle == typedHandle -> claimedHandle
            didOfHandle(claimedHandle) == did -> claimedHandle
            else -> null
        }
        return AtActor(did, handle, personalDataServerIn(did, document))
    }

    suspend fun personalDataServerOf(did: String): String = personalDataServerIn(did, didDocumentOf(did))

    private suspend fun didOfHandle(handle: String): String? = didFromDns(handle) ?: didFromXrpc(handle)

    private suspend fun didFromDns(handle: String): String? {
        val answers = getJsonOrNull(DNS_OVER_HTTPS, "name" to "_atproto.$handle", "type" to "TXT", accept = "application/dns-json")
            ?.get("Answer") as? JsonArray
        return answers.orEmpty().filterIsInstance<JsonObject>()
            .mapNotNull { it["data"].stringOrNull()?.trim('"')?.takeIf { text -> text.startsWith("did=") }?.removePrefix("did=") }
            .firstOrNull { DID_PATTERN.matches(it) }
    }

    private suspend fun didFromXrpc(handle: String): String? =
        getJsonOrNull("$HANDLE_RESOLVER/xrpc/com.atproto.identity.resolveHandle", "handle" to handle)
            ?.get("did").stringOrNull()?.takeIf { DID_PATTERN.matches(it) }

    private suspend fun getJsonOrNull(url: String, vararg parameters: Pair<String, String>, accept: String? = null): JsonObject? {
        val response = try {
            client.get(url) {
                for ((name, value) in parameters) parameter(name, value)
                if (accept != null) header(HttpHeaders.Accept, accept)
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            return null
        }
        if (!response.status.isSuccess()) return null
        return runCatching { parseJsonObject(response.bodyAsText(), "The answer") }.getOrNull()
    }

    private suspend fun didDocumentOf(did: String): JsonObject {
        val documentUrl = when {
            did.startsWith("did:plc:") -> "$PLC_DIRECTORY/$did"
            did.startsWith("did:web:") -> {
                val host = did.removePrefix("did:web:").replace("%3A", ":")
                if ('/' in host || host.count { it == ':' } > 1) failure(EventError.Kind.Fetch, "$did names a path, which atproto does not allow")
                "https://$host/.well-known/did.json"
            }
            else -> failure(EventError.Kind.Fetch, "$did is not a did:plc or did:web")
        }
        return parseJsonObject(client.getOk(documentUrl).bodyAsText(), "The DID document of $did")
    }

    private fun JsonObject.claimedHandle(): String? =
        (this["alsoKnownAs"] as? JsonArray).orEmpty()
            .firstNotNullOfOrNull { it.stringOrNull()?.takeIf { aka -> aka.startsWith("at://") }?.removePrefix("at://") }
            ?.lowercase()?.takeIf { HANDLE_PATTERN.matches(it) }

    private fun personalDataServerIn(did: String, document: JsonObject): String {
        val services = (document["service"] as? JsonArray).orEmpty().filterIsInstance<JsonObject>()
        val pds = services.firstOrNull { service ->
            val id = service["id"].stringOrNull()
            (id == "#atproto_pds" || id == "$did#atproto_pds") && service["type"].stringOrNull() == "AtprotoPersonalDataServer"
        }
        return pds?.get("serviceEndpoint").stringOrNull()?.takeIf { isHttpUrl(it) && parseUrl(it) != null } ?: FALLBACK_PDS
    }
}
