package blue.deniz.vantage.core

import kotlinx.serialization.SerializationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

private const val FEED_SNAPSHOT_KEY_PREFIX = "feedSnapshot."
private const val EVENTS_KEPT_PER_FEED = 50

interface FeedSnapshots {
    fun load(feed: EventFeed): List<ResolvedEvent>?

    fun save(feed: EventFeed, events: List<ResolvedEvent>)
}

@Serializable
private data class StoredFeedEvent(val source: EventSource, val format: EventFormat, val raw: String)

private val storedFeedEventsJson = ListSerializer(StoredFeedEvent.serializer())

class StoredFeedSnapshots(
    private val read: (key: String) -> String?,
    private val write: (key: String, value: String) -> Unit,
    private val resolver: EventResolver,
) : FeedSnapshots {
    override fun load(feed: EventFeed): List<ResolvedEvent>? {
        val stored = read(keyOf(feed)) ?: return null
        val events = try {
            Json.decodeFromString(storedFeedEventsJson, stored)
        } catch (_: SerializationException) {
            return null
        }
        return events.map { resolver.parse(ResolvedEvent(source = it.source, format = it.format, raw = it.raw)) }
    }

    override fun save(feed: EventFeed, events: List<ResolvedEvent>) {
        val kept = events.asSequence()
            .filter { it.data != null }
            .mapNotNull { event -> event.raw?.let { raw -> StoredFeedEvent(event.source, event.format, raw) } }
            .take(EVENTS_KEPT_PER_FEED)
            .toList()
        write(keyOf(feed), Json.encodeToString(storedFeedEventsJson, kept))
    }

    private fun keyOf(feed: EventFeed) = FEED_SNAPSHOT_KEY_PREFIX + when (feed) {
        is EventFeed.OpenEvntIndex -> "index:${feed.url}"
        is EventFeed.AtRepository -> "at:${feed.repo}/${feed.collection}"
    }
}
