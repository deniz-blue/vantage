package blue.deniz.vantage.core

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

private val AT_URI_PATTERN = Regex("""at://([^/?#]+)/([^/?#]+)/([^/?#]+)""")
internal val DID_PATTERN = Regex("""did:[a-z]+:[A-Za-z0-9._:%-]+""")
internal val HANDLE_PATTERN = Regex("""([A-Za-z0-9]([A-Za-z0-9-]{0,61}[A-Za-z0-9])?\.)+[A-Za-z]([A-Za-z0-9-]{0,61}[A-Za-z0-9])?""")
private val NSID_PATTERN = Regex("""[A-Za-z][A-Za-z0-9-]*(\.[A-Za-z0-9-]+)+""")
private val RECORD_KEY_PATTERN = Regex("""[A-Za-z0-9._~:-]{1,512}""")

@Serializable(with = AtUriSerializer::class)
data class AtUri(val repo: String, val collection: String, val rkey: String) {
    init {
        require(DID_PATTERN.matches(repo) || HANDLE_PATTERN.matches(repo)) { "$repo is not a DID or a handle" }
        require(NSID_PATTERN.matches(collection)) { "$collection is not an NSID" }
        require(RECORD_KEY_PATTERN.matches(rkey) && rkey != "." && rkey != "..") { "$rkey is not a record key" }
    }

    val repoIsDid get() = repo.startsWith("did:")

    override fun toString() = "at://$repo/$collection/$rkey"

    companion object {
        fun parse(text: String): AtUri {
            val (repo, collection, rkey) = AT_URI_PATTERN.matchEntire(text)?.destructured
                ?: throw IllegalArgumentException("$text is not an at:// record URI")
            return AtUri(repo, collection, rkey)
        }

        fun parseOrNull(text: String) = try {
            parse(text)
        } catch (_: IllegalArgumentException) {
            null
        }
    }
}

internal object AtUriSerializer : KSerializer<AtUri> {
    override val descriptor = PrimitiveSerialDescriptor("blue.deniz.vantage.core.AtUri", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: AtUri) = encoder.encodeString(value.toString())

    override fun deserialize(decoder: Decoder) = AtUri.parse(decoder.decodeString())
}
