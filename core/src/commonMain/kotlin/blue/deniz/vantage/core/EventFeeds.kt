package blue.deniz.vantage.core

import directory.evnt.OpenEvnt
import directory.evnt.Translations
import directory.evnt.withoutBlankTexts
import io.ktor.client.HttpClient
import io.ktor.client.statement.bodyAsText
import io.ktor.http.URLBuilder
import io.ktor.http.appendPathSegments
import io.ktor.http.parseUrl
import io.ktor.http.protocolWithAuthority
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull
import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.concurrent.atomics.incrementAndFetch
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes

private const val AT_RECORDS_PER_PAGE = 50
private const val INDEX_ITEMS_TO_RESOLVE = 200
private const val AT_PAGES_TO_READ = 20
private const val INDEX_PAGES_TO_READ = 20
private const val INDEX_EVENTS_RESOLVED_AT_ONCE = 8
private const val OPEN_EVNT_INDEX_VERSION = 1
private val FEED_FRESH_FOR = 10.minutes

sealed interface EventFeed {
    data class OpenEvntIndex(val url: String) : EventFeed

    data class AtRepository(val repo: String, val collection: String = OpenEvnt.AT_RECORD_TYPE) : EventFeed

    companion object {
        fun fromAddress(address: String): EventFeed? {
            val text = address.trim()
            return when {
                isHttpUrl(text) && parseUrl(text) != null -> OpenEvntIndex(text)
                isAtActorIdentifier(text) -> AtRepository(text.removePrefix("@"))
                else -> null
            }
        }
    }
}

val EventFeed.address: String
    get() = when (this) {
        is EventFeed.OpenEvntIndex -> url
        is EventFeed.AtRepository -> repo
    }

data class OpenEvntIndexItem(val url: String, val name: Translations?)

data class OpenEvntIndexPage(val items: List<OpenEvntIndexItem>, val next: String?, val total: Int?, val supports: Set<String> = emptySet())

data class AtEventPage(val events: List<ResolvedEvent>, val cursor: String?)

data class OpenEvntIndexEntry(val item: OpenEvntIndexItem, val event: ResolvedEvent?)

data class OpenEvntIndexEntries(val entries: List<OpenEvntIndexEntry>, val total: Int?, val readEveryItem: Boolean)

private data class AtPageKey(val did: String, val collection: String, val cursor: String?)

class EventFeedReader(
    private val client: HttpClient,
    private val resolver: EventResolver,
    private val clock: Clock = Clock.System,
    private val previews: EventPreviews = EventPreviews(resolver, clock),
) {
    private val identity = AtIdentityResolver(client)
    private val actors = ResultCache<String, AtActor>(FEED_FRESH_FOR, clock)
    private val indexPages = ResultCache<String, OpenEvntIndexPage>(FEED_FRESH_FOR, clock)
    private val atPages = ResultCache<AtPageKey, AtEventPage>(FEED_FRESH_FOR, clock)
    private val feedEvents = ResultCache<EventFeed, List<ResolvedEvent>>(FEED_FRESH_FOR, clock)

    suspend fun readIndexPage(url: String): OpenEvntIndexPage = indexPages.freshOrLoad(url) {
        val index = parseJsonObject(client.getOk(url).bodyAsText(), "The index")
        val version = (index["version"] as? JsonPrimitive)?.takeUnless { it.isString }?.intOrNull
        if (version != OPEN_EVNT_INDEX_VERSION) {
            failure(EventError.Kind.UnknownFormat, "The index is version ${index["version"] ?: "none"}, and only version $OPEN_EVNT_INDEX_VERSION is known")
        }
        val items = (index["items"] as? JsonArray).orEmpty().filterIsInstance<JsonObject>().mapNotNull { item ->
            val href = item["href"].stringOrNull()?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            OpenEvntIndexItem(resolveAgainstDomain(url, href), item["name"].translationsOrNull())
        }.distinctBy { it.url }
        OpenEvntIndexPage(
            items,
            next = index["next"].stringOrNull()?.takeIf { it.isNotBlank() }?.let { resolveAgainstDomain(url, it) },
            total = (index["total"] as? JsonPrimitive)?.takeUnless { it.isString }?.intOrNull,
            supports = (index["supports"] as? JsonArray).orEmpty().mapNotNull { it.stringOrNull() }.toSet(),
        )
    }

    @OptIn(ExperimentalAtomicApi::class)
    suspend fun readIndexEntries(
        indexUrl: String,
        query: EventQuery,
        onResolved: suspend (resolved: Int, of: Int) -> Unit = { _, _ -> },
    ): OpenEvntIndexEntries = coroutineScope {
        val first = readIndexPage(indexUrl)
        val pageUrl = query.indexPageUrl(indexUrl, first.supports)
        val start = if (pageUrl == indexUrl) first else readIndexPage(pageUrl)
        val items = start.items.toMutableList()
        val visited = mutableSetOf(pageUrl)
        var next = start.next
        while (next != null && next !in visited && visited.size < INDEX_PAGES_TO_READ && items.size < INDEX_ITEMS_TO_RESOLVE) {
            visited += next
            val page = readIndexPage(next)
            items += page.items
            next = page.next
        }
        val distinct = items.distinctBy { it.url }
        val toResolve = distinct.take(INDEX_ITEMS_TO_RESOLVE)
        val resolving = Semaphore(INDEX_EVENTS_RESOLVED_AT_ONCE)
        val resolved = AtomicInt(0)
        val entries = toResolve.map { item ->
            async {
                val event = ResolvedEvent.fromLink(item.url)?.let { link -> resolving.withPermit { previews.resolve(link) } }
                onResolved(resolved.incrementAndFetch(), toResolve.size)
                OpenEvntIndexEntry(item, event)
            }
        }.awaitAll()
        OpenEvntIndexEntries(entries, start.total, readEveryItem = (next == null || next in visited) && distinct.size == toResolve.size)
    }

    suspend fun resolveActor(handleOrDid: String): AtActor = actors.freshOrLoad(handleOrDid) { identity.resolve(handleOrDid) }

    fun lastResolvedActor(handleOrDid: String): AtActor? = actors.last(handleOrDid)

    fun lastReadEvents(feed: EventFeed): List<ResolvedEvent>? = feedEvents.last(feed)

    suspend fun readEvents(feed: EventFeed): List<ResolvedEvent> = feedEvents.freshOrLoad(feed) {
        when (feed) {
            is EventFeed.OpenEvntIndex -> readIndexEntries(feed.url, EventQuery(from = clock.now())).entries.mapNotNull { it.event }
            is EventFeed.AtRepository -> listAtEvents(resolveActor(feed.repo), feed.collection).events
        }
    }

    fun eventsOf(feed: EventFeed, preferredLanguages: List<String>): EventCollection = FeedEvents(this, feed, preferredLanguages)

    private data class FeedEvents(val reader: EventFeedReader, val feed: EventFeed, val preferredLanguages: List<String>) : EventCollection {
        override val orders = listOf(EventOrder.Start, EventOrder.Name)

        override fun list(query: EventQuery, limit: Int) = listingFlow { publish ->
            when (feed) {
                is EventFeed.OpenEvntIndex -> reader.listIndex(feed.url, query, limit, preferredLanguages, publish)
                is EventFeed.AtRepository -> reader.listAtRepository(feed, query, limit, preferredLanguages, publish)
            }
        }
    }

    private suspend fun listIndex(
        indexUrl: String,
        query: EventQuery,
        limit: Int,
        preferredLanguages: List<String>,
        publish: suspend (EventListing) -> Unit,
    ) {
        val supports = readIndexPage(indexUrl).supports
        val unsupported = query.unsupportedPart(supports)
        if (!unsupported.filtersOrSorts) return listIndexPages(query.indexPageUrl(indexUrl, supports), limit, publish)
        publish(EventListing(emptyList(), EventListingStatus.Loading()))
        val read = readIndexEntries(indexUrl, query) { resolved, of ->
            publish(EventListing(emptyList(), EventListingStatus.Loading(resolved.toFloat() / of)))
        }
        val events = read.entries.map { it.event ?: it.item.link() }
        val listedNames = read.entries.map { it.item }.listedNames()
        val matching = unsupported.filterAndSort(events, preferredLanguages, listedNames)
        publish(
            EventListing(
                matching.take(limit),
                if (matching.size > limit) EventListingStatus.HasMore else EventListingStatus.Complete,
                listedNames,
                EventCount.Matching(matching.size, read.entries.size, read.readEveryItem),
            ),
        )
    }

    private suspend fun listIndexPages(firstPageUrl: String, limit: Int, publish: suspend (EventListing) -> Unit) = coroutineScope {
        val items = mutableListOf<OpenEvntIndexItem>()
        val visited = mutableSetOf<String>()
        var next: String? = firstPageUrl
        var total: Int? = null
        while (next != null && next !in visited && items.size < limit) {
            if (items.isNotEmpty()) publish(listingOf(items, EventListingStatus.Loading(), total))
            visited += next
            val page = readIndexPage(next)
            val listed = items.mapTo(HashSet()) { it.url }
            items += page.items.filter { it.url !in listed }
            total = page.total ?: total
            next = page.next
        }
        val status = if (next != null && next !in visited) EventListingStatus.HasMore else EventListingStatus.Complete
        val first = listingOf(items, status, total)
        publish(first)
        val events = first.events.toMutableList()
        val changing = Mutex()
        val resolving = Semaphore(INDEX_EVENTS_RESOLVED_AT_ONCE)
        for (index in events.indices) launch {
            val resolved = resolving.withPermit { previews.resolve(events[index]) }
            changing.withLock {
                if (resolved == events[index]) return@withLock
                events[index] = resolved
                publish(first.copy(events = events.toList()))
            }
        }
    }

    private fun listingOf(items: List<OpenEvntIndexItem>, status: EventListingStatus, total: Int?) = EventListing(
        items.map { item -> item.link().let { previews.lastResolved(it) ?: it } },
        status,
        items.listedNames(),
        total?.let(EventCount::Total),
    )

    private suspend fun listAtRepository(
        feed: EventFeed.AtRepository,
        query: EventQuery,
        limit: Int,
        preferredLanguages: List<String>,
        publish: suspend (EventListing) -> Unit,
    ) {
        publish(EventListing(emptyList(), EventListingStatus.Loading()))
        val actor = resolveActor(feed.repo)
        val events = mutableListOf<ResolvedEvent>()
        var cursor: String? = null
        var pages = 0
        val wanted = if (query.filtersOrSorts) Int.MAX_VALUE else limit
        do {
            val page = listAtEvents(actor, feed.collection, cursor)
            events += page.events
            cursor = page.cursor
            pages++
            if (!query.filtersOrSorts && cursor != null && events.size < wanted) publish(EventListing(events.toList(), EventListingStatus.Loading()))
        } while (cursor != null && events.size < wanted && pages < AT_PAGES_TO_READ)
        if (!query.filtersOrSorts) {
            return publish(EventListing(events.toList(), if (cursor == null) EventListingStatus.Complete else EventListingStatus.HasMore))
        }
        val matching = query.filterAndSort(events, preferredLanguages)
        publish(
            EventListing(
                matching.take(limit),
                if (matching.size > limit) EventListingStatus.HasMore else EventListingStatus.Complete,
                count = EventCount.Matching(matching.size, events.size, readWholeSource = cursor == null),
            ),
        )
    }

    suspend fun listAtEvents(actor: AtActor, collection: String, cursor: String? = null): AtEventPage =
        atPages.freshOrLoad(AtPageKey(actor.did, collection, cursor)) { readAtPage(actor, collection, cursor) }

    private suspend fun readAtPage(actor: AtActor, collection: String, cursor: String?): AtEventPage {
        val format = EVENT_COLLECTIONS[collection] ?: failure(EventError.Kind.UnknownFormat, "$collection is not an event collection")
        val endpoint = URLBuilder(actor.personalDataServer).apply {
            appendPathSegments("xrpc", "com.atproto.repo.listRecords")
            parameters.append("repo", actor.did)
            parameters.append("collection", collection)
            parameters.append("limit", AT_RECORDS_PER_PAGE.toString())
            if (cursor != null) parameters.append("cursor", cursor)
        }.buildString()
        val page = parseJsonObject(client.getXrpc(endpoint).bodyAsText(), "The record list")
        val events = (page["records"] as? JsonArray).orEmpty().filterIsInstance<JsonObject>().mapNotNull { record ->
            val uri = record["uri"].stringOrNull()?.let(AtUri::parseOrNull) ?: return@mapNotNull null
            val value = record["value"] as? JsonObject ?: return@mapNotNull null
            resolver.parse(
                ResolvedEvent(source = EventSource.At(uri), format = format, raw = value.toString(), revision = Revision(cid = record["cid"].stringOrNull())),
            )
        }
        return AtEventPage(events, page["cursor"].stringOrNull()?.takeIf { it.isNotEmpty() })
    }

    private fun resolveAgainstDomain(indexUrl: String, link: String): String =
        if (isHttpUrl(link)) link else (parseUrl(indexUrl)?.protocolWithAuthority ?: "") + "/" + link.removePrefix("/")
}

private fun OpenEvntIndexItem.link(): ResolvedEvent = ResolvedEvent.fromLink(url) ?: ResolvedEvent(source = EventSource.Http(url))

private fun List<OpenEvntIndexItem>.listedNames(): Map<EventSource, Translations> =
    mapNotNull { item -> item.name?.let { item.link().source to it } }.toMap()

private fun listingFlow(list: suspend (publish: suspend (EventListing) -> Unit) -> Unit): Flow<EventListing> = channelFlow {
    var last: EventListing? = null
    try {
        list { listing ->
            last = listing
            send(listing)
        }
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (failed: Exception) {
        send((last ?: EventListing(emptyList(), EventListingStatus.Loading())).copy(status = EventListingStatus.Failed(failed.message ?: "Unknown error")))
    }
}

private fun JsonElement?.translationsOrNull(): Translations? =
    (this as? JsonObject)
        ?.mapNotNull { (language, text) -> text.stringOrNull()?.let { language to it } }
        ?.toMap()
        ?.withoutBlankTexts()
        ?.takeIf { it.isNotEmpty() }
