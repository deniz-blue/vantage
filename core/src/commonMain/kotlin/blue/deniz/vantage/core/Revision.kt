package blue.deniz.vantage.core

import kotlinx.serialization.Serializable
import kotlin.time.Instant

@Serializable
data class Revision(
    val etag: String? = null,
    val lastModified: String? = null,
    val cid: String? = null,
    val revisionId: Long? = null,
    val revisionTimestamp: Instant? = null,
)
